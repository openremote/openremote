import "url-search-params-polyfill";
import {Console} from "./console";
import rest from "@openremote/rest";
import {AxiosRequestConfig} from "axios";
import {EventProvider, EventProviderFactory, EventProviderStatus, WebSocketEventProvider} from "./event";
import i18next, { InitOptions } from "i18next";
import i18nextBackend from "i18next-http-backend";
import moment from "moment";
import {
    AgentDescriptor,
    AssetDescriptor,
    AssetTypeInfo,
    Attribute,
    AttributeDescriptor,
    ConsoleAppConfig,
    MetaItemDescriptor,
    Role,
    User,
    ValueDescriptor,
    ValueDescriptorHolder,
    ValueHolder,
    WellknownAssets,
    WellknownValueTypes
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
    init(options?: any): PromiseLike<boolean>;
    login(options?: any): void;
    hasRealmRole(role: string): boolean;
    logout(options?: any): void;
    updateToken(expiry: number): PromiseLike<boolean>;
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
}

export enum MapType {
    VECTOR = "VECTOR",
    RASTER = "RASTER"
}

export interface ManagerConfig {
    managerUrl?: string;
    keycloakUrl?: string;
    appVersion?: string;
    auth?: Auth;
    realm?: string;
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
    configureTranslationsOptions?: (i18next: InitOptions) => void;
    skipFallbackToBasicAuth?: boolean;
    basicLoginProvider?: (username: string | undefined, password: string | undefined) => PromiseLike<BasicLoginResult>;
}

