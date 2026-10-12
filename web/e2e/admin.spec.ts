import { expect, test } from "@playwright/test";

/**
 * The owner's journey against the real backend: a guest request arrives, the owner logs in and
 * declines it with a message. Uses dates two years ahead and declines its own booking, so no dates
 * stay held in the dev database. Credentials: the local dev admin from backend application.yml
 * (override with E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD).
 */
const email = process.env.E2E_ADMIN_EMAIL ?? "cheikhaoui.hicham@gmail.com";
const password = process.env.E2E_ADMIN_PASSWORD ?? "local-dev-password";
const propertyId = "6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b";

test.use({ viewport: { width: 1280, height: 900 } });

test("owner logs in, opens a request and declines it with a message", async ({ page, request }) => {
  // A guest request, through the public API (2 years ahead, random week to avoid clashes between runs).
  const start = new Date();
  start.setUTCDate(start.getUTCDate() + 730 + Math.floor(Math.random() * 200));
  const end = new Date(start);
  end.setUTCDate(end.getUTCDate() + 3);
  const iso = (d: Date) => d.toISOString().slice(0, 10);
  const created = await request.post("/api/bookings", {
    data: {
      propertyId, checkIn: iso(start), checkOut: iso(end), guests: 2,
      guest: { fullName: "E2E Owner Test", email: "e2e.owner@example.com", language: "en" },
    },
  });
  expect(created.status()).toBe(201);
  const booking = await created.json();

  // Not logged in: the admin redirects to the login page.
  await page.goto(`/admin/bookings/${booking.id}`);
  await expect(page).toHaveURL(/\/admin\/login\?next=/);

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();

  // Back on the booking we wanted to see.
  await expect(page.getByRole("heading", { name: "E2E Owner Test" })).toBeVisible();
  await expect(page.getByText(booking.reference)).toBeVisible();

  await page.getByRole("button", { name: "Decline" }).click();
  await page.getByLabel("Message to the guest (optional)").fill("Sorry, the house is not available.");
  await page.getByRole("button", { name: "Decline request" }).click();

  await expect(page.getByText("Request declined. The guest has been emailed.")).toBeVisible();
  await expect(page.getByText("Declined", { exact: true })).toBeVisible();
});

test("the admin is never indexed and needs a login", async ({ page }) => {
  await page.context().clearCookies();
  await page.goto("/admin");
  await expect(page).toHaveURL(/\/admin\/login/);
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
});
