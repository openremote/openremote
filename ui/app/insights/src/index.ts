import {combineReducers, configureStore} from "@reduxjs/toolkit";
import "@openremote/or-app";
import {AppConfig, appReducer, OrApp, PageProvider, RealmAppConfig} from "@openremote/or-app";
import {pageViewProvider} from "./pages/page-view";
import {ManagerAppConfig} from "@openremote/model";

declare const CONFIG_URL_PREFIX: string;

const rootReducer = combineReducers({
    app: appReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

const orApp = new OrApp(store);

export const DefaultRealmConfig: RealmAppConfig = {
    appTitle: "OpenRemote Insights",
};

export const DefaultAppConfig: AppConfig<RootState> = {
    pages: [],
    realms: {
        default: DefaultRealmConfig
    }
};

// Try and load the app config from JSON and if anything is found amalgamate it with default
const configURL = (CONFIG_URL_PREFIX || "") + "/manager_config.json";

fetch(configURL).then(async (result) => {
    if (!result.ok) {
        return DefaultAppConfig;
    }

    return await result.json() as ManagerAppConfig;

}).then((appConfig: ManagerAppConfig) => {

    // Set locales and load path
    if (!appConfig.manager) {
        appConfig.manager = {};
    }

    if (appConfig.loadLocales) {
        appConfig.manager.loadTranslations = ["app", "or"];

        if (!appConfig.manager.translationsLoadPath) {
            appConfig.manager.translationsLoadPath = "/locales/{{lng}}/{{ns}}.json";
        }
    }

    // Add config prefix if defined (used in dev)
    if (CONFIG_URL_PREFIX) {
        if (appConfig.manager.translationsLoadPath) {
            appConfig.manager.translationsLoadPath = CONFIG_URL_PREFIX + appConfig.manager.translationsLoadPath;
        }
    }

    // Override specific for Insights app, since it doesn't require login.
    // Login and logout buttons can be found in the sidebar menu of the app.
    appConfig.manager.autoLogin = false;

    orApp.managerConfig = appConfig.manager;

    orApp.appConfigProvider = (manager) => {

        // Configure app config
        let pages: PageProvider<any>[] = [];
        const orAppConfig: AppConfig<RootState> = {
            pages: pages,
        };

        // Configure realms
        if (!appConfig.realms) {
            orAppConfig.realms = {
                default: {...DefaultRealmConfig}
            };
        } else {
            orAppConfig.realms = {};
            const defaultRealm = appConfig.realms.default ? {...DefaultRealmConfig,...appConfig.realms.default} : DefaultRealmConfig;
            orAppConfig.realms.default = defaultRealm;

            Object.entries(appConfig.realms).forEach(([name, realmConfig]) => {
                orAppConfig.realms[name] = { ...defaultRealm, ...(realmConfig as RealmAppConfig) };
            });
        }

        // Check local storage for set language, otherwise use language set in config
        manager.console.retrieveData("LANGUAGE").then((value: string | undefined) => {
            manager.language = (value ? value : orAppConfig.realms[manager.displayRealm].language);
        }).catch(() => {
            if (orAppConfig.realms[manager.displayRealm]){
                manager.language = orAppConfig.realms[manager.displayRealm].language
            } else if (orAppConfig.realms['default']){
                manager.language = orAppConfig.realms['default'].language
            } else {
                manager.language = 'en'
            }
        })

        // Add config prefix if defined (used in dev)
        if (CONFIG_URL_PREFIX) {
            Object.values(orAppConfig.realms).forEach((realmConfig) => {
                if (typeof (realmConfig.logo) === "string") {
                    realmConfig.logo = CONFIG_URL_PREFIX + realmConfig.logo;
                }
                if (typeof (realmConfig.logoMobile) === "string") {
                    realmConfig.logoMobile = CONFIG_URL_PREFIX + realmConfig.logoMobile;
                }
            });
        }

        // Add insights page with correct parameters
        orAppConfig.pages.push(pageViewProvider(store, orAppConfig.realms))

        return orAppConfig;
    }

    document.body.appendChild(orApp);
});