export function normaliseConfig(config: ManagerConfig): ManagerConfig {
    const normalisedConfig: ManagerConfig = config ? Object.assign({}, config) : {};

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

        const parts = (icon || "").split(":");
        const iconName = parts.pop();
        const iconSetName = parts.pop() || DEFAULT_ICONSET;
        if (!iconSetName || iconSetName === "" || !iconName || iconName === "") {
            return;
        }

        const iconSet = IconSets.getIconSet(iconSetName);
        // iconName = iconName.replace(/-([a-z])/g, function (g) { return g[1].toUpperCase(); });

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

    static _assetTypeInfos: AssetTypeInfo[] = [];
    static _metaItemDescriptors: MetaItemDescriptor[] = [];
    static _valueDescriptors: ValueDescriptor[] = [];

    public static getAssetDescriptors(): AssetDescriptor[] {
        return AssetModelUtil._assetTypeInfos.map(info => info.assetDescriptor as AgentDescriptor);
    }

    public static getMetaItemDescriptors(): MetaItemDescriptor[] {
        return [...this._metaItemDescriptors];
    }

    public static getValueDescriptors(): ValueDescriptor[] {
        return [...this._valueDescriptors];
    }

    public static getAssetTypeInfos(): AssetTypeInfo[] {
        return [...this._assetTypeInfos];
    }

    public static getAssetTypeInfo(type: string | AssetDescriptor | AssetTypeInfo): AssetTypeInfo | undefined {
        if (!type) {
            return;
        }

        if ((type as AssetTypeInfo).assetDescriptor) {
            return type as AssetTypeInfo;
        }

        if (typeof(type) !== "string") {
            type = (type as AssetDescriptor).name!;
        }

        return this._assetTypeInfos.find((assetTypeInfo) => {
            return assetTypeInfo.assetDescriptor!.name === type;
        });
    }

    public static getAssetDescriptor(type?: string | AssetDescriptor | AssetTypeInfo): AssetDescriptor | undefined {
        if (!type) {
            return;
        }

        if ((type as AssetTypeInfo).assetDescriptor) {
            return (type as AssetTypeInfo).assetDescriptor;
        }

        if (typeof(type) !== "string") {
            return type as AssetDescriptor;
        }

        const match = this._assetTypeInfos.find((assetTypeInfo) => {
            return assetTypeInfo.assetDescriptor!.name === type;
        });
        return match ? match.assetDescriptor : undefined;
    }

    public static getAttributeDescriptor(attributeName: string, assetTypeOrDescriptor: string | AssetDescriptor | AssetTypeInfo): AttributeDescriptor | undefined {
        if (!attributeName) {
            return;
        }

        const assetTypeInfo = this.getAssetTypeInfo(assetTypeOrDescriptor || WellknownAssets.UNKNOWNASSET);

        if (!assetTypeInfo || !assetTypeInfo.attributeDescriptors) {
            return;
        }

        return assetTypeInfo.attributeDescriptors.find((attributeDescriptor) => attributeDescriptor.name === attributeName);
    }

    public static getValueDescriptor(name?: string): ValueDescriptor | undefined {
        if (!name) {
            return;
        }

        // If name ends with [] then it's an array value type so lookup the base type and then convert to array
        let arrayDimensions: number | undefined;

        if (name.endsWith("[]")) {
            arrayDimensions = 0;
            while(name.endsWith("[]")) {
                name = name.substring(0, name.length - 2);
                arrayDimensions++;
            }
        }

        // Value descriptor names are globally unique
        let valueDescriptor = this._valueDescriptors.find((valueDescriptor) => valueDescriptor.name === name);
        if (valueDescriptor && arrayDimensions) {
            valueDescriptor = {...valueDescriptor, arrayDimensions: arrayDimensions};
        }
        return valueDescriptor;
    }

    public static resolveValueDescriptor(valueHolder: ValueHolder<any> | undefined, descriptorOrValueType: ValueDescriptorHolder | ValueDescriptor | string | undefined): ValueDescriptor | undefined {
        let valueDescriptor: ValueDescriptor | undefined;

        if (descriptorOrValueType) {
            if (typeof(descriptorOrValueType) === "string") {
                valueDescriptor = AssetModelUtil.getValueDescriptor(descriptorOrValueType);
            }
            if ((descriptorOrValueType as ValueDescriptor).jsonType) {
                valueDescriptor = descriptorOrValueType as ValueDescriptor;
            } else {
                // Must be a value descriptor holder or value holder
                valueDescriptor = AssetModelUtil.getValueDescriptor((descriptorOrValueType as ValueDescriptorHolder).type);
            }
        }

        if (!valueDescriptor && valueHolder) {
            // Try and determine the value descriptor based on the value type
            valueDescriptor = this.resolveValueDescriptorFromValue(valueHolder.value);
        }

        return valueDescriptor;
    }

    public static resolveValueTypeFromValue(value: any): string {
        if (value === null || value === undefined) {
            return WellknownValueTypes.JSON;
        }

        if (typeof value === "number") {
            return WellknownValueTypes.NUMBER;
        }
        if (typeof value === "string") {
            return WellknownValueTypes.TEXT;
        }
        if (typeof value === "boolean") {
            return WellknownValueTypes.BOOLEAN;
        }
        if (Array.isArray(value)) {
            let dimensions = 1;
            let v = (value as any[]).find(v => v !== undefined && v !== null);

            while (Array.isArray(v)) {
                v = (v as any[]).find(v => v !== undefined && v !== null);
                dimensions++;
            }

            let valueType = this.resolveValueTypeFromValue(v);

            while (dimensions > 0) {
                valueType += "[]";
                dimensions--;
            }

            return valueType;
        }
        if (value instanceof Date) {
            return WellknownValueTypes.DATEANDTIME;
        } else {
            return WellknownValueTypes.JSONOBJECT;
        }
    }

    public static resolveValueDescriptorFromValue(value: any): ValueDescriptor | undefined {
        const valueType = AssetModelUtil.resolveValueTypeFromValue(value);
        return AssetModelUtil.getValueDescriptor(valueType);
    }

    public static getAttributeAndValueDescriptors(assetType: string | undefined, attributeNameOrDescriptor: string | AttributeDescriptor | undefined, attribute?: Attribute<any>): [AttributeDescriptor | undefined, ValueDescriptor | undefined] {
        let attributeDescriptor: AttributeDescriptor | undefined;
        let valueDescriptor: ValueDescriptor | undefined;

        if (attributeNameOrDescriptor && typeof attributeNameOrDescriptor !== "string") {
            attributeDescriptor = attributeNameOrDescriptor as AttributeDescriptor;
        } else {
            const assetTypeInfo = this.getAssetTypeInfo(assetType || WellknownAssets.UNKNOWNASSET);

            if (!assetTypeInfo) {
                return [undefined, undefined];
            }

            if (typeof (attributeNameOrDescriptor) === "string") {
                attributeDescriptor = this.getAttributeDescriptor(attributeNameOrDescriptor as string, assetTypeInfo);
            }

            if (!attributeDescriptor && attribute) {
                attributeDescriptor = {
                    type: attribute.type,
                    name: attribute.name,
                    meta: attribute.meta
                };
            }
        }

        if (attributeDescriptor) {
            valueDescriptor = this.getValueDescriptor(attributeDescriptor.type);
        }

        return [attributeDescriptor, valueDescriptor];
    }

    public static getMetaItemDescriptor(name?: string): MetaItemDescriptor | undefined {
        if (!name) {
            return;
        }

        // Meta item descriptor names are globally unique
        return this._metaItemDescriptors.find((metaItemDescriptor) => metaItemDescriptor.name === name);
    }

    public static getAssetDescriptorColour(typeOrDescriptor: string | AssetTypeInfo | AssetDescriptor | undefined, fallbackColor?: string): string | undefined {
        const assetDescriptor = this.getAssetDescriptor(typeOrDescriptor);
        return assetDescriptor && assetDescriptor.colour ? assetDescriptor.colour : fallbackColor;
    }

    public static getAssetDescriptorIcon(typeOrDescriptor: string | AssetTypeInfo | AssetDescriptor | undefined, fallbackIcon?: string): string | undefined {
        const assetDescriptor = this.getAssetDescriptor(typeOrDescriptor);
        return assetDescriptor && assetDescriptor.icon ? assetDescriptor.icon : fallbackIcon;
    }
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
            roleMap.set(this._config.clientId!, this._basicIdentity.roles.map((r) => r.name!));
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
        i18next.changeLanguage(lang);
        this.console.storeData("LANGUAGE", lang);
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

    getEventProvider(): EventProvider | undefined {
        return this.events;
    }

    get mapType() {
        return this._config.mapType || MapType.VECTOR;
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
    private _consoleAppConfig?: ConsoleAppConfig;
    private _events?: EventProvider;
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
            console.log("Already initialised");
        }

        let success = false;
        this._config = normaliseConfig(config);

        if (this._config.auth === Auth.BASIC) {
            // BASIC auth will likely require UI so let's init translation at least
            success = await this.doTranslateInit() && success;
            success = await this.doAuthInit();
        } else {
            success = await this.doAuthInit();

            // If failed then we can assume keycloak auth requested but unavailable
            if (!success && !this._config.skipFallbackToBasicAuth) {
                // Try fallback to BASIC
                console.log("Falling back to basic auth");
                this._config.auth = Auth.BASIC;
                success = await this.doAuthInit();
            }
        }

        if (success) {
            success = await this.doInit();
        }

        if (success) {
            success = this.doRestApiInit();
        }

        // Don't let console registration error prevent loading
        await this.doConsoleInit();
        success = await this.doTranslateInit() && success;

        if (success) {
            success = await this.doDescriptorsInit();
            success = await this.getConsoleAppConfig();
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
        } else {
            (this._config as any) = undefined;
            console.warn("Failed to initialise the manager");
        }

        this.displayRealm = config.realm || "master";

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
        const language = !this.console ? undefined : await this.console.retrieveData("LANGUAGE");
        const initOptions: InitOptions = {
            lng: language,
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
            AssetModelUtil._metaItemDescriptors = metaItemDescriptorResponse.data;
            AssetModelUtil._valueDescriptors = valueDescriptorResponse.data;
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
        rest.setTimeout(10000);
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
            const orConsole = new Console(this._config.realm!, this._config.consoleAutoEnable!, () => {
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

    protected async getConsoleAppConfig(): Promise<boolean> {
        try {
            const consoleAppConfigResponse = await this.rest.api.ConsoleAppResource.getAppConfig();
            if (consoleAppConfigResponse.status === 200) {
                this._consoleAppConfig = consoleAppConfigResponse.data;
            }
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
                console.log("Basic authentication cancelled by user");
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
                        console.log("Basic authentication invalid credentials, trying again");
                    }
                }
            } catch (e) {
                console.error("Basic auth failed: ", e);
            }

            if (success) {
                console.log("Basic authentication successful");
                authenticated = true;

                // Get user roles
                const rolesResponse = await rest.api.UserResource.getCurrentUserRoles();
                this._basicIdentity!.roles = rolesResponse.data;
            } else {
                console.log("Unknown response so aborting");
                this._basicIdentity = undefined;
                break;
            }
        }

        this._setAuthenticated(authenticated);
    }

    public isSuperUser(): boolean {
        return !!(this.getRealm() && this.getRealm() === "master" && this.hasRealmRole("admin"));
    }

    public isRestrictedUser(): boolean {
        return !!this.hasRealmRole("restricted_user");
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
        return this._keycloak && this._keycloak.hasRealmRole(role);
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

                const authenticated = await this._keycloak!.init({
                    checkLoginIframe: false, // Doesn't work well with offline tokens or periodic token updates
                    onLoad: this._config.autoLogin ? "login-required" : "check-sso",
                    refreshToken: offlineToken
                });

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

    protected async updateKeycloakAccessToken() {
        try {
            // Access token must be good for X more seconds, should be half of Constants.ACCESS_TOKEN_LIFESPAN_SECONDS
            const tokenRefreshed = await this._keycloak!.updateToken(30);
            // If refreshed from server, it means the refresh token was still good for another access token
            console.debug("Access token update success, refreshed from server: " + tokenRefreshed);
        } catch (e) {
            // Refresh token expired (either SSO max session duration or offline idle timeout), see
            // IDENTITY_SESSION_MAX_MINUTES and IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES server config
            console.info("Access token update failed, refresh token expired, login required");
            this._keycloak!.clearToken();
            this._keycloak!.login();
        }
    }

    protected async _getNativeOfflineRefreshToken(): Promise<string | undefined> {
        if (this.console && this.console.isMobile) {
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
        console.log("Error set: " + error);
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
