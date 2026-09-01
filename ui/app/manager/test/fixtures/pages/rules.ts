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
import type { BasePage, Locator, Page, Shared } from "@openremote/test";
import type { Manager } from "../manager";

export interface AttributeWhenClauseOptions {
  assetType: string;
  asset: string;
  attribute: string;
  value: string;
  operator: string;
}

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

  async configureAttributeWhenClause(
    when: Locator,
    { assetType, asset, attribute, value, operator }: AttributeWhenClauseOptions
  ) {
    await when.getByRole("menuitem", { name: "Add condition" }).click();
    await when.getByRole("menuitem", { name: assetType }).click();
    await when.getByRole("combobox", { name: "Asset", exact: true }).fill(asset);
    await when.getByRole("option", { name: asset, exact: true }).click();
    await when.getByRole("combobox", { name: "Attribute", exact: true }).fill(attribute);
    await when.getByRole("option", { name: attribute, exact: true }).click();
    await when.getByRole("combobox", { name: "Operator", exact: true }).fill(operator);
    await when.getByRole("option", { name: operator, exact: true }).click();
    await when.getByRole("spinbutton", { name: attribute }).fill(value);
  }

  /**
   * The overlay of an action settings dialog, which Vaadin renders in its own element rather than
   * inside `or-rule-json-dialog`.
   * @param scope - Clause the action belongs to, e.g. the `or-rule-then-otherwise` locator.
   */
  actionDialogOverlay(scope: Locator): Locator {
    return scope.getByRole("dialog").locator("#overlay").first();
  }

  /**
   * Moves focus off the field being edited so its `change` event fires and the dialog revalidates.
   * The overlay padding is the only spot within the dialog that takes no focus of its own.
   */
  async blurActiveField(scope: Locator) {
    await this.actionDialogOverlay(scope).click({ position: { x: 0, y: 0 } });
  }
}
