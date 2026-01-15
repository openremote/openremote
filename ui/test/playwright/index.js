import manager from "@openremote/core";
import { thingAssetInfo, metaItemDescriptors, valueDescriptors } from "@openremote/test/data";

import i18next from "i18next";
import HttpBackend from "i18next-http-backend";

import { IconSets, OrIconSet, createMdiIconSet, createSvgIconSet } from "@openremote/or-icon";
import { AssetEventCause, AssetModelUtil, ClientRole } from "@openremote/model";

IconSets.addIconSet("mdi", createMdiIconSet(""));
IconSets.addIconSet("or", createSvgIconSet(OrIconSet.size, OrIconSet.icons));

window._i18next = i18next.use(HttpBackend);

/**
 * @param {string[] | AttributeRef[] | undefined} ids - The asset ids
 * @param {boolean} requestCurrentValues - Not implemented
 * @param {(event: import("@openremote/model").AssetEvent) => void} callback -
 * @returns{string} The subscriptionId
 */
function subscribeAssetEvents(ids, requestCurrentValues, callback) {
    if (window._assets && window._assets.length) {
        console.log("ids", ids, window._assets);
        const assetEvent = {
            eventType: "asset",
            asset: window._assets.find(({ id }) => id === ids[0]),
            cause: AssetEventCause.READ,
        };
        callback(assetEvent);
    } else {
        console.warn("No assets to subscribe to");
    }
    return "test"; // Currently not handling multiple subscriptions
}

// TODO: consider rewriting this to use a mock `WebSocketEventProvider` so we can test @openremote/core and other components
manager.init = async () => {
    manager._basicIdentity = {
        // token: string | undefined,
        // user: User | undefined,
        roles: [ClientRole.WRITE_ASSETS, ClientRole.WRITE_ATTRIBUTES],
    };
    manager._config = {
        clientId: "openremote",
        autoLogin: false,
        realm: undefined,
        consoleAutoEnable: false,
        loadTranslations: ["or"],
    };
    manager._events = {
        subscribeAssetEvents,
        subscribeAttributeEvents: async () => "",
        subscribeStatusChange: () => null,
        unsubscribe: async () => "",
        unsubscribeStatusChange: () => null,
    };
    console.log("init", thingAssetInfo, metaItemDescriptors, valueDescriptors);

    AssetModelUtil._assetTypeInfos = [thingAssetInfo];
    AssetModelUtil._metaItemDescriptors = Object.values(metaItemDescriptors);
    AssetModelUtil._valueDescriptors = Object.values(valueDescriptors);
    // rest._client = {
    //     AssetResource: {},
    //     AlarmResource: {},
    // };
    // rest._client._assetResource.getUserAssetLinks = async () => ({ data: [] });
    // rest._client._alarmResource.getAlarms = async () => ({ data: [] });
    // rest._client._assetResource.queryAssets = async () => ({ data: [] });

    return true;
};

manager.init({}).then((success) => {
    window._initialized = success;
});
