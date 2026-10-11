// Calendar dates ("2027-03-10") as plain strings, like the backend's LocalDate.
// Never convert them through UTC: new Date("2027-03-10") is UTC midnight, which is still
// the 9th in America, a classic off-by-one-day bug.

export type IsoDate = string;

/** Local calendar date of a Date object (what the user clicked in the calendar). */
export function toIsoDate(date: Date): IsoDate {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** Local Date object at midnight for a calendar date (for the calendar component). */
export function fromIsoDate(iso: IsoDate): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(iso: IsoDate, days: number): IsoDate {
  const date = fromIsoDate(iso);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

/** Number of nights between check-in and check-out. */
export function nightsBetween(checkIn: IsoDate, checkOut: IsoDate): number {
  return Math.round((fromIsoDate(checkOut).getTime() - fromIsoDate(checkIn).getTime()) / 86_400_000);
}
