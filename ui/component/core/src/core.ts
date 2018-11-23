export enum ORError {
  NONE = "NONE",
  MANAGER_FAILED_TO_LOAD = "MANAGER_FAILED_TO_LOAD",
  KEYCLOAK_FAILED_TO_LOAD = "KEYCLOAK_FAILED_TO_LOAD",
  AUTH_TYPE_UNSUPPORTED = "AUTH_TYPE_UNSUPPORTED"
}

export enum Auth {
  KEYCLOAK = "KEYCLOAK",
  BASIC = "BASIC",
  NONE = "NONE"
}

export enum OREvent {
  INIT = "INIT",
  AUTHENTICATED = "AUTHENTICATED",
  ERROR = "ERROR"
}

export interface Credentials {
  username: string;
  password: string;
}

export interface LoginOptions {
  redirectUrl?: string;
  credentials?: Credentials;
}

export interface ManagerConfig {
  managerUrl: string;
  keycloakUrl?: string;
  auth?: Auth;
  realm?: string;
  autoLogin: boolean;
  credentials?: Credentials;
}

export type EventCallback = (event: OREvent) => any;

export class Manager {

  get username() {
    return this._username;
  }

  get error() {
    return this._error;
  }

  get authenticated() {
    return this._authenticated;
  }

  get initialised() {
    return this._config != null;
  }

  get config() {
    return this._config;
  }

  get roles() {
    return this._roles;
  }

  get managerVersion() {
    return this._managerVersion;
  }

  get isManagerAvailable() {
    return this._managerVersion && this._managerVersion !== "";
  }

  get isError() {
    return this._error != null;
  }

