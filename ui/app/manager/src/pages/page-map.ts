import {css, html} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {createSlice, Store, PayloadAction} from "@reduxjs/toolkit";
// import "@openremote/or-map";
import {
    MapAssetCardConfig,
    OrMap,
    OrMapAssetCardLoadAssetEvent,
    OrMapClickedEvent,
    OrMapMarkerAsset,
    OrMapMarkerClickedEvent,
    OrMapGeocoderChangeEvent,
    MapMarkerAssetConfig
} from "@openremote/or-map";
import manager, {Util} from "@openremote/core";
import {createSelector} from "reselect";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AssetQuery,
    Attribute,
    AttributeEvent,
    GeoJSONPoint,
    WellknownAttributes,
    WellknownMetaItems
} from "@openremote/model";
import {getAssetsRoute, getMapRoute} from "../routes";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {GenericAxiosResponse} from "@openremote/rest";

export interface MapState {
    assets: Asset[];
    currentAssetId: string;
}

export interface MapStateKeyed extends AppStateKeyed {
    map: MapState;
}

const INITIAL_STATE: MapState = {
    assets: [],
    currentAssetId: undefined
};

const pageMapSlice = createSlice({
    name: "pageMap",
    initialState: INITIAL_STATE,
    reducers: {
        assetEventReceived(state: MapState, action: PayloadAction<AssetEvent>) {

            if (action.payload.cause === AssetEventCause.CREATE) {
                // Update and delete handled by attribute handler

                const asset = action.payload.asset;
                const locationAttr = asset.attributes && asset.attributes.hasOwnProperty(WellknownAttributes.LOCATION) ? asset.attributes[WellknownAttributes.LOCATION] as Attribute<GeoJSONPoint> : undefined;
                if (locationAttr && (!locationAttr.meta || locationAttr.meta && (!locationAttr.meta.hasOwnProperty(WellknownMetaItems.SHOWONDASHBOARD) || !!locationAttr.meta[WellknownMetaItems.SHOWONDASHBOARD]))) {
                    return {
                        ...state,
                        assets: [...state.assets, action.payload.asset]
                    };
                }
            }

            return state;
        },
        attributeEventReceived(state: MapState, action: PayloadAction<[string[], AttributeEvent]>) {
            const assets = state.assets;
            const attrsOfInterest = action.payload[0];
            const attrEvent = action.payload[1];
            const attrName = attrEvent.ref.name;
            const assetId = attrEvent.ref.id;
            const index = assets.findIndex((asst) => asst.id === assetId);
            const asset = index >= 0 ? assets[index] : null;

            if (!asset) {
                return state;
            }

            if (attrName === WellknownAttributes.LOCATION && attrEvent.deleted) {
                return {
                    ...state,
                    assets: [...assets.splice(index, 1)]
                };
            }

            // Only react if attribute is an attribute of interest
            if (!attrsOfInterest.includes(attrName)) {
                return;
            }

            assets[index] = Util.updateAsset({...asset}, attrEvent);
            return state;
        },
        setAssets(state, action: PayloadAction<Asset[]>) {
            return {
                ...state,
                assets: action.payload
            };
        }
    }
});

const {assetEventReceived, attributeEventReceived, setAssets} = pageMapSlice.actions;
export const pageMapReducer = pageMapSlice.reducer;

export interface PageMapConfig {
    card?: MapAssetCardConfig,
    assetQuery?: AssetQuery,
    markers?: MapMarkerAssetConfig
}

export function pageMapProvider(store: Store<MapStateKeyed>, config?: PageMapConfig): PageProvider<MapStateKeyed> {
    return {
        name: "map",
        routes: [
            "map",
            "map/:id"
        ],
        pageCreator: () => {
            const page = new PageMap(store);
            page.config = config || {};
            return page
        }
    };
}


