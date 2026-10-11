import { ExternalLink, MapPin } from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { location } from "@/config/site";
import { locationPhoto } from "@/content/photos";
import { placeIds } from "@/content/property";
import { Photo } from "@/components/ui/Photo";
import { Section } from "@/components/ui/Section";
import { secondaryButton } from "@/components/ui/buttonStyles";

export function LocationSection({ dict }: { dict: Dictionary }) {
  return (
    <Section id="location" title={dict.location.title} intro={dict.location.intro}>
      <div className="grid gap-10 lg:grid-cols-2">
        <div>
          <ul className="space-y-3">
            {placeIds.map((id) => (
              <li key={id} className="flex items-start gap-3">
                <MapPin aria-hidden className="mt-0.5 size-5 shrink-0 text-terracotta" />
                <span>{dict.location.places[id]}</span>
              </li>
            ))}
          </ul>
          <h3 className="mt-8 font-serif text-2xl font-semibold">{dict.location.arrivalTitle}</h3>
          <p className="mt-2 text-muted">{dict.location.arrival}</p>
          <a href={location.googleMapsUrl} target="_blank" rel="noopener noreferrer" className={`${secondaryButton} mt-6`}>
            {dict.location.mapLink}
            <ExternalLink aria-hidden className="size-4" />
          </a>
        </div>
        <div className="relative aspect-[4/3] overflow-hidden rounded-2xl">
          <Photo
            src={locationPhoto.src}
            alt={dict.location.photoAlt}
            fill
            sizes="(min-width: 1024px) 50vw, 100vw"
            className="object-cover"
          />
        </div>
      </div>
    </Section>
  );
}
