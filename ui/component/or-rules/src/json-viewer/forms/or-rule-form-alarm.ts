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
import { html, css } from "lit";
import { OrElement } from "@openremote/or-element";
import { customElement, property } from "lit/decorators.js";
import type { RuleActionAlarm, Alarm, User } from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-combo-box";
import type { OrVaadinComboBox } from "@openremote/or-vaadin-components/or-vaadin-combo-box";
import { isFormValid, type OrRuleForm } from "./or-rule-form";

@customElement("or-rule-form-alarm")
export class OrRuleFormAlarm extends OrElement implements OrRuleForm {
  @property({ type: Object, attribute: false })
  public action!: RuleActionAlarm;

  @property()
  public users: User[] = [];

  static get styles() {
    return css`
      #form-container {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-bottom: 20px;
        min-width: 420px;
        width: 100%;
      }
    `;
  }

  checkValidity(): boolean {
    return isFormValid(this.renderRoot);
  }

  protected render() {
    const alarm: Alarm | undefined = this.action.alarm as Alarm;
    const options: { value: string | undefined; label: string | undefined }[] = this.users
      .filter((u) => u.username !== "manager-keycloak")
      .map((u) => {
        return { value: u.id, label: u.username };
      });
    options.unshift({ value: undefined, label: i18next.t("none") });

    return html`
      <div id="form-container">
        <or-vaadin-text-field
          value=${alarm?.title}
          required
          @change=${(ev: Event) => this.setActionAlarmName((ev.currentTarget as HTMLInputElement).value, "title")}
        >
          <or-translate slot="label" value="alarm.title"></or-translate>
        </or-vaadin-text-field>
        <or-vaadin-text-area
          value=${alarm?.content}
          required
          style="min-height: 200px;"
          @change=${(ev: Event) => this.setActionAlarmName((ev.currentTarget as HTMLInputElement).value, "content")}
        >
          <or-translate slot="label" value="alarm.content"></or-translate>
        </or-vaadin-text-area>
        <or-vaadin-combo-box
          .items=${options}
          .selectedItem=${options.find((o) => o.value === this.action.assigneeId)}
          @change=${(ev: CustomEvent) => {
            // The 'none' option carries no value, which clears the assignee
            this.setActionAssignee((ev.currentTarget as OrVaadinComboBox).selectedItem?.value);
          }}
        >
          <or-translate slot="label" value="alarm.assignee"></or-translate>
        </or-vaadin-combo-box>
      </div>
    `;
  }

  protected setActionAlarmName(value: string | undefined, key: string) {
    if (this.action.alarm) {
      const alarm: any = this.action.alarm;
      alarm[key] = value;
      this.action.alarm = { ...alarm };
    }

    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    this.requestUpdate();
  }

  protected setActionAssignee(assigneeId: string | undefined) {
    this.action.assigneeId = assigneeId;

    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    this.requestUpdate();
  }
}