@customElement("page-map")
export class PageMap extends Page<MapStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
           or-map-asset-card {
                height: 35vh;
                position: absolute;
                bottom: 0;
                right: 0;
                width: 100vw;
                z-index: 3;
            }

           or-map-attribute-chart {
               position: absolute;
               /*bottom: 10px;*/
               /*width: 100%;*/
               /*margin: 0;*/
               /*height: 400px; !* fallback for IE *!*/
               /*height: 100%;*/
               z-index: 4;
               /*max-height: calc(100vh - 150px);*/
           }
        
            or-map {
                display: block;
                height: 100%;
                width: 100%;
            }
        
            @media only screen and (min-width: 415px){
                or-map-asset-card {
                    position: absolute;
                    top: 10px;
                    right: 50px;
                    width: 320px;
                    margin: 0;
                    height: 400px; /* fallback for IE */
                    height: max-content;
                    max-height: calc(100vh - 150px);
                }
                or-map-attribute-chart {
                    display: flex;
                    /*max-height: 300px;*/
                    box-sizing: border-box;
                    background-clip: content-box;
                    bottom: 0;
                    width: 100%;
                    padding: 20px;
                    padding-right: 30px;
                    background-color: white;
                    height: 100%;
                    border-radius: 10px;
                    
                    max-height: 30%;
                    
                    
                    /*max-height: calc(100vh - 150px);*/
                }
            }
        `;
    }

    @property()
    public config?: PageMapConfig;

    @query("#map")
    protected _map?: OrMap;

    @state()
    protected _assets: Asset[] = [];

    @state()
    protected _currentAsset?: Asset;

    protected _assetSelector = (state: MapStateKeyed) => state.map.assets;
    protected _paramsSelector = (state: MapStateKeyed) => state.app.params;
    protected _realmSelector = (state: MapStateKeyed) => state.app.realm || manager.displayRealm;

    protected assetSubscriptionId: string;
    protected attributeSubscriptionId: string;

    protected getAttributesOfInterest(): (string | WellknownAttributes)[] {
        // Extract all label attributes configured in marker config
        let markerLabelAttributes = [];

        if (this.config && this.config.markers) {
            markerLabelAttributes = Object.values(this.config.markers)
              .filter(assetTypeMarkerConfig => assetTypeMarkerConfig.attributeName)
              .map(assetTypeMarkerConfig => assetTypeMarkerConfig.attributeName);
        }

        return [
            ...markerLabelAttributes,
            WellknownAttributes.LOCATION,
            WellknownAttributes.DIRECTION
        ];
    }

    protected subscribeAssets = async (realm: string) => {
        let response: GenericAxiosResponse<Asset[]>;
        const attrsOfInterest = this.getAttributesOfInterest();
        const assetQuery: AssetQuery = this.config && this.config.assetQuery ? this.config.assetQuery : {
            realm: {
                name: realm
            },
            select: {
                attributes: attrsOfInterest,
            },
            attributes: {
                items: [
                    {
                        name: {
                            predicateType: "string",
                            value: WellknownAttributes.LOCATION
                        },
                        meta: [
                            {
                                name: {
                                    predicateType: "string",
                                    value: WellknownMetaItems.SHOWONDASHBOARD
                                },
                                negated: true
                            },
                            {
                                name: {
                                    predicateType: "string",
                                    value: WellknownMetaItems.SHOWONDASHBOARD
                                },
                                value: {
                                    predicateType: "boolean",
                                    value: true
                                }
                            }
                        ]
                    }
                ]
            }
        };

        try {
            response = await manager.rest.api.AssetResource.queryAssets(assetQuery);

            if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                // No longer connected or realm has changed
                return;
            }

            if (response.data) {
                const assets = response.data;

                this._store.dispatch(setAssets(assets));

                const assetSubscriptionId = await manager.events.subscribeAssetEvents(undefined, false, (event) => {
                    this._store.dispatch(assetEventReceived(event));
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    manager.events.unsubscribe(assetSubscriptionId);
                    return;
                }

                this.assetSubscriptionId = assetSubscriptionId;

                const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(undefined, false, (event) => {
                    this._store.dispatch(attributeEventReceived([attrsOfInterest, event]));
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    this.assetSubscriptionId = undefined;
                    manager.events.unsubscribe(assetSubscriptionId);
                    manager.events.unsubscribe(attributeSubscriptionId);
                    return;
                }

                this.attributeSubscriptionId = attributeSubscriptionId;
            }
        } catch (e) {
            console.error("Failed to subscribe to assets", e)
        }
    };

    protected unsubscribeAssets = () => {
        if (this.assetSubscriptionId) {
            manager.events.unsubscribe(this.assetSubscriptionId);
            this.assetSubscriptionId = undefined;
        }
        if (this.attributeSubscriptionId) {
            manager.events.unsubscribe(this.attributeSubscriptionId);
            this.attributeSubscriptionId = undefined;
        }
    };

    protected getRealmState = createSelector(
      [this._realmSelector],
      async (realm) => {
          if (this._assets.length > 0) {
              // Clear existing assets
              this._assets = [];
          }
          this.unsubscribeAssets();
          this.subscribeAssets(realm);

          if (this._map) {
              this._map.refresh();
          }
      }
    )

    protected _getMapAssets = createSelector(
      [this._assetSelector],
      (assets) => {
          return assets;
      });

    protected _getCurrentAsset = createSelector(
      [this._assetSelector, this._paramsSelector],
      (assets, params) => {
          const currentId = params ? params.id : undefined;

          if (!currentId) {
              return null;
          }

          return assets.find((asset) => asset.id === currentId);
      });

    protected _setCenter(geocode: any) {
        this._map!.center = [geocode.geometry.coordinates[0], geocode.geometry.coordinates[1]];
    }

    get name(): string {
        return "map";
    }

    constructor(store: Store<MapStateKeyed>) {
        super(store);
        this.addEventListener(OrMapAssetCardLoadAssetEvent.NAME, this.onLoadAssetEvent);
    }

    protected render() {

        let currentAssetSelected: boolean = (this._currentAsset != undefined);
        let isMouseAPointer = false;
        if(this._map?._map?._mapGl != undefined){
            isMouseAPointer = this._map._map._mapGl.getCanvas().style.cursor == '';
        }
        // currentAssetSelected? console.log("render") : console.log("asset unselected");
            return html`
