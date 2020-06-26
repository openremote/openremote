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
import "@openremote/or-translate";
import "@openremote/or-mwc-components/dist/or-mwc-menu";
import "./or-header";
import "@openremote/or-icon";
import {updateMetadata} from "pwa-helpers/metadata";
import i18next from "i18next";
import Navigo from "navigo";
import manager, {Auth, DefaultColor2, DefaultColor3, DefaultHeaderHeight, ManagerConfig, Util, BasicLoginResult} from "@openremote/core";
import {DEFAULT_LANGUAGES, HeaderConfig, HeaderItem, Languages} from "./or-header";
import {DialogConfig, OrMwcDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {AnyAction, EnhancedStore, Unsubscribe, Action} from "@reduxjs/toolkit";
import {AppStateKeyed, updatePage, updateParams} from "./app";
import {ThunkMiddleware} from "redux-thunk";
import { translate } from "@openremote/or-translate";
import { InputType, OrInputChangedEvent } from "@openremote/or-input";
export * from "./app";
export * from "./or-header";

// Declare MANAGER_URL - Global var injected by webpack
declare var MANAGER_URL: string;

// Configure routing
export const router = new Navigo(null, true, "#!");
export {HeaderConfig};

export interface DefaultAppConfig {
    appTitle: string;
    logo: HTMLTemplateElement | string;
    logoMobile: HTMLTemplateElement | string;
    language?: string;
    header: HeaderConfig;
    styles?: TemplateResult;
}

export interface RealmAppConfig {
    appTitle?: string;
    logo?: HTMLTemplateElement | string;
    logoMobile?: HTMLTemplateElement | string;
    language?: string;
    header?: HeaderConfig;
    styles?: TemplateResult;
}

export interface AppConfig<S extends AppStateKeyed> {
    pages?: {
        default: PageProvider<S>;
        [name: string]: PageProvider<S>;
    };
    default?: DefaultAppConfig;
    realms?: {
        [realm: string]: RealmAppConfig;
    };
}

export interface PageProvider<S extends AppStateKeyed> {
    routes: string[];
    pageCreator: () => Page<S>;
}

export abstract class Page<S extends AppStateKeyed> extends translate(i18next)(LitElement) {

    protected _store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>;

    protected _storeUnsubscribe!: Unsubscribe;

        constructor(store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>) {
        super();
        this._store = store;
    }

    connectedCallback() {
        if (super.connectedCallback) {
            super.connectedCallback();
        }

        this._storeUnsubscribe = this._store.subscribe(() => this.stateChanged(this._store.getState()));
        this.stateChanged(this._store.getState());
    }

    disconnectedCallback() {
        this._storeUnsubscribe();

        if (super.disconnectedCallback) {
            super.disconnectedCallback();
        }
    }

    abstract stateChanged(state: S);
}

export function headerItemMap<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "map",
        href: "#!map",
        text: "map"
    };
}

export function headerItemAssets<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "rhombus-split",
        href: "#!assets",
        text: "asset_plural",
    };
}

export function headerItemRules<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "state-machine",
        href: "#!rules",
        text: "rule_plural",
        hideMobile: true
    };
}

export function headerItemInsights<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "chart-areaspline",
        href: "#!insights",
        text: "insights",
        hideMobile: true
    };
}

export function headerItemGatewayConnection<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "cloud",
        value: "gateway",
        href: "#!gateway",
        text: "gatewayConnection",
        roles: ["write:admin", "read:admin"]
    };
}

export function headerItemLanguage<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "web",
        value: "language",
        text: "language",
        action: () => {
            orApp.showLanguageModal();
        }
    };
}

export function headerItemLogout<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "logout",
        value: "logout",
        text: "logout",
        action: () => {
            orApp.logout();
        }
    };
}

export function headerItemLogs<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "file-document-box-search-outline",
        value: "logs",
        href: "#!logs",
        text: "logs",
        hideMobile: true
    };
}

