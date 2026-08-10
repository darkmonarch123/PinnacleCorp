import { createFileRoute } from "@tanstack/react-router";
import { Check, Plus, Trash2 } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { Panel } from "@/components/Panel";
import { useQuote } from "@/components/Quote";
import { TickerLogo } from "@/components/TickerLogo";
import { money, pnlClass, signedPct } from "@/lib/format";
import { INSTRUMENTS } from "@/lib/market";
import { actions, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/watchlist")({
  head: () => ({
    meta: [
      { title: "Watchlist — Pinnacle" },
      { name: "description", content: "Track live prices and daily change for the symbols you follow." },
      { property: "og:title", content: "Watchlist — Pinnacle" },
      { property: "og:description", content: "Track live prices and daily change for the symbols you follow." },
    ],
  }),
  component: WatchlistPage,
});

function Row({ symbol, tracked }: { symbol: string; tracked: boolean }) {
  const { price, change, name } = useQuote(symbol);
  return (
    <li className="flex items-center gap-3 border-t border-border px-5 py-3.5 first:border-t-0">
      <TickerLogo symbol={symbol} />
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium">{symbol}</div>
        <div className="truncate text-xs text-muted-foreground">{name}</div>
      </div>
      <div className="text-right">
        <div className="font-num text-sm">{money(price)}</div>
        <div className={cn("font-num text-xs", pnlClass(change))}>{signedPct(change)}</div>
      </div>
      <button
        type="button"
        onClick={() => actions.toggleWatch(symbol)}
        aria-label={tracked ? `Remove ${symbol} from watchlist` : `Add ${symbol} to watchlist`}
        className={cn(
          "press grid size-9 min-h-9 place-items-center rounded-lg border transition-colors",
          tracked ? "border-transparent bg-secondary hover:bg-destructive/10" : "border-input hover:bg-secondary",
        )}
      >
        {tracked ? <Trash2 className="size-4" /> : <Plus className="size-4" />}
      </button>
    </li>
  );
}

function WatchlistPage() {
  const watchlist = useStore((s) => s.watchlist);
  const others = INSTRUMENTS.filter((i) => !watchlist.includes(i.symbol));

  return (
    <AppShell title="Watchlist">
      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title={`Tracked (${watchlist.length})`} bodyClassName="p-0">
          <ul>
            {watchlist.map((s) => (
              <Row key={s} symbol={s} tracked />
            ))}
            {watchlist.length === 0 && (
              <li className="px-5 py-10 text-center text-sm text-muted-foreground">
                Your watchlist is empty — add a symbol from the right.
              </li>
            )}
          </ul>
        </Panel>

        <Panel title="Add symbols" bodyClassName="p-0">
          <ul>
            {others.map((i) => (
              <Row key={i.symbol} symbol={i.symbol} tracked={false} />
            ))}
            {others.length === 0 && (
              <li className="flex items-center justify-center gap-2 px-5 py-10 text-center text-sm text-muted-foreground">
                <Check className="size-4" /> Every available symbol is on your watchlist.
              </li>
            )}
          </ul>
        </Panel>
      </div>
    </AppShell>
  );
}
