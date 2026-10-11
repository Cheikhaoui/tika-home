import { describe, expect, it } from "vitest";
import { formatDate, formatMoney, interpolate, plural } from "./format";

describe("interpolate", () => {
  it("replaces known placeholders and keeps unknown ones", () => {
    expect(interpolate("Hello {name}, {missing}", { name: "Sara" })).toBe("Hello Sara, {missing}");
  });
});

describe("plural", () => {
  const nightsEn = { one: "{count} night", other: "{count} nights" };
  const nightsAr = {
    zero: "لا ليالي",
    one: "ليلة واحدة",
    two: "ليلتان",
    few: "{count} ليالٍ",
    many: "{count} ليلة",
    other: "{count} ليلة",
  };

  it("uses English forms", () => {
    expect(plural("en", 1, nightsEn)).toBe("1 night");
    expect(plural("en", 3, nightsEn)).toBe("3 nights");
  });

  it("uses the six Arabic forms", () => {
    expect(plural("ar", 1, nightsAr)).toBe("ليلة واحدة");
    expect(plural("ar", 2, nightsAr)).toBe("ليلتان");
    expect(plural("ar", 5, nightsAr)).toBe("5 ليالٍ"); // 3-10: few
    expect(plural("ar", 11, nightsAr)).toBe("11 ليلة"); // 11-99: many
  });

  it("falls back to 'other' when a form is missing", () => {
    expect(plural("ar", 5, nightsEn)).toBe("5 nights");
  });
});

describe("formatMoney", () => {
  it("formats per locale, with Latin digits in Moroccan Arabic", () => {
    expect(formatMoney("en", 640, "EUR")).toBe("€640");
    expect(formatMoney("fr", 640.5, "EUR")).toMatch(/^640,50\s€$/u);
    expect(formatMoney("ar", 640, "EUR")).toContain("640");
  });
});

describe("formatDate", () => {
  it("formats a calendar date without shifting it to the previous day", () => {
    expect(formatDate("en", "2027-03-10", { day: "numeric", month: "long", year: "numeric" })).toBe("10 March 2027");
    expect(formatDate("fr", "2027-03-10", { day: "numeric", month: "long" })).toBe("10 mars");
  });
});
