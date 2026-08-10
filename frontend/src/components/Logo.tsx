import { cn } from "@/lib/utils";

export function Logo({ className, mono = false }: { className?: string; mono?: boolean }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span
        aria-hidden
        className={cn(
          "grid size-8 shrink-0 place-items-center rounded-[10px]",
          mono ? "bg-ink-foreground/10" : "bg-ink",
        )}
      >
        <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2.2">
          <path d="M3 19 L9.5 11 L14 15 L21 5" className="stroke-lime" strokeLinecap="square" />
        </svg>
      </span>
      <span className="font-display text-[1.15rem] font-semibold tracking-[0.24em] uppercase">
        Pinnacle
      </span>
    </span>
  );
}
