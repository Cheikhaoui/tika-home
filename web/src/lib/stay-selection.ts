// Pure date-selection rules for the booking calendar (no React, easy to unit test).
//
// Key idea: "unavailable" is about NIGHTS. If nights 10..14 are booked, the 10th can't be an
// arrival day, but it CAN be a departure day (the stay 7 -> 10 uses nights 7, 8, 9).
import { addDays, type IsoDate } from "./dates";
import type { DateRange } from "./api";

export const MAX_NIGHTS = 90; // same limit as the backend

export type Stay = { checkIn?: IsoDate; checkOut?: IsoDate };

/** Every booked night as a Set ("2027-03-10", ...). Ranges are [start, end): end is not a night. */
export function bookedNights(unavailable: DateRange[]): Set<IsoDate> {
  const nights = new Set<IsoDate>();
  for (const { start, end } of unavailable) {
    for (let night = start; night < end; night = addDays(night, 1)) {
      nights.add(night);
    }
  }
  return nights;
}

/** Latest possible departure for an arrival: the morning of the first booked night after it. */
export function latestCheckOut(checkIn: IsoDate, booked: Set<IsoDate>, lastDay: IsoDate): IsoDate {
  let day = addDays(checkIn, 1);
  for (let n = 1; n < MAX_NIGHTS && day < lastDay && !booked.has(day); n++) {
    day = addDays(day, 1);
  }
  return day;
}

/** Can a stay start on this day? (the night of that day must be free and not in the past) */
export function canCheckIn(day: IsoDate, booked: Set<IsoDate>, today: IsoDate, lastDay: IsoDate): boolean {
  return day >= today && day < lastDay && !booked.has(day);
}

/**
 * Next selection after the user clicks a day. Like Airbnb: first click = arrival, second = departure;
 * clicking before the arrival or after a booked night starts a new selection.
 */
export function selectDay(
  current: Stay,
  day: IsoDate,
  booked: Set<IsoDate>,
  today: IsoDate,
  lastDay: IsoDate,
): Stay {
  const choosingCheckOut = current.checkIn !== undefined && current.checkOut === undefined;
  if (choosingCheckOut) {
    const checkIn = current.checkIn!;
    if (day > checkIn && day <= latestCheckOut(checkIn, booked, lastDay)) {
      return { checkIn, checkOut: day };
    }
  }
  return canCheckIn(day, booked, today, lastDay) ? { checkIn: day } : current;
}

/** Should the calendar grey out this day for the current step? */
export function isDayDisabled(
  current: Stay,
  day: IsoDate,
  booked: Set<IsoDate>,
  today: IsoDate,
  lastDay: IsoDate,
): boolean {
  const choosingCheckOut = current.checkIn !== undefined && current.checkOut === undefined;
  if (choosingCheckOut && day > current.checkIn!) {
    return day > latestCheckOut(current.checkIn!, booked, lastDay);
  }
  return !canCheckIn(day, booked, today, lastDay);
}
