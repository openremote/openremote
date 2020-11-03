// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

import {combineReducers, configureStore} from "@reduxjs/toolkit";
import "@openremote/or-app";
import {
    OrApp,
    AppConfig,
    appReducer,
    headerItemGatewayConnection,
    headerItemLanguage,
    headerItemLogout,
    headerItemLogs,
    headerItemMap,
    headerItemAssets,
    headerItemRules,
    headerItemAccount,
    headerItemInsights} from "@openremote/or-app";
import "@openremote/or-app/dist/pages/page-map";
import {pageMapReducer, pageMapProvider} from "@openremote/or-app/dist/pages/page-map";
import "@openremote/or-app/dist/pages/page-assets";
import {pageAssetsProvider} from "@openremote/or-app/dist/pages/page-assets";
import "@openremote/or-app/dist/pages/page-gateway";
import {pageGatewayProvider} from "@openremote/or-app/dist/pages/page-gateway";
import "@openremote/or-app/dist/pages/page-insights";
import {pageInsightsProvider} from "@openremote/or-app/dist/pages/page-insights";
import "@openremote/or-app/dist/pages/page-rules";
import {pageRulesProvider} from "@openremote/or-app/dist/pages/page-rules";
import "@openremote/or-app/dist/pages/page-logs";
import {pageLogsProvider} from "@openremote/or-app/dist/pages/page-logs";
import "@openremote/or-app/dist/pages/page-account";
import {pageAccountProvider} from "@openremote/or-app/dist/pages/page-account";

const rootReducer = combineReducers({
    app: appReducer,
    map: pageMapReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

const orApp = new OrApp(store);

const appConfig: AppConfig<RootState> = {
    pages: {
        default: pageMapProvider(store),
        assets: pageAssetsProvider(store),
        gateway: pageGatewayProvider(store),
        logs: pageLogsProvider(store),
        insights: pageInsightsProvider(store),
        rules: pageRulesProvider(store),
        account: pageAccountProvider(store)
    },
    default: {
        appTitle: "OpenRemote Demo",
        logo: require("../images/logo.png"),
        logoMobile: require("../images/logo-mobile.png"),
        header: {
            mainMenu: [
                headerItemMap(orApp),
                headerItemAssets(orApp),
                headerItemRules(orApp),
                headerItemInsights(orApp)
                // {
                //     icon: "android-messages",
                //     href: "#!messages",
                //     text: "messages",
                // }
            ],
            secondaryMenu: [
                headerItemGatewayConnection(orApp),
                headerItemLanguage(orApp),
                headerItemLogs(orApp),
                headerItemAccount(orApp),
                headerItemLogout(orApp)
                // {
                //     icon: "account-cog",
                //     value: "User management",
                //     text: "User management",
                //     isSuperUser: true
                // },
                // {
                //     icon: "tune",
                //     value: "Settings",
                //     text: "Settings"
                // }
            ]
        }
    }
};

orApp.appConfig = appConfig;
document.body.appendChild(orApp);
