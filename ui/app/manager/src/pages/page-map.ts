import {css, html} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {createSlice, Store, PayloadAction} from "@reduxjs/toolkit";
import "@openremote/or-map";
import {
    MapAssetCardConfig,
    OrMap,
    OrMapAssetCardLoadAssetEvent,
    OrMapClickedEvent,
    OrMapMarkerAsset,
    OrMapMarkerClickedEvent,
    OrMapGeocoderChangeEvent,
    MapMarkerAssetConfig,
    OrMapMarkersChangedEvent,
    OrMapLegendEvent
} from "@openremote/or-map";
import manager, {Util} from "@openremote/core";
import {createSelector} from "reselect";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AssetQuery,
    AttributeEvent,
    GeoJSONPoint,
    WellknownAttributes,
    WellknownMetaItems
} from "@openremote/model";
import {getAssetsRoute, getMapRoute} from "../routes";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {GenericAxiosResponse} from "@openremote/rest";
import { ClusterConfig, Util as MapUtil, AssetWithLocation } from "@openremote/or-map";

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
                if (MapUtil.isAssetWithLocation(asset)) {
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
    legend?: {
      show: boolean
    },
    clustering?: ClusterConfig,
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

           or-map-legend {
               position: absolute;
               top: 60px;
               left: 10px;
               width: 254px;
               margin: 10px 0;
               z-index: 1;
           }
        
            or-map {
                display: block;
                height: 100%;
                width: 100%;
            }
        
            @media only screen and (min-width: 40em){
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

                or-map-legend {
                    position: absolute;
                    top: 60px;
                    left: 10px;
                    width: calc(100%);
                    max-width: 254px;
                    margin: 0;
                    height: max-content;
                    max-height: calc(100vh - 150px);
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

    @state()
    protected _assetsOnScreen: AssetWithLocation[] = [];

    protected _assetSelector = (state: MapStateKeyed) => state.map.assets;
    protected _paramsSelector = (state: MapStateKeyed) => state.app.params;
    protected _realmSelector = (state: MapStateKeyed) => state.app.realm || manager.displayRealm;

    protected _assetSubscriptionId: string;
    protected _attributeSubscriptionId: string;

    protected _assetTypes: string[] = [];
    protected _excludedTypes: string[] = [];

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

                this._updateMarkers();

                const assetSubscriptionId = await manager.events.subscribeAssetEvents(undefined, false, (event) => {
                    this._store.dispatch(assetEventReceived(event));
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    manager.events.unsubscribe(assetSubscriptionId);
                    return;
                }

                this._assetSubscriptionId = assetSubscriptionId;

                const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(undefined, false, (event) => {
                    this._store.dispatch(attributeEventReceived([attrsOfInterest, event]));
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    this._assetSubscriptionId = undefined;
                    manager.events.unsubscribe(assetSubscriptionId);
                    manager.events.unsubscribe(attributeSubscriptionId);
                    return;
                }

                this._attributeSubscriptionId = attributeSubscriptionId;
            }
        } catch (e) {
            console.error("Failed to subscribe to assets", e);
        }
    };

    protected unsubscribeAssets = () => {
        if (this._assetSubscriptionId) {
            manager.events.unsubscribe(this._assetSubscriptionId);
            this._assetSubscriptionId = undefined;
        }
        if (this._attributeSubscriptionId) {
            manager.events.unsubscribe(this._attributeSubscriptionId);
            this._attributeSubscriptionId = undefined;
        }
    };

    protected getRealmState = createSelector(
      [this._realmSelector],
      async (realm) => {
          if (this._assets.length > 0) {
              // Clear existing assets
              this._assets = [];
          }
          if (this._excludedTypes.length > 0) {
              this._excludedTypes = [];
          }

          this.unsubscribeAssets();
          this.subscribeAssets(realm).then(async () => {
              await this._map?.reload();
          });
          this._map?.refresh();
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
        this.addEventListener(OrMapAssetCardLoadAssetEvent.NAME, this._onLoadAssetEvent);
    }

    protected render() {
        const showLegend = this.config?.legend?.show !== false && this._assetTypes.length > 1;
        return html`
            ${this._currentAsset ? html `<or-map-asset-card .config="${this.config?.card}" .assetId="${this._currentAsset.id}" .markerconfig="${this.config?.markers}"></or-map-asset-card>` : ``}

            ${showLegend ? html`<or-map-legend .assetTypes="${this._assetTypes}" @or-map-legend-changed="${this._onMapLegendChanged}"></or-map-legend>` : null}

            <or-map id="map" class="or-map" .cluster="${this.config.clustering}" showGeoCodingControl @or-map-geocoder-change="${(ev: OrMapGeocoderChangeEvent) => {this._setCenter(ev.detail.geocode);}}">
                ${this._assetsOnScreen.sort((a,b) => {
                    const pointA = a.attributes[WellknownAttributes.LOCATION].value as GeoJSONPoint;
                    const pointB = b.attributes[WellknownAttributes.LOCATION].value as GeoJSONPoint;
                    if (pointA && pointB){
                        return pointB.coordinates[1] - pointA.coordinates[1];
                    }
                }).map(asset => html`
                    <or-map-marker-asset ?active="${this._currentAsset && this._currentAsset.id === asset.id}" .asset="${asset}" .config="${this.config.markers}"></or-map-marker-asset>
                `)}
            </or-map>
        `;
    }

    public connectedCallback() {
        super.connectedCallback();
        this.addEventListener(OrMapMarkerClickedEvent.NAME, this._onMapMarkerClick);
        this.addEventListener(OrMapClickedEvent.NAME, this._onMapClick);
        this.addEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this._onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this._onMapClick);
        this.removeEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
        this.unsubscribeAssets();
    }

    stateChanged(state: MapStateKeyed) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
        this.getRealmState(state);
        this._updateMarkers();
        this._map?.reload();
    }

    protected _onMapMarkerClick(e: OrMapMarkerClickedEvent) {
        const asset = (e.detail.marker as OrMapMarkerAsset).asset;
        router.navigate(getMapRoute(asset.id));
    }

    protected _onMapClick(e: OrMapClickedEvent) {
        router.navigate(getMapRoute());
    }

    protected _onMapMarkersChanged(e: OrMapMarkersChangedEvent) {
        this._assetsOnScreen = e.detail;
    }

    protected _onLoadAssetEvent(loadAssetEvent: OrMapAssetCardLoadAssetEvent) {
        router.navigate(getAssetsRoute(false, loadAssetEvent.detail));
    }

    protected _onMapLegendChanged(e: OrMapLegendEvent) {
        if (this._map) {
            this._excludedTypes = e.detail;
            this._updateMarkers();
            this._map.reload();
        }
    }

    protected _updateMarkers() {
        if (this._map) {
            this._assetTypes = [];
            this._map.cleanUpAssetMarkers();
            this._assets.forEach((asset: Asset) => {
                if (MapUtil.isAssetWithLocation(asset)) {
                    if (!this._excludedTypes.includes(asset.type)) {
                        this._map.addAssetMarker(asset)
                    }
                    if (!this._assetTypes.includes(asset.type)) {
                        this._assetTypes.push(asset.type);
                    }
                }
            });
        }
    }
}
