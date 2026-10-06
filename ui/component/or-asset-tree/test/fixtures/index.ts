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
import type { Page, Locator } from "@openremote/test";

export class AssetTree {
  constructor(private readonly page: Page) {}

  /**
   * Returns a locator of the filter input
   */
  getFilterInput() {
    return this.page.locator("or-asset-tree #filterInput").getByRole("textbox");
  }

  /**
   * Returns a locator of the filter button
   */
  getFilterButton() {
    return this.page.locator("or-asset-tree #asset-tree-filter").getByTitle("Open filter");
  }

  /**
   * Returns a locator of the filter menu
   */
  getFilterMenu() {
    return this.page.locator("or-asset-tree #asset-tree-filter-setting");
  }

  /**
   * Returns a locator of all nodes that contain assets
   */
  getAssetNodes() {
    return this.page.locator(`or-asset-tree ol li:has(.node-container[node-asset-id]:not([node-asset-id=""]))`);
  }

  /**
   * Returns a locator of all child nodes of the supplied node
   * @param node - Locator pointing to the parent node
   */
  getChildNodes(node: Locator) {
    return node.locator(`ol li .node-container[node-asset-id]:not([node-asset-id=""])`);
  }

  /**
   * Returns a locator of all nodes that are selected
   */
  getSelectedNodes() {
    return this.page.locator(`or-asset-tree ol li[data-selected]`);
  }
}
