import { MessageCircle } from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { contact } from "@/config/site";

/**
 * Floating WhatsApp link with a pre-filled message in the visitor's language.
 * A plain link (https://wa.me/<number>?text=...): opens the app on phones, WhatsApp Web on desktop.
 * "end-5" = right in LTR, left in Arabic (RTL).
 */
export function WhatsAppButton({ dict }: { dict: Dictionary }) {
  const href = `https://wa.me/${contact.whatsapp}?text=${encodeURIComponent(dict.whatsapp.message)}`;
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={dict.whatsapp.label}
      className="fixed bottom-5 end-5 z-30 flex items-center gap-2 rounded-full bg-[#25D366] px-4 py-3 font-medium text-ink shadow-lg transition hover:scale-105 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink"
    >
      <MessageCircle aria-hidden className="size-6" />
      <span className="hidden sm:inline">{dict.whatsapp.short}</span>
    </a>
  );
}
