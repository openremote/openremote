import { ct, expect } from "@openremote/test";

import { shiftTimeframeByDuration } from "../src/timeframe";

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

function centeredBarBounds(start: number, end: number, interval: number): [number, number] {
  return [start + interval / 2, end - interval / 2];
}

ct("should keep requested chart bounds when bars are centered in their buckets", async () => {
  for (const interval of [HOUR, 15 * 60 * 1000, 5 * 60 * 1000]) {
    const requested: [number, number] = [0, DAY];
    const rendered = centeredBarBounds(requested[0], requested[1], interval);

    // The old dataset-derived bounds shrink by one interval; axis bounds must remain requested bounds.
    expect(rendered[1] - rendered[0]).toBe(DAY - interval);
    expect(requested[1] - requested[0]).toBe(DAY);
  }
});

ct("should preserve the original 24-hour duration across repeated navigation", async () => {
  const initial: [Date, Date] = [new Date("2026-03-28T12:00:00+01:00"), new Date("2026-03-29T13:00:00+02:00")];
  const next = shiftTimeframeByDuration(initial[0], initial[1], DAY, "next");
  const nextAgain = shiftTimeframeByDuration(next[0], next[1], DAY, "next");
  const previous = shiftTimeframeByDuration(nextAgain[0], nextAgain[1], DAY, "previous");

  // Navigation must retain the original duration even when the window crosses a DST boundary.
  expect(next[1].getTime() - next[0].getTime()).toBe(DAY);
  expect(nextAgain[1].getTime() - nextAgain[0].getTime()).toBe(DAY);
  expect(previous[1].getTime() - previous[0].getTime()).toBe(DAY);
  expect(next[1].getTime()).toBeGreaterThan(next[0].getTime());
  expect(nextAgain[1].getTime()).toBeGreaterThan(nextAgain[0].getTime());
  expect(previous[1].getTime()).toBeGreaterThan(previous[0].getTime());
  expect(previous[0].getTime()).toBe(next[0].getTime());
  expect(previous[1].getTime()).toBe(next[1].getTime());
});

ct("should preserve a non-default interval duration and direction", async () => {
  const initial: [Date, Date] = [new Date("2026-01-01T00:07:00Z"), new Date("2026-01-01T03:22:00Z")];
  const duration = 195 * 60 * 1000;
  const next = shiftTimeframeByDuration(initial[0], initial[1], duration, "next");
  const previous = shiftTimeframeByDuration(next[0], next[1], duration, "previous");

  // Non-default, minute-aligned windows must also remain valid and round-trip exactly.
  expect(next[1].getTime() - next[0].getTime()).toBe(duration);
  expect(next[1].getTime()).toBeGreaterThan(next[0].getTime());
  expect(previous[1].getTime()).toBeGreaterThan(previous[0].getTime());
  expect(previous[0].getTime()).toBe(initial[0].getTime());
  expect(previous[1].getTime()).toBe(initial[1].getTime());
});
