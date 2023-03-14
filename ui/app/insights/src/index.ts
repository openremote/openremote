import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {appReducer, getRealmQueryParameter, OrApp, PageProvider} from "@openremote/or-app";
import {pageInsightsProvider} from "./pages/page-insights";
import {Auth} from "@openremote/model";


const rootReducer = combineReducers({
    app: appReducer
});

export const store = configureStore({
    reducer: rootReducer
});

const orApp = new OrApp(store);

// Configure manager connection and i18next settings
orApp.managerConfig = {
    auth: Auth.KEYCLOAK,
    loadTranslations: ["app", "or"],
    realm: getRealmQueryParameter()
};

export const DefaultPagesConfig: PageProvider<any>[] = [
    pageInsightsProvider(store)
];

// Configure app pages and per realm styling/settings
orApp.appConfig = {
    pages: [...DefaultPagesConfig],
};

document.body.appendChild(orApp);
