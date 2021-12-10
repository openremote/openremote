import {
    css,
    html,
    LitElement,
    PropertyValues,
    TemplateResult,
    unsafeCSS
} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {AppConfig, Page, RealmAppConfig, router} from "./types";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-menu";
import "@openremote/or-mwc-components/or-mwc-snackbar";
import "./or-header";
import "@openremote/or-icon";
import {updateMetadata} from "pwa-helpers/metadata";
import i18next from "i18next";
import manager, {Auth, DefaultColor2, DefaultColor3, DefaultColor4, ManagerConfig, Util, BasicLoginResult, OREvent, normaliseConfig, Manager} from "@openremote/core";
import {DEFAULT_LANGUAGES, HeaderConfig} from "./or-header";
import {OrMwcDialog, showErrorDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrMwcSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {AnyAction, EnhancedStore, Unsubscribe} from "@reduxjs/toolkit";
import {ThunkMiddleware} from "redux-thunk";
import {AppStateKeyed, updatePage, updateRealm} from "./app";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { ORError } from "@openremote/core";
import { Tenant } from "@openremote/model";

const DefaultLogo = require("../images/logo.svg");
const DefaultMobileLogo = require("../images/logo-mobile.svg");
const DefaultFavIcon = require("../images/favicon.ico");

export * from "./app";
export * from "./or-header";
export * from "./types";

// Declare MANAGER_URL and KEYCLOAK_URL - Global var injected by webpack
declare var MANAGER_URL: string | undefined;
declare var KEYCLOAK_URL: string | undefined;

export {HeaderConfig};

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

    protected _realms!: Tenant[];
    protected _store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>;
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

    constructor(store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>) {
        super();
        this._store = store;

        // Set this element as the host for dialogs
        OrMwcDialog.DialogHostElement = this;
        OrMwcSnackbar.DialogHostElement = this;
    }

    public getState(): S {
        return this._store.getState();
    }

    connectedCallback() {
        super.connectedCallback();
        this._storeUnsubscribe = this._store.subscribe(() => this.stateChanged(this.getState()));
        this.stateChanged(this.getState());
    }

    disconnectedCallback() {
        this._storeUnsubscribe();
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
        managerConfig.basicLoginProvider = (u, p) => this.doBasicLogin(u, p);

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
                const response = await manager.rest.api.TenantResource.getAccessible();
                this._realms = response.data;
                let realm: string | null | undefined = undefined;

                // Set current display realm if super user
                if (manager.isSuperUser()) {
                    // Look in session storage
                    realm = window.sessionStorage.getItem("realm");
                    if (realm && !this._realms.some(r => r.realm === realm)) {
                        realm = undefined;
                    }
                }

                this._store.dispatch(updateRealm(realm || manager.getRealm() || "master"));

                this._initialised = true;

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

        if (changedProps.has("_page")) {
            if (this._mainElem) {
                if (this._mainElem.firstElementChild) {
                    this._mainElem.firstElementChild.remove();
                }
                if (this._page) {
                    const pageProvider = this.appConfig!.pages.find((page) => page.name === this._page);
                    if (pageProvider) {
                        const pageElem = pageProvider.pageCreator();
                        this._mainElem.appendChild(pageElem);
                    }
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
                <or-header .store="${this._store}" .realm="${this._realm}" .realms="${this._realms}" .logo="${this._config.logo}" .logoMobile="${this._config.logoMobile}" .config="${this._config.header}"></or-header>
            ` : ``}
            
            <!-- Main content -->
            <main role="main" class="main-content d-none"></main>
            
            <slot></slot>
        `;
    }

    public stateChanged(state: S) {
        this._realm = state.app.realm;
        this._page = state.app!.page;
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
            .setActions(Object.entries(DEFAULT_LANGUAGES).map(([key, value]) => {
                return {
                    content: i18next.t(value),
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
