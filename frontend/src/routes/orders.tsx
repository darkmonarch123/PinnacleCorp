import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Panel } from "@/components/Panel";
import { dateTime, money } from "@/lib/format";
import type { OrderStatus } from "@/lib/market";
import { actions, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/orders")({
  head: () => ({
    meta: [
      { title: "Orders — Pinnacle" },
      { name: "description", content: "Every simulated order you have placed, with cancellation for pending ones." },
      { property: "og:title", content: "Orders — Pinnacle" },
      { property: "og:description", content: "Every simulated order you have placed, with cancellation for pending ones." },
    ],
  }),
  component: OrdersPage,
});

const STATUS_STYLE: Record<OrderStatus, string> = {
  PENDING: "bg-accent text-accent-foreground",
  FILLED: "bg-lime text-lime-ink",
  CANCELLED: "bg-secondary text-muted-foreground",
  REJECTED: "bg-destructive/10 text-loss",
};

const FILTERS: (OrderStatus | "ALL")[] = ["ALL", "PENDING", "FILLED", "CANCELLED"];

function OrdersPage() {
  const [filter, setFilter] = useState<OrderStatus | "ALL">("ALL");
  const orders = useStore((s) => s.orders);
  const rows = filter === "ALL" ? orders : orders.filter((o) => o.status === filter);

  return (
    <AppShell title="Orders">
      <Panel
        title="Order history"
        bodyClassName="p-0"
        action={
          <div role="group" aria-label="Filter by status" className="flex gap-1 rounded-xl bg-secondary p-1">
            {FILTERS.map((f) => (
              <button
                key={f}
                type="button"
                aria-pressed={filter === f}
                onClick={() => setFilter(f)}
                className={cn(
                  "press rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors",
                  filter === f ? "bg-card shadow-soft" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {f.charAt(0) + f.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        }
      >
        <div className="overflow-x-auto">
          <table className="w-full min-w-[820px] text-sm">
            <thead className="text-left text-xs text-muted-foreground">
              <tr>
                {["Symbol", "Side", "Type", "Qty", "Filled", "Limit", "Status", "Placed", ""].map((h) => (
                  <th key={h} className="px-5 py-2.5 font-medium">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((o) => (
                <tr key={o.id} className="border-t border-border">
                  <td className="px-5 py-3 font-medium">{o.symbol}</td>
                  <td className={cn("px-5 py-3", o.side === "BUY" ? "text-gain" : "text-loss")}>
                    {o.side === "BUY" ? "+ Buy" : "− Sell"}
                  </td>
                  <td className="px-5 py-3">{o.type}</td>
                  <td className="font-num px-5 py-3">{o.quantity}</td>
                  <td className="font-num px-5 py-3">{o.filledQuantity}</td>
                  <td className="font-num px-5 py-3">{o.limitPrice ? money(o.limitPrice) : "—"}</td>
                  <td className="px-5 py-3">
                    <span className={cn("rounded-full px-2.5 py-1 text-xs font-medium", STATUS_STYLE[o.status])}>
                      {o.status.charAt(0) + o.status.slice(1).toLowerCase()}
                    </span>
                  </td>
                  <td className="font-num px-5 py-3 text-muted-foreground">{dateTime(o.createdAt)}</td>
                  <td className="px-5 py-3 text-right">
                    {o.status === "PENDING" ? (
                      <button
                        type="button"
                        onClick={() => {
                          actions.cancelOrder(o.id);
                          toast.success(`Order for ${o.symbol} cancelled`);
                        }}
                        className="press rounded-lg border border-input px-3 py-1.5 text-xs font-medium hover:bg-secondary"
                      >
                        Cancel
                      </button>
                    ) : (
                      <span className="text-xs text-muted-foreground">—</span>
                    )}
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={9} className="px-5 py-10 text-center text-sm text-muted-foreground">
                    No orders match this filter.
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
