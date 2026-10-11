import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "./api";

afterEach(() => vi.unstubAllGlobals());

function mockFetch(status: number, body: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("api", () => {
  it("calls the backend through the /api rewrite", async () => {
    const fetchMock = mockFetch(200, { total: 640 });

    const quote = await api.quote("p1", "2027-03-10", "2027-03-14", 2);

    expect(quote.total).toBe(640);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/properties/p1/quote?checkIn=2027-03-10&checkOut=2027-03-14&guests=2",
      expect.anything(),
    );
  });

  it("turns problem details into an ApiError", async () => {
    mockFetch(409, { status: 409, detail: "Booking cannot be accepted", issues: ["DATES_UNAVAILABLE"] });

    const error = await api
      .createBooking({
        propertyId: "p1",
        checkIn: "2027-03-10",
        checkOut: "2027-03-14",
        guests: 2,
        guest: { fullName: "Sara", email: "sara@example.com", language: "fr" },
      })
      .catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(409);
    expect((error as ApiError).problem.issues).toEqual(["DATES_UNAVAILABLE"]);
  });
});
