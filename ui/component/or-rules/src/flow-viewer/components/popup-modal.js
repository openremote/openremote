var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { modal } from "./flow-editor";
import { translate, i18next } from "@openremote/or-translate";
let PopupModal = class PopupModal extends translate(i18next)(LitElement) {
    constructor() {
        super();
        this.closeButton = true;
        this.isOpen = false;
    }
    close() {
        this.isOpen = false;
    }
    open() {
        this.isOpen = true;
    }
    static get styles() {
        return css `
        :host{
            position: absolute;
            left: 0;
            right: 0;
            top: 0;
            bottom: 0;

            display: flex;
            justify-content: center;
            align-items: center;

            background: rgba(0, 0, 0, 0.2);
            z-index: 10000;

            --topbar-height: 42px;
            --closebutton-padding: 12px;
            --modal-padding: 10px;
        }
        :host(:not([isopen])){
            display: none;
        }
        .modal{
            min-width: 300px;
            min-height: 64px;
            width: auto;
            height: auto;
            display: inline-block;
            background: white;
            border-radius: var(--roundness);
            position: relative;
            padding: var(--modal-padding);
        }
        .close-button{
            position: absolute;
            top: 0;
            right: 0;
            width: calc(var(--topbar-height) - var(--closebutton-padding) * 2);
            height: calc(var(--topbar-height) - var(--closebutton-padding) * 2);
            padding: var(--closebutton-padding);
            cursor: pointer;
        }
        .title{
            margin: 0;
            height: calc(var(--topbar-height) - var(--modal-padding));
            width: 100%;
            text-transform: uppercase;
            font-weight: bold;
        }`;
    }
    firstUpdated() {
        modal.element = this;
        this.addEventListener("mousedown", this.close);
        window.addEventListener("keyup", (e) => {
            if (e.key === "Escape") {
                this.close();
            }
        });
    }
    render() {
        if (!this.isOpen) {
            this.style.display = "none";
            return html ``;
        }
        this.style.display = "";
        return html `
        <div class="modal" @mousedown="${(e) => { e.stopPropagation(); }}">
            <div class="title">${this.header}</div>
            ${this.content}
            ${this.closeButton ? html `<or-icon class="close-button" icon="window-close" @click="${this.close}"></or-icon>` : null}
        </div>
        `;
    }
};
__decorate([
    property({ type: Boolean })
], PopupModal.prototype, "closeButton", void 0);
__decorate([
    property({ type: String })
], PopupModal.prototype, "header", void 0);
__decorate([
    property({ attribute: false })
], PopupModal.prototype, "content", void 0);
__decorate([
    property({ type: Boolean, reflect: true })
], PopupModal.prototype, "isOpen", void 0);
PopupModal = __decorate([
    customElement("popup-modal")
], PopupModal);
export { PopupModal };
//# sourceMappingURL=popup-modal.js.map