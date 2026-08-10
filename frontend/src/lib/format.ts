const nf = (opts: Intl.NumberFormatOptions) => new Intl.NumberFormat("en-US", opts);

export const money = (n: number, currency = "USD") =>
  nf({ style: "currency", currency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);

export const num = (n: number, d = 2) =>
  nf({ minimumFractionDigits: d, maximumFractionDigits: d }).format(n);

export const signedMoney = (n: number, currency = "USD") =>
  `${n >= 0 ? "+" : "−"}${money(Math.abs(n), currency)}`;

export const signedPct = (n: number) => `${n >= 0 ? "+" : "−"}${num(Math.abs(n))}%`;

export const pnlClass = (n: number) => (n >= 0 ? "text-gain" : "text-loss");

export const dateTime = (ms: number) =>
  new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(ms));

export const dateOnly = (ms: number) =>
  new Intl.DateTimeFormat("en-US", { month: "short", day: "2-digit", year: "numeric" }).format(new Date(ms));
