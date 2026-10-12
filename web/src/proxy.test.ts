import { describe, expect, it } from "vitest";
import { NextRequest } from "next/server";
import { ADMIN_COOKIE, proxy } from "./proxy";

const request = (path: string, init?: { cookie?: string; language?: string }) =>
  new NextRequest(new URL(path, "http://localhost:3000"), {
    headers: {
      ...(init?.cookie ? { cookie: init.cookie } : {}),
      ...(init?.language ? { "accept-language": init.language } : {}),
    },
  });

describe("proxy", () => {
  it("sends visitors without a session to the admin login, remembering the page", () => {
    const response = proxy(request("/admin/bookings"));
    expect(response?.headers.get("location")).toBe("http://localhost:3000/admin/login?next=%2Fadmin%2Fbookings");
  });

  it("lets logged-in owners through (the backend still checks the token)", () => {
    expect(proxy(request("/admin/bookings", { cookie: `${ADMIN_COOKIE}=abc` }))).toBeUndefined();
  });

  it("never redirects the login page itself", () => {
    expect(proxy(request("/admin/login"))).toBeUndefined();
  });

  it("does not add a language prefix to admin pages", () => {
    expect(proxy(request("/admin", { cookie: `${ADMIN_COOKIE}=abc`, language: "fr" }))).toBeUndefined();
  });

  it("still localises public pages", () => {
    expect(proxy(request("/", { language: "ar-MA,ar;q=0.9" }))?.headers.get("location")).toBe("http://localhost:3000/ar");
  });
});
