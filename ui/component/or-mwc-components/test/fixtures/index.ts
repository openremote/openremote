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
import type { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import type { Page, Locator } from "@openremote/test";

export class MwcInput {
  constructor(private readonly page: Page) {}

  /**
   * Get the underlying native input element of the or-mwc-input element
   * @param type The {@link InputType} to look for
   * @param locator The locator to start from
   * @returns The locator to the underlying <input> element inside the or-mwc-input
   */
  getInputByType(type: `${InputType}`, locator?: Locator) {
    return (locator ?? this.page).locator(`or-mwc-input[type=${type}] #component > input`);
  }

  /**
   * Returns the locator for the specified {@link InputType.SELECT|select input} option
   */
  getSelectInputOption(option: string, locator?: Locator): Locator {
    return (locator ?? this.page).locator("or-mwc-input li[role=option]").getByText(option, { exact: true });
  }
}

export class MwcMenu {
  constructor(private readonly page: Page) {}

  /**
   * Returns the locator for the specified menu item
   */
  getMenuItem(option: string, locator?: Locator): Locator {
    return (locator ?? this.page).locator("or-mwc-menu li[role=menuitem]", { hasText: option });
  }
}

export class MwcDialog {
  constructor(private readonly page: Page) {}

  /**
   * Returns a locator of the or-mwc-dialog
   */
  getDialog(): Locator {
    return this.page.locator("or-mwc-dialog");
  }
}
