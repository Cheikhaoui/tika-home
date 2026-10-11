"use client";

import { Minus, Plus } from "lucide-react";
import type { Locale } from "@/i18n/config";
import { formatNumber } from "@/i18n/format";

type Props = {
  locale: Locale;
  value: number;
  max: number;
  onChange: (value: number) => void;
  labels: { guests: string; decreaseGuests: string; increaseGuests: string };
};

export function GuestsPicker({ locale, value, max, onChange, labels }: Props) {
  const button =
    "flex size-9 items-center justify-center rounded-full border border-line transition hover:border-ink/40 disabled:cursor-not-allowed disabled:opacity-40";
  return (
    <div className="flex items-center justify-between">
      <span id="guests-label">{labels.guests}</span>
      <div className="flex items-center gap-3" role="group" aria-labelledby="guests-label">
        <button type="button" className={button} onClick={() => onChange(value - 1)} disabled={value <= 1}
                aria-label={labels.decreaseGuests}>
          <Minus aria-hidden className="size-4" />
        </button>
        <output className="w-6 text-center font-medium" aria-live="polite">
          {formatNumber(locale, value)}
        </output>
        <button type="button" className={button} onClick={() => onChange(value + 1)} disabled={value >= max}
                aria-label={labels.increaseGuests}>
          <Plus aria-hidden className="size-4" />
        </button>
      </div>
    </div>
  );
}
