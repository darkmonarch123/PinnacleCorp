-- Pinnacle: initial schema
-- Loaded automatically by the postgres container on first boot.

CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===================== ENUM TYPES =====================

CREATE TYPE order_side       AS ENUM ('BUY', 'SELL');
CREATE TYPE order_type       AS ENUM ('MARKET', 'LIMIT');
CREATE TYPE order_status     AS ENUM (
  'NEW', 'PENDING_RISK_CHECK', 'ROUTED',
  'PARTIALLY_FILLED', 'FILLED',
  'CANCELLED', 'EXPIRED', 'REJECTED'
);
CREATE TYPE position_status  AS ENUM (
  'OPEN', 'MODIFIED', 'PARTIAL_CLOSE', 'PENDING_CLOSE', 'CLOSED'
);
CREATE TYPE ledger_entry_type AS ENUM (
  'DEPOSIT', 'ORDER_FILL_DEBIT', 'ORDER_FILL_CREDIT',
  'REALIZED_PNL', 'FEE', 'DEMO_RESET'
);
CREATE TYPE alert_condition  AS ENUM ('ABOVE', 'BELOW');
CREATE TYPE timeframe        AS ENUM ('1m', '5m', '1h', '1D', '1W');
CREATE TYPE funding_status   AS ENUM ('PENDING', 'CONFIRMED', 'REJECTED');

-- ===================== CORE RELATIONAL TABLES =====================

CREATE TABLE users (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  clerk_user_id     VARCHAR(255) UNIQUE,
  email             VARCHAR(255) NOT NULL UNIQUE,
  password_hash     VARCHAR(255), -- nullable: Clerk owns credentials now, this is a pre-Clerk leftover
  full_name         VARCHAR(255) NOT NULL,
  date_of_birth     DATE,
  country           VARCHAR(2),
  email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
  kyc_completed     BOOLEAN NOT NULL DEFAULT FALSE,
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  is_admin          BOOLEAN NOT NULL DEFAULT FALSE, -- gates funding-request confirm/reject; no broader role system exists
  base_currency     VARCHAR(3) NOT NULL DEFAULT 'USD',
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  currency          VARCHAR(3) NOT NULL DEFAULT 'USD', -- account's operating currency; see currency_rates for conversion
  balance           NUMERIC(18,2) NOT NULL DEFAULT 10000.00, -- derived cache; source of truth is ledger_entries
  buying_power      NUMERIC(18,2) NOT NULL DEFAULT 10000.00,
  margin_used       NUMERIC(18,2) NOT NULL DEFAULT 0,
  is_demo           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id)
);

