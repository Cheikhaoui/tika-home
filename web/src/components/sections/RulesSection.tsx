import { Check } from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { interpolate } from "@/i18n/format";
import { checkInTime, checkOutTime } from "@/content/property";
import { Section } from "@/components/ui/Section";

export function RulesSection({ dict }: { dict: Dictionary }) {
  return (
    <Section id="rules" title={dict.rules.title} tone="white">
      <ul className="grid gap-3 sm:grid-cols-2">
        {dict.rules.items.map((rule) => (
          <li key={rule} className="flex items-start gap-3">
            <Check aria-hidden className="mt-0.5 size-5 shrink-0 text-zellige" />
            <span>{interpolate(rule, { checkIn: checkInTime, checkOut: checkOutTime })}</span>
          </li>
        ))}
      </ul>
    </Section>
  );
}
