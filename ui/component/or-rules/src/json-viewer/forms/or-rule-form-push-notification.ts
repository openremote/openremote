import {html, LitElement, css, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {RuleActionNotification, PushNotificationMessage, AbstractNotificationMessageUnion, PushNotificationLocalizedMessage, PushNotificationButton} from "@openremote/model";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import ISO6391 from "iso-639-1";

@customElement("or-rule-form-push-notification")
export class OrRuleFormPushNotification extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;

    @property()
    public languages?: string[];

    @property()
    public lang = "en";


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
                border-top: 1px solid black;
            }
        `;
    }

    protected render() {
        return html`
            ${when(this._isLocalized(this.action.notification?.message) && this.languages?.length, () => until(this._getLanguageSelectForm(this.lang, this.languages), html`Loading...`))}
            ${until(this._getNotificationForm(this.action.notification?.message, this.lang), html`Loading...`)}
        `;
    }

    /**
     * Internal function that returns the "select language" controls form. (using {@link TemplateResult})
     * Based on {@link languageCodes}, it lists all languages of the ISO6391 specification.
     */
    protected async _getLanguageSelectForm(selected: string, languageCodes: string[] = [this.lang], divider = true): Promise<TemplateResult> {
        const languages = languageCodes.map(key => [key, ISO6391.getNativeName(key)]);
        return html`
            <div style="display: flex; justify-content: space-between;">
                <or-mwc-input .type="${InputType.SELECT}" .options="${languages}" .value="${selected}"
                              @or-mwc-input-changed="${this._onLanguageChange}"
                ></or-mwc-input>
            </div>
            ${when(divider, () => html`
                <div class="divider"></div>`)}
        `;
    }

    /**
     * HTML callback for when the selected language changes.
     */
    protected _onLanguageChange(ev: OrInputChangedEvent) {
        this.lang = ev.detail.value;
    }

    /**
     * Internal function that returns the notification form.
     * If a {@link PushNotificationLocalizedMessage} is provided, it will return the data of the selected {@link language}.
     * Otherwise, a normal {@link PushNotificationMessage} form is shown.
     */
    protected async _getNotificationForm(message?: AbstractNotificationMessageUnion, language?: string): Promise<TemplateResult> {
        if (language && this._isLocalized(message)) {
            return this._getLocalizedNotificationForm(message as PushNotificationLocalizedMessage, language);
        } else if (this._isPushNotification(message)) {
            return this._getPushNotificationForm(message as PushNotificationMessage);
        } else {
            return html`
                <or-translate .value="${"errorOccurred"}"></or-translate>`;
        }
    }

    /**
     * Internal function that returns a form, including the data of the selected {@link language}.
     * Also handles {@link onchange},
     */
    protected async _getLocalizedNotificationForm(message: PushNotificationLocalizedMessage, language: string): Promise<TemplateResult> {
        const translatedMessage = message.languages?.[language];
        return this._getPushNotificationForm(
            translatedMessage,
            async (_ev, pushMessage) => {
                if (!message.languages) {
                    message.languages = {};
                }

                // on change, we check if the language is not defined yet, and create one if necessary
                if (!pushMessage) {
                    pushMessage = {
                        type: "push"
                    } as PushNotificationMessage;
                }
                message.languages[language] = message.languages[language] || pushMessage;
                return pushMessage;
            });
    }

    /**
     * Internal function that returns a form for configuring a {@link PushNotificationMessage}.
     * {@link onchange} is a callback function can be used to process changes before they are applied.
     */
    protected async _getPushNotificationForm(message?: PushNotificationMessage, onchange = async (_ev: OrInputChangedEvent, msg?: PushNotificationMessage) => msg!): Promise<TemplateResult> {

        if (message && !this._isPushNotification(message)) {
            console.error("Could not get notification form; notification is not of type PUSH.");
            return html`
                <or-translate .value="${"errorOccurred"}"></or-translate>
            `;
        }

        return html`
            <form style="display:grid">
                <or-mwc-input .value="${message?.title}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onTitleChange(ev, msg))}"
                              .label="${i18next.t("subject")}"
                              type="${InputType.TEXT}"
                              required
                              placeholder=" "></or-mwc-input>

                <or-mwc-input .value="${message?.body}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onBodyChange(ev, msg))}"
                              .label="${i18next.t("message")}"
                              type="${InputType.TEXTAREA}"
                              required
                              placeholder=" "></or-mwc-input>
                <or-mwc-input .value="${message?.action?.url}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onActionUrlChange(ev, msg))}"
                              .label="${i18next.t("openWebsiteUrl")}"
                              type="${InputType.TEXT}"
                              required
                              placeholder=" "></or-mwc-input>

                <!-- Open in browser switch -->
                <or-mwc-input .value="${message?.action?.openInBrowser}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onOpenInBrowserChange(ev, msg))}"
                              .label="${i18next.t("openInBrowser")}"
                              type="${InputType.SWITCH}"
                              fullWidth
                              placeholder=" "></or-mwc-input>

                <!-- Button controls -->
                <div style="display: flex; gap: 20px;">
                    <or-mwc-input .value="${message?.buttons?.[0]?.title}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onButtonTitleChange(ev, 0, msg))}"
                                  .label="${i18next.t("buttonTextConfirm")}"
                                  type="${InputType.TEXT}"
                                  class="input-small"
                                  required
                                  placeholder=" "></or-mwc-input>
                    <or-mwc-input .value="${message?.buttons?.[1]?.title}"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onchange(ev, message).then(msg => this._onButtonTitleChange(ev, 0, msg))}"
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
        this.requestUpdate();
    }

    /**
     * HTML callback function when the body of a notification message has changed.
     */
    protected _onBodyChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.body = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    /**
     * HTML callback function when the "click here to open URL" of a notification has changed.
     */
    protected _onActionUrlChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.action = message.action || {};
        message.action.url = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    /**
     * HTML callback function when the "open in browser switch" of a notification has changed.
     */
    protected _onOpenInBrowserChange(ev: OrInputChangedEvent, message: PushNotificationMessage) {
        message.action = message.action || {};
        message.action.openInBrowser = ev.detail.value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
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
        this.requestUpdate();
    }

    /**
     * Internal utility function to check if {@link message} is a "multi-language" notification or not.
     */
    protected _isLocalized(message?: AbstractNotificationMessageUnion)/*: message is PushNotificationLocalizedMessage*/ {
        return message?.type === "push_localized";
    }

    /**
     * Internal utility function to check if {@link message} is a regular push notification (and for example not a "multi-language" one)
     */
    protected _isPushNotification(message?: AbstractNotificationMessageUnion)/*: message is PushNotificationMessage*/ {
        return message?.type === "push";
    }
}
