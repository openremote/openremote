var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var ContextMenu_1;
import { LitElement, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { translate, i18next } from "@openremote/or-translate";
let ContextMenu = ContextMenu_1 = class ContextMenu extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.entries = [];
        this.isOpen = false;
        this.closeCallback = () => {
            ContextMenu_1.close();
        };
    }
    static get opened() { return ContextMenu_1.main.isOpen; }
    static get styles() {
        return css `
        :host{
            display: inline-block;
            position: fixed;
            background: white;
            outline: 1px solid rgb(200,200,200);
            box-shadow: rgba(0, 0, 0, 0.05) 0 2px 4px;
            font-size: 13px;
            z-index: 1000000;
        }
        .context-menu-button{
            color: rgb(0,0,0);
            display: grid;
            align-items: center;
            justify-items: start;
            grid-template-rows: 32px;
            grid-template-columns: 32px 220px;
            grid-template-areas:
                "icon label";
        }
        .context-menu-button:hover{
            background: whitesmoke;
        }
        .label
        {
            grid-area: label;
        }
        or-icon{
            justify-self: center;
            grid-area: icon;
            height: 16px;
            width: 16px;
        }
        .muted{
            pointer-events: none;
            color: rgb(150,150,150);
        }
        .context-menu-separator{
            --thickness: 1px;
            border: 0;
            height: var(--thickness);
            border-bottom: solid var(--thickness);
            border-color: rgb(234,234,234);
            padding: 0;
            margin: 5px 10px calc(5px - var(--thickness)) 10px;
        }`;
    }
    static open(x, y, container, buttons) {
        ContextMenu_1.main.container = container;
        ContextMenu_1.main.style.top = y + "px";
        ContextMenu_1.main.style.left = x + "px";
        window.addEventListener("mousedown", ContextMenu_1.main.closeCallback);
        window.addEventListener("blur", ContextMenu_1.main.closeCallback);
        window.addEventListener("wheel", ContextMenu_1.main.closeCallback);
        ContextMenu_1.main.entries = buttons;
        ContextMenu_1.main.isOpen = true;
    }
    static close() {
        window.removeEventListener("mousedown", ContextMenu_1.main.closeCallback);
        window.removeEventListener("blur", ContextMenu_1.main.closeCallback);
        window.removeEventListener("wheel", ContextMenu_1.main.closeCallback);
        ContextMenu_1.main.isOpen = false;
    }
    firstUpdated() {
        ContextMenu_1.main = this;
    }
    updated() {
        if (!this.container) {
            return;
        }
        const bounds = this.container.getBoundingClientRect();
        const box = this.getBoundingClientRect();
        if (box.top + box.height > bounds.bottom) {
            this.style.top = box.top - box.height + "px";
        }
        if (box.left + box.width > bounds.right) {
            this.style.left = bounds.right - box.width + "px";
        }
    }
    render() {
        this.style.display = this.isOpen ? "unset" : "none";
        if (!this.isOpen) {
            return html ``;
        }
        const elements = this.entries.map((e) => {
            switch (e.type) {
                case "button":
                    return this.buttonTemplate(e);
                case "separator":
                    return this.separatorTemplate();
            }
        });
        return html `${elements.length > 0 ? elements : html `<div class="context-menu-button muted">...</div>`}`;
    }
    buttonTemplate(button) {
        const action = (e) => {
            if (e.buttons !== 1) {
                return;
            }
            ContextMenu_1.close();
            if (button.action) {
                button.action();
            }
            e.stopImmediatePropagation();
            e.stopPropagation();
        };
        return html `
        <div class="context-menu-button ${(button.disabled || false) ? `muted` : ``}" @mousedown="${action}">
        ${button.icon ? html `<or-icon icon="${button.icon}"></or-icon>` : null}
        <span class="label">${button.label}</span></div>`;
    }
    separatorTemplate() {
        return html `<div class="context-menu-separator"></div>`;
    }
};
__decorate([
    property({ attribute: false })
], ContextMenu.prototype, "isOpen", void 0);
ContextMenu = ContextMenu_1 = __decorate([
    customElement("context-menu")
], ContextMenu);
export { ContextMenu };
//# sourceMappingURL=context-menu.js.map