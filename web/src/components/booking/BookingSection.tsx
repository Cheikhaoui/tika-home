import type { Dictionary } from "@/app/[lang]/dictionaries";
import type { Locale } from "@/i18n/config";
import { propertyId } from "@/config/site";
import { Section } from "@/components/ui/Section";
import { BookingWidget } from "./BookingWidget";

export function BookingSection({ dict, locale }: { dict: Dictionary; locale: Locale }) {
  return (
    <Section id="booking" title={dict.booking.title} intro={dict.booking.intro}>
      <BookingWidget locale={locale} propertyId={propertyId} labels={dict.booking} />
    </Section>
  );
}