  protected static normaliseConfig(config: ManagerConfig): ManagerConfig {
    const normalisedConfig: ManagerConfig = Object.assign({}, config);

    if (!normalisedConfig.managerUrl || normalisedConfig.managerUrl === "") {
      // Assume manager is running on same host as this code
      normalisedConfig.managerUrl = "/";
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

    return normalisedConfig;
  }

  // TODO: Implement console offline tokens
  protected static getNativeOfflineRefreshToken(): string | null {
    // if (this.console && this.console.enabled) {
    //     let storedToken = this.console.readNativeShellMessage('token');
    //     if (storedToken && storedToken.length > 0) {
    //         let storedTokenParsed = JSON.parse(storedToken);
    //         return storedTokenParsed.refreshToken;
    //     }
    // }
    return null;
  }

  private _error: ORError = ORError.NONE;
  private _config!: ManagerConfig;
  private _authenticated: boolean = false;
  private _name: string = "";
  private _username: string = "";
  private _keycloak: any = null;
  private _roles: string[] = [];
  private _keycloakUpdateTokenInterval?: number = undefined;
  private _managerVersion: string = "";
  private _listeners: EventCallback[] = [];

  public isManagerSameOrigin(): boolean {
    if (!this.initialised) {
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

    this._config = config = Manager.normaliseConfig(config);

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
    } catch (e) {
      // TODO: Implement auto retry?
      console.info("Failed to contact the manager", e);
      this.setError(ORError.MANAGER_FAILED_TO_LOAD);
      this.emitEvent(OREvent.INIT);
      return false;
    }

    switch (config.auth) {
      case Auth.BASIC:
        // TODO: Implement Basic auth support
        this.setError(ORError.AUTH_TYPE_UNSUPPORTED);
        this.emitEvent(OREvent.INIT);
        return false;
      case Auth.KEYCLOAK:
        const success = await this.loadAndInitialiseKeycloak(config);
        this.emitEvent(OREvent.INIT);
        return success;
      case Auth.NONE:
        // Nothing for us to do here
        this.emitEvent(OREvent.INIT);
        return true;
      default:
        this.setError(ORError.AUTH_TYPE_UNSUPPORTED);
        this.emitEvent(OREvent.INIT);
        return false;
    }
  }

  public getLogoutUrl(redirectUrl?: string) {
    if (this._keycloak) {
      const options = redirectUrl && redirectUrl !== "" ? {redirectUri: redirectUrl} : null;
      return this._keycloak.createLogoutUrl(options);
    }

    return "#";
  }

  public getLoginUrl(redirectUrl?: string) {
    if (this._keycloak) {
      const options = redirectUrl && redirectUrl !== "" ? {redirectUri: redirectUrl} : null;
      return this._keycloak.createLoginUrl(options);
    }

    return "#";
  }

  public logout(redirectUrl?: string) {
    if (this._keycloak) {
      const options = redirectUrl && redirectUrl !== "" ? {redirectUri: redirectUrl} : null;
      this._keycloak.logout(options);
    }
  }

  public login(options?: LoginOptions) {
    if (!this.initialised) {
      return;
    }
    switch (this._config.auth) {
      case Auth.BASIC:
        if (options && options.credentials) {
          this._config.credentials = Object.assign({}, options.credentials);
        }
        const username = this._config.credentials ? this._config.credentials.username : null;
        const password = this._config.credentials ? this._config.credentials.password : null;

        if (username && password && username !== "" && password !== "") {
          // TODO: Perform some request to check basic auth credentials
          this.setAuthenticated(true);
        }
        break;
      case Auth.KEYCLOAK:
        if (this._keycloak) {
          const keycloakOptions: any = {};
          if (options && options.redirectUrl && options.redirectUrl !== "") {
            keycloakOptions.redirectUri = options.redirectUrl;
          }
          this._keycloak.login(keycloakOptions);
        }
        break;
      case Auth.NONE:
        break;
    }
  }

  public isSuperUser() {
    return this.hasRole("admin");
  }

  public hasRole(role: string) {
    return this._roles && this._roles.indexOf(role) >= 0;
  }

  public getKeycloakToken(): string | undefined {
    if (this._keycloak && this.authenticated) {
      return this._keycloak.token;
    }
    return undefined;
  }

  // TODO: Native shell support
  protected isNative(): boolean {
    // return this.console && this.console.enabled;
    return false;
  }

  protected onAuthenticated() {
    console.log("Authentication successful");
  }

  // NOTE: The below works with Keycloak 2.x JS API - They made breaking changes in newer versions
  // so this will need updating.
  protected async loadAndInitialiseKeycloak(config: ManagerConfig): Promise<boolean> {

    // Load the keycloak JS API
    const promise = new Promise<Event>((resolve, reject) => {
      // Load keycloak script from keycloak server
      const scriptElement = document.createElement("script");
      scriptElement.src = config.keycloakUrl + "/js/keycloak.js";
      scriptElement.onload = (e) => resolve(e);
      scriptElement.onerror = (e) => reject(e);
      document.querySelector("head")!.appendChild(scriptElement);
    });

    try {
      await promise;

      // Should have Keycloak global var now
      if (!(window as any).Keycloak) {
        this.setError(ORError.KEYCLOAK_FAILED_TO_LOAD);
        return false;
      }

      // Initialise keycloak
      this._keycloak = (window as any).Keycloak({
        clientId: "openremote",
        realm: config.realm,
        url: config.keycloakUrl
      });

      this._keycloak.onAuthSuccess = () => {
        if (keycloakPromise) {
          keycloakPromise(true);
        }
      };

      this._keycloak.onAuthError = () => {
        this.setAuthenticated(false);
      };

      // There's a bug in some Keycloak versions which means the init promise doesn't resolve
      // so putting a check in place; wrap keycloak promise in proper ES6 promise
      let keycloakPromise: any = null;
      try {
        const authenticated = await new Promise<boolean>(((resolve, reject) => {
          keycloakPromise = resolve;
          this._keycloak.init({
            checkLoginIframe: false, // Doesn't work well with offline tokens
            onLoad: config.autoLogin ? "login-required" : "check-sso",
            refreshToken: Manager.getNativeOfflineRefreshToken(), // Try to use a stored offline refresh token
            scope: this.isNative() ? "offline_access" : null
          }).success((auth: boolean) => {
            resolve(auth);
          }).error(() => {
            reject();
          });
        }));

        keycloakPromise = null;

        if (authenticated) {
          this._name = this._keycloak.tokenParsed.name;
          this._username = this._keycloak.tokenParsed.preferred_username;
          this._roles = this._keycloak.realmAccess.roles;

          // Update the access token every 10s (note keycloak will only update if expiring within configured
          // time period.
          if (this._keycloakUpdateTokenInterval) {
            clearInterval(this._keycloakUpdateTokenInterval);
            delete this._keycloakUpdateTokenInterval;
          }
          this._keycloakUpdateTokenInterval = setInterval(() => {
            this.updateKeycloakAccessToken();
          }, 10000);
          this.onAuthenticated();
        }
        this.setAuthenticated(authenticated);
        return true;
      } catch (e) {
        keycloakPromise = null;
        this.setAuthenticated(false);
        return false;
      }
    } catch (error) {
      this.setError(ORError.KEYCLOAK_FAILED_TO_LOAD);
      return false;
    }
  }

  // TODO: Implement console offline tokens
  protected storeNativeOfflineRefreshToken() {
    // // If native shell is enabled, we need an offline refresh token
    // if (this.console && this.console.enabled) {
    //
    //     // If we don't have an offline refresh token, try to get one
    //     if (this.keycloak.refreshTokenParsed.typ !== "Offline") {
    //         console.debug("Native shell enabled, requesting offline refresh token")
    //         this.doKeycloakLogin()
    //         return;
    //     }
    //
    //     // Transfer offline refresh token to native shell so it can be stored for future usage
    //     console.debug("Native shell enabled, storing offline refresh token");
    //     this.console.postNativeShellMessage({
    //         type: 'token',
    //         data: {refreshToken: this.keycloak.refreshToken}
    //     });
    // }
  }

  protected updateKeycloakAccessToken(): Promise<boolean> {
    // Access token must be good for X more seconds, should be half of Constants.ACCESS_TOKEN_LIFESPAN_SECONDS
    return new Promise<boolean>(() => {
      this._keycloak.updateToken(30)
        .success((tokenRefreshed: boolean) => {
          // If refreshed from server, it means the refresh token was still good for another access token
          console.debug("Access token update success, refreshed from server: " + tokenRefreshed);
          return tokenRefreshed;
        })
        .error(() => {
          // Refresh token expired (either SSO max session duration or offline idle timeout), see
          // IDENTITY_SESSION_MAX_MINUTES and IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES server config
          console.info("Access token update failed, refresh token expired, login required");
          this._keycloak.clearToken();
          throw new Error("Access token update failed, refresh token expired, login required");
        });
    });
  }

  protected emitEvent(event: OREvent) {
    for (const listener of this._listeners) {
      listener(event);
    }
  }

  protected setError(error: ORError) {
    this._error = error;
    this.emitEvent(OREvent.ERROR);
  }

  private setAuthenticated(authenticated: boolean) {
    this._authenticated = authenticated;
    if (authenticated) {
      this.emitEvent(OREvent.AUTHENTICATED);
    }
  }
}

export default new Manager();
