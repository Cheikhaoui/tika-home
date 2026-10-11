"use client";

import { useCallback, useEffect, useState } from "react";
import { CircleCheck, LoaderCircle } from "lucide-react";
import type { Locale } from "@/i18n/config";
import { formatDate, formatMoney, interpolate, plural } from "@/i18n/format";
import { api, ApiError, type Booking, type PropertySummary, type Quote } from "@/lib/api";
import { addDays, nightsBetween, type IsoDate } from "@/lib/dates";
import { bookedNights, selectDay, type Stay } from "@/lib/stay-selection";
import { primaryButton, secondaryButton } from "@/components/ui/buttonStyles";
import { AvailabilityCalendar } from "./AvailabilityCalendar";
import { GuestsPicker } from "./GuestsPicker";
import { QuoteSummary } from "./QuoteSummary";
import { BookingForm, type SubmitResult } from "./BookingForm";
import type { BookingFormField, BookingFormValues } from "./bookingSchema";
import type { BookingLabels } from "./types";

const BOOKING_WINDOW_DAYS = 365; // availability loaded in one request (backend max: 366)

type Loaded = { property: PropertySummary; booked: Set<IsoDate>; today: IsoDate; lastDay: IsoDate };
type LoadState = { status: "loading" } | { status: "error" } | ({ status: "ready" } & Loaded);
type QuoteState = { status: "idle" } | { status: "loading" } | { status: "error" } | { status: "ready"; quote: Quote };

/** "Today" in Marrakech (like the backend), whatever the visitor's own time zone. */
function marrakechToday(): IsoDate {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Africa/Casablanca" }).format(new Date());
}

type Props = { locale: Locale; propertyId: string; labels: BookingLabels };

/**
 * The booking "brain": owns the state (dates, guests, quote, step) and talks to the API.
 * Child components only display data and report events (state flows down, events flow up).
 */
