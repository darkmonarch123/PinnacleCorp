import { useSyncExternalStore } from "react";

import {
  INSTRUMENTS,
  STARTING_BALANCE,
  type Alert,
  type Instrument,
  type Order,
  type OrderType,
  type Position,
  type Side,
  type Trade,
} from "./market";
import {
  api,
  isApiConfigured,
  type BackendAlert,
  type BackendOrder,
  type BackendPosition,
  type BackendTrade,
} from "./api";

/**
 * Client-side trading state.
 *
 * IMPORTANT — how the real backend is wired in: actions below still update
 * `state` synchronously first (optimistic, using the same local simulation
 * this file always had), then — when VITE_API_BASE_URL is configured — fire
 * the real backend call in the background and call `syncFromBackend()` once
 * it resolves, which overwrites local state with backend truth.
 *
 * This is a deliberate compromise, not an oversight: rewriting every action
 * to be properly async (and updating every call site across the route files
 * to await it and show loading states) was out of scope for the time
 * available. The tradeoff: the UI still feels instant (optimistic update),
 * but for a brief window after any action, what's on screen is the local
 * simulation's guess, not confirmed backend state, until the background
 * sync lands. Known limitation — see FINAL_HANDOFF.md.
 */
export interface AppState {
  profile: { fullName: string; currency: string; kycComplete: boolean };
  notifications: boolean;
  twoFactor: boolean;
  cash: number;
  prices: Record<string, number>;
  watchlist: string[];
  positions: Position[];
  orders: Order[];
  trades: Trade[];
  alerts: Alert[];
}

const seedTrades: Trade[] = [
  { id: "t1", symbol: "NVDA", side: "BUY", quantity: 12, entryPrice: 118.4, exitPrice: 126.1, pnl: 92.4, openedAt: 1759400000000, closedAt: 1759660000000 },
  { id: "t2", symbol: "TSLA", side: "BUY", quantity: 5, entryPrice: 262.1, exitPrice: 248.3, pnl: -69, openedAt: 1759480000000, closedAt: 1759720000000 },
  { id: "t3", symbol: "AAPL", side: "BUY", quantity: 9, entryPrice: 219.8, exitPrice: 228.05, pnl: 74.25, openedAt: 1759560000000, closedAt: 1759800000000 },
  { id: "t4", symbol: "META", side: "SELL", quantity: 3, entryPrice: 521.4, exitPrice: 508.9, pnl: 37.5, openedAt: 1759620000000, closedAt: 1759860000000 },
  { id: "t5", symbol: "AMD", side: "BUY", quantity: 20, entryPrice: 152.2, exitPrice: 147.9, pnl: -86, openedAt: 1759700000000, closedAt: 1759940000000 },
  { id: "t6", symbol: "MSFT", side: "BUY", quantity: 4, entryPrice: 418.6, exitPrice: 432.4, pnl: 55.2, openedAt: 1759780000000, closedAt: 1760020000000 },
];

function initialState(): AppState {
  return {
    profile: { fullName: "", currency: "USD", kycComplete: false },
    notifications: true,
    twoFactor: false,
    cash: 6_842.35,
    prices: Object.fromEntries(INSTRUMENTS.map((i) => [i.symbol, i.price])),
    watchlist: ["AAPL", "NVDA", "TSLA", "MSFT", "AMZN", "META"],
    positions: [
      { id: "p1", symbol: "AAPL", quantity: 8, avgPrice: 221.4, stopLoss: 210, takeProfit: 245, openedAt: 1759900000000 },
      { id: "p2", symbol: "NVDA", quantity: 14, avgPrice: 121.9, stopLoss: null, takeProfit: 140, openedAt: 1759930000000 },
      { id: "p3", symbol: "MSFT", quantity: 2, avgPrice: 439.5, stopLoss: 415, takeProfit: null, openedAt: 1759960000000 },
    ],
    orders: [
      { id: "o1", symbol: "TSLA", side: "BUY", type: "LIMIT", quantity: 6, filledQuantity: 0, limitPrice: 232, status: "PENDING", createdAt: 1760010000000 },
      { id: "o2", symbol: "AAPL", side: "BUY", type: "MARKET", quantity: 8, filledQuantity: 8, limitPrice: null, status: "FILLED", createdAt: 1759900000000 },
      { id: "o3", symbol: "AMD", side: "SELL", type: "LIMIT", quantity: 20, filledQuantity: 20, limitPrice: 147.9, status: "FILLED", createdAt: 1759940000000 },
      { id: "o4", symbol: "GOOGL", side: "BUY", type: "LIMIT", quantity: 10, filledQuantity: 0, limitPrice: 160, status: "PENDING", createdAt: 1760030000000 },
      { id: "o5", symbol: "NFLX", side: "BUY", type: "MARKET", quantity: 1, filledQuantity: 0, limitPrice: null, status: "CANCELLED", createdAt: 1759870000000 },
    ],
    trades: seedTrades,
    alerts: [
      { id: "a1", symbol: "NVDA", targetPrice: 135, condition: "ABOVE", triggeredAt: null },
      { id: "a2", symbol: "TSLA", targetPrice: 235, condition: "BELOW", triggeredAt: null },
      { id: "a3", symbol: "AAPL", targetPrice: 226, condition: "ABOVE", triggeredAt: 1760040000000 },
    ],
  };
}

