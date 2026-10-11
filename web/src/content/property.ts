// PLACEHOLDER house facts: replace with the real ones from the owner.
// Prices, capacity limits and minimum stay come from the backend (single source of truth).

export const facts = {
  guests: 6,
  bedrooms: 3,
  bathrooms: 2,
  beds: 4,
};

export const checkInTime = "15:00";
export const checkOutTime = "11:00";

/** Amenity ids; texts are in the dictionaries (amenities.items.<id>), icons in AmenitiesSection. */
export const amenityIds = [
  "wifi",
  "aircon",
  "pool",
  "terrace",
  "kitchen",
  "washer",
  "linens",
  "breakfast",
  "transfer",
] as const;

export type AmenityId = (typeof amenityIds)[number];

/** Walking / driving times shown in the location section (texts in location.places.<id>). */
export const placeIds = ["jemaa", "souks", "koutoubia", "majorelle", "airport"] as const;
