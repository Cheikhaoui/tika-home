"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarDays, CircleDollarSign, LayoutDashboard, ListChecks, RefreshCw, type LucideIcon } from "lucide-react";

export const adminLinks: { href: string; label: string; icon: LucideIcon }[] = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/bookings", label: "Bookings", icon: ListChecks },
  { href: "/admin/calendar", label: "Calendar", icon: CalendarDays },
  { href: "/admin/pricing", label: "Pricing", icon: CircleDollarSign },
  { href: "/admin/channels", label: "Channels", icon: RefreshCw },
];

/**
 * usePathname() highlights the current page. With Cache Components it suspends above dynamic routes
 * (/admin/bookings/[id]), so the shell renders this inside <Suspense> with AdminNavLinks as fallback.
 */
export function AdminNav({ variant }: { variant: "side" | "bottom" }) {
  const pathname = usePathname();
  return <AdminNavLinks variant={variant} pathname={pathname} />;
}

export function AdminNavLinks({ variant, pathname }: { variant: "side" | "bottom"; pathname?: string }) {
  const isActive = (href: string) => (href === "/admin" ? pathname === "/admin" : pathname?.startsWith(href));
  if (variant === "bottom") {
    return (
      <ul className="grid grid-cols-5">
        {adminLinks.map(({ href, label, icon: Icon }) => (
          <li key={href}>
            <Link
              href={href}
              aria-current={isActive(href) ? "page" : undefined}
              className="flex flex-col items-center gap-0.5 py-2 text-[11px] text-muted aria-[current=page]:text-terracotta"
            >
              <Icon aria-hidden className="size-5" />
              {label}
            </Link>
          </li>
        ))}
      </ul>
    );
  }
  return (
    <ul className="space-y-1">
      {adminLinks.map(({ href, label, icon: Icon }) => (
        <li key={href}>
          <Link
            href={href}
            aria-current={isActive(href) ? "page" : undefined}
            className="flex items-center gap-3 rounded-lg px-3 py-2 text-muted transition hover:bg-sand-deep hover:text-ink aria-[current=page]:bg-ink aria-[current=page]:text-sand"
          >
            <Icon aria-hidden className="size-5" />
            {label}
          </Link>
        </li>
      ))}
    </ul>
  );
}
