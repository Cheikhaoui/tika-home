import type { Metadata } from "next";
import { Cormorant_Garamond, Geist } from "next/font/google";
import { Providers } from "@/components/admin/Providers";
import "../globals.css";

const sans = Geist({ variable: "--font-latin-sans", subsets: ["latin"] });
const serif = Cormorant_Garamond({ variable: "--font-latin-serif", subsets: ["latin"], weight: ["500", "600", "700"] });

export const metadata: Metadata = {
  title: { default: "Admin · Tika Home", template: "%s · Tika Home admin" },
  robots: { index: false, follow: false }, // never in search engines
};

// A second ROOT layout (next to app/[lang]/layout.tsx): the admin is English only, with its own shell.
// Going between the public site and /admin does a full page load, which is fine.
export default function AdminRootLayout({ children }: LayoutProps<"/admin">) {
  return (
    <html lang="en" className={`${sans.variable} ${serif.variable} antialiased`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
