import "url-search-params-polyfill";
import {Console} from "./console";
import rest from "@openremote/rest";
import {AxiosRequestConfig} from "axios";
import {EventProvider, EventProviderFactory, EventProviderStatus, WebSocketEventProvider} from "./event";
import i18next from "i18next";
import i18nextXhr from "i18next-xhr-backend";
import sprintf from 'i18next-sprintf-postprocessor';
import moment from "moment";
import {
    AssetDescriptor,
    AttributeDescriptor,
    AttributeValueDescriptor,
    MetaItemDescriptor,
    User,
    Role
} from "@openremote/model";
import * as Util from "./util";
import orIconSet from "./or-icon-set";

// Re-exports
export {Util};
export * from "./asset-mixin";
export * from "./console";
export * from "./event";
export * from "./defaults";

export const DEFAULT_ICONSET: string = "mdi";

export declare type KeycloakPromise<T> = {
    success<TResult1 = T, TResult2 = never>(onfulfilled?: ((value: T) => TResult1 | KeycloakPromise<TResult1>) | undefined | null, onrejected?: ((reason: any) => TResult2 | KeycloakPromise<TResult2>) | undefined | null): KeycloakPromise<TResult1 | TResult2>;
    error<TResult = never>(onrejected?: ((reason: any) => TResult | KeycloakPromise<TResult>) | undefined | null): Promise<T | TResult>;
}

export declare type Keycloak = {
    token: string;
    refreshToken: string;
    tokenParsed: any;
    refreshTokenParsed: any;
    resourceAccess: any;
    onAuthSuccess: () => void;
    onAuthError: () => void;
    init(options?: any): KeycloakPromise<boolean>;
    login(options?: any): void;
    hasRealmRole(role: string): boolean;
    logout(options?: any): void;
    updateToken(expiry: number): KeycloakPromise<boolean>;
    clearToken(): void;
}

export enum ORError {
    MANAGER_FAILED_TO_LOAD = "MANAGER_FAILED_TO_LOAD",
    AUTH_FAILED = "AUTH_FAILED",
    AUTH_TYPE_UNSUPPORTED = "AUTH_TYPE_UNSUPPORTED",
    CONSOLE_ERROR = "CONSOLE_INIT_ERROR",
    EVENTS_CONNECTION_ERROR = "EVENTS_CONNECTION_ERROR",
    TRANSLATION_ERROR = "TRANSLATION_ERROR"
}

export enum Auth {
    KEYCLOAK = "KEYCLOAK",
    BASIC = "BASIC",
    NONE = "NONE"
}

export enum OREvent {
    ERROR = "ERROR",
    READY = "READY",
    CONSOLE_INIT = "CONSOLE_INIT",
    CONSOLE_READY = "CONSOLE_READY",
    EVENTS_CONNECTED = "EVENTS_CONNECTED",
    EVENTS_CONNECTING = "EVENTS_CONNECTING",
    EVENTS_DISCONNECTED = "EVENTS_DISCONNECTED",
    TRANSLATE_INIT = "TRANSLATE_INIT",
    TRANSLATE_LANGUAGE_CHANGED = "TRANSLATE_LANGUAGE_CHANGED",
    DISPLAY_REALM_CHANGED = "DISPLAY_REALM_CHANGED"
}

export enum EventProviderType {
    WEBSOCKET = "WEBSOCKET",
    POLLING = "POLLING"
}

export interface Credentials {
    username: string;
    password: string;
}

export interface LoginOptions {
    redirectUrl?: string;
    credentials?: Credentials;
}

export interface BasicLoginResult {
    username: string;
    password: string;
    cancel: boolean;
    closeCallback: undefined | ((authenticated: boolean) => void);
}

export enum MapType {
    VECTOR = "VECTOR",
    RASTER = "RASTER"
}

