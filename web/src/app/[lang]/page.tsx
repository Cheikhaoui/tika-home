import { Container } from "@/components/ui/Container";
import { getDictionary } from "./dictionaries";

// Temporary home page: the real sections arrive in the next commits.
export default async function HomePage() {
  const dict = await getDictionary();
  return (
    <Container className="py-24 text-center">
      <h1 className="font-serif text-5xl font-semibold">{dict.site.name}</h1>
      <p className="mt-4 text-lg text-muted">{dict.site.tagline}</p>
    </Container>
  );
}
