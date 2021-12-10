// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
import {pageProvisioningProvider} from "./pages/page-provisioning";

declare var CONFIG_URL_PREFIX: string;

import {combineReducers, configureStore} from "@reduxjs/toolkit";
import "@openremote/or-app";
import {
    OrApp,
    AppConfig,
    appReducer,
    PageProvider,
    RealmAppConfig,
    HeaderConfig,
    HeaderItem} from "@openremote/or-app";
import {
    headerItemGatewayConnection,
    headerItemLanguage,
    headerItemLogout,
    headerItemLogs,
    headerItemMap,
    headerItemAssets,
    headerItemRules,
    headerItemAccount,
    headerItemUsers,
    headerItemRoles,
    headerItemRealms,
    headerItemInsights,
    headerItemExport, headerItemProvisioning
} from "./headers";
import "./pages/page-map";
import {pageMapReducer, pageMapProvider, PageMapConfig} from "./pages/page-map";
import "./pages/page-assets";
import {PageAssetsConfig, pageAssetsProvider} from "./pages/page-assets";
import "./pages/page-gateway";
import {pageGatewayProvider} from "./pages/page-gateway";
import "./pages/page-insights";
import {PageInsightsConfig, pageInsightsProvider} from "./pages/page-insights";
import "./pages/page-rules";
import {PageRulesConfig, pageRulesProvider} from "./pages/page-rules";
import "./pages/page-logs";
import {PageLogsConfig, pageLogsProvider} from "./pages/page-logs";
import "./pages/page-account";
import {pageAccountProvider} from "./pages/page-account";
import "./pages/page-users";
import {pageUsersProvider} from "./pages/page-users";
import "./pages/page-roles";
import {pageRolesProvider} from "./pages/page-roles";
import "./pages/page-realms";
import {pageRealmsProvider} from "./pages/page-realms";
import "./pages/page-realms";
import {pageExportProvider} from "./pages/page-export";
import { ManagerConfig } from "@openremote/core";

const rootReducer = combineReducers({
    app: appReducer,
    map: pageMapReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

type HeaderName = "map" | "assets" | "rules" | "insights" | "gateway" | "logs" | "account" | "users" | "roles" | "realms" | "logout" | "language" | "export";

export interface ManagerRealmConfig {
    appTitle?: string;
    logo?: HTMLTemplateElement | string;
    logoMobile?: HTMLTemplateElement | string;
    favicon?: HTMLTemplateElement | string;
    headers?: HeaderName[];
    language?: string;
    styles?: string;
}

export interface ManagerAppConfig {
    pages?: {
        [name in HeaderName]: any;
    },
    realms?: {
        default?: ManagerRealmConfig;
        [realm: string]: ManagerRealmConfig;
    };
    loadLocales?: boolean;
    manager?: ManagerConfig;
}

const orApp = new OrApp(store);

export const DefaultPagesConfig: PageProvider<any>[] = [
    pageMapProvider(store),
    pageAssetsProvider(store),
    pageGatewayProvider(store),
    pageLogsProvider(store),
    pageInsightsProvider(store),
    pageRulesProvider(store),
    pageAccountProvider(store),
    pageRolesProvider(store),
    pageUsersProvider(store),
    pageRealmsProvider(store),
    pageExportProvider(store),
    pageProvisioningProvider(store)
];

export const DefaultHeaderMainMenu: {[name: string]: HeaderItem} = {
    map: headerItemMap(orApp),
    assets: headerItemAssets(orApp),
    rules: headerItemRules(orApp),
    insights: headerItemInsights(orApp)
};

export const DefaultHeaderSecondaryMenu: {[name: string]: HeaderItem} = {
    gateway: headerItemGatewayConnection(orApp),
    language: headerItemLanguage(orApp),
    logs: headerItemLogs(orApp),
    account: headerItemAccount(orApp),
    users: headerItemUsers(orApp),
    roles: headerItemRoles(orApp),
    realms: headerItemRealms(orApp),
    export: headerItemExport(orApp),
    provisioning: headerItemProvisioning(orApp),
    logout: headerItemLogout(orApp)
};

export const DefaultHeaderConfig: HeaderConfig = {
    mainMenu: Object.values(DefaultHeaderMainMenu),
    secondaryMenu: Object.values(DefaultHeaderSecondaryMenu)
};

export const DefaultRealmConfig: RealmAppConfig = {
    appTitle: "OpenRemote Manager",
    header: DefaultHeaderConfig
};

export const DefaultAppConfig: AppConfig<RootState> = {
    pages: DefaultPagesConfig,
    superUserHeader: DefaultHeaderConfig,
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

    orApp.managerConfig = appConfig.manager;

    orApp.appConfigProvider = (manager) => {

        // Build pages
        let pages: PageProvider<any>[] = [...DefaultPagesConfig];

        if (!manager.isSuperUser() && appConfig.pages) {

            // Replace any supplied page configs
            pages = pages.map(pageProvider => {
                const config = appConfig.pages[pageProvider.name];

                switch (pageProvider.name) {
                    case "map": {
                        pageProvider = config ? pageMapProvider(store, config as PageMapConfig) : pageProvider;
                        break;
                    }
                    case "assets": {
                        pageProvider = config ? pageAssetsProvider(store, config as PageAssetsConfig) : pageProvider;
                        break;
                    }
                    case "rules": {
                        pageProvider = config ? pageRulesProvider(store, config as PageRulesConfig) : pageProvider;
                        break;
                    }
                    case "insights": {
                        pageProvider = config ? pageInsightsProvider(store, config as PageInsightsConfig) : pageProvider;
                        break;
                    }
                    case "logs": {
                        pageProvider = config ? pageLogsProvider(store, config as PageLogsConfig) : pageProvider;
                        break;
                    }
                }

                return pageProvider;
            });
        }

        const orAppConfig: AppConfig<RootState> = {
            pages: pages,
            superUserHeader: DefaultHeaderConfig
        };

        // Configure realms
        if (!appConfig.realms) {
            orAppConfig.realms = {
                default: {...DefaultRealmConfig, header: DefaultHeaderConfig}
            };
        } else {
            orAppConfig.realms = {};
            const defaultRealm = appConfig.realms.default ? {...DefaultRealmConfig,...appConfig.realms.default} : DefaultRealmConfig;
            orAppConfig.realms.default = defaultRealm;

            Object.entries(appConfig.realms).forEach(([name, realmConfig]) => {

                const normalisedConfig = {...defaultRealm,...realmConfig};
                let headers = DefaultHeaderConfig;

                if (normalisedConfig.headers) {
                    headers = {
                        mainMenu: [],
                        secondaryMenu: []
                    };
                    normalisedConfig.headers.forEach((pageName) => {
                        // Insert header
                        if (DefaultHeaderMainMenu.hasOwnProperty(pageName)) {
                            headers.mainMenu.push(DefaultHeaderMainMenu[pageName]);
                        } else if (DefaultHeaderSecondaryMenu.hasOwnProperty(pageName)) {
                            headers.secondaryMenu!.push(DefaultHeaderSecondaryMenu[pageName]);
                        }
                    });
                }

                orAppConfig.realms[name] = {...defaultRealm, header: headers,...(realmConfig as RealmAppConfig)};
            });
        }

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

        return orAppConfig;
    };

    document.body.appendChild(orApp);
});
