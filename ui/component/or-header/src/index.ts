import {style} from "./style";
import {customElement, html, LitElement, property, query} from "lit-element";
import {MDCDialog} from "@material/dialog";
import i18next from "i18next";
import manager from "@openremote/core";
import {MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import "@openremote/or-icon";
import "./or-language";
import "./or-language-modal";
import "./or-realm-picker";

interface menuOption {
   icon: string;
   text: string;
   value?: string;
   href?: string;
   hideMobile?: boolean;
}

const mainItems: menuOption[] = [
    {
        icon: "map",
        href: "#!map",
        text: "map"
    },
    {
        icon: "rhombus-split",
        href: "#!assets",
        text: "asset_plural",
    },
    {
        icon: "state-machine",
        href: "#!rules",
        text: "rule_plural",
        hideMobile: true
    },
    {
        icon: "chart-areaspline",
        href: "#!insights",
        text: "insights",
        hideMobile: true
    },
    // {
    //     icon: "android-messages",
    //     href: "#!messages",
    //     text: "messages",
    // }
];

const secondaryItems: menuOption[] = [
    // {
    //     icon: "cloud",
    //     value: "cloud",
    //     text: "Cloud connection",
    //     isSuperUser: true
    // },
    {
        icon: "web",
        value: "language",
        text: "lang"
    },
    {
        icon: "file-document-box-search-outline",
        value: "logs",
        href: "#!logs",
        text: "logs"
    },
    // {
    //     icon: "account-cog",
    //     value: "User management",
    //     text: "User management",
    //     isSuperUser: true
    // },
    // {
    //     icon: "tune",
    //     value: "Settings",
    //     text: "Settings"
    // },
    {
        icon: "logout",
        value: "logout",
        text: "logout"
    }
];

function getHeaderMenu(items: menuOption[]): MenuItem[] {
    return items.map(option => {
        return {
            text: i18next.t(option.text),
            value: option.value ? option.value : "",
            icon: option.icon,
            href: option.href
        };
    });
}

@customElement("or-header")
class OrHeader extends LitElement {

    @property({type: Boolean})
    private _drawerOpened = false;

    @property({type: String})
    private logo = "";

    @property({ type: String })
    private logoMobile = "";

    @property({ type: Array })
    private mainItems: menuOption[] = mainItems;

    @property({ type: Array })
    private secondaryItems: menuOption[] = secondaryItems;

    @property({ type: String })
    private activeMenu: string | undefined = window.location.hash ? window.location.hash : "#!map";

    @query("slot[name=mobile-bottom]")
    protected _mobileBottomSlot?: HTMLSlotElement;

    @query("div[id=mobile-bottom]")
    protected _mobileBottomDiv?: HTMLDivElement;


    static get styles() {
        return [
            style
        ];
    }

    protected render() {
        return html`
           <!-- Header -->
            <div id="header" class="shadow">
                <div id="toolbar-top">
                    <div><img id="logo" src="${this.logo}" /><img id="logo-mobile" src="${this.logoMobile}" /></div>
                    <!-- This gets hidden on a small screen-->
                    
                    <nav id="toolbar-list">
                        <div id="desktop-left">
                            ${this.mainItems.map(menuItem => {
                                return html`
                                    <a class="menu-item" href="${menuItem.href}" ?selected="${this.activeMenu === menuItem.href}" data-navigo @click="${() => this.activeMenu = menuItem.href}"><or-icon icon="${menuItem.icon}"></or-icon><or-translate value="${menuItem.text}"></or-translate></a>
                                `
                            })}
                        </div>
                    </nav>
                    <div id="desktop-right">
                        ${(manager.isSuperUser() ? html`<a slot="desktop-right"><or-realm-picker></or-realm-picker></a>` : ``)}
                        <slot name="desktop-right"></slot>
                    </div>
                    ${getContentWithMenuTemplate(
                            html`
                                <button id="desktop-menu-btn" class="menu-btn" title="Menu"><or-icon icon="dots-vertical"></or-icon></button>
                            `,
                            getHeaderMenu(this.secondaryItems),
                            this.activeMenu,
                            (values: string | string[]) => this.setActiveMenu(values as string))}
                            <div id="menu-btn-mobile" >
                                <button id="menu-btn" class="menu-btn" title="Menu" @click="${this._menuButtonClicked}">${this._drawerOpened ? html`<or-icon icon="close"></or-icon>` : html`<or-icon icon="menu"></or-icon>`}</button>
                            </div>
                </div>
            </div>
            <or-language-modal id="language-modal"></or-language-modal>
            <div id="drawer" ?opened="${this._drawerOpened}" @click="${this._close}">
                <div>                    
                    <div id="mobile-top">
                        <nav id="drawer-list">
                            ${this.mainItems.map(menuItem => {
                                if (menuItem.hideMobile) return html``;
                                return html`
                                    <a class="menu-item" href="${menuItem.href}" ?selected="${this.activeMenu === menuItem.href}" data-navigo @click="${() => this.activeMenu = menuItem.href}"><or-icon icon="${menuItem.icon}"></or-icon><or-translate value="${menuItem.text}"></or-translate></a>
                                `
                            })}
                        </nav>
                    </div>
                    
                    <div id="mobile-bottom">
                            ${this.secondaryItems.map(menuItem => {
                                if (menuItem.hideMobile) return html``;
                                if(menuItem.href) 
                                    return html`
                                        <a class="menu-item" href="${menuItem.href}" ?selected="${this.activeMenu === menuItem.href}" data-navigo @click="${() => this.activeMenu = menuItem.href}"><or-icon icon="${menuItem.icon}"></or-icon><or-translate value="${menuItem.text}"></or-translate></a>
                                    `
                                else
                                    return html`
                                        <a class="menu-item" @click=${() => this.setActiveMenu(menuItem.value as string)} ><or-icon icon="${menuItem.icon}"></or-icon><or-translate value="${menuItem.text}"></or-translate></a>
                                    `
                            })}
                    </div>
                </div>
            </div>

       
            
        `;
    }

    protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
        if (this._mobileBottomSlot && this._mobileBottomDiv) {
            const childNodes = this._mobileBottomSlot.assignedNodes();
            if (childNodes.length > 0) {
                this._mobileBottomDiv.classList.add("has-children");
            }
        }
    }

    private _close() {
        this._drawerOpened = false;
    }

    private _menuButtonClicked() {
        this._drawerOpened = !this._drawerOpened;
    }
    
    setActiveMenu(value: string) {
        switch (value) {
            case "logout":
                    manager.logout();
                break;
            case "language":
                    const modal = this.shadowRoot!.getElementById("language-modal");
                    if (modal && modal.shadowRoot) {
                        const component = modal.shadowRoot.getElementById("mdc-dialog-language");
                        if (component) {
                            const dialog = new MDCDialog(component);
                            if (dialog) {
                                dialog.open();
                            }
                        }
                    }
                   
                break;
        }
        this.activeMenu = value;
    }

}
