// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

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
    headerItemInsights} from "./headers";
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

const rootReducer = combineReducers({
    app: appReducer,
    map: pageMapReducer
});

type RootState = ReturnType<typeof rootReducer>;


export interface ManagerPageConfig {
    name: string;
    hide?: boolean;
    default?: boolean;
    config?: any;
}

export interface ManagerRealmAppConfig {
    appTitle?: string;
    logo?: HTMLTemplateElement | string;
    logoMobile: HTMLTemplateElement | string;
    language?: string;
    styles?: string;
}

export interface ManagerAppConfig {
    pages?: ManagerPageConfig[],
    realms?: {
        default?: ManagerRealmAppConfig;
        [realm: string]: ManagerRealmAppConfig;
    };
}


export const store = configureStore({
    reducer: rootReducer
});

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
    pageRealmsProvider(store)
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
    logout: headerItemLogout(orApp)
};

export const DefaultHeaderConfig: HeaderConfig = {
    mainMenu: Object.values(DefaultHeaderMainMenu),
    secondaryMenu: Object.values(DefaultHeaderSecondaryMenu)
};

export const DefaultRealmAppConfig: RealmAppConfig = {
    appTitle: "OpenRemote Manager",
    logo: require("../images/logo.png"),
    logoMobile: require("../images/logo-mobile.png"),
    header: DefaultHeaderConfig
};

export const DefaultAppConfig: AppConfig<RootState> = {
    pages: DefaultPagesConfig,
    realms: {
        default: DefaultRealmAppConfig
    }
};

// Try and load the app config from JSON and if anything is found amalgamate it with
fetch("./app_config.json").then((result) => {
    const appConfig = result.ok ? result.json() as ManagerAppConfig : undefined;

    if (!appConfig) {
        return DefaultAppConfig;
    }

    // Build pages
    let pages: PageProvider<any>[];
    let headerConfig: HeaderConfig;

    if (!appConfig.pages) {
        pages = DefaultPagesConfig;
        headerConfig = DefaultHeaderConfig;
    } else {
        headerConfig = {
            mainMenu: [],
            secondaryMenu: []
        };
        pages = [...DefaultPagesConfig]
            // Filter out hidden pages
            .filter((pageProvider) => !appConfig.pages?.some((p) => p.name === pageProvider.name && p.hide))
            .map(pageProvider => {
                // Replace any configs
                const pageConfig = appConfig.pages?.find((pageConfig) => pageConfig.name === pageProvider.name);
                const config = pageConfig ? pageConfig.config : undefined;

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

                // Generate headers
                if (DefaultHeaderMainMenu.hasOwnProperty(pageProvider.name)) {
                    headerConfig.mainMenu.push(DefaultHeaderMainMenu[pageProvider.name]);
                } else if (DefaultHeaderSecondaryMenu.hasOwnProperty(pageProvider.name)) {
                    headerConfig.secondaryMenu!.push(DefaultHeaderSecondaryMenu[pageProvider.name]);
                }

                return pageProvider;
            });

        if (pages.length > 0) {
            // Look for a new default
            const defaultPageConfig = appConfig.pages.find((pageConfig) => !!pageConfig.default);

            if (defaultPageConfig && defaultPageConfig.name !== pages[0].name) {
                const newDefaultIndex = pages.findIndex((pageProvider) => pageProvider.name === defaultPageConfig.name);
                if (newDefaultIndex > 0) {
                    const newDefault = pages.splice(newDefaultIndex, 1)[0];
                    pages.splice(0, 0, newDefault);
                }
            }
        }
    }

    const orAppConfig: AppConfig<RootState> = {
        pages: pages
    };

    if (!appConfig.realms) {
        orAppConfig.realms = {
            default: {...DefaultRealmAppConfig, header: headerConfig}
        };
    } else {

    }

    return orAppConfig;


}).then((orAppConfig) => {
    orApp.appConfig = orAppConfig!;
    document.body.appendChild(orApp);
});
