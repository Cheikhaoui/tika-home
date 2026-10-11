import { Hero } from "@/components/sections/Hero";
import { HouseSection } from "@/components/sections/HouseSection";
import { AmenitiesSection } from "@/components/sections/AmenitiesSection";
import { GallerySection } from "@/components/sections/GallerySection";
import { LocationSection } from "@/components/sections/LocationSection";
import { RulesSection } from "@/components/sections/RulesSection";
import { FaqSection } from "@/components/sections/FaqSection";
import { getDictionary, getLocale } from "./dictionaries";

// A Server Component: runs at build time (static page per language), so it can be async and
// read the dictionary directly. The interactive parts are small Client Components inside it.
export default async function HomePage() {
  const [dict, locale] = await Promise.all([getDictionary(), getLocale()]);
  return (
    <>
      <Hero dict={dict} />
      <HouseSection dict={dict} locale={locale} />
      <GallerySection dict={dict} locale={locale} />
      <AmenitiesSection dict={dict} />
      <LocationSection dict={dict} />
      <RulesSection dict={dict} />
      <FaqSection dict={dict} />
    </>
  );
}
