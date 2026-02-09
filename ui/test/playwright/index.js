import manager from "@openremote/core";
import { thingAssetInfo, metaItemDescriptors, valueDescriptors } from "@openremote/test/data";

import i18next from "i18next";
import HttpBackend from "i18next-http-backend";

import { IconSets, OrIconSet, createMdiIconSet, createSvgIconSet } from "@openremote/or-icon";
import { AssetEventCause, AssetModelUtil, ClientRole } from "@openremote/model";

// Import theme package for importing type definitions
import "@openremote/theme";
// Import theme CSS file as string to use within Lit
import themeCss from "@openremote/theme/default.css";

// Apply theme to the Manager app
const style = document.createElement("style");
style.id = "orDefaultTheme";
style.textContent = themeCss;
document.head.appendChild(style);

IconSets.addIconSet("mdi", createMdiIconSet(""));
IconSets.addIconSet("or", createSvgIconSet(OrIconSet.size, OrIconSet.icons));

window._i18next = i18next.use(HttpBackend);

/**
 * Mimics subscribing to asset events without connecting to an actual WebSocket.
 * @param {string[] | AttributeRef[] | undefined} ids - The asset ids to consider
 * @param {boolean} requestCurrentValues - Not implemented
 * @param {(event: import("@openremote/model").AssetEvent) => void} callback - Calls the `_onEvent` of a subscribed component
 * @returns {string} The subscriptionId
 */
function subscribeAssetEvents(ids, requestCurrentValues, callback) {
    if (window._assets && window._assets.length) {
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
        consoleAutoEnable: false,
    };
    manager._events = {
        subscribeAssetEvents,
        subscribeAttributeEvents: async () => "",
        subscribeStatusChange: () => null,
        unsubscribe: async () => "",
        unsubscribeStatusChange: () => null,
    };
    // Similar to `manager.doDescriptorsInit`, but without requesting the API
    AssetModelUtil._assetTypeInfos = [thingAssetInfo];
    AssetModelUtil._metaItemDescriptors = Object.values(metaItemDescriptors);
    AssetModelUtil._valueDescriptors = Object.values(valueDescriptors);

    return true;
};

manager.init({}).then((success) => {
    window._initialized = success;
});
