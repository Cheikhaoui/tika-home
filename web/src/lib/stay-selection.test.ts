import { describe, expect, it } from "vitest";
import { bookedNights, isDayDisabled, latestCheckOut, selectDay } from "./stay-selection";

const today = "2027-03-01";
const lastDay = "2028-03-01";
// Someone stays 10 -> 15: nights 10, 11, 12, 13, 14 are booked.
const booked = bookedNights([{ start: "2027-03-10", end: "2027-03-15" }]);

describe("bookedNights", () => {
  it("expands ranges to nights, excluding the departure day", () => {
    expect([...booked]).toEqual(["2027-03-10", "2027-03-11", "2027-03-12", "2027-03-13", "2027-03-14"]);
  });
});

describe("latestCheckOut", () => {
  it("is the first booked night (you leave that morning)", () => {
    expect(latestCheckOut("2027-03-05", booked, lastDay)).toBe("2027-03-10");
  });

  it("allows a one-night stay right before a booking", () => {
    expect(latestCheckOut("2027-03-09", booked, lastDay)).toBe("2027-03-10");
  });
});

describe("selectDay", () => {
  it("first click picks the arrival, second the departure", () => {
    const afterFirst = selectDay({}, "2027-03-05", booked, today, lastDay);
    expect(afterFirst).toEqual({ checkIn: "2027-03-05" });
    expect(selectDay(afterFirst, "2027-03-08", booked, today, lastDay)).toEqual({
      checkIn: "2027-03-05",
      checkOut: "2027-03-08",
    });
  });

  it("allows checking out on the arrival day of the next guest", () => {
    expect(selectDay({ checkIn: "2027-03-07" }, "2027-03-10", booked, today, lastDay)).toEqual({
      checkIn: "2027-03-07",
      checkOut: "2027-03-10",
    });
  });

  it("cannot arrive on a booked night", () => {
    expect(selectDay({}, "2027-03-12", booked, today, lastDay)).toEqual({});
  });

  it("allows arriving on the departure day of the previous guest", () => {
    expect(selectDay({}, "2027-03-15", booked, today, lastDay)).toEqual({ checkIn: "2027-03-15" });
  });

  it("clicking before the arrival starts a new selection", () => {
    expect(selectDay({ checkIn: "2027-03-05" }, "2027-03-03", booked, today, lastDay)).toEqual({
      checkIn: "2027-03-03",
    });
  });

  it("a third click starts over", () => {
    const full = { checkIn: "2027-03-05", checkOut: "2027-03-08" };
    expect(selectDay(full, "2027-03-20", booked, today, lastDay)).toEqual({ checkIn: "2027-03-20" });
  });

  it("cannot arrive in the past", () => {
    expect(selectDay({}, "2027-02-20", booked, today, lastDay)).toEqual({});
  });
});

describe("isDayDisabled", () => {
  it("greys out booked nights when choosing the arrival", () => {
    expect(isDayDisabled({}, "2027-03-10", booked, today, lastDay)).toBe(true);
    expect(isDayDisabled({}, "2027-03-09", booked, today, lastDay)).toBe(false);
  });

  it("when choosing the departure, enables the first booked day and disables beyond it", () => {
    const stay = { checkIn: "2027-03-07" };
    expect(isDayDisabled(stay, "2027-03-10", booked, today, lastDay)).toBe(false);
    expect(isDayDisabled(stay, "2027-03-11", booked, today, lastDay)).toBe(true);
  });
});
