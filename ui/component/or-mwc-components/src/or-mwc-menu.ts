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

import {MDCMenu} from "@material/menu";

const listStyle = require("!!raw-loader!@material/list/dist/mdc.list.css");
const menuSurfaceStyle = require("!!raw-loader!@material/menu-surface/dist/mdc.menu-surface.css");
const menuStyle = require("!!raw-loader!@material/menu/dist/mdc.menu.css");

export interface MenuGroup {
    icon?: string;
    items: (MenuItem | null)[];
}

export interface MenuItem {
    content: TemplateResult,
    value: any;
}

export class OrMwcMenuChangedEvent extends CustomEvent<string> {

    public static readonly NAME = "or-mwc-menu-changed";

    constructor(value: string) {
        super(OrMwcMenuChangedEvent.NAME, {
            detail: value,
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMwcMenuChangedEvent.NAME]: OrMwcMenuChangedEvent;
    }
}

export function getContentWithMenuTemplate(content: TemplateResult, menuItems: (MenuItem | MenuGroup | null)[], selectedValue: string | undefined, valueChangedCallback: (v: string) => void): TemplateResult {

    const openMenu = (evt: Event) => {
        if (!menuItems) {
            return;
        }

        ((evt.currentTarget as Element).parentElement!.lastElementChild as OrMwcMenu).open();
    };

    return html`
        <span>
            <span @click="${openMenu}">${content}</span>
            ${menuItems ? html`<or-mwc-menu @or-mwc-menu-changed="${(evt: OrMwcMenuChangedEvent) => valueChangedCallback(evt.detail)}" .value="${selectedValue}" .menuItems="${menuItems}" id="menu"></or-mwc-menu>` : ``}
        </span>
    `;
}

// language=CSS
const style = css`
    :host {
        white-space: nowrap;
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
    public menuItems?: (MenuItem | MenuGroup | null)[];

    @property({type: String})
    public value?: string;

    @property({type: Boolean, attribute: true})
    public visible?: boolean;

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
                <div class="mdc-menu mdc-menu-surface" id="menu">
                    <ul class="mdc-list" role="menu" aria-hidden="true" aria-orientation="vertical" tabindex="-1">
                        ${this.getItemsTemplate(this.menuItems)}
                    </ul>
                </div>
            </div>
        `;
    }

    protected getItemsTemplate(items: (MenuItem | MenuGroup | null)[], icon?: string): TemplateResult {
        return html`
            ${items.map((item) => {
                if (item === null) {
                    return html`<li class="mdc-list-divider" role="separator"></li>`;
                }
                if ((item as MenuGroup).items) {
                    return html`
                        <li>
                            <ul class="mdc-menu__selection-group">
                                ${this.getItemsTemplate((item as MenuGroup).items, (item as MenuGroup).icon)}
                            </ul>
                        </li>
                    `;
                }
                if ((item as MenuItem).content) {
                    return html`
                        <li @click="${() => this._onSelect((item as MenuItem).value)}" class="mdc-list-item ${(item as MenuItem).value === this.value}" role="menuitem">
                            ${icon ? html`<or-icon icon="logout" class="mdc-list-item__graphic mdc-menu__selection-group-icon"></or-icon>` : ``}
                            ${(item as MenuItem).content}
                        </li>
                    `;
                }
            })}
        `;
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

    private _onSelect(value: string) {
        this.value = value;
        this.dispatchEvent(new OrMwcMenuChangedEvent(value));
    }
}
