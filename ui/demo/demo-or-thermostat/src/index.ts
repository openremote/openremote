import '@polymer/iron-demo-helpers/demo-pages-shared-styles';
import '@polymer/iron-demo-helpers/demo-snippet';
import {html, render} from "lit-html";
import "@openremote/or-thermostat";
import {
    AssetQuery,
    AttributeEvent,
    BaseAssetQueryInclude,
    BaseAssetQueryMatch,
    EventSubscription,
    Asset
} from "@openremote/model";
import rest from "@openremote/rest";
import openremote, {Auth, Manager, OREvent} from "@openremote/core";
import {getApartment1Asset} from "../../demo-core/src/util";

async function initApartment1Asset(): Promise<string|undefined> {
    let query: AssetQuery = {
        name: {
            predicateType: "string",
            match: BaseAssetQueryMatch.EXACT,
            value: "Living Room"
        },
        parent: {
            name: "Apartment 1"
        },
        type: {
            predicateType: "string",
            match: BaseAssetQueryMatch.EXACT,
            value: "urn:openremote:asset:room"
        },
        select: {
            include: BaseAssetQueryInclude.ONLY_ID_AND_NAME
        }
    };

    let response = await rest.api.AssetResource.queryAssets(query);
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

openremote.addListener((event: OREvent) => {
    console.log("OR Event:" + event);

    if(event === OREvent.EVENTS_CONNECTED) {
        initApartment1Asset().then(assetId => {
            if (assetId) {
                refreshUI(assetId);
            }
        });
    }
});

openremote.init({
    auth: Auth.KEYCLOAK,
    autoLogin: true,
    keycloakUrl: "https://localhost/auth",
    managerUrl: "https://localhost",
    realm: "tenantA"
})
    .then(getApartment1Asset)
    .then((apartment1) => {

        if (apartment1) {
            console.log(apartment1);
        }
    });

