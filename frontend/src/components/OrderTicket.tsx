import { Minus, Plus } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { TickerLogo } from "@/components/TickerLogo";
import { money } from "@/lib/format";
import type { OrderType, Side } from "@/lib/market";
import { accountSummary, actions, priceOf, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

const field =
  "mt-1.5 w-full rounded-xl border border-input bg-card px-3 py-2 text-sm font-num focus-visible:border-ring";

export function OrderTicket({ symbol }: { symbol: string }) {
  const price = useStore((s) => priceOf(s, symbol));
  const buyingPower = useStore((s) => accountSummary(s).buyingPower);
  const [side, setSide] = useState<Side>("BUY");
  const [type, setType] = useState<OrderType>("MARKET");
  const [quantity, setQuantity] = useState(5);
  const [limitPrice, setLimitPrice] = useState("");
  const [stopLoss, setStopLoss] = useState("");
  const [takeProfit, setTakeProfit] = useState("");

  const effective = type === "LIMIT" && limitPrice ? Number(limitPrice) : price;
  const estimate = effective * quantity;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const res = actions.placeOrder({
          symbol,
          side,
          type,
          quantity,
          limitPrice: limitPrice ? Number(limitPrice) : null,
          stopLoss: stopLoss ? Number(stopLoss) : null,
          takeProfit: takeProfit ? Number(takeProfit) : null,
        });
        if (res.ok) toast.success(res.message);
        else toast.error(res.message);
      }}
      className="rounded-2xl border border-border bg-card p-5 shadow-soft"
    >
      <div className="flex items-center gap-2.5">
        <TickerLogo symbol={symbol} />
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold">{symbol}</div>
          <div className="font-num text-xs text-muted-foreground">{money(price)}</div>
        </div>
      </div>

      <div role="group" aria-label="Order side" className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-secondary p-1">
        {(["BUY", "SELL"] as Side[]).map((s) => (
          <button
            key={s}
            type="button"
            aria-pressed={side === s}
            onClick={() => setSide(s)}
            className={cn(
              "press rounded-lg py-2 text-sm font-semibold transition-colors",
              side === s
                ? s === "BUY"
                  ? "bg-lime text-lime-ink"
                  : "bg-ink text-ink-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
          >
            {s === "BUY" ? "Buy" : "Sell"}
          </button>
        ))}
      </div>

      <div role="group" aria-label="Order type" className="mt-3 grid grid-cols-2 gap-1 rounded-xl bg-secondary p-1">
        {(["MARKET", "LIMIT"] as OrderType[]).map((t) => (
          <button
            key={t}
            type="button"
            aria-pressed={type === t}
            onClick={() => setType(t)}
            className={cn(
              "press rounded-lg py-1.5 text-xs font-medium transition-colors",
              type === t ? "bg-card shadow-soft" : "text-muted-foreground hover:text-foreground",
            )}
          >
            {t === "MARKET" ? "Market" : "Limit"}
          </button>
        ))}
      </div>

      <div className="mt-4">
        <label htmlFor="ticket-qty" className="text-xs font-medium text-muted-foreground">
          Quantity
        </label>
        <div className="mt-1.5 flex items-center gap-2">
          <button
            type="button"
            aria-label="Decrease quantity"
            onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            className="press grid size-9 shrink-0 place-items-center rounded-lg border border-input hover:bg-secondary"
          >
            <Minus className="size-4" />
          </button>
          <input
            id="ticket-qty"
            type="number"
            min={1}
            value={quantity}
            onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
            className="font-num w-full rounded-xl border border-input bg-card px-3 py-2 text-center text-sm"
          />
          <button
            type="button"
            aria-label="Increase quantity"
            onClick={() => setQuantity((q) => q + 1)}
            className="press grid size-9 shrink-0 place-items-center rounded-lg border border-input hover:bg-secondary"
          >
            <Plus className="size-4" />
          </button>
        </div>
      </div>

      {type === "LIMIT" && (
        <div className="mt-3">
          <label htmlFor="ticket-limit" className="text-xs font-medium text-muted-foreground">
            Limit price
          </label>
          <input
            id="ticket-limit"
            type="number"
            step="0.01"
            required
            value={limitPrice}
            onChange={(e) => setLimitPrice(e.target.value)}
            placeholder={price.toFixed(2)}
            className={field}
          />
        </div>
      )}

      <div className="mt-3 grid grid-cols-2 gap-3">
        <div>
          <label htmlFor="ticket-sl" className="text-xs font-medium text-muted-foreground">
            Stop loss
          </label>
          <input
            id="ticket-sl"
            type="number"
            step="0.01"
            value={stopLoss}
            onChange={(e) => setStopLoss(e.target.value)}
            placeholder="—"
            className={field}
          />
        </div>
        <div>
          <label htmlFor="ticket-tp" className="text-xs font-medium text-muted-foreground">
            Take profit
          </label>
          <input
            id="ticket-tp"
            type="number"
            step="0.01"
            value={takeProfit}
            onChange={(e) => setTakeProfit(e.target.value)}
            placeholder="—"
            className={field}
          />
        </div>
      </div>

      <dl className="mt-5 space-y-1.5 border-t border-border pt-4 text-sm">
        <div className="flex justify-between">
          <dt className="text-muted-foreground">Estimated cost</dt>
          <dd className="font-num font-medium">{money(estimate)}</dd>
        </div>
        <div className="flex justify-between">
          <dt className="text-muted-foreground">Buying power after</dt>
          <dd className="font-num">{money(side === "BUY" ? buyingPower - estimate : buyingPower + estimate)}</dd>
        </div>
      </dl>

      <button
        type="submit"
        className={cn(
          "press mt-4 w-full rounded-xl px-4 py-3 text-sm font-semibold",
          side === "BUY" ? "bg-lime text-lime-ink hover:brightness-95" : "bg-ink text-ink-foreground hover:brightness-110",
        )}
      >
        {side === "BUY" ? "Buy" : "Sell"} {quantity} {symbol}
      </button>
      <p className="mt-3 text-center text-[0.7rem] text-muted-foreground">
        Simulated order · virtual funds only
      </p>
    </form>
  );
}
