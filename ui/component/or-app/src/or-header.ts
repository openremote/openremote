import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import manager, {
    DefaultBoxShadowBottom,
    DefaultColor1,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    DefaultHeaderHeight,
    Util,
  DEFAULT_LANGUAGES,
  Languages
} from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-icon";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {Alarm, AlarmEvent, AlarmStatus, PersistenceEvent, Realm} from "@openremote/model";
import {AppStateKeyed, router, updateRealm} from "./index";
import {AnyAction, Store} from "@reduxjs/toolkit";
import * as Model from "@openremote/model";


export {DEFAULT_LANGUAGES, Languages}

export interface HeaderConfig {
    mainMenu: HeaderItem[];
    secondaryMenu?: HeaderItem[];
}

export interface HeaderItem {
   icon: string;
   text: string;
   value?: string;
   href?: string;
   absolute?: boolean;
   action?: () => void;
   hideMobile?: boolean;
   roles?: string[] | {[client: string]: string[]} | (() => boolean);
}

function getHeaderMenuItems(items: HeaderItem[]): ListItem[] {
    return items.filter(hasRequiredRole).map((option) => {
        return {
            text: option.text,
            value: option.value ? option.value : "",
            icon: option.icon,
            href: option.href
        };
    });
}

function hasRequiredRole(option: HeaderItem): boolean {
    if (!option.roles) {
        return true;
    }

    if (Array.isArray(option.roles)) {
        return option.roles.some((r) => manager.hasRole(r));
    }

    if (Util.isFunction(option.roles)) {
        return (option.roles as () => boolean)();
    }

    return Object.entries(option.roles).some(([client, roles]) => roles.some((r: string) => manager.hasRole(r, client)));
}


function getCurrentMenuItemRef(defaultRef?: string): string | undefined {
    const menu = window.location.hash.substr(2).split("/")[0];
	return menu || defaultRef;
}

@customElement("or-header")
export class OrHeader extends LitElement {

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
                --internal-or-header-drawer-separator-color: var(--or-header-drawer-separator-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
                
                display: block;
                z-index: 4;
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
                width: 100%;
                position: absolute;
                top: var(--internal-or-header-height);
                max-height: 0;
                height: calc(100% - var(--internal-or-header-height));
                transition: max-height 0.25s ease-out;
                background: var(--internal-or-header-drawer-color);
                color: var(--internal-or-header-drawer-text-color);
                --or-icon-fill: var(--internal-or-header-drawer-text-color);
                --or-icon-height: calc(var(--internal-or-header-drawer-item-size) - 10px);
                --or-icon-width: calc(var(--internal-or-header-drawer-item-size) - 10px);
                overflow: auto;
            }
            
