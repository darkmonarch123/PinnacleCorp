import { createFileRoute } from "@tanstack/react-router";
import { BellRing, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Panel } from "@/components/Panel";
import { TickerLogo } from "@/components/TickerLogo";
import { dateTime, money } from "@/lib/format";
import { INSTRUMENTS } from "@/lib/market";
import { actions, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/alerts")({
  head: () => ({
    meta: [
      { title: "Price alerts — Pinnacle" },
      { name: "description", content: "Create price alerts above or below any level and watch the live triggered feed." },
      { property: "og:title", content: "Price alerts — Pinnacle" },
      { property: "og:description", content: "Create price alerts above or below any level and watch the live triggered feed." },
    ],
  }),
  component: AlertsPage,
});

function AlertsPage() {
  const alerts = useStore((s) => s.alerts);
  const [symbol, setSymbol] = useState(INSTRUMENTS[0]!.symbol);
  const [price, setPrice] = useState("");
  const [condition, setCondition] = useState<"ABOVE" | "BELOW">("ABOVE");

  const active = alerts.filter((a) => !a.triggeredAt);
  const triggered = alerts.filter((a) => a.triggeredAt);
  const field = "mt-1.5 w-full rounded-xl border border-input bg-card px-3 py-2.5 text-sm";

  return (
    <AppShell title="Alerts">
      <div className="grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
        <Panel title="New alert">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              actions.createAlert(symbol, Number(price), condition);
              setPrice("");
              toast.success(`Alert set for ${symbol}`);
            }}
          >
            <div>
              <label htmlFor="alert-symbol" className="text-xs font-medium text-muted-foreground">Symbol</label>
              <select id="alert-symbol" value={symbol} onChange={(e) => setSymbol(e.target.value)} className={field}>
                {INSTRUMENTS.map((i) => (
                  <option key={i.symbol}>{i.symbol}</option>
                ))}
              </select>
            </div>

            <div className="mt-3">
              <span className="text-xs font-medium text-muted-foreground">Condition</span>
              <div role="group" aria-label="Alert condition" className="mt-1.5 grid grid-cols-2 gap-1 rounded-xl bg-secondary p-1">
                {(["ABOVE", "BELOW"] as const).map((c) => (
                  <button
                    key={c}
                    type="button"
                    aria-pressed={condition === c}
                    onClick={() => setCondition(c)}
                    className={cn(
                      "press rounded-lg py-2 text-xs font-medium transition-colors",
                      condition === c ? "bg-card shadow-soft" : "text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {c === "ABOVE" ? "Rises above" : "Falls below"}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-3">
              <label htmlFor="alert-price" className="text-xs font-medium text-muted-foreground">Target price</label>
              <input
                id="alert-price"
                type="number"
                step="0.01"
                required
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="0.00"
                className={`${field} font-num`}
              />
            </div>

            <button
              type="submit"
              className="press mt-5 w-full rounded-xl bg-lime px-4 py-3 text-sm font-semibold text-lime-ink hover:brightness-95"
            >
              Create alert
            </button>
          </form>
        </Panel>

        <div className="space-y-5">
          <Panel title={`Active alerts (${active.length})`} bodyClassName="p-0">
            <ul className="divide-y divide-border">
              {active.map((a) => (
                <li key={a.id} className="flex items-center gap-3 px-5 py-3.5 text-sm">
                  <TickerLogo symbol={a.symbol} className="size-7" />
                  <span className="min-w-0 flex-1">
                    <span className="font-medium">{a.symbol}</span>{" "}
                    <span className="text-muted-foreground">
                      {a.condition === "ABOVE" ? "rises above" : "falls below"}
                    </span>{" "}
                    <span className="font-num">{money(a.targetPrice)}</span>
                  </span>
                  <button
                    type="button"
                    onClick={() => actions.deleteAlert(a.id)}
                    aria-label={`Delete ${a.symbol} alert`}
                    className="press grid size-9 min-h-9 place-items-center rounded-lg border border-input hover:bg-secondary"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </li>
              ))}
              {active.length === 0 && (
                <li className="px-5 py-10 text-center text-sm text-muted-foreground">No active alerts.</li>
              )}
            </ul>
          </Panel>

          <Panel title="Triggered feed" bodyClassName="p-0">
            <ul className="divide-y divide-border" aria-live="polite">
              {triggered.map((a) => (
                <li key={a.id} className="flex items-center gap-3 px-5 py-3.5 text-sm">
                  <span className="grid size-7 shrink-0 place-items-center rounded-full bg-lime text-lime-ink" aria-hidden>
                    <BellRing className="size-3.5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="font-medium">{a.symbol}</span>{" "}
                    <span className="text-muted-foreground">
                      {a.condition === "ABOVE" ? "rose above" : "fell below"}
                    </span>{" "}
                    <span className="font-num">{money(a.targetPrice)}</span>
                  </span>
                  <span className="font-num text-xs text-muted-foreground">{dateTime(a.triggeredAt!)}</span>
                  <button
                    type="button"
                    onClick={() => actions.deleteAlert(a.id)}
                    aria-label={`Dismiss ${a.symbol} notification`}
                    className="press grid size-9 min-h-9 place-items-center rounded-lg border border-input hover:bg-secondary"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </li>
              ))}
              {triggered.length === 0 && (
                <li className="px-5 py-10 text-center text-sm text-muted-foreground">
                  Nothing has triggered yet.
                </li>
              )}
            </ul>
          </Panel>
        </div>
      </div>
    </AppShell>
  );
}
