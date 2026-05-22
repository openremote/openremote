/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {AppConfig, appReducer, OrApp, PageProvider, RealmAppConfig} from "@openremote/or-app";
import {pageViewProvider} from "./pages/page-view";
import {ManagerAppConfig} from "@openremote/model";

declare const MANAGER_URL: string | undefined;

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
const configURL = (MANAGER_URL ?? "") + "/api/master/configuration/manager";

fetch(configURL).then(async (result) => {
    if (!result.ok || result.status === 204) {
        return DefaultAppConfig;
    }

    const appConfig = await result.json() as ManagerAppConfig;

    if (appConfig === null) {
        return DefaultAppConfig;
    }

    return appConfig;

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

    // Override specific for Insights app, since it doesn't require login.
    // Login and logout buttons can be found in the sidebar menu of the app.
    appConfig.manager.autoLogin = false;

    orApp.managerConfig = appConfig.manager;

    orApp.appConfigProvider = (manager) => {

        // Configure app config
        const pages: PageProvider<any>[] = [];
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
            manager.language = (value || orAppConfig.realms[manager.displayRealm].language);
        }).catch(() => {
            if (orAppConfig.realms[manager.displayRealm]){
                manager.language = orAppConfig.realms[manager.displayRealm].language
            } else if (orAppConfig.realms.default){
                manager.language = orAppConfig.realms.default.language
            } else {
                manager.language = 'en'
            }
        })

        // Add insights page with correct parameters
        orAppConfig.pages.push(pageViewProvider(store, orAppConfig.realms))

        return orAppConfig;
    }

    document.body.appendChild(orApp);
});
