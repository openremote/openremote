/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { customElement } from "lit/decorators.js";
import { CheckboxGroup } from "@vaadin/checkbox-group";
import { Checkbox } from "@vaadin/checkbox";
import { OrVaadinComponent } from "./util";
import "@vaadin/checkbox";

export type * from "@vaadin/checkbox-group";

@customElement("or-vaadin-checkbox-group")
export class OrVaadinCheckboxGroup extends CheckboxGroup implements OrVaadinComponent {

    /*
     * Vaadin's `CheckboxGroupMixin` recognises group items via a strict `localName === "vaadin-checkbox"`
     * check, so wrapped children (`or-vaadin-checkbox`, `or-vaadin-toggle`) would never register: the
     * group's `value`, its validation and its disabled/readonly propagation would all ignore them.
     * Accept any `Checkbox` subclass instead. This overrides a private Vaadin API, so re-check it when
     * upgrading Vaadin (verified against 25.2.1).
     */
    private __filterCheckboxes(nodes: Node[]): Checkbox[] {
        return nodes.filter((node): node is Checkbox => node instanceof Checkbox);
    }
}
