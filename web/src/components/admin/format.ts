import { formatDate, formatMoney } from "@/i18n/format";
import type { IsoDate } from "@/lib/dates";

// The admin is in English; dates like "Fri 20 Nov".
export const money = (amount: number, currency: string) => formatMoney("en", amount, currency);
export const day = (iso: IsoDate) => formatDate("en", iso, { weekday: "short", day: "numeric", month: "short" });
export const dayYear = (iso: IsoDate) => formatDate("en", iso, { day: "numeric", month: "short", year: "numeric" });
export const stay = (checkIn: IsoDate, checkOut: IsoDate) => `${day(checkIn)} → ${day(checkOut)}`;
export const dateTime = (instant: string | null) =>
  instant
    ? new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short", timeZone: "Africa/Casablanca" })
        .format(new Date(instant))
    : "never";
