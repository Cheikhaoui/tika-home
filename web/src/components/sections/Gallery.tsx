"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { Photo } from "@/components/ui/Photo";
import { interpolate } from "@/i18n/format";

export type GalleryPhoto = { id: string; src: string; width: number; height: number; alt: string };

export type GalleryLabels = {
  open: string;
  dialogLabel: string;
  close: string;
  next: string;
  previous: string;
  counter: string;
};

type Props = { photos: GalleryPhoto[]; labels: GalleryLabels; rtl: boolean };

const SWIPE_THRESHOLD_PX = 50;

/**
 * Photo grid + full-screen viewer. The viewer is a native <dialog> opened with showModal():
 * the browser traps focus inside it, closes it on Escape and makes the rest of the page inert.
 */
export function Gallery({ photos, labels, rtl }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [index, setIndex] = useState<number | null>(null);
  const pointerStartX = useRef<number | null>(null);
  const total = photos.length;

  const open = (i: number) => {
    setIndex(i);
    dialogRef.current?.showModal();
  };
  const close = () => dialogRef.current?.close();
  const go = useCallback((delta: number) => setIndex((i) => (i === null ? i : (i + delta + total) % total)), [total]);

  // Arrow keys follow reading direction: in Arabic (RTL) the "next" photo is on the left.
  useEffect(() => {
    if (index === null) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "ArrowRight") go(rtl ? -1 : 1);
      if (e.key === "ArrowLeft") go(rtl ? 1 : -1);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [index, go, rtl]);

  const onPointerUp = (e: React.PointerEvent) => {
    if (pointerStartX.current === null) return;
    const dx = e.clientX - pointerStartX.current;
    pointerStartX.current = null;
    if (Math.abs(dx) < SWIPE_THRESHOLD_PX) return;
    const swipedLeft = dx < 0;
    go(swipedLeft !== rtl ? 1 : -1); // swipe left = next (LTR) / previous (RTL)
  };

  const current = index === null ? null : photos[index];
  const PrevIcon = rtl ? ChevronRight : ChevronLeft;
  const NextIcon = rtl ? ChevronLeft : ChevronRight;

  return (
    <>
      <ul className="grid grid-cols-2 gap-2 sm:gap-3 md:grid-cols-4">
        {photos.map((photo, i) => (
          <li key={photo.id} className={i === 0 ? "col-span-2 row-span-2" : ""}>
            <button
              type="button"
              onClick={() => open(i)}
              aria-label={interpolate(labels.open, { index: i + 1, total })}
              className="group relative block aspect-square w-full overflow-hidden rounded-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-terracotta"
            >
              <Photo
                src={photo.src}
                alt={photo.alt}
                fill
                sizes={i === 0 ? "(min-width: 768px) 50vw, 100vw" : "(min-width: 768px) 25vw, 50vw"}
                className="object-cover transition duration-500 group-hover:scale-105"
              />
            </button>
          </li>
        ))}
      </ul>

      <dialog
        ref={dialogRef}
        aria-label={labels.dialogLabel}
        onClose={() => setIndex(null)}
        className="m-0 h-dvh max-h-none w-screen max-w-none bg-ink/95 p-0 text-white backdrop:bg-ink/80"
      >
        {current && (
          <div
            className="relative flex h-full w-full touch-pan-y items-center justify-center"
            onPointerDown={(e) => (pointerStartX.current = e.clientX)}
            onPointerUp={onPointerUp}
          >
            <div className="relative h-[80vh] w-[92vw]">
              <Photo src={current.src} alt={current.alt} fill sizes="92vw" className="object-contain" />
            </div>
            <p className="absolute top-4 start-4 text-sm text-white/80" aria-live="polite">
              {interpolate(labels.counter, { index: index! + 1, total })}
            </p>
            <button
              type="button"
              onClick={close}
              aria-label={labels.close}
              className="absolute top-3 end-3 rounded-full p-2 hover:bg-white/10"
            >
              <X aria-hidden className="size-7" />
            </button>
            <button
              type="button"
              onClick={() => go(-1)}
              aria-label={labels.previous}
              className="absolute start-2 top-1/2 -translate-y-1/2 rounded-full bg-black/30 p-2 hover:bg-black/50"
            >
              <PrevIcon aria-hidden className="size-8" />
            </button>
            <button
              type="button"
              onClick={() => go(1)}
              aria-label={labels.next}
              className="absolute end-2 top-1/2 -translate-y-1/2 rounded-full bg-black/30 p-2 hover:bg-black/50"
            >
              <NextIcon aria-hidden className="size-8" />
            </button>
          </div>
        )}
      </dialog>
    </>
  );
}
