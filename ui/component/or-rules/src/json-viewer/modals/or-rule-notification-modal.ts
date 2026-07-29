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
import { html, LitElement, type PropertyValues } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import type { RuleActionNotification, AssetQuery } from "@openremote/model";

import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next, translate } from "@openremote/or-translate";

import {
  type DialogAction,
  type DialogActionBase,
  type OrMwcDialog,
  OrMwcDialogOpenedEvent,
} from "@openremote/or-mwc-components/or-mwc-dialog";
import type { OrRuleFormLocalized } from "../forms/or-rule-form-localized";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import { isFormValid } from "../util";

export class OrRulesNotificationModalCancelEvent extends CustomEvent<void> {
  public static readonly NAME = "or-rules-notification-modal-cancel";

  constructor() {
    super(OrRulesNotificationModalCancelEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

export class OrRulesNotificationModalOkEvent extends CustomEvent<void> {
  public static readonly NAME = "or-rules-notification-modal-ok";

  constructor() {
    super(OrRulesNotificationModalOkEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

@customElement("or-rule-notification-modal")
export class OrRuleNotificationModal extends translate(i18next)(LitElement) {
  @property({ type: Object })
  public action!: RuleActionNotification;

  @property({ type: String })
  public title = "message";

  @property({ type: Object })
  public query?: AssetQuery;

  @property({ type: Boolean })
  public readonly = false;

  @query("#notification-modal")
  protected _orMwcDialog?: OrMwcDialog;

  constructor() {
    super();
    this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initDialog);
  }

  connectedCallback() {
    this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
    return super.connectedCallback();
  }

  disconnectedCallback() {
    this.removeEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
    return super.disconnectedCallback();
  }

  protected _onJsonRuleChanged(ev: Event) {
    // Keep edits inside the dialog: the rule is only really changed once "ok" is pressed, so letting this
    // reach the rule editor would arm its save button while the dialog is still open (and cancellable).
    ev.stopPropagation();
    this.validateForm();
  }

  initDialog() {
    if (!this._orMwcDialog) return;
  }

  renderDialogHTML(action: RuleActionNotification) {
    const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
    if (!this.shadowRoot) return;

    const slot: HTMLSlotElement | null = this.shadowRoot.querySelector(".notification-form-slot");
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
    const root = this._orMwcDialog?.shadowRoot;
    if (!root) {
      return false;
    }

    // The localized form spans several languages, so it decides on its own whether it is complete
    const localizedNotification = root.querySelector("or-rule-form-localized");
    if (localizedNotification) {
      return (localizedNotification as OrRuleFormLocalized).isValid();
    }

    const form = root.querySelector("or-rule-form-email-message, or-rule-form-push-notification");
    return isFormValid(form?.shadowRoot);
  }

  protected render() {
    const onCancel = () => {
      this.dispatchEvent(new OrRulesNotificationModalCancelEvent());
    };

    const onOk = () => {
      this.dispatchEvent(new OrRulesNotificationModalOkEvent());
    };

    const dismissAction: DialogActionBase = {
      actionName: "cancel",
      action: onCancel,
    };

    const actions: DialogAction[] = [
      {
        actionName: "cancel",
        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
        action: onCancel,
      },
      {
        actionName: "ok",
        content: "ok",
        disabled: true, // (by default, can be changed in checkForm())
        action: onOk,
      },
    ];

    const styles = html`
      <style>
        .mdc-dialog__actions {
          justify-content: space-between !important;
        }
      </style>
    `;

    const notificationPickerModalOpen = () => {
      const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
      if (dialog) {
        dialog.open();
      }
    };

    return html`
      <or-vaadin-button ?disabled=${this.readonly} @click=${() => notificationPickerModalOpen()}>
        <or-translate value="message"></or-translate>
      </or-vaadin-button>
      <or-mwc-dialog
        id="notification-modal"
        .heading="${this.title}"
        .dismissAction="${dismissAction}"
        .actions="${actions}"
        .styles="${styles}"
      ></or-mwc-dialog>
      <slot class="notification-form-slot"></slot>
    `;
  }
}
