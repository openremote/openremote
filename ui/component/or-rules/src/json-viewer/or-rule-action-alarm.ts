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
import { css, html } from "lit";
import { OrElement } from "@openremote/or-element";
import { customElement, property, query } from "lit/decorators.js";
import type { ActionType, RulesConfig } from "../index";
import { AlarmSeverity, type JsonRule, type RuleActionAlarm, type User, type UserQuery } from "@openremote/model";
import "@openremote/or-vaadin-components/or-vaadin-select";
import "./or-rule-json-dialog";
import "./forms/or-rule-form-alarm";
import type { OrRuleFormAlarm } from "./forms/or-rule-form-alarm";
import manager from "@openremote/core";
import type { OrRulesActionDialogCancelEvent, OrRulesActionDialogOkEvent } from "./or-rule-json-dialog";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import type { OrVaadinSelect, SelectItem } from "@openremote/or-vaadin-components/or-vaadin-select";
import { i18next } from "@openremote/or-translate";

// language=CSS
const style = css`
  :host {
    display: flex;
    align-items: baseline;
  }

  :host > * {
    margin: 0 3px 6px;
  }
`;

@customElement("or-rule-action-alarm")
export class OrRuleActionAlarm extends OrElement {
  static get styles() {
    return style;
  }

  @property({ type: Object, attribute: false })
  public rule!: JsonRule;

  @property({ type: Object, attribute: false })
  public action!: RuleActionAlarm;

  @property({ type: String, attribute: false })
  public actionType!: ActionType;

  @property({ type: Boolean })
  public readonly?: boolean;

  @property({ type: Object })
  public config?: RulesConfig;

  @query("or-rule-form-alarm")
  protected _formElem?: OrRuleFormAlarm;

  protected _initialAction?: RuleActionAlarm;
  protected _loadedUsers: User[] = [];

  async connectedCallback(): Promise<void> {
    this._initialAction = structuredClone(this.action);
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

    // When 'cancel' is pressed, reset ACTION to the initial state (all changes get removed)
    const onModalCancel = (_ev: OrRulesActionDialogCancelEvent) => {
      if (this._initialAction?.alarm && this.action.alarm) {
        // Severity is edited outside of the dialog, so it is kept rather than rolled back
        const initialAlarm = { ...structuredClone(this._initialAction.alarm), severity: this.action.alarm.severity };

        // Check if anything in the alarm has changed
        if (
          JSON.stringify(this.action.alarm) !== JSON.stringify(initialAlarm) ||
          this.action.assigneeId !== this._initialAction.assigneeId
        ) {
          // Assign into the existing action, as the rule holds a reference to it
          this.action.alarm = initialAlarm;
          this.action.assigneeId = this._initialAction.assigneeId;
          this._formElem?.requestUpdate();
          this.requestUpdate("action");
        }
      } else {
        console.warn("Could not rollback alarm form.");
      }
    };

    const onModalOk = (_ev: OrRulesActionDialogOkEvent) => {
      this._initialAction = structuredClone(this.action); // update initial action for opening the modal in the future
      this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    };

    const severityOptions: SelectItem[] = [
      { value: AlarmSeverity.LOW, label: i18next.t("alarm.severity_LOW") },
      { value: AlarmSeverity.MEDIUM, label: i18next.t("alarm.severity_MEDIUM") },
      { value: AlarmSeverity.HIGH, label: i18next.t("alarm.severity_HIGH") },
    ];

    return html`
      <or-vaadin-select
        value=${this.action.alarm?.severity}
        .items=${severityOptions}
        ?readonly=${this.readonly}
        style="width: 240px;"
        @change=${(ev: Event) => this.setActionAlarmSeverity((ev.currentTarget as OrVaadinSelect).value)}
      >
        <or-translate slot="label" value="alarm.severity"></or-translate>
      </or-vaadin-select>
      <or-rule-json-dialog ?readonly=${this.readonly} @cancel="${onModalCancel}" @ok="${onModalOk}">
        <or-translate slot="button" value="settings"></or-translate>
        <or-translate slot="title" value="alarm."></or-translate>
        <or-rule-form-alarm
          .users="${this._loadedUsers}"
          .action="${this.action}"
          ?readonly=${this.readonly}
        ></or-rule-form-alarm>
      </or-rule-json-dialog>
    `;
  }

  protected setActionAlarmSeverity(value: string | undefined) {
    if (value && this.action.alarm) {
      const alarm: any = this.action.alarm;
      alarm.severity = value;
      this.action.alarm = { ...alarm };
    }

    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    this.requestUpdate();
  }
}
