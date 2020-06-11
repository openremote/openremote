import {css, customElement, html, LitElement, property, query, TemplateResult, unsafeCSS} from "lit-element";
import {until} from "lit-html/directives/until";
import {ifDefined} from "lit-html/directives/if-defined.js";
import manager, {
    DefaultBoxShadowBottom,
    DefaultColor1,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultHeaderHeight
} from "@openremote/core";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import "@openremote/or-icon";
import {getContentWithMenuTemplate, MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {Tenant} from "@openremote/model";

export interface HeaderConfig {
    mainMenu: HeaderItem[];
    secondaryMenu?: HeaderItem[];
}

export interface HeaderItem {
   icon: string;
   text: string;
   value?: string;
   href?: string;
   action?: () => void;
   hideMobile?: boolean;
   roles?: string[];
}

export interface Languages {
    [langKey: string]: string;
}

export const DEFAULT_LANGUAGES: Languages = {
    en: "english",
    nl: "dutch",
    fr: "french",
    de: "german",
    es: "spanish"
};

function getHeaderMenuItems(items: HeaderItem[]): MenuItem[] {
    return items.filter((option) => !option.roles || option.roles.some((r) => manager.hasRole(r))).map((option) => {
        return {
            text: option.text,
            value: option.value ? option.value : "",
            icon: option.icon,
            href: option.href
        };
    });
}

@customElement("or-header")
class OrHeader extends LitElement {

    // language=CSS
    static get styles() {
        return css`
        
            :host {
                --internal-or-header-color: var(--or-header-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));    
                --internal-or-header-selected-color: var(--or-header-selected-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
                --internal-or-header-text-color: var(--or-header-text-color, var(--or-app-color3, inherit));
                --internal-or-header-height: var(--or-header-height, ${unsafeCSS(DefaultHeaderHeight)});
                --internal-or-header-logo-margin: var(--or-header-logo-margin, 0 40px 0 0);
                --internal-or-header-logo-height: var(--or-header-logo-height, var(--internal-or-header-height, ${unsafeCSS(DefaultHeaderHeight)}));
                --internal-or-header-item-size: var(--or-header-item-size, calc(${unsafeCSS(DefaultHeaderHeight)} - 20px));
                --internal-or-header-drawer-color: var(--or-header-drawer-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
                --internal-or-header-drawer-text-color: var(--or-header-drawer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
                --internal-or-header-drawer-item-size: var(--or-header-drawer-item-size, 30px);
                --internal-or-header-drawer-separator-color: var(--or-header-drawer-separator-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
                
                display: block;
            }
              
            #toolbar-top {
                display: flex;
                padding: 0;
            }
            
            #logo-mobile {
                margin: 8px;
                height: calc(var(--internal-or-header-logo-height) - 16px);
                display: block;
            }
    
            #logo {
                display: none;
            }
                                            
            #header {
                opacity: 1;
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: var(--internal-or-header-height);
                text-align: center;
                background-color: var(--internal-or-header-color);
                color: var(--internal-or-header-text-color);
                --or-icon-fill: var(--internal-or-header-text-color);
                --or-icon-height: calc(var(--internal-or-header-item-size) - 12px);
                --or-icon-width: calc(var(--internal-or-header-item-size) - 12px);
                z-index: 9999999;
            }
    
            .shadow {
                -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
                -moz-box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
                box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
            }
                    
            #drawer {
                z-index: 999999;
                position: absolute;
                top: var(--internal-or-header-height);
                width: 100%;
                height: calc(100% - var(--internal-or-header-height));
                transition: all 300ms ease-in;
                transition-property: -webkit-transform;
                transition-property: transform;
                -webkit-transform: translate3d(0, -100%, 0);
                transform: translate3d(0, -100%, 0);
                background: var(--internal-or-header-drawer-color);
                color: var(--internal-or-header-drawer-text-color);
                --or-icon-fill: var(--internal-or-header-drawer-text-color);
                --or-icon-height: calc(var(--internal-or-header-drawer-item-size) - 10px);
                --or-icon-width: calc(var(--internal-or-header-drawer-item-size) - 10px);
            }
            
            #drawer[opened] {
                -webkit-transform: translate3d(0, 0, 0);
                transform: translate3d(0, 0, 0);
            }
                            
            #drawer > div {
                box-sizing: border-box;
                width: 100%;
                height: 100%;
                padding: 6px 24px;            
                position: relative;
            }
              
            .menu-btn {
                background: none;
                border: none;
                cursor: pointer;
                padding: 0 6px;
                height: 100%;
            }
            
            #menu-btn-mobile {
                margin-left: auto;
            }
    
            #menu-btn-desktop {
                    display: none;
            }
            
            #desktop-right {
                margin-left: auto;
                padding-right: 10px;
                display: none;
            }
    
            #mobile-bottom {
                border-top: 1px solid var(--internal-or-header-drawer-separator-color);
                margin-top: 20px;
                padding-top: 10px;
            }
          
            .menu-item {
                opacity: 0.7;
                cursor: pointer;
                text-decoration: none !important;         
                color: inherit;       
                padding: 0 20px;
                font-size: 14px;       
            }        
            
            .menu-item:hover,
            .menu-item[selected] {
                opacity: 1;
            }                
      
            #desktop-left .menu-item  {
                display: none;
                line-height: calc(var(--internal-or-header-height) - 4px);
            }
            
            #desktop-right .menu-item  {
                line-height: var(--internal-or-header-height);
            }
            
            #drawer .menu-item  {
                display: block;
                line-height: var(--internal-or-header-drawer-item-size);
                margin: 10px 0;
            }
            
            #desktop-left .menu-item[selected] {
                display: inline-block;
                line-height: var(--internal-or-header-height);
            }
    
            or-mwc-menu {
                margin-right: 10px;
                display: block;
            }
            
            .or-language-container {
                display: flex;
                height: 50px;
                align-items: center;
            }
          
            #realm-picker {
                position: relative;
                display: flex;
                height: 50px;
                align-items: center;
            }
          
            /* Wide layout: when the viewport width is bigger than 780px, layout
            changes to a wide layout. */
           
    
            @media (min-width: 780px) {
                #menu-btn-desktop {
                    display: block;
                }          
    
                #menu-btn-mobile {
                    display: none;
                }
    
                #drawer {
                    display: none;
                }
                
                #desktop-right {
                    display: flex;
                }
                
                #desktop-left .menu-item {
                    display: inline-block;
                }
                
                #desktop-left .menu-item or-icon{
                    display: none;
                }
    
                #desktop-left .menu-item[selected] {                
                    border-bottom: 4px solid var(--internal-or-header-selected-color);
                    line-height: calc(var(--internal-or-header-height) - 4px);
                }
            }
            
            @media (min-width: 1024px) {
                #logo {
                    margin: var(--internal-or-header-logo-margin);
                    height: var(--internal-or-header-logo-height);
                    display: block;
                }
    
                #logo-mobile {
                    display: none;
                }
    
                #desktop-left .menu-item or-icon{
                    display: inline-block;
                }
            }
    `;
    }

    @property({type: String})
    public logo?: string;

    @property({ type: String })
    public logoMobile?: string;

    @property({ type: Object })
    public config?: HeaderConfig;

    @query("div[id=mobile-bottom]")
    protected _mobileBottomDiv!: HTMLDivElement;

    protected _tenants?: Tenant[];

    @property({type: Boolean})
    private _drawerOpened = false;

    @property({ type: String })
    private activeMenu: string | undefined = window.location.hash ? window.location.hash : "#!map";

    public connectedCallback(): void {
        super.connectedCallback();
        window.addEventListener("hashchange", this._hashCallback, false);
    }

    public disconnectedCallback(): void {
        super.disconnectedCallback();
        window.removeEventListener("hashchange", this._hashCallback);
    }

    public _onHashChanged(e: Event) {
        const menu = window.location.hash.split("/")[0];
        this.activeMenu = menu;
    }

    public _onRealmSelect(realm: string) {
        manager.displayRealm = realm;
        this.requestUpdate();
    }
    protected _hashCallback = (e: Event) => {
        this._onHashChanged(e);
    }

    protected render() {

        const mainItems = this.config ? this.config.mainMenu : undefined;
        const secondaryItems = this.config ? this.config.secondaryMenu : undefined;

        return html`
           <!-- Header -->
            <div id="header" class="shadow">
                <div id="toolbar-top">
                    <div><img id="logo" src="${this.logo}" /><img id="logo-mobile" src="${this.logoMobile}" /></div>

                    <!-- This gets hidden on a small screen-->
                    <nav id="toolbar-list">
                        <div id="desktop-left">
                            ${mainItems ? mainItems.filter((option) => !option.roles || option.roles.some((r) => manager.hasRole(r))).map((headerItem) => {
                                return html`
                                    <a class="menu-item" href="${headerItem.href}" @click="${(e: MouseEvent) => this._onHeaderItemSelect(headerItem)}" ?selected="${this.activeMenu === headerItem.href}"><or-icon icon="${headerItem.icon}"></or-icon><or-translate value="${headerItem.text}"></or-translate></a>
                                `;
                            }) : ``}
                        </div>
                    </nav>
                    <div id="desktop-right">
                        ${this._getRealmMenu((value: string) => this._onRealmSelect(value))}
                        ${secondaryItems ? getContentWithMenuTemplate(html`
                            <button id="menu-btn-desktop" class="menu-btn" title="Menu"><or-icon icon="dots-vertical"></or-icon></button>
                        `,
                        getHeaderMenuItems(secondaryItems),
                        undefined,
                        (values: string | string[]) => this._onSecondaryMenuSelect(values as string)) : ``}
                    </div>
                    <div id="menu-btn-mobile">
                        <button id="menu-btn" class="menu-btn" title="Menu" @click="${this._toggleDrawer}"><or-icon icon="${this._drawerOpened ? "close" : "menu"}"></or-icon></button>
                    </div>
                </div>
            </div>
            <div id="drawer" ?opened="${this._drawerOpened}" @click="${this._closeDrawer}">
                <div>                    
                    <div id="mobile-top">
                        <nav id="drawer-list">
                            ${mainItems ? mainItems.filter((option) => !option.hideMobile && (!option.roles || option.roles.some((r) => manager.hasRole(r)))).map((headerItem) => {
                                return html`
                                    <a class="menu-item" href="${headerItem.href}" @click="${(e: MouseEvent) => this._onHeaderItemSelect(headerItem)}" ?selected="${this.activeMenu === headerItem.href}"><or-icon icon="${headerItem.icon}"></or-icon><or-translate value="${headerItem.text}"></or-translate></a>
                                `;
                            }) : ``}
                        </nav>
                    </div>
                    
                    ${secondaryItems ? html`
                        <div id="mobile-bottom">
                                ${secondaryItems.filter((option) => !option.hideMobile && (!option.roles || option.roles.some((r) => manager.hasRole(r)))).map((headerItem) => {
                                    return html`
                                        <a class="menu-item" href="${ifDefined(headerItem.href)}" @click="${(e: MouseEvent) => this._onHeaderItemSelect(headerItem)}" ?selected="${this.activeMenu === headerItem.href}"><or-icon icon="${headerItem.icon}"></or-icon><or-translate value="${headerItem.text}"></or-translate></a>
                                    `;
                                })}
                        </div>` : ``}
                </div>
            </div>
        `;
    }

    protected _getRealmMenu(callback: (language: string) => void): TemplateResult {
        if (!manager.isSuperUser()) {
            return html``;
        }

        const picker = this._getTenants().then((tenants) => {

            const menuItems = tenants.map((r) => {
                return {
                    text: r.displayName!,
                    value: r.realm!
                } as MenuItem;
            });

            return html`
            ${getContentWithMenuTemplate(
                html`
                    <div id="realm-picker">
                        <span style="margin-left: 10px;">${manager.displayRealm}</span>
                        <or-icon icon="chevron-down"></or-icon>
                    </div>
                `,
                menuItems,
                manager.displayRealm,
                (values: string | string[]) => callback(values as string))}
        `;
        });

        return html`${until(picker, html``)}`;
    }

    protected async _getTenants() {
        if (!this._tenants) {
            const response = await manager.rest.api.TenantResource.getAll();
            this._tenants = response.data;
        }

        return this._tenants;
    }

    protected _onSecondaryMenuSelect(value: string) {
        const headerItem = this.config!.secondaryMenu!.find((item) => item.value === value);
        if (headerItem) {
            this._onHeaderItemSelect(headerItem);
        }
    }

    protected _onHeaderItemSelect(headerItem: HeaderItem) {
        if (headerItem.action) {
            headerItem.action();
        } else if (headerItem.href) {
            window.location.href = headerItem.href;
        }
    }

    protected _closeDrawer() {
        this._drawerOpened = false;
    }

    protected _toggleDrawer() {
        this._drawerOpened = !this._drawerOpened;
    }
}
