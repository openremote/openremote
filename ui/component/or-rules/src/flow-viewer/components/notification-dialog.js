var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { i18next, translate } from "@openremote/or-translate";
let NotificationDialog = class NotificationDialog extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.buttonText = i18next.t("ok");
        this.message = " ";
    }
    static get styles() {
        return css `
        .message{
            width: 100%;
            padding: 15px 5px 25px 5px;
            text-align: center;
            max-width: 50vw;
            overflow: auto;
            user-select: text;
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
        <div class="message">${this.message}</div>
        <div class="container">
            <or-mwc-input type="button" unElevated label="${this.buttonText}"
                @click="${() => { this.dispatchEvent(new CustomEvent("closed")); }}">
            </or-mwc-input>
        </div>
        `;
    }
};
__decorate([
    property({ type: String })
], NotificationDialog.prototype, "buttonText", void 0);
__decorate([
    property({ type: String })
], NotificationDialog.prototype, "message", void 0);
NotificationDialog = __decorate([
    customElement("notification-dialog")
], NotificationDialog);
export { NotificationDialog };
//# sourceMappingURL=notification-dialog.js.map