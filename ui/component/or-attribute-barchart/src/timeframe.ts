import moment from "moment";

export type TimeframeDirection = "previous" | "next";

export type TimeframeUnit = moment.unitOfTime.DurationConstructor;

const CALENDAR_UNITS = new Set(["day", "week", "month", "quarter", "year"]);

function normalizeUnit(unit: TimeframeUnit): string {
  return (moment as typeof moment & { normalizeUnits: (value: TimeframeUnit) => string }).normalizeUnits(unit);
}

export function getChartAxisBounds(start: number, end: number, _dataset?: unknown): [number, number] {
  return [start, end];
}

export function getNavigationDuration(
  start: number,
  end: number,
  isCustomTimeframe: boolean,
  unit: TimeframeUnit,
  value: number
): number {
  return isCustomTimeframe ? end - start : moment.duration(value, unit).asMilliseconds();
}

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
  const normalizedUnit = normalizeUnit(unit);
  if (!CALENDAR_UNITS.has(normalizedUnit)) {
    return shiftTimeframeByDuration(currentStart, currentEnd, duration, direction);
  }

  const start = moment(currentStart);
  const end = moment(currentEnd);
  direction === "previous" ? start.subtract(value, normalizedUnit) : start.add(value, normalizedUnit);
  direction === "previous" ? end.subtract(value, normalizedUnit) : end.add(value, normalizedUnit);
  return [start.toDate(), end.toDate()];
}
