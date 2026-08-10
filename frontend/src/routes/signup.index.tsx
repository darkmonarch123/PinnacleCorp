import { createFileRoute } from "@tanstack/react-router";

import { AuthScreen } from "@/components/AuthScreen";

export const Route = createFileRoute("/signup/")({
  head: () => ({
    meta: [
      { title: "Create an account — Pinnacle" },
      { name: "description", content: "Open a free Pinnacle account with a $10,000 virtual balance." },
      { property: "og:title", content: "Create an account — Pinnacle" },
      { property: "og:description", content: "Open a free Pinnacle account with a $10,000 virtual balance." },
    ],
  }),
  component: () => <AuthScreen mode="sign-up" />,
});
