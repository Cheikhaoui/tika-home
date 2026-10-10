import Link from "next/link";
import { localeNames, locales } from "@/i18n/config";
import { getDictionary, getLocale } from "./dictionaries";

// A Server Component (the default in the App Router): it runs on the server only,
// so it can be async and read the dictionary directly. No "use client", no useEffect.
export default async function HomePage() {
  const [dict, locale] = await Promise.all([getDictionary(), getLocale()]);

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8 text-center">
      <h1 className="text-5xl font-semibold tracking-tight">{dict.home.title}</h1>
      <p className="text-xl text-foreground/70">{dict.home.subtitle}</p>
      <p className="text-foreground/50">{dict.home.comingSoon}</p>

      <nav aria-label={dict.language} className="flex gap-4">
        {locales.map((l) => (
          <Link
            key={l}
            href={`/${l}`}
            hrefLang={l}
            aria-current={l === locale ? "page" : undefined}
            className="rounded-full border border-foreground/20 px-4 py-1 aria-[current=page]:bg-foreground aria-[current=page]:text-background"
          >
            {localeNames[l]}
          </Link>
        ))}
      </nav>
    </main>
  );
}
