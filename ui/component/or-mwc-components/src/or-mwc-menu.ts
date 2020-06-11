import {
    css,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    query,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import {styleMap} from "lit-html/directives/style-map";
import {MDCMenu} from "@material/menu";
import { DefaultColor8, DefaultColor4 } from "@openremote/core";

const listStyle = require("!!raw-loader!@material/list/dist/mdc.list.css");
const menuSurfaceStyle = require("!!raw-loader!@material/menu-surface/dist/mdc.menu-surface.css");
const menuStyle = require("!!raw-loader!@material/menu/dist/mdc.menu.css");
const checkboxStyle = require("!!raw-loader!@material/checkbox/dist/mdc.checkbox.css");

export interface MenuItem {
    icon?: string;
    trailingIcon?: string;
    text?: string;
    secondaryText?: string;
    value: string;
    styleMap?: {[style: string]: string};
}

export class OrMwcMenuChangedEvent extends CustomEvent<string | string[]> {

    public static readonly NAME = "or-mwc-menu-changed";

    constructor(values: string | string[]) {
        super(OrMwcMenuChangedEvent.NAME, {
            detail: values,
            bubbles: true,
            composed: true
        });
    }
}

export class OrMwcMenuClosedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-mwc-menu-closed";

    constructor() {
        super(OrMwcMenuClosedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMwcMenuChangedEvent.NAME]: OrMwcMenuChangedEvent;
        [OrMwcMenuClosedEvent.NAME]: OrMwcMenuClosedEvent;
    }
}

export function getContentWithMenuTemplate(content: TemplateResult, menuItems: (MenuItem | MenuItem[] | null)[], selectedValues: string[] | string | undefined, valueChangedCallback: (values: string[] | string) => void, closedCallback?: () => void, multiSelect = false): TemplateResult {

    const openMenu = (evt: Event) => {
        if (!menuItems) {
            return;
        }

        ((evt.currentTarget as Element).parentElement!.lastElementChild as OrMwcMenu).open();
    };

    return html`
        <span>
            <span @click="${openMenu}">${content}</span>
            ${menuItems ? html`<or-mwc-menu ?multiselect="${multiSelect}" @or-mwc-menu-closed="${() => {if (closedCallback) { closedCallback(); }} }" @or-mwc-menu-changed="${(evt: OrMwcMenuChangedEvent) => {if (valueChangedCallback) { valueChangedCallback(evt.detail); }} }" .values="${selectedValues}" .menuItems="${menuItems}" id="menu"></or-mwc-menu>` : ``}
        </span>
    `;
}

// language=CSS
const style = css`
    :host {
        white-space: nowrap;
        --internal-or-input-color: var(--or-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-input-text-color: var(--or-input-text-color, var(--or-app-color1, ${unsafeCSS(DefaultColor8)}));
        
        --mdc-theme-primary: var(--internal-or-input-color);
        --mdc-theme-on-primary: var(--internal-or-input-text-color);
        --mdc-theme-secondary: var(--internal-or-input-color);
    }
    
    .mdc-list-item__graphic {
        margin-right: 16px;
    }   

    a {
        text-decoration: none;
        color: rgba(0, 0, 0, 0.87);
    }
     
`;

@customElement("or-mwc-menu")
export class OrMwcMenu extends LitElement {

    static get styles() {
        return [
            css`${unsafeCSS(listStyle)}`,
            css`${unsafeCSS(menuStyle)}`,
            css`${unsafeCSS(menuSurfaceStyle)}`,
            css`${unsafeCSS(checkboxStyle)}`,
            style
        ];
    }

    @property({type: Array})
    public menuItems?: (MenuItem | MenuItem[] | null)[];

    @property({type: Array})
    public values?: string[] | string;

    @property({type: Boolean, attribute: true})
    public multiSelect?: boolean;

    @property({type: Boolean, attribute: true})
    public visible?: boolean;

    @property({type: Boolean, attribute: true})
    public twoLine?: boolean;

    public anchorElem?: HTMLElement;

    @query("#wrapper")
    protected _wrapperElem!: HTMLElement;

    @query("#menu")
    protected _mdcElem!: HTMLElement;

    protected _mdcComponent?: MDCMenu;

    public open() {
        this._mdcComponent!.open = true;
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }

