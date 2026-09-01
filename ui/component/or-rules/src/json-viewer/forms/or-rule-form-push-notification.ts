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
import { html, css, type TemplateResult } from "lit";
import { OrElement } from "@openremote/or-element";
import { customElement, property, query } from "lit/decorators.js";
import { i18next, translate } from "@openremote/or-translate";
import type { PushNotificationMessage, PushNotificationButton } from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import { until } from "lit/directives/until.js";
import { when } from "lit/directives/when.js";
import "@openremote/or-vaadin-components/or-vaadin-toggle";
import { isFormValid, type OrRuleForm } from "./or-rule-form";

@customElement("or-rule-form-push-notification")
export class OrRuleFormPushNotification extends translate(i18next)(OrElement) implements OrRuleForm {
  @property({ type: Object })
  public message?: PushNotificationMessage;

  @property({ type: Boolean })
  public readonly?: boolean;

  @query("#push-title")
  protected _pushTitleElem?: HTMLInputElement;

  @query("#push-body")
  protected _pushBodyElem?: HTMLInputElement;

  @query("#push-url")
  protected _pushUrlElem?: HTMLInputElement;

  @query("#push-browser-toggle")
  protected _pushBrowserToggleElem?: HTMLInputElement;

  @query("#push-button1")
  protected _pushButton1Elem?: HTMLInputElement;

  @query("#push-button2")
  protected _pushButton2Elem?: HTMLInputElement;

  static get styles() {
    return css`
      .input-small {
        min-width: auto;
      }

      or-vaadin-text-field,
      or-vaadin-text-area,
      or-vaadin-toggle {
        margin-bottom: 20px;
        min-width: 420px;
        width: 100%;
      }

      .divider {
        margin-bottom: 20px;
        border-top: 1px solid rgba(0, 0, 0, 12%);
      }
    `;
  }

  checkValidity(): boolean {
    return isFormValid(this.renderRoot);
  }

  protected render() {
    return html`
      ${when(
        this.message,
        () => until(this._getPushNotificationForm(this.message!), html`Loading...`),
        () => html`<or-translate value="errorOccurred"></or-translate>`
      )}
    `;
  }

  /**
   * Internal function that returns a form for configuring a {@link PushNotificationMessage}.
   * {@link onchange} is a callback function can be used to process changes before they are applied.
   */
  protected async _getPushNotificationForm(
    message: PushNotificationMessage,
    onchange = async (ev: Event, msg?: PushNotificationMessage) => msg!
  ): Promise<TemplateResult> {
    return html`
      <form id="form-container" style="display:grid">
        <or-vaadin-text-field
          id="push-title"
          value=${message.title}
          required
          ?readonly=${this.readonly}
          @change=${(ev: Event) => onchange(ev, message).then((msg) => this._onTitleChange(this._pushTitleElem!, msg))}
        >
          <or-translate slot="label" value="title"></or-translate>
        </or-vaadin-text-field>
        <or-vaadin-text-area
          id="push-body"
          value=${message.body}
          required
          ?readonly=${this.readonly}
          style="min-height: 200px;"
          @change=${(ev: Event) => onchange(ev, message).then((msg) => this._onBodyChange(this._pushBodyElem!, msg))}
        >
          <or-translate slot="label" value="body"></or-translate>
        </or-vaadin-text-area>
        <or-vaadin-text-field
          id="push-url"
          type="url"
          pattern="^[a-zA-Z][a-zA-Z0-9+.\\-]*://.+$"
          error-message="${i18next.t("invalidUrl")}"
          placeholder="https://example.com"
          value=${message.action?.url}
          ?readonly=${this.readonly}
          @change=${(ev: Event) => onchange(ev, message).then((msg) => this._onActionUrlChange(this._pushUrlElem!, msg))}
        >
          <or-translate slot="label" value="openWebsiteUrl"></or-translate>
        </or-vaadin-text-field>

        <!-- Open in browser switch -->
        <or-vaadin-toggle
          id="push-browser-toggle"
          ?checked="${message.action?.openInBrowser ?? false}"
          ?readonly=${this.readonly}
          @change="${(ev: Event) => onchange(ev, message).then((msg) => this._onOpenInBrowserChange(this._pushBrowserToggleElem!, msg))}"
        >
          <or-translate slot="label" value="openInBrowser"></or-translate>
        </or-vaadin-toggle>

        <!-- Button controls -->
        <div style="display: flex; gap: 20px;">
          <or-vaadin-text-field
            id="push-button1"
            value=${message.buttons?.[0]?.title}
            class="input-small"
            ?readonly=${this.readonly}
            @change=${(ev: Event) => onchange(ev, message).then((msg) => this._onButtonTitleChange(this._pushButton1Elem!, 0, msg))}
          >
            <or-translate slot="label" value="buttonTextConfirm"></or-translate>
          </or-vaadin-text-field>
          <or-vaadin-text-field
            id="push-button2"
            value=${message.buttons?.[1]?.title}
            class="input-small"
            ?readonly=${this.readonly}
            @change=${(ev: Event) => onchange(ev, message).then((msg) => this._onButtonTitleChange(this._pushButton2Elem!, 1, msg))}
          >
            <or-translate slot="label" value="buttonTextDecline"></or-translate>
          </or-vaadin-text-field>
        </div>
      </form>
    `;
  }

  /**
   * HTML callback function when the subject of a notification message has changed.
   */
  protected _onTitleChange(elem: HTMLInputElement, message: PushNotificationMessage) {
    message.title = elem.checkValidity() ? elem.value : undefined;
    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
  }

  /**
   * HTML callback function when the body of a notification message has changed.
   */
  protected _onBodyChange(elem: HTMLInputElement, message: PushNotificationMessage) {
    message.body = elem.checkValidity() ? elem.value : undefined;
    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
  }

  /**
   * HTML callback function when the "click here to open URL" of a notification has changed.
   */
  protected _onActionUrlChange(elem: HTMLInputElement, message: PushNotificationMessage) {
    message.action ??= {};
    message.action.url = elem.checkValidity() ? elem.value : undefined;
    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
  }

  /**
   * HTML callback function when the "open in browser switch" of a notification has changed.
   */
  protected _onOpenInBrowserChange(elem: HTMLInputElement, message: PushNotificationMessage) {
    message.action ??= {};
    message.action.openInBrowser = elem.checkValidity() ? elem.checked : undefined;
    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
  }

  /**
   * HTML callback function when any button text of the notification has changed.
   */
  protected _onButtonTitleChange(elem: HTMLInputElement, key: number, message: PushNotificationMessage) {
    message.buttons ??= [];
    if (elem.checkValidity()) {
      message.buttons[key] = {
        title: elem.value,
        action: key === 0 ? message.action : undefined,
      } as PushNotificationButton;
    } else {
      message.buttons[key] = {};
    }
    this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
  }
}
