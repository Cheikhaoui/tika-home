"use client";

// Client Component only because `loader` is a function: functions can't be passed from a Server
// Component to the browser. Unsplash resizes images itself (?w=...), so for Unsplash URLs we ask its CDN
// for the right width directly; local photos (/photos/...) use Next.js' built-in optimizer.
import Image, { type ImageLoader, type ImageProps } from "next/image";

const unsplashLoader: ImageLoader = ({ src, width, quality }) =>
  `${src}?w=${width}&q=${quality ?? 75}&auto=format&fit=max`;

export function Photo({ alt, ...props }: ImageProps) {
  const isUnsplash = typeof props.src === "string" && props.src.startsWith("https://images.unsplash.com/");
  return <Image alt={alt} {...props} loader={isUnsplash ? unsplashLoader : undefined} />;
}
