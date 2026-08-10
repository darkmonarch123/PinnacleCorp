import { createFileRoute } from "@tanstack/react-router";
import { Download } from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Panel, StatCard } from "@/components/Panel";
import { dateTime, money, num, pnlClass, signedMoney } from "@/lib/format";
import { equityCurve, INSTRUMENTS } from "@/lib/market";
import { useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/history")({
  head: () => ({
    meta: [
      { title: "Trade history — Pinnacle" },
      { name: "description", content: "Win rate, drawdown, equity curve and a filterable log of your closed simulated trades." },
      { property: "og:title", content: "Trade history — Pinnacle" },
      { property: "og:description", content: "Win rate, drawdown, equity curve and a filterable log of your closed simulated trades." },
    ],
  }),
  component: HistoryPage,
});

function EquityChart({ points }: { points: { i: number; equity: number }[] }) {
  const hi = Math.max(...points.map((p) => p.equity));
  const lo = Math.min(...points.map((p) => p.equity));
  const range = hi - lo || 1;
  const d = points
    .map((p, i) => `${i === 0 ? "M" : "L"} ${(i / Math.max(1, points.length - 1)) * 100} ${58 - ((p.equity - lo) / range) * 50}`)
    .join(" ");
  return (
    <svg viewBox="0 0 100 60" preserveAspectRatio="none" className="h-48 w-full" role="img" aria-label="Equity curve">
      {[8, 33, 58].map((y) => (
        <line key={y} x1="0" x2="100" y1={y} y2={y} className="stroke-border" strokeWidth="0.4" />
      ))}
      <path d={`${d} L 100 60 L 0 60 Z`} className="fill-lime/25" stroke="none" />
      <path d={d} fill="none" className="stroke-gain" strokeWidth="1" vectorEffect="non-scaling-stroke" />
    </svg>
  );
}

function HistoryPage() {
  const trades = useStore((s) => s.trades);
  const [symbol, setSymbol] = useState("ALL");
  const [outcome, setOutcome] = useState<"ALL" | "WIN" | "LOSS">("ALL");

  const stats = useMemo(() => {
    const wins = trades.filter((t) => t.pnl > 0);
    const losses = trades.filter((t) => t.pnl <= 0);
    const avgWin = wins.length ? wins.reduce((a, t) => a + t.pnl, 0) / wins.length : 0;
    const avgLoss = losses.length ? Math.abs(losses.reduce((a, t) => a + t.pnl, 0) / losses.length) : 0;
    const curve = equityCurve([...trades].reverse());
    let peak = -Infinity;
    let maxDd = 0;
    for (const p of curve) {
      peak = Math.max(peak, p.equity);
      maxDd = Math.max(maxDd, ((peak - p.equity) / peak) * 100);
    }
    return {
      winRate: trades.length ? (wins.length / trades.length) * 100 : 0,
      ratio: avgLoss ? avgWin / avgLoss : avgWin ? Infinity : 0,
      maxDd,
      realized: trades.reduce((a, t) => a + t.pnl, 0),
      curve,
    };
  }, [trades]);

  const rows = trades.filter(
    (t) =>
      (symbol === "ALL" || t.symbol === symbol) &&
      (outcome === "ALL" || (outcome === "WIN" ? t.pnl > 0 : t.pnl <= 0)),
  );

  const exportCsv = () => {
    const header = "symbol,side,quantity,entry,exit,pnl,opened,closed";
    const body = rows
      .map((t) =>
        [t.symbol, t.side, t.quantity, t.entryPrice, t.exitPrice, t.pnl, new Date(t.openedAt).toISOString(), new Date(t.closedAt).toISOString()].join(","),
      )
      .join("\n");
    const url = URL.createObjectURL(new Blob([`${header}\n${body}`], { type: "text/csv" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = "pinnacle-trade-history.csv";
    a.click();
    URL.revokeObjectURL(url);
  };

  const select = "rounded-xl border border-input bg-card px-3 py-2 text-sm";

  return (
    <AppShell title="Trade history">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Win rate" value={`${num(stats.winRate, 1)}%`} hint={`${trades.length} closed trades`} highlight />
        <StatCard label="Avg win / loss" value={stats.ratio === Infinity ? "∞" : num(stats.ratio)} hint="Reward-to-risk realised" />
        <StatCard label="Max drawdown" value={`−${num(stats.maxDd, 1)}%`} tone="text-loss" hint="Peak-to-trough equity" />
        <StatCard
          label="Realized P&L"
          value={signedMoney(stats.realized)}
          tone={pnlClass(stats.realized)}
          hint="All closed trades"
        />
      </div>

      <Panel title="Equity curve" className="mt-5">
        <EquityChart points={stats.curve} />
      </Panel>

      <Panel
        title="Closed trades"
        className="mt-5"
        bodyClassName="p-0"
        action={
          <div className="flex flex-wrap items-center gap-2">
            <label className="sr-only" htmlFor="filter-symbol">Filter by symbol</label>
            <select id="filter-symbol" value={symbol} onChange={(e) => setSymbol(e.target.value)} className={select}>
              <option value="ALL">All symbols</option>
              {INSTRUMENTS.map((i) => (
                <option key={i.symbol}>{i.symbol}</option>
              ))}
            </select>
            <label className="sr-only" htmlFor="filter-outcome">Filter by outcome</label>
            <select
              id="filter-outcome"
              value={outcome}
              onChange={(e) => setOutcome(e.target.value as "ALL" | "WIN" | "LOSS")}
              className={select}
            >
              <option value="ALL">All outcomes</option>
              <option value="WIN">Wins</option>
              <option value="LOSS">Losses</option>
            </select>
            <button
              type="button"
              onClick={exportCsv}
              className="press inline-flex items-center gap-2 rounded-xl bg-lime px-3.5 py-2 text-sm font-semibold text-lime-ink hover:brightness-95"
            >
              <Download className="size-4" /> CSV
            </button>
          </div>
        }
      >
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-sm">
            <thead className="text-left text-xs text-muted-foreground">
              <tr>
                {["Symbol", "Side", "Qty", "Entry", "Exit", "P&L", "Result", "Closed"].map((h) => (
                  <th key={h} className="px-5 py-2.5 font-medium">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((t) => (
                <tr key={t.id} className="border-t border-border">
                  <td className="px-5 py-3 font-medium">{t.symbol}</td>
                  <td className="px-5 py-3">{t.side === "BUY" ? "+ Long" : "− Short"}</td>
                  <td className="font-num px-5 py-3">{t.quantity}</td>
                  <td className="font-num px-5 py-3">{money(t.entryPrice)}</td>
                  <td className="font-num px-5 py-3">{money(t.exitPrice)}</td>
                  <td className={cn("font-num px-5 py-3", pnlClass(t.pnl))}>{signedMoney(t.pnl)}</td>
                  <td className="px-5 py-3">
                    <span
                      className={cn(
                        "rounded-full px-2.5 py-1 text-xs font-medium",
                        t.pnl > 0 ? "bg-lime text-lime-ink" : "bg-secondary text-muted-foreground",
                      )}
                    >
                      {t.pnl > 0 ? "Win" : "Loss"}
                    </span>
                  </td>
                  <td className="font-num px-5 py-3 text-muted-foreground">{dateTime(t.closedAt)}</td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-10 text-center text-sm text-muted-foreground">
                    No closed trades match these filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>
    </AppShell>
  );
}
