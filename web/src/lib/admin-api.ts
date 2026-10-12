// Typed client for /api/admin/**. The session is the HttpOnly cookie: the browser sends it automatically
// (same origin thanks to the /api rewrite) and JavaScript never sees the token.
import { propertyId } from "@/config/site";
import { ApiError, type Problem } from "./api";
import type { IsoDate } from "./dates";

export type BookingStatus = "REQUESTED" | "CONFIRMED" | "DECLINED" | "CANCELLED";
export type BookingView = "PENDING" | "UPCOMING" | "PAST" | "CLOSED";
export type BlockSource = "MANUAL" | "ICAL";
export type ChannelType = "AIRBNB" | "BOOKING_COM" | "VRBO" | "OTHER";
export type SyncStatus = "SUCCESS" | "FAILED";

export type Me = { email: string; expiresAt: string };

export type BookingSummary = {
  id: string;
  reference: string;
  status: BookingStatus;
  checkIn: IsoDate;
  checkOut: IsoDate;
  nights: number;
  guests: number;
  totalPrice: number;
  currency: string;
  guestName: string;
  guestEmail: string;
  createdAt: string;
};

export type BlockSummary = {
  id: string;
  source: BlockSource;
  channelName: string | null;
  startDate: IsoDate;
  endDate: IsoDate;
  reason: string | null;
};

export type BookingDetail = Omit<BookingSummary, "guestName" | "guestEmail"> & {
  message: string | null;
  updatedAt: string;
  guest: { fullName: string; email: string; phone: string | null; language: string };
  conflicts: BlockSummary[];
};

export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number };

export type ChannelDto = {
  id: string;
  type: ChannelType;
  name: string;
  importUrlMasked: string;
  enabled: boolean;
  lastSyncedAt: string | null;
  lastSuccessAt: string | null;
  lastSyncStatus: SyncStatus | null;
  lastError: string | null;
  lastConflictCount: number;
  consecutiveFailures: number;
};

export type SyncResult = {
  status: SyncStatus;
  created: number;
  updated: number;
  deleted: number;
  conflictingBookingIds: string[];
  error: string | null;
};

export type Dashboard = {
  pendingCount: number;
  pending: BookingSummary[];
  currentStay: BookingSummary | null;
  nextArrivals: BookingSummary[];
  conflicts: { booking: BookingSummary; platformReservation: BlockSummary }[];
  failingChannels: ChannelDto[];
};

export type PropertySettings = { baseNightlyPrice: number; cleaningFee: number; minNights: number; maxGuests: number };
export type PriceRule = {
  id?: string;
  name: string;
  startDate: IsoDate;
  endDate: IsoDate;
  nightlyPrice: number;
  minNights: number | null;
  priority: number;
};
export type Pricing = { currency: string; settings: PropertySettings; rules: PriceRule[] };

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api/admin${path}`, {
    ...init,
    credentials: "same-origin",
    headers: { Accept: "application/json", ...(init?.body ? { "Content-Type": "application/json" } : {}) },
  });
  if (response.status === 401 && !path.startsWith("/auth/")) {
    // Session expired: back to the login page, then return here after logging in. A full reload on purpose:
    // this module isn't a component (no router) and the reload clears cached admin data.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.assign(`/admin/login?next=${encodeURIComponent(window.location.pathname)}`);
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Partial<Problem> & Record<string, unknown>;
    throw new ApiError({ ...problem, status: response.status });
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

const json = (body: unknown) => JSON.stringify(body);
const p = `/properties/${propertyId}`;

export const adminApi = {
  login: (email: string, password: string) =>
    request<Me>("/auth/login", { method: "POST", body: json({ email, password }) }),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
  me: () => request<Me>("/auth/me"),

  dashboard: () => request<Dashboard>(`${p}/dashboard`),

  bookings: (view: BookingView, page: number) => request<Page<BookingSummary>>(`/bookings?view=${view}&page=${page}&size=20`),
  booking: (id: string) => request<BookingDetail>(`/bookings/${id}`),
  confirm: (id: string) =>
    request<{ booking: BookingDetail; warnings: string[] }>(`/bookings/${id}/confirm`, { method: "POST" }),
  decline: (id: string, message: string) =>
    request<BookingDetail>(`/bookings/${id}/decline`, { method: "POST", body: json({ message: message || null }) }),

  calendar: (from: IsoDate, to: IsoDate) =>
    request<{ bookings: BookingSummary[]; blocks: BlockSummary[] }>(`${p}/calendar?from=${from}&to=${to}`),
  block: (startDate: IsoDate, endDate: IsoDate, reason: string) =>
    request<BlockSummary>(`${p}/blocked-dates`, { method: "POST", body: json({ startDate, endDate, reason }) }),
  unblock: (blockId: string) => request<void>(`/blocked-dates/${blockId}`, { method: "DELETE" }),

  pricing: () => request<Pricing>(`${p}/pricing`),
  saveSettings: (settings: PropertySettings) =>
    request<Pricing>(`${p}/settings`, { method: "PUT", body: json(settings) }),
  createRule: (rule: PriceRule) => request<PriceRule>(`${p}/price-rules`, { method: "POST", body: json(rule) }),
  updateRule: (rule: PriceRule) => request<PriceRule>(`/price-rules/${rule.id}`, { method: "PUT", body: json(rule) }),
  deleteRule: (id: string) => request<void>(`/price-rules/${id}`, { method: "DELETE" }),

  channels: () => request<{ exportUrl: string; channels: ChannelDto[] }>(`${p}/channels`),
  createChannel: (type: ChannelType, name: string, importUrl: string) =>
    request<{ result: SyncResult; channel: ChannelDto }>(`${p}/channels`, {
      method: "POST",
      body: json({ type, name, importUrl }),
    }),
  updateChannel: (c: ChannelDto, changes: { name?: string; importUrl?: string; enabled?: boolean }) =>
    request<ChannelDto>(`/channels/${c.id}`, {
      method: "PUT",
      body: json({ type: c.type, name: changes.name ?? c.name, importUrl: changes.importUrl ?? "", enabled: changes.enabled ?? c.enabled }),
    }),
  deleteChannel: (id: string) => request<void>(`/channels/${id}`, { method: "DELETE" }),
  syncChannel: (id: string) => request<{ result: SyncResult; channel: ChannelDto }>(`/channels/${id}/sync`, { method: "POST" }),
};

/** Human-readable message for an error thrown by adminApi. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const errors = (error.problem as Problem & { errors?: Record<string, string> }).errors;
    if (errors) return Object.entries(errors).map(([field, msg]) => `${field}: ${msg}`).join(", ");
    return error.problem.detail ?? `Error ${error.status}`;
  }
  return "Network error: is the server running?";
}
