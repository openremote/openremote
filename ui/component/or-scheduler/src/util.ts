/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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

export const WEEKDAYS = {
  MO: "Monday",
  TU: "Tuesday",
  WE: "Wednesday",
  TH: "Thursday",
  FR: "Friday",
  SA: "Saturday",
  SU: "Sunday",
}

export const MONTHS = {
    "1": "january",
    "2": "february",
    "3": "march",
    "4": "april",
    "5": "may",
    "6": "june",
    "7": "july",
    "8": "august",
    "9": "september",
    "10": "october",
    "11": "november",
    "12": "december",
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
