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
import { html, LitElement, type PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import type { ActionType, RulesConfig } from "../index";
import type { JsonRule, RuleActionAlarm, User, UserQuery } from "@openremote/model";
import "./modals/or-rule-alarm-modal";
import "./forms/or-rule-form-alarm";
import manager from "@openremote/core";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";

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

  protected _initialAction?: RuleActionAlarm;

  async connectedCallback(): Promise<void> {
    await this.loadUsers();
    this._initialAction = structuredClone(this.action);
    super.connectedCallback();
  }

  willUpdate(changedProps: PropertyValues) {
    // If the rule property changes, we assume it is a "new rule", so the rollback cache no longer applies.
    if (changedProps.has("rule") && changedProps.get("rule") !== undefined) {
      this._initialAction = structuredClone(this.action);
    }

    return super.willUpdate(changedProps);
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

    // When 'cancel' is pressed, reset the ACTION to the initial state (all changes get removed)
    const onModalCancel = () => {
      if (!this._initialAction) {
        console.warn("Could not rollback alarm form.");
        return;
      }
      const initial = structuredClone(this._initialAction);

      if (
        JSON.stringify(this.action.alarm) !== JSON.stringify(initial.alarm) ||
        this.action.assigneeId !== initial.assigneeId
      ) {
        console.debug("Rolling back the alarm to former state...");
        this.action.alarm = initial.alarm;
        this.action.assigneeId = initial.assigneeId;
        this.requestUpdate("action");
      } else {
        console.debug("Rolling back was not necessary, as no changes have been done.");
      }
    };

    const onModalOk = () => {
      this._initialAction = structuredClone(this.action); // update initial action for opening the modal in the future
      this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    };

    return html`
      <or-rule-alarm-modal
        title="alarm."
        .action="${this.action}"
        @or-rules-alarm-modal-cancel="${onModalCancel}"
        @or-rules-alarm-modal-ok="${onModalOk}"
      >
        <or-rule-form-alarm .users="${this._loadedUsers}" .action="${this.action}"></or-rule-form-alarm>
      </or-rule-alarm-modal>
    `;
  }
}
