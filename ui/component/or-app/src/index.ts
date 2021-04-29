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
import {unsafeHTML} from "lit-html/directives/unsafe-html";
import {AppConfig, RealmAppConfig, router} from "./types";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-menu";
import "@openremote/or-mwc-components/or-mwc-snackbar";
import "./or-header";
import "@openremote/or-icon";
import {updateMetadata} from "pwa-helpers/metadata";
import i18next from "i18next";
import manager, {Auth, DefaultColor2, DefaultColor3, ManagerConfig, Util, BasicLoginResult, OREvent, normaliseConfig, Manager} from "@openremote/core";
import {DEFAULT_LANGUAGES, HeaderConfig, HeaderItem, Languages} from "./or-header";
import {DialogConfig, OrMwcDialog, showErrorDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrMwcSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {AnyAction, EnhancedStore, Unsubscribe} from "@reduxjs/toolkit";
import {ThunkMiddleware} from "redux-thunk";
import {AppStateKeyed, updatePage} from "./app";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { ORError } from "@openremote/core";

const DefaultLogo = require("../images/logo.png");
const DefaultMobileLogo = require("../images/logo-mobile.png");
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
    realm: getRealmQueryParameter(),
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

    @property()
    protected _initialised = false;

    @property()
    protected _page?: string;

    @property()
    protected _config!: RealmAppConfig;

    protected _store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>;
    protected _storeUnsubscribe!: Unsubscribe;

    // language=CSS
    static get styles() {
        return css`
            :host {
                --or-app-color2: ${unsafeCSS(DefaultColor2)};
                --or-app-color3: #4C4C4C;
                --or-app-color4: #4D9D2A;
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

    connectedCallback() {
        super.connectedCallback();
        this._storeUnsubscribe = this._store.subscribe(() => this.stateChanged(this._store.getState()));
        this.stateChanged(this._store.getState());
    }

    disconnectedCallback() {
        this._storeUnsubscribe();
        super.disconnectedCallback();
    }

    protected _onManagerEvent = (event: OREvent) => {
        switch (event) {
            case OREvent.DISPLAY_REALM_CHANGED:
                const config = this._getConfig(manager.displayRealm);

                if (!config) {
                    console.error("No default AppConfig or realm specific config for requested realm: " + manager.displayRealm);
                    return;
                } else {
                    this._config = config;
                }
                break;
        }
    };

    protected doAppConfigInit() {
        this.appConfig = this.appConfig || (this.appConfigProvider ? this.appConfigProvider(manager) : undefined);

        if (!this.appConfig) {
            return;
        }

        if (!this._config) {
            const realm = getRealmQueryParameter();
            this._config = this._getConfig(realm);
        }
    }

    protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
        super.firstUpdated(_changedProperties);

        const managerConfig: ManagerConfig = this.managerConfig ? {...DEFAULT_MANAGER_CONFIG,...this.managerConfig} : DEFAULT_MANAGER_CONFIG;
        managerConfig.skipFallbackToBasicAuth = true; // We do this so we can load styling config before displaying basic login
        managerConfig.basicLoginProvider = (u, p) => this.doBasicLogin(u, p);
        manager.addListener(this._onManagerEvent);

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

                this._initialised = true;

                this.appConfig.pages.forEach((pageProvider, index) => {
                    if (pageProvider.routes) {
                        pageProvider.routes.forEach((route) => {
                            router.on(
                                route, (params, query) => {
                                    this._store.dispatch(updatePage({page: pageProvider.name, params: params}));
                                }
                            );
                        });
                    }
                });

                if (this.appConfig.pages.length > 0) {
                    router.on("*", (params, query) => {
                        this._store.dispatch(updatePage(this.appConfig!.pages[0].name));
                    });
                }
               
                router.resolve();
            } else {
                showErrorDialog(manager.isError ? "managerError." + manager.error : "");
            }
        });
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

        const dialog = showDialog({
            styles: html`<style>${styles}</style>`,
            title: html`<img id="login-logo" src="${this._config.logoMobile || this._config.logo}" /></or-icon><or-translate value="login"></or-translate>`,
            content: html`
                <div id="login_wrapper">
                    <or-mwc-input .label="${i18next.t("user")}" .type="${InputType.TEXT}" min="1" required .value="${username}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => u = e.detail.value}"></or-mwc-input>            
                    <or-mwc-input .label="${i18next.t("password")}" .type="${InputType.PASSWORD}" min="1" required .value="${password}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => p = e.detail.value}"></or-mwc-input>           
                </div>
            `,
            actions: [
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
            ]
        }, document.body); // Attach to document as or-app isn't visible until initialised

        return deferred.promise;
    }

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (!this._initialised) {
            return;
        }

        if (changedProps.has("_page")) {
            const appTitle = this._config.appTitle || "";
            let pageTitle = (i18next.isInitialized ? i18next.t(appTitle) : appTitle);

            if (this._mainElem) {
                if (this._mainElem.firstElementChild) {
                    this._mainElem.firstElementChild.remove();
                }
                if (this._page) {
                    const pageProvider = this.appConfig!.pages.find((page) => page.name === this._page);
                    if (pageProvider) {
                        const pageElem = pageProvider.pageCreator();
                        this._mainElem.appendChild(pageElem);
                        pageTitle += (i18next.isInitialized ? " - " + i18next.t(pageElem.name) : " - " + pageElem.name);
                    }
                }
            }

            updateMetadata({
                title: pageTitle,
                description: pageTitle
            });
        }
    }

    protected shouldUpdate(changedProps: PropertyValues): boolean {
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
            ${this._config.styles ? typeof(this._config.styles) === "string" ? html`<style>${this._config.styles}</style>` : unsafeHTML(this._config.styles.strings) : ``}
            ${consoleStyles}
            ${this._config.header ? html`
                <or-header .logo="${this._config.logo}" .logoMobile="${this._config.logoMobile}" .config="${this._config.header}"></or-header>
            ` : ``}
            
            <!-- Main content -->
            <main role="main" class="main-content d-none"></main>
            
            <slot></slot>
        `;
    }

    public logout() {
        manager.logout();
    }

    public setLanguage(lang: string) {
        manager.language = lang;
    }

    public showLanguageModal() {
        showDialog(this._getLanguageModalConfig(DEFAULT_LANGUAGES));
    }

    public stateChanged(state: S) {
        this._page = state.app!.page;
    }

    protected _getConfig(realm: string | undefined): RealmAppConfig {
        realm = realm || "default";
        const defaultConfig = this.appConfig!.realms ? this.appConfig!.realms.default : {};
        let realmConfig = this.appConfig!.realms ? this.appConfig!.realms![realm] : undefined;
        realmConfig = Util.mergeObjects(defaultConfig, realmConfig, false);

        if (this.appConfig && this.appConfig.superUserHeader && manager.isSuperUser()) {
            realmConfig.header = this.appConfig.superUserHeader;
        }
        return realmConfig;
    }

    protected _getLanguageModalConfig(languages: Languages): DialogConfig {
        const title = "language";
        const actions = Object.entries(languages).map(([key, value]) => {
            return {
                content: i18next.t(value),
                actionName: key,
                action: () => {
                    manager.language = key;
                }
            };
        });

        return {
            title: title,
            actions: actions,
            dismissAction: null
        };
    }
}
