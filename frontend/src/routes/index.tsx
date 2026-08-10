import { Link } from "@tanstack/react-router";
import { createFileRoute } from "@tanstack/react-router";
import { ArrowUpRight, Bell, CandlestickChart, ShieldCheck, Wallet } from "lucide-react";

import { Logo } from "@/components/Logo";
import { money, num } from "@/lib/format";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Pinnacle — Elevate Your Wealth | Simulated Stock Trading" },
      {
        name: "description",
        content:
          "Trade real market data with a $10,000 virtual balance. Pinnacle is a simulated trading platform — full order management, positions and alerts, zero real-world risk.",
      },
      { property: "og:title", content: "Pinnacle — Elevate Your Wealth" },
      {
        property: "og:description",
        content:
          "A simulated stock trading platform with live market data, full order management and zero real-world risk.",
      },
    ],
  }),
  component: Landing,
});

const PARTNERS = ["VISA", "MERIDIAN", "PLAIDLIKE", "NORTHGATE", "DELTASTREAM"];

const FEATURES = [
  {
    icon: CandlestickChart,
    title: "Live market data",
    body: "Candlestick charts across five timeframes, streaming quotes and a watchlist that updates as you work.",
  },
  {
    icon: Wallet,
    title: "Full order management",
    body: "Market and limit orders, quantity steppers, stop-loss and take-profit — with a live cost estimate before you submit.",
  },
  {
    icon: ShieldCheck,
    title: "Zero real-world risk",
    body: "Every dollar is virtual. Practise position sizing and discipline without a brokerage account or real capital.",
  },
  {
    icon: Bell,
    title: "Smart alerts",
    body: "Set price alerts above or below any level and get a live feed the moment a condition is met.",
  },
];

const STEPS = [
  { title: "Create your account", body: "Sign up and clear a short simulated identity step." },
  { title: "Get funded", body: "Start with a $10,000 virtual balance, instantly available." },
  { title: "Trade real market data", body: "Place market and limit orders across major tickers." },
  { title: "Track performance", body: "Win rate, drawdown, equity curve and a full trade log." },
];

