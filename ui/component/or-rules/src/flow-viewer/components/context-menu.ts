import { LitElement, html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { ContextMenuEntry, ContextMenuButton } from "../models/context-menu-button";
import { translate, i18next } from "@openremote/or-translate";

@customElement("context-menu")
export class ContextMenu extends translate(i18next)(LitElement) {
    public static get opened() { return ContextMenu.main.isOpen; }

    public static get styles() {
        return css`
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

    public static open(x: number, y: number, container: Element, buttons: (ContextMenuEntry)[]) {
        ContextMenu.main.container = container;
        ContextMenu.main.style.top = y + "px";
        ContextMenu.main.style.left = x + "px";
        window.addEventListener("mousedown", ContextMenu.main.closeCallback);
        window.addEventListener("blur", ContextMenu.main.closeCallback);
        window.addEventListener("wheel", ContextMenu.main.closeCallback);
        ContextMenu.main.entries = buttons;
        ContextMenu.main.isOpen = true;
    }

    public static close() {
        window.removeEventListener("mousedown", ContextMenu.main.closeCallback);
        window.removeEventListener("blur", ContextMenu.main.closeCallback);
        window.removeEventListener("wheel", ContextMenu.main.closeCallback);
        ContextMenu.main.isOpen = false;
    }

    private static main: ContextMenu;
    private entries: ContextMenuEntry[] = [];

    @property({ attribute: false }) private isOpen = false;

    private container!: Element;

    protected firstUpdated() {
        ContextMenu.main = this;
    }

    protected updated() {
        if (!this.container) { return; }
        const bounds = this.container.getBoundingClientRect();
        const box = this.getBoundingClientRect();
        if (box.top + box.height > bounds.bottom) {
            this.style.top = box.top - box.height + "px";
        }

        if (box.left + box.width > bounds.right) {
            this.style.left = bounds.right - box.width + "px";
        }
    }

    protected render() {
        this.style.display = this.isOpen ? "unset" : "none";
        if (!this.isOpen) {
            return html``;
        }
        const elements = this.entries.map(
            (e) => {
                switch (e.type) {
                    case "button":
                        return this.buttonTemplate(e as ContextMenuButton);
                    case "separator":
                        return this.separatorTemplate();
                }
            }
        );
        return html`${elements.length > 0 ? elements : html`<div class="context-menu-button muted">...</div>`}`;
    }

    private closeCallback = () => {
        ContextMenu.close();
    }

    private buttonTemplate(button: ContextMenuButton) {
        const action = (e: MouseEvent) => {
            if (e.buttons !== 1) { return; }
            ContextMenu.close();
            if (button.action) {
                button.action();
            }
            e.stopImmediatePropagation();
            e.stopPropagation();
        };
        return html`
        <div class="context-menu-button ${(button.disabled || false) ? `muted` : ``}" @mousedown="${action}">
        ${button.icon ? html`<or-icon icon="${button.icon}"></or-icon>` : null}
        <span class="label">${button.label}</span></div>`;
    }

    private separatorTemplate() {
        return html`<div class="context-menu-separator"></div>`;
    }
}
