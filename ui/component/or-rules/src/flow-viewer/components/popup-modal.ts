import { LitElement, css, html, TemplateResult } from "lit";
import {customElement, property} from "lit/decorators.js";
import { modal } from "./flow-editor";
import { translate, i18next } from "@openremote/or-translate";

@customElement("popup-modal")
export class PopupModal extends translate(i18next)(LitElement) {
    @property({ type: Boolean }) public closeButton = true;
    @property({ type: String }) public header?: string;
    @property({ attribute: false }) public content?: TemplateResult;

    @property({ type: Boolean, reflect: true }) private isOpen = false;

    constructor() {
        super();
    }

    public close() {
        this.isOpen = false;
    }

    public open() {
        this.isOpen = true;
    }

    public static get styles() {
        return css`
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

    protected firstUpdated() {
        modal.element = this;
        this.addEventListener("mousedown", this.close);
        window.addEventListener("keyup", (e: KeyboardEvent) => {
            if (e.key === "Escape") {
                this.close();
            }
        });
    }

    protected render() {
        if (!this.isOpen) {
            this.style.display = "none";
            return html``;
        }
        this.style.display = "";
        return html`
        <div class="modal" @mousedown="${(e: MouseEvent) => { e.stopPropagation(); }}">
            <div class="title">${this.header}</div>
            ${this.content}
            ${this.closeButton ? html`<or-icon class="close-button" icon="window-close" @click="${this.close}"></or-icon>` : null}
        </div>
        `;
    }
}
