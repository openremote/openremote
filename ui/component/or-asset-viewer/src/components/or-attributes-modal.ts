import {customElement, html, css, LitElement, property, query, unsafeCSS} from "lit-element";
import "@openremote/or-icon";
import i18next from "i18next";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {MDCDialog} from "@material/dialog";
const dialogStyle = require("!!raw-loader!@material/dialog/dist/mdc.dialog.css");

export interface AttributesConfig {
    name: string;
    value: boolean;
}

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

@customElement("or-attributes-modal")
export class OrAttributesModal extends LitElement {

    @property()
    public selectedAttributes!: AttributesConfig[];

    @query("#mdc-dialog-add-remove-attributes")
    protected _dialogElem!: HTMLElement;

    protected _dialog!: MDCDialog;
    
    static get styles() {
        return [
            css`${unsafeCSS(dialogStyle)}`,
            style
        ];
    }

    firstUpdated() {
        this._dialog = new MDCDialog(this._dialogElem);
    }

    private checkboxChanged = (attributeName: string, newValue: boolean) => {
        const index = this.selectedAttributes.findIndex((attr: AttributesConfig) => attr.name === attributeName);
        this.selectedAttributes[index] = {name: attributeName, value: newValue};
    }

    protected render() {
        return html`
            <div id="mdc-dialog-add-remove-attributes"
                class="mdc-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="my-dialog-title"
                aria-describedby="my-dialog-content">
                <div class="mdc-dialog__container">
                    <div class="mdc-dialog__surface">
                    <h2 class="mdc-dialog__title" id="my-dialog-title">${i18next.t("add_remove_attributes")}</h2>
                    <div class="dialog-container mdc-dialog__content" id="language-dialog-content" style="display:grid">
                    ${this.selectedAttributes.map((attribute: AttributesConfig) => {
                        return html`<div style="grid-column: 1 / -1;">
                            <or-input .type="${InputType.CHECKBOX}" .label="${attribute.name}" .value="${attribute.value}"
                                @or-input-changed="${(evt: OrInputChangedEvent) => this.checkboxChanged(attribute.name, evt.detail.value)}"></or-input>
                        </div>`;
                    })}
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
                            data-mdc-dialog-action="yes"></or-input>

                    </footer>
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
    }
}
