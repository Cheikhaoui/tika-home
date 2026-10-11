"use client";

// A Client Component: it needs state (open/closed) and click handlers, which only exist in the browser.
// Everything it shows is passed in as props by the Server Component Header.
import { useEffect, useState } from "react";
import { Menu, X } from "lucide-react";

type Props = {
  links: { href: string; label: string }[];
  openLabel: string;
  closeLabel: string;
};

export function MobileNav({ links, openLabel, closeLabel }: Props) {
  const [open, setOpen] = useState(false);

  // Close with Escape, like a native menu.
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  return (
    <div className="md:hidden">
      <button
        type="button"
        aria-expanded={open}
        aria-controls="mobile-menu"
        aria-label={open ? closeLabel : openLabel}
        onClick={() => setOpen((o) => !o)}
        className="rounded-full p-2 hover:bg-sand-deep"
      >
        {open ? <X aria-hidden size={22} /> : <Menu aria-hidden size={22} />}
      </button>
      {open && (
        <nav id="mobile-menu" className="absolute inset-x-0 top-full border-b border-line bg-sand shadow-sm">
          <ul className="flex flex-col px-4 py-2">
            {links.map((link) => (
              <li key={link.href}>
                <a href={link.href} onClick={() => setOpen(false)} className="block py-3 text-lg">
                  {link.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </div>
  );
}