            #drawer[opened] {
                max-height: 10000px;
                transition: max-height 0.75s ease-in;
            }
                            
            #drawer > div {
                box-sizing: border-box;
                width: 100%;
                height: 100%;
                padding: 10px 0px;
                position: relative;
            }
              
            .menu-btn {
                background: none;
                border: none;
                cursor: pointer;
                padding: 0 16px;
                height: 100%;
            }
            
            #menu-btn-mobile {
                display: flex;
                margin-left: auto;
                --or-icon-height: calc(var(--internal-or-header-item-size) - 8px);
                --or-icon-width: calc(var(--internal-or-header-item-size) - 8px);
            }

            #menu-btn-mobile #realm-picker > span{
                max-width: 70px;
                text-overflow: ellipsis;
                overflow: hidden;
                white-space: nowrap;
            }
    
            #menu-btn-desktop {
                display: none;
            }
            
            #desktop-right {
                margin-left: auto;
                padding-right: 10px;
                display: none;
            }
    
            .mobile-bottom-border {
                border-top: 1px solid var(--internal-or-header-drawer-separator-color);
                margin-top: 16px;
                padding-top: 8px;
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
            #desktop-left .menu-item or-icon {
                margin-right: 10px;
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
                margin: 6px 0;
                padding: 8px 16px;
            }
            
            #drawer .menu-item  or-icon {
                margin: 0 10px;
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
                cursor: pointer;
                margin-left: 10px;
            }
            
            #realm-picker > span {
                margin-right: 2px;
            }
          
            /* Wide layout: when the viewport width is bigger than 768px, layout
            changes to a wide layout. */
            @media (min-width: 768px) {
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
                
                #desktop-left .menu-item or-icon {
                    display: none;
                }
    
                #desktop-left .menu-item[selected] {                
                    border-bottom: 4px solid var(--internal-or-header-selected-color);
                    line-height: calc(var(--internal-or-header-height) - 4px);
                }

                #logo {
                    margin: var(--internal-or-header-logo-margin);
                    height: var(--internal-or-header-logo-height);
                    display: block;
                }
    
                #logo-mobile {
                    display: none;
                }
                
                #desktop-left ::slotted(*) {
                    display: inline-block;
                }
    
                #desktop-left ::slotted(*[selected]) {                
                    border-bottom: 4px solid var(--internal-or-header-selected-color);
                    line-height: calc(var(--internal-or-header-height) - 4px);
                }
            }
            
            @media (min-width: 1024px) {
               
    
                #desktop-left .menu-item or-icon{
                    display: inline-block;
                }
            }
    `;
    }

    @property({type: Array})
    public realms!: Realm[];

    @property({type: String})
    public realm!: string;

    @property({type: Object})
    public store!: Store<AppStateKeyed, AnyAction>;

    @property({type: String})
    public logo?: string;

    @property({ type: String })
    public logoMobile?: string;

    @property({ type: Object })
    public config?: HeaderConfig;

    @query("div[id=mobile-bottom]")
    protected _mobileBottomDiv!: HTMLDivElement;

    @property()
    public activeMenu: string | undefined;

    @state()
    private _drawerOpened = false;

    @state()
    private alarmButton = 'bell-outline';

    @state()
    private alarmColor = '--or-app-color3, ${unsafeCSS(DefaultColor3)}';

    private _eventSubscriptionId?: string;

    public _onRealmSelect(realm: string) {
        this._getAlarmButton().then();
        this.store.dispatch(updateRealm(realm));
    }

    protected shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("config")) {
            this.activeMenu = getCurrentMenuItemRef(this.config && this.config.mainMenu && this.config.mainMenu.length > 0 ? this.config.mainMenu[0].href : undefined);
        }
        return super.shouldUpdate(changedProperties);
    }

    connectedCallback() {
        super.connectedCallback();
        this._subscribeEvents().then();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._unsubscribeEvents();
    }

    protected async _subscribeEvents() {
        if (manager.events) {
            this._eventSubscriptionId = await manager.events.subscribe<AlarmEvent>({
                eventType: "alarm"
            }, (ev) => this._getAlarmButton());
        }
    }

    protected _unsubscribeEvents() {
        if (this._eventSubscriptionId) {
            manager.events!.unsubscribe(this._eventSubscriptionId);
            this._eventSubscriptionId = undefined;
        }
    }

    protected render() {

        if (!this.config) {
            return html``;
        }

        const mainItems = this.config.mainMenu;
        const secondaryItems = this.config.secondaryMenu;

        return html`
           <!-- Header -->
            <div id="header" class="shadow">
                <div id="toolbar-top">
                    <div><img id="logo" src="${this.logo}" /><img id="logo-mobile" src="${this.logoMobile}" /></div>

                    <!-- This gets hidden on a small screen-->
                    <nav id="toolbar-list">
                        <div id="desktop-left">
                            ${mainItems ? mainItems.filter(hasRequiredRole).map((headerItem) => {
                                return html`
                                    <a class="menu-item" @click="${(e: MouseEvent) => this._onHeaderItemSelect(headerItem)}" ?selected="${this.activeMenu === headerItem.href}"><or-icon icon="${headerItem.icon}"></or-icon><or-translate value="${headerItem.text}"></or-translate></a>
                                `;
                            }) : ``}
                        </div>
                    </nav>
                    <div id="desktop-right">
                        <div id="alarm-btn">
                            <a class="menu-item" @click="${(e: MouseEvent) => router.navigate('alarms')}">
                                <or-icon icon="${this.alarmButton}" style="color:var(${this.alarmColor})"></or-icon>
                            </a>
                        </div>
                        ${this._getRealmMenu((value: string) => this._onRealmSelect(value))}
                        ${secondaryItems ? getContentWithMenuTemplate(html`
                            <button id="menu-btn-desktop" class="menu-btn" title="Menu"><or-icon icon="dots-vertical"></or-icon></button>
                        `,
                        getHeaderMenuItems(secondaryItems),
                        undefined,
                        (value) => this._onSecondaryMenuSelect(value as string)) : ``}
                    </div>
                    <div id="menu-btn-mobile">
                        ${this._getRealmMenu((value: string) => this._onRealmSelect(value))}
                        <button id="menu-btn" class="menu-btn" title="Menu" @click="${this._toggleDrawer}"><or-icon icon="${this._drawerOpened ? "close" : "menu"}"></or-icon></button>
                    </div>
                </div>
            </div>
            <div id="drawer" ?opened="${this._drawerOpened}" @click="${this._closeDrawer}">
                <div>                    
                    <div id="mobile-top">
                        <nav id="drawer-list">
                            ${mainItems ? mainItems.filter((option) => !option.hideMobile && hasRequiredRole(option)).map((headerItem) => {
                                return html`
                                    <a class="menu-item" @click="${(e: MouseEvent) => this._onHeaderItemSelect(headerItem)}" ?selected="${this.activeMenu === headerItem.href}"><or-icon icon="${headerItem.icon}"></or-icon><or-translate value="${headerItem.text}"></or-translate></a>
                                `;
                            }) : ``}
                        </nav>
                    </div>
                    
                    ${secondaryItems ? html`
                        <div id="mobile-bottom" class="${mainItems.length > 0 ? 'mobile-bottom-border' : ''}">
                            ${secondaryItems.filter((option) => !option.hideMobile && hasRequiredRole(option)).map((headerItem) => {
                                return html`
                                    <a class="menu-item" @click="${(e: MouseEvent) => this._onHeaderItemSelect(headerItem)}" ?selected="${this.activeMenu === headerItem.href}"><or-icon icon="${headerItem.icon}"></or-icon><or-translate value="${headerItem.text}"></or-translate></a>
                                `;
                            })}
                        </div>` : ``}
                </div>
            </div>
        `;
    }

    protected _getRealmMenu(callback: (realm: string) => void): TemplateResult {

        const currentRealm = this.realms.find((t) => t.name === this.realm);

        let realmTemplate = html`
            <div id="realm-picker">
                ${this.realms.length > 1 ? html`
                    <span>${currentRealm ? currentRealm.displayName : ""}</span>
                    <or-icon icon="chevron-down"></or-icon>
                ` : ``}
            </div>
        `;

        if (manager.isSuperUser()) {
            const menuItems = this.realms.map((r) => {
                return {
                    text: r.displayName!,
                    value: r.name!
                } as ListItem;
            });

            realmTemplate = html`
                ${getContentWithMenuTemplate(
                        realmTemplate,
                        menuItems,
                        currentRealm ? currentRealm.name : undefined,
                        (value) => callback(value as string))}
            `;
        }

        return realmTemplate;
    }

    protected async _getAlarmButton() {
        let newAlarms= false;
        if(manager.isRestrictedUser()){
            // TODO Filter alarms by linked assets
        }
        if(manager.hasRole("read:alarms") || manager.hasRole("read:admin")){
            const response = await manager.rest.api.AlarmResource.getOpenAlarms();
            let open = response.data.filter((alarm) => alarm.realm === manager.displayRealm);
            newAlarms = (open.length > 0);
        }
        this.alarmButton = newAlarms ? 'bell-badge-outline' : 'bell-outline';
        this.alarmColor = newAlarms ? '--or-app-color4, ${unsafeCSS(DefaultColor4)}' : '--or-app-color3, ${unsafeCSS(DefaultColor3)}';
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
            if (headerItem.absolute) {
                window.location.href = headerItem.href;
            } else {
                router.navigate(headerItem.href);
            }
        }
    }

    protected _closeDrawer() {
        this._drawerOpened = false;
    }

    protected _toggleDrawer() {
        this._drawerOpened = !this._drawerOpened;
    }
}
