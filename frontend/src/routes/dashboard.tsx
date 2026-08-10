import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { CandleChart } from "@/components/CandleChart";
import { OrderTicket } from "@/components/OrderTicket";
import { Panel } from "@/components/Panel";
import { QuoteChip, useQuote } from "@/components/Quote";
import { TickerLogo } from "@/components/TickerLogo";
import { dateTime, money, pnlClass, signedMoney, signedPct } from "@/lib/format";
import { buildCandles, TIMEFRAMES, type Timeframe } from "@/lib/market";
import { actions, priceOf, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "Trading dashboard — Pinnacle" },
      { name: "description", content: "Charts, watchlist, order ticket and open positions in one simulated trading workspace." },
      { property: "og:title", content: "Trading dashboard — Pinnacle" },
      { property: "og:description", content: "Charts, watchlist, order ticket and open positions in one simulated trading workspace." },
    ],
  }),
  component: Dashboard,
});

type Tab = "positions" | "orders" | "alerts";

function Dashboard() {
  const [symbol, setSymbol] = useState("AAPL");
  const [timeframe, setTimeframe] = useState<Timeframe>("1h");
  const [tab, setTab] = useState<Tab>("positions");

  const watchlist = useStore((s) => s.watchlist);
  const positions = useStore((s) => s.positions);
  const prices = useStore((s) => s.prices);
  const orders = useStore((s) => s.orders.filter((o) => o.status === "PENDING"));
  const alerts = useStore((s) => s.alerts);
  const quote = useQuote(symbol);

  const candles = useMemo(() => buildCandles(symbol, timeframe), [symbol, timeframe]);

  return (
    <AppShell title="Dashboard">
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <div className="space-y-5">
          <Panel bodyClassName="p-4 sm:p-5">
            <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4">
              <div className="flex min-w-0 items-center gap-3">
                <TickerLogo symbol={symbol} className="size-10" />
                <div className="min-w-0">
                  <h2 className="truncate text-lg font-semibold">{symbol}</h2>
                  <p className="truncate text-xs text-muted-foreground">{quote.name}</p>
                </div>
                <div className="ml-3 hidden sm:block">
                  <div className="font-num text-xl font-medium">{money(quote.price)}</div>
                  <div className={cn("font-num text-xs", pnlClass(quote.change))}>{signedPct(quote.change)} today</div>
                </div>
              </div>
              <div role="group" aria-label="Chart timeframe" className="flex gap-1 rounded-xl bg-secondary p-1">
                {TIMEFRAMES.map((tf) => (
                  <button
                    key={tf}
                    type="button"
                    aria-pressed={tf === timeframe}
                    onClick={() => setTimeframe(tf)}
                    className={cn(
                      "font-num press rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors",
                      tf === timeframe ? "bg-card shadow-soft" : "text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {tf}
                  </button>
                ))}
              </div>
            </div>
            <div className="mt-4">
              <CandleChart candles={candles} />
            </div>
          </Panel>

          <div className="-mx-4 overflow-x-auto px-4 md:mx-0 md:px-0">
            <div className="flex gap-2.5 pb-1">
              {watchlist.map((s) => (
                <QuoteChip key={s} symbol={s} active={s === symbol} onSelect={setSymbol} />
              ))}
            </div>
          </div>

          <Panel bodyClassName="p-0">
            <div role="tablist" aria-label="Account activity" className="flex gap-1 border-b border-border px-3 pt-3">
              {(
                [
                  ["positions", `Open positions (${positions.length})`],
                  ["orders", `Pending orders (${orders.length})`],
                  ["alerts", `Alerts (${alerts.filter((a) => !a.triggeredAt).length})`],
                ] as [Tab, string][]
              ).map(([id, label]) => (
                <button
                  key={id}
                  role="tab"
                  aria-selected={tab === id}
                  onClick={() => setTab(id)}
                  className={cn(
                    "rounded-t-lg px-3.5 py-2.5 text-sm font-medium transition-colors",
                    tab === id ? "border-b-2 border-foreground" : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {label}
                </button>
              ))}
            </div>

            <div className="overflow-x-auto">
              {tab === "positions" && (
                <table className="w-full min-w-[620px] text-sm">
                  <thead className="text-left text-xs text-muted-foreground">
                    <tr>
                      {["Symbol", "Qty", "Avg price", "Last", "Unrealized P&L", ""].map((h) => (
                        <th key={h} className="px-5 py-2.5 font-medium">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {positions.map((p) => {
                      const last = prices[p.symbol] ?? p.avgPrice;
                      const pnl = (last - p.avgPrice) * p.quantity;
                      return (
                        <tr key={p.id} className="border-t border-border">
                          <td className="px-5 py-3 font-medium">{p.symbol}</td>
                          <td className="font-num px-5 py-3">{p.quantity}</td>
                          <td className="font-num px-5 py-3">{money(p.avgPrice)}</td>
                          <td className="font-num px-5 py-3">{money(last)}</td>
                          <td className={cn("font-num px-5 py-3", pnlClass(pnl))}>{signedMoney(pnl)}</td>
                          <td className="px-5 py-3 text-right">
                            <button
                              type="button"
                              onClick={() => actions.closeQuantity(p.id, p.quantity)}
                              className="press rounded-lg border border-input px-3 py-1.5 text-xs font-medium hover:bg-secondary"
                            >
                              Close
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                    {positions.length === 0 && (
                      <tr>
                        <td colSpan={6} className="px-5 py-8 text-center text-sm text-muted-foreground">
                          No open positions.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              )}

              {tab === "orders" && (
                <table className="w-full min-w-[620px] text-sm">
                  <thead className="text-left text-xs text-muted-foreground">
                    <tr>
                      {["Symbol", "Side", "Type", "Qty", "Limit", "Placed", ""].map((h) => (
                        <th key={h} className="px-5 py-2.5 font-medium">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map((o) => (
                      <tr key={o.id} className="border-t border-border">
                        <td className="px-5 py-3 font-medium">{o.symbol}</td>
                        <td className="px-5 py-3">{o.side === "BUY" ? "+ Buy" : "− Sell"}</td>
                        <td className="px-5 py-3">{o.type}</td>
                        <td className="font-num px-5 py-3">{o.quantity}</td>
                        <td className="font-num px-5 py-3">{o.limitPrice ? money(o.limitPrice) : "—"}</td>
                        <td className="font-num px-5 py-3 text-muted-foreground">{dateTime(o.createdAt)}</td>
                        <td className="px-5 py-3 text-right">
                          <button
                            type="button"
                            onClick={() => actions.cancelOrder(o.id)}
                            className="press rounded-lg border border-input px-3 py-1.5 text-xs font-medium hover:bg-secondary"
                          >
                            Cancel
                          </button>
                        </td>
                      </tr>
                    ))}
                    {orders.length === 0 && (
                      <tr>
                        <td colSpan={7} className="px-5 py-8 text-center text-sm text-muted-foreground">
                          No pending orders.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              )}

              {tab === "alerts" && (
                <ul className="divide-y divide-border">
                  {alerts.map((a) => (
                    <li key={a.id} className="flex items-center justify-between gap-4 px-5 py-3.5 text-sm">
                      <span className="flex items-center gap-2.5">
                        <TickerLogo symbol={a.symbol} className="size-7" />
                        <span>
                          <span className="font-medium">{a.symbol}</span>{" "}
                          <span className="text-muted-foreground">
                            {a.condition === "ABOVE" ? "rises above" : "falls below"}
                          </span>{" "}
                          <span className="font-num">{money(a.targetPrice)}</span>
                        </span>
                      </span>
                      <span
                        className={cn(
                          "rounded-full px-2.5 py-1 text-xs font-medium",
                          a.triggeredAt ? "bg-lime text-lime-ink" : "bg-secondary text-muted-foreground",
                        )}
                      >
                        {a.triggeredAt ? "Triggered" : "Watching"}
                      </span>
                    </li>
                  ))}
                  {alerts.length === 0 && (
                    <li className="px-5 py-8 text-center text-sm text-muted-foreground">No alerts yet.</li>
                  )}
                </ul>
              )}
            </div>
          </Panel>
        </div>

        <div className="xl:sticky xl:top-28 xl:self-start">
          <OrderTicket symbol={symbol} />
        </div>
      </div>
    </AppShell>
  );
}

