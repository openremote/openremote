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
import { customElement } from "lit/decorators.js";
import { ComboBox } from "@vaadin/combo-box";
import type { OrVaadinComponent } from "./util";
import type { PropertyValues, LitElement } from "lit";

/**
 * Vaadin uses custom directives for rendering the dialog content.
 * https://lit.dev/docs/templates/custom-directives/
 * https://vaadin.com/docs/latest/components/combo-box/
 */
export { comboBoxRenderer, ComboBoxLitRenderer } from "@vaadin/combo-box/lit";
export { ComboBoxDataProviderCallback, ComboBoxDataProviderParams } from "@vaadin/combo-box";

@customElement("or-vaadin-combo-box")
export class OrVaadinComboBox extends (ComboBox as new () => ComboBox & LitElement) implements OrVaadinComponent {
  override shouldUpdate(changedProps: PropertyValues) {
    if (changedProps.has("items")) {
      // To prevent unnecessary component updates, we do a strict JSON check on the list of items.
      return (
        JSON.stringify(this.items) !== JSON.stringify(changedProps.get("items")) && super.shouldUpdate(changedProps)
      );
    }
    return super.shouldUpdate(changedProps);
  }
}
