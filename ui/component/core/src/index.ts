import "url-search-params-polyfill";
import {Console} from "./console";
import rest from "@openremote/rest";
import {AxiosRequestConfig} from "axios";
import {EventProvider, EventProviderFactory, EventProviderStatus, WebSocketEventProvider} from "./event";
import i18next, {InitOptions} from "i18next";
import i18nextBackend from "i18next-http-backend";
import moment from "moment";
import {
    AssetModelUtil,
    Auth,
    ConsoleAppConfig,
    EventProviderType,
    ManagerConfig,
    MapType,
    Role,
    User,
    UsernamePassword
} from "@openremote/model";
import * as Util from "./util";
import {createMdiIconSet, createSvgIconSet, IconSets, OrIconSet} from "@openremote/or-icon";
import Keycloak from 'keycloak-js';

// Re-exports
export {Util};
export * from "./asset-mixin";
export * from "./console";
export * from "./event";
export * from "./defaults";

export const DEFAULT_ICONSET = "mdi";
export const OPENREMOTE_CLIENT_ID = "openremote";
export const RESTRICTED_USER_REALM_ROLE = "restricted_user";

export declare type KeycloakPromise<T> = {
    success<TResult1 = T, TResult2 = never>(onfulfilled?: ((value: T) => TResult1 | KeycloakPromise<TResult1>) | undefined | null, onrejected?: ((reason: any) => TResult2 | KeycloakPromise<TResult2>) | undefined | null): KeycloakPromise<TResult1 | TResult2>;
    error<TResult = never>(onrejected?: ((reason: any) => TResult | KeycloakPromise<TResult>) | undefined | null): Promise<T | TResult>;
}

export enum ORError {
    MANAGER_FAILED_TO_LOAD = "MANAGER_FAILED_TO_LOAD",
    AUTH_FAILED = "AUTH_FAILED",
    AUTH_TYPE_UNSUPPORTED = "AUTH_TYPE_UNSUPPORTED",
    CONSOLE_ERROR = "CONSOLE_INIT_ERROR",
    EVENTS_CONNECTION_ERROR = "EVENTS_CONNECTION_ERROR",
    TRANSLATION_ERROR = "TRANSLATION_ERROR"
}

export enum OREvent {
    ERROR = "ERROR",
    READY = "READY",
    ONLINE = "ONLINE",
    OFFLINE = "OFFLINE",
    CONNECTING = "CONNECTING",
    CONSOLE_INIT = "CONSOLE_INIT",
    CONSOLE_READY = "CONSOLE_READY",
    TRANSLATE_INIT = "TRANSLATE_INIT",
    TRANSLATE_LANGUAGE_CHANGED = "TRANSLATE_LANGUAGE_CHANGED",
    DISPLAY_REALM_CHANGED = "DISPLAY_REALM_CHANGED"
}

export interface LoginOptions {
    redirectUrl?: string;
    action?: string;
    credentials?: UsernamePassword;
}

export interface BasicLoginResult {
    username: string;
    password: string;
    cancel: boolean;
}

export interface Languages {
    [langKey: string]: string;
}

export const DEFAULT_LANGUAGES: Languages = {
    en: "english",
    cn: "chinese",
    nl: "dutch",
    fr: "french",
    de: "german",
    it: "italian",
    pt: "portuguese",
    ro: "romanian",
    es: "spanish",
    uk: "ukrainian"
};

