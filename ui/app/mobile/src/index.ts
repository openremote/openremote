// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

import {combineReducers, configureStore} from "@reduxjs/toolkit";
import "@openremote/or-app";
import {
    OrApp,
    AppConfig,
    appReducer,
    getRealmQueryParameter,
    headerItemLogout,
    headerItemLogs,
    headerItemMap,
    headerItemAssets,
    headerItemRules,
    headerItemAccount,
    headerItemInsights} from "@openremote/or-app";

import manager, {Auth, DefaultColor2, DefaultColor3, DefaultHeaderHeight, ManagerConfig, Util, BasicLoginResult} from "@openremote/core";
import "@openremote/or-app/dist/pages/page-mobile-onboarding";
import {pageMobileOnboardingProvider, OnboardingConfig} from "@openremote/or-app/dist/pages/page-mobile-onboarding";
import "@openremote/or-app/dist/pages/page-mobile-splash";
import {pageMobileSplashProvider, SplashConfig} from "@openremote/or-app/dist/pages/page-mobile-splash";
import "@openremote/or-app/dist/pages/page-mobile-geofences";
import {pageMobileGeofencesProvider, GeofencesConfig} from "@openremote/or-app/dist/pages/page-mobile-geofences";

const onboardingConfig:OnboardingConfig  = {
    pages: [
        {
            title: "Welcome",
            type: "default",
            description: "Welcome at the first page",
            image: require("../images/logo-mobile.png")
        },
        {
            title: "At the right time and place",
            type: "bottom-image",
            description: "Welcome at the second page",
            enableProviders: [
                {
                    name:"geofence",
                    action:"GEOFENCE_REFRESH"
                }
            ],
            image: require("../images/logo-mobile.png")
        },
        {
            title: "View your messages",
            type: "default",
            description: "Welcome at the third page",
            image: require("../images/logo-mobile.png")
        }
    ],
    redirect: "#!geofences?realm=smartcity&consoleProviders=geofence push storage"
}

const splashConfig:SplashConfig  = {
    redirect: "#!onboarding/",
    interval: 5000,
    logoMobile: require("../images/logo-mobile.png")
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
    realm: getRealmQueryParameter(),
    consoleAutoEnable: true,
    loadTranslations: ["app", "or"]
};

const appConfig: AppConfig<RootState> = {
    pages: {
        default: pageMobileSplashProvider(store, splashConfig),
        geofences: pageMobileGeofencesProvider(store),
        onboarding: pageMobileOnboardingProvider(store, onboardingConfig)
    },
    default: {
        appTitle: "OpenRemote Demo",
        logo: require("../images/logo.png"),
        logoMobile: require("../images/logo-mobile.png")
    }
};
orApp.managerConfig = managerConfig;
orApp.appConfig = appConfig;

document.body.appendChild(orApp);
