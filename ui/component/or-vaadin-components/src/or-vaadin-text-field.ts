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
import {customElement, property} from "lit/decorators.js";
import {TextField} from "@vaadin/text-field";
import {PropertyValues, type LitElement} from "lit";
import {OrVaadinComponent} from "./util";

@customElement("or-vaadin-text-field")
export class OrVaadinTextField extends (TextField as new () => TextField & LitElement) implements OrVaadinComponent {

    /**
     * Custom `type` attribute that echoes to the input element.
     * This is useful for types like 'url' and 'email'.
     * Use with caution: using unknown types, or mismatched types like 'number' could potentially break the component.
     */
    @property({type: String})
    public type?: string;

    override willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if(changedProps.has("type") && this.type !== undefined) {
            (this as any)._setType(this.type);
        }
    }

    override _onEnter(ev: KeyboardEvent) {
        this.dispatchEvent(new CustomEvent("submit", {bubbles: true, composed: true}));
        return super._onEnter(ev);
    }
}
