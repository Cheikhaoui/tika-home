import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { Container } from "@/components/ui/Container";
import { primaryButton } from "@/components/ui/buttonStyles";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { MobileNav } from "./MobileNav";

/** Server Component: rendered to HTML on the server, no JavaScript shipped for it (except MobileNav). */
export function Header({ dict, locale }: { dict: Dictionary; locale: Locale }) {
  const links = [
    { href: "#house", label: dict.nav.house },
    { href: "#gallery", label: dict.nav.gallery },
    { href: "#amenities", label: dict.nav.amenities },
    { href: "#location", label: dict.nav.location },
    { href: "#faq", label: dict.nav.faq },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-sand/95 backdrop-blur">
      <Container className="relative flex h-16 items-center justify-between gap-4">
        <a href={`/${locale}`} className="font-serif text-2xl font-semibold text-terracotta-dark">
          {dict.site.name}
        </a>

        <nav aria-label={dict.nav.label} className="hidden md:block">
          <ul className="flex gap-6 text-sm">
            {links.map((link) => (
              <li key={link.href}>
                <a href={link.href} className="text-muted transition hover:text-ink">
                  {link.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex items-center gap-2">
          <div className="hidden sm:block">
            <LanguageSwitcher current={locale} label={dict.nav.language} />
          </div>
          <a href="#booking" className={`${primaryButton} hidden px-4 py-2 text-sm sm:inline-flex`}>
            {dict.nav.book}
          </a>
          <MobileNav
            links={[...links, { href: "#booking", label: dict.nav.book }]}
            openLabel={dict.nav.openMenu}
            closeLabel={dict.nav.closeMenu}
          />
        </div>
      </Container>
      {/* On phones the language links sit in a thin bar under the header. */}
      <div className="border-t border-line sm:hidden">
        <Container className="flex justify-center py-1">
          <LanguageSwitcher current={locale} label={dict.nav.language} />
        </Container>
      </div>
    </header>
  );
}
