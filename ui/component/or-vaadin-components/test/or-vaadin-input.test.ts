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

import { OrVaadinInput } from "@openremote/or-vaadin-components/or-vaadin-input";

// The date time picker shows the format of the browser locale.
ct.use({ locale: "en-US" });

ct("should render a date time picker for the datetime-local type", async ({ mount }) => {
  const component = await mount(OrVaadinInput, { props: { type: "datetime-local" } });

  await expect(component.locator("or-vaadin-date-time-picker")).toBeVisible();
  // The date and the time field are separate inputs of the picker.
  await expect(component.getByRole("combobox")).toHaveCount(2);
});

ct("should expose the picked date and time as a local ISO string", async ({ mount }) => {
  let changeCount = 0;
  const component = await mount(OrVaadinInput, {
    props: { type: "datetime-local" },
    on: {
      // The `change` CustomEvent carries no detail, so it can only be counted.
      change: () => {
        changeCount += 1;
      },
    },
  });

  // Typed dates follow the date format of the browser locale, so the date is set through the local ISO value instead.
  await component.evaluate((el: OrVaadinInput) => el.setAttribute("value", "2026-01-02T10:30"));
  const time = component.getByRole("combobox").last();
  await expect(time).toHaveValue("10:30 AM");
  await time.fill("11:45 AM");
  await time.press("Enter");

  await expect.poll(() => component.evaluate((el: OrVaadinInput) => el.nativeValue)).toBe("2026-01-02T11:45");
  // Setting the value does not count as a change, only the picked time does.
  await expect.poll(() => changeCount).toBe(1);
});
