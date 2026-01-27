/*
 * Copyright 2024, OpenRemote Inc.
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
import { html, LitElement, TemplateResult } from "lit";
import { customElement, property } from "lit/decorators.js";
import { ActionType, RulesConfig } from "../index";
import {
    JsonRule,
    RuleActionAlarm,
    User,
    UserQuery,
} from "@openremote/model";
import "./modals/or-rule-alarm-modal";
import "./forms/or-rule-form-alarm";
import manager from "@openremote/core";

@customElement("or-rule-action-alarm")
export class OrRuleActionAlarm extends LitElement {

    @property({ type: Object, attribute: false })
    public rule!: JsonRule;

    @property({ type: Object, attribute: false })
    public action!: RuleActionAlarm;

    @property({ type: String, attribute: false })
    public actionType!: ActionType;

    public readonly?: boolean;

    @property({ type: Object })
    public config?: RulesConfig;

    protected _loadedUsers: User[] = [];

    async connectedCallback(): Promise<void> {
        await this.loadUsers();
        super.connectedCallback();
    }

    protected async loadUsers() {
        const usersResponse = await manager.rest.api.UserResource.query({
            realmPredicate: { name: manager.displayRealm },
        } as UserQuery);

        if (usersResponse.status !== 200) {
            return;
        }

        this._loadedUsers = usersResponse.data.filter((user) => user.enabled && !user.serviceAccount);
    }


    protected render() {
        if (!this.action.alarm || !this.action.alarm.title) {
            return html``;
        }

        const alarm = this.action.alarm;

        let modalTemplate: TemplateResult | string = ``;

        if (alarm) {
            modalTemplate = html`
                        <or-rule-alarm-modal title="alarm." .action="${this.action}">
                            <or-rule-form-alarm .users="${this._loadedUsers}" .action="${this.action}"></or-rule-form-alarm>
                        </or-rule-alarm-modal>
                    `;
        }

        return html`${modalTemplate}`;
    }
}
