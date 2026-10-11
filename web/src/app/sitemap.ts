import type { MetadataRoute } from "next";
import { locales } from "@/i18n/config";
import { languageAlternates, localeUrl } from "@/lib/seo";

/** /sitemap.xml: one entry per language, each listing its translations (hreflang). */
export default function sitemap(): MetadataRoute.Sitemap {
  return locales.map((locale) => ({
    url: localeUrl(locale),
    changeFrequency: "weekly",
    priority: 1,
    alternates: { languages: languageAlternates() },
  }));
}