export function headerItemAccount<S extends AppStateKeyed, A extends AnyAction>(orApp: OrApp<S>): HeaderItem {
    return {
        icon: "account",
        value: "account",
        href: "#!account",
        text: "account"
    };
}

function getRealmQueryParameter(): string {
    return Util.getQueryParameter(location.search, "realm");
}

const DEFAULT_MANAGER_CONFIG: ManagerConfig = {
    managerUrl: MANAGER_URL,
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: getRealmQueryParameter(),
    consoleAutoEnable: true,
    loadTranslations: ["app", "or"]
};

@customElement("or-app")
export class OrApp<S extends AppStateKeyed> extends LitElement {

    @property({type: Object})
    public appConfig!: AppConfig<S>;

    @property({type: Object})
    public managerConfig?: ManagerConfig;

    @query("main")
    protected _mainElem!: HTMLElement;

    @property()
    protected _initialised = false;

    @property()
    protected _page?: string;

    @query("#app-modal")
    protected _appModal!: OrMwcDialog;

    protected _config!: DefaultAppConfig;

    protected _store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>;
    protected _storeUnsubscribe!: Unsubscribe;

    // language=CSS
    static get styles() {
        return css`
            :host {
                --or-app-color2: ${unsafeCSS(DefaultColor2)};
                --or-app-color3: #22211f;
                --or-app-color4: #4D9D2A;
                color: ${unsafeCSS(DefaultColor3)};
                fill: ${unsafeCSS(DefaultColor3)};
                font-size: 14px;
            }
                
            .main-content {
                display: flex;
                padding-top: ${unsafeCSS(DefaultHeaderHeight)};
                height: 100vh;
                box-sizing: border-box;
                background-color: var(--or-app-color2);
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
    }

    connectedCallback() {
        if (super.connectedCallback) {
            super.connectedCallback();
        }

        this._storeUnsubscribe = this._store.subscribe(() => this.stateChanged(this._store.getState()));
        this.stateChanged(this._store.getState());
    }

    disconnectedCallback() {
        this._storeUnsubscribe();

        if (super.disconnectedCallback) {
            super.disconnectedCallback();
        }
    }

    protected firstUpdated(_changedProperties: Map<PropertyKey, unknown>): void {
        super.firstUpdated(_changedProperties);

        if (!this.appConfig) {
            console.error("No AppConfig supplied");
            return;
        }

        if (!this._store) {
            console.error("No Redux store supplied");
            return;
        }

        if (!this.appConfig.pages || Object.keys(this.appConfig.pages).length === 0) {
            console.error("No page providers");
            return;
        }

        Object.entries(this.appConfig.pages).map(([pageName, provider]) => {
            if (provider.routes) {
                provider.routes.forEach((route) => {
                    router.on(
                        route, (params, query) => {
                            this._store.dispatch(updatePage(pageName));
                            this._store.dispatch(updateParams(params));
                        }
                    )
                });
            }
        });

        if (this.appConfig.pages.default) {
            router.on("*", (params, query) => {
                this._store.dispatch(updatePage("default"));
            })
        }

        const realm = getRealmQueryParameter();
        const config = this._getConfig(realm);

        if (!config) {
            console.error("No default AppConfig or realm specific config for requested realm: " + realm);
            return;
        } else {
            this._config = config;
        }

        const managerConfig = this.managerConfig || DEFAULT_MANAGER_CONFIG;
        managerConfig.basicLoginProvider = (u, p) => this.doBasicLogin(u, p);

        console.info("Initialising the manager");

        manager.init(managerConfig).then((success) => {
            if (success) {
                this._initialised = true;
                router.resolve();
            } else {
                this.showErrorModal(manager.isError ? "managerError." + manager.error : "");
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
            
            #login_wrapper > or-input {
                margin: 10px 0;
                width: 100%;
            }
        `;

        this.showModal({
            styles: html`<style>${styles}</style>`,
            title: html`<img id="login-logo" src="${this._config.logoMobile || this._config.logo}" /></or-icon><or-translate value="login"></or-translate>`,
            content: html`
                <div id="login_wrapper">
                    <or-input .label="${i18next.t("user")}" .type="${InputType.TEXT}" min="1" required .value="${username}" @or-input-changed="${(e: OrInputChangedEvent) => u = e.detail.value}"></or-input>            
                    <or-input .label="${i18next.t("password")}" .type="${InputType.PASSWORD}" min="1" required .value="${password}" @or-input-changed="${(e: OrInputChangedEvent) => p = e.detail.value}"></or-input>           
                </div>
            `,
            actions: [
                {
                    actionName: "submit",
                    default: true,
                    action: () => {
                        deferred.resolve({
                            cancel: false,
                            username: u,
                            password: p,
                            closeCallback: () => { this.closeModal(); }
                        });
                    },
                    content: html`<or-input .type=${InputType.BUTTON} .label="${i18next.t("submit")}" raised></or-input>`
                }
            ]
        });

        return deferred.promise;
    }

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (!this._initialised) {
            return;
        }

