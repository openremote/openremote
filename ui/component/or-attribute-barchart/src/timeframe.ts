export type TimeframeDirection = "previous" | "next";

export function shiftTimeframeByDuration(
  currentStart: Date,
  currentEnd: Date,
  duration: number,
  direction: TimeframeDirection
): [Date, Date] {
  const offset = direction === "previous" ? -duration : duration;
  return [new Date(currentStart.getTime() + offset), new Date(currentEnd.getTime() + offset)];
}
