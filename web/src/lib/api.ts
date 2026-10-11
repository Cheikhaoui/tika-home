// Typed client for the Spring Boot API. The browser calls /api/... on our own domain and
// Next.js forwards it to the backend (see rewrites in next.config.ts).
import type { IsoDate } from "./dates";

export type QuoteIssue =
  | "DATES_UNAVAILABLE"
  | "CHECK_IN_IN_PAST"
  | "BELOW_MIN_NIGHTS"
  | "ABOVE_MAX_NIGHTS"
  | "TOO_MANY_GUESTS";

export type PropertySummary = {
  id: string;
  name: string;
  maxGuests: number;
  baseNightlyPrice: number;
  cleaningFee: number;
  currency: string;
  minNights: number;
};

export type DateRange = { start: IsoDate; end: IsoDate }; // end exclusive

export type Availability = {
  propertyId: string;
  from: IsoDate;
  to: IsoDate;
  unavailable: DateRange[];
  days: { date: IsoDate; available: boolean }[];
};

export type Quote = {
  checkIn: IsoDate;
  checkOut: IsoDate;
  nights: number;
  guests: number;
  currency: string;
  nightly: { date: IsoDate; price: number; rule: string | null }[];
  subtotal: number;
  cleaningFee: number;
  total: number;
  minNights: number;
  bookable: boolean;
  issues: QuoteIssue[];
};

export type BookingRequest = {
  propertyId: string;
  checkIn: IsoDate;
  checkOut: IsoDate;
  guests: number;
  guest: { fullName: string; email: string; phone?: string; language: string };
  message?: string;
};

export type Booking = {
  id: string;
  reference: string;
  status: "REQUESTED" | "CONFIRMED" | "DECLINED" | "CANCELLED";
  checkIn: IsoDate;
  checkOut: IsoDate;
  guests: number;
  totalPrice: number;
  currency: string;
};

/** RFC 9457 problem details returned by the backend, with our extra fields. */
export type Problem = {
  status: number;
  title?: string;
  detail?: string;
  issues?: QuoteIssue[];
  errors?: Record<string, string>;
};

export class ApiError extends Error {
  constructor(public readonly problem: Problem) {
    super(problem.detail ?? `HTTP ${problem.status}`);
  }

  get status() {
    return this.problem.status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...init,
    headers: { Accept: "application/json", ...(init?.body ? { "Content-Type": "application/json" } : {}) },
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Partial<Problem>;
    throw new ApiError({ ...problem, status: response.status });
  }
  return (await response.json()) as T;
}

export const api = {
  property: (propertyId: string, signal?: AbortSignal) =>
    request<PropertySummary>(`/properties/${propertyId}`, { signal }),

  availability: (propertyId: string, from: IsoDate, to: IsoDate, signal?: AbortSignal) =>
    request<Availability>(`/properties/${propertyId}/availability?from=${from}&to=${to}`, { signal }),

  quote: (propertyId: string, checkIn: IsoDate, checkOut: IsoDate, guests: number, signal?: AbortSignal) =>
    request<Quote>(
      `/properties/${propertyId}/quote?checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}`,
      { signal },
    ),

  createBooking: (body: BookingRequest) =>
    request<Booking>("/bookings", { method: "POST", body: JSON.stringify(body) }),
};
