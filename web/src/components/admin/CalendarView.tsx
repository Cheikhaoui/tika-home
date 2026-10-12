"use client";

import Link from "next/link";
import { useMemo, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import { adminApi, type BlockSummary, type BookingSummary } from "@/lib/admin-api";
import { ApiError } from "@/lib/api";
import { addDays, fromIsoDate, toIsoDate, type IsoDate } from "@/lib/dates";
import { primaryButton, secondaryButton } from "@/components/ui/buttonStyles";
import { day, stay } from "./format";
import { Card, Dialog, ErrorBox, Field, inputClass, PageHeader } from "./ui";
import { useToast } from "./Toast";

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

type Item =
  | { kind: "booking"; booking: BookingSummary; start: IsoDate; end: IsoDate }
  | { kind: "block"; block: BlockSummary; start: IsoDate; end: IsoDate };

/** Monday on or before the 1st of the month, and the Sunday after its last day: a full 5-6 week grid. */
function monthGrid(month: IsoDate) {
  const first = fromIsoDate(month);
  const start = addDays(month, -((first.getDay() + 6) % 7));
  const last = toIsoDate(new Date(first.getFullYear(), first.getMonth() + 1, 0));
  const end = addDays(last, 6 - ((fromIsoDate(last).getDay() + 6) % 7) + 1); // exclusive
  const days: IsoDate[] = [];
  for (let d = start; d < end; d = addDays(d, 1)) days.push(d);
  return { start, end, days };
}

const itemColor = (item: Item) =>
  item.kind === "booking"
    ? item.booking.status === "CONFIRMED" ? "bg-emerald-600 text-white" : "bg-amber-400 text-ink"
    : item.block.source === "ICAL" ? "bg-sky-600 text-white" : "bg-stone-500 text-white";

const itemLabel = (item: Item) =>
  item.kind === "booking"
    ? item.booking.guestName.split(" ")[0]
    : item.block.source === "ICAL" ? item.block.channelName ?? "Platform" : item.block.reason ?? "Blocked";

export function CalendarView() {
  const today = toIsoDate(new Date());
  const [month, setMonth] = useState(() => `${today.slice(0, 7)}-01`);
  const grid = useMemo(() => monthGrid(month), [month]);
  const { data, error } = useQuery({
    queryKey: ["calendar", grid.start, grid.end],
    queryFn: () => adminApi.calendar(grid.start, grid.end),
  });
  const [blocking, setBlocking] = useState(false);
  const [selectedBlock, setSelectedBlock] = useState<BlockSummary | null>(null);

  const items: Item[] = useMemo(
    () => [
      ...(data?.bookings ?? []).map((b) => ({ kind: "booking" as const, booking: b, start: b.checkIn, end: b.checkOut })),
      ...(data?.blocks ?? []).map((b) => ({ kind: "block" as const, block: b, start: b.startDate, end: b.endDate })),
    ].sort((a, b) => a.start.localeCompare(b.start)),
    [data],
  );
  const itemsOn = (d: IsoDate) => items.filter((i) => i.start <= d && d < i.end); // nights, end exclusive
  const monthLabel = new Intl.DateTimeFormat("en-GB", { month: "long", year: "numeric" }).format(fromIsoDate(month));
  const shiftMonth = (delta: number) => {
    const d = fromIsoDate(month);
    setMonth(toIsoDate(new Date(d.getFullYear(), d.getMonth() + delta, 1)));
  };

  return (
    <>
      <PageHeader title="Calendar">
        <button type="button" className={primaryButton} onClick={() => setBlocking(true)}>
          <Plus aria-hidden className="size-4" /> Block dates
        </button>
      </PageHeader>

      <Card>
        <div className="mb-4 flex items-center justify-between">
          <button type="button" aria-label="Previous month" onClick={() => shiftMonth(-1)} className="rounded-full p-2 hover:bg-sand">
            <ChevronLeft aria-hidden className="size-5" />
          </button>
          <h2 className="font-serif text-2xl font-semibold">{monthLabel}</h2>
          <button type="button" aria-label="Next month" onClick={() => shiftMonth(1)} className="rounded-full p-2 hover:bg-sand">
            <ChevronRight aria-hidden className="size-5" />
          </button>
        </div>
        {error && <ErrorBox error={error} />}
        <div className="grid grid-cols-7 gap-px overflow-hidden rounded-lg border border-line bg-line text-xs">
          {WEEKDAYS.map((w) => (
            <div key={w} className="bg-sand py-1 text-center font-medium text-muted">{w}</div>
          ))}
          {grid.days.map((d) => {
            const inMonth = d.slice(0, 7) === month.slice(0, 7);
            return (
              <div key={d} className={`min-h-16 bg-white p-1 sm:min-h-24 ${inMonth ? "" : "bg-sand/50 text-muted"}`}>
                <p className={`mb-1 text-end ${d === today ? "font-bold text-terracotta" : ""}`}>{Number(d.slice(8))}</p>
                <div className="space-y-0.5">
                  {itemsOn(d).map((item) => (
                    <p
                      key={item.kind + (item.kind === "booking" ? item.booking.id : item.block.id)}
                      className={`truncate rounded px-1 ${itemColor(item)}`}
                      title={`${itemLabel(item)} · ${stay(item.start, item.end)}`}
                    >
                      <span className="hidden sm:inline">{itemLabel(item)}</span>
                      <span className="sm:hidden">&nbsp;</span>
                    </p>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
        <ul className="mt-3 flex flex-wrap gap-4 text-xs text-muted">
          <li className="flex items-center gap-1.5"><span className="size-3 rounded bg-emerald-600" /> Confirmed</li>
          <li className="flex items-center gap-1.5"><span className="size-3 rounded bg-amber-400" /> Request</li>
          <li className="flex items-center gap-1.5"><span className="size-3 rounded bg-sky-600" /> Airbnb / Booking.com</li>
          <li className="flex items-center gap-1.5"><span className="size-3 rounded bg-stone-500" /> Blocked by you</li>
        </ul>
      </Card>

      <Card title="This month" className="mt-5">
        {items.filter((i) => i.end > month && i.start < addDays(month, 31)).length === 0 && (
          <p className="text-muted">Nothing this month.</p>
        )}
        <ul className="divide-y divide-line">
          {items
            .filter((i) => i.end > grid.start && i.start < grid.end)
            .map((item) => (
              <li key={item.kind + (item.kind === "booking" ? item.booking.id : item.block.id)} className="flex items-center gap-3 py-2 text-sm">
                <span className={`size-3 shrink-0 rounded ${itemColor(item)}`} />
                <span className="w-40 shrink-0 text-muted">{stay(item.start, item.end)}</span>
                {item.kind === "booking" ? (
                  <Link href={`/admin/bookings/${item.booking.id}`} className="underline">{item.booking.guestName}</Link>
                ) : item.block.source === "MANUAL" ? (
                  <button type="button" className="underline" onClick={() => setSelectedBlock(item.block)}>
                    {item.block.reason ?? "Blocked"}
                  </button>
                ) : (
                  <span>{item.block.channelName} reservation</span>
                )}
              </li>
            ))}
        </ul>
      </Card>

      <BlockDialog open={blocking} onClose={() => setBlocking(false)} defaultStart={today} />
      <UnblockDialog block={selectedBlock} onClose={() => setSelectedBlock(null)} />
    </>
  );
}

function BlockDialog({ open, onClose, defaultStart }: { open: boolean; onClose: () => void; defaultStart: IsoDate }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [start, setStart] = useState(defaultStart);
  const [end, setEnd] = useState(addDays(defaultStart, 1));
  const [reason, setReason] = useState("");
  const block = useMutation({
    mutationFn: () => adminApi.block(start, end, reason),
    onSuccess: async () => {
      toast("Dates blocked.");
      onClose();
      setReason("");
      await Promise.all(["calendar", "dashboard"].map((k) => queryClient.invalidateQueries({ queryKey: [k] })));
    },
  });
  const conflictingBookings =
    block.error instanceof ApiError ? ((block.error.problem as { bookings?: BookingSummary[] }).bookings ?? []) : [];

  const submit = (e: FormEvent) => {
    e.preventDefault();
    block.mutate();
  };

  return (
    <Dialog open={open} onClose={onClose} title="Block dates">
      <form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label="First night">{(id) => <input id={id} type="date" required value={start} min={defaultStart}
                 onChange={(e) => setStart(e.target.value)} className={inputClass} />}</Field>
          <Field label="Free again on">{(id) => <input id={id} type="date" required value={end} min={addDays(start, 1)}
                 onChange={(e) => setEnd(e.target.value)} className={inputClass} />}</Field>
        </div>
        <Field label="Reason (only you see it)">{(id) => <input id={id} type="text" maxLength={200} value={reason}
               placeholder="Family visit, works…" onChange={(e) => setReason(e.target.value)} className={inputClass} />}</Field>
        {block.error && <ErrorBox error={block.error} />}
        {conflictingBookings.length > 0 && (
          <ul className="text-sm">
            {conflictingBookings.map((b) => (
              <li key={b.id}><Link className="underline" href={`/admin/bookings/${b.id}`}>{b.guestName}</Link> · {stay(b.checkIn, b.checkOut)}</li>
            ))}
          </ul>
        )}
        <div className="flex justify-end gap-3">
          <button type="button" className={secondaryButton} onClick={onClose}>Cancel</button>
          <button type="submit" className={primaryButton} disabled={block.isPending || end <= start}>Block</button>
        </div>
      </form>
    </Dialog>
  );
}

function UnblockDialog({ block, onClose }: { block: BlockSummary | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const unblock = useMutation({
    mutationFn: (id: string) => adminApi.unblock(id),
    onSuccess: async () => {
      toast("Dates free again.");
      onClose();
      await Promise.all(["calendar", "dashboard"].map((k) => queryClient.invalidateQueries({ queryKey: [k] })));
    },
  });
  return (
    <Dialog open={block !== null} onClose={onClose} title="Remove this block?">
      {block && (
        <p className="text-sm">
          {block.reason ?? "Blocked"}: {day(block.startDate)} → {day(block.endDate)}. Guests will be able to book these nights again.
        </p>
      )}
      {unblock.error && <div className="mt-3"><ErrorBox error={unblock.error} /></div>}
      <div className="mt-5 flex justify-end gap-3">
        <button type="button" className={secondaryButton} onClick={onClose}>Keep it</button>
        <button type="button" className={primaryButton} disabled={unblock.isPending} onClick={() => block && unblock.mutate(block.id)}>
          Remove block
        </button>
      </div>
    </Dialog>
  );
}
