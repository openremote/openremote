// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

import {combineReducers, configureStore} from "@reduxjs/toolkit";
import "@openremote/or-app";
import {
    OrApp,
    AppConfig,
    appReducer,
    getRealmQueryParameter} from "@openremote/or-app";

import {Auth, ManagerConfig} from "@openremote/core";
import "./pages/page-mobile-onboarding";
import {pageMobileOnboardingProvider, OnboardingConfig} from "./pages/page-mobile-onboarding";
import "./pages/page-mobile-splash";
import {pageMobileSplashProvider, SplashConfig} from "./pages/page-mobile-splash";
import "./pages/page-mobile-geofences";
import {pageMobileGeofencesProvider} from "./pages/page-mobile-geofences";

const onboardingConfig:OnboardingConfig  = {
    pages: [
        {
            title: "The city assets in your pocket",
            type: "default",
            description: "With this app you have all live asset data conveniently available. Handy when doing maintenance or installing new devices.",
            image: require("../images/onboarding-assets.svg"),
            enableProviders: [
                {
                    name:"push",
                    action:"PROVIDER_ENABLE"
                }
            ],
        },
        {
            title: "At the right time and the right place",
            type: "bottom-image",
            description: "You can receive notifications that are created by workflow rules. Based on your location relevant messages can be sent to you.",
            enableProviders: [
                {
                    name:"geofence",
                    action:"PROVIDER_ENABLE"
                }
            ],
            image: require("../images/onboarding-geofence.svg")
        }
    ],
    redirect: "/manager/?realm=smartcity&consoleProviders=geofence push storage"
}

const splashConfig:SplashConfig  = {
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
    realm: getRealmQueryParameter(),
    consoleAutoEnable: false,
    loadTranslations: ["app", "or"]
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
