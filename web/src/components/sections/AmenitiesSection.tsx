import {
  AirVent,
  CarFront,
  Coffee,
  CookingPot,
  Shirt,
  Sun,
  Waves,
  WashingMachine,
  Wifi,
  type LucideIcon,
} from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { amenityIds, type AmenityId } from "@/content/property";
import { Section } from "@/components/ui/Section";

const icons: Record<AmenityId, LucideIcon> = {
  wifi: Wifi,
  aircon: AirVent,
  pool: Waves,
  terrace: Sun,
  kitchen: CookingPot,
  washer: WashingMachine,
  linens: Shirt,
  breakfast: Coffee,
  transfer: CarFront,
};

export function AmenitiesSection({ dict }: { dict: Dictionary }) {
  return (
    <Section id="amenities" title={dict.amenities.title} intro={dict.amenities.intro} tone="white">
      <ul className="grid gap-x-8 gap-y-6 sm:grid-cols-2 lg:grid-cols-3">
        {amenityIds.map((id) => {
          const Icon = icons[id];
          const item = dict.amenities.items[id];
          return (
            <li key={id} className="flex gap-4">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-full bg-sand-deep">
                <Icon aria-hidden className="size-5 text-terracotta" />
              </span>
              <div>
                <p className="font-medium">{item.title}</p>
                <p className="text-sm text-muted">{item.text}</p>
              </div>
            </li>
          );
        })}
      </ul>
    </Section>
  );
}