export interface ManagerConfig {
    managerUrl: string;
    keycloakUrl?: string;
    appVersion?: string;
    auth?: Auth;
    realm: string;
    clientId?: string;
    autoLogin?: boolean;
    credentials?: Credentials;
    consoleAutoEnable?: boolean;
    eventProviderType?: EventProviderType;
    pollingIntervalMillis?: number;
    loadIcons?: boolean;
    loadDescriptors?: boolean;
    mapType?: MapType;
    loadTranslations?: string[];
    translationsLoadPath?: string;
    configureTranslationsOptions?: (i18next: i18next.InitOptions) => void;
    basicLoginProvider?: (username: string | undefined, password: string | undefined) => PromiseLike<BasicLoginResult>;
}

export class IconSetAddedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-iconset-added";

    constructor() {
        super(IconSetAddedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export interface OrManagerEventDetail {
    event: OREvent;
    error?: ORError;
}

declare global {
    export interface HTMLElementEventMap {
        [IconSetAddedEvent.NAME]: IconSetAddedEvent;
    }
}

export interface IconSetSvg {
    size: number;
    icons: {[name: string]: string};
}

export class ORIconSets {
    private _icons: {[name: string]: IconSetSvg} = {};

    addIconSet(name: string, iconset: IconSetSvg) {
        this._icons[name] = iconset;
        window.dispatchEvent(new IconSetAddedEvent());
    }

    getIconSet(name: string) {
        return this._icons[name];
    }

    getIcon(icon: string | undefined): Element | undefined {
        if (!icon) {
            return undefined;
        }

        let parts = (icon || "").split(":");
        let iconName = parts.pop();
        let iconSetName = parts.pop() || DEFAULT_ICONSET;
        if (!iconSetName || iconSetName === "" || !iconName || iconName === "") {
            return;
        }

        let iconSet = IconSets.getIconSet(iconSetName);
        //iconName = iconName.replace(/-([a-z])/g, function (g) { return g[1].toUpperCase(); });

        if (!iconSet || !iconSet.icons.hasOwnProperty(iconName)) {
            return;
        }

        const iconData = iconSet.icons[iconName];
        const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("viewBox", "0 0 " + iconSet.size + " " + iconSet.size);
        svg.style.cssText = "pointer-events: none; display: block; width: 100%; height: 100%;";
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
        svg.setAttribute("focusable", "false");
        if (iconData.startsWith("<")) {
            svg.innerHTML = iconData;
        } else {
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", iconData);
            path.style.pointerEvents = "pointer-events: var(--or-icon-pointer-events, none);";
            svg.appendChild(path);
        }
        return svg;
    }
}

export class AssetModelUtil {

    public static _assetDescriptors: AssetDescriptor[] = [];
    public static _attributeDescriptors: AttributeDescriptor[] = [];
    public static _attributeValueDescriptors: AttributeValueDescriptor[] = [];
    public static _metaItemDescriptors: MetaItemDescriptor[] = [];

    public static getAssetDescriptors(): AssetDescriptor[] {
        return [...this._assetDescriptors];
    }

    public static getAttributeDescriptors(): AttributeDescriptor[] {
        return [...this._attributeDescriptors];
    }

    public static getAttributeValueDescriptors(): AttributeValueDescriptor[] {
        return [...this._attributeValueDescriptors];
    }

    public static getMetaItemDescriptors(): MetaItemDescriptor[] {
        return [...this._metaItemDescriptors];
    }

    public static getAssetDescriptor(type?: string): AssetDescriptor | undefined {
        if (!type) {
            return;
        }

        return this._assetDescriptors.find((assetDescriptor) => {
            return assetDescriptor.type === type;
        });
    }

    public static getAssetAttributeDescriptor(assetDescriptor?: AssetDescriptor, attributeName?: string): AttributeDescriptor | undefined {
        if (!attributeName || !assetDescriptor || !assetDescriptor.attributeDescriptors) {
            return;
        }

        return assetDescriptor.attributeDescriptors.find((attributeDescriptor) => attributeDescriptor.attributeName === attributeName);
    }

    public static getAttributeDescriptor(attributeName?: string): AttributeDescriptor | undefined {
        if (!attributeName) {
            return;
        }

        return this._attributeDescriptors.find((attributeDescriptor) => {
            return attributeDescriptor.attributeName === attributeName;
        });
    }

    public static getAttributeDescriptorFromAsset(attributeName: string, assetType?: string): AttributeDescriptor | undefined {
        if (!attributeName) {
            return;
        }

        if (assetType) {
            const assetDescriptor = this.getAssetDescriptor(assetType);
            if (assetDescriptor && assetDescriptor.attributeDescriptors) {
                const attributeDescriptor = assetDescriptor.attributeDescriptors.find((ad) => ad.attributeName === attributeName);
                if (attributeDescriptor) {
                    return attributeDescriptor;
                }
            }
        }

        return this.getAttributeDescriptor(attributeName);
    }

    public static getAttributeValueDescriptor(name: string): AttributeValueDescriptor | undefined {
        return this._attributeValueDescriptors.find((attributeValueDescriptor) => {
            return attributeValueDescriptor.name === name;
        });
    }

    public static getAttributeValueDescriptorFromAsset(name: string | undefined, assetType?: string, attributeName?: string): AttributeValueDescriptor | undefined {
        if (!name) {
            return;
        }

        if (attributeName) {
            const attributeDescriptor = this.getAttributeDescriptorFromAsset(attributeName, assetType);
            if (attributeDescriptor) {
                return attributeDescriptor.valueDescriptor;
            }
        }

        return this.getAttributeValueDescriptor(name);
    }

    public static getMetaItemDescriptor(urn?: string): MetaItemDescriptor | undefined {
        if (!urn) {
            return;
        }

        return this._metaItemDescriptors.find((metaItemDescriptor) => {
            return metaItemDescriptor.urn === urn;
        });
    }

    public static attributeValueDescriptorsMatch(attributeValueDescriptor1: AttributeValueDescriptor, attributeValueDescriptor2: AttributeValueDescriptor) {
        if (attributeValueDescriptor1 === attributeValueDescriptor2) {
            return true;
        }
        if (!attributeValueDescriptor1 || !attributeValueDescriptor2) {
            return false;
        }
        return attributeValueDescriptor1.name === attributeValueDescriptor2.name && attributeValueDescriptor1.valueType === attributeValueDescriptor2.valueType;
    }

    public static getMetaInitialValueFromMetaDescriptors(metaItemUrn: MetaItemDescriptor | string, metaItemDescriptors: MetaItemDescriptor[] | undefined): any | undefined {
        if (!metaItemDescriptors) {
            return;
        }

        const matchUrn = typeof metaItemUrn === "string" ? metaItemUrn : metaItemUrn.urn;
        const metaItemDescriptor = metaItemDescriptors && metaItemDescriptors.find((mid) => mid && mid.urn === matchUrn);
        if (metaItemDescriptor) {
            return metaItemDescriptor.initialValue;
        }
    }

    public static getMetaItemDescriptorInitialValue(metaItemDescriptor: MetaItemDescriptor, initialValue: any) {
        if (metaItemDescriptor.valueFixed) {
            return metaItemDescriptor;
        }

        const newMetaItem = JSON.parse(JSON.stringify(metaItemDescriptor)) as MetaItemDescriptor;
        newMetaItem.initialValue = initialValue;
        return newMetaItem;
    }

    public static getAssetDescriptorColor(descriptor: AssetDescriptor | undefined, fallbackColor?: string): string | undefined {
        return descriptor && descriptor.color ? descriptor.color : fallbackColor;
    }

    public static getAssetDescriptorIcon(descriptor: AssetDescriptor | undefined, fallbackIcon?: string): string | undefined {
        return descriptor && descriptor.icon ? descriptor.icon : fallbackIcon;
    }

    public static hasMetaItem(descriptor: AttributeDescriptor | undefined, name: string): boolean {
        if (!descriptor || !descriptor.metaItemDescriptors) {
            return false;
        }

        return !!descriptor.metaItemDescriptors.find((mid) => mid.urn === name);
    }
}

export type EventCallback = (event: OREvent) => any;

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

    get roles(): string[] {
        if (this._keycloak) {
            if (this._keycloak.resourceAccess) {
                let roles: string[];
                if (this._config.clientId && this._keycloak!.resourceAccess.hasOwnProperty(this._config.clientId)) {
                    roles = this._keycloak!.resourceAccess[this._config.clientId].roles;
                } else {
                    roles = this._keycloak!.resourceAccess.account.roles;
                }
                return roles || [];
            }
        } else if (this._basicIdentity && this._basicIdentity.roles) {
            return this._basicIdentity.roles.map((r) => r.name!);
        }

        return [];
    }

    get managerVersion() {
        return this._managerVersion;
    }

    get isManagerAvailable() {
        return this._managerVersion && this._managerVersion !== "";
    }

    get managerUrl() {
        return this._config.managerUrl;
    }

    get keycloakUrl() {
        return this._config.keycloakUrl;
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
        i18next.changeLanguage(lang);
        this.console.storeData("LANGUAGE", lang);
    }

    get displayRealm() {
        return this._displayRealm || this._config.realm;
    }

    set displayRealm(realm: string) {
        if (!this.isSuperUser()) {
            return;
        }
        this._displayRealm = realm;
        this._emitEvent(OREvent.DISPLAY_REALM_CHANGED);
    }

    getEventProvider(): EventProvider | undefined {
        return this.events;
    }

    get mapType() {
        return this._config.mapType || MapType.VECTOR;
    }

    protected static normaliseConfig(config: ManagerConfig): ManagerConfig {
        const normalisedConfig: ManagerConfig = Object.assign({}, config);

        if (!normalisedConfig.managerUrl || normalisedConfig.managerUrl === "") {
            // Assume manager is running on same host as this code
            normalisedConfig.managerUrl = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ":" + window.location.port : "");
        } else {
            // Normalise by stripping any trailing slashes
            normalisedConfig.managerUrl = normalisedConfig.managerUrl.replace(/\/+$/, "");
        }

        if (!normalisedConfig.realm || normalisedConfig.realm === "") {
            // Assume master realm
            normalisedConfig.realm = "master";
        }

        if (normalisedConfig.auth === Auth.KEYCLOAK) {
            // Determine URL of keycloak server
            if (!normalisedConfig.keycloakUrl || normalisedConfig.keycloakUrl === "") {
                // Assume keycloak is running on same host as the manager
                normalisedConfig.keycloakUrl = normalisedConfig.managerUrl + "/auth";
            } else {
                // Normalise by stripping any trailing slashes
                normalisedConfig.keycloakUrl = normalisedConfig.keycloakUrl.replace(/\/+$/, "");
            }
        }

        if (normalisedConfig.consoleAutoEnable === undefined) {
            normalisedConfig.consoleAutoEnable = true;
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
            normalisedConfig.clientId = "openremote";
        }
        
        return normalisedConfig;
    }

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
        roles: Role[] | undefined
    };
    private _keycloakUpdateTokenInterval?: number = undefined;
    private _managerVersion: string = "";
    private _listeners: EventCallback[] = [];
    private _console!: Console;
    private _events?: EventProvider;
    private _displayRealm?: string = "";

    public isManagerSameOrigin(): boolean {
        if (!this.ready) {
            return false;
        }

        const managerUrl = new URL(this._config.managerUrl);
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
            console.log("Already initialised");
        }

        this._config = Manager.normaliseConfig(config);

        let success = await this.doAuthInit();

        if (success) {
            success = await this.doInit();
        }

        if (success) {
            success = this.doRestApiInit();
        }

        success = await this.doConsoleInit() && success;

        success = await this.doTranslateInit() && success;

        if (success) {
            success = await this.doDescriptorsInit();
        }

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
        }

        this._displayRealm = config.realm;

        return success;
    }

    protected async doInit(): Promise<boolean> {
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

            // Load material design and OR icon sets if requested
            if (this._config.loadIcons) {
                const response = await fetch(manager.config.managerUrl + "/shared/mdi-icons.json");
                const mdiIconSet = await response.json();
                IconSets.addIconSet("mdi", mdiIconSet);
                IconSets.addIconSet("or", orIconSet);
            }

            return true;
        } catch (e) {
            // TODO: Implement auto retry?
            console.error("Failed to contact the manager", e);
            this._setError(ORError.MANAGER_FAILED_TO_LOAD);
            return false;
        }
    }

    protected async doTranslateInit(): Promise<boolean> {

        i18next.on("initialized", (options) => {
            this._emitEvent(OREvent.TRANSLATE_INIT);
        });

        i18next.on("languageChanged", (lng) => {
            moment.locale(lng);
            this._emitEvent(OREvent.TRANSLATE_LANGUAGE_CHANGED);
        });

        // Look for language preference in local storage
        const language = !this.console ? undefined : await this.console.retrieveData("LANGUAGE");
        const initOptions: i18next.InitOptions = {
            lng: language,
            fallbackLng: "en",
            defaultNS: "app",
            fallbackNS: "or",
            ns: this.config.loadTranslations,
            interpolation: {
                format: function(value, format, lng) {
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
            await i18next.use(sprintf).use(i18nextXhr).init(initOptions);
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
            const assetDescriptorResponse = await rest.api.AssetModelResource.getAssetDescriptors();
            const attributeDescriptorResponse = await rest.api.AssetModelResource.getAttributeDescriptors();
            const attributeValueDescriptorResponse = await rest.api.AssetModelResource.getAttributeValueDescriptors();
            const metaItemDescriptorResponse = await rest.api.AssetModelResource.getMetaItemDescriptors();

            AssetModelUtil._assetDescriptors = assetDescriptorResponse.data;
            AssetModelUtil._attributeDescriptors = attributeDescriptorResponse.data;
            AssetModelUtil._attributeValueDescriptors = attributeValueDescriptorResponse.data;
            AssetModelUtil._metaItemDescriptors = metaItemDescriptorResponse.data;
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

                if (!success) {
                    // Try fallback to BASIC
                    console.log("Falling back to basic auth");
                    this._config.auth = Auth.BASIC;
                    return this.doAuthInit();
                }
                break;
            case Auth.NONE:
                // Nothing for us to do here
                return true;
            default:
                this._setError(ORError.AUTH_TYPE_UNSUPPORTED);
                return false;
        }

        // Add interceptor to inject authorization header on each request
        rest.addRequestInterceptor(
            (config: AxiosRequestConfig) => {
                if (!config.headers.Authorization) {
                    const authHeader = this.getAuthorizationHeader();

                    if (authHeader) {
                        config.headers.Authorization = authHeader;
                    }
                }

                return config;
            }
        );
        return success;
    }

    protected doRestApiInit(): boolean {
        rest.setTimeout(10000);
        rest.initialise(this.getApiBaseUrl());
        return true;
    }

    protected async doEventsSubscriptionInit(): Promise<boolean> {
        let connected = false;

        switch (this._config.eventProviderType) {
            case EventProviderType.WEBSOCKET:
                this._events = new WebSocketEventProvider(this._config.managerUrl);
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

    protected _onEventProviderStatusChanged(status: EventProviderStatus) {
        switch (status) {
            case EventProviderStatus.DISCONNECTED:
                this._emitEvent(OREvent.EVENTS_DISCONNECTED);
                break;
            case EventProviderStatus.CONNECTED:
                this._emitEvent(OREvent.EVENTS_CONNECTED);
                break;
            case EventProviderStatus.CONNECTING:
                this._emitEvent(OREvent.EVENTS_CONNECTING);
                break;
        }
    }

    protected async doConsoleInit(): Promise<boolean> {
        try {
            let orConsole = new Console(this._config.realm, this._config.consoleAutoEnable!, () => {
                this._emitEvent(OREvent.CONSOLE_READY);
            });

            this._console = orConsole;

            await orConsole.initialise();
            this._emitEvent(OREvent.CONSOLE_INIT);
            return true;
        } catch (e) {
            this._setError(ORError.CONSOLE_ERROR);
            return false;
        }
    }

    public logout(redirectUrl?: string) {
        if (this._keycloak) {
            if (this.console.isMobile) {
                this.console.storeData("REFRESH_TOKEN", null);
            }
            const options = redirectUrl && redirectUrl !== "" ? {redirectUri: redirectUrl} : null;
            this._keycloak.logout(options);
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
            console.log("No basicLoginProvider defined on config so cannot display login UI");
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
            username: this._config.credentials ? this._config.credentials.username : "",
            password: this._config.credentials ? this._config.credentials.password : "",
            cancel: false,
            closeCallback: undefined
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
                console.log("Basic authentication cancelled by user");
                if (result.closeCallback) {
                    result.closeCallback(false);
                }
                break;
            }

            if (!result.username || !result.password) {
                continue;
            }

            // Update basic token so we can use rest api to make calls
            this._basicIdentity!.token = btoa(result.username + ":" + result.password);
            const userResponse = await rest.api.UserResource.getCurrent();
            const status = userResponse.status;

            if (status === 200) {
                console.log("Basic authentication successful");
                authenticated = true;
                this._basicIdentity!.user = userResponse.data;

                // Get user roles
                const rolesResponse = await rest.api.UserResource.getCurrentUserRoles();
                this._basicIdentity!.roles = rolesResponse.data;

                // Undertow incorrectly returns 403 when no authorization header and a 401 when it is set and not valid
            } else if (status === 401 || status === 403) {
                console.log("Basic authentication invalid credentials, trying again");
                this._basicIdentity = undefined;
            } else {
                console.log("Unkown response so aborting");
                this._basicIdentity = undefined;
                break;
            }
        }

        this._setAuthenticated(authenticated);

        if (result.closeCallback) {
            result.closeCallback(authenticated);
        }
    }

    public isSuperUser() {
        return this.getRealm() && this.getRealm() === "master" && this.hasRealmRole("admin");
    }

    public getApiBaseUrl() {
        let baseUrl = this._config.managerUrl;
        baseUrl += "/api/" + this._config.realm + "/";
        return baseUrl;
    }

    public getAppName(): string {
        let pathArr = location.pathname.split('/');
        return pathArr.length >= 1 ? pathArr[1] : "";
    }

    public hasRealmRole(role: string) {
        return this._keycloak && this._keycloak.hasRealmRole(role);
    }

    public hasRole(role: string) {
        return this.roles && this.roles.indexOf(role) >= 0;
    }

    public getAuthorizationHeader(): string | undefined {
        if (this._keycloak) {
            return "Bearer " + this._keycloak.token;
        }

        if (this.getBasicToken()) {
            return "Basic " + this.getBasicToken();
        }

        return undefined;
    }

    public getKeycloakToken(): string | undefined {
        if (this._keycloak) {
            return this._keycloak.token;
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
        // If native shell is enabled, we need an offline refresh token
        if (this.console && this.console.isMobile && this.config.auth === Auth.KEYCLOAK) {

            if (this._keycloak && this._keycloak.refreshTokenParsed.typ === "Offline") {
                console.debug("Storing offline refresh token");
                this.console.storeData("REFRESH_TOKEN", this._keycloak!.refreshToken);
            } else {
                this.login();
            }
        }
    }

    // NOTE: The below works with Keycloak 2.x JS API - They made breaking changes in newer versions
    // so this will need updating.
    protected async loadAndInitialiseKeycloak(): Promise<boolean> {

        try {

            // There's a bug in some Keycloak versions which means the init promise doesn't resolve
            // so putting a check in place; wrap keycloak promise in proper ES6 promise
            let keycloakPromise: any = null;

            // Load the keycloak JS API
            await Util.loadJs(this._config.keycloakUrl + "/js/keycloak.js");

            // Should have Keycloak global var now
            if (!(window as any).Keycloak) {
                console.log("Keycloak global variable not found probably failed to load keycloak or manager doesn't support it");
                return false;
            }

            // Initialise keycloak
            this._keycloak = (window as any).Keycloak({
                clientId: this._config.clientId,
                realm: this._config.realm,
                url: this._config.keycloakUrl
            });

            this._keycloak!.onAuthSuccess = () => {
                if (keycloakPromise) {
                    keycloakPromise(true);
                }
            };

            this._keycloak!.onAuthError = () => {
                this._setAuthenticated(false);
            };

            try {
                // Try to use a stored offline refresh token if defined
                const offlineToken = await this._getNativeOfflineRefreshToken();

                const authenticated = await new Promise<boolean>(((resolve, reject) => {
                    keycloakPromise = resolve;
                    this._keycloak!.init({
                        checkLoginIframe: false, // Doesn't work well with offline tokens or periodic token updates
                        onLoad: this._config.autoLogin ? "login-required" : "check-sso",
                        refreshToken: offlineToken
                    }).success((auth: boolean) => {
                        resolve(auth);
                    }).error(() => {
                        reject();
                    });
                }));

                keycloakPromise = null;

                if (authenticated) {

                    this._name = this._keycloak!.tokenParsed.name;
                    this._username = this._keycloak!.tokenParsed.preferred_username;

                    // Update the access token every 10s (note keycloak will only update if expiring within configured
                    // time period.
                    if (this._keycloakUpdateTokenInterval) {
                        clearInterval(this._keycloakUpdateTokenInterval);
                        delete this._keycloakUpdateTokenInterval;
                    }
                    this._keycloakUpdateTokenInterval = window.setInterval(() => {
                        this.updateKeycloakAccessToken();
                    }, 10000);
                    this._onAuthenticated();
                }
                this._setAuthenticated(authenticated);
                return true;
            } catch (e) {
                console.error(e);
                keycloakPromise = null;
                this._setAuthenticated(false);
                return false;
            }
        } catch (error) {
            this._setError(ORError.AUTH_FAILED);
            console.error("Failed to load Keycloak");
            return false;
        }
    }

    protected updateKeycloakAccessToken(): Promise<boolean> {
        // Access token must be good for X more seconds, should be half of Constants.ACCESS_TOKEN_LIFESPAN_SECONDS
        return new Promise<boolean>(() => {
            this._keycloak!.updateToken(30)
                .success((tokenRefreshed: boolean) => {
                    // If refreshed from server, it means the refresh token was still good for another access token
                    console.debug("Access token update success, refreshed from server: " + tokenRefreshed);
                    return tokenRefreshed;
                })
                .error(() => {
                    // Refresh token expired (either SSO max session duration or offline idle timeout), see
                    // IDENTITY_SESSION_MAX_MINUTES and IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES server config
                    console.info("Access token update failed, refresh token expired, login required");
                    this._keycloak!.clearToken();
                    this._keycloak!.login();
                });
        });
    }

    protected async _getNativeOfflineRefreshToken(): Promise<string | undefined> {
        if (this.console && this.console.isMobile) {
            return await this.console.retrieveData("REFRESH_TOKEN");
        }
    }

    protected _emitEvent(event: OREvent) {
        window.setTimeout(() => {
            let listeners = this._listeners.slice();
            for (const listener of listeners) {
                listener(event);
            }
        }, 0);
    }

    protected _setError(error: ORError) {
        this._error = error;
        this._emitEvent(OREvent.ERROR);
    }

    // TODO: Remove events logic once websocket supports anonymous connections
    protected _setAuthenticated(authenticated: boolean) {
        this._authenticated = authenticated;
        if (authenticated) {
            if (!this._events) {
                this.doEventsSubscriptionInit();
            }
        } else {
            if (this._events) {
                this._events.disconnect();
            }
            this._events = undefined;
        }
    }
}

export const manager = new Manager(); // Needed for webpack bundling
export const IconSets = new ORIconSets();
export default manager;
