import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { ShieldCheck } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Logo } from "@/components/Logo";
import { actions } from "@/lib/store";

export const Route = createFileRoute("/kyc")({
  head: () => ({
    meta: [
      { title: "Identity check — Pinnacle" },
      { name: "description", content: "A short simulated identity step before your Pinnacle demo account opens." },
      { property: "og:title", content: "Identity check — Pinnacle" },
      { property: "og:description", content: "A short simulated identity step before your Pinnacle demo account opens." },
    ],
  }),
  component: Kyc,
});

const COUNTRIES = ["United States", "United Kingdom", "Canada", "Germany", "India", "Australia", "Singapore"];

function Kyc() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [dob, setDob] = useState("");
  const [country, setCountry] = useState(COUNTRIES[0]!);

  const field = "mt-1.5 w-full rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm";

  return (
    <div className="route-enter flex min-h-dvh flex-col items-center justify-center gap-6 px-5 py-14">
      <Logo />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          actions.completeKyc(fullName.trim());
          toast.success("Simulated verification complete");
          navigate({ to: "/dashboard" });
        }}
        className="w-full max-w-md rounded-2xl border border-border bg-card p-7 shadow-soft"
      >
        <span className="inline-flex items-center gap-2 rounded-full bg-accent px-3 py-1 text-xs font-medium text-accent-foreground">
          <ShieldCheck className="size-3.5" aria-hidden /> Mock verification
        </span>
        <h1 className="mt-4 font-display text-2xl">Confirm your details</h1>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          This is a simulated identity step for a demo account. No documents are requested, nothing is
          verified against real records, and no data leaves your practice account.
        </p>

        <div className="mt-6 space-y-4">
          <div>
            <label htmlFor="kyc-name" className="text-sm font-medium">Full name</label>
            <input
              id="kyc-name"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Alex Morgan"
              className={field}
            />
          </div>
          <div>
            <label htmlFor="kyc-dob" className="text-sm font-medium">Date of birth</label>
            <input
              id="kyc-dob"
              type="date"
              required
              value={dob}
              onChange={(e) => setDob(e.target.value)}
              className={`${field} font-num`}
            />
          </div>
          <div>
            <label htmlFor="kyc-country" className="text-sm font-medium">Country of residence</label>
            <select
              id="kyc-country"
              value={country}
              onChange={(e) => setCountry(e.target.value)}
              className={field}
            >
              {COUNTRIES.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>

        <button
          type="submit"
          className="press mt-7 w-full rounded-xl bg-lime px-4 py-3 text-sm font-semibold text-lime-ink hover:brightness-95"
        >
          Complete verification
        </button>
      </form>
    </div>
  );
}