export function normaliseConfig(config: ManagerConfig): ManagerConfig {
    const normalisedConfig: ManagerConfig = config ? Object.assign({}, config) : {};

    if (!normalisedConfig.managerUrl || normalisedConfig.managerUrl === "") {
        // Assume manager is running on same host as this code
        normalisedConfig.managerUrl = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ":" + window.location.port : "")
            + window.location.pathname.replace(/\/[^/]+\/?$/, '');
    } else {
        // Normalise by stripping any trailing slashes
        normalisedConfig.managerUrl = normalisedConfig.managerUrl.replace(/\/+$/, "");
    }

    if (!normalisedConfig.realm || normalisedConfig.realm === "") {
        // Assume master realm
        normalisedConfig.realm = "master";
    }

    if (!normalisedConfig.auth) {
        normalisedConfig.auth = Auth.KEYCLOAK;
    }

    if (normalisedConfig.consoleAutoEnable === undefined) {
        normalisedConfig.consoleAutoEnable = true;
    }

    if (normalisedConfig.applyConfigToAdmin === undefined) {
        normalisedConfig.applyConfigToAdmin = true;
    }

    if (!normalisedConfig.eventProviderType) {
        normalisedConfig.eventProviderType = EventProviderType.WEBSOCKET;
    }

    if (!normalisedConfig.pollingIntervalMillis || normalisedConfig.pollingIntervalMillis < 5000) {
        normalisedConfig.pollingIntervalMillis = 10000;
    }

    if (normalisedConfig.loadIcons === undefined) {
        normalisedConfig.loadIcons = true;
    }

    if (normalisedConfig.loadTranslations === undefined) {
        normalisedConfig.loadTranslations = ["or"];
    }

    if (normalisedConfig.translationsLoadPath === undefined) {
        normalisedConfig.translationsLoadPath = "locales/{{lng}}/{{ns}}.json";
    }

    if (normalisedConfig.loadDescriptors === undefined) {
        normalisedConfig.loadDescriptors = true;
    }

    if (normalisedConfig.clientId === undefined) {
        normalisedConfig.clientId = OPENREMOTE_CLIENT_ID;
    }

    return normalisedConfig;
}

export interface OrManagerEventDetail {
    event: OREvent;
    error?: ORError;
}

export type EventCallback = (event: OREvent) => void;

export class Manager implements EventProviderFactory {

    get username() {
        return this._username;
    }

    get error() {
        return this._error;
    }

    get authenticated() {
        return this._authenticated;
    }

    get ready() {
        return this._ready;
    }

    get config() {
        return this._config;
    }

    get roles(): Map<string, string[]> {
        const roleMap = new Map<string, string[]>();

        if (this._keycloak) {
            if (this._keycloak.resourceAccess) {
                if (this._config.clientId && this._keycloak!.resourceAccess) {
                    Object.entries(this._keycloak!.resourceAccess).forEach(([client, resourceObj]) => {
                        const roles = (resourceObj as any).roles as string[];
                        roleMap.set(client, roles);
                    })
                }
            }
        } else if (this._basicIdentity && this._basicIdentity.roles) {
            roleMap.set(this._config.clientId!, this._basicIdentity.roles!);
        }

        return roleMap;
    }

    get managerVersion() {
        return this._managerVersion;
    }

    get isManagerAvailable() {
        return this._managerVersion && this._managerVersion !== "";
    }

    get managerUrl() {
        return this._config?.managerUrl;
    }

    get keycloakUrl() {
        return this._config?.keycloakUrl;
    }

    get isError() {
        return !!this._error;
    }

    get connectionStatus() {
        return this._events && this._events.status;
    }

    get console() {
        return this._console;
    }

    get consoleAppConfig() {
        return this._consoleAppConfig;
    }

    get events() {
        return this._events;
    }

    get rest() {
        return rest;
    }

    get language() {
        return i18next.language;
    }

    set language(lang: string) {
        console.debug(`Changing language to ${lang}.`);
        if (lang) {
            i18next.changeLanguage(lang);
            this.console.storeData("LANGUAGE", lang);
            if(this.authenticated) {
                this.updateKeycloakUserLanguage(lang).catch(e => console.error(e));
            }
        }
    }

    get displayRealm() {
        return this._displayRealm || this._config.realm!;
    }

    set displayRealm(realm: string) {
        if (!this.isSuperUser() || this._displayRealm === realm) {
            return;
        }
        this._displayRealm = realm;
        this._emitEvent(OREvent.DISPLAY_REALM_CHANGED);
    }

    get clientId() {
        return this._config.clientId || OPENREMOTE_CLIENT_ID;
    }

    getEventProvider(): EventProvider | undefined {
        return this.events;
    }

    get mapType() {
        return this._config.mapType || MapType.VECTOR;
    }

