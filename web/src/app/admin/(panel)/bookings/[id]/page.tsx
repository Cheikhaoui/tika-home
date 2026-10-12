import { Suspense } from "react";
import { BookingDetailView } from "@/components/admin/BookingDetailView";
import { Loading } from "@/components/admin/ui";

// With Cache Components, the [id] param is only known at request time: read it inside <Suspense>
// so Next.js can still prerender the static shell (admin layout + navigation).
export default function BookingPage({ params }: PageProps<"/admin/bookings/[id]">) {
  return (
    <Suspense fallback={<Loading />}>
      <BookingDetail params={params} />
    </Suspense>
  );
}

async function BookingDetail({ params }: Pick<PageProps<"/admin/bookings/[id]">, "params">) {
  const { id } = await params;
  return <BookingDetailView id={id} />;
}
