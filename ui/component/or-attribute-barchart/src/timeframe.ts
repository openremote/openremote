import moment from "moment";

export type TimeframeDirection = "previous" | "next";

export type TimeframeUnit = moment.unitOfTime.DurationConstructor;

const CALENDAR_UNITS = new Set<TimeframeUnit>(["days", "weeks", "months", "quarters", "years"]);

export function shiftTimeframeByDuration(
  currentStart: Date,
  currentEnd: Date,
  duration: number,
  direction: TimeframeDirection
): [Date, Date] {
  const offset = direction === "previous" ? -duration : duration;
  return [new Date(currentStart.getTime() + offset), new Date(currentEnd.getTime() + offset)];
}

export function shiftTimeframe(
  currentStart: Date,
  currentEnd: Date,
  duration: number,
  unit: TimeframeUnit,
  value: number,
  direction: TimeframeDirection
): [Date, Date] {
  if (!CALENDAR_UNITS.has(unit)) {
    return shiftTimeframeByDuration(currentStart, currentEnd, duration, direction);
  }

  const start = moment(currentStart);
  const end = moment(currentEnd);
  direction === "previous" ? start.subtract(value, unit) : start.add(value, unit);
  direction === "previous" ? end.subtract(value, unit) : end.add(value, unit);
  return [start.toDate(), end.toDate()];
}
