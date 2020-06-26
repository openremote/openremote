import {css, customElement, html, property, TemplateResult, unsafeCSS} from "lit-element";
import "@openremote/or-asset-tree";
import "@openremote/or-asset-viewer";
import {ViewerConfig} from "@openremote/or-asset-viewer";
import {OrAssetTreeSelectionChangedEvent} from "@openremote/or-asset-tree";
import {DefaultBoxShadow} from "@openremote/core";
import {AppStateKeyed, Page, router} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export interface PageAssetsConfig {
    viewer: ViewerConfig;
}

export function pageAssetsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config: PageAssetsConfig = PAGE_ASSETS_DEFAULT_CONFIG) {
    return {
        routes: [
            "assets",
            "assets/:id"
        ],
        pageCreator: () => {
            const page = new PageAssets(store);
            if (config) {
                page.config = config;
            }
            return page;
        }
    };
}

export const PAGE_ASSETS_DEFAULT_CONFIG: PageAssetsConfig = {
    viewer: {
        historyConfig: {
            table: {
                attributeNames: {
                    // TODO: These need to move to custom project
                    // "gunshotEvent": {
                    //     columns: [
                    //         {
                    //             header: "area",
                    //             type: "prop",
                    //             path: "$.content.area",
                    //             styles: {
                    //                 width: "20%"
                    //             }
                    //         },
                    //         {
                    //             header: "location",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].position.coordinates",
                    //             styles: {
                    //                 width: "30%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (Array.isArray(value)) {
                    //                     return "[" + Number(value[0]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + ", " + Number(value[1]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + "]"
                    //                 }
                    //             }
                    //         },
                    //         {
                    //             header: "intensity",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].intensity",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 return Number(value).toLocaleString(undefined, {maximumFractionDigits: 0})
                    //             }
                    //         },
                    //         {
                    //             header: "timestamp",
                    //             type: "prop",
                    //             path: "$.content.time",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (value) {
                    //                     return moment(value).format("L HH:mm:ss");
                    //                 }
                    //             }
                    //         }
                    //     ]
                    // },
                    // "aggressionEvent": {
                    //     columns: [
                    //         {
                    //             header: "area",
                    //             type: "prop",
                    //             path: "$.content.area",
                    //             styles: {
                    //                 width: "20%"
                    //             }
                    //         },
                    //         {
                    //             header: "location",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].position.coordinates",
                    //             styles: {
                    //                 width: "30%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (Array.isArray(value)) {
                    //                     return "[" + Number(value[0]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + ", " + Number(value[1]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + "]"
                    //                 }
                    //             }
                    //         },
                    //         {
                    //             header: "intensity",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].intensity",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 return Number(value).toLocaleString(undefined, {maximumFractionDigits: 0})
                    //             }
                    //         },
                    //         {
                    //             header: "timestamp",
                    //             type: "prop",
                    //             path: "$.content.time",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (value) {
                    //                     return moment(value).format("L HH:mm:ss");
                    //                 }
                    //             }
                    //         }
                    //     ]
                    // },
                    // "alarmEvent": {
                    //     columns: [
                    //         {
                    //             header: "area",
                    //             type: "prop",
                    //             path: "$.content.area",
                    //             styles: {
                    //                 width: "20%"
                    //             }
                    //         },
                    //         {
                    //             header: "location",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].position.coordinates",
                    //             styles: {
                    //                 width: "30%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (Array.isArray(value)) {
                    //                     return "[" + Number(value[0]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + ", " + Number(value[1]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + "]"
                    //                 }
                    //             }
                    //         },
                    //         {
                    //             header: "intensity",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].intensity",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 return Number(value).toLocaleString(undefined, {maximumFractionDigits: 0})
                    //             }
                    //         },
                    //         {
                    //             header: "timestamp",
                    //             type: "prop",
                    //             path: "$.content.time",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (value) {
                    //                     return moment(value).format("L HH:mm:ss");
                    //                 }
                    //             }
                    //         }
                    //     ]
                    // },
                    // "intensityEvent": {
                    //     columns: [
                    //         {
                    //             header: "area",
                    //             type: "prop",
                    //             path: "$.content.area",
                    //             styles: {
                    //                 width: "20%"
                    //             }
                    //         },
                    //         {
                    //             header: "location",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].position.coordinates",
                    //             styles: {
                    //                 width: "30%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (Array.isArray(value)) {
                    //                     return "[" + Number(value[0]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + ", " + Number(value[1]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + "]"
                    //                 }
                    //             }
                    //         },
                    //         {
                    //             header: "intensity",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].intensity",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 return Number(value).toLocaleString(undefined, {maximumFractionDigits: 0})
                    //             }
                    //         },
                    //         {
                    //             header: "timestamp",
                    //             type: "prop",
                    //             path: "$.content.time",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (value) {
                    //                     return moment(value).format("L HH:mm:ss");
                    //                 }
                    //             }
                    //         }
                    //     ]
                    // },
                    // "breakingGlassEvent": {
                    //     columns: [
                    //         {
                    //             header: "area",
                    //             type: "prop",
                    //             path: "$.content.area",
                    //             styles: {
                    //                 width: "20%"
                    //             }
                    //         },
                    //         {
                    //             header: "location",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].position.coordinates",
                    //             styles: {
                    //                 width: "30%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (Array.isArray(value)) {
                    //                     return "[" + Number(value[0]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + ", " + Number(value[1]).toLocaleString(undefined, {
                    //                         minimumFractionDigits: 5,
                    //                         maximumFractionDigits: 5
                    //                     }) + "]"
                    //                 }
                    //             }
                    //         },
                    //         {
                    //             header: "intensity",
                    //             type: "prop",
                    //             path: "$.content.triggers[0].intensity",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 return Number(value).toLocaleString(undefined, {maximumFractionDigits: 0})
                    //             }
                    //         },
                    //         {
                    //             header: "timestamp",
                    //             type: "prop",
                    //             path: "$.content.time",
                    //             styles: {
                    //                 width: "25%"
                    //             },
                    //             contentProvider: (datapoint, value, config) => {
                    //                 if (value) {
                    //                     return moment(value).format("L HH:mm:ss");
                    //                 }
                    //             }
                    //         }
                    //     ]
                    // }
                }
            }
        },
        default: {
            panels: {
                "attributes": {
                    exclude: ["location"]
                },
                "info": {
                    exclude: ["accessPublicRead"]
                }
            }
        },
        assetTypes: {
            // TODO: These need to move to custom project
            // "urn:openremote:asset:topic": {
            //     panels: {
            //         "attributes": {
            //             exclude: ["location", "status"]
            //         }
            //     }
            // },
            // "urn:openremote:asset:tcn:soilmoisture": {
            //     panels: {
            //         "attributes": {
            //             exclude: ["location", "name", "id", "code", "type"]
            //         }
            //     }
            // },
            // "urn:openremote:asset:tcn:meteo": {
            //     panels: {
            //         "attributes": {
            //             exclude: ["location", "name", "id", "code", "type"]
            //         }
            //     }
            // },
            // "urn:openremote:asset:tcn:point": {
            //     panels: {
            //         "attributes": {
            //             exclude: ["location", "name", "id", "code", "type"]
            //         }
            //     }
            // },
            "urn:openremote:asset:residence": {
                panels: {
                    "attributes": {
                        exclude: [
                            "dayScene",
                            "daySceneEnabledFRIDAY",
                            "daySceneEnabledMONDAY",
                            "daySceneEnabledTUESDAY",
                            "daySceneEnabledWEDNESDAY",
                            "daySceneEnabledTHURSDAY",
                            "daySceneEnabledSATURDAY",
                            "daySceneEnabledSUNDAY",
                            "daySceneTimeFRIDAY",
                            "daySceneTimeMONDAY",
                            "daySceneTimeTUESDAY",
                            "daySceneTimeWEDNESDAY",
                            "daySceneTimeTHURSDAY",
                            "daySceneTimeSATURDAY",
                            "daySceneTimeSUNDAY",
                            "disableSceneTimer",
                            "enableSceneTimer",
                            "eveningScene",
                            "dayScene",
                            "eveningSceneEnabledFRIDAY",
                            "eveningSceneEnabledMONDAY",
                            "eveningSceneEnabledTUESDAY",
                            "eveningSceneEnabledWEDNESDAY",
                            "eveningSceneEnabledTHURSDAY",
                            "eveningSceneEnabledSATURDAY",
                            "eveningSceneEnabledSUNDAY",
                            "eveningSceneTimeFRIDAY",
                            "eveningSceneTimeMONDAY",
                            "eveningSceneTimeTUESDAY",
                            "eveningSceneTimeWEDNESDAY",
                            "eveningSceneTimeTHURSDAY",
                            "eveningSceneTimeSATURDAY",
                            "eveningSceneTimeSUNDAY",
                            "morningScene",
                            "nightscene",
                            "morningSceneEnabledFRIDAY",
                            "morningSceneEnabledMONDAY",
                            "morningSceneEnabledTUESDAY",
                            "morningSceneEnabledWEDNESDAY",
                            "morningSceneEnabledTHURSDAY",
                            "morningSceneEnabledSATURDAY",
                            "morningSceneEnabledSUNDAY",
                            "morningSceneTimeFRIDAY",
                            "morningSceneTimeMONDAY",
                            "morningSceneTimeTUESDAY",
                            "morningSceneTimeWEDNESDAY",
                            "morningSceneTimeTHURSDAY",
                            "morningSceneTimeSATURDAY",
                            "morningSceneTimeSUNDAY",
                            "nightSceneEnabledFRIDAY",
                            "nightSceneEnabledMONDAY",
                            "nightSceneEnabledTUESDAY",
                            "nightSceneEnabledWEDNESDAY",
                            "nightSceneEnabledTHURSDAY",
                            "nightSceneEnabledSATURDAY",
                            "nightSceneEnabledSUNDAY",
                            "nightSceneTimeFRIDAY",
                            "nightSceneTimeMONDAY",
                            "nightSceneTimeTUESDAY",
                            "nightSceneTimeWEDNESDAY",
                            "nightSceneTimeTHURSDAY",
                            "nightSceneTimeSATURDAY",
                            "nightSceneTimeSUNDAY",
                            "vacationUntil",
                            "ventilationLevel",
                            "location",
                            "firstPresenceDetected",
                            "lastPresenceDetected",
                            "smartSwitchStartTimeA",
                            "smartSwitchStartTimeB",
                            "smartSwitchStartTimeC",
                            "smartSwitchEnabledA",
                            "smartSwitchEnabledB",
                            "smartSwitchEnabledC",
                            "smartSwitchStopTimeA",
                            "smartSwitchStopTimeB",
                            "smartSwitchStopTimeC",
                            "smartSwitchBeginEndA",
                            "smartSwitchBeginEndB",
                            "smartSwitchBeginEndC",
                            "smartSwitchModeA",
                            "smartSwitchModeB",
                            "smartSwitchModeC"
                        ]
                    }
                }
            }
        }
    }
};

