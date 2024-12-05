/*
 * Copyright 2024, OpenRemote Inc.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { Auth, ManagerConfig } from "@openremote/model";

declare function require(name: string): any;

import { combineReducers, configureStore } from "@reduxjs/toolkit";
import "@openremote/or-app";
import {
    OrApp,
    AppConfig,
    appReducer
} from "@openremote/or-app";

import "./pages/page-mobile-onboarding";
import { pageMobileOnboardingProvider, OnboardingConfig } from "./pages/page-mobile-onboarding";
import "./pages/page-mobile-splash";
import { pageMobileSplashProvider, SplashConfig } from "./pages/page-mobile-splash";
import "./pages/page-mobile-geofences";
import { pageMobileGeofencesProvider } from "./pages/page-mobile-geofences";
import { Util } from "@openremote/core";

const onboardingConfig: OnboardingConfig = {
    pages: [
        {
            title: "The city assets in your pocket",
            type: "default",
            description: "With this app you have all live asset data conveniently available. Handy when doing maintenance or installing new devices.",
            image: require("../images/onboarding-assets.svg"),
            enableProviders: [
                {
                    name: "push",
                    action: "PROVIDER_ENABLE"
                }
            ],
        },
        {
            title: "At the right time and the right place",
            type: "bottom-image",
            description: "You can receive notifications that are created by workflow rules. Based on your location relevant messages can be sent to you.",
            enableProviders: [
                {
                    name: "geofence",
                    action: "PROVIDER_ENABLE"
                }
            ],
            image: require("../images/onboarding-geofence.svg")
        }
    ],
    redirect: "/manager/?realm=smartcity&consoleProviders=geofence push storage"
}

const splashConfig: SplashConfig = {
    redirect: "#onboarding/",
    interval: 3000,
    logoMobile: require("../images/logo-mobile.svg")
}

declare var MANAGER_URL: string;

const rootReducer = combineReducers({
    app: appReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

const orApp = new OrApp(store);
const managerConfig: ManagerConfig = {
    managerUrl: MANAGER_URL,
    auth: Auth.NONE,
    autoLogin: false,
    realm: Util.getQueryParameter("realm"),
    consoleAutoEnable: false,
    loadTranslations: ["app", "or"],
    defaultLanguage: Util.getBrowserLanguage(),
};

const appConfig: AppConfig<RootState> = {
    pages: [
        pageMobileSplashProvider(store, splashConfig),
        pageMobileGeofencesProvider(store),
        pageMobileOnboardingProvider(store, onboardingConfig)
    ],
    realms: {
        default: {
            appTitle: "OpenRemote Demo",
            logo: require("../images/logo.png"),
            logoMobile: require("../images/logo-mobile.png")
        }
    }
};
orApp.managerConfig = managerConfig;
orApp.appConfig = appConfig;
document.body.appendChild(orApp);
