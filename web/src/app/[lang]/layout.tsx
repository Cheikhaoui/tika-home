import type { Metadata } from "next";
import { Amiri, Cormorant_Garamond, Geist, Noto_Sans_Arabic } from "next/font/google";
import { direction, locales } from "@/i18n/config";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { WhatsAppButton } from "@/components/layout/WhatsAppButton";
import { ChatAssistant } from "@/components/assistant/ChatAssistant";
import { propertyId, siteUrl } from "@/config/site";
import { heroPhoto } from "@/content/photos";
import { languageAlternates, ogLocale } from "@/lib/seo";
import { getDictionary, getLocale } from "./dictionaries";
import "../globals.css";

// next/font downloads the fonts at build time and serves them from our own domain:
// no request to Google from visitors, and no layout shift while fonts load.
const latinSans = Geist({ variable: "--font-latin-sans", subsets: ["latin"] });
const latinSerif = Cormorant_Garamond({
  variable: "--font-latin-serif",
  subsets: ["latin"],
  weight: ["500", "600", "700"],
});
const arabicSans = Noto_Sans_Arabic({ variable: "--font-arabic-sans", subsets: ["arabic"] });
const arabicSerif = Amiri({ variable: "--font-arabic-serif", subsets: ["arabic"], weight: ["400", "700"] });

// One static HTML version per language is generated at build time (/fr, /en, /ar).
export async function generateStaticParams() {
  return locales.map((lang) => ({ lang }));
}

export async function generateMetadata(): Promise<Metadata> {
  const [locale, dict] = await Promise.all([getLocale(), getDictionary()]);
  const ogImage = `${heroPhoto.src}?w=1200&h=630&fit=crop&q=80&auto=format`;
  return {
    metadataBase: new URL(siteUrl),
    title: dict.meta.title,
    description: dict.meta.description,
    alternates: { canonical: `/${locale}`, languages: languageAlternates() },
    openGraph: {
      type: "website",
      url: `/${locale}`,
      siteName: dict.site.name,
      title: dict.meta.title,
      description: dict.meta.description,
      locale: ogLocale[locale],
      alternateLocale: locales.filter((l) => l !== locale).map((l) => ogLocale[l]),
      images: [{ url: ogImage, width: 1200, height: 630, alt: dict.hero.photoAlt }],
    },
    twitter: { card: "summary_large_image", title: dict.meta.title, description: dict.meta.description, images: [ogImage] },
  };
}

// This is the ROOT layout (there is no app/layout.tsx): it lives under [lang] so that
// <html lang dir> can depend on the language. dir="rtl" mirrors the whole page for Arabic.
export default async function RootLayout({ children }: LayoutProps<"/[lang]">) {
  const [locale, dict] = await Promise.all([getLocale(), getDictionary()]);
  return (
    <html
      lang={locale}
      dir={direction(locale)}
      className={`${latinSans.variable} ${latinSerif.variable} ${arabicSans.variable} ${arabicSerif.variable} antialiased`}
    >
      <body className="flex min-h-screen flex-col">
        <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-white focus:p-3">
          {dict.nav.skipToContent}
        </a>
        <Header dict={dict} locale={locale} />
        <main id="main" className="flex-1">
          {children}
        </main>
        <Footer dict={dict} locale={locale} />
        <WhatsAppButton dict={dict} />
        {/* Only the assistant texts are sent to the browser, not the whole dictionary. */}
        <ChatAssistant locale={locale} propertyId={propertyId} labels={dict.assistant} />
      </body>
    </html>
  );
}
