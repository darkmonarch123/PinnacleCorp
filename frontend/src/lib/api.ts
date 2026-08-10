/**
 * The real bridge to the Pinnacle Spring Boot backend. Everything here was
 * missing before — store.ts referenced this file in a comment, but it never
 * existed, so the entire app ran as an in-memory simulation with zero
 * network calls. This file makes every action in store.ts actually hit the
 * backend when VITE_API_BASE_URL is configured.
 *
 * Auth: Clerk exposes a global `window.Clerk` object once loaded — this is
 * the supported way to grab a session token from plain functions that
 * aren't React components.
 */

declare global {
  interface Window {
    Clerk?: {
      session?: {
        getToken(): Promise<string | null>;
      };
    };
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string | undefined;

/** True when a backend is actually configured — callers use this to decide whether to hit the network or fall back to local simulation. */
export const isApiConfigured = Boolean(API_BASE_URL);

async function getAuthToken(): Promise<string | null> {
  return (await window.Clerk?.session?.getToken()) ?? null;
}

async function authHeaders(): Promise<HeadersInit> {
  const token = await getAuthToken();
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  if (!API_BASE_URL) {
    throw new Error("VITE_API_BASE_URL is not configured — cannot reach the backend.");
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { ...(await authHeaders()), ...options.headers },
  });

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed: ${path} (${res.status})`);
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}

const get = <T>(path: string) => request<T>(path);
const post = <T>(path: string, body?: unknown) =>
  request<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined });
const patch = <T>(path: string, body?: unknown) =>
  request<T>(path, { method: "PATCH", body: body !== undefined ? JSON.stringify(body) : undefined });
const del = <T>(path: string) => request<T>(path, { method: "DELETE" });

// ---------------- Backend DTO shapes (match the Java records exactly) ----------------

export interface BackendAccountSummary {
  currency: string;
  balance: number;
  buyingPower: number;
  unrealizedPnl: number;
  equity: number;
}

export interface BackendTicker {
  id: string;
  symbol: string;
  exchange: string;
  sector: string;
}

export interface BackendCandle {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface BackendOrder {
  id: string;
  symbol: string;
  side: "BUY" | "SELL";
  type: "MARKET" | "LIMIT";
  status: "NEW" | "PENDING_RISK_CHECK" | "ROUTED" | "PARTIALLY_FILLED" | "FILLED" | "CANCELLED" | "EXPIRED" | "REJECTED";
  quantity: number;
  filledQuantity: number;
  limitPrice: number | null;
  stopLoss: number | null;
  takeProfit: number | null;
  rejectionReason: string | null;
  createdAt: string;
}

export interface BackendPosition {
  id: string;
  symbol: string;
  side: "BUY" | "SELL";
  status: "OPEN" | "MODIFIED" | "PARTIAL_CLOSE" | "PENDING_CLOSE" | "CLOSED";
  quantity: number;
  remainingQuantity: number;
  entryPrice: number;
  stopLoss: number | null;
  takeProfit: number | null;
  openedAt: string;
  closedAt: string | null;
}

export interface BackendTrade {
  id: string;
  symbol: string;
  side: "BUY" | "SELL";
  quantity: number;
  entryPrice: number;
  exitPrice: number;
  realizedPnl: number;
  openedAt: string;
  closedAt: string;
}

export interface BackendWatchlistItem {
  id: string;
  symbol: string;
  lastPrice: number | null;
}

export interface BackendAlert {
  id: string;
  symbol: string;
  targetPrice: number;
  condition: "ABOVE" | "BELOW";
  active: boolean;
  triggeredAt: string | null;
  createdAt: string;
}

export interface BackendProfile {
  id: string;
  email: string;
  fullName: string;
  country: string | null;
  baseCurrency: string;
  twoFactorEnabled: boolean;
  notificationsEnabled: boolean;
  kycCompleted: boolean;
  emailVerified: boolean;
}

export interface BackendCurrencyRate {
  currency: string;
  usdRate: number;
}

export interface BackendBankTransfer {
  id: string;
  amount: number;
  currency: string;
  transferReference: string;
  status: "PENDING" | "CONFIRMED" | "REJECTED";
  adminNote: string | null;
  createdAt: string;
  confirmedAt: string | null;
}

export interface BackendCryptoFunding {
  id: string;
  cryptoType: string;
  network: string;
  walletAddress: string;
  transactionHash: string;
  amount: number;
  usdEquivalent: number | null;
  status: "PENDING" | "CONFIRMED" | "REJECTED";
  adminNote: string | null;
  createdAt: string;
  confirmedAt: string | null;
}

// ---------------- API surface ----------------

export const api = {
  // Account
  getAccountSummary: () => get<BackendAccountSummary>("/api/account"),
  resetDemoBalance: () => post<BackendAccountSummary>("/api/account/reset"),

  // Profile / KYC
  getProfile: () => get<BackendProfile>("/api/users/me"),
  updateProfile: (body: Partial<Pick<BackendProfile, "fullName" | "baseCurrency" | "twoFactorEnabled" | "notificationsEnabled">>) =>
    patch<BackendProfile>("/api/users/me", body),
  submitKyc: (fullName: string, dateOfBirth: string, country: string) =>
    post<void>("/api/auth/kyc", { fullName, dateOfBirth, country }),

  // Market data
  getTickers: () => get<BackendTicker[]>("/api/market-data/tickers"),
  getCandles: (symbol: string, timeframe: string, limit = 500) =>
    get<BackendCandle[]>(`/api/market-data/candles?symbol=${symbol}&timeframe=${timeframe}&limit=${limit}`),
  getCurrencyRates: () => get<BackendCurrencyRate[]>("/api/currency-rates"),

  // Orders
  placeOrder: (body: {
    symbol: string; side: "BUY" | "SELL"; type: "MARKET" | "LIMIT";
    quantity: number; limitPrice?: number; stopLoss?: number; takeProfit?: number;
  }) => post<BackendOrder>("/api/orders", body),
  listOrders: () => get<BackendOrder[]>("/api/orders"),
  cancelOrder: (orderId: string) => post<BackendOrder>(`/api/orders/${orderId}/cancel`),

  // Positions
  listPositions: (openOnly = true) => get<BackendPosition[]>(`/api/positions?openOnly=${openOnly}`),
  modifyPosition: (positionId: string, body: { stopLoss?: number; takeProfit?: number; clearStopLoss?: boolean; clearTakeProfit?: boolean }) =>
    patch<BackendPosition>(`/api/positions/${positionId}`, body),
  closePosition: (positionId: string, quantity?: number) =>
    post<BackendPosition>(`/api/positions/${positionId}/close`, quantity != null ? { quantity } : {}),

  // Watchlist
  getWatchlist: () => get<BackendWatchlistItem[]>("/api/watchlist"),
  addToWatchlist: (symbol: string) => post<BackendWatchlistItem>("/api/watchlist", { symbol }),
  removeFromWatchlist: (symbol: string) => del<void>(`/api/watchlist/${symbol}`),

  // Alerts
  listAlerts: () => get<BackendAlert[]>("/api/alerts"),
  createAlert: (symbol: string, targetPrice: number, condition: "ABOVE" | "BELOW") =>
    post<BackendAlert>("/api/alerts", { symbol, targetPrice, condition }),
  deleteAlert: (alertId: string) => del<void>(`/api/alerts/${alertId}`),

  // Trades / reporting
  listTrades: () => get<BackendTrade[]>("/api/trades"),
  getTradeStats: () =>
    get<{ totalTrades: number; winningTrades: number; losingTrades: number; winRatePercent: number; avgWinLossRatio: number | null; maxDrawdownPercent: number; totalRealizedPnl: number }>("/api/trades/stats"),
  getEquityCurve: () => get<{ time: string; equity: number }[]>("/api/trades/equity-curve"),

  // Funding — bank transfer
  createBankTransfer: (amount: number, currency: string) =>
    post<BackendBankTransfer>("/api/funding/bank-transfer", { amount, currency }),
  listMyBankTransfers: () => get<BackendBankTransfer[]>("/api/funding/bank-transfer"),

  // Funding — crypto
  createCryptoFunding: (body: { cryptoType: string; network: string; walletAddress: string; transactionHash: string; amount: number }) =>
    post<BackendCryptoFunding>("/api/funding/crypto", body),
  listMyCryptoFundings: () => get<BackendCryptoFunding[]>("/api/funding/crypto"),

  // Funding — admin (requires the signed-in user's User.isAdmin() to be true on the backend)
  listPendingBankTransfers: () => get<BackendBankTransfer[]>("/api/funding/admin/bank-transfer/pending"),
  confirmBankTransfer: (requestId: string, adminNote?: string) =>
    post<BackendBankTransfer>(`/api/funding/admin/bank-transfer/${requestId}/confirm`, { adminNote }),
  rejectBankTransfer: (requestId: string, adminNote?: string) =>
    post<BackendBankTransfer>(`/api/funding/admin/bank-transfer/${requestId}/reject`, { adminNote }),
  listPendingCryptoFundings: () => get<BackendCryptoFunding[]>("/api/funding/admin/crypto/pending"),
  confirmCryptoFunding: (requestId: string, usdEquivalent: number, adminNote?: string) =>
    post<BackendCryptoFunding>(`/api/funding/admin/crypto/${requestId}/confirm`, { usdEquivalent, adminNote }),
  rejectCryptoFunding: (requestId: string, adminNote?: string) =>
    post<BackendCryptoFunding>(`/api/funding/admin/crypto/${requestId}/reject`, { adminNote }),
};