    protected static MAX_RECONNECT_DELAY = 45000;
    private _error?: ORError;
    private _config!: ManagerConfig;
    private _authenticated: boolean = false;
    private _ready: boolean = false;
    private _readyCallback?: () => PromiseLike<any>;
    private _name: string = "";
    private _username: string = "";
    private _keycloak?: Keycloak;
    private _basicIdentity?: {
        token: string | undefined,
        user: User | undefined,
        roles: string[] | undefined
    };
    private _keycloakUpdateTokenInterval?: number = undefined;
    private _managerVersion: string = "";
    public _authServerUrl: string = "";
    private _listeners: EventCallback[] = [];
    private _console!: Console;
    private _consoleAppConfig?: ConsoleAppConfig;
    private _events?: EventProvider;
    private _disconnected: boolean = false;
    private _reconnectTimer?: number;
    private _displayRealm?: string;

    public isManagerSameOrigin(): boolean {
        if (!this.ready) {
            return false;
        }

        const managerUrl = new URL(this._config.managerUrl!);
        const windowUrl = window.location;
        return managerUrl.protocol === windowUrl.protocol
            && managerUrl.hostname === windowUrl.hostname
            && managerUrl.port === windowUrl.port;
    }

    public addListener(callback: EventCallback) {
        const index = this._listeners.indexOf(callback);
        if (index < 0) {
            this._listeners.push(callback);
        }
    }

    public removeListener(callback: EventCallback) {
        const index = this._listeners.indexOf(callback);
        if (index >= 0) {
            this._listeners.splice(index, 1);
        }
    }

    public async init(config: ManagerConfig): Promise<boolean> {
        if (this._config) {
            console.debug("Already initialised");
        }

        this._config = normaliseConfig(config);

        let success = await this.loadManagerInfo();

        // Create console as we need to access storage during auth
        const orConsole = new Console(this._config.realm!, this._config.consoleAutoEnable!, () => {
            this._emitEvent(OREvent.CONSOLE_READY);
        });
        this._console = orConsole;

        if (this._config.auth === Auth.BASIC) {
            // BASIC auth will likely require UI so lets init translation at least
            success = await this.doTranslateInit() && success;
            success = await this.doAuthInit();
        } else if (this._config.auth === Auth.KEYCLOAK) {

            // The info endpoint of the manager might return a relative URL (relative to the manager)
            if (!this._config.keycloakUrl && this._authServerUrl) {
                const managerURL = new URL(this._config.managerUrl!);
                let authServerURL: URL;

                if (this._authServerUrl.startsWith("//")) {
                    this._authServerUrl = managerURL.protocol + this._authServerUrl;
                }

                try {
                    authServerURL = new URL(this._authServerUrl);
                } catch (e) {
                    // Could be a relative URL
                    authServerURL = new URL(managerURL);
                    authServerURL.pathname = this._authServerUrl;
                }

                // Use manager URL info
                if (!authServerURL.protocol) {
                    authServerURL.protocol = managerURL.protocol;
                }
                if (!authServerURL.hostname) {
                    authServerURL.hostname = managerURL.hostname;
                }
                if (!authServerURL.port) {
                    authServerURL.port = managerURL.port;
                }

                this._config.keycloakUrl = authServerURL.toString();
            }

            // If we still don't know auth server URL then use manager URL
            if (!this._config.keycloakUrl) {
                this._config.keycloakUrl = this._config.managerUrl + "/auth";
            }

            // Normalise by stripping any trailing slashes
            this._config.keycloakUrl = this._config.keycloakUrl.replace(/\/+$/, "");

            success = await this.doAuthInit();

            // If failed then we can assume keycloak auth requested but unavailable
            if (!success && !this._config.skipFallbackToBasicAuth) {
                // Try fallback to BASIC
                console.debug("Falling back to basic auth");
                this._config.auth = Auth.BASIC;
                success = await this.doAuthInit();
            }
        }

        if (!success) {
            return false;
        }

        if (success) {
            success = this.doRestApiInit();
        }

        // Don't let console registration error prevent loading
        const consoleSuccess = await this.doConsoleInit();
        if(consoleSuccess) {
            // Send the console a message to clear the web history, so no pages outside the app can be accessed.
            // For example, this prevents navigating back to an authentication screen.
            this._clearWebHistory();
        }

        success = await this.doTranslateInit() && success;

        if (success) {
            success = await this.doDescriptorsInit();
            success = await this.getConsoleAppConfig();
        }

        this.doIconInit();

        // TODO: Reinstate this once websocket supports anonymous connections
        // if (success) {
        //     success = await this.doEventsSubscriptionInit();
        // }
        if (success) {
            if (this._readyCallback) {
                await this._readyCallback();
            }
            this._ready = true;
            this._emitEvent(OREvent.READY);
        } else {
            (this._config as any) = undefined;
            console.warn("Failed to initialise the manager");
        }

        this.displayRealm = config.realm || "master";

        return success;
    }

