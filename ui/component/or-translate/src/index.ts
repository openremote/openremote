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
import {translate} from "./translate-mixin";
import i18next, {InitOptions, TOptions } from "i18next";
import {LitElement, html, css} from "lit";
import {customElement, property} from "lit/decorators.js";

export {i18next};

export {translate};

@customElement("or-translate")
export class OrTranslate extends translate(i18next)(LitElement) {

    public static styles = css`
        :host {
            display: inline-block;
        }
        
        :host([hidden]) {
            display: none;
        }
    `;

    @property({type: String})
    public value?: string;

    @property({type: Object})
    public options?: TOptions<InitOptions>;

    protected render() {
        return html`
            ${this._getTranslatedValue()}
        `;
    }

    protected _getTranslatedValue() {
        return this.value ? i18next.isInitialized ? i18next.t(this.value, this.options) : this.value : "";
    }
}
