import type { Metadata } from "next";
import { Suspense } from "react";
import { CalendarView } from "@/components/admin/CalendarView";
import { Loading } from "@/components/admin/ui";

export const metadata: Metadata = { title: "Calendar" };

// CalendarView reads today's date (new Date()). Inside <Suspense> it renders at request time; outside, Cache
// Components would freeze the date of the build into the prerendered page.
export default function CalendarPage() {
  return (
    <Suspense fallback={<Loading />}>
      <CalendarView />
    </Suspense>
  );
}
