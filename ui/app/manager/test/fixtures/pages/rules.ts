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
import { type BasePage, type Locator, type Page, type Shared, expect } from "@openremote/test";
import type { Manager } from "../manager";

export class RulesPage implements BasePage {
  constructor(
    private readonly page: Page,
    private readonly shared: Shared,
    private readonly manager: Manager
  ) {}

  async goto() {
    await this.manager.navigateToTab("Rules");
  }

  /** The rule editor's save button, only enabled once the rule is both modified and valid. */
  getSaveButton(): Locator {
    return this.page.getByRole("button", { name: "Save", exact: true });
  }

  getWhenClause(): Locator {
    return this.page.locator("or-rule-when");
  }

  getThenClause(): Locator {
    return this.page.locator("or-rule-then-otherwise");
  }

  /** Add an action of the given type (e.g. "Email", "Alarm") to the then clause. */
  async addThenAction(type: string) {
    await this.getThenClause().getByRole("menuitem", { name: "Add action" }).click();
    await this.getThenClause().getByRole("menuitem", { name: type, exact: true }).click();
  }

  /**
   * The open action dialog.
   *
   * Every action keeps its dialog in the DOM whether or not it is showing, so this matches on the MDC open state
   * rather than on the element, to avoid resolving to a closed one.
   */
  getOpenDialog(): Locator {
    return this.page.locator(".mdc-dialog--open");
  }

  /** Open an action's dialog through its own button ("Message" for notifications/webhooks, "Settings" for alarms). */
  async openActionDialog(openWith: string) {
    await this.getThenClause().getByRole("button", { name: openWith, exact: true }).click();
    await expect(this.getOpenDialog()).toBeVisible();
  }

  getDialogButton(name: string): Locator {
    return this.getOpenDialog().getByRole("button", { name, exact: true });
  }

  /** A text field inside the open dialog, located by its visible label. */
  getDialogField(label: string): Locator {
    return this.getOpenDialog().getByRole("textbox", { name: label, exact: true });
  }
}