export function BookingWidget({ locale, propertyId, labels }: Props) {
  const [load, setLoad] = useState<LoadState>({ status: "loading" });
  const [stay, setStay] = useState<Stay>({});
  const [guests, setGuests] = useState(2);
  const [quoteState, setQuoteState] = useState<QuoteState>({ status: "idle" });
  const [step, setStep] = useState<"dates" | "details" | "done">("dates");
  const [booking, setBooking] = useState<{ result: Booking; name: string; email: string }>();
  const [notice, setNotice] = useState<string>();

  // 1. Load the house facts and 12 months of availability (in the browser, after the page shows).
  const loadAvailability = useCallback(
    async (signal?: AbortSignal) => {
      setLoad({ status: "loading" });
      try {
        const today = marrakechToday();
        const lastDay = addDays(today, BOOKING_WINDOW_DAYS);
        const [property, availability] = await Promise.all([
          api.property(propertyId, signal),
          api.availability(propertyId, today, lastDay, signal),
        ]);
        setLoad({ status: "ready", property, booked: bookedNights(availability.unavailable), today, lastDay });
      } catch (e) {
        if (!signal?.aborted) setLoad({ status: "error" });
        if (!(e instanceof Error && e.name === "AbortError")) console.error(e);
      }
    },
    [propertyId],
  );

  useEffect(() => {
    const controller = new AbortController();
    // Effects are how React components talk to the outside world (here: the API) after rendering.
    void loadAvailability(controller.signal); // eslint-disable-line react-hooks/set-state-in-effect
    return () => controller.abort(); // cleanup: cancel the request if the component goes away
  }, [loadAvailability]);

  // 2. Ask the backend for a price every time the dates or the number of guests change.
  const { checkIn, checkOut } = stay;
  useEffect(() => {
    if (!checkIn || !checkOut) {
      setQuoteState({ status: "idle" }); // eslint-disable-line react-hooks/set-state-in-effect
      return;
    }
    const controller = new AbortController(); // a newer click cancels the older, slower request
    setQuoteState({ status: "loading" });
    api
      .quote(propertyId, checkIn, checkOut, guests, controller.signal)
      .then((quote) => setQuoteState({ status: "ready", quote }))
      .catch(() => !controller.signal.aborted && setQuoteState({ status: "error" }));
    return () => controller.abort();
  }, [propertyId, checkIn, checkOut, guests]);

  // 3. Send the booking request; translate backend errors into something the guest understands.
  async function submit(values: BookingFormValues): Promise<SubmitResult> {
    if (!checkIn || !checkOut) return;
    try {
      const result = await api.createBooking({
        propertyId,
        checkIn,
        checkOut,
        guests,
        guest: { fullName: values.fullName, email: values.email, phone: values.phone || undefined, language: locale },
        message: values.message || undefined,
      });
      setBooking({ result, name: values.fullName, email: values.email });
      setStep("done");
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        // Someone else was faster: show the up-to-date calendar and ask for new dates.
        setNotice(labels.errors.conflict);
        setStay({});
        setStep("dates");
        await loadAvailability();
        return;
      }
      if (e instanceof ApiError && e.status === 422 && quoteState.status === "ready") {
        setQuoteState({ status: "ready", quote: { ...quoteState.quote, bookable: false, issues: e.problem.issues ?? [] } });
        setStep("dates");
        return;
      }
      if (e instanceof ApiError && e.status === 400 && e.problem.errors) {
        return { fieldErrors: mapFieldErrors(e.problem.errors, labels) };
      }
      return { formError: e instanceof ApiError ? labels.errors.generic : labels.errors.network };
    }
  }

  if (load.status === "loading") {
    return (
      <div className="flex items-center gap-3 rounded-2xl bg-white p-8 text-muted shadow-sm" role="status">
        <LoaderCircle aria-hidden className="size-5 animate-spin" /> {labels.loading}
      </div>
    );
  }

  if (load.status === "error") {
    return (
      <div className="rounded-2xl bg-white p-8 shadow-sm" role="alert">
        <p>{labels.loadError}</p>
        <button type="button" className={`${secondaryButton} mt-4`} onClick={() => loadAvailability()}>
          {labels.retry}
        </button>
      </div>
    );
  }

  const { property, booked, today, lastDay } = load;
  const money = (amount: number) => formatMoney(locale, amount, property.currency);
  const dateLabel = (iso?: IsoDate) =>
    iso ? formatDate(locale, iso, { weekday: "short", day: "numeric", month: "short" }) : labels.notSelected;

  if (step === "done" && booking) {
    return (
      <div className="rounded-2xl bg-white p-8 text-center shadow-sm" role="status" aria-live="polite">
        <CircleCheck aria-hidden className="mx-auto size-12 text-success" />
        <h3 className="mt-4 font-serif text-3xl font-semibold">{labels.success.title}</h3>
        <p className="mx-auto mt-3 max-w-lg">
          {interpolate(labels.success.text, { name: booking.name, reference: booking.result.reference })}
        </p>
        <p className="mt-2 text-sm text-muted">{interpolate(labels.success.email, { email: booking.email })}</p>
        <button
          type="button"
          className={`${secondaryButton} mt-6`}
          onClick={() => {
            setStay({});
            setBooking(undefined);
            setStep("dates");
            void loadAvailability();
          }}
        >
          {labels.success.another}
        </button>
      </div>
    );
  }

  const nights = checkIn && checkOut ? nightsBetween(checkIn, checkOut) : 0;
  const quote = quoteState.status === "ready" ? quoteState.quote : undefined;

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
      <div className="overflow-x-auto rounded-2xl bg-white p-4 shadow-sm sm:p-6">
        <p className="mb-2 text-sm font-medium" aria-live="polite">
          {checkIn && !checkOut ? labels.hintCheckOut : labels.hintCheckIn}
        </p>
        <AvailabilityCalendar
          locale={locale}
          stay={stay}
          booked={booked}
          today={today}
          lastDay={lastDay}
          onDayClick={(day) => {
            setNotice(undefined);
            setStay((current) => selectDay(current, day, booked, today, lastDay));
          }}
        />
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3 text-xs text-muted">
          <div className="flex gap-4">
            <span className="flex items-center gap-1.5">
              <span className="size-3 rounded-full border border-line bg-white" /> {labels.legendAvailable}
            </span>
            <span className="flex items-center gap-1.5">
              <span className="size-3 rounded-full bg-line" /> {labels.legendBooked}
            </span>
          </div>
          {checkIn && (
            <button type="button" className="underline hover:text-ink" onClick={() => setStay({})}>
              {labels.clearDates}
            </button>
          )}
        </div>
      </div>

      <aside className="h-fit rounded-2xl bg-white p-6 shadow-sm lg:sticky lg:top-24">
        {step === "details" && quote ? (
          <BookingForm labels={labels} onSubmit={submit} onBack={() => setStep("dates")} />
        ) : (
          <div className="space-y-5">
            <div>
              <p className="text-lg font-semibold">
                {interpolate(labels.fromPrice, { price: money(property.baseNightlyPrice) })}
              </p>
              <p className="text-sm text-muted">{plural(locale, property.minNights, labels.minStay)}</p>
            </div>
            {notice && (
              <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">
                {notice}
              </p>
            )}
            <div className="grid grid-cols-2 overflow-hidden rounded-xl border border-line">
              <div className="border-e border-line p-3">
                <p className="text-xs uppercase tracking-wide text-muted">{labels.checkIn}</p>
                <p className="font-medium">{dateLabel(checkIn)}</p>
              </div>
              <div className="p-3">
                <p className="text-xs uppercase tracking-wide text-muted">{labels.checkOut}</p>
                <p className="font-medium">{dateLabel(checkOut)}</p>
              </div>
            </div>
            <GuestsPicker locale={locale} value={guests} max={property.maxGuests} onChange={setGuests} labels={labels} />

            {nights > 0 && (
              <div aria-live="polite">
                {quoteState.status === "loading" && <p className="text-sm text-muted">{labels.calculating}</p>}
                {quoteState.status === "error" && <p className="text-sm text-danger">{labels.quoteError}</p>}
                {quote && <QuoteSummary locale={locale} quote={quote} maxGuests={property.maxGuests} labels={labels} />}
              </div>
            )}

            <button
              type="button"
              className={`${primaryButton} w-full`}
              disabled={!quote?.bookable}
              onClick={() => setStep("details")}
            >
              {labels.continue}
            </button>
            <p className="text-center text-xs text-muted">{labels.noPaymentNow}</p>
          </div>
        )}
      </aside>
    </div>
  );
}

/** Backend field names ("guest.email") -> form fields, with our translated messages. */
function mapFieldErrors(errors: Record<string, string>, labels: BookingLabels) {
  const fields: Record<string, BookingFormField> = {
    "guest.fullName": "fullName",
    "guest.email": "email",
    "guest.phone": "phone",
    message: "message",
  };
  const messages: Record<BookingFormField, string> = {
    fullName: labels.validation.required,
    email: labels.validation.email,
    phone: labels.validation.phone,
    message: labels.validation.tooLong,
  };
  const result: Partial<Record<BookingFormField, string>> = {};
  for (const key of Object.keys(errors)) {
    const field = fields[key];
    if (field) result[field] = messages[field];
  }
  return result;
}
