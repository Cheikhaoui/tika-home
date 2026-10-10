import { NextResponse, type NextRequest } from "next/server";
import { defaultLocale, isLocale, locales, type Locale } from "@/i18n/config";

/**
 * Proxy (called "middleware" before Next.js 16) runs before every matched request.
 * Here it only does one thing: send "/" or "/anything" without a language prefix
 * to "/{best language}/anything", based on the browser's Accept-Language header.
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasLocale = locales.some((l) => pathname === `/${l}` || pathname.startsWith(`/${l}/`));
  if (hasLocale) return;

  request.nextUrl.pathname = `/${preferredLocale(request)}${pathname === "/" ? "" : pathname}`;
  return NextResponse.redirect(request.nextUrl);
}

/** "ar-MA,ar;q=0.9,fr;q=0.8" -> first supported primary language, else the default. */
function preferredLocale(request: NextRequest): Locale {
  const header = request.headers.get("accept-language") ?? "";
  const ranked = header
    .split(",")
    .map((part) => {
      const [tag, q] = part.trim().split(";q=");
      return { lang: tag.split("-")[0].toLowerCase(), q: q ? Number(q) : 1 };
    })
    .sort((a, b) => b.q - a.q);
  return ranked.map((r) => r.lang).find(isLocale) ?? defaultLocale;
}

export const config = {
  // Skip Next internals, API routes and files with an extension (favicon.ico, images, ...).
  matcher: ["/((?!_next|api|.*\\..*).*)"],
};
