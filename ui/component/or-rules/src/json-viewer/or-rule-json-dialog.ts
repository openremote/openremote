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
import { OrElement } from "@openremote/or-element";
import { customElement, property, query, queryAssignedElements, state } from "lit/decorators.js";
import { html } from "lit";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-vaadin-components/or-vaadin-dialog";
import type { OrVaadinDialog } from "@openremote/or-vaadin-components/or-vaadin-dialog";
import type { OrRuleForm } from "./forms/or-rule-form";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";

export class OrRulesActionDialogCancelEvent extends CustomEvent<void> {
  public static readonly NAME = "cancel";

  constructor() {
    super(OrRulesActionDialogCancelEvent.NAME, {
      bubbles: false,
      composed: true,
    });
  }
}

export class OrRulesActionDialogOkEvent extends CustomEvent<void> {
  public static readonly NAME = "ok";

  constructor() {
    super(OrRulesActionDialogOkEvent.NAME, {
      bubbles: false,
      composed: true,
    });
  }
}

/**
 * @fires {OrRulesActionDialogCancelEvent} cancel - Fires on pressing 'cancel' in the dialog
 * @fires {OrRulesActionDialogOkEvent} ok - Fires on pressing 'OK' in the dialog
 */
@customElement("or-rule-json-dialog")
export class OrRuleJsonDialog extends OrElement {
  @property({ type: Boolean })
  public readonly = false;

  @state()
  protected _invalid = false;

  @queryAssignedElements({ slot: undefined })
  protected _childNodes?: Array<HTMLElement>;

  @query("or-vaadin-dialog")
  protected _dialog?: OrVaadinDialog;

  override render() {
    return html`
      <or-vaadin-button @click=${() => this._openDialog()}>
        <slot name="button"></slot>
      </or-vaadin-button>
      <or-vaadin-dialog width="768px" no-close-on-esc no-close-on-outside-click @closed=${() => this._onClose()}>
        <h2 slot="header-content">
          <slot name="title"></slot>
        </h2>
        <slot></slot>
        <div slot="footer">
          <or-vaadin-button @click=${() => this._onCancel()}>
            <or-translate value="cancel"></or-translate>
          </or-vaadin-button>
          <or-vaadin-button theme="primary" ?disabled=${this.readonly || this._invalid} @click=${() => this._onOk()}>
            <or-translate value="ok"></or-translate>
          </or-vaadin-button>
        </div>
      </or-vaadin-dialog>
    `;
  }

  protected _openDialog() {
    this._dialog?.open();
    this._subscribeToValueChanges();
  }

  protected _subscribeToValueChanges() {
    this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onFormValueChange);
    this._onFormValueChange(); // Check validity once upon subscribing
  }

  protected _unsubscribeFromValueChanges() {
    this.removeEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onFormValueChange);
  }

  protected _onFormValueChange(ev?: OrRulesJsonRuleChangedEvent) {
    ev?.stopPropagation(); // Don't make parent elements aware of form changes; only upon closing the dialog.
    this._invalid =
      (this._childNodes
        ?.map((c) => c as unknown as OrRuleForm | HTMLInputElement)
        .filter((c) => c && c.checkValidity)
        .filter((c) => !c.checkValidity()).length ?? 0) > 0;
  }

  protected _onCancel() {
    this._dialog?.close();
    this.dispatchEvent(new OrRulesActionDialogCancelEvent());
  }

  protected _onOk() {
    this._dialog?.close();
    this.dispatchEvent(new OrRulesActionDialogOkEvent());
  }

  protected _onClose() {
    this._unsubscribeFromValueChanges();
  }
}
