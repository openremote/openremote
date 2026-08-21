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
import type { BasePage, Page, Shared } from "@openremote/test";
import type { Manager } from "../manager";
import {energyRule} from "../data/rules";

export class RulesPage implements BasePage {
  constructor(
    private readonly page: Page,
    private readonly shared: Shared,
    private readonly manager: Manager
  ) {}

  async goto() {
    return this.manager.navigateToTab("Rules");
  }

  async createRule(type: "When-Then" | "Flow" | "Groovy") {
    await this.page.click(".mdi-plus >> nth=0");
    await this.page.getByRole("menuitem", { name: type, exact: true }).click();
  }

  async setRuleName(name: string) {
    return this.page.getByRole("textbox", { name: "Rule name" }).fill(name);
  }
}