    protected async loadManagerInfo(): Promise<boolean> {
        // Check manager exists by calling the info endpoint
        try {
            const json = await new Promise<any>((resolve, reject) => {
                const oReq = new XMLHttpRequest();
                oReq.addEventListener("load", () => {
                    resolve(JSON.parse(oReq.responseText));
                });
                oReq.addEventListener("error", () => {
                    reject(new Error("Failed to contact the manager"));
                });
                oReq.open("GET", this._config.managerUrl + "/api/master/info");
                oReq.send();
            });
            this._managerVersion = json && json.version ? json.version : "";
            this._authServerUrl = json && json.authServerUrl ? json.authServerUrl : "";

            return true;
        } catch (e) {
            // TODO: Implement auto retry?
            console.error("Failed to contact the manager", e);
            this._setError(ORError.MANAGER_FAILED_TO_LOAD);
            return false;
        }
    }

    protected async doTranslateInit(): Promise<boolean> {
        if (i18next.isInitialized) {
            return true;
        }

        i18next.on("initialized", (options) => {
            this._emitEvent(OREvent.TRANSLATE_INIT);
        });

        i18next.on("languageChanged", (lng) => {
            moment.locale(lng);
            this._emitEvent(OREvent.TRANSLATE_LANGUAGE_CHANGED);
        });

        // Look for language preference in local storage
        const initOptions: InitOptions = {
            lng: await this.getConsolePreferredLanguage() || await this.getUserPreferredLanguage() || this.config.defaultLanguage || "en",
            fallbackLng: "en",
            defaultNS: "app",
            fallbackNS: "or",
            ns: this.config.loadTranslations,
            interpolation: {
                format: (value, format, lng) => {
                    if (format === "uppercase") return value.toUpperCase();
                    if (value instanceof Date) {
                        return moment(value).format(format);
                    }
                    return value;
                }
            },
            backend: {
                loadPath: (langs: string[], namespaces: string[]) => {
                    if (namespaces.length === 1 && namespaces[0] === "or") {
                        return this.config.managerUrl + "/shared/locales/{{lng}}/{{ns}}.json";
                    }

                    if (this.config.translationsLoadPath) {
                        return this.config.translationsLoadPath;
                    }

                    return "locales/{{lng}}/{{ns}}.json";
                }
            }
        };

        if (this.config.configureTranslationsOptions) {
            this.config.configureTranslationsOptions(initOptions);
        }

        try {
            await i18next.use(i18nextBackend).init(initOptions);
        } catch (e) {
            console.error(e);
            this._setError(ORError.TRANSLATION_ERROR);
            return false;
        }

        return true;
    }

    protected async doDescriptorsInit(): Promise<boolean> {
        if (!this.config.loadDescriptors) {
            return true;
        }

        try {
            const assetInfosResponse = await rest.api.AssetModelResource.getAssetInfos();
            const metaItemDescriptorResponse = await rest.api.AssetModelResource.getMetaItemDescriptors();
            const valueDescriptorResponse = await rest.api.AssetModelResource.getValueDescriptors();

            AssetModelUtil._assetTypeInfos = assetInfosResponse.data;
            AssetModelUtil._metaItemDescriptors = Object.values(metaItemDescriptorResponse.data);
            AssetModelUtil._valueDescriptors = Object.values(valueDescriptorResponse.data);
        } catch (e) {
            console.error(e);
            return false;
        }
        return true;
    }

    protected async doAuthInit(): Promise<boolean> {
        let success = true;
        switch (this._config.auth) {
            case Auth.BASIC:
                success = await this.initialiseBasicAuth();
                break;
            case Auth.KEYCLOAK:
                success = await this.loadAndInitialiseKeycloak();
                break;
            case Auth.NONE:
                // Nothing for us to do here
                return true;
            default:
                this._setError(ORError.AUTH_TYPE_UNSUPPORTED);
                return false;
        }

        if (!success) {
            return false;
        }

        // Add interceptor to inject authorization header on each request
        rest.addRequestInterceptor(
            (config: AxiosRequestConfig) => {
                if (!config!.headers!.Authorization) {
                    const authHeader = this.getAuthorizationHeader();

                    if (authHeader) {
                        config!.headers!.Authorization = authHeader;
                    }
                }

                return config;
            }
        );
        return success;
    }

