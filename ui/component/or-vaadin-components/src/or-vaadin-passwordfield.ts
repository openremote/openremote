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
import {PasswordField} from "@vaadin/password-field";
import {OrVaadinComponent} from "./util";
import {LitElement} from "lit";

@customElement("or-vaadin-passwordfield")
export class OrVaadinPasswordField extends (PasswordField as new () => PasswordField & LitElement) implements OrVaadinComponent {

    override _onEnter(ev: KeyboardEvent) {
        this.dispatchEvent(new CustomEvent("submit", {bubbles: true, composed: true}));
        return super._onEnter(ev);
    }
}
