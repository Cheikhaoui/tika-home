"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, ArrowLeft, Mail, MessageCircle, Phone } from "lucide-react";
import { adminApi, errorMessage, type BlockSummary } from "@/lib/admin-api";
import { ApiError } from "@/lib/api";
import { primaryButton, secondaryButton } from "@/components/ui/buttonStyles";
import { dateTime, money, stay } from "./format";
import { Card, Dialog, ErrorBox, inputClass, Loading, PageHeader, StatusBadge } from "./ui";
import { useToast } from "./Toast";

export function BookingDetailView({ id }: { id: string }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { data: booking, error, isPending } = useQuery({ queryKey: ["booking", id], queryFn: () => adminApi.booking(id) });
  const [dialog, setDialog] = useState<"confirm" | "decline" | null>(null);
  const [declineMessage, setDeclineMessage] = useState("");
  const [conflicts, setConflicts] = useState<BlockSummary[]>([]);

  // After any change, refresh everything that shows bookings (lists, dashboard, calendar).
  const refreshAll = () =>
    Promise.all(["booking", "bookings", "dashboard", "calendar"].map((key) => queryClient.invalidateQueries({ queryKey: [key] })));

  const confirm = useMutation({
    mutationFn: () => adminApi.confirm(id),
    onSuccess: async ({ warnings }) => {
      setDialog(null);
      toast("Booking confirmed. The guest has been emailed.");
      warnings.forEach((w) => toast(`Could not refresh ${w}`, "error"));
      await refreshAll();
    },
    onError: async (err) => {
      if (err instanceof ApiError && err.status === 409) {
        const found = (err.problem as { conflicts?: BlockSummary[] }).conflicts;
        if (found) setConflicts(found);
      }
      await refreshAll();
    },
  });

  const decline = useMutation({
    mutationFn: () => adminApi.decline(id, declineMessage),
    onSuccess: async () => {
      setDialog(null);
      toast("Request declined. The guest has been emailed.");
      await refreshAll();
    },
  });

  if (isPending) return <Loading />;
  if (error) return <ErrorBox error={error} />;

  const guest = booking.guest;
  const blocking = conflicts.length > 0 ? conflicts : booking.conflicts;
  const phoneDigits = guest.phone?.replace(/[^\d]/g, "");

  return (
    <>
      <Link href="/admin/bookings" className="mb-3 inline-flex items-center gap-1 text-sm text-muted hover:text-ink">
        <ArrowLeft aria-hidden className="size-4" /> Bookings
      </Link>
      <PageHeader title={guest.fullName}>
        <StatusBadge status={booking.status} />
      </PageHeader>

      {blocking.length > 0 && booking.status !== "DECLINED" && (
        <div role="alert" className="mb-5 flex gap-3 rounded-2xl border-2 border-danger bg-white p-4">
          <AlertTriangle aria-hidden className="size-5 shrink-0 text-danger" />
          <div>
            <p className="font-medium text-danger">These dates are also blocked:</p>
            <ul className="text-sm">
              {blocking.map((b) => (
                <li key={b.id}>
                  {b.source === "ICAL" ? `${b.channelName} reservation` : `Your block${b.reason ? ` (${b.reason})` : ""}`}:{" "}
                  {stay(b.startDate, b.endDate)}
                </li>
              ))}
            </ul>
            {booking.status === "REQUESTED" && (
              <p className="mt-2 text-sm text-muted">
                Recommended: decline this request politely and suggest other dates (no platform penalty).
              </p>
            )}
          </div>
        </div>
      )}

      <div className="grid gap-5 lg:grid-cols-[3fr_2fr]">
        <Card title="Stay">
          <dl className="grid grid-cols-2 gap-y-3 text-sm">
            <dt className="text-muted">Dates</dt><dd className="font-medium">{stay(booking.checkIn, booking.checkOut)}</dd>
            <dt className="text-muted">Nights</dt><dd className="font-medium">{booking.nights}</dd>
            <dt className="text-muted">Guests</dt><dd className="font-medium">{booking.guests}</dd>
            <dt className="text-muted">Total</dt><dd className="font-medium">{money(booking.totalPrice, booking.currency)}</dd>
            <dt className="text-muted">Reference</dt><dd className="font-medium">{booking.reference}</dd>
            <dt className="text-muted">Requested</dt><dd>{dateTime(booking.createdAt)}</dd>
          </dl>
          {booking.message && (
            <blockquote className="mt-4 rounded-lg bg-sand p-3 text-sm">
              <p className="mb-1 text-xs font-semibold uppercase text-muted">Message from the guest</p>
              {booking.message}
            </blockquote>
          )}
        </Card>

        <Card title="Guest">
          <ul className="space-y-2 text-sm">
            <li className="flex items-center gap-2"><Mail aria-hidden className="size-4 text-muted" />
              <a href={`mailto:${guest.email}?subject=${encodeURIComponent(`Your stay at Tika Home (${booking.reference})`)}`}
                 className="underline">{guest.email}</a></li>
            {guest.phone && (
              <>
                <li className="flex items-center gap-2"><Phone aria-hidden className="size-4 text-muted" />
                  <a href={`tel:${guest.phone}`} className="underline" dir="ltr">{guest.phone}</a></li>
                <li className="flex items-center gap-2"><MessageCircle aria-hidden className="size-4 text-muted" />
                  <a href={`https://wa.me/${phoneDigits}`} target="_blank" rel="noopener noreferrer" className="underline">WhatsApp</a></li>
              </>
            )}
            <li className="text-muted">Speaks: {guest.language.toUpperCase()} (emails are sent in this language)</li>
          </ul>
        </Card>
      </div>

      {booking.status === "REQUESTED" && (
        <div className="mt-6 flex flex-wrap gap-3">
          <button type="button" className={primaryButton} onClick={() => setDialog("confirm")}>Confirm booking</button>
          <button type="button" className={secondaryButton} onClick={() => setDialog("decline")}>Decline</button>
        </div>
      )}

      <Dialog open={dialog === "confirm"} onClose={() => setDialog(null)} title="Confirm this booking?">
        <p className="text-sm text-muted">
          Airbnb and Booking.com calendars are checked again first. The guest gets a confirmation email
          in {guest.language.toUpperCase()}.
        </p>
        {confirm.error && <div className="mt-3"><ErrorBox error={confirm.error} /></div>}
        <div className="mt-5 flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={() => setDialog(null)}>Cancel</button>
          <button type="button" className={primaryButton} disabled={confirm.isPending} onClick={() => confirm.mutate()}>
            {confirm.isPending ? "Checking calendars…" : "Confirm"}
          </button>
        </div>
      </Dialog>

      <Dialog open={dialog === "decline"} onClose={() => setDialog(null)} title="Decline this request?">
        <label htmlFor="decline-message" className="mb-1 block text-sm font-medium">Message to the guest (optional)</label>
        <textarea id="decline-message" rows={4} maxLength={1000} value={declineMessage}
                  onChange={(e) => setDeclineMessage(e.target.value)} className={inputClass}
                  placeholder="e.g. Sorry, the house is taken that week. The week after is free!" />
        <p className="mt-1 text-xs text-muted">Added to the email, which is sent in {guest.language.toUpperCase()}.</p>
        {decline.error && <div className="mt-3"><p role="alert" className="text-sm text-danger">{errorMessage(decline.error)}</p></div>}
        <div className="mt-5 flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={() => setDialog(null)}>Cancel</button>
          <button type="button" className={`${primaryButton} bg-danger hover:bg-danger/90`} disabled={decline.isPending}
                  onClick={() => decline.mutate()}>
            {decline.isPending ? "Declining…" : "Decline request"}
          </button>
        </div>
      </Dialog>
    </>
  );
}