function HeroCard() {
  return (
    <div className="relative">
      <div
        aria-hidden
        className="absolute -inset-6 rounded-[2.5rem] bg-lime/35 blur-3xl"
      />
      <div className="bg-ink text-ink-foreground relative rotate-[-2.5deg] rounded-3xl p-6 shadow-lift transition-transform duration-300 hover:rotate-0">
        <div className="text-ink-muted flex items-center justify-between text-xs">
          <span>Portfolio equity</span>
          <span className="rounded-full bg-lime px-2 py-0.5 text-[0.65rem] font-semibold text-lime-ink">
            + {num(3.42)}% today
          </span>
        </div>
        <div className="font-num mt-2 text-4xl font-medium">{money(12_486.19)}</div>
        <div className="text-ink-muted mt-1 text-xs">Virtual funds · simulated account</div>

        <svg viewBox="0 0 300 90" className="mt-6 w-full" aria-hidden preserveAspectRatio="none">
          {[18, 46, 74].map((y) => (
            <line key={y} x1="0" x2="300" y1={y} y2={y} className="stroke-ink-border" strokeWidth="1" />
          ))}
          <path
            d="M0 76 L30 68 L60 72 L90 54 L120 58 L150 40 L180 46 L210 30 L240 34 L270 16 L300 10"
            fill="none"
            className="stroke-lime"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        <div className="border-ink-border mt-5 grid grid-cols-3 gap-3 border-t pt-4">
          {[
            ["AAPL", "+1.24%"],
            ["NVDA", "+3.81%"],
            ["TSLA", "−1.77%"],
          ].map(([sym, chg]) => (
            <div key={sym}>
              <div className="text-ink-muted text-[0.65rem] tracking-wide">{sym}</div>
              <div className="font-num text-sm">{chg}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="absolute -bottom-6 -left-4 hidden rounded-2xl border border-border bg-card px-4 py-3 shadow-lift sm:block">
        <div className="text-[0.65rem] tracking-wide text-muted-foreground uppercase">Buying power</div>
        <div className="font-num text-lg font-medium">{money(6_842.35)}</div>
      </div>
    </div>
  );
}

function Landing() {
  return (
    <div className="min-h-dvh">
      <header className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-6">
        <Logo />
        <nav className="flex items-center gap-2" aria-label="Account">
          <Link
            to="/login"
            className="press rounded-full px-4 py-2 text-sm font-medium hover:bg-secondary"
          >
            Log in
          </Link>
          <Link
            to="/signup"
            className="press rounded-full bg-lime px-4 py-2 text-sm font-semibold text-lime-ink shadow-soft hover:brightness-95"
          >
            Get started
          </Link>
        </nav>
      </header>

      <main>
        <section className="mx-auto grid max-w-6xl items-center gap-14 px-5 pt-10 pb-20 lg:grid-cols-2 lg:pt-16">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs font-medium text-muted-foreground">
              <span className="size-1.5 rounded-full bg-lime" aria-hidden /> Simulated trading · virtual funds
            </span>
            <h1 className="mt-5 font-display text-5xl leading-[1.05] tracking-tight sm:text-6xl">
              Elevate Your <span className="italic">Wealth</span>
            </h1>
            <p className="mt-5 max-w-lg text-[1.02rem] leading-relaxed text-muted-foreground">
              Pinnacle is a paper-trading platform for people who want the reps without the risk. Real
              market data, a complete order desk, and a $10,000 virtual balance from the moment you sign
              up.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link
                to="/signup"
                className="press inline-flex items-center gap-2 rounded-full bg-lime px-6 py-3 text-sm font-semibold text-lime-ink shadow-soft hover:brightness-95"
              >
                Open a free account <ArrowUpRight className="size-4" />
              </Link>
              <Link
                to="/dashboard"
                className="press inline-flex items-center gap-2 rounded-full border border-foreground/15 px-6 py-3 text-sm font-medium hover:bg-secondary"
              >
                Explore the dashboard
              </Link>
            </div>
          </div>
          <HeroCard />
        </section>

        <section aria-label="Data and payment networks" className="border-y border-border bg-surface">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-center gap-x-12 gap-y-4 px-5 py-8">
            <span className="text-[0.68rem] tracking-widest text-muted-foreground uppercase">
              Built on standard rails
            </span>
            {PARTNERS.map((p) => (
              <span
                key={p}
                className="font-display text-lg tracking-[0.18em] text-muted-foreground/60 uppercase"
              >
                {p}
              </span>
            ))}
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-5 py-20">
          <h2 className="max-w-2xl font-display text-3xl leading-tight sm:text-4xl">
            Everything a real desk gives you — <span className="italic">minus the risk</span>
          </h2>
          <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {FEATURES.map(({ icon: Icon, title, body }) => (
              <article key={title} className="card-lift rounded-2xl border border-border bg-card p-6 shadow-soft">
                <span className="grid size-10 place-items-center rounded-xl bg-lime text-lime-ink" aria-hidden>
                  <Icon className="size-5" />
                </span>
                <h3 className="mt-4 text-base font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{body}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="bg-ink text-ink-foreground">
          <div className="mx-auto max-w-6xl px-5 py-20">
            <h2 className="font-display text-3xl sm:text-4xl">How it works</h2>
            <ol className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {STEPS.map((s, i) => (
                <li key={s.title} className="border-ink-border border-t pt-5">
                  <span className="font-num text-lime text-sm">0{i + 1}</span>
                  <h3 className="mt-2 text-lg font-semibold">{s.title}</h3>
                  <p className="text-ink-muted mt-2 text-sm leading-relaxed">{s.body}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-5 py-20">
          <div className="rounded-3xl bg-lime px-8 py-14 text-center shadow-soft">
            <h2 className="font-display text-3xl text-lime-ink sm:text-4xl">Start trading in minutes</h2>
            <p className="mx-auto mt-3 max-w-md text-sm text-lime-ink/75">
              No funding, no paperwork, no risk. Just the market and your decisions.
            </p>
            <Link
              to="/signup"
              className="press mt-7 inline-flex items-center gap-2 rounded-full bg-ink px-6 py-3 text-sm font-semibold text-ink-foreground hover:brightness-110"
            >
              Create your account <ArrowUpRight className="size-4" />
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-6xl flex-col gap-6 px-5 py-10 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <Logo />
            <p className="mt-3 text-sm text-muted-foreground">Elevate Your Wealth</p>
          </div>
          <p className="max-w-md text-xs leading-relaxed text-muted-foreground">
            Pinnacle is a simulated (paper) trading platform for education and practice. All balances,
            positions and orders are virtual. No real money is held, transferred or invested, no
            securities are bought or sold, and Pinnacle is not a broker-dealer, investment adviser or
            licensed financial institution. Nothing here is financial advice. Company marks shown are
            illustrative placeholders and do not imply any partnership.
          </p>
        </div>
      </footer>
    </div>
  );
}
