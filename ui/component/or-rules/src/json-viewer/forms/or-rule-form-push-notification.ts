import {html, LitElement, css, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import {i18next, translate} from "@openremote/or-translate"
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {PushNotificationMessage, PushNotificationButton} from "@openremote/model";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
import {until} from "lit/directives/until.js";
import {when} from "lit/directives/when.js";

@customElement("or-rule-form-push-notification")
export class OrRuleFormPushNotification extends translate(i18next)(LitElement) {

    @property({type: Object})
    public message?: PushNotificationMessage;

    @query("#push-title")
    protected _pushTitleElem?: HTMLInputElement;

    @query("#push-body")
    protected _pushBodyElem?: HTMLInputElement;

    @query("#push-url")
    protected _pushUrlElem?: HTMLInputElement;

    @query("#push-button1")
    protected _pushButton1Elem?: HTMLInputElement;

    @query("#push-button2")
    protected _pushButton2Elem?: HTMLInputElement;

    static get styles() {
        return css`
            .input-small {
                min-width: auto;
            }

            or-vaadin-text-field, or-vaadin-text-area {
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
    protected async _getPushNotificationForm(message: PushNotificationMessage, onchange = async (ev: Event, msg?: PushNotificationMessage) => msg!): Promise<TemplateResult> {

        return html`
            <form style="display:grid">
                <or-vaadin-text-field id="push-title" value=${message.title} required
                                      @change=${(ev: Event) => onchange(ev, message).then(msg => this._onTitleChange(this._pushTitleElem!, msg))}>
                    <or-translate slot="label" value="subject"></or-translate>
                </or-vaadin-text-field>
                <or-vaadin-text-area id="push-body" value=${message.body} required style="min-height: 200px;"
                                     @change=${(ev: Event) => onchange(ev, message).then(msg => this._onBodyChange(this._pushBodyElem!, msg))}>
                    <or-translate slot="label" value="message"></or-translate>
                </or-vaadin-text-area>
                <or-vaadin-text-field id="push-url" value=${message.action?.url}
                                      @change=${(ev: Event) => onchange(ev, message).then(msg => this._onActionUrlChange(this._pushUrlElem!, msg))}>
                    <or-translate slot="label" value="openWebsiteUrl"></or-translate>
                </or-vaadin-text-field>

                <!-- Open in browser switch -->
                <or-mwc-input .value="${message.action?.openInBrowser}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onOpenInBrowserChange(ev, msg))}"
                              .label="${i18next.t("openInBrowser")}"
                              type="${InputType.SWITCH}"
                              fullWidth
                              placeholder=" "></or-mwc-input>

                <!-- Button controls -->
                <div style="display: flex; gap: 20px;">
                    <or-vaadin-text-field id="push-button1" value=${message.buttons?.[0]?.title} class="input-small"
                                          @change=${(ev: Event) => onchange(ev, message).then(msg => this._onButtonTitleChange(this._pushButton1Elem!, 0, msg))}>
                        <or-translate slot="label" value="buttonTextConfirm"></or-translate>
                    </or-vaadin-text-field>
                    <or-vaadin-text-field id="push-button2" value=${message.buttons?.[1]?.title} class="input-small"
                                          @change=${(ev: Event) => onchange(ev, message).then(msg => this._onButtonTitleChange(this._pushButton2Elem!, 1, msg))}>
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
        if(elem.checkValidity()) {
            message.title = elem.value;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        }
    }

    /**
     * HTML callback function when the body of a notification message has changed.
     */
    protected _onBodyChange(elem: HTMLInputElement, message: PushNotificationMessage) {
        if(elem.checkValidity()) {
            message.body = elem.value;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        }
    }

    /**
     * HTML callback function when the "click here to open URL" of a notification has changed.
     */
    protected _onActionUrlChange(elem: HTMLInputElement, message: PushNotificationMessage) {
        if(elem.checkValidity()) {
            message.action = message.action || {};
            message.action.url = elem.value;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        }
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
    protected _onButtonTitleChange(elem: HTMLInputElement, key: number, message: PushNotificationMessage) {
        if(elem.checkValidity()) {
            message.buttons = message.buttons || [];
            message.buttons[key] = {
                title: elem.value
            } as PushNotificationButton;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        }
    }
}