        if (changedProps.has("_page")) {
            const appTitle = this._config.appTitle || "OpenRemote Manager";
            let pageTitle = (i18next.isInitialized ? i18next.t(appTitle) : appTitle);

            if (this._mainElem) {
                if (this._mainElem.firstElementChild) {
                    this._mainElem.firstElementChild.remove();
                }
                if (this._page && this.appConfig.pages[this._page]) {
                    const pageElem = this.appConfig.pages[this._page].pageCreator();
                    this._mainElem.appendChild(pageElem);
                    pageTitle += (i18next.isInitialized ? " - " + i18next.t(this._page) : " - " + this._page);
                }
            }

            updateMetadata({
                title: pageTitle,
                description: pageTitle
            });
        }
    }

    protected render(): TemplateResult | void {

        if (!this._initialised) {
            return html`<or-mwc-dialog id="app-modal"></or-mwc-dialog>`;
        }
        return html`
            ${unsafeHTML(this._config.styles ? this._config.styles.strings : ``)}
            <or-header logo="${this._config.logo}" .logoMobile="${this._config.logoMobile}" .config="${this._config.header}"></or-header>
            
            <!-- Main content -->
            <main role="main" class="main-content d-none"></main>

            <or-mwc-dialog id="app-modal"></or-mwc-dialog>
        `;
    }

    public logout() {
        manager.logout();
    }

    public setLanguage(lang: string) {
        manager.language = lang;
    }

    public showLanguageModal() {
        this.showModal(this._getLanguageModalConfig(DEFAULT_LANGUAGES));
    }

    public showErrorModal(errorMessage: string) {
         this.showModal({
             title: "error",
             content: html`
                <div>
                    <p><or-translate value="errorOccurred"></or-translate>
                    ${errorMessage ? html`
                        :</p>
                        <p>
                            <or-translate value="error"></or-translate>
                            <span> = </span> 
                            <or-translate .value="${errorMessage}"></or-translate>
                    ` : ``}
                    </p>
                </div>`,
             actions: [{
                 actionName: "ok",
                 content: i18next.t("ok"),
                 default: true
             }]
         });
    }

    public showModal(config: DialogConfig) {
        if (this._appModal) {
            this._appModal.config = config;
            this._appModal.open();
        }
    }

    public closeModal() {
        if (this._appModal) {
            this._appModal.close();
        }
    }

    public stateChanged(state: S) {
        this._page = state.app!.page;
    }

    protected _getConfig(realm: string): DefaultAppConfig {
         const realmConfig = this.appConfig.realms ? this.appConfig.realms![realm] : undefined;
         return Object.assign({}, this.appConfig!.default, realmConfig);
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
            actions: actions
        };
    }
}
