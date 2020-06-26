import {customElement, html, LitElement, property, TemplateResult} from "lit-element";
import manager, {AssetModelUtil, Auth, Manager, OREvent, subscribe} from "@openremote/core";
import "@openremote/or-icon";
import "@openremote/or-translate";
import {IconSets} from "@openremote/or-icon";
import i18next from "i18next";

import {AssetEvent, AttributeEvent, MetaItemType, SharedEvent} from "@openremote/model";
import {getApartment1Asset} from "./util";

@customElement("or-demo")
class OrDemo extends subscribe(manager)(LitElement) {

    @property()
    protected alarmEnabled = false;

    @property()
    protected _assetId?: string;

    protected loggedInTemplate = (openremote: Manager) => html`<span>Welcome ${manager.username} </span><button @click="${() =>
        manager.logout()}">logout</button>`;

    protected loggedOutTemplate = (openremote: Manager) => html`<button @click="${() => manager.login()}">login</button>`;

    protected assetTemplate = (alarmEnabled: boolean) => html`<p><b>Alarm Enabled: </b> ${alarmEnabled}</p>`;

    protected render(): TemplateResult | void {
        return html`
            <p><b>Message:</b> ${manager.authenticated ? this.loggedInTemplate(manager) : this.loggedOutTemplate(manager)}</p>
            <br/>
            <p><b>Initialised: </b> ${manager.initialised}</p>
            <p><b>Manager Version: </b> ${manager.managerVersion}</p>
            <p><b>Authenticated: </b> ${manager.authenticated}</p>
            <p><b>Username: </b> ${manager.username}</p>
            <p><b>Roles: </b> ${manager.roles ? manager.roles.join(", ") : ""}</p>
            <p><b>Is Super User: </b> ${manager.isSuperUser()}</p>
            <p><b>Is Manager Same Origin: </b> ${manager.isManagerSameOrigin()}</p>
            <p><b>Connection Status: </b> ${manager.connectionStatus}</p>
            <p><b>Is Error: </b> ${manager.isError}</p>
            <p><b>Error:</b> ${manager.error}</p>
            <p><b>Config: </b> ${manager.config ? JSON.stringify(manager.config, null, 2) : ""}</p>
            <p><b>Console Registration: </b>${manager.console ? JSON.stringify(manager.console.registration, null, 2) : ""}</p>
            <p><b>Icon Example (Material Design icon set): </b><or-icon icon="access-point" /></p>
            <p><b>Icon Example (OR icon set): </b><or-icon icon="or:logo"></or-icon><or-icon icon="or:logo-plain"></or-icon><or-icon style="fill: #C4D600;" icon="or:marker"></or-icon></p>
            <p><b>Icon Example (dynamic Set click to add): </b><button @click="${() => this.createIconSet()}">Load</button>: <or-icon icon="test:x"></or-icon></p>
            <p><b>Translation Example: </b> <or-translate value="temperature"></or-translate>   <button @click="${() => this.toggleLanguage()}">${i18next.language}</button></p>
            <p><b>Asset Descriptors: </b>${JSON.stringify(AssetModelUtil.getAssetDescriptors())}</p>
            <p><b>Attribute Descriptors: </b>${JSON.stringify(AssetModelUtil.getAttributeDescriptors())}</p>
            <p><b>Attribute Value Descriptors: </b>${JSON.stringify(AssetModelUtil.getAttributeValueDescriptors())}</p>
            <p><b>Meta Item Descriptors: </b>${JSON.stringify(AssetModelUtil.getMetaItemDescriptors())}</p>
            <p><b>Rule State Meta Item: </b>${JSON.stringify(AssetModelUtil.getMetaItemDescriptor(MetaItemType.RULE_STATE.urn))}</p>
            `;
    }

    public set assetId(assetId: string) {
        this._assetId = assetId;
        super.assetIds = assetId ? [assetId] : undefined;
    }

    protected _onOrEvent = (event: OREvent) => {
        console.log("OR Event:" + event);
    };

    connectedCallback() {
        super.connectedCallback();
        manager.addListener(() => this._onOrEvent);
    }

    public createIconSet() {
        let testIconSet = {
            size: 100,
            icons: {
                "x": "<path d=\"m0.3125,39.74088l37.33242,0l11.53601,-37.43277l11.53601,37.43277l37.33242,0l-30.20251,23.13446l11.5366,37.43277l-30.20252,-23.13509l-30.20252,23.13509l11.53661,-37.43277l-30.20252,-23.13446z\" stroke-width='5' stroke=\"#000\"/>"
            }
        }   ;
        IconSets.addIconSet("test", testIconSet);
    }

    public toggleLanguage() {
        manager.language = i18next.language === "en" ? "nl" : "en";
        this.requestUpdate();
    }

    public onEvent(event: SharedEvent) {
        console.log("Asset Event Received:" + JSON.stringify(event, null, 2));

        if (event.eventType === "attribute") {
            const attributeEvent = event as AttributeEvent;
            if (attributeEvent.attributeState && attributeEvent.attributeState.attributeRef!.attributeName === "alarmEnabled") {
                this.alarmEnabled = attributeEvent.attributeState!.value;
            }
        }
    }
}

manager.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: false,
    realm: "building",
    configureTranslationsOptions: (options) => {
        options.lng = "nl"; // Change initial language to dutch
    }
}).then((success) => {
    if (success) {
        if (manager.authenticated) {
            getApartment1Asset().then((apartment1) => {
                if (apartment1) {
                    console.log("Apartment 1 Asset received: " + JSON.stringify(apartment1, null, 2));
                    (document.getElementById("or-demo") as OrDemo).assetId = apartment1!.id!;
                }
            });
        }
    }
});
