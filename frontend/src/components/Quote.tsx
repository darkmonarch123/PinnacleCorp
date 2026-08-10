import { TickerLogo } from "@/components/TickerLogo";
import { money, pnlClass, signedPct } from "@/lib/format";
import { INSTRUMENTS } from "@/lib/market";
import { priceOf, useStore, type AppState } from "@/lib/store";
import { cn } from "@/lib/utils";

export function useQuote(symbol: string) {
  const price = useStore((s: AppState) => priceOf(s, symbol));
  const inst = INSTRUMENTS.find((i) => i.symbol === symbol);
  const prev = inst?.prevClose ?? price;
  const change = ((price - prev) / prev) * 100;
  return { price, change, name: inst?.name ?? symbol };
}

export function QuoteChip({
  symbol,
  active,
  onSelect,
}: {
  symbol: string;
  active?: boolean;
  onSelect?: (s: string) => void;
}) {
  const { price, change } = useQuote(symbol);
  return (
    <button
      type="button"
      onClick={() => onSelect?.(symbol)}
      aria-pressed={active}
      className={cn(
        "press flex min-w-[150px] items-center gap-2.5 rounded-xl border px-3 py-2.5 text-left transition-colors",
        active ? "border-transparent bg-lime" : "border-border bg-card hover:bg-secondary",
      )}
    >
      <TickerLogo symbol={symbol} className="size-7" />
      <span className="min-w-0">
        <span className="block truncate text-xs font-semibold">{symbol}</span>
        <span className="font-num block text-xs">{money(price)}</span>
      </span>
      <span className={cn("font-num ml-auto text-xs font-medium", active ? "text-lime-ink" : pnlClass(change))}>
        {signedPct(change)}
      </span>
    </button>
  );
}
