// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
import {pageProvisioningProvider} from "./pages/page-provisioning";
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import "@openremote/or-app";
import {AppConfig, appReducer, HeaderConfig, HeaderItem, OrApp, PageProvider, RealmAppConfig} from "@openremote/or-app";
import {
    headerItemAccount,
    headerItemAssets,
    headerItemConfiguration,
    headerItemExport,
    headerItemGatewayConnection,
    headerItemGatewayTunnel,
    headerItemInsights,
    headerItemServices,
    headerItemLanguage,
    headerItemLogout,
    headerItemLogs,
    headerItemMap,
    headerItemProvisioning,
    headerItemRealms,
    headerItemRoles,
    headerItemRules,
    headerItemUsers
} from "./headers";
import "./pages/page-map";
import {PageMapConfig, pageMapProvider, pageMapReducer} from "./pages/page-map";
import "./pages/page-assets";
import {PageAssetsConfig, pageAssetsProvider, pageAssetsReducer} from "./pages/page-assets";
import "./pages/page-gateway";
import {pageGatewayProvider} from "./pages/page-gateway";
import "./pages/page-insights";
import {PageInsightsConfig, pageInsightsProvider} from "./pages/page-insights";
import "./pages/page-services";
import {pageServicesProvider} from "./pages/page-services";
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
import {pageExportProvider} from "./pages/page-export";
import { pageConfigurationProvider } from "./pages/page-configuration";
import {pageAlarmsProvider} from "./pages/page-alarms";
import { ManagerAppConfig } from "@openremote/model";
import {pageGatewayTunnelProvider} from "./pages/page-gateway-tunnel";

declare var MANAGER_URL: string | undefined;

const rootReducer = combineReducers({
    app: appReducer,
    map: pageMapReducer,
    assets: pageAssetsReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

const orApp = new OrApp(store);

export const DefaultPagesConfig: PageProvider<any>[] = [
    pageMapProvider(store),
    pageAssetsProvider(store),
    pageGatewayProvider(store),
    pageGatewayTunnelProvider(store),
    pageLogsProvider(store),
    pageInsightsProvider(store),
    pageServicesProvider(store),
    pageRulesProvider(store),
    pageAccountProvider(store),
    pageRolesProvider(store),
    pageUsersProvider(store),
    pageRealmsProvider(store),
    pageExportProvider(store),
    pageProvisioningProvider(store),
    pageConfigurationProvider(store),
    pageAlarmsProvider(store)
];

export const DefaultHeaderMainMenu: {[name: string]: HeaderItem} = {
    map: headerItemMap(orApp),
    assets: headerItemAssets(orApp),
    rules: headerItemRules(orApp),
    services: headerItemServices(orApp),
    insights: headerItemInsights(orApp),
};

export const DefaultHeaderSecondaryMenu: {[name: string]: HeaderItem} = {
    gatewayConnection: headerItemGatewayConnection(orApp),
    gatewayTunnel: headerItemGatewayTunnel(orApp),
    language: headerItemLanguage(orApp),
    logs: headerItemLogs(orApp),
    account: headerItemAccount(orApp),
    users: headerItemUsers(orApp),
    roles: headerItemRoles(orApp),
    realms: headerItemRealms(orApp),
    export: headerItemExport(orApp),
    provisioning: headerItemProvisioning(orApp),
    appearance: headerItemConfiguration(orApp),
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

// Try and load the app config from JSON and if anything is found amalgamate it with default
const managerURL = MANAGER_URL || window.location.protocol + "//" + window.location.hostname + (window.location.port ? ":" + window.location.port : "")
    + window.location.pathname.replace(/\/[^/]+\/?$/, '');
const configURL = managerURL + "/api/master/configuration/manager";

fetch(configURL).then<ManagerAppConfig>(async (result) => {
    let appConfig: ManagerAppConfig;

    if (result.status === 200) {
        appConfig = await result.json() as ManagerAppConfig;
    }

    return {...appConfig};
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

    orApp.managerConfig = appConfig.manager;

    orApp.appConfigProvider = (manager) => {

        // Build pages
        let pages: PageProvider<any>[] = [...DefaultPagesConfig];
        const isAdmin = manager.isSuperUser() && manager.username === "admin";
        const applyConfigToAdmin = appConfig.manager.applyConfigToAdmin !== undefined ? appConfig.manager.applyConfigToAdmin : true;

        if ((!isAdmin || applyConfigToAdmin) && appConfig.pages) {

            // Replace any supplied page configs
            pages = pages.map(pageProvider => {
                const config: {[p: string]: any} | undefined = appConfig.pages[pageProvider.name];

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
                        const newConfig = (config || {
                            rules: {}
                        }) as PageRulesConfig;
                        if(!newConfig.rules?.notifications) {
                            newConfig.rules.notifications = Object.fromEntries(
                                Object.entries(appConfig.realms).map(entry => [entry[0], entry[1].notifications])
                            );
                        }
                        pageProvider = pageRulesProvider(store, newConfig);
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

                orAppConfig.realms[name] = { ...defaultRealm, header: headers, ...(realmConfig as RealmAppConfig) };
            });
        }

        // When the page is not present in the header, move the PageProvider to the end of the array.
        // This is to prevent the landing page for the user not being visible in the header
        const realmAppConfig = orAppConfig.realms[manager.displayRealm] || orAppConfig.realms.default;
        if(realmAppConfig) {
            const headerPaths = [...realmAppConfig.header.mainMenu, ...realmAppConfig.header.secondaryMenu].map(item => item.href);
            orAppConfig.pages = pages
                .filter(pageProvider => headerPaths.includes(pageProvider.name))
                .concat(pages.filter(pageProvider => !headerPaths.includes(pageProvider.name)));
        }

        // If the user does not have a preferred language configured (in Keycloak),
        // we need to update it with their preferred language from other sources. (consoles, realm configuration etc.)
        // Check local storage for set language, otherwise use language set in config
        manager.getUserPreferredLanguage().then((userLang: string | undefined) => {
            if(!userLang) {
                manager.getConsolePreferredLanguage().then((consoleLang: string | undefined) => {
                    if (consoleLang) {
                        manager.language = consoleLang;
                    } else if (orAppConfig.realms[manager.displayRealm]) {
                        manager.language = orAppConfig.realms[manager.displayRealm].language
                    } else if (orAppConfig.realms['default']) {
                        manager.language = orAppConfig.realms['default'].language
                    } else {
                        manager.language = 'en'
                    }
                }).catch(reason => {
                    console.error("Failed to initialise app: " + reason);
                })
            }
        })

        return orAppConfig;
    };

    document.body.appendChild(orApp);
});
