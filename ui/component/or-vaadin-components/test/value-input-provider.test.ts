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

import { ValueInputProviderHarness } from "./fixtures/value-input-provider-harness.js";

// The picker shows local time, so the zone is pinned for the expected values.
ct.use({ timezoneId: "Europe/Amsterdam" });

// 2026-01-02T10:30 in Europe/Amsterdam (UTC+1)
const TIMESTAMP = Date.UTC(2026, 0, 2, 9, 30);

ct.describe("Date time input", () => {
  ct("should show a timestamp as local date and time and write back a timestamp", async ({ mount }) => {
    const values: unknown[] = [];
    const component = await mount(ValueInputProviderHarness, {
      props: { valueType: "timestamp", value: TIMESTAMP },
      on: { "value-change": (value: unknown) => values.push(value) },
    });

    const time = component.getByRole("combobox").last();
    await expect(time).toHaveValue("10:30");
    await time.fill("11:45");
    await time.press("Enter");

    await expect.poll(() => values).toEqual([Date.UTC(2026, 0, 2, 10, 45)]);
  });

  ct("should write back an ISO 8601 string for a string timestamp", async ({ mount }) => {
    const values: unknown[] = [];
    const component = await mount(ValueInputProviderHarness, {
      props: { valueType: "timestampISO8601", jsonType: "string", value: "2026-01-02T09:30:00.000Z" },
      on: { "value-change": (value: unknown) => values.push(value) },
    });

    const time = component.getByRole("combobox").last();
    await expect(time).toHaveValue("10:30");
    await time.fill("11:45");
    await time.press("Enter");

    // A number used to be written back, which fails the ISO 8601 pattern of the value type.
    await expect.poll(() => values).toEqual(["2026-01-02T10:45:00.000Z"]);
  });

  ct("should write back null when the date and time are cleared", async ({ mount }) => {
    const values: unknown[] = [];
    const component = await mount(ValueInputProviderHarness, {
      props: { valueType: "timestamp", value: TIMESTAMP },
      on: { "value-change": (value: unknown) => values.push(value) },
    });

    const date = component.getByRole("combobox").first();
    const time = component.getByRole("combobox").last();
    await date.fill("");
    await date.press("Enter");
    await time.fill("");
    await time.press("Enter");

    await expect.poll(() => values).toEqual([null]);
  });

  ct("should render an empty picker for a value that is not a date and time", async ({ mount }) => {
    const component = await mount(ValueInputProviderHarness, {
      props: { valueType: "timestampISO8601", jsonType: "string", value: "10:30:00" },
    });

    // Rendering used to throw on the invalid date, leaving no input at all.
    await expect(component.getByRole("combobox")).toHaveCount(2);
    await expect(component.getByRole("combobox").first()).toHaveValue("");
    await expect(component.getByRole("combobox").last()).toHaveValue("");
  });
});
