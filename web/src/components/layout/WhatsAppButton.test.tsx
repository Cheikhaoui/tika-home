import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import fr from "@/app/[lang]/dictionaries/fr.json";
import { WhatsAppButton } from "./WhatsAppButton";

describe("WhatsAppButton", () => {
  it("links to WhatsApp with an encoded message in the page language", () => {
    render(<WhatsAppButton dict={fr} />);

    const link = screen.getByRole("link", { name: "Discutez avec nous sur WhatsApp" });
    const url = new URL(link.getAttribute("href")!);
    expect(url.origin + url.pathname).toBe("https://wa.me/212600000000");
    expect(url.searchParams.get("text")).toBe(fr.whatsapp.message);
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });
});
