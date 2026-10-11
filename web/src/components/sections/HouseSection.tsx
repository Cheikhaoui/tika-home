import { BedDouble, Bath, DoorOpen, Users } from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import type { Locale } from "@/i18n/config";
import { interpolate, plural } from "@/i18n/format";
import { checkInTime, checkOutTime, facts } from "@/content/property";
import { Section } from "@/components/ui/Section";

export function HouseSection({ dict, locale }: { dict: Dictionary; locale: Locale }) {
  const f = dict.house.facts;
  const items = [
    { icon: Users, label: plural(locale, facts.guests, f.guests) },
    { icon: DoorOpen, label: plural(locale, facts.bedrooms, f.bedrooms) },
    { icon: BedDouble, label: plural(locale, facts.beds, f.beds) },
    { icon: Bath, label: plural(locale, facts.bathrooms, f.bathrooms) },
  ];

  return (
    <Section id="house" title={dict.house.title} intro={dict.house.intro}>
      <div className="grid gap-10 lg:grid-cols-[3fr_2fr]">
        <div className="space-y-4 text-lg leading-relaxed">
          {dict.house.paragraphs.map((p) => (
            <p key={p}>{p}</p>
          ))}
        </div>
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <ul className="grid grid-cols-2 gap-5">
            {items.map(({ icon: Icon, label }) => (
              <li key={label} className="flex items-center gap-3">
                <Icon aria-hidden className="size-6 shrink-0 text-zellige" />
                <span>{label}</span>
              </li>
            ))}
          </ul>
          <div className="mt-6 space-y-1 border-t border-line pt-4 text-sm text-muted">
            <p>{interpolate(dict.house.checkIn, { time: checkInTime })}</p>
            <p>{interpolate(dict.house.checkOut, { time: checkOutTime })}</p>
          </div>
        </div>
      </div>
    </Section>
  );
}
