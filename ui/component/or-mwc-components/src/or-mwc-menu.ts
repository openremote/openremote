import {
    css,
    html,
    LitElement,
    PropertyValues,
    TemplateResult,
    unsafeCSS
} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {classMap} from 'lit/directives/class-map.js';
import {MDCMenu} from "@material/menu";
import {DefaultColor4, DefaultColor8} from "@openremote/core";

// @ts-ignore
import listStyle from "@material/list/dist/mdc.list.css";
// @ts-ignore
import menuSurfaceStyle from "@material/menu-surface/dist/mdc.menu-surface.css";
import {getItemTemplate, getListTemplate, ListItem, ListType, MDCListActionEvent} from "./or-mwc-list";
import { ref } from 'lit/directives/ref.js';
// @ts-ignore
const menuStyle = require("@material/menu/dist/mdc.menu.css");

export class OrMwcMenuChangedEvent extends CustomEvent<any[] | any> {

    public static readonly NAME = "or-mwc-menu-changed";

    constructor(values: any[] | any) {
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

export function positionMenuAtElement<T extends OrMwcMenu>(
    menu: T,
    hostElement?: HTMLElement
): T {
    if (!hostElement) {
        hostElement = document.body;
    }
    const rect = hostElement.getBoundingClientRect();

    // Applying a style that is calculated from the runtime coordinates of
    // the host element.
    Object.assign(menu.style, {
        position: 'fixed',
        top: `${rect.bottom}px`,
        left: `${rect.left}px`,
        zIndex: '1000',
        display: 'block'
    });

    return menu;
}

export function getContentWithMenuTemplate(content: TemplateResult, menuItems: (ListItem | ListItem[] | null)[], selectedValues: string[] | string | undefined, valueChangedCallback: (values: string[] | string) => void, closedCallback?: () => void, multiSelect = false, translateValues = true, midHeight = false, fullWidth = false, menuId = "menu", fixedToHost = false): TemplateResult {
    let menuRef: OrMwcMenu | null = null;   // Reference to the menu

    const openMenu = (evt: Event) => {
        if (!menuItems) {
            return;
        }

        if (fixedToHost && menuRef) {
            const hostElement = evt.currentTarget as HTMLElement;

            // Using run time coordinates to assign a fixed position to the menu
            positionMenuAtElement(
                menuRef,
                hostElement
            );
        }
        ((evt.currentTarget as Element).parentElement!.lastElementChild as OrMwcMenu).open();
    };

    return html`
        <span>
            <span @click="${openMenu}">${content}</span>
            ${menuItems ? html`<or-mwc-menu ?multiselect="${multiSelect}" @or-mwc-menu-closed="${() => {if (closedCallback) { closedCallback(); }} }" @or-mwc-menu-changed="${(evt: OrMwcMenuChangedEvent) => {if (valueChangedCallback) { valueChangedCallback(evt.detail); }} }" .translateValues="${translateValues}" .values="${selectedValues}" .menuItems="${menuItems}" .midHeight="${midHeight}" .fullWidth="${fullWidth}" id="${menuId}" ${ref(el => (menuRef = el as OrMwcMenu))}></or-mwc-menu>` : ``}
        </span>
    `;
}

// language=CSS
const style = css`
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

@customElement("or-mwc-menu")
export class OrMwcMenu extends LitElement {

    static get styles() {
        return [
            css`${unsafeCSS(listStyle)}`,
            css`${unsafeCSS(menuStyle)}`,
            css`${unsafeCSS(menuSurfaceStyle)}`,
            style
        ];
    }

    @property({type: Array})
    public menuItems?: (ListItem | ListItem[] | null)[];

    @property({type: Array})
    public values?: any[] | any;

    @property({type: Boolean, attribute: true})
    public multiSelect?: boolean;

    @property({type: Boolean, attribute: true})
    public visible?: boolean;

    @property({type: Boolean, attribute: true})
    public translateValues?: boolean;

    @property({type: Boolean, attribute: false})
    public midHeight?: boolean;

    @property({type: Boolean, attribute: false})
    public fullWidth?: boolean;

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

        const content = this.getItemsTemplate(this.menuItems, this.translateValues);
        const isTwoLine = this.menuItems && this.menuItems.some((item) => item && !Array.isArray(item) && !!item.secondaryText);

        const classes = {
            'mdc-menu-surface-mid-height': (this.midHeight ? 1 : 0),
            'mdc-menu-surface-full-width': (this.fullWidth ? 1 : 0)
        }
        return html`
            <div id="wrapper" class="mdc-menu-surface--anchor">
                <div class="mdc-menu mdc-menu-surface ${classMap(classes)}" id="menu" @MDCMenuSurface:closed="${this._onMenuClosed}">
                    ${getListTemplate(ListType.MULTI_TICK, content, isTwoLine, "menu")}
                </div>
            </div>
        `;
    }

    protected getItemsTemplate(items: (ListItem | ListItem[] | null)[], translate?: boolean): TemplateResult {

        const type = this.multiSelect ? ListType.MULTI_TICK : ListType.PLAIN;

        return html`
            ${items.map((item, index) => {

            if (Array.isArray(item)) {
                return html`
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

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        if (this._mdcElem) {
            this._mdcComponent = new MDCMenu(this._mdcElem);

            // This overrides the standard mdc menu body click capture handler as it doesn't work with webcomponents
            (this._mdcComponent as any).menuSurface_.foundation.handleBodyClick = function (evt: MouseEvent) {
                const el = evt.composedPath()[0]; // Use composed path not evt target to work with webcomponents
                if (this.adapter.isElementInContainer(el)) {
                    return;
                }
                this.close();
            };

            this._mdcComponent!.quickOpen = true;
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

    private _itemClicked(e: MouseEvent, item: ListItem) {
        e.stopPropagation();
        const value = item.value;

        if (!this.multiSelect) {
            this.values = value;
            this._mdcComponent!.open = false;
        } else {
            if (!Array.isArray(this.values)) {
                this.values = this.values ? [this.values] : [];
            }
            const index = this.values.findIndex((v: any) => v === value);
            if (index >= 0) {
                this.values.splice(index, 1);
            } else {
                this.values.push(value);
            }
            this.requestUpdate();
        }
        this.dispatchEvent(new OrMwcMenuChangedEvent(this.values!));
    }
}
