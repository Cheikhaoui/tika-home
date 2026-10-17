import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker: a self-contained server (.next/standalone) with only the files it needs.
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  images: {
    // Placeholder photos come from Unsplash (see src/content/photos.ts). Real photos can be put in /public.
    remotePatterns: [new URL("https://images.unsplash.com/photo-**")],
  },
  turbopack: {
    root: path.join(__dirname), // this app is the root, not a parent folder with its own lockfile
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
