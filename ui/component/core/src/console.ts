import {ConsoleRegistration} from "@openremote/model";
import rest from "@openremote/rest";

declare function require(name: string): any;

// No ES6 module support in platform lib
let platform = require('platform');

export interface ProviderMessage {
    provider: string;
    action: string;
    data?: any;
}

interface ProviderInitialiseResponse extends ProviderMessage {
    version: string;
    requiresPermission: boolean;
    hasPermission: boolean;
    success: boolean;
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
    private _registration: ConsoleRegistration;
    private _autoEnable: boolean = false;
    protected _initialised: boolean = false;
    protected _initialiseInProgress: boolean = false;
    protected _pendingProviderPromises: { [name: string]: ((response: any) => void) | null } = {};
    private _pendingProviderEnables: string[] = [];
    protected _enableCompleteCallback: (() => void) | null;

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

        if (!consoleProviders) {
            //consoleProviders = "push"; // Use push provider by default
        }

        let requestedProviders = consoleProviders && consoleProviders.length > 0 ? consoleProviders.split(" ") : [];
        this._pendingProviderEnables = consoleProviders && consoleProviders.length > 0 ? consoleProviders.split(" ") : [];

        // Look for existing console registration in local storage or just create a new one
        let consoleReg: ConsoleRegistration = Console._createConsoleRegistration();

        let consoleRegStr = window.localStorage.getItem("OpenRemoteConsole:" + realm);
        if (consoleRegStr) {
            try {
                consoleReg = JSON.parse(consoleRegStr) as ConsoleRegistration;
            } catch (e) {
                console.error("Failed to deserialise console registration");
            }
        }

        let oldProviders = consoleReg.providers;
        consoleReg.providers = {};

        for (let providerName of requestedProviders) {
            let provider = oldProviders && oldProviders.hasOwnProperty(providerName) ? oldProviders[providerName] : {disabled: false};
            consoleReg.providers[providerName] = provider;
            if (provider.disabled) {
                let index = this._pendingProviderEnables.indexOf(providerName);
                this._pendingProviderEnables.splice(index, 1);
            }
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
        return this._pendingProviderEnables;
    }