-- Fixed/mock conversion rates for the demo — not live FX. One row per
-- supported currency, rate expressed as "1 unit of currency = X USD".
CREATE TABLE currency_rates (
  currency          VARCHAR(3) PRIMARY KEY,
  usd_rate          NUMERIC(18,6) NOT NULL,
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO currency_rates (currency, usd_rate) VALUES
  ('USD', 1.0),
  ('NGN', 0.00062),
  ('GHS', 0.068),
  ('KES', 0.0077),
  ('ZAR', 0.055),
  ('EGP', 0.020)
ON CONFLICT (currency) DO NOTHING;

CREATE TABLE tickers (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  symbol            VARCHAR(16) NOT NULL UNIQUE,
  exchange           VARCHAR(32) NOT NULL,
  sector            VARCHAR(64),
  min_order_size    NUMERIC(18,4) NOT NULL DEFAULT 1,
  max_order_size    NUMERIC(18,4) NOT NULL DEFAULT 100000,
  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  account_id        UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  ticker_id         UUID NOT NULL REFERENCES tickers(id),
  side              order_side NOT NULL,
  type              order_type NOT NULL,
  status            order_status NOT NULL DEFAULT 'NEW',
  quantity          NUMERIC(18,4) NOT NULL,
  filled_quantity   NUMERIC(18,4) NOT NULL DEFAULT 0,
  limit_price       NUMERIC(18,4),
  stop_loss         NUMERIC(18,4),
  take_profit       NUMERIC(18,4),
  rejection_reason  VARCHAR(255),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE positions (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  account_id        UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  ticker_id         UUID NOT NULL REFERENCES tickers(id),
  origin_order_id   UUID NOT NULL REFERENCES orders(id),
  side              order_side NOT NULL,
  status            position_status NOT NULL DEFAULT 'OPEN',
  quantity          NUMERIC(18,4) NOT NULL,
  remaining_quantity NUMERIC(18,4) NOT NULL,
  entry_price       NUMERIC(18,4) NOT NULL,
  stop_loss         NUMERIC(18,4),
  take_profit       NUMERIC(18,4),
  opened_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at         TIMESTAMPTZ
);

CREATE TABLE trades (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  account_id        UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  position_id       UUID NOT NULL REFERENCES positions(id),
  ticker_id         UUID NOT NULL REFERENCES tickers(id),
  side              order_side NOT NULL,
  quantity          NUMERIC(18,4) NOT NULL,
  entry_price       NUMERIC(18,4) NOT NULL,
  exit_price        NUMERIC(18,4) NOT NULL,
  realized_pnl      NUMERIC(18,2) NOT NULL,
  opened_at         TIMESTAMPTZ NOT NULL,
  closed_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  account_id        UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  entry_type        ledger_entry_type NOT NULL,
  amount            NUMERIC(18,2) NOT NULL, -- signed: positive credits, negative debits
  reference_order_id    UUID REFERENCES orders(id),
  reference_trade_id    UUID REFERENCES trades(id),
  description       VARCHAR(255),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE price_alerts (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  ticker_id         UUID NOT NULL REFERENCES tickers(id),
  target_price      NUMERIC(18,4) NOT NULL,
  condition         alert_condition NOT NULL,
  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  triggered_at      TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Simulated funding: no real banking or blockchain integration. Confirmation
-- is administrator-gated (users.is_admin); confirming posts a DEPOSIT entry
-- via the existing ledger, never mutates account balance directly.
CREATE TABLE bank_transfer_requests (
  id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  account_id          UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  amount              NUMERIC(18,2) NOT NULL,
  currency            VARCHAR(3) NOT NULL,
  transfer_reference  VARCHAR(64) NOT NULL UNIQUE,
  status              funding_status NOT NULL DEFAULT 'PENDING',
  admin_note          VARCHAR(255),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  confirmed_at        TIMESTAMPTZ
);
CREATE INDEX idx_bank_transfer_account_status ON bank_transfer_requests(account_id, status);

CREATE TABLE crypto_funding_requests (
  id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  account_id          UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  crypto_type         VARCHAR(16) NOT NULL,
  network             VARCHAR(64) NOT NULL,
  wallet_address       VARCHAR(128) NOT NULL,
  transaction_hash     VARCHAR(128) NOT NULL UNIQUE,
  amount               NUMERIC(28,8) NOT NULL,
  usd_equivalent       NUMERIC(18,2),
  status               funding_status NOT NULL DEFAULT 'PENDING',
  admin_note           VARCHAR(255),
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  confirmed_at         TIMESTAMPTZ
);
CREATE INDEX idx_crypto_funding_account_status ON crypto_funding_requests(account_id, status);

CREATE TABLE watchlist_items (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  ticker_id         UUID NOT NULL REFERENCES tickers(id),
  sort_order        INTEGER NOT NULL DEFAULT 0,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, ticker_id)
);

-- Not in the original brand/schema sheet: added to back the "background job
-- triggers in-app notification on match" requirement for price alerts.
CREATE TABLE notifications (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  message           VARCHAR(255) NOT NULL,
  reference_alert_id UUID REFERENCES price_alerts(id),
  is_read           BOOLEAN NOT NULL DEFAULT FALSE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_created_at ON notifications(user_id, created_at DESC);

-- ===================== INDEXES =====================

CREATE INDEX idx_orders_account_status ON orders(account_id, status);
CREATE INDEX idx_positions_account_status ON positions(account_id, status);
CREATE INDEX idx_trades_account_closed_at ON trades(account_id, closed_at DESC);
CREATE INDEX idx_ledger_account_created_at ON ledger_entries(account_id, created_at DESC);
CREATE INDEX idx_price_alerts_ticker_active ON price_alerts(ticker_id) WHERE is_active = TRUE;

-- ===================== TIMESCALE HYPERTABLES =====================

CREATE TABLE price_ticks (
  time              TIMESTAMPTZ NOT NULL,
  symbol            VARCHAR(16) NOT NULL,
  price             NUMERIC(18,4) NOT NULL,
  volume            NUMERIC(20,4)
);
SELECT create_hypertable('price_ticks', 'time');
CREATE INDEX idx_price_ticks_symbol_time ON price_ticks(symbol, time DESC);
-- Raw ticks are high-volume and short-lived: 7-day retention.
SELECT add_retention_policy('price_ticks', INTERVAL '7 days');

CREATE TABLE price_ohlc (
  time              TIMESTAMPTZ NOT NULL,
  symbol            VARCHAR(16) NOT NULL,
  timeframe         timeframe NOT NULL,
  open              NUMERIC(18,4) NOT NULL,
  high              NUMERIC(18,4) NOT NULL,
  low               NUMERIC(18,4) NOT NULL,
  close             NUMERIC(18,4) NOT NULL,
  volume            NUMERIC(20,4)
);
SELECT create_hypertable('price_ohlc', 'time');
-- Retained indefinitely: candles are the durable historical record.
CREATE INDEX idx_price_ohlc_symbol_tf_time ON price_ohlc(symbol, timeframe, time DESC);
-- One candle per (symbol, timeframe, bucket) — required for the rollup job's ON CONFLICT upsert.
CREATE UNIQUE INDEX uq_price_ohlc_symbol_tf_time ON price_ohlc(symbol, timeframe, time);

-- ===================== SEED DATA =====================

INSERT INTO tickers (symbol, exchange, sector) VALUES
  ('AAPL', 'NASDAQ', 'Technology'),
  ('MSFT', 'NASDAQ', 'Technology'),
  ('GOOGL', 'NASDAQ', 'Technology'),
  ('AMZN', 'NASDAQ', 'Consumer Discretionary'),
  ('TSLA', 'NASDAQ', 'Consumer Discretionary'),
  ('NVDA', 'NASDAQ', 'Technology'),
  ('JPM',  'NYSE',   'Financials'),
  ('V',    'NYSE',   'Financials'),
  -- Nigerian Exchange (NGX)
  ('DANGCEM', 'NGX', 'Industrials'),
  ('MTNN', 'NGX', 'Telecommunications'),
  ('ZENITHBANK', 'NGX', 'Financials'),
  ('GTCO', 'NGX', 'Financials'),
  -- Johannesburg Stock Exchange (JSE)
  ('NPN', 'JSE', 'Technology'),
  ('SHP', 'JSE', 'Consumer Discretionary'),
  ('SOL', 'JSE', 'Energy'),
  ('SBK', 'JSE', 'Financials'),
  -- Nairobi Securities Exchange (NSE)
  ('SCOM', 'NSE', 'Telecommunications'),
  ('EQTY', 'NSE', 'Financials'),
  ('KCB', 'NSE', 'Financials'),
  -- Ghana Stock Exchange (GSE)
  ('MTNGH', 'GSE', 'Telecommunications'),
  ('EGH', 'GSE', 'Financials'),
  -- Egyptian Exchange (EGX)
  ('COMI', 'EGX', 'Financials'),
  ('ETEL', 'EGX', 'Telecommunications')
ON CONFLICT (symbol) DO NOTHING;
