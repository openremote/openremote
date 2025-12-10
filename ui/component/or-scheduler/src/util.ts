import { Frequency } from "rrule";

export const FREQUENCIES = {
    YEARLY: "rrule.frequency.YEARLY",
    MONTHLY: "rrule.frequency.MONTHLY",
    WEEKLY: "rrule.frequency.WEEKLY",
    DAILY: "rrule.frequency.DAILY",
    HOURLY: "rrule.frequency.HOURLY",
    MINUTELY: "rrule.frequency.MINUTELY",
    SECONDLY: "rrule.frequency.SECONDLY",
} as Record<keyof typeof Frequency, string>;

/**
 * Evaluation order: BYMONTH, BYWEEKNO, BYYEARDAY, BYMONTHDAY, BYDAY, BYHOUR, BYMINUTE and BYSECOND.
 * As per https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10 page 44
 */
export const BY_RRULE_PARTS = [
    "bymonth",
    "byweekno",
    "byyearday",
    "bymonthday",
    "byweekday",
    "byhour",
    "byminute",
    "bysecond",
] as const;

/**
 * Dependency of by rule parts table
 * As per https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10 page 44
 */
export const NOT_APPLICABLE_BY_RRULE_PARTS = {
    SECONDLY: ["BYWEEKNO"],
    MINUTELY: ["BYWEEKNO"],
    HOURLY: ["BYWEEKNO"],
    DAILY: ["BYWEEKNO", "BYYEARDAY"],
    WEEKLY: ["BYWEEKNO", "BYYEARDAY", "BYMONTHDAY"],
    MONTHLY: ["BYWEEKNO", "BYYEARDAY"],
} as Record<keyof typeof Frequency, string[]>;

export const MONTHS = {
    1: "JAN",
    2: "FEB",
    3: "MAR",
    4: "APR",
    5: "MAY",
    6: "JUN",
    7: "JUL",
    8: "AUG",
    9: "SEP",
    10: "OCT",
    11: "NOV",
    12: "DEC",
};

export enum EventTypes {
    default = "default",
    period = "period",
    recurrence = "recurrence",
}

export const rruleEnds = {
    never: "never",
    until: "schedule.ends.until",
    count: "schedule.ends.count",
}
