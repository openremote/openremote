import {ConsoleProvider, ConsoleRegistration} from "@openremote/model";
import manager from "./index";
import {AxiosResponse} from "axios";
import {Deferred} from "./util";

// No ES6 module support in platform lib
let platform = require('platform');

export interface ProviderAction {
    provider: string;
    action: string;
}

export interface ProviderMessage extends ProviderAction{
    data?: any;
    [x: string]: any;
}

interface ProviderInitialiseResponse extends ProviderMessage {
    version: string;
    requiresPermission: boolean;
    hasPermission: boolean;
    success: boolean;
    enabled: boolean;
    disabled: boolean;
}

export interface ProviderEnableRequest extends ProviderMessage {
    consoleId: string;
}

export interface ProviderEnableResponse extends ProviderMessage {
    hasPermission: boolean;
    success: boolean;
}

export class Console {

    protected _realm: string;
    protected _registration: ConsoleRegistration;
    protected _autoEnable: boolean = false;
    protected _initialised: boolean = false;
    protected _initialiseInProgress: boolean = false;
    protected _pendingProviderPromises: { [name: string]: [Deferred<any>, number] } = {};
    protected _providerMessageListeners: { [name: string]: (msg: ProviderMessage) => void } = {};
    protected _pendingProviderEnables: string[] = [];
    protected _enableCompleteCallback: (() => void) | null;
    protected _registrationTimer: number | null = null;

    constructor(realm: string, autoEnable: boolean, enableComplete: () => void) {

        this._realm = realm;
        this._autoEnable = autoEnable;
        this._enableCompleteCallback = enableComplete;

        // Export this to the window to make it accessible from mobile webview code
        // @ts-ignore
        window.OpenRemoteConsole = this;

        // Check for query parameters to override values
        let queryParams = new URLSearchParams(window.location.search);
        let consoleName = queryParams.get("consoleName");
        let consoleVersion = queryParams.get("consoleVersion");
        let consolePlatform = queryParams.get("consolePlatform");
        let consoleProviders = queryParams.get("consoleProviders");
        let autoEnableStr = queryParams.get("consoleAutoEnable");

        let requestedProviders = consoleProviders && consoleProviders.length > 0 ? consoleProviders.split(" ") : ["push", "storage"];

        if (requestedProviders.indexOf("storage") < 0) {
            requestedProviders.push("storage"); // Storage provider is essential to operation and should always be available
        }
        this._pendingProviderEnables = requestedProviders;

        // Look for existing console registration in local storage or just create a new one
        let consoleReg: ConsoleRegistration = Console._createConsoleRegistration();

        let consoleRegStr = window.localStorage.getItem("OpenRemoteConsole:" + realm);
        if (consoleRegStr) {
            try {
                let storedRegObj = JSON.parse(consoleRegStr);
                let storedReg = storedRegObj as ConsoleRegistration;
                if (storedReg.id) {
                    consoleReg.id = storedReg.id;
                }
                if (storedReg.name) {
                    consoleReg.name = storedReg.name;
                }
                if (storedReg.providers) {
                    consoleReg.providers = storedReg.providers;
                }
                if (storedReg.apps) {
                    consoleReg.apps = storedReg.apps;
                }
            } catch (e) {
                console.error("Failed to deserialise console registration");
            }
        }

        let oldProviders = consoleReg.providers;
        consoleReg.providers = {};

        for (let providerName of requestedProviders) {
            let provider = oldProviders && oldProviders.hasOwnProperty(providerName) ? oldProviders[providerName] : {
                enabled: false,
                disabled: false
            };
            consoleReg.providers[providerName] = provider;
        }

        let appName = manager.getAppName();
        if (appName.length > 0 && consoleReg.apps!.indexOf(appName) < 0) {
            consoleReg.apps!.push(appName);
        }

        this._registration = consoleReg;

        if (consoleName) {
            consoleReg.name = consoleName;
        }
        if (consoleVersion) {
            consoleReg.version = consoleVersion;
        }
        if (consolePlatform) {
            consoleReg.platform = consolePlatform;
        }

        if (autoEnableStr) {
            this._autoEnable = autoEnableStr === "TRUE" || autoEnableStr === "true";
        }
    }

    get registration(): ConsoleRegistration {
        return this._registration;
    }

    public get autoEnable(): boolean {
        return this._autoEnable;
    }

    get pendingProviderEnables(): string[] {
        return this._pendingProviderEnables.slice(0);
    }

