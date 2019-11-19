import Navigo from "navigo";
import manager, {Auth} from "@openremote/core";
import {store} from "./store";
import "./components/my-app";
import {resolveApp, updatePage, updateRule, setActiveAsset} from "./actions/app";

// Declare MANAGER_URL
declare var MANAGER_URL: string;

// Configure routing
export const router = new Navigo(null, true, "#!");

router.on({
    "map": (params, query) => {
        store.dispatch(setActiveAsset(null));
        store.dispatch(updatePage("map"));
    },
    "map/:id": (params, query) => {
        store.dispatch(setActiveAsset(params.id));
        store.dispatch(updatePage("map"));
    },
    "assets": (params, query) => {
        store.dispatch(setActiveAsset(null));
        store.dispatch(updatePage("assets"));
    },
    "assets/:id": (params, query) => {
        store.dispatch(setActiveAsset(params.id));
        store.dispatch(updatePage("assets", params.id));
    },
    "rules": (params, query) => {
        store.dispatch(updatePage("rules"));
    },
    "rules/:id": (params, query) => {
        store.dispatch(updateRule(params.id));
        store.dispatch(updatePage("rules"));
    },
    "*": (params, query) => {
        store.dispatch(updatePage("map"));
    }
});

function getUrlParameter(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    const regex = new RegExp("[\\?&]" + name + "=([^&#]*)");
    const results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

const realmFromPath = getUrlParameter("realm");

manager.init({
    managerUrl:  MANAGER_URL,
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    realm: realmFromPath,
    consoleAutoEnable: true,
    loadTranslations: ["app", "or"]
}).then(() => {
    if (manager.authenticated) {
        router.resolve();
        store.dispatch(resolveApp(true));
    }
});