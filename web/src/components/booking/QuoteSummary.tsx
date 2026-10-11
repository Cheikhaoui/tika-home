"use client";

import type { Locale } from "@/i18n/config";
import { formatMoney, interpolate, plural } from "@/i18n/format";
import type { Quote, QuoteIssue } from "@/lib/api";
import type { BookingLabels } from "./types";

type Props = { locale: Locale; quote: Quote; maxGuests: number; labels: BookingLabels };

/** Price breakdown. Nights with the same price are grouped: "€120 × 2 nights", "€180 × 3 nights". */
export function QuoteSummary({ locale, quote, maxGuests, labels }: Props) {
  const money = (amount: number) => formatMoney(locale, amount, quote.currency);
  const groups = new Map<number, number>();
  for (const night of quote.nightly) {
    groups.set(night.price, (groups.get(night.price) ?? 0) + 1);
  }

  return (
    <div className="space-y-3">
      <dl className="space-y-2 text-sm">
        {[...groups].map(([price, count]) => (
          <div key={price} className="flex justify-between gap-4">
            <dt>{interpolate(labels.priceLine, { price: money(price), nights: plural(locale, count, labels.nights) })}</dt>
            <dd>{money(price * count)}</dd>
          </div>
        ))}
        {quote.cleaningFee > 0 && (
          <div className="flex justify-between gap-4">
            <dt>{labels.cleaningFee}</dt>
            <dd>{money(quote.cleaningFee)}</dd>
          </div>
        )}
        <div className="flex justify-between gap-4 border-t border-line pt-3 text-base font-semibold">
          <dt>{labels.total}</dt>
          <dd>{money(quote.total)}</dd>
        </div>
      </dl>
      {quote.issues.length > 0 && (
        <ul role="alert" className="space-y-1 rounded-lg bg-danger/10 p-3 text-sm text-danger">
          {quote.issues.map((issue) => (
            <li key={issue}>{issueText(issue, quote, maxGuests, locale, labels)}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function issueText(issue: QuoteIssue, quote: Quote, maxGuests: number, locale: Locale, labels: BookingLabels) {
  return interpolate(labels.issues[issue], {
    minNights: plural(locale, quote.minNights, labels.nights),
    maxGuests,
  });
}