    protected doRestApiInit(): boolean {
        rest.setTimeout(20000);
        rest.initialise(this.getApiBaseUrl());
        return true;
    }

    protected async doEventsSubscriptionInit(): Promise<boolean> {
        let connected = false;

        switch (this._config.eventProviderType) {
            case EventProviderType.WEBSOCKET:
                this._events = new WebSocketEventProvider(this._config.managerUrl!);
                this._events.subscribeStatusChange((status: EventProviderStatus) => this._onEventProviderStatusChanged(status));
                connected = await this._events.connect();
                break;
            case EventProviderType.POLLING:
                break;
        }

        if (!connected) {
            this._setError(ORError.EVENTS_CONNECTION_ERROR);
        }

        return connected;
    }

    // Function that connects the EventProvider.
    protected _connectEvents() {
        if(this.events?.status === EventProviderStatus.DISCONNECTED) {
            this.events!.connect().catch((e) => {
                console.error(`Failed to connect EventProvider.`);
                console.error(e);
            });
        } else {
            console.warn("Tried to connect EventProvider, but it wasn't disconnected!");
        }
    }

    protected _onEventProviderStatusChanged(status: EventProviderStatus) {
        switch (status) {
            case EventProviderStatus.DISCONNECTED:
                this._onDisconnect();
                break;
            case EventProviderStatus.CONNECTED:
                break;
            case EventProviderStatus.CONNECTING:
                this._emitEvent(OREvent.CONNECTING);
                break;
        }
    }

    protected async doConsoleInit(): Promise<boolean> {
        try {
            await this.console.initialise();
            this._emitEvent(OREvent.CONSOLE_INIT);
            return true;
        } catch (e) {
            this._setError(ORError.CONSOLE_ERROR);
            return false;
        }
    }

    protected doIconInit() {
        // Load material design and OR icon sets if requested
        if (this._config.loadIcons) {
            IconSets.addIconSet(
                "mdi",
                createMdiIconSet(manager.managerUrl!)
            );
            IconSets.addIconSet(
                "or",
                createSvgIconSet(OrIconSet.size, OrIconSet.icons)
            );
        }
    }

    protected async getConsoleAppConfig(): Promise<boolean> {
        try {
            const response = await fetch((manager.managerUrl ?? "") + "/consoleappconfig/" + manager.displayRealm + ".json");
            this._consoleAppConfig = await response.json() as ConsoleAppConfig;
            return true;
        } catch (e) {
            return true;
        }
    }

    /**
     * Checks the native console to gather the preferred language of the device.
     */
    public async getConsolePreferredLanguage(orConsole = this.console): Promise<string | undefined> {
        return orConsole.retrieveData("LANGUAGE");
    }

    /**
     * Checks the keycloak access token to gather the preferred language of a user.
     */
    public async getUserPreferredLanguage(keycloak = this._keycloak): Promise<string | undefined> {

        if(keycloak && keycloak.authenticated) {
            const profile: Keycloak.KeycloakProfile | undefined = keycloak?.profile || await keycloak?.loadUserProfile();
            if(profile?.attributes) {
                const attributes = new Map(Object.entries(profile.attributes));
                if(attributes.has("locale")) {
                    const attr = attributes.get("locale") as any[];
                    if(typeof attr[0] === "string") {
                        return attr[0];
                    }
                }
                console.warn("Could not get user language from keycloak: no user attributes were found.");
            } else {
                console.warn("Could not get user language from keycloak: no valid keycloak user profile was found.");
            }
        }
    }

    protected async updateKeycloakUserLanguage(lang: string, rest = this.rest): Promise<void> {
        if(!this.authenticated) {
            console.warn("Tried updating user language, but the user is not authenticated.");
            return;
        }
        if(!rest) {
            console.warn("Tried updating user language, but the REST API is not initialized yet.");
            return;
        }
        await rest.api.UserResource.updateCurrentUserLocale(lang, { headers: { "Content-Type": "application/json" } });
    }

