import { Frequency as FrequencyValue, Options } from "rrule";
import { EventTypes } from "./data";

/**
 * Supported recurrence rule parts in evaluation order:
 * - `interval`
 * - `freq`
 * - `bymonth`
 * - `byweekno`
 * - `byyearday`
 * - `bymonthday`
 * - `byweekday` = `byday` in {@link https://datatracker.ietf.org/doc/html/rfc5545#section-3.3.10|rfc5545#section-3.3.10}.
 * - `byhour`
 * - `byminute`
 * - `bysecond`
 * - `count`
 * - `until`
 * @todo wkst not implemented
 *
 * Ignored
 * - | 'byeaster'// Not applicable, introduced by {@link https://labix.org/python-dateutil/#head-a65103993a21b717f6702063f3717e6e75b4ba66|python-dateutil}.
 * - | 'bynmonthday' // Not specified
 * - | 'bynweekday' // Not specified
 * - | 'bysetpos' // Too complex for the time being
 * - | 'dtstart' // Not applicable CalendarEvent already specifies start
 * - | 'tzid' // Not part of the recurrence rule parts
 *
 * @see {@link RRule} and {@link https://labix.org/python-dateutil/#head-a65103993a21b717f6702063f3717e6e75b4ba66|python-dateutil}.
 */
export type RuleParts = Pick<
    Options,
    | "interval"
    | "freq" // Must exist (should default to DAILY?)
    | "bymonth"
    | "byweekno"
    | "byyearday"
    | "bymonthday"
    | "byweekday"
    | "byhour"
    | "byminute"
    | "bysecond"
    | "count"
    | "until"
>;

export type LabeledEventTypes = Record<EventTypes, string>;
export type RulePartKey = keyof RuleParts;
export type Frequency = (keyof typeof FrequencyValue);
