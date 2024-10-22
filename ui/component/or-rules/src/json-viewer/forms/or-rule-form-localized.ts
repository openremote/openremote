import {i18next, translate} from "@openremote/or-translate";
import { LitElement, TemplateResult, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { LocalizedNotificationMessage} from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { when } from "lit/directives/when.js";
import { until } from "lit/directives/until.js";
import "./or-rule-form-email-message";
import "./or-rule-form-push-notification";
import ISO6391 from "iso-639-1";

@customElement("or-rule-form-localized")
export class OrRuleFormLocalized extends translate(i18next)(LitElement) {

    @property({type: Object})
    public message?: LocalizedNotificationMessage;

    @property({type: String})
    public type?: "push" | "email";

    @property()
    public languages?: string[];

    @property()
    public lang = "en";

    static get styles() {
        return css`
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
            <div>
                ${until(this._getLanguageSelectForm(this.lang, this.languages), html`Loading...`)}
                ${until(this._getNotificationForm(this.message, this.lang), html`Loading...`)}
            </div>
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
     * Internal function that returns the correct notification form, based on the type.
     * Based on {@link lang}, it uses the Notification configured for that language.
     */
    protected async _getNotificationForm(message = this.message, lang = this.lang): Promise<TemplateResult> {
        if(!message?.languages) {
            return html`<or-translate .value="${"errorOccurred"}"></or-translate>`;
        }
        const msg = message.languages[lang];
        const type = msg?.type || this.type;

        if(type === "push") {
            return html`
                <or-rule-form-push-notification .message="${msg}"></or-rule-form-push-notification>
            `;
        } else if(type === "email") {
            return html`
                <or-rule-form-email-message .message="${msg}"></or-rule-form-email-message>
            `;
        } else {
            return html`
                <or-translate .value="${"errorOccurred"}"></or-translate>
            `;
        }
    }
}
