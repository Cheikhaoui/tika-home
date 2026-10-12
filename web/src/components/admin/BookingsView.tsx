"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { adminApi, type BookingView } from "@/lib/admin-api";
import { secondaryButton } from "@/components/ui/buttonStyles";
import { BookingRow } from "./BookingRow";
import { Card, ErrorBox, Loading, PageHeader } from "./ui";

const tabs: { view: BookingView; label: string }[] = [
  { view: "PENDING", label: "Requests" },
  { view: "UPCOMING", label: "Upcoming" },
  { view: "PAST", label: "Past" },
  { view: "CLOSED", label: "Declined" },
];

export function BookingsView() {
  const [view, setView] = useState<BookingView>("PENDING");
  const [page, setPage] = useState(0);
  // The query key includes view + page: each combination is cached separately.
  const { data, error, isPending, isFetching } = useQuery({
    queryKey: ["bookings", view, page],
    queryFn: () => adminApi.bookings(view, page),
    placeholderData: keepPreviousData, // keep showing the previous page while the next one loads
  });

  return (
    <>
      <PageHeader title="Bookings" />
      <div role="tablist" aria-label="Booking lists" className="mb-4 flex gap-1 overflow-x-auto">
        {tabs.map((t) => (
          <button
            key={t.view}
            role="tab"
            type="button"
            aria-selected={view === t.view}
            onClick={() => {
              setView(t.view);
              setPage(0);
            }}
            className="rounded-full px-4 py-1.5 text-sm text-muted aria-selected:bg-ink aria-selected:text-sand"
          >
            {t.label}
          </button>
        ))}
      </div>
      <Card>
        {isPending && <Loading />}
        {error && <ErrorBox error={error} />}
        {data && data.items.length === 0 && <p className="text-muted">No bookings here.</p>}
        {data && data.items.length > 0 && (
          <ul className={`-mx-3 divide-y divide-line ${isFetching ? "opacity-60" : ""}`}>
            {data.items.map((b) => <BookingRow key={b.id} booking={b} />)}
          </ul>
        )}
        {data && data.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <button type="button" className={secondaryButton} disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </button>
            <span className="text-muted">Page {page + 1} of {data.totalPages}</span>
            <button type="button" className={secondaryButton} disabled={page + 1 >= data.totalPages}
                    onClick={() => setPage((p) => p + 1)}>
              Next
            </button>
          </div>
        )}
      </Card>
    </>
  );
}