    public logout(redirectUrl?: string) {
        if (!this._authenticated) {
            return;
        }
        this._authenticated = true;

        if (this._keycloak) {
            if (this.isMobile()) {
                this.console.storeData("REFRESH_TOKEN", null);
            }
            if (this._keycloakUpdateTokenInterval) {
                window.clearTimeout(this._keycloakUpdateTokenInterval);
                this._keycloakUpdateTokenInterval = undefined;
            }
            this._keycloak.logout(redirectUrl && redirectUrl !== "" ? {redirectUri: redirectUrl} : undefined);
        } else if (this._basicIdentity) {
            this._basicIdentity = undefined;
            if (redirectUrl) {
                window.location.href = redirectUrl;
            } else {
                window.location.reload();
            }
        }
    }

    public login(options?: LoginOptions) {
        switch (this._config.auth) {
            case Auth.BASIC:
                if (options && options.credentials) {
                    this._config.credentials = Object.assign({}, options.credentials);
                }
                this.doBasicLogin();
                break;
            case Auth.KEYCLOAK:
                if (this._keycloak) {
                    const keycloakOptions: any = {};
                    if (options && options.redirectUrl && options.redirectUrl !== "") {
                        keycloakOptions.redirectUri = options.redirectUrl;
                    }
                    if(options?.action && options.action !== "") {
                        keycloakOptions.action = options.action;
                    }
                    if (this.isMobile()) {
                        keycloakOptions.scope = "offline_access";
                    }
                    this._keycloak.login(keycloakOptions);
                }
                break;
            case Auth.NONE:
                break;
        }
    }

    protected async initialiseBasicAuth(): Promise<boolean> {

        if (!this.config.basicLoginProvider) {
            console.debug("No basicLoginProvider defined on config so cannot display login UI");
            return false;
        }

        if (this.config.autoLogin) {
            // Delay basic login until other inits are done
            this._readyCallback = () => {
                return this.doBasicLogin();
            };
        }

        return true;
    }

    protected async doBasicLogin() {

        if (!this.config.basicLoginProvider) {
            return;
        }

        let result: BasicLoginResult = {
            username: this.config.credentials?.username ? this.config.credentials?.username : "",
            password: this.config.credentials?.password ? this.config.credentials?.password : "",
            cancel: false
        };
        let authenticated = false;

        this._basicIdentity = {
            roles: undefined,
            token: undefined,
            user: undefined
        };

        while (!authenticated) {
            result = await this.config.basicLoginProvider(result.username, result.password);

            if (result.cancel) {
                console.debug("Basic authentication cancelled by user");
                break;
            }

            if (!result.username || !result.password) {
                continue;
            }

            // Update basic token so we can use rest api to make calls
            this._basicIdentity!.token = btoa(result.username + ":" + result.password);
            let success = false;

            try {
                const userResponse = await rest.api.UserResource.getCurrent();
                if (userResponse.status === 200) {
                    success = true;
                    this._basicIdentity!.user = userResponse.data;
                }

                if (!success) {
                    // Undertow incorrectly returns 403 when no authorization header and a 401 when it is set and not valid
                    if (userResponse.status === 401 || userResponse.status === 403) {
                        console.debug("Basic authentication invalid credentials, trying again");
                    }
                }
            } catch (e) {
                console.error("Basic auth failed: ", e);
            }

            if (success) {
                console.debug("Basic authentication successful");
                authenticated = true;

                // Get user roles
                const rolesResponse = await rest.api.UserResource.getCurrentUserClientRoles(this.clientId);
                this._basicIdentity!.roles = rolesResponse.data;
            } else {
                console.debug("Unknown response so aborting");
                this._basicIdentity = undefined;
                break;
            }
        }

        if (authenticated) {
            this._onAuthenticated();
        }
    }

    public isSuperUser(): boolean {
        return !!(this.getRealm() && this.getRealm() === "master" && this.hasRealmRole("admin"));
    }

    public isRestrictedUser(): boolean {
        return !!this.hasRealmRole(RESTRICTED_USER_REALM_ROLE);
    }

    public getApiBaseUrl(): string {
        let baseUrl = this._config.managerUrl!;
        baseUrl += "/api/" + this._config.realm + "/";
        return baseUrl;
    }

    public getAppName(): string {
        const pathArr = location.pathname.split('/');
        return pathArr.length >= 1 ? pathArr[1] : "";
    }

