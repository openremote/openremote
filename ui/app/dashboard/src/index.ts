import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {
    OrApp,
    AppConfig,
    appReducer,
    getRealmQueryParameter,
    PageProvider} from "@openremote/or-app";
import {pageDashboardProvider} from "./pages/page-dashboard";



const rootReducer = combineReducers({
    app: appReducer
});

type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
    reducer: rootReducer
});

const orApp = new OrApp(store);

// Configure manager connection and i18next settings
orApp.managerConfig = {
    realm: getRealmQueryParameter(),
    autoLogin: false
};

export const DefaultPagesConfig: PageProvider<any>[] = [
    pageDashboardProvider(store)
];

// Configure app pages and per realm styling/settings
orApp.appConfig = {
    pages: [...DefaultPagesConfig],
};

document.body.appendChild(orApp);
