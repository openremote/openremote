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
import { css, html, LitElement, type PropertyValues } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { type RuleActionAlarm, type AssetQuery, AlarmSeverity } from "@openremote/model";

import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next, translate } from "@openremote/or-translate";
import type { DialogAction, OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import type { OrVaadinSelect, SelectItem } from "@openremote/or-vaadin-components/or-vaadin-select";
import { isFormValid } from "../util";

export class OrRulesAlarmModalCancelEvent extends CustomEvent<void> {
  public static readonly NAME = "or-rules-alarm-modal-cancel";

  constructor() {
    super(OrRulesAlarmModalCancelEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

export class OrRulesAlarmModalOkEvent extends CustomEvent<void> {
  public static readonly NAME = "or-rules-alarm-modal-ok";

  constructor() {
    super(OrRulesAlarmModalOkEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

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
export class OrRuleAlarmModal extends translate(i18next)(LitElement) {
  static get styles() {
    return style;
  }

  @property({ type: Object, attribute: false })
  public action!: RuleActionAlarm;

  @property({ type: String })
  public title = "settings";

  @property({ type: Object })
  public query?: AssetQuery;

  @query("#alarm-modal")
  protected _orMwcDialog?: OrMwcDialog;

  connectedCallback() {
    this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
    return super.connectedCallback();
  }

  disconnectedCallback() {
    this.removeEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
    return super.disconnectedCallback();
  }

  protected _onJsonRuleChanged(ev: Event) {
    // The severity select sits in the action row, not in the dialog, and dispatches from this element; it applies
    // straight away like any other action control.
    if (ev.composedPath()[0] === this) {
      return;
    }

    // Keep edits inside the dialog: the rule is only really changed once "ok" is pressed, so letting this
    // reach the rule editor would arm its save button while the dialog is still open (and cancellable).
    ev.stopPropagation();
    this.validateForm();
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
    // Possibly, a render is triggered by renderDialogHTML(), so we await the pending update. (if there is any)
    this.getUpdateComplete().finally(() => {
      this.validateForm();
    });
  }

  validateForm() {
    const valid = this.checkForm();
    this._orMwcDialog?.setActions(
      this._orMwcDialog?.actions?.map((action) => {
        if (action.actionName === "ok") {
          action.disabled = !valid;
        }
        return action;
      })
    );
  }

  checkForm() {
    // renderDialogHTML() moves the slotted form into the dialog's content, so it lives in the dialog's shadow root
    const form = this._orMwcDialog?.shadowRoot?.querySelector("or-rule-form-alarm");
    return isFormValid(form?.shadowRoot);
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
        action: () => this.dispatchEvent(new OrRulesAlarmModalCancelEvent()),
      },
      {
        actionName: "ok",
        content: "ok",
        disabled: true, // (by default, can be changed in validateForm())
        action: () => this.dispatchEvent(new OrRulesAlarmModalOkEvent()),
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
