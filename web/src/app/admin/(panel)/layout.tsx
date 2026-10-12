import { AdminShell } from "@/components/admin/AdminShell";

// "(panel)" is a route group: it groups the logged-in pages under one layout without adding to the URL.
export default function PanelLayout({ children }: LayoutProps<"/admin">) {
  return <AdminShell>{children}</AdminShell>;
}
