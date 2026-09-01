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
import { customElement, property, query } from "lit/decorators.js";
import type { RuleActionAlarm, Alarm, User } from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-combo-box";
import type { OrVaadinComboBox } from "@openremote/or-vaadin-components/or-vaadin-combo-box";
import type { OrRuleForm } from "./or-rule-form";

@customElement("or-rule-form-alarm")
export class OrRuleFormAlarm extends OrElement implements OrRuleForm {
  @property({ type: Object, attribute: false })
  public action!: RuleActionAlarm;

  @property()
  public users: User[] = [];

  @query("#form-container")
  protected _formContainerElem?: HTMLElement;

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

  checkValidity() {
    if (this._formContainerElem) {
      const elems = Array.from(this._formContainerElem!.children) as HTMLInputElement[];
      return elems.filter((e) => !e.checkValidity()).length === 0;
    }
    return false;
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
          required
          .items=${options}
          .selectedItem=${options.find((o) => o.value === this.action.assigneeId)}
          @change=${(ev: CustomEvent) => {
            // In the combobox, the 'value' is the User ID, while 'label' holds the username we need for assigneeId
            const value: string | undefined = (ev.currentTarget as OrVaadinComboBox).selectedItem?.label;
            this.action.assigneeId = value;
            this.setActionAlarmName(value, undefined);
          }}
        >
          <or-translate slot="label" value="alarm.assignee"></or-translate>
        </or-vaadin-combo-box>
      </div>
    `;
  }

  protected setActionAlarmName(value: string | undefined, key?: string) {
    if (key && this.action.alarm) {
      const alarm: any = this.action.alarm;
      alarm[key] = value;
      this.action.alarm = { ...alarm };
    }
    if (!key) {
      const user = this.users.filter((obj) => obj.username === value).map((obj) => obj.id)[0];
      if (!user) {
        console.warn(`Could not select user ${value}, as we can't find the user in cache.`);
      }
      this.action.assigneeId = user;
    }

    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    this.requestUpdate();
  }
}
