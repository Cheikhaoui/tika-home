"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { adminApi } from "@/lib/admin-api";
import { BookingRow } from "./BookingRow";
import { dateTime, stay } from "./format";
import { Card, ErrorBox, Loading, PageHeader } from "./ui";

export function DashboardView() {
  const { data, error, isPending } = useQuery({ queryKey: ["dashboard"], queryFn: adminApi.dashboard });

  return (
    <>
      <PageHeader title="Dashboard" />
      {isPending && <Loading />}
      {error && <ErrorBox error={error} />}
      {data && (
        <div className="grid gap-5 lg:grid-cols-2">
          {data.conflicts.length > 0 && (
            <Card className="border-2 border-danger lg:col-span-2" title={<span className="text-danger">Double bookings</span>}>
              <ul className="space-y-3">
                {data.conflicts.map(({ booking, platformReservation }) => (
                  <li key={booking.id + platformReservation.id} className="flex gap-3">
                    <AlertTriangle aria-hidden className="mt-0.5 size-5 shrink-0 text-danger" />
                    <p>
                      <Link href={`/admin/bookings/${booking.id}`} className="font-medium underline">
                        {booking.guestName} ({booking.reference})
                      </Link>{" "}
                      overlaps a {platformReservation.channelName} reservation{" "}
                      {stay(platformReservation.startDate, platformReservation.endDate)}. Open the booking to resolve it.
                    </p>
                  </li>
                ))}
              </ul>
            </Card>
          )}

          {data.failingChannels.length > 0 && (
            <Card className="border-2 border-amber-400 lg:col-span-2" title="Calendars not syncing">
              <ul className="space-y-2">
                {data.failingChannels.map((c) => (
                  <li key={c.id} className="flex gap-3">
                    <RefreshCw aria-hidden className="mt-0.5 size-5 shrink-0 text-amber-600" />
                    <p>
                      <strong>{c.name}</strong>: {c.lastError} (last success {dateTime(c.lastSuccessAt)}).{" "}
                      <Link href="/admin/channels" className="underline">Check the channel</Link>
                    </p>
                  </li>
                ))}
              </ul>
            </Card>
          )}

          <Card title={`Requests to answer (${data.pendingCount})`}>
            {data.pending.length === 0 ? (
              <p className="text-muted">Nothing to answer. 🎉</p>
            ) : (
              <ul className="-mx-3">
                {data.pending.map((b) => <BookingRow key={b.id} booking={b} />)}
              </ul>
            )}
            {data.pendingCount > data.pending.length && (
              <Link href="/admin/bookings" className="mt-2 inline-block text-sm underline">See all requests</Link>
            )}
          </Card>

          <Card title="Staying now">
            {data.currentStay ? (
              <ul className="-mx-3"><BookingRow booking={data.currentStay} /></ul>
            ) : (
              <p className="text-muted">The house is free tonight.</p>
            )}
          </Card>

          <Card title="Next arrivals" className="lg:col-span-2">
            {data.nextArrivals.length === 0 ? (
              <p className="text-muted">No confirmed arrivals yet.</p>
            ) : (
              <ul className="-mx-3">
                {data.nextArrivals.map((b) => <BookingRow key={b.id} booking={b} />)}
              </ul>
            )}
          </Card>
        </div>
      )}
    </>
  );
}
