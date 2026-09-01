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
import type { JsonRule, RuleActionWebhook, Webhook } from "@openremote/model";
import { css, html } from "lit";
import { OrElement } from "@openremote/or-element";
import { customElement, property } from "lit/decorators.js";
import "./or-rule-json-dialog";
import "./forms/or-rule-form-webhook";
import type { OrRulesActionDialogCancelEvent, OrRulesActionDialogOkEvent } from "./or-rule-json-dialog";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";

// language=CSS
const style = css`
  :host {
    height: 100%;
    margin: 2px 3px auto 0;
  }

  :host > * {
    margin: 0 3px 6px;
  }

  .min-width {
    flex: 0 0 240px;
  }
`;

@customElement("or-rule-action-webhook")
export class OrRuleActionWebhook extends OrElement {
  static get styles() {
    return style;
  }

  @property({ type: Object, attribute: false })
  public rule!: JsonRule;

  @property({ type: Object, attribute: false })
  public action!: RuleActionWebhook;

  @property({ type: Boolean })
  public readonly?: boolean;

  protected _initialWebhook?: Webhook;

  override connectedCallback() {
    this._initialWebhook = structuredClone(this.action.webhook);
    super.connectedCallback();
  }

  /* ---------------------- */

  override render() {
    // When 'cancel' is pressed, reset ACTION to the initial state (all changes get removed)
    const onModalCancel = (_ev: OrRulesActionDialogCancelEvent) => {
      if (this._initialWebhook && this.action.webhook) {
        const initialWebhook = structuredClone(this._initialWebhook);

        // Check if anything in the message has changed
        if (JSON.stringify(this.action.webhook) !== JSON.stringify(initialWebhook)) {
          this.action.webhook = initialWebhook;
          this.requestUpdate("action");
        }
      } else {
        console.warn("Could not rollback webhook form.");
      }
    };

    const onModalOk = (_ev: OrRulesActionDialogOkEvent) => {
      this._initialWebhook = structuredClone(this.action.webhook); // update initial action for opening the modal in the future
      this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    };

    return html`
      <or-rule-json-dialog ?readonly=${this.readonly} @cancel="${onModalCancel}" @ok="${onModalOk}">
        <or-translate slot="button" value="message"></or-translate>
        <or-translate slot="title" value="message"></or-translate>
        <or-rule-form-webhook .webhook="${this.action.webhook}"></or-rule-form-webhook>
      </or-rule-json-dialog>
    `;
  }
}
