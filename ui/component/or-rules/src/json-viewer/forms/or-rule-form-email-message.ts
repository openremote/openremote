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
import {html, LitElement, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import {i18next, translate} from "@openremote/or-translate"
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {EmailNotificationMessage} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";

@customElement("or-rule-form-email-message")
export class OrRuleFormEmailMessage extends translate(i18next)(LitElement) {

    @property({type: Object})
    public message?: EmailNotificationMessage;

    static get styles() {
        return css`
            or-mwc-input {
                margin-bottom: 20px;
                min-width: 420px;
                width: 100%;
            }
        `;
    }

    protected render() {
        if(!this.message) {
            return html`<or-translate .value="${"errorOccurred"}"></or-translate>`;
        }
        
        return html`
            <form style="display:grid">
                <or-mwc-input value="${this.message.subject || ''}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "subject")}" .label="${i18next.t("subject")}" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${this.message.html || ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "html")}" .label="${i18next.t("message")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
            </form>
        `
    }

    protected setActionNotificationName(value: string | undefined, key?: string) {
        if(key && this.message){
            (this.message as any)[key] = value;
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
