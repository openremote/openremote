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
import { css, html, type PropertyValues } from "lit";
import { OrElement } from "@openremote/or-element";
import { customElement, property } from "lit/decorators.js";
import { type RuleActionAlarm, type AssetQuery, AlarmSeverity } from "@openremote/model";

import "@openremote/or-mwc-components/or-mwc-input";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next, translate } from "@openremote/or-translate";
import {
  type DialogAction,
  type OrMwcDialog,
  OrMwcDialogOpenedEvent,
} from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import type { OrVaadinSelect, SelectItem } from "@openremote/or-vaadin-components/or-vaadin-select";

const checkValidity = (form: HTMLElement | null, dialog: OrMwcDialog) => {
  if (form) {
    const inputs = form.querySelectorAll("or-mwc-input");
    const elements = Array.prototype.slice.call(inputs);

    const valid = elements.every((element) => {
      if (element.shadowRoot) {
        const input = element.shadowRoot.querySelector("input, textarea") as any;

        if (element.type === InputType.SELECT) {
          return true;
        }

        if (input && input.checkValidity()) {
          return true;
        } else {
          element._mdcComponent.valid = false;
          element._mdcComponent.helperTextContent = "required";

          return false;
        }
      } else {
        return false;
      }
    });
    if (valid) {
      dialog.close();
    }
  }
};

// language=CSS
const style = css`
  :host {
    display: flex;
    align-items: baseline;
  }
  :host > * {
    margin: 0 3px 6px;
  }
  .min-width {
    flex: 0 0 240px;
  }
`;

@customElement("or-rule-alarm-modal")
export class OrRuleAlarmModal extends translate(i18next)(OrElement) {
  static get styles() {
    return style;
  }

  @property({ type: Object, attribute: false })
  public action!: RuleActionAlarm;

  @property({ type: String })
  public title = "settings";

  @property({ type: Object })
  public query?: AssetQuery;

  constructor() {
    super();
    this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initDialog);
  }

  initDialog() {
    const modal = this.shadowRoot!.getElementById("alarm-modal");
    if (!modal) return;
  }

  renderDialogHTML(action: RuleActionAlarm) {
    const dialog: OrMwcDialog = this.shadowRoot!.getElementById("alarm-modal") as OrMwcDialog;
    if (!this.shadowRoot) return;

    const slot: HTMLSlotElement | null = this.shadowRoot.querySelector(".alarm-form-slot");
    if (dialog && slot) {
      const container = document.createElement("div");
      slot.assignedNodes({ flatten: true }).forEach((child) => {
        if (child instanceof HTMLElement) {
          container.appendChild(child);
        }
      });
      dialog.content = html`${container}`;
      dialog.dismissAction = null;
      this.requestUpdate();
    }
  }

  firstUpdated(changedProperties: PropertyValues) {
    if (changedProperties.has("action")) {
      this.renderDialogHTML(this.action);
    }
  }

  checkForm() {
    const dialog: OrMwcDialog = this.shadowRoot!.host as OrMwcDialog;

    if (this.shadowRoot) {
      const alarmConfig = this.shadowRoot.querySelector("or-rule-form-alarm");

      if (alarmConfig && alarmConfig.shadowRoot) {
        const form = alarmConfig.shadowRoot.querySelector("form");
        return checkValidity(form, dialog);
      }
    }
  }

  protected render() {
    if (!this.action) return html``;

    const alarmPickerModalActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: html`<or-mwc-input
          class="button"
          .type="${InputType.BUTTON}"
          .label="${i18next.t("cancel")}"
        ></or-mwc-input>`,
        action: (dialog) => {},
      },
      {
        actionName: "",
        content: html`<or-mwc-input
          class="button"
          .type="${InputType.BUTTON}"
          .label="${i18next.t("ok")}"
          @or-mwc-input-changed="${this.checkForm}"
        ></or-mwc-input>`,
      },
    ];

    const alarmPickerModalOpen = () => {
      const dialog: OrMwcDialog = this.shadowRoot!.getElementById("alarm-modal") as OrMwcDialog;
      if (dialog) {
        dialog.open();
      }
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
        style="width: 240px;"
        @change=${(ev: Event) => this.setActionAlarmSeverity((ev.currentTarget as OrVaadinSelect).value)}
      >
        <or-translate slot="label" value="alarm.severity"></or-translate>
      </or-vaadin-select>
      <or-vaadin-button @click=${() => alarmPickerModalOpen()}>
        <or-translate value="settings"></or-translate>
      </or-vaadin-button>
      <or-mwc-dialog id="alarm-modal" heading="${this.title}" .actions="${alarmPickerModalActions}"></or-mwc-dialog>
      <slot class="alarm-form-slot"></slot>
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
