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
import type { InputType } from "@openremote/or-vaadin-components/util";
import { ct as base, type Locator, type Page, type SharedComponentTestFixtures, withPage } from "@openremote/test";
export { expect } from "@openremote/test";

export class VaadinInput {
  constructor(private readonly page: Page) {}

  /**
   * Returns the locator for the specified {@link InputType.SELECT|select input} option
   */
  getSelectInputOption(option: string, locator?: Locator): Locator {
    return (locator ?? this.page).locator("or-vaadin-select").getByRole("option").getByText(option, { exact: true });
  }
}

export class VaadinDialog {
  constructor(private readonly page: Page) {}

  /**
   * Returns a locator of the or-vaadin-dialog
   */
  getDialog(): Locator {
    return this.page.locator("or-vaadin-dialog");
  }
}

export class VaadinDateTimePicker {
  constructor(private readonly page: Page) {}

  /**
   * Returns the locator of the date input of the or-vaadin-date-time-picker
   */
  getDateInput(locator?: Locator): Locator {
    return (locator ?? this.page).locator("[slot=date-picker] input");
  }

  /**
   * Returns the locator of the time input of the or-vaadin-date-time-picker, which has no combobox role when the step of
   * the picker is below 15 minutes
   */
  getTimeInput(locator?: Locator): Locator {
    return (locator ?? this.page).locator("[slot=time-picker] input");
  }

  /**
   * Returns the locator of the first weekday header of the open calendar, which is hidden from the accessibility tree
   */
  getFirstWeekdayHeader(): Locator {
    return this.page.locator("th[scope='col'][part~='weekday']").first();
  }
}

interface ComponentFixtures extends SharedComponentTestFixtures {
  vaadinDateTimePicker: VaadinDateTimePicker;
}

export const ct = base.extend<ComponentFixtures>({
  vaadinDateTimePicker: withPage(VaadinDateTimePicker),
  // The locale option only applies per file or describe block, so a test sets its browser locale with a locale annotation
  locale: async ({ locale }, use, testInfo) => {
    await use(testInfo.annotations.find((annotation) => annotation.type === "locale")?.description ?? locale);
  },
});