    public get shellApple(): boolean {
        // @ts-ignore
        const platform = navigator.userAgentData && navigator.userAgentData.platform ? navigator.userAgentData.platform : navigator.platform;
        // @ts-ignore
        return platform && (platform.substring(0, 2) === 'iP' || platform.substring(0, 3) === 'Mac') && window.webkit && window.webkit.messageHandlers;
    }

    public get shellAndroid(): boolean {
        // @ts-ignore
        return !!window.MobileInterface;
    }

    public get isMobile(): boolean {
        return this.shellApple || this.shellAndroid;
    }

    public async initialise(): Promise<void> {
        if (this._initialised || this._initialiseInProgress) {
            return;
        }

        console.debug("Console: initialising");
        this._initialiseInProgress = true;

        try {
            if (this._registration.providers) {
                for (let providerName of Object.keys(this._registration.providers)) {
                    await this._initialiseProvider(providerName);
                }
            }

            // Get an ID for this console if it doesn't have one
            if (!this._registration.id) {
                await this.sendRegistration(0);
            }

            this._initialised = true;
            this._initialiseInProgress = false;

            if (this._pendingProviderEnables.length === 0) {
                await this.sendRegistration();
                this._callCompletedCallback();
            } else if (this._autoEnable) {
                await this.enableProviders();
            }
        } catch (e) {
            console.error(e);
        } finally {
            this._initialiseInProgress = false;
        }
    }


    // This mechanism doesn't support sending extra data to the providers being enabled and it doesn't allow data
    // to be retrieved from the replies; if that is required then providers should be manually enabled from the calling app
    public async enableProviders(): Promise<void> {
        if (!this._initialised) {
            throw new Error("Console must be initialised before enabling providers");
        }

        for (let index = this._pendingProviderEnables.length - 1; index > -1; index--) {
            let providerName = this._pendingProviderEnables[index];
            await this.enableProvider(providerName);
        }
    }

    public async enableProvider(providerName: string, data?: any): Promise<ProviderEnableResponse> {
        if (!this._initialised) {
            console.debug("Console must be initialised before disabling providers");
            throw new Error("Console must be initialised before enabling providers");
        }

        if (!this._registration.providers!.hasOwnProperty(providerName)) {
            console.debug("Invalid console provider '" + providerName + "'");
            throw new Error("Invalid console provider '" + providerName + "'");
        }

        console.debug("Console: enabling provider '" + providerName + "'");
        let msg: ProviderEnableRequest = {
            provider: providerName,
            action: "PROVIDER_ENABLE",
            consoleId: this._registration.id!,
            data
        };
        let response = await this.sendProviderMessage(msg, true);

        this._registration.providers![providerName].hasPermission = response.hasPermission;
        this._registration.providers![providerName].success = response.success;
        this._registration.providers![providerName].enabled = response.success;
        this._registration.providers![providerName].data = response.data;

        let index = this._pendingProviderEnables.indexOf(providerName);

        if (index >= 0) {
            this._pendingProviderEnables.splice(index, 1);
            if (this._pendingProviderEnables.length === 0) {
                this.sendRegistration();
                this._callCompletedCallback();
            }
        }

        return response;
    }

    public async disableProvider(provider: string): Promise<ProviderMessage> {
        if (!this._initialised) {
            console.debug("Console must be initialised before disabling providers");
            throw new Error("Console must be initialised before disabling providers");
        }

        if (!this._registration.providers!.hasOwnProperty(provider)) {
            console.debug("Invalid console provider '" + provider + "'");
            throw new Error("Invalid console provider '" + provider + "'");
        }

        console.debug("Console: disabling provider '" + provider + "'");

        let response = await this.sendProviderMessage({
            provider: provider,
            action: "PROVIDER_DISABLE"
        }, true);
        this._registration.providers![provider].disabled = true;
        this._registration.providers![provider].enabled = false;
        return response;
    }

    public getProvider(name: string): ConsoleProvider | undefined {
        return this._registration && this._registration.providers ? this._registration.providers[name] : undefined;
    }

    public async sendProviderMessage(message: ProviderMessage, waitForResponse: boolean): Promise<any | null> {
        if (!this._registration.providers!.hasOwnProperty(message.provider)) {
            console.debug("Invalid console provider '" + message.provider + "'");
            throw new Error("Invalid console provider '" + message.provider + "'");
        }

        if (!waitForResponse) {
            this._doSendProviderMessage(message);
            return;
        }

        let promiseName = message.provider + message.action;

        if (this._pendingProviderPromises[promiseName]) {
            throw new Error("Message already pending for provider '" + message.provider + "' with action '" + message.action + "'");
        }

        const deferred = new Deferred();
        const cancel = () => {
            delete this._pendingProviderPromises[promiseName];
            deferred.reject("No response from provider");
        };
        this._pendingProviderPromises[promiseName] = [deferred, window.setTimeout(cancel, 5000)];
        this._doSendProviderMessage(message);
        return deferred.promise;
    }

