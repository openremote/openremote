import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {AppConfig, Page, RealmAppConfig, router} from "./types";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-menu";
import "@openremote/or-mwc-components/or-mwc-snackbar";
import "./or-header";
import "@openremote/or-icon";
import {updateMetadata} from "pwa-helpers/metadata";
import i18next from "i18next";
import manager, {BasicLoginResult, DefaultColor2, DefaultColor3, DefaultColor4, Manager, normaliseConfig, ORError, OREvent, Util} from "@openremote/core";
import {DEFAULT_LANGUAGES, HeaderConfig} from "./or-header";
import {OrMwcDialog, showDialog, showErrorDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrMwcSnackbar, showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {AnyAction, Store, Unsubscribe} from "@reduxjs/toolkit";
import {AppStateKeyed, setOffline, setVisibility, updatePage, updateRealm} from "./app";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {Auth, ManagerConfig, Realm} from "@openremote/model";
import {pageOfflineProvider} from "./page-offline";

export const DefaultLogo = require("../images/logo.svg");
export const DefaultMobileLogo = require("../images/logo-mobile.svg");
export const DefaultFavIcon = require("../images/favicon.ico");

export {AnyAction};
export * from "./app";
export * from "./or-header";
export * from "./types";

// Declare MANAGER_URL and KEYCLOAK_URL - Global var injected by webpack
declare var MANAGER_URL: string | undefined;
declare var KEYCLOAK_URL: string | undefined;

export {HeaderConfig, DEFAULT_LANGUAGES};

export function getRealmQueryParameter(): string | undefined {
    if(location.search && location.search !== "") {
        return Util.getQueryParameter(location.search, "realm");
    }

    if(location.hash) {
        const index = location.hash.indexOf("?");
        if(index > -1) {
            return Util.getQueryParameter(location.hash.substring(index + 1), "realm");
        }
    }
}

export function getDefaultManagerConfig() {
    return normaliseConfig(DEFAULT_MANAGER_CONFIG);
}

const DEFAULT_MANAGER_CONFIG: ManagerConfig = {
    managerUrl: MANAGER_URL,
    keycloakUrl: KEYCLOAK_URL,
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: undefined,
    consoleAutoEnable: true,
    loadTranslations: ["or"]
};

@customElement("or-app")
export class OrApp<S extends AppStateKeyed> extends LitElement {

    @property({type: Object})
    public appConfig?: AppConfig<S>;

    public appConfigProvider?: (manager: Manager) => AppConfig<S>;

    @property({type: Object})
    public managerConfig?: ManagerConfig;

    @query("main")
    protected _mainElem!: HTMLElement;

    @state()
    protected _initialised = false;

    @state()
    protected _page?: string;

    @state()
    protected _config!: RealmAppConfig;

    @state()
    protected _realm?: string;

    @state()
    protected _offline: boolean = false;

    @state()
    protected _showOfflineFallback: boolean = false;

    @state()
    protected _activeMenu?: string;

    protected _onEventBind?: any;
    protected _onVisibilityBind?: any;
    protected _realms!: Realm[];
    protected _offlineFallbackDeferred?: Util.Deferred<void>;
    protected _store: Store<S, AnyAction>;
    protected _storeUnsubscribe!: Unsubscribe;

    // language=CSS
    static get styles() {
        return css`
            :host {
                --or-app-color2: ${unsafeCSS(DefaultColor2)};
                --or-app-color3: ${unsafeCSS(DefaultColor3)};
                --or-app-color4: ${unsafeCSS(DefaultColor4)};
                --or-console-primary-color: #4D9D2A;
                color: ${unsafeCSS(DefaultColor3)};
                fill: ${unsafeCSS(DefaultColor3)};
                font-size: 14px;

                height: 100vh;
                display: flex;
                flex: 1;
                flex-direction: column;
            }
                
            .main-content {
                display: flex;
                flex: 1;
                box-sizing: border-box;
                background-color: var(--or-app-color2);
                overflow: auto;
            }
            
            .no-scroll {
                overflow: hidden;
            }

            main > * {
                display: flex;
                flex: 1;
                position: relative;
            }
    
            .desktop-hidden {
                display: none !important;
            }
            
            @media only screen and (max-width: 780px) {
                .desktop-hidden {
                    display: inline-block !important;
                }
            }
            
            /* HEADER STYLES */
            or-header a > or-icon {
                margin-right: 10px;
            }
        `;
    }

    constructor(store: Store<S, AnyAction>) {
        super();
        this._store = store;

        // Set this element as the host for dialogs
        OrMwcDialog.DialogHostElement = this;
        OrMwcSnackbar.DialogHostElement = this;
    }

    public getState(): S {
        return this._store.getState();
    }

    // Using HTML 'visibilitychange' listener to see whether the Manager is visible for the user.
    // TODO; Add an ConsoleProvider that listens to background/foreground changes, and dispatch the respective OREvent. This will improve responsiveness of logic attached to it.
    // For example used for triggering reconnecting logic once the UI becomes visible again.
    protected onVisibilityChange(ev: Event) {
        if(document.visibilityState === "visible") {
            this._onEvent(OREvent.CONSOLE_VISIBLE);
        } else {
            this._onEvent(OREvent.CONSOLE_HIDDEN);
        }
    }

    connectedCallback() {
        super.connectedCallback();
        this._storeUnsubscribe = this._store.subscribe(() => this.stateChanged(this.getState()));
        this._onVisibilityBind = this.onVisibilityChange.bind(this);
        document.addEventListener("visibilitychange", this._onVisibilityBind);
        this.stateChanged(this.getState());
    }

    disconnectedCallback() {
        this._storeUnsubscribe();
        if(this._onVisibilityBind) {
            document.removeEventListener("visibilityChange", this._onVisibilityBind);
        }
        if(this._onEventBind) {
            manager.removeListener(this._onEventBind);
        }
        super.disconnectedCallback();
    }

    protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
        super.firstUpdated(_changedProperties);

        const managerConfig: ManagerConfig = this.managerConfig ? {...DEFAULT_MANAGER_CONFIG,...this.managerConfig} : DEFAULT_MANAGER_CONFIG;
        if (!managerConfig.realm) {
            // Use realm query parameter if no specific realm provided
            managerConfig.realm = getRealmQueryParameter();
        }
        managerConfig.skipFallbackToBasicAuth = true; // We do this so we can load styling config before displaying basic login
        managerConfig.basicLoginProvider = (u:any, p:any) => this.doBasicLogin(u, p);

        console.info("Initialising the manager");

        manager.init(managerConfig).then(async (success) => {
            if (!success && manager.error === ORError.AUTH_FAILED && (!managerConfig.auth || managerConfig.auth === Auth.KEYCLOAK)) {
                this.doAppConfigInit();

                // Fallback to basic auth now styling is loaded
                managerConfig.auth = Auth.BASIC;
                success = await manager.init(managerConfig);
            }

            if (success) {
                this.doAppConfigInit();

                if (!this.appConfig) {
                    showErrorDialog("appError.noConfig", document.body);
                    console.error("No AppConfig supplied");
                    return;
                }

                if (!this._config) {
                    showErrorDialog("appError.noConfig", document.body);
                    console.error("No default AppConfig or realm specific config provided so cannot render");
                    return;
                }

                if (!this._store) {
                    showErrorDialog("appError.noReduxStore", document.body);
                    console.error("No Redux store supplied");
                    return;
                }

                if (!this.appConfig.pages || Object.keys(this.appConfig.pages).length === 0) {
                    showErrorDialog("appError.noPages", document.body);
                    console.error("No page providers");
                    return;
                }

                // Load available realm info
                const response = await manager.rest.api.RealmResource.getAccessible();
                this._realms = response.data;

                let realm: string | null | undefined = undefined;

                // Set current display realm if super user
                if (manager.isSuperUser()) {
                    // Look in session storage
                    realm = window.sessionStorage.getItem("realm");
                    if (realm && !this._realms.some(r => r.name === realm)) {
                        realm = undefined;
                    }
                }

                this._store.dispatch(updateRealm(realm || manager.getRealm() || "master"));

                this._initialised = true;

                // Register listener to change global state based on certain events
                this._onEventBind = this._onEvent.bind(this);
                manager.addListener(this._onEventBind);

                // Create route listener to set header active item (this must be done before any routes added)
                const headerUpdater = (activeMenu: string | undefined) => {
                    this._activeMenu = activeMenu;
                };
                router.hooks({
                    before(done, match) {
                        headerUpdater(match ? match.url.split('/')[0] : undefined);
                        done();
                    }
                });

                // Configure routes
                this.appConfig.pages.forEach((pageProvider, index) => {
                    if (pageProvider.routes) {
                        pageProvider.routes.forEach((route) => {
                            router.on(
                                route, (match) => {
                                    this._store.dispatch(updatePage({page: pageProvider.name, params: match!.data}));
                                }
                            );
                        });
                    }
                });
                if (this.appConfig.pages.length > 0) {
                    router.notFound(() => {
                        this._store.dispatch(updatePage(this.appConfig!.pages[0].name));
                    });
                }
                router.resolve();
            } else {
                showErrorDialog(manager.isError ? "managerError." + manager.error : "");
            }
        });
    }

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (!this._initialised) {
            return;
        }

        // If either page or 'offline'-status is changed, it should update to the correct page,
        // by appending the page to the HTML content
        if (changedProps.has("_page") || changedProps.has("_offline") || changedProps.has("_showOfflineFallback")) {
            if (this._mainElem) {

                const pageProvider = this.appConfig!.pages.find((page) => page.name === this._page);
                const showOfflineFallback = (this._showOfflineFallback && !pageProvider?.allowOffline);
                const offlinePage = this._mainElem.querySelector('#offline-page');

                // If page has changed, replace the previous content with the new page.
                // However, if no page is present yet, append it to the page.
                if(changedProps.has('_page') && pageProvider) {
                    const currentPage = this._mainElem.firstElementChild;
                    if(currentPage) {
                        const newPage = pageProvider.pageCreator();
                        if(showOfflineFallback) {
                            newPage.style.setProperty('display', 'none'); // hide the new page while offline overlay page is shown
                            newPage.setAttribute('loadedDuringOffline', 'true'); // mark the page as "loaded during offline", since the content is either empty or invalid
                        }
                        this._mainElem.replaceChild(newPage, currentPage); // replace content
                    } else {
                        this._mainElem.appendChild(pageProvider.pageCreator());
                    }
                }

                // CASE: "Offline overlay page is present, but should not be shown"
                if(offlinePage && !showOfflineFallback) {
                    this._mainElem.removeChild(offlinePage); // remove offline overlay

                    const elem = this._mainElem.firstElementChild as Page<any>;
                    elem?.style.removeProperty('display'); // show the current page again (back to the foreground)
                    if(elem?.onRefresh) {
                        elem.onRefresh(); // If custom onRefresh() is set by the page, run that function.
                    }
                }

                // CASE: "Offline overlay page is NOT present, but needs to be there"
                // It either shows the default offline fallback page, or a custom one defined in the AppConfig.
                else if(!offlinePage && showOfflineFallback) {
                    const newOfflinePage = (this.appConfig?.offlinePage) ? this.appConfig.offlinePage.pageCreator() : pageOfflineProvider(this._store).pageCreator();
                    (this._mainElem.firstElementChild as HTMLElement)?.style.setProperty('display', 'none'); // Hide the current page (to the background)
                    newOfflinePage.id = "offline-page";
                    this._mainElem.appendChild(newOfflinePage);
                }
            }

            this.updateWindowTitle();
        }
    }

    protected shouldUpdate(changedProps: PropertyValues): boolean {
        if (changedProps.has("_realm")) {
            this._config = this._getConfig();
            if (this._realm) {
                manager.displayRealm = this._realm;
                window.sessionStorage.setItem("realm", this._realm!);
            } else {
                window.sessionStorage.removeItem("realm");
            }
        }

        if (changedProps.has("_config") && this._config) {

            if (!this._config.logo) {
                this._config.logo = DefaultLogo;
            }
            if (!this._config.logoMobile) {
                this._config.logoMobile = DefaultMobileLogo;
            }

            const favIcon = this._config && this._config.favicon ? this._config.favicon : DefaultFavIcon;

            let link = document.querySelector("link[rel~='icon']") as HTMLLinkElement;

            if (!link) {
                link = document.createElement("link");
                link.rel = "icon";
                document.getElementsByTagName("head")[0].appendChild(link);
            }
            link.href = favIcon;
        }

        this.updateWindowTitle();
        return super.shouldUpdate(changedProps);
    }

    protected render(): TemplateResult | void {

        if (!this._initialised) {
            return html`<or-mwc-dialog id="app-modal"></or-mwc-dialog>`;
        }
        let consoleStyles;
        if (manager.consoleAppConfig) {
            const consoleAppConfig = manager.consoleAppConfig;
            const primary = consoleAppConfig.primaryColor;
            const secondary = consoleAppConfig.secondaryColor;
            consoleStyles = html`<style>:host {--or-console-primary-color:${primary};--or-console-secondary-color:${secondary};}</style>`;
        }
        return html`
            ${this._config.styles ? typeof(this._config.styles) === "string" ? html`<style>${this._config.styles}</style>` : this._config.styles.strings : ``}
            ${consoleStyles}
            ${this._config.header ? html`
                <or-header .activeMenu="${this._activeMenu}" .store="${this._store}" .realm="${this._realm}" .realms="${this._realms}" .logo="${this._config.logo}" .logoMobile="${this._config.logoMobile}" .config="${this._config.header}"></or-header>
            ` : ``}
            
            <!-- Main content -->
            <main role="main" class="main-content d-none"></main>
            
            <slot></slot>
        `;
    }

    public stateChanged(state: AppStateKeyed) {
        this._realm = state.app.realm;
        this._page = state.app!.page;
        this._offline = state.app!.offline;
    }

    protected _onEvent(event: OREvent) {
        if(event === OREvent.OFFLINE) {
            if(!this._offline) {
                this._store.dispatch((setOffline(true)))
            }
        } else if(event === OREvent.ONLINE) {
            if(this._offline) {
                this._showOfflineFallback = false;
                this._completeOfflineFallbackTimer(); // complete fallback timer
                this._store.dispatch((setOffline(false)));
            }
        } else if(event === OREvent.RECONNECT_FAILED) {
            this._startOfflineFallbackTimer(); // start fallback timer (if not done yet)

        } else if(event === OREvent.CONSOLE_VISIBLE) {
            this._store.dispatch((setVisibility(true)));

            // When the manager appears on Mobile devices, but the connection is OFFLINE,
            // we reset the timer to the {appConfig.offlineTimeout} seconds. This is because we saw issues with reopening the app,
            // and seeing a connection interval of 30+ seconds. We now give the user the benefit of the doubt, by resetting the timer.
            if(manager.console?.isMobile && this._offline) {
                this._startOfflineFallbackTimer(true);
            }
            // Always try reconnecting (if necessary)
            manager.reconnect(true);

        } else if(event === OREvent.CONSOLE_HIDDEN) {
            this._store.dispatch((setVisibility(false)));
        }
    }

    // Offline timer logic
    //
    // This will start a Deferred promise that keeps track of the 'wait before showing offline page' timer.
    // - Resolving the promise updates the 'show offline fallback' variable based on OFFLINE state.
    // - Rejecting the promise 'aborts the process' and skips that logic and does nothing.
    //
    // To explain; when the Manager reports "We're offline!" it will wait 10+ seconds before visually reporting the user that he/she is offline.
    // However, if the user reconnects within that time period, we resolve this promise early. (which is why using Deferred is useful)
    protected _startOfflineFallbackTimer(force = false): void {
        if(force) {
            this._completeOfflineFallbackTimer(true);
        } else if(this._offlineFallbackDeferred || this._showOfflineFallback) {
            return;
        }

        const deferred = new Util.Deferred<void>();
        let finished = false;
        deferred.promise.then(() => {
            this._showOfflineFallback = this._offline;
        }).finally(() => {
            finished = true;
        });

        setTimeout(() => {
            if(!finished) { deferred.resolve(); }  // resolve THIS timer if not done yet.
        }, this.appConfig?.offlineTimeout || 10000)

        this._offlineFallbackDeferred = deferred;
    }

    // Completes and removes the 'show offline page' timer
    // Resolving the timer updates the 'show offline fallback' variable based on OFFLINE state.
    // if 'aborted' is TRUE it will skip that logic. See startOfflineTimer() for more details.
    protected _completeOfflineFallbackTimer(aborted = false) {
        if(aborted) {
            this._offlineFallbackDeferred?.reject();
        } else {
            this._offlineFallbackDeferred?.resolve();
        }
        this._offlineFallbackDeferred = undefined;
    }

    public logout() {
        manager.logout();
    }

    public setLanguage(lang: string) {
        manager.language = lang;
    }

    public showLanguageModal() {
        showDialog(new OrMwcDialog()
            .setHeading("language")
            .setDismissAction(null)
            .setStyles(html`<style>.selected { color: ${unsafeCSS(DefaultColor4)} }</style>`)
            .setActions(Object.entries(this.appConfig!.languages || DEFAULT_LANGUAGES).map(([key, value]) => {
                return {
                    content: html`<span class="${(key === manager.language) ? 'selected' : ''}">${i18next.t(value)}</span>`,
                    actionName: key,
                    action: () => {
                        manager.language = key;
                    }
                }})));
    }

    protected doAppConfigInit() {
        this.appConfig = this.appConfig || (this.appConfigProvider ? this.appConfigProvider(manager) : undefined);

        if (!this.appConfig) {
            return;
        }

        if (!this._config) {
            this._config = this._getConfig();
        }
    }

    protected doBasicLogin(username: string | undefined, password: string | undefined): PromiseLike<BasicLoginResult> {
        const deferred = new Util.Deferred<BasicLoginResult>();

        let u = username;
        let p = password;

        // language=CSS
        const styles = html`
            #login-logo {
                width: 24px;
                height: 24px;
            }
            
            #login_wrapper > or-mwc-input {
                margin: 10px 0;
                width: 100%;
            }
        `;

        const dialog = showDialog(new OrMwcDialog()
            .setStyles(html`<style>${styles}</style>`)
            .setHeading(html`<img id="login-logo" src="${this._config.logoMobile || this._config.logo}" /></or-icon><or-translate value="login"></or-translate>`)
            .setContent(html`
                <div id="login_wrapper">
                    <or-mwc-input .label="${i18next.t("user")}" .type="${InputType.TEXT}" min="1" required .value="${username}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => u = e.detail.value}"></or-mwc-input>            
                    <or-mwc-input .label="${i18next.t("password")}" .type="${InputType.PASSWORD}" min="1" required .value="${password}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => p = e.detail.value}"></or-mwc-input>           
                </div>
            `)
            .setActions([
                {
                    actionName: "submit",
                    default: true,
                    action: () => {
                        deferred.resolve({
                            cancel: false,
                            username: u!,
                            password: p!
                        });
                    },
                    content: html`<or-mwc-input .type=${InputType.BUTTON} .label="${i18next.t("submit")}" raised></or-mwc-input>`
                }
            ]), document.body); // Attach to document as or-app isn't visible until initialised

        return deferred.promise;
    }

    protected updateWindowTitle() {
        if (!this._initialised) {
            return;
        }

        const appTitle = this._config.appTitle || "";
        let pageTitle = (i18next.isInitialized ? i18next.t(appTitle) : appTitle);
        const pageElem = (this._mainElem ? this._mainElem.firstElementChild : undefined) as Page<any>;
        if (pageElem) {
            pageTitle += (i18next.isInitialized ? " - " + i18next.t(pageElem.name) : " - " + pageElem.name);
        }

        updateMetadata({
            title: pageTitle,
            description: pageTitle
        });
    }

    protected _getConfig(): RealmAppConfig {
        const defaultConfig = this.appConfig!.realms ? this.appConfig!.realms.default : {};
        let realmConfig = this.appConfig!.realms ? this.appConfig!.realms![this._realm || ""] : undefined;
        realmConfig = Util.mergeObjects(defaultConfig, realmConfig, false);

        if (this.appConfig && this.appConfig.superUserHeader && manager.isSuperUser()) {
            realmConfig.header = this.appConfig.superUserHeader;
        }
        return realmConfig;
    }
}
