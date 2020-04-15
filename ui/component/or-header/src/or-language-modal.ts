import {customElement, html, css, LitElement, property, query, unsafeCSS} from "lit-element";
import "@openremote/or-icon";
import i18next from "i18next";
import manager from "@openremote/core";
import { InputType } from "@openremote/or-input";
import {MDCDialog} from '@material/dialog';
const dialogStyle = require("!!raw-loader!@material/dialog/dist/mdc.dialog.css");

// language=CSS
const style = css`
    :host {
        position: relative;
    }
    
    #language-dialog-content {
            display: flex;
    }
    
    #language-picker {
        flex: 1 1 0;
    }

`;
interface languageOptions {
    [key: string]: string;
}

export const languageOptions: languageOptions = {
    "en": "english",
    "nl": "dutch",
    "fr": "french",
    "de": "german",
    "es": "spanish"
};

const getLanguageOptions = () => {
    return Object.entries(languageOptions).map(([key, value]) => {
        return [key, i18next.t(value)]
    });
}

@customElement("or-language-modal")
export class OrLanguageModal extends LitElement {


    @property({type: Boolean})
    isVisible?: boolean = false;

    @property({type: String})
    language: string = manager.language;

    @query("#mdc-dialog-language")
    protected _dialogElem!: HTMLElement;

    protected _dialog!: MDCDialog;
    
  
    static get styles() {
        return [
            css`${unsafeCSS(dialogStyle)}`,
            style
        ];
    }

    firstUpdated() {
        this.language = manager.language ? manager.language : "en";
        this._dialog = new MDCDialog(this._dialogElem);
    }

    protected render() {
        return html`
               <div id="mdc-dialog-language"
                class="mdc-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="my-dialog-title"
                aria-describedby="my-dialog-content">
                <div class="mdc-dialog__container">
                    <div class="mdc-dialog__surface">
                    <h2 class="mdc-dialog__title" id="my-dialog-title"><or-translate value="choose_lang"></or-translate></h2>
                    <div class="dialog-container mdc-dialog__content" id="language-dialog-content">
                        <or-input id="language-picker"
                                    .label="${i18next.t("language")}" 
                                    .type="${InputType.LIST}"
                                    .options="${getLanguageOptions()}"></or-input>
                    </div>
                    <footer class="mdc-dialog__actions">
                        <or-input class="button" 
                                slot="secondaryAction"
                                .type="${InputType.BUTTON}" 
                                label="${i18next.t("Cancel")}" 
                                class="mdc-button mdc-dialog__button" 
                                data-mdc-dialog-action="no"></or-input>

                        <or-input class="button" 
                            slot="primaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("ok")}" 
                            class="mdc-button mdc-dialog__button" 
                            data-mdc-dialog-action="yes"
                            @click="${this.changeLanguage}"></or-input>

                    </footer>
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
    }

    changeLanguage() {
        if(this.shadowRoot && this.shadowRoot.getElementById('language-picker')){
            const elm = this.shadowRoot.getElementById('language-picker') as HTMLInputElement;
            manager.language = elm.value;
            this.language = manager.language;
            this.requestUpdate();
        }
    }
}
