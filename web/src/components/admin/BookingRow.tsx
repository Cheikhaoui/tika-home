import Link from "next/link";
import { ChevronRight } from "lucide-react";
import type { BookingSummary } from "@/lib/admin-api";
import { money, stay } from "./format";
import { StatusBadge } from "./ui";

/** One booking in a list: works as a card on phones and a row on desktop. */
export function BookingRow({ booking }: { booking: BookingSummary }) {
  return (
    <li>
      <Link
        href={`/admin/bookings/${booking.id}`}
        className="flex items-center gap-4 rounded-xl px-3 py-3 transition hover:bg-sand"
      >
        <div className="min-w-0 flex-1">
          <p className="flex flex-wrap items-center gap-2 font-medium">
            {booking.guestName} <StatusBadge status={booking.status} />
          </p>
          <p className="text-sm text-muted">
            {stay(booking.checkIn, booking.checkOut)} · {booking.nights} nights · {booking.guests} guests
          </p>
        </div>
        <div className="text-end">
          <p className="font-medium">{money(booking.totalPrice, booking.currency)}</p>
          <p className="text-xs text-muted">{booking.reference}</p>
        </div>
        <ChevronRight aria-hidden className="size-4 shrink-0 text-muted rtl:rotate-180" />
      </Link>
    </li>
  );
}
