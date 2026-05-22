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
import { LitElement, html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { i18next, translate } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

@customElement("confirmation-dialog")
export class ConfirmationDialog extends translate(i18next)(LitElement) {
    @property({ type: String }) public agreeText = i18next.t("ok");
    @property({ type: String }) public disagreeText = i18next.t("cancel");
    @property({ type: String }) public question = i18next.t("areYouSure", "Are you sure?");

    public static get styles() {
        return css`
        .question{
            width: 100%;
            padding: 15px 5px 25px 5px;
            text-align: center;
        }
        .container{
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            justify-content: space-evenly;
            justify-content: space-around;
        }`;
    }

    protected render() {
        return html`
        <div class="question">${this.question}</div>
        <div class="container">
            <or-mwc-input type="${InputType.BUTTON}" unElevated label="${this.agreeText}"
                @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent("agreed")); }}">
            </or-mwc-input>

            <or-mwc-input type="button" label="${this.disagreeText}"
                @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent("disagreed")); }}">
            </or-mwc-input>
        </div>
        `;
    }
}
