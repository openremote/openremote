/*
 * Copyright 2025, OpenRemote Inc.
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
import {customElement} from "lit/decorators.js";
import {TextField} from "@vaadin/text-field";
import {LitElement} from "lit";
import {OrVaadinComponent} from "./util";

@customElement("or-vaadin-textfield")
export class OrVaadinTextfield extends (TextField as new () => TextField & LitElement) implements OrVaadinComponent {

    override _onEnter(ev: KeyboardEvent) {
        this.dispatchEvent(new CustomEvent("submit", {bubbles: true, composed: true}));
        return super._onEnter(ev);
    }
}