    public hasRealmRole(role: string) {
        return this.isKeycloak() && this._keycloak!.hasRealmRole(role);
    }

    public hasRole(role: string, client: string = this._config.clientId!) {
        const roles = this.roles;
        return roles && roles.has(client) && roles.get(client)!.indexOf(role) >= 0;
    }

    public getAuthorizationHeader(): string | undefined {
        if (this.getKeycloakToken()) {
            return "Bearer " + this.getKeycloakToken();
        }

        if (this.getBasicToken()) {
            return "Basic " + this.getBasicToken();
        }
    }

    public getKeycloakToken(): string | undefined {
        if (this.isKeycloak()) {
            return this._keycloak!.token;
        }
        return undefined;
    }

    public getBasicToken(): string | undefined {
        return this._basicIdentity ? this._basicIdentity.token : undefined;
    }

    public getRealm(): string | undefined {
        if (this._config) {
            return this._config.realm;
        }
        return undefined;
    }

    protected isMobile(): boolean {
        return this.console && this.console.isMobile;
    }

    public isKeycloak(): boolean {
        return !!this._keycloak;
    }

    protected _onAuthenticated() {
        this._authenticated = true;

        // TODO: Move events init logic once websocket supports anonymous connections
        if (!this._events) {
            this.doEventsSubscriptionInit();
        }
    }

    protected async loadAndInitialiseKeycloak(): Promise<boolean> {
        try {
            // Initialise keycloak
            this._keycloak = new Keycloak({
                clientId: this._config.clientId!,
                realm: this._config.realm!,
                url: this._config.keycloakUrl
            });

            // Try to use a stored offline refresh token if defined
            const offlineToken = await this._getNativeOfflineRefreshToken();

            // Cannot inject offlineToken here as this adapter is designed for interactive login and even check-sso
            // does a redirect to keycloak but we can inject the offline token as shown and then update the access token
            let authenticated = await this._keycloak!.init({
                checkLoginIframe: false, // Doesn't work well with offline tokens or periodic token updates
                onLoad: "check-sso"
            });

            if (!authenticated && offlineToken) {
                try {
                    console.error("SETTING OFFLINE TOKEN");
                    this._keycloak.refreshToken = offlineToken;
                    authenticated = await this._updateKeycloakAccessToken();
                } catch (e) {
                    console.error("Failed to authenticate using offline token");
                }
            }

            if (authenticated) {
                this._name = this._keycloak.tokenParsed?.name;
                this._username = this._keycloak.tokenParsed?.preferred_username;

                this._createTokenUpdateInterval();

                // If native shell is enabled store offline token
                if (this.isMobile() && this._keycloak?.refreshTokenParsed?.typ === "Offline") {
                    console.debug("Storing offline refresh token");
                    this.console.storeData("REFRESH_TOKEN", this._keycloak!.refreshToken);
                }
                this._onAuthenticated();
            } else if (this.config.autoLogin) {
                this.login();
                return false;
            }
            return true;
        } catch (error) {
            this._authenticated = false;
            this._setError(ORError.AUTH_FAILED);
            console.error("Failed to initialise Keycloak: " + error);
            return false;
        }
    }

    protected _createTokenUpdateInterval() {
        if (!this._keycloakUpdateTokenInterval) {
            this._keycloakUpdateTokenInterval = window.setInterval(async () => {
                await this._updateKeycloakAccessToken().catch(() => {
                    console.debug("Keycloak failed to refresh the access token");
                    this._onDisconnect();
                });
            }, 10000);
        }
    }

    protected async _updateKeycloakAccessToken(): Promise<boolean> {
        const tokenRefreshed = await this._keycloak!.updateToken(20);
        console.debug("Access token update success, refreshed from server: " + tokenRefreshed);
        if (tokenRefreshed) {
            this._onAuthenticated();
        }
        return tokenRefreshed;
    }

    protected async _getNativeOfflineRefreshToken(): Promise<string | undefined> {
        if (this.isMobile()) {
            return await this.console.retrieveData("REFRESH_TOKEN");
        }
    }

    protected _emitEvent(event: OREvent) {
        window.setTimeout(() => {
            const listeners = this._listeners;
            for (const listener of listeners) {
                listener(event);
            }
        }, 0);
    }

