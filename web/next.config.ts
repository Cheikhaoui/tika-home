import path from "node:path";
import type { NextConfig } from "next";

// Where the Spring Boot API runs. Only used on the server (rewrites), never shipped to the browser.
const apiUrl = process.env.API_URL ?? "http://localhost:8081";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  images: {
    // Placeholder photos come from Unsplash (see src/content/photos.ts). Real photos can be put in /public.
    remotePatterns: [new URL("https://images.unsplash.com/photo-**")],
  },
  // The browser calls /api/... on this site; Next.js forwards it to the backend.
  // Same origin for the browser = no CORS setup, and the backend address stays private.
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiUrl}/api/:path*` }];
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
