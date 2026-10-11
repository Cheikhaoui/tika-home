import type { Dictionary } from "@/app/[lang]/dictionaries";
import { heroPhoto } from "@/content/photos";
import { Photo } from "@/components/ui/Photo";
import { Container } from "@/components/ui/Container";
import { primaryButton } from "@/components/ui/buttonStyles";

export function Hero({ dict }: { dict: Dictionary }) {
  return (
    <section className="relative isolate flex min-h-[min(78vh,760px)] items-end overflow-hidden">
      <Photo
        src={heroPhoto.src}
        alt={dict.hero.photoAlt}
        fill
        preload // above the fold: start loading immediately (Next 16 name for the old `priority`)
        sizes="100vw"
        className="-z-10 object-cover"
      />
      {/* Gradient so white text stays readable on any photo. */}
      <div className="absolute inset-0 -z-10 bg-linear-to-t from-ink/85 via-ink/55 to-ink/25" />
      <Container className="pb-16 pt-32 text-white sm:pb-24">
        <p className="text-sm font-medium uppercase tracking-[0.2em] text-white/80">{dict.hero.eyebrow}</p>
        <h1 className="mt-3 max-w-3xl font-serif text-4xl font-semibold leading-tight sm:text-6xl">
          {dict.hero.title}
        </h1>
        <p className="mt-5 max-w-2xl text-lg text-white/90">{dict.hero.subtitle}</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <a href="#booking" className={primaryButton}>
            {dict.hero.ctaBook}
          </a>
          <a
            href="#gallery"
            className="inline-flex items-center rounded-full border border-white/60 px-6 py-3 font-medium transition hover:bg-white/10"
          >
            {dict.hero.ctaPhotos}
          </a>
        </div>
      </Container>
    </section>
  );
}
