# Pinnacle Trading Lab

# Pinnacle — Master Frontend Rebuild Prompt (for Lovable)

Paste this entire prompt into Lovable to rebuild the frontend in one pass.

---

## Role

You are a senior frontend engineer and product designer rebuilding the frontend for **Pinnacle**, a simulated stock trading platform (paper trading — virtual funds, no real broker, no real money). The existing backend (Spring Boot/Java) already implements everything the UI needs: auth, market data, order management, positions, trade history, watchlists, price alerts, and account settings. **This prompt is frontend-only** — build against the REST/WebSocket API described below, do not invent new backend behavior.

---

## Brand

- **Name**: Pinnacle
- **Logo**: wordmark "PINNACLE" set in an elegant, high-contrast serif with wide letter-spacing — think editorial/luxury branding (similar in spirit to fonts like **Bodoni Moda** or **Playfair Display**; use whichever renders the crisper high-contrast stroke). Pair it with a simple, modern mark (a minimal ascending line/chart glyph or an abstract geometric icon) to its left — not literal, not a mascot.
- **Tagline**: "Elevate Your Wealth"

## Visual direction

Light theme, fintech-editorial, NOT a dark trading-terminal look. Reference direction:

- **Background**: off-white / warm white (`#FAFAF8` or similar), not stark white
- **Primary accent**: a vivid lime/chartreuse green (`#C6FF3D`–`#D4FF4F` range) used boldly on CTAs, highlight cards, and small accent shapes — this is the signature color, use it with confidence, not timidly
- **Text**: near-black (`#111111`) for headings, dark gray for body copy
- **Secondary surface**: a deep near-black card (`#0E1210` or similar) used sparingly for contrast blocks (e.g. a dark dashboard-preview card floating in an otherwise light hero) — light and dark surfaces coexist, dark is the accent not the base
- **Cards**: soft rounded corners (16–20px radius), subtle shadows, generous padding
- **Partner/trust strip**: a horizontal row of muted, grayscale-treated logos (payment networks, data providers, etc. — use a neutral placeholder set like "Visa," "Plaid-style" generic marks; do not fabricate real partnerships)
- **Hero graphic**: a floating card/phone-mockup graphic showing a stat or balance figure, tilted slightly, with soft glow/shadow — this should feel like a real product screenshot, not a flat illustration
- **Numbers/figures**: use a monospace or tabular-figure font for all prices, balances, and P&L so columns align — this is functional, not optional, for a trading UI

## Typography

- **Display/headings**: the elegant serif described above (Bodoni Moda or Playfair Display), used for the logo and large hero headlines only
- **Body/UI**: a clean grotesk sans (Inter, or similar) for everything else — labels, buttons, table content
- **Numeric data**: IBM Plex Mono or JetBrains Mono for every price, quantity, and balance figure

## Motion

- Page-level transitions: a subtle slide/fade when navigating between routes (150–250ms, ease-out) — not jarring, should feel premium and quick, never sluggish
- Micro-interactions: hover states lift cards slightly (subtle scale/shadow increase), buttons have a gentle press-down effect
- Respect `prefers-reduced-motion` — disable/shorten all of the above for users who request it
- No looping decorative animation, no parallax gimmicks — motion should always be a response to user action (navigation, hover, click), never ambient

## Icons & logos

- Use a consistent icon set throughout (Lucide icons or similar outline-style set) for navigation, feature callouts, and UI actions — one icon library, used consistently, not mixed styles
- For stock ticker logos (AAPL, MSFT, TSLA, etc. shown in the watchlist, dashboard, and order ticket): **do not hardcode or bundle real company logo image files**. Instead, fetch them at runtime by a logo-lookup service keyed by company domain (e.g. a pattern like `https://logo.clearbit.com/{domain}.com`, falling back to a plain ticker-letter badge if the fetch fails). This gets the recognizable-logo effect without bundling trademarked assets directly into the app.

## Authentication — Clerk

Auth is handled entirely by **Clerk** (`@clerk/clerk-react` or the Lovable-equivalent Clerk integration) — do not build custom login/signup forms.

