import '@polymer/iron-demo-helpers/demo-pages-shared-styles';
import '@polymer/iron-demo-helpers/demo-snippet';
import {html, render} from "lit-html";
import "@openremote/or-thermostat";
import {
    AssetQuery,
    AttributeEvent,
    AssetQueryInclude,
    AssetQueryMatch,
    EventSubscription,
    Asset
} from "@openremote/model";
import manager, {Auth, Manager, OREvent} from "@openremote/core";

async function initApartment1Asset(): Promise<string|undefined> {
    let query: AssetQuery = {
        name: {
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "Living Room"
        },
        parent: {
            name: "Apartment 1"
        },
        type: {
            predicateType: "string",
            match: AssetQueryMatch.EXACT,
            value: "RoomAsset"
        }
    };

    let response = await manager.rest.api.AssetResource.queryAssets(query);
    let assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the 'Apartment 1' asset");
        return undefined;
    }

    return assets[0].id;
}

let thermostatTemplate = (assetId: string) => html `
  <or-thermostat assetId="${assetId}"/>
`;

async function refreshUI(assetId:string) {
    render(thermostatTemplate(assetId), document.getElementById("thermostat")!);
}

manager.addListener((event: OREvent) => {
    console.log("OR Event:" + event);

    if(event === OREvent.EVENTS_CONNECTED) {
        initApartment1Asset().then(assetId => {
            if (assetId) {
                refreshUI(assetId);
            }
        });
    }
});

manager.init({
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    keycloakUrl: "https://localhost/auth",
    managerUrl: "https://localhost",
    realm: "building"
});
