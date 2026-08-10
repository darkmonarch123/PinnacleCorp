export type Side = "BUY" | "SELL";
export type OrderType = "MARKET" | "LIMIT";
export type OrderStatus = "PENDING" | "FILLED" | "CANCELLED" | "REJECTED";

export interface Instrument {
  symbol: string;
  name: string;
  domain: string;
  sector: string;
  price: number;
  prevClose: number;
}

export interface Candle {
  t: number;
  o: number;
  h: number;
  l: number;
  c: number;
}

export interface Position {
  id: string;
  symbol: string;
  quantity: number;
  avgPrice: number;
  stopLoss: number | null;
  takeProfit: number | null;
  openedAt: number;
}

export interface Order {
  id: string;
  symbol: string;
  side: Side;
  type: OrderType;
  quantity: number;
  filledQuantity: number;
  limitPrice: number | null;
  status: OrderStatus;
  createdAt: number;
}

export interface Trade {
  id: string;
  symbol: string;
  side: Side;
  quantity: number;
  entryPrice: number;
  exitPrice: number;
  pnl: number;
  openedAt: number;
  closedAt: number;
}

export interface Alert {
  id: string;
  symbol: string;
  targetPrice: number;
  condition: "ABOVE" | "BELOW";
  triggeredAt: number | null;
}

export const STARTING_BALANCE = 10_000;

export const INSTRUMENTS: Instrument[] = [
  { symbol: "AAPL", name: "Apple Inc.", domain: "apple.com", sector: "Technology", price: 228.42, prevClose: 226.1 },
  { symbol: "MSFT", name: "Microsoft Corp.", domain: "microsoft.com", sector: "Technology", price: 431.18, prevClose: 434.9 },
  { symbol: "NVDA", name: "NVIDIA Corp.", domain: "nvidia.com", sector: "Semiconductors", price: 126.74, prevClose: 122.05 },
  { symbol: "TSLA", name: "Tesla Inc.", domain: "tesla.com", sector: "Automotive", price: 248.9, prevClose: 253.4 },
  { symbol: "AMZN", name: "Amazon.com Inc.", domain: "amazon.com", sector: "Consumer", price: 186.33, prevClose: 184.7 },
  { symbol: "GOOGL", name: "Alphabet Inc.", domain: "abc.xyz", sector: "Technology", price: 172.05, prevClose: 173.6 },
  { symbol: "META", name: "Meta Platforms", domain: "meta.com", sector: "Technology", price: 512.6, prevClose: 505.2 },
  { symbol: "JPM", name: "JPMorgan Chase", domain: "jpmorganchase.com", sector: "Financials", price: 214.87, prevClose: 213.02 },
  { symbol: "NFLX", name: "Netflix Inc.", domain: "netflix.com", sector: "Media", price: 664.15, prevClose: 671.4 },
  { symbol: "AMD", name: "Advanced Micro Devices", domain: "amd.com", sector: "Semiconductors", price: 148.22, prevClose: 145.9 },
];

function mulberry(seed: number) {
  let a = seed;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const TF_MS: Record<string, number> = {
  "1m": 60_000,
  "5m": 300_000,
  "1h": 3_600_000,
  "1D": 86_400_000,
  "1W": 604_800_000,
};

export const TIMEFRAMES = ["1m", "5m", "1h", "1D", "1W"] as const;
export type Timeframe = (typeof TIMEFRAMES)[number];

/** Deterministic OHLC series so server and client render the same chart. */
export function buildCandles(symbol: string, timeframe: Timeframe, count = 70): Candle[] {
  const inst = INSTRUMENTS.find((i) => i.symbol === symbol) ?? INSTRUMENTS[0]!;
  const seed = [...symbol].reduce((a, c) => a + c.charCodeAt(0), 0) * (TIMEFRAMES.indexOf(timeframe) + 3);
  const rnd = mulberry(seed);
  const step = TF_MS[timeframe] ?? 60_000;
  const vol = inst.price * (0.004 + TIMEFRAMES.indexOf(timeframe) * 0.004);
  const out: Candle[] = [];
  let price = inst.price * (1 - (rnd() * 0.08 + 0.02));
  const end = Math.floor(1_760_000_000_000 / step) * step;
  for (let i = count - 1; i >= 0; i--) {
    const drift = (inst.price - price) * 0.05;
    const o = price;
    const c = Math.max(1, o + drift + (rnd() - 0.5) * vol * 2);
    const h = Math.max(o, c) + rnd() * vol;
    const l = Math.min(o, c) - rnd() * vol;
    out.push({ t: end - i * step, o, h, l, c });
    price = c;
  }
  return out;
}

export function equityCurve(trades: Trade[]) {
  let running = STARTING_BALANCE;
  return [{ i: 0, equity: running }, ...trades.map((t, i) => ({ i: i + 1, equity: (running += t.pnl) }))];
}