- Wrap the app in `ClerkProvider` using the project's publishable key
- Use Clerk's own `<SignIn>` and `<SignUp>` components for the Login and Signup pages, themed via Clerk's `appearance` prop to match the brand (light background, lime-green primary button, serif headings where Clerk allows it)
- **Important**: the Clerk instance currently shows a "Development mode" watermark. This is controlled by the Clerk account/instance configuration, not app code — before final delivery, switch the Clerk instance to a production configuration (or a properly configured instance) so this watermark is gone. Flag this clearly if it's still showing after setup, since it's a dashboard setting, not something fixable purely in this codebase.
- After sign-up completes, redirect to a `/kyc` step (see pages below) before the dashboard
- Protected routes should check Clerk's session state; unauthenticated users are redirected to `/login`

---

## Pages (all 11)

### 1. Landing (`/`)
Public marketing page. Hero with the floating dashboard/balance-card graphic, headline built around "Elevate Your Wealth," the partner-logo trust strip, a feature grid (4 cards: live market data, full order management, zero real-world risk, smart alerts), a "how it works" numbered sequence (create account → get funded with $10,000 virtual balance → trade real market data → track performance), final CTA, and a footer disclaimer that this is a simulated platform with no real money involved.

### 2. Login (`/login/*`)
Clerk `<SignIn>`, themed to match brand. Wildcard path since Clerk needs nested routes for its own steps (MFA, verification, etc.).

### 3. Signup (`/signup/*`)
Clerk `<SignUp>`, themed to match brand. Redirects to `/kyc` on completion.

### 4. KYC (`/kyc`)
A single, short mock-identity-verification step (full name, date of birth, country) — explicitly labeled as mock/simulated, no document upload. On submit, calls the backend and redirects to `/dashboard`.

### 5. Dashboard (`/dashboard`)
The core trading screen. Layout:
- Left icon rail (Dashboard, Watchlist, Orders, Portfolio, History, Alerts, Settings)
- Sticky top account bar: Balance, Equity, Buying Power, Day P&L, Total P&L
- Center: candlestick chart with timeframe selector (1m/5m/1h/1D/1W), watchlist strip below it, and a bottom docked panel with tabs for Open Positions / Pending Orders / Alerts
- Right panel (collapses to bottom on mobile): order ticket — Buy/Sell toggle, Market/Limit selector, quantity stepper, limit price (when applicable), stop-loss/take-profit fields, live cost estimate, submit button

### 6. Watchlist (`/watchlist`)
List of user-tracked symbols with live price + % change, add/remove controls.

### 7. Orders (`/orders`)
Full order history table (symbol, side, type, quantity, filled quantity, limit price, status, timestamp) with a cancel action for still-pending orders.

### 8. Portfolio (`/portfolio`)
Account summary cards (equity, unrealized P&L, buying power), an allocation breakdown by symbol, an open-positions table with inline stop-loss/take-profit editing and a close action, and a recent-trades strip.

### 9. Trade History (`/history`)
Stat cards (win rate, avg win/loss ratio, max drawdown, total realized P&L), an equity curve chart, a filterable (by symbol and win/loss) closed-trades table, and a CSV export button.

### 10. Alerts (`/alerts`)
Create/list/delete price alerts (symbol, target price, above/below condition), plus a live feed of triggered-alert notifications.

### 11. Settings (`/settings`)
Profile (name, base currency), notification and two-factor toggles, and a confirmation-gated "reset demo balance" action (explicitly warns that it closes open positions and cancels pending orders, but keeps trade history).

---

## Accessibility & responsiveness (non-negotiable)

- Visible keyboard focus states on every interactive element
- Color is never the only signal — pair gain/loss colors with a +/− prefix or icon
- Responsive down to mobile: the dashboard's grid degrades to a stacked, scrollable layout (chart → watchlist → order ticket → positions), not a shrunken version of the desktop grid
- Sufficient contrast between the lime accent and both light and dark backgrounds for text use — verify this explicitly, high-chroma lime-on-white or lime-on-black can fail contrast at small sizes

## Explicit constraints

- Simulated platform only — reinforce this in the footer/disclaimers, no implication of real trading, real brokerage, or real financial licensing
- Do not fabricate real partner/integration relationships in the trust-logo strip — use neutral, generic marks
- Do not bundle real trademarked company logo files — use the lookup-by-domain pattern described above

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/81ae6468-e16a-4d88-b8c4-2cec12b28893).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
