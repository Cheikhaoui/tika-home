import { lang } from "next/root-params";
import { notFound } from "next/navigation";
import { isLocale, type Locale } from "@/i18n/config";

// Each dictionary is loaded lazily and only on the server: translations never ship as client JS
// unless we explicitly pass some strings down to a Client Component.
// The English file defines the shape; fr/ar must provide the same keys (checked by `satisfies` below).
export type Dictionary = typeof import("./dictionaries/en.json");

const dictionaries = {
  fr: () => import("./dictionaries/fr.json").then((m) => m.default),
  en: () => import("./dictionaries/en.json").then((m) => m.default),
  ar: () => import("./dictionaries/ar.json").then((m) => m.default),
} satisfies Record<Locale, () => Promise<Dictionary>>;

/**
 * Current locale from the [lang] root segment; 404 for unknown values like /de.
 * lang() is typed string | undefined because /admin is another root layout without a [lang] segment.
 */
export async function getLocale(): Promise<Locale> {
  const value = await lang();
  if (!value || !isLocale(value)) notFound();
  return value;
}

export async function getDictionary(): Promise<Dictionary> {
  return dictionaries[await getLocale()]();
}
