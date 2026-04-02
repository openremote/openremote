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
import { Frequency as FrequencyValue, Options } from "rrule";

export type ByRRuleParts = Pick<
    Options,
    | "bymonth"
    | "byweekno"
    | "byyearday"
    | "bymonthday"
    | "byweekday"
    | "byhour"
    | "byminute"
    | "bysecond"
>;
export type ByRRulePartsKeys = keyof ByRRuleParts;
export type ByRRuleCombination = Record<keyof typeof FrequencyValue, ByRRulePartsKeys[]>;

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
export type RRuleParts = Pick<
    Options,
    | "interval"
    | "freq" // Must exist (should default to DAILY?)
    | "count"
    | "until"
> & ByRRuleParts;

export type Frequency = keyof typeof FrequencyValue;
export type RRulePartKeys = keyof RRuleParts;
export type PartKeys =
  | RRulePartKeys
  | "start"
  | "start-time"
  | "end"
  | "end-time"
  | "all-day"
  | "recurrence-ends";