    // Uses a delayed mechanism to avoid excessive calls to the server during enabling providers
    public sendRegistration(delay?: number): Promise<void> {

        if (this._registrationTimer) {
            window.clearTimeout(this._registrationTimer);
            this._registrationTimer = null;
        }

        delay = delay !== undefined ? delay : 2000;

        console.debug("Sending registration in: " + delay + "ms");

        return new Promise((resolve, reject) => {
            this._registrationTimer = window.setTimeout(() => {
                this._registrationTimer = null;
                console.debug("Console: updating registration");

                try {
                    // Ensure console name, platform, version and providers are not null
                    if (!this._registration.name) {
                        this._registration.name = "Console";
                    }
                    if (!this._registration.platform) {
                        this._registration.platform = "N/A";
                    }
                    if (!this._registration.version) {
                        this._registration.version = "N/A"
                    }
                    if (!this._registration.providers) {
                        this._registration.providers = {};
                    }
                    manager.rest.api.ConsoleResource.register(this._registration).then((response: AxiosResponse<ConsoleRegistration>) => {
                        if (response.status !== 200) {
                            throw new Error("Failed to register console");
                        }

                        this._registration = response.data;
                        console.debug("Console: registration successful");
                        console.debug("Console: updating locally stored registration");
                        window.localStorage.setItem("OpenRemoteConsole:" + this._realm, JSON.stringify(this._registration));
                        resolve();
                    });
                } catch (e) {
                    console.error("Failed to register console");
                    reject("Failed to register console");
                }
            },);
        });
    }

    public storeData(key: string, value: any) {
        this.sendProviderMessage({
            provider: "storage",
            action: "STORE",
            key: key,
            value: value
        }, false);
    }


    public async retrieveData<T>(key: string): Promise<T | undefined> {
        let response = await this.sendProviderMessage({
            provider: "storage",
            action: "RETRIEVE",
            key: key
        }, true);

        if (response && response.value) {
            return response.value as T;
        }
    }

    public addProviderMessageListener(providerAction: ProviderAction, listener: (msg: ProviderMessage) => void) {
        this._providerMessageListeners[providerAction.provider + providerAction.action] = listener;
    }

    public removeProviderMessageListener(providerAction: ProviderAction) {
        delete this._providerMessageListeners[providerAction.provider + providerAction.action];
    }

    protected _postNativeShellMessage(jsonMessage: any) {
        if (this.shellAndroid) {
            // @ts-ignore
            return window.MobileInterface.postMessage(JSON.stringify(jsonMessage));
        }
        if (this.shellApple) {
            // @ts-ignore
            return window.webkit.messageHandlers.int.postMessage(jsonMessage);
        }
    }

