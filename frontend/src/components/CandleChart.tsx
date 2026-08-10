import { useMemo } from "react";

import type { Candle } from "@/lib/market";

/** Lightweight SVG candlestick chart — no ambient animation. */
export function CandleChart({ candles, height = 320 }: { candles: Candle[]; height?: number }) {
  const { hi, lo } = useMemo(() => {
    const highs = candles.map((c) => c.h);
    const lows = candles.map((c) => c.l);
    return { hi: Math.max(...highs), lo: Math.min(...lows) };
  }, [candles]);

  const w = 1000;
  const pad = 12;
  const range = hi - lo || 1;
  const step = w / candles.length;
  const y = (v: number) => pad + ((hi - v) / range) * (height - pad * 2);
  const gridLines = 4;

  return (
    <svg
      viewBox={`0 0 ${w} ${height}`}
      preserveAspectRatio="none"
      className="h-[240px] w-full sm:h-[320px]"
      role="img"
      aria-label="Price candlestick chart"
    >
      {Array.from({ length: gridLines + 1 }, (_, i) => {
        const gy = pad + (i / gridLines) * (height - pad * 2);
        return <line key={i} x1={0} x2={w} y1={gy} y2={gy} className="stroke-border" strokeWidth={1} />;
      })}
      {candles.map((c, i) => {
        const x = i * step + step / 2;
        const up = c.c >= c.o;
        const cls = up ? "fill-gain stroke-gain" : "fill-loss stroke-loss";
        const top = y(Math.max(c.o, c.c));
        const bottom = y(Math.min(c.o, c.c));
        return (
          <g key={c.t} className={cls}>
            <line x1={x} x2={x} y1={y(c.h)} y2={y(c.l)} strokeWidth={1.5} />
            <rect
              x={x - step * 0.3}
              y={top}
              width={step * 0.6}
              height={Math.max(1.5, bottom - top)}
              rx={1}
            />
          </g>
        );
      })}
    </svg>
  );
}

export function Sparkline({ values, positive }: { values: number[]; positive: boolean }) {
  const hi = Math.max(...values);
  const lo = Math.min(...values);
  const range = hi - lo || 1;
  const d = values
    .map((v, i) => `${i === 0 ? "M" : "L"} ${(i / (values.length - 1)) * 100} ${28 - ((v - lo) / range) * 24}`)
    .join(" ");
  return (
    <svg viewBox="0 0 100 30" preserveAspectRatio="none" className="h-7 w-20" aria-hidden>
      <path d={d} fill="none" strokeWidth={2} className={positive ? "stroke-gain" : "stroke-loss"} />
    </svg>
  );
}
