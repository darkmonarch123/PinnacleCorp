import { useAuth as useClerkAuth, useClerk } from "@clerk/clerk-react";
import { useEffect, useState } from "react";

/**
 * Clerk-backed auth.
 *
 * When `VITE_CLERK_PUBLISHABLE_KEY` is present the real Clerk provider and
 * <SignIn>/<SignUp> components are used. Without a key (preview before the key
 * is added) a local demo session keeps every route reachable instead of
 * crashing the app.
 *
 * NOTE: the "Development mode" watermark Clerk renders comes from the Clerk
 * instance configuration in the Clerk dashboard — the instance must be switched
 * to a production configuration there; it cannot be removed from app code.
 */
export const CLERK_KEY: string | undefined = import.meta.env["VITE_CLERK_PUBLISHABLE_KEY"];
export const clerkEnabled = Boolean(CLERK_KEY);

const DEMO_KEY = "pinnacle.demo-session";

export interface Session {
  isLoaded: boolean;
  isSignedIn: boolean;
  signIn: () => void;
  signOut: () => void;
}

function useDemoSession(): Session {
  const [signedIn, setSignedIn] = useState<boolean | null>(null);

  useEffect(() => {
    setSignedIn(window.localStorage.getItem(DEMO_KEY) === "1");
  }, []);

  return {
    isLoaded: signedIn !== null,
    isSignedIn: Boolean(signedIn),
    signIn: () => {
      window.localStorage.setItem(DEMO_KEY, "1");
      setSignedIn(true);
    },
    signOut: () => {
      window.localStorage.removeItem(DEMO_KEY);
      setSignedIn(false);
    },
  };
}

function useRealSession(): Session {
  const { isLoaded, isSignedIn } = useClerkAuth();
  const clerk = useClerk();
  return {
    isLoaded,
    isSignedIn: Boolean(isSignedIn),
    signIn: () => clerk.redirectToSignIn(),
    signOut: () => void clerk.signOut(),
  };
}

/** Selected once at module load — never switches between renders. */
export const useSession: () => Session = clerkEnabled ? useRealSession : useDemoSession;

export const clerkAppearance = {
  variables: {
    colorPrimary: "#111111",
    colorText: "#111111",
    colorBackground: "#ffffff",
    borderRadius: "0.75rem",
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif",
  },
  elements: {
    card: "rounded-2xl border border-border shadow-soft",
    headerTitle: "font-display",
    formButtonPrimary: "bg-lime text-lime-ink hover:brightness-95 normal-case font-semibold",
    footerActionLink: "text-foreground underline",
  },
} as const;