    protected _setError(error: ORError) {
        this._error = error;
        this._emitEvent(OREvent.ERROR);
        console.warn("Error set: " + error);
    }

    /** Function that clears the `WebView` history of a console. It will not delete the history on regular browsers. */
    protected _clearWebHistory(): void {
        this.console?._doSendGenericMessage("CLEAR_WEB_HISTORY", undefined);
    }

    /**
     * Disconnect can occur when events status changes to offline and/or keycloak token refresh fails
     * we just try to reach keycloak and then get a new token as well as wait for the events status to change
     */
    protected async _onDisconnect() {
        if (this._disconnected) {
            return;
        }
        console.debug("Disconnected");
        this._disconnected = true;

        // Cancel token refresh timer
        if (this._keycloakUpdateTokenInterval) {
            window.clearTimeout(this._keycloakUpdateTokenInterval);
            this._keycloakUpdateTokenInterval = undefined;
        }

        this._emitEvent(OREvent.OFFLINE);
        this.reconnect();
    }

    protected _onReconnected() {
        console.debug("Reconnected");
        this._disconnected = false;

        // Reinstate token update interval
        this._createTokenUpdateInterval();
        this._emitEvent(OREvent.ONLINE);
    }

    /**
     * Checks keycloak is available and token is valid otherwise will redirect to login; also checks if event bus is
     * online.
     */
    public async reconnect(reattemptDelayMillis: number = 3000) {

        if (!this._disconnected) {
            return;
        }

        if (this._reconnectTimer) {
            window.clearTimeout(this._reconnectTimer);
            this._reconnectTimer = undefined;
        }

        const tryReconnect = async () => {
            console.debug("Attempting reconnect");
            let keycloakOffline = !await this.isKeycloakReachable();

            if (keycloakOffline) {
                console.debug("Keycloak is unreachable");
                return false;
            }
            console.debug("Keycloak is reachable");

            // Check if access token can be refreshed
            console.debug("Checking keycloak access token");
            try {
                await this._updateKeycloakAccessToken();
            } catch (e) {
                // Try and use offline token if it is available
                const offlineToken = await this._getNativeOfflineRefreshToken();
                this._keycloak!.refreshToken = offlineToken;

                try {
                    await this._updateKeycloakAccessToken();
                } catch (e) {
                    console.debug("Cannot update access token so sending to login");
                    this.login();
                    return;
                }
                console.debug("Keycloak access token is valid");
                return true;
            }

            // Check events
            const eventsOffline = this.events && this.events.status === EventProviderStatus.CONNECTING;
            console.debug("If event provider offline then attempting reconnect: offline=" + eventsOffline);
            // Force reconnect attempt now if needed
            return !eventsOffline || await this.events?.connect();
        };

        const connected = await tryReconnect();

        if (connected === undefined) {
            // Going back to keycloak login so nothing to do
            return;
        }

        if (!connected) {
            // Schedule reconnect again
            reattemptDelayMillis = Math.min(Manager.MAX_RECONNECT_DELAY, reattemptDelayMillis + 3000);
            console.debug("Scheduling another reconnect attempt in (ms): " + reattemptDelayMillis);
            this._reconnectTimer = window.setTimeout(() => this.reconnect(reattemptDelayMillis), reattemptDelayMillis);
            return;
        }

        this._onReconnected();
    }

    // Checks whether keycloak is reachable using a simple HTTP HEAD request since the keycloak JS adapter doesn't give
    // us details of the HTTP responses they get, we test it manually using this.
    protected async isKeycloakReachable(timeoutMillis: number = 2000) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), timeoutMillis);
        try {
            // Get the token URL from keycloak as this must already have CORS configured for keycloak to work
            // Make an OPTIONS request as GET/HEAD requests are not allowed on the token endpoint
            // Note if keycloak service is unavailable the proxy may respond with 503 and no CORS headers
            // but this is handled by the catch block
            // @ts-ignore
            const tokenUrl = this._keycloak.endpoints.token();
            const result = await fetch(tokenUrl, {method: 'OPTIONS', signal: controller.signal});
            return result.status === 200;
        } catch (e) {
            return false;
        } finally {
            clearTimeout(timeout);
        }
    }
}

export const manager = new Manager(); // Needed for webpack bundling
export default manager;
