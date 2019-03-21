import {html, render} from "lit-html";
    import {when} from "lit-html/directives/when";
import rest from "@openremote/rest";
import openremote, {Auth, Manager, OREvent} from "@openremote/core";
import "@openremote/or-icon";
import {IconSets} from "@openremote/or-icon";
import {IconSetSvg} from "@openremote/or-icon/dist/icon-set-svg";

import {
    AssetQuery,
    AttributeEvent,
    BaseAssetQueryInclude,
    BaseAssetQueryMatch
} from "@openremote/model";

let alarmEnabled = false;

let loggedInTemplate = (openremote: Manager) => html`<span>Welcome ${openremote.username} </span><button @click="${() => {
    openremote.logout()
}}">logout</button>`;

let loggedOutTemplate = (openremote: Manager) => html`<button @click="${() => {
    openremote.login()
}}">login</button>`;

let mainTemplate = (openremote: Manager) => html`
<p><b>Message:</b> ${when(openremote.authenticated, () => loggedInTemplate(openremote), () => loggedOutTemplate(openremote))}</p>
<br/>
<p><b>Initialised: </b> ${openremote.initialised}</p>
<p><b>Manager Version: </b> ${openremote.managerVersion}</p>
<p><b>Authenticated: </b> ${openremote.authenticated}</p>
<p><b>Username: </b> ${openremote.username}</p>
<p><b>Roles: </b> ${openremote.roles ? openremote.roles.join(", ") : ""}</p>
<p><b>Is Super User: </b> ${openremote.isSuperUser()}</p>
<p><b>Is Manager Same Origin: </b> ${openremote.isManagerSameOrigin()}</p>
<p><b>Connection Status: </b> ${openremote.connectionStatus}</p>
<p><b>Is Error: </b> ${openremote.isError}</p>
<p><b>Error:</b> ${openremote.error}</p>
<p><b>Config: </b> ${openremote.config ? JSON.stringify(openremote.config, null, 2) : ""}</p>
<p><b>Console Registration: </b>${openremote.console ? JSON.stringify(openremote.console.registration, null, 2) : ""}</p>
<p><b>Icon Example (Material Design icon set): </b><or-icon icon="access-point" /></p>
<p><b>Icon Example (OR icon set): </b><or-icon icon="or:logo"></or-icon><or-icon icon="or:logo-plain"></or-icon></p>
<p><b>Icon Example (dynamic Set click to add): </b><button @click="${() => {createIconSet()}}">Load</button>: <or-icon icon="test:x"></or-icon></p>
`;

let assetTemplate = (alarmEnabled: boolean) => html `
<p><b>Alarm Enabled: </b> ${alarmEnabled}</p>
`;

async function refreshUI() {
    render(mainTemplate(openremote), document.getElementById("info")!);
    render(assetTemplate(alarmEnabled), document.getElementById("asset")!);
}

function createIconSet() {
    let testIconSet = new IconSetSvg(100, {x: "<path d=\"M0,0 L100,100 M100,0 L0,100\" stroke=\"#000\"/>"});
    IconSets.addIconSet("test", testIconSet);
}

async function subscribeApartmentAttributeEvents(assetId: string) {

    let callback: (event: AttributeEvent) => void = (event) => {
        console.log("Event Received:" + JSON.stringify(event, null, 2));
        if (event.attributeState && event.attributeState.attributeRef!.attributeName === "alarmEnabled") {
            alarmEnabled = event.attributeState!.value;
            refreshUI();
        }
    };

    let subscriptionId = await openremote.events!.subscribeAttributeEvents([assetId], callback);
    console.log("Subscribed: " + subscriptionId);
}

async function initApartment1Asset(): Promise<void> {
    let query: AssetQuery = {
        name: {
            predicateType: "string",
            match: BaseAssetQueryMatch.EXACT,
            value: "Apartment 1"
        },
        type: {
            predicateType: "string",
            match: BaseAssetQueryMatch.EXACT,
            value: "urn:openremote:asset:residence"
        },
        select: {
            include: BaseAssetQueryInclude.ONLY_ID_AND_NAME
        }
    };

    let response = await rest.api.AssetResource.queryAssets(query);
    let assets = response.data;

    if (assets.length !== 1) {
        console.log("Failed to retrieve the 'Apartment 1' asset");
        return;
    }

    let apartment1 = assets[0];
    console.log("Apartment 1 Asset received: " + JSON.stringify(apartment1, null, 2));
    subscribeApartmentAttributeEvents(apartment1.id!);
}

openremote.addListener((event: OREvent) => {
    console.log("OR Event:" + event);

    switch(event) {
        case OREvent.READY:
            if (openremote.authenticated) {
                initApartment1Asset().then(refreshUI);
            }
            break;
        default:
            refreshUI();
    }
});

openremote.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: false,
    realm: "tenantA"
})
;
