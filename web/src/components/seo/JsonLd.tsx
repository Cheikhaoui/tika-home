import type { Dictionary } from "@/app/[lang]/dictionaries";
import type { Locale } from "@/i18n/config";
import { location } from "@/config/site";
import { galleryPhotos } from "@/content/photos";
import { amenityIds, checkInTime, checkOutTime, facts } from "@/content/property";
import { localeUrl } from "@/lib/seo";

/**
 * Structured data (schema.org VacationRental) so search engines understand the page:
 * a whole house to rent in Marrakech, its capacity, rooms, photos and amenities.
 */
export function JsonLd({ dict, locale }: { dict: Dictionary; locale: Locale }) {
  const data = {
    "@context": "https://schema.org",
    "@type": "VacationRental",
    name: dict.site.name,
    description: dict.meta.description,
    url: localeUrl(locale),
    inLanguage: locale,
    image: galleryPhotos.slice(0, 6).map((p) => `${p.src}?w=1600&q=80&auto=format`),
    address: { "@type": "PostalAddress", addressLocality: "Marrakech", addressCountry: "MA" },
    geo: { "@type": "GeoCoordinates", latitude: location.latitude, longitude: location.longitude },
    checkinTime: checkInTime,
    checkoutTime: checkOutTime,
    containsPlace: {
      "@type": "Accommodation",
      additionalType: "EntirePlace",
      occupancy: { "@type": "QuantitativeValue", value: facts.guests },
      numberOfBedrooms: facts.bedrooms,
      numberOfBathroomsTotal: facts.bathrooms,
      amenityFeature: amenityIds.map((id) => ({
        "@type": "LocationFeatureSpecification",
        name: dict.amenities.items[id].title,
        value: true,
      })),
    },
  };

  return (
    <script
      type="application/ld+json"
      // "<" escaped so a text containing "</script>" can never break out of the tag (XSS).
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }}
    />
  );
}