    protected render() {

        if (!this.menuItems || this.menuItems.length === 0) {
            return html``;
        }

        return html`
            <div id="wrapper" class="mdc-menu-surface--anchor">
                <div class="mdc-menu mdc-menu-surface" id="menu" @MDCMenuSurface:closed="${this._onMenuClosed}">
                    <ul class="mdc-list ${this.twoLine ? "mdc-list--two-line" : ""}" role="menu" aria-hidden="true" aria-orientation="vertical" tabindex="-1">
                        ${this.getItemsTemplate(this.menuItems)}
                    </ul>
                </div>
            </div>
        `;
    }

    protected getItemsTemplate(items: (MenuItem | MenuItem[] | null)[]): TemplateResult {

        const hasIcon = this.multiSelect || items.find((mi) => mi && !Array.isArray(mi) && mi.icon);

        return html`
            ${items.map((item) => {
                if (item === null) {
                    return html`<li class="mdc-list-divider" role="separator"></li>`;
                }
                if (Array.isArray(item)) {
                    return html`
                        <li>
                            <ul class="mdc-menu__selection-group">
                                ${this.getItemsTemplate(item)}
                            </ul>
                        </li>
                    `;
                }

                const isSelected = this.isValueSelected((item as MenuItem).value);
                let icon = item.icon;
                const text = item.text !== undefined ? item.text : item.value;
                let leftTemplate: TemplateResult | string = ``;
                let rightTemplate: TemplateResult | string = ``;
                
                if (this.multiSelect) {
                    icon = isSelected ? "check" : undefined;
                }
                
                if (hasIcon) {
                    leftTemplate = html`<span class="mdc-list-item__graphic mdc-menu__selection-group-icon" aria-hidden="true">
                        <or-icon icon="${icon}"></or-icon>
                    </span>`;
                }
                
                if (item.trailingIcon) {
                    rightTemplate = html`<span class="mdc-list-item__meta" aria-hidden="true">
                        <or-icon icon="${item.trailingIcon}"></or-icon>
                    </span>`;
                }
                
                return html`
                    <li @click="${(e: MouseEvent) => {this._itemClicked(e, item)}}" style="${item.styleMap ? styleMap(item.styleMap) : ""}" class="mdc-list-item ${isSelected ? "mdc-menu-item--selected" : ""}" role="menuitem" aria-checked="${isSelected}">
                        ${leftTemplate}
                        ${!text ? html`` : html`
                            <span class="mdc-list-item__text">
                                <span class="${this.twoLine ? "mdc-list-item__primary-text" : ""}"><or-translate value="${item.text}"></or-translate></span>
                                ${this.twoLine ? html`<span class="mdc-list-item__secondary-text"><or-translate value="${item.secondaryText}"></or-translate></span>` : ``}
                            </span>
                        `}
                        ${rightTemplate}
                    </li>
                `;
            })}
        `;
    }

    protected isValueSelected(value: string): boolean {

        if (Array.isArray(this.values) && this.values.length > 0) {
            if (this.multiSelect) {
                return !!this.values.find((v) => v === value);
            }
            return this.values[0] === value;
        }
        return this.values === value;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        if (this._mdcElem) {
            this._mdcComponent = new MDCMenu(this._mdcElem);
            this._mdcComponent!.quickOpen = true;
            const elem = this.anchorElem || this._wrapperElem;
            // This doesn't work
            //this._mdcComponent.setAnchorElement(elem);
        }
    }

    protected updated(_changedProperties: PropertyValues): void {
        if (_changedProperties.has("visible")) {
            this._mdcComponent!.open = this.visible || false;
        }
    }

    protected _onMenuClosed() {
        this.dispatchEvent(new OrMwcMenuClosedEvent());
    }

    private _itemClicked(e: MouseEvent, item: MenuItem) {
        e.stopPropagation();
        const value = item.value;

        if (!this.multiSelect) {
            this.values = value;
            this._mdcComponent!.open = false;
        } else {
            if (!Array.isArray(this.values)) {
                this.values = this.values ? [this.values] : [];
            }
            const index = this.values.findIndex((v) => v === value);
            if (index >= 0) {
                this.values.splice(index, 1);
            } else {
                this.values.push(value);
            }
            this.requestUpdate();
        }
        this.dispatchEvent(new OrMwcMenuChangedEvent(this.values));
    }
}