@customElement("page-assets")
class PageAssets<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`
            
            or-asset-tree {
                align-items: stretch;
                z-index: 1;
            }
            
            .hideMobile {
                display: none;
            }
                
            or-asset-viewer {
                align-items: stretch;
                z-index: 0;
            }
            
            @media only screen and (min-width: 768px){
                or-asset-tree {
                    width: 300px;
                    min-width: 300px;
                    box-shadow: ${unsafeCSS(DefaultBoxShadow)} 
                }
                
                .hideMobile {
                    display: flex;
                }
                
                or-asset-viewer,
                or-asset-viewer.hideMobile {
                    display: initial;
                }
            }
        `;
    }

    @property()
    public config?: PageAssetsConfig;
    
    @property()
    protected _assetId;

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener(OrAssetTreeSelectionChangedEvent.NAME, this._onTreeSelectionChanged);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrAssetTreeSelectionChangedEvent.NAME, this._onTreeSelectionChanged);
    }

    protected render(): TemplateResult | void {
        const selectedIds = [this._assetId];
        return html`
              <or-asset-tree id="pageAssetTree" class="${this._assetId ? "hideMobile" : ""}" .selectedIds="${selectedIds}"></or-asset-tree>
              <or-asset-viewer class="${!this._assetId ? "hideMobile" : ""}" .config="${this.config && this.config.viewer ? this.config.viewer : PAGE_ASSETS_DEFAULT_CONFIG}" .assetId="${this._assetId}"></or-asset-viewer>
        `;
    }

    stateChanged(state: S) {
        this._assetId = state.app.params && state.app.params.id ? state.app.params.id : undefined;
    }

    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionChangedEvent) {
        const nodes = event.detail;
        if (nodes[0]) {
            router.navigate("assets/" + nodes[0].asset.id);
        } else {
            router.navigate("assets/");
        }
    }
}
