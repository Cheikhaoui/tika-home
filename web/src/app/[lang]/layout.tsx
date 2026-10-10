import type { Metadata } from "next";
import { Geist, Noto_Sans_Arabic } from "next/font/google";
import { direction, locales } from "@/i18n/config";
import { getDictionary, getLocale } from "./dictionaries";
import "../globals.css";

// next/font downloads fonts at build time and self-hosts them: no request to Google from the visitor.
const geist = Geist({ variable: "--font-latin", subsets: ["latin"] });
const notoArabic = Noto_Sans_Arabic({ variable: "--font-arabic", subsets: ["arabic"] });

// One static HTML version per language is generated at build time (/fr, /en, /ar).
export async function generateStaticParams() {
  return locales.map((lang) => ({ lang }));
}

export async function generateMetadata(): Promise<Metadata> {
  const dict = await getDictionary();
  return { title: dict.meta.title, description: dict.meta.description };
}

// This is the ROOT layout (there is no app/layout.tsx): it lives under [lang] so that
// <html lang dir> can depend on the language. dir="rtl" flips the whole page for Arabic.
export default async function RootLayout({ children }: LayoutProps<"/[lang]">) {
  const locale = await getLocale();
  return (
    <html
      lang={locale}
      dir={direction(locale)}
      className={`${geist.variable} ${notoArabic.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
