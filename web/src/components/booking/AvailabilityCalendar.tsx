"use client";

import { DayPicker } from "@daypicker/react";
import { arMA, enGB, fr } from "@daypicker/react/locale";
import "@daypicker/react/style.css";
import type { Locale } from "@/i18n/config";
import { addDays, fromIsoDate, toIsoDate, type IsoDate } from "@/lib/dates";
import { isDayDisabled, type Stay } from "@/lib/stay-selection";
import { useMediaQuery } from "@/hooks/useMediaQuery";

const calendarLocales = { en: enGB, fr, ar: arMA };

type Props = {
  locale: Locale;
  stay: Stay;
  booked: Set<IsoDate>;
  today: IsoDate;
  lastDay: IsoDate;
  onDayClick: (day: IsoDate) => void;
};

/**
 * Presentational calendar: shows the stay and booked nights, and reports clicks.
 * The selection rules live in lib/stay-selection.ts, the state in BookingWidget.
 */
export function AvailabilityCalendar({ locale, stay, booked, today, lastDay, onDayClick }: Props) {
  const twoMonths = useMediaQuery("(min-width: 768px)");

  return (
    <DayPicker
      mode="range"
      // Controlled: we ignore DayPicker's own range logic and only use the clicked day.
      selected={{
        from: stay.checkIn ? fromIsoDate(stay.checkIn) : undefined,
        to: stay.checkOut ? fromIsoDate(stay.checkOut) : undefined,
      }}
      onSelect={(_range, clicked) => onDayClick(toIsoDate(clicked))}
      disabled={(date) => isDayDisabled(stay, toIsoDate(date), booked, today, lastDay)}
      modifiers={{ booked: (date) => booked.has(toIsoDate(date)) }}
      modifiersClassNames={{ booked: "rdp-booked" }}
      locale={calendarLocales[locale]}
      dir={locale === "ar" ? "rtl" : "ltr"}
      numberOfMonths={twoMonths ? 2 : 1}
      startMonth={fromIsoDate(today)}
      endMonth={fromIsoDate(addDays(lastDay, -1))}
      showOutsideDays={false}
    />
  );
}
