import type { ReactNode } from "react";
import { Container } from "./Container";

type Props = {
  id: string;
  title: string;
  intro?: string;
  children: ReactNode;
  tone?: "sand" | "white";
};

/** A page section with an anchor id (for the header links) and a heading. */
export function Section({ id, title, intro, children, tone = "sand" }: Props) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className={tone === "white" ? "bg-white" : ""}>
      <Container className="py-16 sm:py-24">
        <h2 id={`${id}-title`} className="font-serif text-3xl font-semibold sm:text-4xl">
          {title}
        </h2>
        {intro && <p className="mt-3 max-w-2xl text-lg text-muted">{intro}</p>}
        <div className="mt-10">{children}</div>
      </Container>
    </section>
  );
}
