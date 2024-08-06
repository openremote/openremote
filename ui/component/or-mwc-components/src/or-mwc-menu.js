var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { classMap } from 'lit/directives/class-map.js';
import { MDCMenu } from "@material/menu";
import { DefaultColor4, DefaultColor8 } from "@openremote/core";
// @ts-ignore
import listStyle from "@material/list/dist/mdc.list.css";
// @ts-ignore
import menuSurfaceStyle from "@material/menu-surface/dist/mdc.menu-surface.css";
import { getItemTemplate, getListTemplate, ListType } from "./or-mwc-list";
// @ts-ignore
const menuStyle = require("@material/menu/dist/mdc.menu.css");
export class OrMwcMenuChangedEvent extends CustomEvent {
    constructor(values) {
        super(OrMwcMenuChangedEvent.NAME, {
            detail: values,
            bubbles: true,
            composed: true
        });
    }
}
OrMwcMenuChangedEvent.NAME = "or-mwc-menu-changed";
export class OrMwcMenuClosedEvent extends CustomEvent {
    constructor() {
        super(OrMwcMenuClosedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrMwcMenuClosedEvent.NAME = "or-mwc-menu-closed";
export function getContentWithMenuTemplate(content, menuItems, selectedValues, valueChangedCallback, closedCallback, multiSelect = false, translateValues = true, midHeight = false, fullWidth = false) {
    const openMenu = (evt) => {
        if (!menuItems) {
            return;
        }
        evt.currentTarget.parentElement.lastElementChild.open();
    };
    return html `
        <span>
            <span @click="${openMenu}">${content}</span>
            ${menuItems ? html `<or-mwc-menu ?multiselect="${multiSelect}" @or-mwc-menu-closed="${() => { if (closedCallback) {
        closedCallback();
    } }}" @or-mwc-menu-changed="${(evt) => { if (valueChangedCallback) {
        valueChangedCallback(evt.detail);
    } }}" .translateValues="${translateValues}" .values="${selectedValues}" .menuItems="${menuItems}" .midHeight="${midHeight}" .fullWidth="${fullWidth}" id="menu"></or-mwc-menu>` : ``}
        </span>
    `;
}
// language=CSS
const style = css `
    :host {
        white-space: nowrap;
        --internal-or-mwc-input-color: var(--or-mwc-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-mwc-input-text-color: var(--or-mwc-input-text-color, var(--or-app-color8, ${unsafeCSS(DefaultColor8)}));
        
        --mdc-theme-primary: var(--internal-or-mwc-input-color);
        --mdc-theme-on-primary: var(--internal-or-mwc-input-text-color);
        --mdc-theme-secondary: var(--internal-or-mwc-input-color);
    }
    
    .mdc-list-item__graphic {
        margin-right: 16px;
    }   

    a {
        text-decoration: none;
        color: rgba(0, 0, 0, 0.87);
    }
     
    .mdc-menu-surface-mid-height {
        max-height: calc(45vh - 32px) !important;
    }
    .mdc-menu-surface-full-width {
        width: 100%;
    }
`;
let OrMwcMenu = class OrMwcMenu extends LitElement {
    static get styles() {
        return [
            css `${unsafeCSS(listStyle)}`,
            css `${unsafeCSS(menuStyle)}`,
            css `${unsafeCSS(menuSurfaceStyle)}`,
            style
        ];
    }
    open() {
        this._mdcComponent.open = true;
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }
    render() {
        if (!this.menuItems || this.menuItems.length === 0) {
            return html ``;
        }
        const content = this.getItemsTemplate(this.menuItems, this.translateValues);
        const isTwoLine = this.menuItems && this.menuItems.some((item) => item && !Array.isArray(item) && !!item.secondaryText);
        const classes = {
            'mdc-menu-surface-mid-height': (this.midHeight ? 1 : 0),
            'mdc-menu-surface-full-width': (this.fullWidth ? 1 : 0)
        };
        return html `
            <div id="wrapper" class="mdc-menu-surface--anchor">
                <div class="mdc-menu mdc-menu-surface ${classMap(classes)}" id="menu" @MDCMenuSurface:closed="${this._onMenuClosed}">
                    ${getListTemplate(ListType.MULTI_TICK, content, isTwoLine, "menu")}
                </div>
            </div>
        `;
    }
    getItemsTemplate(items, translate) {
        const type = this.multiSelect ? ListType.MULTI_TICK : ListType.PLAIN;
        return html `
            ${items.map((item, index) => {
            if (Array.isArray(item)) {
                return html `
                    <li>
                        <ul class="mdc-menu__selection-group">
                            ${this.getItemsTemplate(item, translate)}
                        </ul>
                    </li>
                `;
            }
            return getItemTemplate(item, index, (Array.isArray(this.values) ? this.values : this.values ? [this.values] : []), type, translate, (e, item) => this._itemClicked(e, item));
        })}`;
    }
    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        if (this._mdcElem) {
            this._mdcComponent = new MDCMenu(this._mdcElem);
            // This overrides the standard mdc menu body click capture handler as it doesn't work with webcomponents
            this._mdcComponent.menuSurface_.foundation.handleBodyClick = function (evt) {
                const el = evt.composedPath()[0]; // Use composed path not evt target to work with webcomponents
                if (this.adapter.isElementInContainer(el)) {
                    return;
                }
                this.close();
            };
            this._mdcComponent.quickOpen = true;
        }
    }
    updated(_changedProperties) {
        if (_changedProperties.has("visible")) {
            this._mdcComponent.open = this.visible || false;
        }
    }
    _onMenuClosed() {
        this.dispatchEvent(new OrMwcMenuClosedEvent());
    }
    _itemClicked(e, item) {
        e.stopPropagation();
        const value = item.value;
        if (!this.multiSelect) {
            this.values = value;
            this._mdcComponent.open = false;
        }
        else {
            if (!Array.isArray(this.values)) {
                this.values = this.values ? [this.values] : [];
            }
            const index = this.values.findIndex((v) => v === value);
            if (index >= 0) {
                this.values.splice(index, 1);
            }
            else {
                this.values.push(value);
            }
            this.requestUpdate();
        }
        this.dispatchEvent(new OrMwcMenuChangedEvent(this.values));
    }
};
__decorate([
    property({ type: Array })
], OrMwcMenu.prototype, "menuItems", void 0);
__decorate([
    property({ type: Array })
], OrMwcMenu.prototype, "values", void 0);
__decorate([
    property({ type: Boolean, attribute: true })
], OrMwcMenu.prototype, "multiSelect", void 0);
__decorate([
    property({ type: Boolean, attribute: true })
], OrMwcMenu.prototype, "visible", void 0);
__decorate([
    property({ type: Boolean, attribute: true })
], OrMwcMenu.prototype, "translateValues", void 0);
__decorate([
    property({ type: Boolean, attribute: false })
], OrMwcMenu.prototype, "midHeight", void 0);
__decorate([
    property({ type: Boolean, attribute: false })
], OrMwcMenu.prototype, "fullWidth", void 0);
__decorate([
    query("#wrapper")
], OrMwcMenu.prototype, "_wrapperElem", void 0);
__decorate([
    query("#menu")
], OrMwcMenu.prototype, "_mdcElem", void 0);
OrMwcMenu = __decorate([
    customElement("or-mwc-menu")
], OrMwcMenu);
export { OrMwcMenu };
//# sourceMappingURL=or-mwc-menu.js.map