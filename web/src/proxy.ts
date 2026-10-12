import { NextResponse, type NextRequest } from "next/server";
import { defaultLocale, isLocale, locales, type Locale } from "@/i18n/config";

/** Name of the HttpOnly session cookie set by the backend on login. */
export const ADMIN_COOKIE = "tikahome_admin";

/**
 * Proxy (called "middleware" before Next.js 16) runs before every matched request.
 * - /admin/...: without the session cookie, send the visitor to the login page. This is only for a nice
 *   experience: the real protection is the backend, which checks the token on every API call.
 * - public pages: "/" or "/anything" without a language prefix -> "/{best language}/anything".
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname === "/admin" || pathname.startsWith("/admin/")) {
    if (pathname !== "/admin/login" && !request.cookies.has(ADMIN_COOKIE)) {
      const login = new URL("/admin/login", request.url);
      login.searchParams.set("next", pathname);
      return NextResponse.redirect(login);
    }
    return;
  }

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
  // Skip Next internals, API/iCal routes and files with an extension (favicon.ico, images, ...).
  matcher: ["/((?!_next|api|ical|.*\\..*).*)"],
};
