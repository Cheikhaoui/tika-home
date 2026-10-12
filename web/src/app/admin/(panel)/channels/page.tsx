import type { Metadata } from "next";
import { ChannelsView } from "@/components/admin/ChannelsView";

export const metadata: Metadata = { title: "Channels" };

export default function ChannelsPage() {
  return <ChannelsView />;
}
