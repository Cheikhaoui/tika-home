import type { Locale } from "./config";

/**
 * Locale used for number/date formatting. Arabic uses ar-MA (Moroccan conventions, Latin digits 1 2 3)
 * like the backend emails; English uses en-GB (day before month, as in Europe and Morocco).
 */
export function intlLocale(locale: Locale): string {
  return { fr: "fr-MA", ar: "ar-MA", en: "en-GB" }[locale];
}

/** "Hello {name}" + { name: "Sara" } -> "Hello Sara". Unknown placeholders are left as is. */
export function interpolate(template: string, vars: Record<string, string | number> = {}): string {
  return template.replace(/\{(\w+)\}/g, (match, key: string) => (key in vars ? String(vars[key]) : match));
}

/**
 * Plural forms: { one: "{count} night", other: "{count} nights" }.
 * Intl.PluralRules knows each language's rules: English has 2 forms, French 2 (+ "many"),
 * Arabic 6 (zero, one, two, few, many, other). Missing forms fall back to "other".
 */
export function plural(locale: Locale, count: number, forms: Record<string, string>): string {
  const category = new Intl.PluralRules(intlLocale(locale)).select(count);
  return interpolate(forms[category] ?? forms.other, { count: formatNumber(locale, count) });
}

export function formatNumber(locale: Locale, value: number): string {
  return new Intl.NumberFormat(intlLocale(locale)).format(value);
}

export function formatMoney(locale: Locale, amount: number, currency: string): string {
  return new Intl.NumberFormat(intlLocale(locale), {
    style: "currency",
    currency,
    maximumFractionDigits: Number.isInteger(amount) ? 0 : 2,
  }).format(amount);
}

/** Formats an ISO date "2027-03-10" without time-zone surprises (parsed as a calendar date, not UTC midnight). */
export function formatDate(locale: Locale, isoDate: string, options: Intl.DateTimeFormatOptions): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Intl.DateTimeFormat(intlLocale(locale), { ...options, timeZone: "UTC" }).format(
    new Date(Date.UTC(y, m - 1, d)),
  );
}
