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
import { MenuBar } from "@vaadin/menu-bar";
import { type LitElement, render, type TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import type { OrVaadinComponent } from "./util";
import "@vaadin/menu-bar";

export { MenuBarItem, SubMenuItem } from "@vaadin/menu-bar";

export function createMenuBarItem(content: TemplateResult) {
  const item = document.createElement("vaadin-menu-bar-item");
  render(content, item);
  return item;
}

@customElement("or-vaadin-menu-bar")
export class OrVaadinMenuBar extends (MenuBar as new () => MenuBar & LitElement) implements OrVaadinComponent {}
