import type { MetadataRoute } from "next";
import { siteUrl } from "@/config/site";

/** /robots.txt: crawl the site, but not the API or the private iCal export. */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/api/", "/ical/"] },
    sitemap: new URL("/sitemap.xml", siteUrl).toString(),
  };
}
