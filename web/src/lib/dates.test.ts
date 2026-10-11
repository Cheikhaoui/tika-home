import { describe, expect, it } from "vitest";
import { addDays, fromIsoDate, nightsBetween, toIsoDate } from "./dates";

describe("dates", () => {
  it("round-trips calendar dates without time-zone shifts", () => {
    expect(toIsoDate(fromIsoDate("2027-03-10"))).toBe("2027-03-10");
  });

  it("adds days across month and year boundaries", () => {
    expect(addDays("2027-02-27", 2)).toBe("2027-03-01");
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
  });

  it("counts nights, also across a daylight-saving change", () => {
    expect(nightsBetween("2027-03-10", "2027-03-14")).toBe(4);
    expect(nightsBetween("2027-03-26", "2027-03-30")).toBe(4); // Europe switches clocks on 28 March
  });
});
