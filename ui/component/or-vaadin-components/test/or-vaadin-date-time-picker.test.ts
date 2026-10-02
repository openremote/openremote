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
import { ct, expect } from "./fixtures";
import { browserLocales } from "./fixtures/data/locales";

import { OrVaadinDateTimePicker } from "@openremote/or-vaadin-components/or-vaadin-date-time-picker";

ct.beforeEach(async ({ shared }) => {
  await shared.locales();
});

ct.describe("Date and time format", () => {
  for (const { locale, date, time, typedDate, typedTime } of browserLocales) {
    ct(
      `should show and parse the date and time in the format of ${locale}`,
      { annotation: { type: "locale", description: locale } },
      async ({ mount }) => {
        const component = await mount(OrVaadinDateTimePicker, { props: { value: "2026-01-02T10:30" } });

        const dateInput = component.getByRole("combobox").first();
        const timeInput = component.getByRole("combobox").last();
        await expect(dateInput).toHaveValue(date);
        await expect(timeInput).toHaveValue(time);
        await dateInput.fill(typedDate);
        await dateInput.press("Enter");
        await timeInput.fill(typedTime);
        await timeInput.press("Enter");

        await expect(component).toHaveJSProperty("value", "2026-01-03T14:15");
      }
    );
  }
});

ct.describe("Calendar", () => {
  for (const { locale, firstWeekday } of browserLocales) {
    ct(
      `should name the calendar in the app language with the first weekday of ${locale}`,
      { annotation: { type: "locale", description: locale } },
      async ({ mount, page, vaadinDateTimePicker }) => {
        const component = await mount(OrVaadinDateTimePicker, { props: { value: "2026-01-02T10:30" } });
        await component.getByRole("combobox").first().click();

        await expect(page.getByText("January 2026")).toBeVisible();
        await expect(vaadinDateTimePicker.getFirstWeekdayHeader()).toHaveText(firstWeekday);
        await expect(page.getByRole("button", { name: "Today" })).toBeVisible();
        await expect(page.getByRole("button", { name: "Cancel" })).toBeVisible();
      }
    );
  }

  ct(
    "should follow a change of the app language in the names only",
    { annotation: { type: "locale", description: "en-US" } },
    async ({ mount, page, vaadinDateTimePicker }) => {
      const component = await mount(OrVaadinDateTimePicker, { props: { value: "2026-01-02T10:30" } });

      const dateInput = component.getByRole("combobox").first();
      await dateInput.click();
      await expect(page.getByText("January 2026")).toBeVisible();

      await page.evaluate(() => window._i18next.changeLanguage("nl"));
      await expect(page.getByText("januari 2026")).toBeVisible();
      // The week still starts on Sunday in the United States.
      await expect(vaadinDateTimePicker.getFirstWeekdayHeader()).toHaveText("zo");
      await expect(page.getByRole("button", { name: "Vandaag" })).toBeVisible();
      await expect(page.getByRole("button", { name: "Annuleren" })).toBeVisible();
      await expect(dateInput).toHaveValue("01/02/2026");
    }
  );
});
