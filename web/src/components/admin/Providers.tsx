"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { ToastProvider } from "./Toast";

/**
 * TanStack Query keeps server data in a cache shared by all admin pages: each query has a key
 * (e.g. ["booking", id]); after a change we "invalidate" keys and the affected screens refetch.
 */
export function Providers({ children }: { children: ReactNode }) {
  // useState: one QueryClient per browser tab, created once (not on every render).
  const [client] = useState(
    () => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: true } } }),
  );
  return (
    <QueryClientProvider client={client}>
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
}
