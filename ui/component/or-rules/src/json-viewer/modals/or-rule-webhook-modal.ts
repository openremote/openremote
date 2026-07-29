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
import type { RuleActionWebhook } from "@openremote/model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import type { DialogAction, OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { i18next } from "@openremote/or-translate";
import { html, LitElement, type PropertyValues } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import { isFormValid } from "../util";

export class OrRulesWebhookModalCancelEvent extends CustomEvent<void> {
  public static readonly NAME = "or-rules-webhook-modal-cancel";

  constructor() {
    super(OrRulesWebhookModalCancelEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

export class OrRulesWebhookModalOkEvent extends CustomEvent<void> {
  public static readonly NAME = "or-rules-webhook-modal-ok";

  constructor() {
    super(OrRulesWebhookModalOkEvent.NAME, {
      bubbles: true,
      composed: true,
    });
  }
}

@customElement("or-rule-webhook-modal")
export class OrRuleWebhookModal extends LitElement {
  @property({ type: Object })
  protected action!: RuleActionWebhook;

  @property({ type: String })
  public title: string = i18next.t("message");

  @query("#webhook-modal")
  protected _orMwcDialog?: OrMwcDialog;

  /* ----------------------- */

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

  firstUpdated(changedProperties: PropertyValues) {
    if (changedProperties.has("action")) {
      this.renderDialogHTML(this.action);
    }
    // Possibly, a render is triggered by renderDialogHTML(), so we await the pending update. (if there is any)
    this.getUpdateComplete().finally(() => {
      this.validateForm();
    });
  }

  renderDialogHTML(action: RuleActionWebhook) {
    const dialog: OrMwcDialog = this.shadowRoot!.getElementById("webhook-modal") as OrMwcDialog;
    if (!this.shadowRoot) return;

    const slot: HTMLSlotElement | null = this.shadowRoot.querySelector(".webhook-form-slot");
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
    const form = this._orMwcDialog?.shadowRoot?.querySelector("or-rule-form-webhook");
    return isFormValid(form?.shadowRoot);
  }

  render() {
    if (!this.action) {
      return html`${i18next.t("errorOccurred")}`;
    }
    const webhookModalActions: DialogAction[] = [
      {
        actionName: "cancel",
        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
        action: () => this.dispatchEvent(new OrRulesWebhookModalCancelEvent()),
      },
      {
        actionName: "ok",
        content: "ok",
        disabled: true, // (by default, can be changed in validateForm())
        action: () => this.dispatchEvent(new OrRulesWebhookModalOkEvent()),
      },
    ];
    const webhookModalOpen = () => {
      const dialog: OrMwcDialog = this.shadowRoot!.getElementById("webhook-modal") as OrMwcDialog;
      if (dialog) {
        dialog.open();
      }
    };
    return html`
      <or-vaadin-button @click=${() => webhookModalOpen()}>
        <or-translate value="message"></or-translate>
      </or-vaadin-button>
      <or-mwc-dialog id="webhook-modal" heading="${this.title}" .actions="${webhookModalActions}"></or-mwc-dialog>
      <slot class="webhook-form-slot"></slot>
    `;
  }
}
