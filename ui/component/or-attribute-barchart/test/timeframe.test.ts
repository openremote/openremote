/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { ct, expect } from "@openremote/test";

import { BarChartInterval, OrAttributeBarChart } from "@openremote/or-attribute-barchart";
import {
  getChartAxisBounds,
  getNavigationDuration,
  shiftTimeframe,
  type TimeframeUnit,
} from "@openremote/or-attribute-barchart/timeframe";

ct.use({ timezoneId: "Europe/Stockholm" });

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

function centeredDatasetBounds(start: number, end: number, interval: number): [number, number] {
  return [start + interval / 2, end - interval / 2];
}

ct("should keep requested bounds when production updates a centered dataset", async ({ mount }) => {
  const component = await mount(OrAttributeBarChart, {
    props: { timePrefixKey: "this", timeWindowKey: "Hour", interval: BarChartInterval.ONE_HOUR },
  });
  const bounds = await component.evaluate(
    (element, { hour, day }) => {
      const chart = {
        setOption: (option: any) => ((element as any).__testChartOption = option),
      };
      Object.assign(element as any, {
        _chart: chart,
        _startOfPeriod: 0,
        _endOfPeriod: day,
        _intervalConfig: { millis: hour },
        _data: [
          {
            data: [
              [hour / 2, 1],
              [day - hour / 2, 2],
            ],
          },
        ],
      });
      (element as any)._updateChartData();
      return (element as any).__testChartOption.xAxis;
    },
    { hour: HOUR, day: DAY }
  );

  // If _updateChartData() restores centered datapoints into its period state, min/max become H/2 and DAY-H/2.
  expect(bounds.min).toBe(0);
  expect(bounds.max).toBe(DAY);
});

ct("should keep requested chart bounds when bars are centered in their buckets", async () => {
  for (const interval of [HOUR, 15 * 60 * 1000, 5 * 60 * 1000]) {
    const requested: [number, number] = [0, DAY];
    const centeredDataset = centeredDatasetBounds(requested[0], requested[1], interval);
    const rendered = getChartAxisBounds(requested[0], requested[1], centeredDataset);
    const corrupted = getChartAxisBounds(centeredDataset[0], centeredDataset[1], centeredDataset);

    // The old dataset-derived bounds shrink by one interval; production axis bounds must remain requested bounds.
    expect(centeredDataset[1] - centeredDataset[0]).toBe(DAY - interval);
    expect(requested[1] - requested[0]).toBe(DAY);
    expect(rendered).toEqual(requested);
    // Falsification: restoring the old assignments would pass these centered values as the requested bounds.
    expect(corrupted).not.toEqual(requested);
  }
});

ct("should use configured elapsed duration for fixed windows and exact duration for custom windows", async () => {
  expect(getNavigationDuration(0, DAY - 1, false, "hours", 24)).toBe(DAY);
  expect(getNavigationDuration(0, 195 * 60 * 1000, true, "minutes", 195)).toBe(195 * 60 * 1000);

  let start = 0;
  for (let step = 0; step < 10; step++) start += getNavigationDuration(0, HOUR - 1, false, "hours", 1);
  expect(start).toBe(10 * HOUR);
});

ct("should preserve the original 24-hour duration across repeated navigation", async () => {
  const initial: [Date, Date] = [new Date("2026-03-28T12:00:00+01:00"), new Date("2026-03-29T13:00:00+02:00")];
  const next = shiftTimeframe(initial[0], initial[1], DAY, "hours", 24, "next");
  const nextAgain = shiftTimeframe(next[0], next[1], DAY, "hours", 24, "next");
  const previous = shiftTimeframe(nextAgain[0], nextAgain[1], DAY, "hours", 24, "previous");

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

ct("should preserve calendar-day semantics across DST", async () => {
  const initial: [Date, Date] = [new Date("2026-03-28T00:00:00+01:00"), new Date("2026-03-29T00:00:00+01:00")];
  const next = shiftTimeframe(initial[0], initial[1], DAY, "days", 1, "next");

  // A calendar day crossing the spring transition is 23 elapsed hours by design.
  expect(next[1].getTime() - next[0].getTime()).toBe(23 * HOUR);
});

ct("should recognize Moment singular and alias calendar units", async () => {
  const initial: [Date, Date] = [new Date("2025-01-31T00:00:00Z"), new Date("2025-02-28T00:00:00Z")];
  for (const unit of ["day", "d", "week", "w", "month", "M", "quarter", "Q", "year", "y"] as TimeframeUnit[]) {
    const next = shiftTimeframe(initial[0], initial[1], 0, unit, 1, "next");
    expect(next[0].getTime()).toBeGreaterThan(initial[0].getTime());
  }
});

ct("should preserve calendar-month semantics instead of a fixed month length", async () => {
  const initial: [Date, Date] = [new Date("2025-01-15T00:00:00Z"), new Date("2025-02-15T00:00:00Z")];
  const next = shiftTimeframe(initial[0], initial[1], 31 * DAY, "months", 1, "next");

  expect(next[0].toISOString()).toBe("2025-02-15T00:00:00.000Z");
  expect(next[1].toISOString()).toBe("2025-03-15T00:00:00.000Z");
  expect(next[1].getTime() - next[0].getTime()).toBe(28 * DAY);
});

ct("should preserve a non-default interval duration and direction", async () => {
  const initial: [Date, Date] = [new Date("2026-01-01T00:07:00Z"), new Date("2026-01-01T03:22:00Z")];
  const duration = 195 * 60 * 1000;
  const next = shiftTimeframe(initial[0], initial[1], duration, "minutes", 195, "next");
  const previous = shiftTimeframe(next[0], next[1], duration, "minutes", 195, "previous");

  // Non-default, minute-aligned windows must also remain valid and round-trip exactly.
  expect(next[1].getTime() - next[0].getTime()).toBe(duration);
  expect(next[1].getTime()).toBeGreaterThan(next[0].getTime());
  expect(previous[1].getTime()).toBeGreaterThan(previous[0].getTime());
  expect(previous[0].getTime()).toBe(initial[0].getTime());
  expect(previous[1].getTime()).toBe(initial[1].getTime());
});
