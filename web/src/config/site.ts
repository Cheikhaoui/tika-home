// Site-wide settings. PLACEHOLDERS (marked) must be replaced with the real values before launch.

/** Public URL of the website, used for SEO (canonical links, sitemap, Open Graph). */
export const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

/** Id of the house in the backend (seed data, V3__seed_property.sql). */
export const propertyId = process.env.NEXT_PUBLIC_PROPERTY_ID ?? "6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b";

export const contact = {
  /** PLACEHOLDER WhatsApp number, international format without "+" or spaces (used in wa.me links). */
  whatsapp: "212600000000",
  /** PLACEHOLDER display version of the number. */
  whatsappDisplay: "+212 600 000 000",
  /** PLACEHOLDER public contact email. */
  email: "hello@tikahome.example",
};

/** PLACEHOLDER location: near Jemaa el-Fna, Marrakech medina. */
export const location = {
  latitude: 31.6258,
  longitude: -7.9891,
  googleMapsUrl: "https://www.google.com/maps/search/?api=1&query=31.6258,-7.9891",
};
