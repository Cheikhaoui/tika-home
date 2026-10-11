import Link from "next/link";
import { localeNames, locales, type Locale } from "@/i18n/config";

/** Plain links (works without JavaScript). hrefLang tells browsers and Google which language each link is. */
export function LanguageSwitcher({ current, label }: { current: Locale; label: string }) {
  return (
    <nav aria-label={label} className="flex items-center gap-1 text-sm">
      {locales.map((l) => (
        <Link
          key={l}
          href={`/${l}`}
          hrefLang={l}
          lang={l}
          aria-current={l === current ? "true" : undefined}
          className="rounded-full px-2.5 py-1 text-muted transition hover:text-ink aria-[current=true]:bg-ink aria-[current=true]:text-sand"
        >
          {localeNames[l]}
        </Link>
      ))}
    </nav>
  );
}
