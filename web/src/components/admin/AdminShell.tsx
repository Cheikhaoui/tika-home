"use client";

import { Suspense, type ReactNode } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { LogOut } from "lucide-react";
import { adminApi } from "@/lib/admin-api";
import { AdminNav, AdminNavLinks } from "./AdminNav";

/** Desktop: sidebar. Phone: top bar + bottom tab bar (the owner will mostly use a phone). */
export function AdminShell({ children }: { children: ReactNode }) {
  const me = useQuery({ queryKey: ["me"], queryFn: adminApi.me });
  const logout = useMutation({
    mutationFn: adminApi.logout,
    // Full reload on purpose (not router.push): it wipes all cached admin data from memory.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    onSettled: () => window.location.assign("/admin/login"),
  });

  return (
    <div className="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside className="hidden border-e border-line bg-white p-4 md:flex md:flex-col">
        <p className="mb-6 px-3 font-serif text-2xl font-semibold text-terracotta-dark">Tika Home</p>
        <nav aria-label="Admin">
          <Suspense fallback={<AdminNavLinks variant="side" />}>
            <AdminNav variant="side" />
          </Suspense>
        </nav>
        <div className="mt-auto border-t border-line px-3 pt-4 text-sm">
          <p className="truncate text-muted">{me.data?.email}</p>
          <button type="button" onClick={() => logout.mutate()} className="mt-2 flex items-center gap-2 hover:text-terracotta">
            <LogOut aria-hidden className="size-4" /> Log out
          </button>
        </div>
      </aside>

      <header className="flex items-center justify-between border-b border-line bg-white px-4 py-3 md:hidden">
        <p className="font-serif text-xl font-semibold text-terracotta-dark">Tika Home</p>
        <button type="button" onClick={() => logout.mutate()} aria-label="Log out" className="rounded-full p-2 hover:bg-sand-deep">
          <LogOut aria-hidden className="size-5" />
        </button>
      </header>

      <main className="px-4 pb-24 pt-6 md:px-8 md:pb-10">{children}</main>

      <nav aria-label="Admin" className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-white md:hidden">
        <Suspense fallback={<AdminNavLinks variant="bottom" />}>
          <AdminNav variant="bottom" />
        </Suspense>
      </nav>
    </div>
  );
}