    public _doSendProviderMessage(msg: any) {
        if (this.isMobile) {
            this._postNativeShellMessage({type: "provider", data: msg});
        } else {
            if (!msg.provider || !msg.action) {
                return;
            }
            switch (msg.provider.trim().toUpperCase()) {
                // TODO: Implement web browser provider handling (web push etc.)
                case "PUSH":

                    switch (msg.action.trim().toUpperCase()) {
                        case "PROVIDER_INIT":
                            let initResponse: ProviderInitialiseResponse = {
                                action: "PROVIDER_INIT",
                                provider: "push",
                                version: "web",
                                enabled: true,
                                disabled: false,
                                hasPermission: true,
                                requiresPermission: false,
                                success: true
                            };
                            this._handleProviderResponse(JSON.stringify(initResponse));
                            break;
                        case "PROVIDER_ENABLE":
                            let enableResponse: ProviderEnableResponse = {
                                action: "PROVIDER_ENABLE",
                                provider: "push",
                                hasPermission: true,
                                success: true
                            };
                            this._handleProviderResponse(JSON.stringify(enableResponse));
                            break;
                        default:
                            throw new Error("Unsupported provider '" + msg.provider + "' and action '" + msg.action + "'");
                    }

                    break;

                case "STORAGE":

                    switch (msg.action) {
                        case "PROVIDER_INIT":
                            let initResponse: ProviderInitialiseResponse = {
                                action: "PROVIDER_INIT",
                                provider: "storage",
                                version: "1.0.0",
                                disabled: false,
                                enabled: true,
                                hasPermission: true,
                                requiresPermission: false,
                                success: true
                            };
                            this._handleProviderResponse(JSON.stringify(initResponse));
                            break;
                        case "PROVIDER_ENABLE":
                            let enableResponse: ProviderEnableResponse = {
                                action: "PROVIDER_ENABLE",
                                provider: "storage",
                                hasPermission: true,
                                success: true
                            };
                            this._handleProviderResponse(JSON.stringify(enableResponse));
                            break;
                        case "STORE": {
                                let keyValue = msg.key ? msg.key.trim() : null;

                                if (!keyValue || keyValue.length === 0) {
                                    throw new Error("Storage provider 'store' action requires a `key`");
                                }

                                if (msg.value === null) {
                                    window.localStorage.removeItem(keyValue);
                                } else {
                                    window.localStorage.setItem(keyValue, JSON.stringify(msg.value));
                                }
                            }
                            break;
                        case "RETRIEVE": {
                                let keyValue = msg.key ? msg.key.trim() : null;

                                if (!keyValue || keyValue.length === 0) {
                                    throw new Error("Storage provider 'retrieve' action requires a `key`");
                                }

                                let val = window.localStorage.getItem(keyValue);
                                if (val !== null) {
                                    try {
                                        val = JSON.parse(val);
                                    } catch (e) {
                                        // Fallback to just returning the val as it might not be JSON
                                    }
                                }

                                this._handleProviderResponse(JSON.stringify({
                                    action: "RETRIEVE",
                                    provider: "storage",
                                    key: keyValue,
                                    value: val
                                }));
                            }
                            break;
                        default:
                            throw new Error("Unsupported provider '" + msg.provider + "' and action '" + msg.action + "'");
                    }
                    break;
                default:
                    throw new Error("Unsupported provider: " + msg.provider);
            }
        }
    }

    // This is called by native web view code
    protected _handleProviderResponse(msg: any) {
        if (!msg) {
            return;
        }

        let msgJson = JSON.parse(msg);
        let name = msgJson.provider;
        let action = msgJson.action;

        let deferredAndTimeout = this._pendingProviderPromises[name + action];

        if (deferredAndTimeout) {
            window.clearTimeout(deferredAndTimeout[1]);
            delete this._pendingProviderPromises[name + action];
            deferredAndTimeout[0].resolve(msgJson);
        }

        let listener = this._providerMessageListeners[name + action];
        if (listener) {
            listener(msgJson);
        }
    }

    protected _callCompletedCallback() {
        let callback = this._enableCompleteCallback;
        this._enableCompleteCallback = null;
        if (callback) {
            window.setTimeout(() => {
                callback!()
            }, 0);
        }
    }

    protected static _createConsoleRegistration(): ConsoleRegistration {
        let reg: ConsoleRegistration = {
            name: platform.name,
            version: platform.version,
            platform: platform.os.toString(),
            apps: [],
            model: ((platform.manufacturer ? platform.manufacturer + " " : "") + (platform.product ? platform.product : "")).trim()
        };

        return reg;
    }

    protected async _initialiseProvider(providerName: string): Promise<void> {
        console.debug("Console: initialising provider '" + providerName + "'");
        let initResponse: ProviderInitialiseResponse;

        try {
            initResponse = await this.sendProviderMessage({
                provider: providerName,
                action: "PROVIDER_INIT"
            }, true) as ProviderInitialiseResponse;

            this._registration.providers![providerName].version = initResponse.version;
            this._registration.providers![providerName].requiresPermission = initResponse.requiresPermission;
            this._registration.providers![providerName].hasPermission = initResponse.hasPermission;
            this._registration.providers![providerName].success = initResponse.success;
            this._registration.providers![providerName].enabled = initResponse.enabled;
            this._registration.providers![providerName].disabled = initResponse.disabled;
            this._registration.providers![providerName].data = initResponse.data;
        } catch (e) {
            console.error(e);
            initResponse = {
                action: "",
                disabled: false,
                enabled: false,
                hasPermission: false,
                provider: "",
                requiresPermission: false,
                version: "",
                success: false
            };
        }

        if (!initResponse.success) {
            console.debug("Provider initialisation failed: '" + providerName + "'");
            initResponse.disabled = true;
            this._registration.providers![providerName].disabled = true;
        }

        if (initResponse.disabled || initResponse.enabled) {
            let index = this._pendingProviderEnables.indexOf(providerName);
            if (index >= 0) {
                this._pendingProviderEnables.splice(index, 1);
            }
        }
    }
}
