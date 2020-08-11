import {css, customElement, html, property} from "lit-element";
import {AppStateKeyed, Page} from "../index";
import {NavigationControl} from "mapbox-gl";
import {EnhancedStore} from "@reduxjs/toolkit";
import manager from "@openremote/core";
import { JsonRulesetDefinition } from "@openremote/model";
import { getGeoNotificationsFromRulesSet } from "@openremote/core/dist/util";
import { OrMap, OrMapMarkerClickedEvent, OrMapClickedEvent } from "@openremote/or-map";

export interface GeofencesConfig {
}

export function pageMobileGeofencesProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: GeofencesConfig) {
    return {
        routes: [
            "geofences"
        ],
        pageCreator: () => {
            const page = new PageMobileGeofences(store);
            if(config) page.config = config;
            return page;
        }
    };
}
const QUERY_VIEW = new URLSearchParams(window.location.search).get("view");
const APP_ICON_POSITION = new URLSearchParams(window.location.search).get("appIconPosition");
@customElement("page-mobile-geofences")
class PageMobileGeofences<S extends AppStateKeyed> extends Page<S> {

    
    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;     
                align-items: center;
                justify-content: center;     
                flex-direction: column;           
            }

            .header {
                position: fixed;
                z-index: 99999; 
                width: auto;
                top: 0;
                right: 0;
            }

            .d-flex {
                display: flex;
                flex-direction: column;
            }

            .right {
                align-items: flex-end;
            }

            .flex {
                flex: 1;
            }

            .list-container {
                width: 100vw;
                height: 100vh;
            }

            .list-item {
                display: flex;
                flex-direction: row;
                text-decoration: none;
                padding: 20px;
                color: var(--or-app-color3);
                border-bottom: 1px solid var(--or-app-color3);;
            }

            h3, p {
                margin: 0;
            }

            h3 {
                color: var(--or-app-color4);
            }
            
            .button {
                cursor: pointer;
                background-color: var(--or-app-color2);
                color: var(--or-app-color4);
                --or-icon-fill: var(--or-app-color3);
                width: 42px;
                height: 42px;
                align-items: center;
                justify-content: center;  
                text-align: center;
                border-radius: 30px;
                margin: 10px;
                -webkit-box-shadow: 0 2px 4px rgba(0,0,0,0.16), 0 2px 4px rgba(0,0,0,0.19);
                -moz-box-shadow: 0 2px 4px rgba(0,0,0,0.16), 0 2px 4px rgba(0,0,0,0.19);
                box-shadow: 0 2px 4px rgba(0,0,0,0.16), 0 2px 4px rgba(0,0,0,0.19);
            }

            .marker-tooltip {
                position: absolute;
                bottom: 0;
                right: 0;
                margin: 15px;
                padding: 20px;
                background-color: var(--or-app-color2);
                color: var(--or-app-color3);
                text-decoration: none;
                max-width: 260px;
                
                -webkit-box-shadow: 0 2px 4px rgba(0,0,0,0.16), 0 2px 4px rgba(0,0,0,0.19);
                -moz-box-shadow: 0 2px 4px rgba(0,0,0,0.16), 0 2px 4px rgba(0,0,0,0.19);
                box-shadow: 0 2px 4px rgba(0,0,0,0.16), 0 2px 4px rgba(0,0,0,0.19);
                
                z-index: 99999;
                
            }
            
            .marker-tooltip.bottom_left {
                top: auto;
                bottom: 0;
                right: auto;
                left: 0;
            }

            .marker-tooltip.bottom_right {
                top: auto;
                bottom: 0;
                right: 0;
                left: auto;
            }

            .marker-tooltip.top_left {
                top: 0;
                bottom: auto;
                right: auto;
                left: 0;
            }

            .marker-tooltip.top_right {
                top: 0;
                bottom: auto;
                right: 0;
                left: auto;
            }

            or-map-marker[active] {
                --or-map-marker-width: 60px;
                --or-map-marker-height: 60px;
            }

         

          
            @supports(padding:max(0px)) {
                body, .header, footer {
                    padding-top: min(0vmin, env(safe-area-inset-top));
                    padding-bottom: min(0vmin, env(safe-area-inset-bottom));
                }
            }
            .list-container,
            .header {
                padding-top: env(safe-area-inset-top); /* Apply safe area */
                padding-bottom: env(safe-area-inset-bottom);
            }
            
        `;
    }

    @property({type: String})
    view =  QUERY_VIEW ? QUERY_VIEW : "map";

    @property({type: Array})
    mapItems = [];

    @property({type: Array})
    listItems = [];

    @property({type: Object})
    activeItem;

    get name(): string {
        return "mobile-geofences";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
        this.getGeoNotifications();
    }
    
    public connectedCallback() {
        super.connectedCallback();
        this.addEventListener(OrMapMarkerClickedEvent.NAME, this.onMapMarkerClick);
        this.addEventListener(OrMapClickedEvent.NAME, this.onMapClick);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this.onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this.onMapClick);
    }

    protected onMapMarkerClick(evt: OrMapMarkerClickedEvent) {
        const marker:any = evt.detail.marker;
        this.activeItem = marker.marker;
    }

    protected onMapClick(e: OrMapClickedEvent) {
        this.activeItem = null;
    }

    public stateChanged(state: S) {
    }


    @property()
    public config?: GeofencesConfig;

    protected render() {
        const controls = [new NavigationControl({showCompass: false, showZoom: false})];
        return html`
            ${this.view === 'map' ? html`
                    <div class="header">
                        <div class="d-flex right">
                            <a class="d-flex button" @click="${() => history.back()}"><or-icon icon="close"></or-icon></a>
                            <a class="d-flex button" @click="${() => this.view = "list"}"><or-icon icon="view-list"></or-icon></a>
                            <a class="d-flex location-button button" @click="${this.getLocation}"><or-icon icon="crosshairs-gps"></or-icon></a>
                        </div>
                    </div>
                    <or-map id="vector" class="or-map" .controls="${controls}" type="VECTOR" style="height: 100%; width: 100%;">
                        ${this.mapItems.map((marker: any) => {
                        return html`
                            <or-map-marker ?active="${JSON.stringify(this.activeItem) === JSON.stringify(marker) }" icon="information" .marker="${marker}" lat="${marker.predicate.lat}" lng="${marker.predicate.lng}"></or-map-marker>
                        `})}
                    </or-map>
                    ${this.activeItem ? html`
                        <a class="marker-tooltip ${manager.consoleAppConfig.appIconPosition ? manager.consoleAppConfig.appIconPosition.toLowerCase()  : ""}" href="${this.createUrl(this.activeItem.notification.action)}">
                            <div class="flex marker-tooltip-inner" >
                                    <div style="flex-direction:row;" class="d-flex">
                                    <div class="flex">
                                        <h3>${this.activeItem.notification.title}</h3>
                                        <p>${this.activeItem.notification.body}</p>
                                    </div>
                                    <div style="align-items: center; justify-content: center; display: flex;">
                                        <or-icon style="margin-left: 20px;" icon="chevron-right"></or-icon>
                                    </div>
                                </div>
                            </div>
                        </a>
                    ` : ``}
            ` : ``}
           
           ${this.view === 'list' ? html`
                <div class="header">
                    <div class="d-flex right">
                        <a class="d-flex button" @click="${() => history.back()}"><or-icon icon="close"></or-icon></a>
                        <a class="d-flex button" @click="${() => this.view = "map"}"><or-icon icon="map"></or-icon></a>
                    </div>
                </div>
                <div class="d-flex flex list-container">
                    ${this.listItems.map((item: any) => {
                        return html`
                        <a class="list-item" href="${this.createUrl(item.notification.action)}">
                            <div class="flex">
                                <h3>${item.notification.title}</h3>
                                <p>${item.notification.body}</p>
                            </div>
                            <div style="align-items: center; justify-content: center; display: flex;">
                                <or-icon style="margin-left: 20px;" icon="chevron-right"></or-icon>
                            </div>
                        </a>
                    `})}
                </div>
            ` : ``}

        `;
    }


    protected checkPeriode(datetime){
        const today = new Date().getTime();
        const startDate = new Date(datetime.start).getTime();
        const endDate = new Date(datetime.end).getTime();

        if((today - startDate) > 0 && (today - endDate) < 0) {
            return true;
        } else {
            return false;
        }
    }

    protected getGeoNotifications() {
        manager.rest.api.RulesResource.getTenantRulesets(manager.config.realm, {fullyPopulate: true}).then((response: any) => {
            const mapItemDefinition: JsonRulesetDefinition = {
                rules: []
            };

            const listItemtDefinition: JsonRulesetDefinition = {
                rules: []
            };
            if (response && response.data) {
                response.data.forEach((rulesSet) => {
                    let parsedRules = JSON.parse(rulesSet.rules);
                    // TODO enable this when custom validity en meta items are availible
                    // const datetime = rulesSet.meta.validity;
                    // if(this.checkPeriode(datetime) === false || rulesSet.enabled === false) {
                    //     return;
                    // }
                    // if(rulesSet.meta && rulesSet.meta.showOnMap) {
                    //     mapItemDefinition.rules.push(...parsedRules.rules);
                    // }
                    // if(rulesSet.meta && rulesSet.meta.showOnList) {
                    //     listItemtDefinition.rules.push(...parsedRules.rules);
                    // }
                    mapItemDefinition.rules.push(...parsedRules.rules);
                    listItemtDefinition.rules.push(...parsedRules.rules);

                });
            }
            this.mapItems = getGeoNotificationsFromRulesSet(mapItemDefinition);
            this.listItems = getGeoNotificationsFromRulesSet(listItemtDefinition);
        }).catch((reason) => {
            console.log("Error:" + reason);
            const rulesetDefinition: JsonRulesetDefinition = {
                rules: []
            };
            this.mapItems = getGeoNotificationsFromRulesSet(rulesetDefinition);
            this.listItems = getGeoNotificationsFromRulesSet(rulesetDefinition);
        });
    }

    protected createUrl(action) {
        if(action){
            let url = action.url;

            if(action.openInBrowser) {
                url = url.replace("https", "webbrowser");
            }

            return url;
        }
    }

    protected getLocation() {
        const map = this.shadowRoot.querySelector('.or-map');
        const vectorMap = map as OrMap;

        manager.console.sendProviderMessage({provider: 'geofence', action: "GET_LOCATION"}, true).then(response => {
            if (response.data) {
                vectorMap.flyTo({lng: response.data.longitude, lat: response.data.latitude});
            }
        });
    }
}
