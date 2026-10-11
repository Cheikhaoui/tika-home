import { expect, test, type Page } from "@playwright/test";

/**
 * Real backend for availability and prices; the final POST /api/bookings is intercepted so test runs
 * don't create real bookings (and emails) in the dev database. The real POST is covered by the
 * backend's BookingApiIT / BookingEmailIT.
 */

// Pick an arrival 4-7 months ahead, where the calendar is usually free.
async function openFutureMonth(page: Page) {
  const months = 4 + Math.floor(Math.random() * 4);
  for (let i = 0; i < months; i++) {
    await page.locator("#booking .rdp-button_next").click();
  }
}

/** Clicks the first free arrival day whose stay of `nights` is also free; returns [checkIn, checkOut]. */
async function pickStay(page: Page, nights: number): Promise<[string, string]> {
  const days = page.locator("#booking [data-day]:not([data-disabled]):not([data-outside]) button");
  const count = await days.count();
  for (let i = 0; i < count; i++) {
    const checkIn = (await days.nth(i).locator("xpath=..").getAttribute("data-day"))!;
    const d = new Date(`${checkIn}T12:00:00Z`);
    d.setUTCDate(d.getUTCDate() + nights);
    const checkOut = d.toISOString().slice(0, 10);
    await days.nth(i).click();
    const departure = page.locator(`#booking [data-day="${checkOut}"] button`);
    if ((await departure.count()) > 0 && (await departure.first().isEnabled())) {
      await departure.first().click();
      return [checkIn, checkOut];
    }
  }
  throw new Error("No free stay found in this month");
}

test("a guest picks dates, sees the price and sends a request (French)", async ({ page }) => {
  await page.route("**/api/bookings", async (route) => {
    const body = route.request().postDataJSON();
    expect(body.guest).toMatchObject({ fullName: "Sara Benali", email: "sara@example.com", language: "fr" });
    await route.fulfill({
      status: 201,
      json: { id: "e2e", reference: "TK-E2E001", status: "REQUESTED", checkIn: body.checkIn, checkOut: body.checkOut,
        guests: body.guests, totalPrice: 400, currency: "EUR" },
    });
  });

  await page.goto("/fr");
  await expect(page.locator("html")).toHaveAttribute("lang", "fr");
  await page.getByRole("link", { name: "Voir les disponibilités" }).click();

  const booking = page.locator("#booking");
  await expect(booking.getByText(/À partir de .* la nuit/)).toBeVisible(); // property loaded from the API
  await openFutureMonth(page);
  await pickStay(page, 3);

  await expect(booking.getByText("Total")).toBeVisible(); // quote from the API
  await booking.getByRole("button", { name: "Continuer" }).click();

  await booking.getByLabel("Nom complet").fill("Sara Benali");
  await booking.getByLabel("E-mail").fill("sara@example.com");
  await booking.getByRole("button", { name: "Envoyer la demande" }).click();

  await expect(booking.getByText("Demande envoyée !")).toBeVisible();
  await expect(booking.getByText(/TK-E2E001/)).toBeVisible();
});

test("dates taken in the meantime: the guest is asked to choose again", async ({ page }) => {
  await page.route("**/api/bookings", (route) =>
    route.fulfill({ status: 409, json: { status: 409, detail: "taken", issues: ["DATES_UNAVAILABLE"] } }),
  );

  await page.goto("/en#booking");
  await openFutureMonth(page);
  await pickStay(page, 3);
  const booking = page.locator("#booking");
  await booking.getByRole("button", { name: "Continue" }).click();
  await booking.getByLabel("Full name").fill("Sara Benali");
  await booking.getByLabel("Email").fill("sara@example.com");
  await booking.getByRole("button", { name: "Send booking request" }).click();

  await expect(booking.getByText(/these dates were just booked by someone else/)).toBeVisible();
  await expect(booking.getByText("Add a date")).toHaveCount(2);
});

test("Arabic page is right-to-left with links to the other languages", async ({ page }) => {
  await page.goto("/ar");
  await expect(page.locator("html")).toHaveAttribute("dir", "rtl");
  await expect(page.locator('link[rel="alternate"][hreflang="fr"]')).toHaveAttribute("href", /\/fr$/);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("رياضك الخاص في قلب مراكش");
});

test("visitors without a language are sent to their browser language", async ({ browser }) => {
  const context = await browser.newContext({ locale: "ar-MA", extraHTTPHeaders: { "Accept-Language": "ar-MA,ar;q=0.9" } });
  const page = await context.newPage();
  await page.goto("/");
  await expect(page).toHaveURL(/\/ar$/);
  await context.close();
});
