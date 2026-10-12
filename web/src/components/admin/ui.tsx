"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { LoaderCircle } from "lucide-react";
import type { BookingStatus, SyncStatus } from "@/lib/admin-api";
import { errorMessage } from "@/lib/admin-api";

export function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
      <h1 className="font-serif text-3xl font-semibold">{title}</h1>
      {children}
    </div>
  );
}

export function Card({ title, children, className = "" }: { title?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-2xl bg-white p-5 shadow-sm ${className}`}>
      {title && <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">{title}</h2>}
      {children}
    </section>
  );
}

export function Loading() {
  return (
    <p className="flex items-center gap-2 text-muted" role="status">
      <LoaderCircle aria-hidden className="size-4 animate-spin" /> Loading…
    </p>
  );
}

export function ErrorBox({ error }: { error: unknown }) {
  return (
    <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">
      {errorMessage(error)}
    </p>
  );
}

const statusStyles: Record<BookingStatus | SyncStatus, string> = {
  REQUESTED: "bg-amber-100 text-amber-900",
  CONFIRMED: "bg-emerald-100 text-emerald-900",
  DECLINED: "bg-stone-200 text-stone-700",
  CANCELLED: "bg-stone-200 text-stone-700",
  SUCCESS: "bg-emerald-100 text-emerald-900",
  FAILED: "bg-red-100 text-red-900",
};

const statusLabels: Record<BookingStatus | SyncStatus, string> = {
  REQUESTED: "Request",
  CONFIRMED: "Confirmed",
  DECLINED: "Declined",
  CANCELLED: "Cancelled",
  SUCCESS: "Synced",
  FAILED: "Failing",
};

export function StatusBadge({ status }: { status: BookingStatus | SyncStatus }) {
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${statusStyles[status]}`}>
      {statusLabels[status]}
    </span>
  );
}

export const inputClass =
  "w-full rounded-lg border border-line bg-white px-3 py-2 outline-none transition focus:border-terracotta focus:ring-2 focus:ring-terracotta/20";

export function Field({ label, children, hint }: { label: string; children: (id: string) => ReactNode; hint?: string }) {
  const id = useId();
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium">
        {label}
      </label>
      {children(id)}
      {hint && <p className="mt-1 text-xs text-muted">{hint}</p>}
    </div>
  );
}

/** Modal built on the native <dialog>: focus trap, Escape to close and inert background for free. */
export function Dialog({ open, onClose, title, children }: { open: boolean; onClose: () => void; title: string; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      onClose={onClose}
      aria-label={title}
      className="m-auto w-[min(32rem,calc(100vw-2rem))] rounded-2xl bg-white p-0 shadow-xl backdrop:bg-ink/50"
    >
      {open && (
        <div className="p-6">
          <h2 className="mb-4 font-serif text-2xl font-semibold">{title}</h2>
          {children}
        </div>
      )}
    </dialog>
  );
}
