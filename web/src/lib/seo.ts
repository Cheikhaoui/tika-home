import { locales, type Locale } from "@/i18n/config";
import { siteUrl } from "@/config/site";

export const defaultSeoLocale: Locale = "fr";

/** Absolute URL of a language version, e.g. https://tikahome.ma/fr */
export function localeUrl(locale: Locale): string {
  return new URL(`/${locale}`, siteUrl).toString();
}

/** hreflang map for <link rel="alternate">: each language + x-default for everyone else. */
export function languageAlternates(): Record<string, string> {
  return {
    ...Object.fromEntries(locales.map((l) => [l, localeUrl(l)])),
    "x-default": localeUrl(defaultSeoLocale),
  };
}

/** Open Graph locale codes (language_TERRITORY). */
export const ogLocale: Record<Locale, string> = { fr: "fr_FR", en: "en_GB", ar: "ar_MA" };
