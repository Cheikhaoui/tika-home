import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import en from "@/app/[lang]/dictionaries/en.json";
import { api, ApiError, type Quote } from "@/lib/api";
import { BookingWidget } from "./BookingWidget";

// Replace only the network calls; ApiError and the types stay real.
vi.mock("@/lib/api", async (importActual) => {
  const actual = await importActual<typeof import("@/lib/api")>();
  return {
    ...actual,
    api: { property: vi.fn(), availability: vi.fn(), quote: vi.fn(), createBooking: vi.fn() },
  };
});

const mocked = vi.mocked(api);

const property = {
  id: "p1",
  name: "Tika Home",
  maxGuests: 6,
  baseNightlyPrice: 120,
  cleaningFee: 40,
  currency: "EUR",
  minNights: 2,
};

function quote(overrides: Partial<Quote> = {}): Quote {
  return {
    checkIn: "2027-03-05",
    checkOut: "2027-03-08",
    nights: 3,
    guests: 2,
    currency: "EUR",
    nightly: ["2027-03-05", "2027-03-06", "2027-03-07"].map((date) => ({ date, price: 120, rule: null })),
    subtotal: 360,
    cleaningFee: 40,
    total: 400,
    minNights: 2,
    bookable: true,
    issues: [],
    ...overrides,
  };
}

function renderWidget() {
  return render(<BookingWidget locale="en" propertyId="p1" labels={en.booking} />);
}

// Day buttons are labelled like "Friday, 5 March 2027": match after the comma so 5 != 15 or 25.
const day = (label: string) => screen.findByRole("button", { name: new RegExp(`, ${label}`) });

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"] }); // freeze "today" only; timers keep running for userEvent
  vi.setSystemTime(new Date("2027-03-01T10:00:00Z"));
  mocked.property.mockResolvedValue(property);
  mocked.availability.mockResolvedValue({
    propertyId: "p1",
    from: "2027-03-01",
    to: "2028-02-29",
    unavailable: [{ start: "2027-03-10", end: "2027-03-15" }],
    days: [],
  });
  mocked.quote.mockResolvedValue(quote());
});

afterEach(() => {
  vi.useRealTimers();
  vi.clearAllMocks();
});

async function chooseDates(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await day("5 March 2027"));
  await user.click(await day("8 March 2027"));
}

describe("BookingWidget", () => {
  it("shows the backend price once dates are chosen", async () => {
    const user = userEvent.setup();
    renderWidget();

    expect(await screen.findByText("From €120 per night")).toBeInTheDocument();
    await chooseDates(user);

    expect(await screen.findByText("€400")).toBeInTheDocument();
    expect(mocked.quote).toHaveBeenCalledWith("p1", "2027-03-05", "2027-03-08", 2, expect.any(AbortSignal));
    expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled();
  });

  it("does not let guests arrive on a booked night", async () => {
    renderWidget();

    expect(await day("12 March 2027")).toBeDisabled();
    expect(await day("15 March 2027")).toBeEnabled(); // previous guest leaves that morning
  });

  it("shows translated backend issues and blocks Continue", async () => {
    mocked.quote.mockResolvedValue(quote({ bookable: false, issues: ["BELOW_MIN_NIGHTS"], minNights: 3 }));
    const user = userEvent.setup();
    renderWidget();

    await chooseDates(user);

    expect(await screen.findByText("The minimum stay for these dates is 3 nights.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
  });

  it("validates the form before calling the API, then shows the confirmation", async () => {
    mocked.createBooking.mockResolvedValue({
      id: "b1",
      reference: "TK-ABC123",
      status: "REQUESTED",
      checkIn: "2027-03-05",
      checkOut: "2027-03-08",
      guests: 2,
      totalPrice: 400,
      currency: "EUR",
    });
    const user = userEvent.setup();
    renderWidget();
    await chooseDates(user);
    await user.click(await screen.findByRole("button", { name: "Continue" }));

    await user.type(screen.getByLabelText("Full name"), "Sara Benali");
    await user.type(screen.getByLabelText("Email"), "not-an-email");
    await user.click(screen.getByRole("button", { name: "Send booking request" }));
    expect(screen.getByText("Please enter a valid email address.")).toBeInTheDocument();
    expect(mocked.createBooking).not.toHaveBeenCalled();

    await user.clear(screen.getByLabelText("Email"));
    await user.type(screen.getByLabelText("Email"), "sara@example.com");
    await user.click(screen.getByRole("button", { name: "Send booking request" }));

    expect(await screen.findByText("Request sent!")).toBeInTheDocument();
    expect(screen.getByText(/Your request TK-ABC123 has been sent/)).toBeInTheDocument();
    expect(mocked.createBooking).toHaveBeenCalledWith({
      propertyId: "p1",
      checkIn: "2027-03-05",
      checkOut: "2027-03-08",
      guests: 2,
      guest: { fullName: "Sara Benali", email: "sara@example.com", phone: undefined, language: "en" },
      message: undefined,
    });
  });

  it("handles dates taken by someone else in the meantime (409)", async () => {
    mocked.createBooking.mockRejectedValue(new ApiError({ status: 409, issues: ["DATES_UNAVAILABLE"] }));
    const user = userEvent.setup();
    renderWidget();
    await chooseDates(user);
    await user.click(await screen.findByRole("button", { name: "Continue" }));
    await user.type(screen.getByLabelText("Full name"), "Sara Benali");
    await user.type(screen.getByLabelText("Email"), "sara@example.com");

    await user.click(screen.getByRole("button", { name: "Send booking request" }));

    expect(await screen.findByText(en.booking.errors.conflict)).toBeInTheDocument();
    await waitFor(() => expect(mocked.availability).toHaveBeenCalledTimes(2)); // calendar refreshed
    expect(screen.getAllByText("Add a date")).toHaveLength(2); // dates cleared
  });
});
