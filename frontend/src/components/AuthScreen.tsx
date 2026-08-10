import { SignIn, SignUp } from "@clerk/clerk-react";
import { Link, useNavigate } from "@tanstack/react-router";

import { Logo } from "@/components/Logo";
import { clerkEnabled, useSession } from "@/lib/auth";

export function AuthScreen({ mode }: { mode: "sign-in" | "sign-up" }) {
  const navigate = useNavigate();
  const session = useSession();
  const isSignIn = mode === "sign-in";

  return (
    <div className="grid min-h-dvh lg:grid-cols-2">
      <aside className="bg-ink text-ink-foreground hidden flex-col justify-between p-12 lg:flex">
        <Link to="/" aria-label="Pinnacle home">
          <Logo mono />
        </Link>
        <div>
          <h2 className="font-display text-4xl leading-tight">
            Elevate Your <span className="italic">Wealth</span>
          </h2>
          <p className="text-ink-muted mt-4 max-w-sm text-sm leading-relaxed">
            A $10,000 virtual balance, live market data and a full order desk. Simulated trading, real
            practice.
          </p>
        </div>
        <p className="text-ink-muted text-xs">Simulated platform · no real money is ever involved.</p>
      </aside>

      <main className="flex flex-col items-center justify-center gap-6 px-5 py-14">
        <Link to="/" className="lg:hidden" aria-label="Pinnacle home">
          <Logo />
        </Link>
        <h1 className="font-display text-3xl">{isSignIn ? "Welcome back" : "Create your account"}</h1>

        {clerkEnabled ? (
          isSignIn ? (
            <SignIn routing="path" path="/login" signUpUrl="/signup" forceRedirectUrl="/dashboard" />
          ) : (
            <SignUp routing="path" path="/signup" signInUrl="/login" forceRedirectUrl="/kyc" />
          )
        ) : (
          <div className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-soft">
            <p className="text-sm text-muted-foreground">
              Clerk isn&apos;t configured yet. Add <code className="font-num">VITE_CLERK_PUBLISHABLE_KEY</code>{" "}
              to render Clerk&apos;s hosted {isSignIn ? "sign-in" : "sign-up"} form here. Until then you can
              continue with a local demo session.
            </p>
            <button
              type="button"
              onClick={() => {
                session.signIn();
                navigate({ to: isSignIn ? "/dashboard" : "/kyc" });
              }}
              className="press mt-5 w-full rounded-xl bg-lime px-4 py-3 text-sm font-semibold text-lime-ink hover:brightness-95"
            >
              {isSignIn ? "Continue to dashboard" : "Continue to verification"}
            </button>
            <p className="mt-4 text-center text-xs text-muted-foreground">
              {isSignIn ? (
                <>
                  No account? <Link to="/signup" className="underline">Sign up</Link>
                </>
              ) : (
                <>
                  Already registered? <Link to="/login" className="underline">Log in</Link>
                </>
              )}
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
