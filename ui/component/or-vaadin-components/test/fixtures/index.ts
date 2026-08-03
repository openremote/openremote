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
import type { Page, Locator } from "@openremote/test";

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
