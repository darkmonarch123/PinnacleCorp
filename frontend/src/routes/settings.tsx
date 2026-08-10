import { createFileRoute } from "@tanstack/react-router";
import { AlertTriangle } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Panel } from "@/components/Panel";
import { Switch } from "@/components/ui/switch";
import { clerkEnabled } from "@/lib/auth";
import { actions, useStore } from "@/lib/store";

export const Route = createFileRoute("/settings")({
  head: () => ({
    meta: [
      { title: "Settings — Pinnacle" },
      { name: "description", content: "Profile, notification and security preferences, plus a demo balance reset." },
      { property: "og:title", content: "Settings — Pinnacle" },
      { property: "og:description", content: "Profile, notification and security preferences, plus a demo balance reset." },
    ],
  }),
  component: SettingsPage,
});

function SettingsPage() {
  const profile = useStore((s) => s.profile);
  const notifications = useStore((s) => s.notifications);
  const twoFactor = useStore((s) => s.twoFactor);
  const [confirm, setConfirm] = useState("");

  const field = "mt-1.5 w-full rounded-xl border border-input bg-card px-3.5 py-2.5 text-sm";

  return (
    <AppShell title="Settings">
      <div className="grid max-w-4xl gap-5">
        <Panel title="Profile">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="set-name" className="text-sm font-medium">Full name</label>
              <input
                id="set-name"
                value={profile.fullName}
                onChange={(e) => actions.updateProfile({ fullName: e.target.value })}
                placeholder="Alex Morgan"
                className={field}
              />
            </div>
            <div>
              <label htmlFor="set-currency" className="text-sm font-medium">Base currency</label>
              <select
                id="set-currency"
                value={profile.currency}
                onChange={(e) => actions.updateProfile({ currency: e.target.value })}
                className={field}
              >
                {["USD", "EUR", "GBP"].map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </div>
          </div>
          {clerkEnabled && (
            <p className="mt-4 text-xs text-muted-foreground">
              Email and password are managed in your Clerk account profile.
            </p>
          )}
        </Panel>

        <Panel title="Preferences">
          <div className="space-y-5">
            <div className="flex items-center justify-between gap-6">
              <label htmlFor="set-notifs" className="text-sm">
                <span className="font-medium">Alert notifications</span>
                <span className="block text-xs text-muted-foreground">Notify me when a price alert triggers.</span>
              </label>
              <Switch
                id="set-notifs"
                checked={notifications}
                onCheckedChange={(v) => actions.toggleSetting("notifications", v)}
              />
            </div>
            <div className="flex items-center justify-between gap-6">
              <label htmlFor="set-2fa" className="text-sm">
                <span className="font-medium">Two-factor authentication</span>
                <span className="block text-xs text-muted-foreground">Require a second factor at sign-in.</span>
              </label>
              <Switch id="set-2fa" checked={twoFactor} onCheckedChange={(v) => actions.toggleSetting("twoFactor", v)} />
            </div>
          </div>
        </Panel>

        <Panel title="Reset demo balance">
          <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">
            <p className="flex items-start gap-2 text-sm">
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-loss" aria-hidden />
              <span>
                Resetting restores your virtual balance to $10,000. It <strong>closes every open position</strong>{" "}
                and <strong>cancels all pending orders</strong>. Your trade history is kept.
              </span>
            </p>
          </div>
          <label htmlFor="set-confirm" className="mt-4 block text-sm font-medium">
            Type <span className="font-num">RESET</span> to confirm
          </label>
          <input
            id="set-confirm"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            placeholder="RESET"
            className={`${field} font-num max-w-xs`}
          />
          <button
            type="button"
            disabled={confirm !== "RESET"}
            onClick={() => {
              actions.resetDemoBalance();
              setConfirm("");
              toast.success("Demo balance reset to $10,000");
            }}
            className="press mt-4 rounded-xl bg-ink px-5 py-2.5 text-sm font-semibold text-ink-foreground disabled:cursor-not-allowed disabled:opacity-40"
          >
            Reset demo balance
          </button>
        </Panel>
      </div>
    </AppShell>
  );
}
