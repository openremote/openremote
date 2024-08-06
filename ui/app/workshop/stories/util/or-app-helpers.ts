import {AppConfig, appReducer, HeaderConfig, HeaderItem, OrApp, PageProvider, RealmAppConfig} from "@openremote/or-app";
import {AnyAction, Store, configureStore, combineReducers} from "@reduxjs/toolkit";
import {OrMobileApp} from "@openremote/or-mobile-app";
import {
    headerItemAssets,
    headerItemInsights,
    headerItemLanguage,
    headerItemLogout,
    headerItemMap,
    headerItemRealms,
    headerItemRoles,
    headerItemRules,
    headerItemUsers
} from "@openremote/manager/headers";
import {pageMapProvider, pageMapReducer} from "@openremote/manager/pages/page-map";
import {pageAssetsProvider, pageAssetsReducer} from "@openremote/manager/pages/page-assets";
import {pageInsightsProvider} from "@openremote/manager/pages/page-insights";
import {pageRulesProvider} from "@openremote/manager/pages/page-rules";
import {pageRolesProvider} from "@openremote/manager/pages/page-roles";
import {pageUsersProvider} from "@openremote/manager/pages/page-users";
import {pageRealmsProvider} from "@openremote/manager/pages/page-realms";

type RootState = ReturnType<typeof rootReducer>;

const rootReducer = combineReducers({
    app: appReducer,
    map: pageMapReducer,
    assets: pageAssetsReducer
});

export const DefaultStore = configureStore({
    reducer: rootReducer
});

export function getManagerPages(store: Store<any, AnyAction> = DefaultStore): PageProvider<any>[] {
    return [
        pageAssetsProvider(store),
        pageMapProvider(store),
        pageInsightsProvider(store),
        pageRulesProvider(store),
        pageRolesProvider(store),
        pageUsersProvider(store),
        pageRealmsProvider(store),
    ]
}

export function getManagerHeaderMainMenu(orApp: OrApp<any>): {[name: string]: HeaderItem} {
    return {
        map: headerItemMap(orApp),
        assets: headerItemAssets(orApp),
        rules: headerItemRules(orApp),
        insights: headerItemInsights(orApp)
    }
}

export function getManagerHeaderSecondaryMenu(orApp: OrApp<any>): {[name: string]: HeaderItem} {
    return {
        language: headerItemLanguage(orApp),
        users: headerItemUsers(orApp),
        roles: headerItemRoles(orApp),
        realms: headerItemRealms(orApp),
        logout: headerItemLogout(orApp)
    }
}

export function getManagerHeaderConfig(orApp: OrApp<any>): HeaderConfig {
    return {
        mainMenu: Object.values(getManagerHeaderMainMenu(orApp)),
        secondaryMenu: Object.values(getManagerHeaderSecondaryMenu(orApp))
    }
}

export function getManagerRealmConfig(orApp: OrApp<any>): RealmAppConfig {
    return {
        appTitle: "OpenRemote Manager",
        header: getManagerHeaderConfig(orApp),
    }
}

export function getManagerAppConfig(orApp: OrApp<any>, store: Store<any, AnyAction> = DefaultStore): AppConfig<RootState> {
    return {
        pages: getManagerPages(store),
        superUserHeader: getManagerHeaderConfig(orApp),
        realms: {
            default: getManagerRealmConfig(orApp)
        }
    }
}


/**
 * Initialises {@link OrApp}, awaits its first update, and returns the HTML object.
 */
export async function loadOrApp(args: any, getConfig?: (orApp: OrApp<any>) => Promise<AppConfig<any>>, getStore: () => Store<any, AnyAction> = () => DefaultStore): Promise<OrApp<any>> {
    console.debug("Loading OrApp...");
    const app = Object.assign(new OrApp(getStore()), args) as OrApp<any>;
    if(getConfig) {
        app.appConfig = {...app.appConfig, ...(await getConfig?.(app))};
    }
    console.debug("Waiting for OrApp update complete...");
    await new Promise((r) => setTimeout(r, 1000));
    console.debug("OrApp loaded");
    return app;
}


export async function loadOrMobileApp(args: any, getConfig?: (orApp: OrApp<any>) => Promise<AppConfig<any>>, getStore: () => Store<any, AnyAction> = () => DefaultStore): Promise<OrMobileApp<any>> {
    console.debug("Loading OrMobileApp...");
    const app = Object.assign(new OrMobileApp(getStore()), args);
    if(getConfig) {
        app.appConfig = {...app.appConfig, ...(await getConfig?.(app))};
    }
    console.debug("Waiting for OrMobileApp update complete...");
    await new Promise((r) => setTimeout(r, 1000));
    console.debug("OrMobileApp loaded");
    return app;
}