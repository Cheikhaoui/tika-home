import type { Dictionary } from "@/app/[lang]/dictionaries";
import { direction, type Locale } from "@/i18n/config";
import { galleryPhotos } from "@/content/photos";
import { Section } from "@/components/ui/Section";
import { Gallery } from "./Gallery";

/**
 * Server Component wrapper: picks the translated alt texts and labels on the server and hands
 * plain data (strings, numbers) to the interactive Client Component.
 */
export function GallerySection({ dict, locale }: { dict: Dictionary; locale: Locale }) {
  const g = dict.gallery;
  const photos = galleryPhotos.map((p) => ({
    id: p.id,
    src: p.src,
    width: p.width,
    height: p.height,
    alt: g.alt[p.id as keyof typeof g.alt] ?? "",
  }));
  const authors = [...new Map(galleryPhotos.map((p) => [p.author, p])).values()];
  // "Placeholder photos by {authors} on Unsplash." -> text before, linked names, text after.
  const [creditBefore, creditAfter = ""] = g.placeholderNote.split("{authors}");

  return (
    <Section id="gallery" title={g.title} intro={g.intro} tone="white">
      <Gallery
        photos={photos}
        rtl={direction(locale) === "rtl"}
        labels={{
          open: g.open,
          dialogLabel: g.dialogLabel,
          close: g.close,
          next: g.next,
          previous: g.previous,
          counter: g.counter,
        }}
      />
      <p className="mt-4 text-xs text-muted">
        {creditBefore}
        {authors.map((p, i) => (
          <span key={p.author}>
            {i > 0 && ", "}
            <a href={p.authorUrl} target="_blank" rel="noopener noreferrer" className="underline hover:text-ink">
              {p.author}
            </a>
          </span>
        ))}
        {creditAfter}
      </p>
    </Section>
  );
}
