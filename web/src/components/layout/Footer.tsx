import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { contact } from "@/config/site";
import { Container } from "@/components/ui/Container";
import { LanguageSwitcher } from "./LanguageSwitcher";

export function Footer({ dict, locale }: { dict: Dictionary; locale: Locale }) {
  return (
    <footer className="border-t border-line bg-sand-deep">
      <Container className="grid gap-8 py-12 sm:grid-cols-3">
        <div>
          <p className="font-serif text-2xl font-semibold text-terracotta-dark">{dict.site.name}</p>
          <p className="mt-2 text-sm text-muted">{dict.site.tagline}</p>
        </div>
        <div className="text-sm">
          <p className="font-medium">{dict.footer.contact}</p>
          <ul className="mt-2 space-y-1 text-muted">
            <li>
              <a href={`mailto:${contact.email}`} className="hover:text-ink">
                {contact.email}
              </a>
            </li>
            <li>
              {/* dir="ltr": a phone number must not be reversed inside Arabic text. */}
              <a href={`https://wa.me/${contact.whatsapp}`} className="hover:text-ink" dir="ltr">
                WhatsApp {contact.whatsappDisplay}
              </a>
            </li>
          </ul>
        </div>
        <div className="text-sm">
          <p className="font-medium">{dict.nav.language}</p>
          <div className="mt-2">
            <LanguageSwitcher current={locale} label={dict.nav.language} />
          </div>
        </div>
      </Container>
      <Container className="border-t border-line py-4 text-xs text-muted">
        <p>
          © {dict.site.name} · {dict.footer.rights}
        </p>
      </Container>
    </footer>
  );
}