<!--                
                <or-map-location-history-markers .assetId="${this._currentAsset?.id}" .map="${this._map}"></or-map-location-history-markers>
            -->
                <or-map-location-history-overlay .assetId="${this._currentAsset?.id}" .map="${this._map}" ></or-map-location-history-overlay>
${currentAssetSelected ? html`
                <or-map-asset-card .config="${this.config?.card}" .assetId="${this._currentAsset.id}"
                                   .markerconfig="${this.config?.markers}" .map="${this._map}"></or-map-asset-card>
<!--                
                <or-map-location-history-overlay .assetId="${this._currentAsset.id}" .map="${this._map}" ></or-map-location-history-overlay>
                -->
${!isMouseAPointer && currentAssetSelected ? 
            html`
            
            `: `
            `}
                
<!--                <or-map-attribute-chart .assetId="${this._currentAsset.id}" .asset="${this._currentAsset}"></or-map-attribute-chart>-->
            ` : ``}
            

            <or-map id="map" class="or-map" showGeoCodingControl
                    @or-map-geocoder-change="${(ev: OrMapGeocoderChangeEvent) => {
                        this._setCenter(ev.detail.geocode);
                    }}" 
            >
                ${
                        this._assets.filter((asset) => {
                            if (!asset.attributes) {
                                return false;
                            }
                            const attr = asset.attributes[WellknownAttributes.LOCATION] as Attribute<GeoJSONPoint>;
                            return !attr.meta || !attr.meta.hasOwnProperty(WellknownMetaItems.SHOWONDASHBOARD) || !!Util.getMetaValue(WellknownMetaItems.SHOWONDASHBOARD, attr);
                        })
                                .sort((a, b) => {
                                    if (a.attributes[WellknownAttributes.LOCATION].value && b.attributes[WellknownAttributes.LOCATION].value) {
                                        return b.attributes[WellknownAttributes.LOCATION].value.coordinates[1] - a.attributes[WellknownAttributes.LOCATION].value.coordinates[1];
                                    } else {
                                        return;
                                    }
                                })
                                .map(asset => {
                                    return html`
                                        <or-map-marker-asset
                                                ?active="${this._currentAsset && this._currentAsset.id === asset.id}"
                                                .asset="${asset}"
                                                .config="${this.config.markers}"></or-map-marker-asset>
                                    `;
                                })
                }
                
            </or-map>
            
            
        `;
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
        this.unsubscribeAssets();
    }

    stateChanged(state: MapStateKeyed) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
        this.getRealmState(state);
    }

    protected onMapMarkerClick(e: OrMapMarkerClickedEvent) {
        const asset = (e.detail.marker as OrMapMarkerAsset).asset;
        router.navigate(getMapRoute(asset.id));
    }

    protected onMapClick(e: OrMapClickedEvent) {
        router.navigate(getMapRoute());
    }

    protected getCurrentAsset() {
        this._getCurrentAsset(this.getState());
    }

    protected onLoadAssetEvent(loadAssetEvent: OrMapAssetCardLoadAssetEvent) {
        router.navigate(getAssetsRoute(false, loadAssetEvent.detail));
    }
}
