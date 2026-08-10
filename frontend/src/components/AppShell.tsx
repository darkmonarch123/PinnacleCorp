import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import {
  Bell,
  ClipboardList,
  History,
  LayoutDashboard,
  LogOut,
  PieChart,
  Settings,
  Star,
  type LucideIcon,
} from "lucide-react";
import { useEffect, type ReactNode } from "react";

import { Logo } from "@/components/Logo";
import { useSession } from "@/lib/auth";
import { money, pnlClass, signedMoney } from "@/lib/format";
import { accountSummary, actions, useStore } from "@/lib/store";
import { cn } from "@/lib/utils";

const NAV: { to: string; label: string; icon: LucideIcon }[] = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/watchlist", label: "Watchlist", icon: Star },
  { to: "/orders", label: "Orders", icon: ClipboardList },
  { to: "/portfolio", label: "Portfolio", icon: PieChart },
  { to: "/history", label: "History", icon: History },
  { to: "/alerts", label: "Alerts", icon: Bell },
  { to: "/settings", label: "Settings", icon: Settings },
];

function AccountBar() {
  const summary = useStore(accountSummary);
  const currency = useStore((s) => s.profile.currency);

  const items = [
    { label: "Balance", value: money(summary.balance, currency), tone: "" },
    { label: "Equity", value: money(summary.equity, currency), tone: "" },
    { label: "Buying power", value: money(summary.buyingPower, currency), tone: "" },
    { label: "Day P&L", value: signedMoney(summary.dayPnl, currency), tone: pnlClass(summary.dayPnl) },
    { label: "Total P&L", value: signedMoney(summary.totalPnl, currency), tone: pnlClass(summary.totalPnl) },
  ];

  return (
    <div className="grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-3 lg:flex lg:items-center lg:gap-8">
      {items.map((i) => (
        <div key={i.label} className="min-w-0">
          <div className="text-[0.68rem] font-medium tracking-wide text-muted-foreground uppercase">
            {i.label}
          </div>
          <div className={cn("font-num truncate text-[0.95rem] font-medium", i.tone)}>{i.value}</div>
        </div>
      ))}
    </div>
  );
}

export function AppShell({ title, children }: { title: string; children: ReactNode }) {
  const session = useSession();
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  useEffect(() => {
    const id = window.setInterval(() => actions.tickPrices(), 3000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    if (session.isLoaded && !session.isSignedIn) navigate({ to: "/login", replace: true });
  }, [session.isLoaded, session.isSignedIn, navigate]);

  return (
    <div className="min-h-dvh bg-background">
      {/* Desktop icon rail */}
      <nav
        aria-label="Primary"
        className="bg-ink fixed inset-y-0 left-0 z-30 hidden w-[76px] flex-col items-center gap-1 py-5 md:flex"
      >
        <Link to="/dashboard" aria-label="Pinnacle home" className="mb-4">
          <span className="grid size-9 place-items-center rounded-xl bg-lime">
            <svg viewBox="0 0 24 24" className="text-lime-ink size-4" fill="none" stroke="currentColor" strokeWidth="2.4">
              <path d="M3 19 L9.5 11 L14 15 L21 5" strokeLinecap="square" />
            </svg>
          </span>
        </Link>
        {NAV.map(({ to, label, icon: Icon }) => {
          const active = pathname === to;
          return (
            <Link
              key={to}
              to={to}
              aria-label={label}
              aria-current={active ? "page" : undefined}
              title={label}
              className={cn(
                "press grid size-11 place-items-center rounded-xl transition-colors",
                active ? "text-lime-ink bg-lime" : "text-ink-muted hover:bg-ink-border/60 hover:text-ink-foreground",
              )}
            >
              <Icon className="size-[18px]" />
            </Link>
          );
        })}
        <button
          type="button"
          onClick={() => {
            session.signOut();
            navigate({ to: "/" });
          }}
          aria-label="Sign out"
          title="Sign out"
          className="text-ink-muted hover:bg-ink-border/60 hover:text-ink-foreground press mt-auto grid size-11 place-items-center rounded-xl transition-colors"
        >
          <LogOut className="size-[18px]" />
        </button>
      </nav>

      <div className="md:pl-[76px]">
        <header className="bg-background/90 sticky top-0 z-20 border-b border-border backdrop-blur">
          <div className="flex flex-col gap-4 px-4 py-3.5 lg:flex-row lg:items-center lg:justify-between lg:px-8">
            <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3">
              <div className="min-w-0">
                <Logo className="md:hidden" />
                <h1 className="hidden truncate font-display text-xl md:block">{title}</h1>
              </div>
            </div>
            <AccountBar />
          </div>
        </header>

        <main className="route-enter px-4 pt-5 pb-28 md:px-8 md:pb-12">{children}</main>
      </div>

      {/* Mobile bottom nav */}
      <nav
        aria-label="Primary"
        className="bg-ink fixed inset-x-0 bottom-0 z-30 flex items-center justify-between gap-1 px-2 py-2 md:hidden"
      >
        {NAV.map(({ to, label, icon: Icon }) => {
          const active = pathname === to;
          return (
            <Link
              key={to}
              to={to}
              aria-label={label}
              aria-current={active ? "page" : undefined}
              className={cn(
                "grid min-h-11 min-w-11 flex-1 place-items-center rounded-lg",
                active ? "text-lime-ink bg-lime" : "text-ink-muted",
              )}
            >
              <Icon className="size-[18px]" />
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
