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
import type { JsonRule, RuleActionWebhook } from "@openremote/model";
import { css, html, LitElement, type PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import "./modals/or-rule-webhook-modal";
import "./forms/or-rule-form-webhook";
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
export class OrRuleActionWebhook extends LitElement {
  static get styles() {
    return style;
  }

  @property({ type: Object, attribute: false })
  public rule!: JsonRule;

  @property({ type: Object, attribute: false })
  public action!: RuleActionWebhook;

  protected _initialAction?: RuleActionWebhook;

  /* ---------------------- */

  connectedCallback() {
    this._initialAction = structuredClone(this.action);
    return super.connectedCallback();
  }

  willUpdate(changedProps: PropertyValues) {
    // If the rule property changes, we assume it is a "new rule", so the rollback cache no longer applies.
    if (changedProps.has("rule") && changedProps.get("rule") !== undefined) {
      this._initialAction = structuredClone(this.action);
    }

    return super.willUpdate(changedProps);
  }

  render() {
    // When 'cancel' is pressed, reset the ACTION to the initial state (all changes get removed)
    const onModalCancel = () => {
      if (!this._initialAction) {
        console.warn("Could not rollback webhook form.");
        return;
      }
      const initial = structuredClone(this._initialAction);

      if (JSON.stringify(this.action.webhook) !== JSON.stringify(initial.webhook)) {
        console.debug("Rolling back the webhook to former state...");
        this.action.webhook = initial.webhook;
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
      <div style="display: flex; align-items: center; height: 100%;">
        <or-rule-webhook-modal
          .action="${this.action}"
          @or-rules-webhook-modal-cancel="${onModalCancel}"
          @or-rules-webhook-modal-ok="${onModalOk}"
        >
          <or-rule-form-webhook .webhook="${this.action.webhook}"></or-rule-form-webhook>
        </or-rule-webhook-modal>
      </div>
    `;
  }
}
