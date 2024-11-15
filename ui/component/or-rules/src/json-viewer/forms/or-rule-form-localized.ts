import {i18next, translate} from "@openremote/or-translate";
import {LitElement, PropertyValues, TemplateResult, css, html} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {AbstractNotificationMessageUnion, LocalizedNotificationMessage} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {guard} from "lit/directives/guard.js";
import "./or-rule-form-email-message";
import "./or-rule-form-push-notification";
import ISO6391 from "iso-639-1";
import {DefaultColor6} from "@openremote/core";

@customElement("or-rule-form-localized")
export class OrRuleFormLocalized extends translate(i18next)(LitElement) {

    @property({type: Object})
    public message?: LocalizedNotificationMessage;

    @property({type: String})
    public type: "push" | "email" = "push";

    @property()
    public languages: string[] = ["en"];

    @property()
    public defaultLang = "en";

    @property()
    public wrongLanguage = false;

    @state()
    protected _selectedLanguage = "en";

    @state()
    protected _validLanguages?: string[];

    static get styles() {
        return css`
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

    connectedCallback() {
        super.connectedCallback();
        if(!this.defaultLang || !this.languages.includes(this.defaultLang)) {
            this.defaultLang = this.languages[0];
        }
        this._selectedLanguage = this.defaultLang;
    }

    protected render() {
        return html`
            <div>
                ${when(this.wrongLanguage, () => until(this._getWrongLanguageTemplate()))}
                ${guard([this.message, this._selectedLanguage, this.languages, this.type], () => html`
                    ${until(this._getLanguageSelectForm(this._selectedLanguage, this.languages), html`Loading...`)}
                    ${until(this._getNotificationForm(this.message, this._selectedLanguage), html`Loading...`)}
                `)}
                ${when(this.languages?.length && this._validLanguages && (this._validLanguages.length < this.languages.length), 
                        () => until(this._getLanguageErrorTemplate(this.languages!, this._validLanguages!))
                )}
            </div>
        `;
    }

    /**
     * Internal function that returns we "the default language has changed! Fix here" warning. (using {@link TemplateResult})
     * This template includes a button that will update the default language to the correct one.
     */
    protected async _getWrongLanguageTemplate(): Promise<TemplateResult> {
        return html`
            <or-mwc-input .type="${InputType.BUTTON}" fullWidth outlined
                          label="defaultLanguageChangedError" style="margin-top: 10px;"
                          @or-mwc-input-changed="${this._fixDefaultLanguage}"
            ></or-mwc-input>
        `;
    }

    /**
     * Function that corrects the default language in the {@link LocalizedNotificationMessage},
     * to the one provided by parent elements using {@link defaultLang}.
     * The {@link wrongLanguage} property will be updated to 'false'.
     */
    protected _fixDefaultLanguage(): void {
        if(this.message) {
            console.debug("Updating default language from " + this.message.defaultLanguage + " to " + this.defaultLang);
            this.message.defaultLanguage = this.defaultLang;
            this.wrongLanguage = false;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        } else {
            console.warn("Could not fix language, because the message could not be found.");
            showSnackbar(undefined, "Could not fix language, because the message could not be found.");
        }
    }

    /**
     * Internal function that returns the "select language" controls form. (using {@link TemplateResult})
     * Based on {@link languageCodes}, it lists all languages of the ISO6391 specification.
     */
    protected async _getLanguageSelectForm(selected: string, languageCodes: string[] = [this._selectedLanguage], divider = true): Promise<TemplateResult> {
        const languages = languageCodes.map(key => [key, ISO6391.getName(key)]);
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
        this._selectedLanguage = ev.detail.value;
    }

    /**
     * Internal function that returns the correct notification form, based on the type.
     * Based on {@link lang}, it uses the Notification configured for that language.
     */
    protected async _getNotificationForm(message = this.message, lang = this._selectedLanguage): Promise<TemplateResult> {
        if(!message?.languages) {
            return html`<or-translate .value="${"errorOccurred"}"></or-translate>`;
        }
        if(!message.languages[lang]) {
            message.languages[lang] = {
                type: this.type
            };
        }
        const msg = message.languages[lang];

        if(msg.type === "push") {
            return html`
                <or-rule-form-push-notification .message="${msg}"></or-rule-form-push-notification>
            `;
        } else if(msg.type === "email") {
            return html`
                <or-rule-form-email-message .message="${msg}"></or-rule-form-email-message>
            `;
        } else {
            return html`
                <or-translate .value="${"errorOccurred"}"></or-translate>
            `;
        }
    }

    /**
     * Internal function that returns the "language error" text, if a message is invalid. (using {@link TemplateResult})
     * Based on {@link validLanguages} and the listed {@link languages}, it shows a string of which ones are invalid.
     */
    protected async _getLanguageErrorTemplate(languages: string[], validLanguages: string[]): Promise<TemplateResult> {
        const invalidLocalesStr = languages.filter(l => !validLanguages.includes(l)).map(l => ISO6391.getName(l)).join(", ");
        return html`
            <div style="margin-top: 10px; display: flex; justify-content: end; color: ${DefaultColor6};">
                <or-translate value="languagesInvalidError"></or-translate>
                <span style="padding-left: 4px;">${invalidLocalesStr}</span>
            </div>
        `;
    }

    /**
     * A public function that checks if the localized {@link message} is valid.
     * By looping through the languages, we verify for each email- and push notification if some required variables are empty.
     * Returns true, or false if ANY language is not valid.
     */
    public isValid(): boolean {
        if(this.message?.languages && this.languages?.length) {

            // First, cleanup the message
            this._cleanNotificationMessage();

            const validLanguages = this.languages?.filter(lang => {
                if(!this.message?.languages?.[lang]) {
                    return true;
                }
                const msg = this.message.languages[lang];
                switch(msg.type) {
                    case "email":
                        return msg.subject && msg.html;
                    case "push":
                        return msg.title && msg.body;
                    case "localized":
                        return false;
                    default:
                        return true;
                }
            });
            // Update cached list of valid languages
            if(JSON.stringify(this._validLanguages) !== JSON.stringify(validLanguages)) {
                this._validLanguages = validLanguages;
            }

            return validLanguages.length === this.languages.length;
        }
        return false;
    }

    /**
     * Function that cleans up the localized notification message, by removing empty (or not filled in-) messages.
     * By looping through the languages, checking the amount of keys in the JS object, we delete languages that can be cleared.
     */
    protected _cleanNotificationMessage() {

        if(this.message?.languages) {
            const languageEntries = Object.entries(this.message.languages).filter(([lang, msg]) => {

                const userDefinedFields = Object.entries(msg).filter(entry => {
                    if(entry[0] === "type") return false;
                    if(entry[1] == null) return false; // key has no value
                    if(typeof entry[1] === "string" && entry[1].length === 0) return false;
                    if(Array.isArray(entry[1])) {
                        // Check if any object in the array has at least one key-value pair with a truthy value
                        const arrayHasValue = entry[1].some(obj =>
                            Object.keys(obj).length > 0 && Object.values(obj).some(value => value)
                        );
                        if(!arrayHasValue) return false;

                    } else if(typeof entry[1] === "object") {
                        if(Object.keys(entry[1]).length === 0 || Object.values(entry[1]).filter(value => value).length === 0) return false;
                    }
                    return true;
                });

                if(userDefinedFields.length === 0) {
                    console.debug(`Removing fields of notification language '${lang}', as they were all empty.`);
                    return false;
                }
                return true;
            });
            this.message.languages = Object.fromEntries(languageEntries) as {[p: string]: AbstractNotificationMessageUnion};
        }
    }
}
