import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { adminApi, type BookingDetail } from "@/lib/admin-api";
import { ApiError } from "@/lib/api";
import { ToastProvider } from "./Toast";
import { BookingDetailView } from "./BookingDetailView";

vi.mock("@/lib/admin-api", async (importActual) => ({
  ...(await importActual<typeof import("@/lib/admin-api")>()),
  adminApi: { booking: vi.fn(), confirm: vi.fn(), decline: vi.fn() },
}));

const booking: BookingDetail = {
  id: "b1",
  reference: "TK-ABC123",
  status: "REQUESTED",
  checkIn: "2027-03-10",
  checkOut: "2027-03-14",
  nights: 4,
  guests: 2,
  totalPrice: 520,
  currency: "EUR",
  message: "Arriving late",
  createdAt: "2027-01-01T10:00:00Z",
  updatedAt: "2027-01-01T10:00:00Z",
  guest: { fullName: "Sara Benali", email: "sara@example.com", phone: "+212 600 000 000", language: "fr" },
  conflicts: [],
};

function renderDetail() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <BookingDetailView id="b1" />
      </ToastProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => vi.mocked(adminApi.booking).mockResolvedValue(booking));
afterEach(() => vi.clearAllMocks());

describe("BookingDetailView", () => {
  it("shows the guest, the stay and contact links", async () => {
    renderDetail();
    expect(await screen.findByRole("heading", { name: "Sara Benali" })).toBeInTheDocument();
    expect(screen.getByText("Arriving late")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "WhatsApp" })).toHaveAttribute("href", "https://wa.me/212600000000");
  });

  it("confirms after the owner agrees in the dialog", async () => {
    vi.mocked(adminApi.confirm).mockResolvedValue({ booking: { ...booking, status: "CONFIRMED" }, warnings: [] });
    const user = userEvent.setup();
    renderDetail();

    await user.click(await screen.findByRole("button", { name: "Confirm booking" }));
    await user.click(screen.getByRole("button", { name: "Confirm" }));

    expect(adminApi.confirm).toHaveBeenCalledWith("b1");
    expect(await screen.findByText("Booking confirmed. The guest has been emailed.")).toBeInTheDocument();
  });

  it("shows which platform reservation blocks a confirmation (409)", async () => {
    vi.mocked(adminApi.confirm).mockRejectedValue(new ApiError({
      status: 409,
      detail: "These dates are blocked by another reservation or by the owner",
      conflicts: [{ id: "x", source: "ICAL", channelName: "Airbnb", startDate: "2027-03-12", endDate: "2027-03-16", reason: null }],
    } as never));
    const user = userEvent.setup();
    renderDetail();

    await user.click(await screen.findByRole("button", { name: "Confirm booking" }));
    await user.click(screen.getByRole("button", { name: "Confirm" }));

    expect(await screen.findByText(/Airbnb reservation/)).toBeInTheDocument();
    expect(screen.getByText(/decline this request politely/)).toBeInTheDocument();
  });

  it("declines with the owner's message", async () => {
    vi.mocked(adminApi.decline).mockResolvedValue({ ...booking, status: "DECLINED" });
    const user = userEvent.setup();
    renderDetail();

    await user.click(await screen.findByRole("button", { name: "Decline" }));
    await user.type(screen.getByLabelText("Message to the guest (optional)"), "Sorry, full that week.");
    await user.click(screen.getByRole("button", { name: "Decline request" }));

    expect(adminApi.decline).toHaveBeenCalledWith("b1", "Sorry, full that week.");
    expect(await screen.findByText("Request declined. The guest has been emailed.")).toBeInTheDocument();
  });
});
