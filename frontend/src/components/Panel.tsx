import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

export function Panel({
  title,
  action,
  className,
  bodyClassName,
  children,
}: {
  title?: string;
  action?: ReactNode;
  className?: string;
  bodyClassName?: string;
  children: ReactNode;
}) {
  return (
    <section className={cn("rounded-2xl border border-border bg-card shadow-soft", className)}>
      {(title || action) && (
        <header className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
          {title && <h2 className="text-sm font-semibold tracking-tight">{title}</h2>}
          {action}
        </header>
      )}
      <div className={cn("p-5", bodyClassName)}>{children}</div>
    </section>
  );
}

export function StatCard({
  label,
  value,
  hint,
  tone,
  highlight,
}: {
  label: string;
  value: string;
  hint?: string;
  tone?: string;
  highlight?: boolean;
}) {
  return (
    <div
      className={cn(
        "card-lift rounded-2xl border p-5 shadow-soft",
        highlight ? "border-transparent bg-lime" : "border-border bg-card",
      )}
    >
      <div
        className={cn(
          "text-[0.7rem] font-medium tracking-wide uppercase",
          highlight ? "text-lime-ink/70" : "text-muted-foreground",
        )}
      >
        {label}
      </div>
      <div className={cn("font-num mt-2 text-2xl font-medium", highlight ? "text-lime-ink" : tone)}>{value}</div>
      {hint && (
        <div className={cn("mt-1 text-xs", highlight ? "text-lime-ink/70" : "text-muted-foreground")}>{hint}</div>
      )}
    </div>
  );
}