let state: AppState = initialState();
const listeners = new Set<() => void>();

function emit() {
  state = { ...state };
  listeners.forEach((l) => l());
}

function subscribe(l: () => void) {
  listeners.add(l);
  return () => listeners.delete(l);
}

export function useStore<T>(selector: (s: AppState) => T): T {
  return useSyncExternalStore(
    subscribe,
    () => selector(state),
    () => selector(state),
  );
}

export function getState() {
  return state;
}

export const priceOf = (s: AppState, symbol: string) =>
  s.prices[symbol] ?? INSTRUMENTS.find((i) => i.symbol === symbol)?.price ?? 0;

export const instrumentOf = (symbol: string): Instrument =>
  INSTRUMENTS.find((i) => i.symbol === symbol) ?? INSTRUMENTS[0]!;

export function positionsValue(s: AppState) {
  return s.positions.reduce((sum, p) => sum + p.quantity * priceOf(s, p.symbol), 0);
}

export function unrealizedPnl(s: AppState) {
  return s.positions.reduce((sum, p) => sum + (priceOf(s, p.symbol) - p.avgPrice) * p.quantity, 0);
}

export function accountSummary(s: AppState) {
  const invested = positionsValue(s);
  const equity = s.cash + invested;
  const unrealized = unrealizedPnl(s);
  const realized = s.trades.reduce((a, t) => a + t.pnl, 0);
  return {
    balance: s.cash,
    equity,
    invested,
    buyingPower: s.cash,
    dayPnl: unrealized * 0.42,
    unrealized,
    totalPnl: equity - STARTING_BALANCE,
    realized,
  };
}

const uid = () => Math.random().toString(36).slice(2, 10);

/* ---------------------------- backend <-> local mapping ---------------------------- */

function mapOrderStatus(status: BackendOrder["status"]): Order["status"] {
  switch (status) {
    case "FILLED":
    case "PARTIALLY_FILLED":
      return "FILLED";
    case "CANCELLED":
    case "EXPIRED":
      return "CANCELLED";
    case "REJECTED":
      return "REJECTED";
    default:
      return "PENDING";
  }
}

function mapOrder(o: BackendOrder): Order {
  return {
    id: o.id,
    symbol: o.symbol,
    side: o.side,
    type: o.type,
    quantity: o.quantity,
    filledQuantity: o.filledQuantity,
    limitPrice: o.limitPrice,
    status: mapOrderStatus(o.status),
    createdAt: new Date(o.createdAt).getTime(),
  };
}

function mapPosition(p: BackendPosition): Position {
  return {
    id: p.id,
    symbol: p.symbol,
    quantity: p.remainingQuantity,
    avgPrice: p.entryPrice,
    stopLoss: p.stopLoss,
    takeProfit: p.takeProfit,
    openedAt: new Date(p.openedAt).getTime(),
  };
}

function mapTrade(t: BackendTrade): Trade {
  return {
    id: t.id,
    symbol: t.symbol,
    side: t.side,
    quantity: t.quantity,
    entryPrice: t.entryPrice,
    exitPrice: t.exitPrice,
    pnl: t.realizedPnl,
    openedAt: new Date(t.openedAt).getTime(),
    closedAt: new Date(t.closedAt).getTime(),
  };
}

function mapAlert(a: BackendAlert): Alert {
  return {
    id: a.id,
    symbol: a.symbol,
    targetPrice: a.targetPrice,
    condition: a.condition,
    triggeredAt: a.triggeredAt ? new Date(a.triggeredAt).getTime() : null,
  };
}

export async function syncFromBackend(): Promise<void> {
  if (!isApiConfigured) return;

  try {
    const [summary, profile, orders, positions, trades, alerts, watchlist] = await Promise.all([
      api.getAccountSummary(),
      api.getProfile().catch(() => null),
      api.listOrders(),
      api.listPositions(true),
      api.listTrades(),
      api.listAlerts(),
      api.getWatchlist(),
    ]);

    state.cash = summary.balance;
    if (profile) {
      state.profile = {
        fullName: profile.fullName,
        currency: profile.baseCurrency,
        kycComplete: profile.kycCompleted,
      };
      state.notifications = profile.notificationsEnabled;
      state.twoFactor = profile.twoFactorEnabled;
    }
    state.orders = orders.map(mapOrder);
    state.positions = positions.map(mapPosition);
    state.trades = trades.map(mapTrade);
    state.alerts = alerts.map(mapAlert);
    state.watchlist = watchlist.map((w) => w.symbol);
    for (const w of watchlist) {
      if (w.lastPrice != null) state.prices[w.symbol] = w.lastPrice;
    }
    emit();
  } catch (err) {
    console.error("syncFromBackend failed, continuing on local state:", err);
  }
}

