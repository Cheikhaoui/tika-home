import { ChevronDown } from "lucide-react";
import type { Dictionary } from "@/app/[lang]/dictionaries";
import { Section } from "@/components/ui/Section";

/** <details>/<summary>: an accessible accordion with no JavaScript at all. */
export function FaqSection({ dict }: { dict: Dictionary }) {
  return (
    <Section id="faq" title={dict.faq.title}>
      <div className="divide-y divide-line rounded-2xl bg-white shadow-sm">
        {dict.faq.items.map((item) => (
          <details key={item.q} className="group px-6 py-4">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-medium [&::-webkit-details-marker]:hidden">
              {item.q}
              <ChevronDown aria-hidden className="size-5 shrink-0 text-muted transition group-open:rotate-180" />
            </summary>
            <p className="mt-3 text-muted">{item.a}</p>
          </details>
        ))}
      </div>
    </Section>
  );
}
