import { useEffect, useState } from "react";

import { instrumentOf } from "@/lib/store";
import { cn } from "@/lib/utils";

/**
 * Ticker mark resolved at runtime from a logo lookup service keyed by company
 * domain. No trademarked image files are bundled; a letter badge is rendered
 * whenever the lookup fails.
 */
export function TickerLogo({ symbol, className }: { symbol: string; className?: string }) {
  const [failed, setFailed] = useState(false);
  const inst = instrumentOf(symbol);

  useEffect(() => setFailed(false), [symbol]);

  const base = cn(
    "grid size-8 shrink-0 place-items-center overflow-hidden rounded-full border border-border bg-secondary",
    className,
  );

  if (failed) {
    return (
      <span className={base} aria-hidden>
        <span className="text-[0.65rem] font-semibold tracking-tight">{symbol.slice(0, 2)}</span>
      </span>
    );
  }

  return (
    <span className={base}>
      <img
        src={`https://logo.clearbit.com/${inst.domain}`}
        alt=""
        loading="lazy"
        className="size-full object-contain p-1"
        onError={() => setFailed(true)}
      />
    </span>
  );
}
