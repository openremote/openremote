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
import { JsonRule, RuleActionWebhook } from "@openremote/model";
import {css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "./modals/or-rule-webhook-modal";
import "./forms/or-rule-form-webhook";

// language=CSS
const style = css`
    :host {
        height: 100%;
        margin: 2px 3px auto 0;
    }

    :host > * {
        margin: 0 3px 6px;
    }

    .min-width {
        min-width: 200px;
    }
`;

@customElement("or-rule-action-webhook")
export class OrRuleActionWebhook extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public rule!: JsonRule;

    @property({type: Object, attribute: false})
    public action!: RuleActionWebhook;


    /* ---------------------- */

    render() {
        return html`
            <div style="display: flex; align-items: center; height: 100%;">
                <or-rule-webhook-modal .action="${this.action}">
                    <or-rule-form-webhook .webhook="${this.action.webhook}"></or-rule-form-webhook>
                </or-rule-webhook-modal>
            </div>
        `
    }
}
