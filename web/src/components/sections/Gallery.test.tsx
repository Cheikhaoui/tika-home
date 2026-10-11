import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Gallery, type GalleryPhoto } from "./Gallery";

const photos: GalleryPhoto[] = ["a", "b", "c"].map((id) => ({
  id,
  src: `https://images.unsplash.com/photo-${id}`,
  width: 100,
  height: 100,
  alt: `Photo ${id.toUpperCase()}`,
}));

const labels = {
  open: "Open photo {index} of {total}",
  dialogLabel: "Photo viewer",
  close: "Close",
  next: "Next photo",
  previous: "Previous photo",
  counter: "{index} / {total}",
};

function viewerImage() {
  // The thumbnail and the viewer image share the alt text; the viewer's is the last one.
  return screen.getAllByRole("img").at(-1);
}

describe("Gallery", () => {
  it("opens the clicked photo and navigates with buttons", async () => {
    const user = userEvent.setup();
    render(<Gallery photos={photos} labels={labels} rtl={false} />);

    await user.click(screen.getByRole("button", { name: "Open photo 2 of 3" }));
    expect(screen.getByText("2 / 3")).toBeInTheDocument();
    expect(viewerImage()).toHaveAttribute("alt", "Photo B");

    await user.click(screen.getByRole("button", { name: "Next photo" }));
    expect(screen.getByText("3 / 3")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Next photo" }));
    expect(screen.getByText("1 / 3")).toBeInTheDocument(); // wraps around
  });

  it("mirrors arrow keys in right-to-left languages", async () => {
    const user = userEvent.setup();
    render(<Gallery photos={photos} labels={labels} rtl />);

    await user.click(screen.getByRole("button", { name: "Open photo 1 of 3" }));
    await user.keyboard("{ArrowLeft}"); // in Arabic, left = next
    expect(screen.getByText("2 / 3")).toBeInTheDocument();
  });

  it("closes with the close button", async () => {
    const user = userEvent.setup();
    render(<Gallery photos={photos} labels={labels} rtl={false} />);

    await user.click(screen.getByRole("button", { name: "Open photo 1 of 3" }));
    await user.click(screen.getByRole("button", { name: "Close" }));
    expect(screen.queryByText("1 / 3")).not.toBeInTheDocument();
  });
});
