import { createFileRoute } from "@tanstack/react-router";

import { AuthScreen } from "@/components/AuthScreen";

export const Route = createFileRoute("/login/")({
  head: () => ({
    meta: [
      { title: "Log in — Pinnacle" },
      { name: "description", content: "Log in to your Pinnacle simulated trading account." },
      { property: "og:title", content: "Log in — Pinnacle" },
      { property: "og:description", content: "Log in to your Pinnacle simulated trading account." },
    ],
  }),
  component: () => <AuthScreen mode="sign-in" />,
});
