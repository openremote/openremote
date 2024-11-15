import {html, LitElement, css, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {PushNotificationMessage, PushNotificationButton} from "@openremote/model";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
import {until} from "lit/directives/until.js";
import {when} from "lit/directives/when.js";

@customElement("or-rule-form-push-notification")
export class OrRuleFormPushNotification extends translate(i18next)(LitElement) {

    @property({type: Object})
    public message?: PushNotificationMessage;

    static get styles() {
        return css`
            .input-small {
                min-width: auto;
            }

            or-mwc-input {
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

    protected render() {
        return html`
            ${when(this.message,
                    () => until(this._getPushNotificationForm(this.message!), html`Loading...`),
                    () => html`<or-translate value="errorOccurred"></or-translate>`
            )}
        `;
    }

    /**
     * Internal function that returns a form for configuring a {@link PushNotificationMessage}.
     * {@link onchange} is a callback function can be used to process changes before they are applied.
     */
    protected async _getPushNotificationForm(message: PushNotificationMessage, onchange = async (_ev: OrInputChangedEvent, msg?: PushNotificationMessage) => msg!): Promise<TemplateResult> {

        return html`
            <form style="display:grid">
                <or-mwc-input .value="${message.title}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onTitleChange(ev, msg))}"
                              .label="${i18next.t("subject")}"
                              type="${InputType.TEXT}"
                              required
                              placeholder=" "></or-mwc-input>

                <or-mwc-input .value="${message.body}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onBodyChange(ev, msg))}"
                              .label="${i18next.t("message")}"
                              type="${InputType.TEXTAREA}"
                              required
                              placeholder=" "></or-mwc-input>
                <or-mwc-input .value="${message.action?.url}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onActionUrlChange(ev, msg))}"
                              .label="${i18next.t("openWebsiteUrl")}"
                              type="${InputType.TEXT}"
                              placeholder=" "></or-mwc-input>

                <!-- Open in browser switch -->
                <or-mwc-input .value="${message.action?.openInBrowser}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onOpenInBrowserChange(ev, msg))}"
                              .label="${i18next.t("openInBrowser")}"
                              type="${InputType.SWITCH}"
                              fullWidth
                              placeholder=" "></or-mwc-input>

                <!-- Button controls -->
                <div style="display: flex; gap: 20px;">
                    <or-mwc-input .value="${message.buttons?.[0]?.title}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onButtonTitleChange(ev, 0, msg))}"
                                  .label="${i18next.t("buttonTextConfirm")}"
                                  type="${InputType.TEXT}"
                                  class="input-small"
                                  placeholder=" "></or-mwc-input>
                    <or-mwc-input .value="${message.buttons?.[1]?.title}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onButtonTitleChange(ev, 1, msg))}"
                                  .label="${i18next.t("buttonTextDecline")}"
                                  type="${InputType.TEXT}"
                                  class="input-small"
                                  placeholder=" "></or-mwc-input>
                </div>
            </form>
        `;
    }

    /**
     * HTML callback function when the subject of a notification message has changed.
     */
    protected _onTitleChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.title = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }

    /**
     * HTML callback function when the body of a notification message has changed.
     */
    protected _onBodyChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.body = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }

    /**
     * HTML callback function when the "click here to open URL" of a notification has changed.
     */
    protected _onActionUrlChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.action = message.action || {};
        message.action.url = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }

    /**
     * HTML callback function when the "open in browser switch" of a notification has changed.
     */
    protected _onOpenInBrowserChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.action = message.action || {};
        message.action.openInBrowser = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }

    /**
     * HTML callback function when any button text of the notification has changed.
     */
    protected _onButtonTitleChange(ev: OrInputChangedEvent, key: number, message: PushNotificationMessage) {
        message.buttons = message.buttons || [];
        message.buttons[key] = {
            title: ev.detail.value
        } as PushNotificationButton;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
    }
}
