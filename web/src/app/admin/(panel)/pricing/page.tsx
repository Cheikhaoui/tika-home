import type { Metadata } from "next";
import { Suspense } from "react";
import { PricingView } from "@/components/admin/PricingView";
import { Loading } from "@/components/admin/ui";

export const metadata: Metadata = { title: "Pricing" };

// PricingView reads today's date (new Date()). Inside <Suspense> it renders at request time; outside, Cache
// Components would freeze the date of the build into the prerendered page.
export default function PricingPage() {
  return (
    <Suspense fallback={<Loading />}>
      <PricingView />
    </Suspense>
  );
}