    public get shellApple(): boolean {
        // @ts-ignore
        return navigator.platform.substr(0, 2) === 'iP' && window.webkit && window.webkit.messageHandlers;
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
            // Get an ID for this console if it doesn't have one
            if (!this._registration.id) {
                await this.sendRegistration();
            }

            if (this._registration.providers) {

                for (let providerName of Object.keys(this._registration.providers)) {
                    let initResponse = await this._initialiseProvider(providerName);

                    this._registration.providers![providerName].version = initResponse.version;
                    this._registration.providers![providerName].requiresPermission = initResponse.requiresPermission;
                    this._registration.providers![providerName].hasPermission = initResponse.hasPermission;
                    this._registration.providers![providerName].success = initResponse.success;

                    if (!initResponse.success) {
                        console.debug("Provider initialisation failed: '" + providerName + "'");
                        this._registration.providers![providerName].disabled = true;
                        let index = this._pendingProviderEnables.indexOf(providerName);
                        if (index >= 0) {
                            this._pendingProviderEnables.splice(index, 1);
                        }
                    }
                }
            }

            this._initialised = true;
            this._initialiseInProgress = false;

            if (this._pendingProviderEnables.length === 0) {
                let callback = this._enableCompleteCallback;
                this._enableCompleteCallback = null;
                if (callback) {
                    window.setTimeout(() => {
                        callback!()
                    }, 0);
                }
            }
        } catch (e) {
            console.error(e);
            this._initialiseInProgress = false;
        }
    }

    protected static _createConsoleRegistration(): ConsoleRegistration {
        let reg: ConsoleRegistration = {
            name: platform.name,
            version: platform.version,
            platform: platform.os.toString(),
            model: ((platform.manufacturer ? platform.manufacturer + " " : "") + (platform.product ? platform.product : "")).trim()
        };

        return reg;
    }

    protected async _initialiseProvider(provider: string): Promise<ProviderInitialiseResponse> {
        console.debug("Console: initialising provider '" + provider + "'");
        return await this.sendProviderMessage({
                provider: provider,
                action: "PROVIDER_INIT"
            }, true);
    }

    // This mechanism doesn't support sending extra data to the providers being enabled and it doesn't allow data
    // to be retrieved from the replies; if that is required then providers should be manually enabled from the calling app
    public async enableProviders(): Promise<void> {
        if (!this._initialised) {
            throw new Error("Console must be initialised before enabling providers");
        }

        for (let providerName of this._pendingProviderEnables) {
            await this.enableProvider(providerName);
        }
    }

    public async enableProvider(provider: string, data?: any): Promise<ProviderEnableResponse> {
        if (!this._initialised) {
            console.debug("Console must be initialised before disabling providers");
            throw new Error("Console must be initialised before enabling providers");
        }

        if (!this._registration.providers!.hasOwnProperty(provider)) {
            console.debug("Invalid console provider '" + provider + "'");
            throw new Error("Invalid console provider '" + provider + "'");
        }

        console.debug("Console: enabling provider '" + provider + "'");
        let msg: ProviderEnableRequest = {
            provider: provider,
            action: "PROVIDER_ENABLE",
            consoleId: this._registration.id!,
            data
        };
        let response = await this.sendProviderMessage(msg, true);
        let index = this._pendingProviderEnables.indexOf(provider);

        if (index >= 0) {
            this._pendingProviderEnables.splice(index, 1);
            if (this._pendingProviderEnables.length === 0) {
                let callback = this._enableCompleteCallback;
                this._enableCompleteCallback = null;
                if (callback) {
                    callback();
                }
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
        return response;
    }

    public async sendProviderMessage(message: ProviderMessage, waitForResponse: boolean) : Promise<any> {
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
            throw new Error("Message already pending for provider '" + name + "' with action '" + message.action + "'");
        }

        await new Promise(resolve => {
            this._pendingProviderPromises[promiseName] = resolve;
            this._doSendProviderMessage(message);
        });
    }

    private _doSendProviderMessage(msg: any) {
        if (this.isMobile) {
            this.postNativeShellMessage({type: "provider", data: msg});
        } else {
            // TODO: Implement web browser provider handling (web push etc.)
        }
    }

    public async sendRegistration(): Promise<void> {
        console.debug("Console: updating registration");
        let response = await rest.api.ConsoleResource.register(this._registration);
        if (response.status !== 200) {
            throw new Error("Failed to register console");
        }

        this._registration = response.data;
        console.debug("Console: registration successful");
        console.debug("Console: updating locally stored registration");
        window.localStorage.setItem("OpenRemoteConsole:" + this._realm, JSON.stringify(this._registration));
    }

    public postNativeShellMessage(jsonMessage: any) {
        if (this.shellAndroid) {
            // @ts-ignore
            return window.MobileInterface.postMessage(JSON.stringify(jsonMessage));
        }
        if (this.shellApple) {
            // @ts-ignore
            return window.webkit.messageHandlers.int.postMessage(jsonMessage);
        }
    }

    protected readNativeShellMessage(messageKey: string): string | null {
        if (this.shellAndroid) {
            // @ts-ignore
            return window.MobileInterface.getMessage(messageKey);
        }
        if (this.shellApple) {
            return prompt(messageKey);
        }
        return null;
    }

    // This is called by native web view code
    handleProviderResponse(msg: any) {
        if (!msg) {
            return;
        }

        let msgJson = JSON.parse(msg);
        let name = msgJson.provider;
        let action = msgJson.action;

        let resolve = this._pendingProviderPromises[name+action];

        if (resolve != null) {
            this._pendingProviderPromises[name+action] = null;
            resolve(msgJson);
        }
    }
}