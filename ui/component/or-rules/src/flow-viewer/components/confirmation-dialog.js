var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next, translate } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
let ConfirmationDialog = class ConfirmationDialog extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.agreeText = i18next.t("ok");
        this.disagreeText = i18next.t("cancel");
        this.question = i18next.t("areYouSure", "Are you sure?");
    }
    static get styles() {
        return css `
        .question{
            width: 100%;
            padding: 15px 5px 25px 5px;
            text-align: center;
        }
        .container{
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            justify-content: space-evenly;
            justify-content: space-around;
        }`;
    }
    render() {
        return html `
        <div class="question">${this.question}</div>
        <div class="container">
            <or-mwc-input type="${InputType.BUTTON}" unElevated label="${this.agreeText}"
                @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent("agreed")); }}">
            </or-mwc-input>

            <or-mwc-input type="button" label="${this.disagreeText}"
                @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent("disagreed")); }}">
            </or-mwc-input>
        </div>
        `;
    }
};
__decorate([
    property({ type: String })
], ConfirmationDialog.prototype, "agreeText", void 0);
__decorate([
    property({ type: String })
], ConfirmationDialog.prototype, "disagreeText", void 0);
__decorate([
    property({ type: String })
], ConfirmationDialog.prototype, "question", void 0);
ConfirmationDialog = __decorate([
    customElement("confirmation-dialog")
], ConfirmationDialog);
export { ConfirmationDialog };
//# sourceMappingURL=confirmation-dialog.js.map