if (isApiConfigured) {
  void syncFromBackend();
}

/* ---------------------------------- actions --------------------------------- */

export const actions = {
  tickPrices() {
    const next = { ...state.prices };
    for (const inst of INSTRUMENTS) {
      const cur = next[inst.symbol] ?? inst.price;
      next[inst.symbol] = Math.max(1, +(cur + (Math.random() - 0.5) * cur * 0.0015).toFixed(2));
    }
    state.prices = next;
    state.alerts = state.alerts.map((a) => {
      if (a.triggeredAt) return a;
      const p = next[a.symbol] ?? 0;
      const hit = a.condition === "ABOVE" ? p >= a.targetPrice : p <= a.targetPrice;
      return hit ? { ...a, triggeredAt: Date.now() } : a;
    });
    emit();
  },

  completeKyc(fullName: string) {
    state.profile = { ...state.profile, fullName, kycComplete: true };
    emit();
    if (isApiConfigured) {
      api.submitKyc(fullName, "2000-01-01", "Unknown")
        .then(syncFromBackend)
        .catch((err) => console.error("submitKyc failed:", err));
    }
  },

  updateProfile(patch: Partial<AppState["profile"]>) {
    state.profile = { ...state.profile, ...patch };
    emit();
    if (isApiConfigured) {
      api.updateProfile({
        ...(patch.fullName !== undefined ? { fullName: patch.fullName } : {}),
        ...(patch.currency !== undefined ? { baseCurrency: patch.currency } : {}),
      }).then(syncFromBackend).catch((err) => console.error("updateProfile failed:", err));
    }
  },

  toggleSetting(key: "notifications" | "twoFactor", value: boolean) {
    state[key] = value;
    emit();
    if (isApiConfigured) {
      api.updateProfile(
        key === "notifications" ? { notificationsEnabled: value } : { twoFactorEnabled: value }
      ).catch((err) => console.error("toggleSetting failed:", err));
    }
  },

  toggleWatch(symbol: string) {
    const wasWatched = state.watchlist.includes(symbol);
    state.watchlist = wasWatched ? state.watchlist.filter((s) => s !== symbol) : [...state.watchlist, symbol];
    emit();
    if (isApiConfigured) {
      (wasWatched ? api.removeFromWatchlist(symbol) : api.addToWatchlist(symbol))
        .catch((err) => console.error("toggleWatch failed:", err));
    }
  },

  placeOrder(input: {
    symbol: string;
    side: Side;
    type: OrderType;
    quantity: number;
    limitPrice: number | null;
    stopLoss: number | null;
    takeProfit: number | null;
  }): { ok: boolean; message: string } {
    const price = input.type === "LIMIT" && input.limitPrice ? input.limitPrice : priceOf(state, input.symbol);
    const cost = price * input.quantity;
    if (input.quantity <= 0) return { ok: false, message: "Quantity must be greater than zero." };
    if (input.side === "BUY" && input.type === "MARKET" && cost > state.cash)
      return { ok: false, message: "Insufficient buying power for this order." };

    const fills = input.type === "MARKET";
    const order: Order = {
      id: uid(),
      symbol: input.symbol,
      side: input.side,
      type: input.type,
      quantity: input.quantity,
      filledQuantity: fills ? input.quantity : 0,
      limitPrice: input.type === "LIMIT" ? input.limitPrice : null,
      status: fills ? "FILLED" : "PENDING",
      createdAt: Date.now(),
    };
    state.orders = [order, ...state.orders];

    if (fills) {
      if (input.side === "BUY") {
        state.cash -= cost;
        const existing = state.positions.find((p) => p.symbol === input.symbol);
        if (existing) {
          const qty = existing.quantity + input.quantity;
          state.positions = state.positions.map((p) =>
            p.id === existing.id
              ? {
                  ...p,
                  quantity: qty,
                  avgPrice: (existing.avgPrice * existing.quantity + cost) / qty,
                  stopLoss: input.stopLoss ?? p.stopLoss,
                  takeProfit: input.takeProfit ?? p.takeProfit,
                }
              : p,
          );
        } else {
          state.positions = [
            ...state.positions,
            {
              id: uid(),
              symbol: input.symbol,
              quantity: input.quantity,
              avgPrice: price,
              stopLoss: input.stopLoss,
              takeProfit: input.takeProfit,
              openedAt: Date.now(),
            },
          ];
        }
      } else {
        const existing = state.positions.find((p) => p.symbol === input.symbol);
        if (!existing) return { ok: false, message: "No open position to sell for this symbol." };
        const qty = Math.min(existing.quantity, input.quantity);
        actions.closeQuantity(existing.id, qty, price);
        return { ok: true, message: `Sold ${qty} ${input.symbol} at market.` };
      }
    }
    emit();

    if (isApiConfigured) {
      api.placeOrder({
        symbol: input.symbol,
        side: input.side,
        type: input.type,
        quantity: input.quantity,
        ...(input.limitPrice != null ? { limitPrice: input.limitPrice } : {}),
        ...(input.stopLoss != null ? { stopLoss: input.stopLoss } : {}),
        ...(input.takeProfit != null ? { takeProfit: input.takeProfit } : {}),
      }).then(syncFromBackend).catch((err) => console.error("placeOrder failed:", err));
    }

    return {
      ok: true,
      message: fills
        ? `${input.side === "BUY" ? "Bought" : "Sold"} ${input.quantity} ${input.symbol}.`
        : `Limit order for ${input.quantity} ${input.symbol} is pending.`,
    };
  },

  closeQuantity(positionId: string, quantity: number, exitPrice?: number) {
    const pos = state.positions.find((p) => p.id === positionId);
    if (!pos) return;
    const price = exitPrice ?? priceOf(state, pos.symbol);
    const qty = Math.min(quantity, pos.quantity);
    state.cash += price * qty;
    state.trades = [
      {
        id: uid(),
        symbol: pos.symbol,
        side: "BUY",
        quantity: qty,
        entryPrice: pos.avgPrice,
        exitPrice: price,
        pnl: +((price - pos.avgPrice) * qty).toFixed(2),
        openedAt: pos.openedAt,
        closedAt: Date.now(),
      },
      ...state.trades,
    ];
    state.positions =
      qty >= pos.quantity
        ? state.positions.filter((p) => p.id !== positionId)
        : state.positions.map((p) => (p.id === positionId ? { ...p, quantity: p.quantity - qty } : p));
    emit();

    if (isApiConfigured && !positionId.match(/^p\d+$/) && positionId.length > 10) {
      api.closePosition(positionId, qty).then(syncFromBackend).catch((err) => console.error("closePosition failed:", err));
    }
  },

  updatePosition(positionId: string, patch: Partial<Pick<Position, "stopLoss" | "takeProfit">>) {
    state.positions = state.positions.map((p) => (p.id === positionId ? { ...p, ...patch } : p));
    emit();
    if (isApiConfigured && positionId.length > 10) {
      const body: {
        stopLoss?: number;
        takeProfit?: number;
        clearStopLoss?: boolean;
        clearTakeProfit?: boolean;
      } = {};
      if (patch.stopLoss !== undefined) {
        if (patch.stopLoss == null) body.clearStopLoss = true;
        else body.stopLoss = patch.stopLoss as number;
      }
      if (patch.takeProfit !== undefined) {
        if (patch.takeProfit == null) body.clearTakeProfit = true;
        else body.takeProfit = patch.takeProfit as number;
      }

      api.modifyPosition(positionId, body).then(syncFromBackend).catch((err) => console.error("modifyPosition failed:", err));
    }
  },

  cancelOrder(id: string) {
    state.orders = state.orders.map((o) => (o.id === id && o.status === "PENDING" ? { ...o, status: "CANCELLED" } : o));
    emit();
    if (isApiConfigured && id.length > 10) {
      api.cancelOrder(id).then(syncFromBackend).catch((err) => console.error("cancelOrder failed:", err));
    }
  },

  createAlert(symbol: string, targetPrice: number, condition: Alert["condition"]) {
    state.alerts = [{ id: uid(), symbol, targetPrice, condition, triggeredAt: null }, ...state.alerts];
    emit();
    if (isApiConfigured) {
      api.createAlert(symbol, targetPrice, condition).then(syncFromBackend).catch((err) => console.error("createAlert failed:", err));
    }
  },

  deleteAlert(id: string) {
    state.alerts = state.alerts.filter((a) => a.id !== id);
    emit();
    if (isApiConfigured && id.length > 10) {
      api.deleteAlert(id).catch((err) => console.error("deleteAlert failed:", err));
    }
  },

  resetDemoBalance() {
    state.cash = STARTING_BALANCE;
    state.positions = [];
    state.orders = state.orders.map((o) => (o.status === "PENDING" ? { ...o, status: "CANCELLED" } : o));
    emit();
    if (isApiConfigured) {
      api.resetDemoBalance().then(syncFromBackend).catch((err) => console.error("resetDemoBalance failed:", err));
    }
  },
};
