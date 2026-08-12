import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Panel, StatCard } from "@/components/Panel";
import { TickerLogo } from "@/components/TickerLogo";
import { dateTime, money, num, pnlClass, signedMoney } from "@/lib/format";
import { accountSummary, actions, positionsValue, recentTrades, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/portfolio")({
  head: () => ({
    meta: [
      { title: "Portfolio — Pinnacle" },
      { name: "description", content: "Equity, allocation, open positions and recent simulated trades." },
      { property: "og:title", content: "Portfolio — Pinnacle" },
      { property: "og:description", content: "Equity, allocation, open positions and recent simulated trades." },
    ],
  }),
  component: PortfolioPage,
});

function PortfolioPage() {
  const summary = useStore(accountSummary);
  const positions = useStore((s) => s.positions);
  const prices = useStore((s) => s.prices);
  const invested = useStore(positionsValue);
  const trades = useStore(recentTrades);

  const numberField =
    "font-num w-24 rounded-lg border border-input bg-card px-2 py-1.5 text-xs";

  return (
    <AppShell title="Portfolio">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Equity" value={money(summary.equity)} hint="Cash + open positions" highlight />
        <StatCard
          label="Unrealized P&L"
          value={signedMoney(summary.unrealized)}
          tone={pnlClass(summary.unrealized)}
          hint="Across open positions"
        />
        <StatCard label="Buying power" value={money(summary.buyingPower)} hint="Available virtual cash" />
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
        <Panel title="Allocation">
          {positions.length === 0 ? (
            <p className="text-sm text-muted-foreground">No open positions to allocate.</p>
          ) : (
            <ul className="space-y-4">
              {positions.map((p) => {
                const value = p.quantity * (prices[p.symbol] ?? p.avgPrice);
                const pct = invested ? (value / invested) * 100 : 0;
                return (
                  <li key={p.id}>
                    <div className="flex items-center justify-between text-sm">
                      <span className="flex items-center gap-2">
                        <TickerLogo symbol={p.symbol} className="size-6" />
                        {p.symbol}
                      </span>
                      <span className="font-num text-muted-foreground">{num(pct, 1)}%</span>
                    </div>
                    <div className="mt-1.5 h-2 rounded-full bg-secondary">
                      <div className="h-2 rounded-full bg-lime" style={{ width: `${pct}%` }} />
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </Panel>

        <Panel title="Open positions" bodyClassName="p-0">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-sm">
              <thead className="text-left text-xs text-muted-foreground">
                <tr>
                  {["Symbol", "Qty", "Avg", "Last", "P&L", "Stop loss", "Take profit", ""].map((h) => (
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
                      <td className="px-5 py-3">
                        <input
                          type="number"
                          step="0.01"
                          aria-label={`Stop loss for ${p.symbol}`}
                          defaultValue={p.stopLoss ?? ""}
                          placeholder="—"
                          onBlur={(e) =>
                            actions.updatePosition(p.id, { stopLoss: e.target.value ? Number(e.target.value) : null })
                          }
                          className={numberField}
                        />
                      </td>
                      <td className="px-5 py-3">
                        <input
                          type="number"
                          step="0.01"
                          aria-label={`Take profit for ${p.symbol}`}
                          defaultValue={p.takeProfit ?? ""}
                          placeholder="—"
                          onBlur={(e) =>
                            actions.updatePosition(p.id, { takeProfit: e.target.value ? Number(e.target.value) : null })
                          }
                          className={numberField}
                        />
                      </td>
                      <td className="px-5 py-3 text-right">
                        <button
                          type="button"
                          onClick={() => {
                            actions.closeQuantity(p.id, p.quantity);
                            toast.success(`Closed ${p.quantity} ${p.symbol}`);
                          }}
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
                    <td colSpan={8} className="px-5 py-10 text-center text-sm text-muted-foreground">
                      No open positions.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>

      <Panel title="Recent trades" className="mt-5" bodyClassName="p-0">
        <ul className="divide-y divide-border">
          {trades.map((t) => (
            <li key={t.id} className="flex items-center justify-between gap-4 px-5 py-3.5 text-sm">
              <span className="flex min-w-0 items-center gap-2.5">
                <TickerLogo symbol={t.symbol} className="size-7" />
                <span className="min-w-0">
                  <span className="font-medium">{t.symbol}</span>
                  <span className="font-num ml-2 text-xs text-muted-foreground">
                    {t.quantity} @ {money(t.entryPrice)} → {money(t.exitPrice)}
                  </span>
                </span>
              </span>
              <span className="flex items-center gap-4">
                <span className="font-num hidden text-xs text-muted-foreground sm:block">{dateTime(t.closedAt)}</span>
                <span className={cn("font-num text-sm font-medium", pnlClass(t.pnl))}>{signedMoney(t.pnl)}</span>
              </span>
            </li>
          ))}
        </ul>
      </Panel>
    </AppShell>
  );
}