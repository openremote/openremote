import {css, html, PropertyValues} from "lit";
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
    OrMapLegendEvent,
    OrMapLoadedEvent
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
    locatedAssets: AssetWithLocation[];
    unlocatedAssets: Asset[];
    currentAssetId: string;
}

export interface MapStateKeyed extends AppStateKeyed {
    map: MapState;
}

const INITIAL_STATE: MapState = {
    locatedAssets: [],
    unlocatedAssets: [],
    currentAssetId: undefined
};

const pageMapSlice = createSlice({
    name: "pageMap",
    initialState: INITIAL_STATE,
    reducers: {
        assetEventReceived(state: MapState, action: PayloadAction<AssetEvent>) {
            if (action.payload.cause === AssetEventCause.CREATE) {
                if (MapUtil.isAssetWithLocation(action.payload.asset)) {
                    state.locatedAssets.push(action.payload.asset);
                } else {
                    state.unlocatedAssets.push(action.payload.asset);
                }
            } else if (action.payload.cause === AssetEventCause.DELETE) {
                const index = state.locatedAssets.findIndex(asset => asset.id === action.payload.asset.id);
                if (index > -1) state.locatedAssets.splice(index, 1);
                else {
                    const index = state.unlocatedAssets.findIndex(asset => asset.id === action.payload.asset.id);
                    if (index > -1) state.unlocatedAssets.splice(index, 1);
                }
            }
            return state;
        },
        attributeEventReceived(state: MapState, action: PayloadAction<[string[], AttributeEvent]>) {
            const attrsOfInterest = action.payload[0];
            const attrEvent = action.payload[1];
            const attrName = attrEvent.ref.name;

            // Only react if attribute is an attribute of interest
            if (!attrsOfInterest.includes(attrName)) {
                return;
            }

            const locatedAssets = state.locatedAssets;
            const unlocatedAssets = state.unlocatedAssets;
            const assetId = attrEvent.ref.id;

            const located = locatedAssets.findIndex((a) => a.id === assetId);
            if (located > -1) {
                const asset = Util.updateAsset({ ...locatedAssets[located] }, attrEvent);
                if (MapUtil.isAssetWithLocation(asset)) {
                    locatedAssets[located] = asset;
                } else {
                    locatedAssets.splice(located, 1);
                    unlocatedAssets.push(asset);
                }
            } else {
                const unlocated = unlocatedAssets.findIndex((a) => a.id === assetId);
                if (unlocated > -1) {
                    const asset = Util.updateAsset({ ...unlocatedAssets[unlocated] }, attrEvent);
                    if (MapUtil.isAssetWithLocation(asset)) {
                        unlocatedAssets.splice(unlocated, 1);
                        locatedAssets.push(asset);
                    } else {
                        unlocatedAssets[unlocated] = asset;
                    }
                }
            }

            return state;
        },
        setAssets(state, action: PayloadAction<Asset[]>) {
            const locatedAssets = [], unlocatedAssets = [];
            for (const asset of action.payload) {
                if (MapUtil.isAssetWithLocation(asset)) {
                    locatedAssets.push(asset);
                } else {
                    unlocatedAssets.push(asset);
                }
            }
            return { ...state, locatedAssets, unlocatedAssets };
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
          :host {
              display: flex;
          }

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
                flex: 1 1 auto;
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
    protected _assets: AssetWithLocation[] = [];

    @state()
    protected _currentAsset?: Asset;

    @state()
    protected _assetTypes: string[] = [];

    @state()
    protected _excludedTypes: string[] = [];

    @state()
    protected _assetsOnScreen: AssetWithLocation[] = [];

    protected _locatedAssetSelector = (state: MapStateKeyed) => state.map.locatedAssets;
    protected _unlocatedAssetSelector = (state: MapStateKeyed) => state.map.unlocatedAssets;
    protected _paramsSelector = (state: MapStateKeyed) => state.app.params;
    protected _realmSelector = (state: MapStateKeyed) => state.app.realm || manager.displayRealm;

    protected _assetSubscriptionId: string;
    protected _attributeSubscriptionId: string;

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
                this._map?.addAssets(this._assets);

                const assetSubscriptionId = await manager.events.subscribeAssetEvents(undefined, false, (event) => {
                    this._store.dispatch(assetEventReceived(event));
                    switch (event.cause) {
                        case "DELETE": this._map?.removeAssets([event.asset.id]); break;
                        case "CREATE":
                            if (MapUtil.isAssetWithLocation(event.asset) && !this._excludedTypes.includes(event.asset?.type)) {
                                this._map?.addAsset(event.asset);
                            }
                            break;
                    }
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    manager.events.unsubscribe(assetSubscriptionId);
                    return;
                }

                this._assetSubscriptionId = assetSubscriptionId;

                const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(undefined, false, (event) => {
                    const interested = attrsOfInterest.includes(event.ref.name);
                    let asset: Asset | undefined;
                    if (interested) {
                        const assets = this._unlocatedAssetSelector(this.getState());
                        asset = assets.find(asset => asset.id === event.ref.id);
                        // Update the attribute if the asset already has a location
                        // assuming the asset is in the located asset state
                        if (!asset) {
                            this._map?.updateAttribute(event);
                        }
                    }
                    this._store.dispatch(attributeEventReceived([attrsOfInterest, event]));
                    // Add the asset after map state has been updated
                    if (interested && asset && !this._excludedTypes.includes(asset.type)) {
                        this._map?.addAsset(this._assets.find(asset => asset.id === event.ref.id));
                    }
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
        this._map?.removeAllAssets();
    };

    protected getRealmState = createSelector(
      [this._realmSelector],
      async (realm) => {
          this._excludedTypes = [];
          this.unsubscribeAssets();
          this.subscribeAssets(realm);
          this._map?.refresh();
      }
    )

    protected _getMapAssets = createSelector(
      [this._locatedAssetSelector],
      (assets) => {
          return assets;
      });

    protected _getCurrentAsset = createSelector(
      [this._locatedAssetSelector, this._paramsSelector],
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

    shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has('_assets')) {
            const newTypes = [];
            for (const { type } of this._assets) {
                if (!newTypes.includes(type)) newTypes.push(type);
            }
            this._assetTypes = newTypes;
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected render() {
        const showLegend = this.config?.legend?.show !== false && this._assetTypes.length > 1;
        return html`
            ${this._currentAsset ? html `<or-map-asset-card .config="${this.config?.card}" .assetId="${this._currentAsset.id}" .markerconfig="${this.config?.markers}"></or-map-asset-card>` : ``}

            ${showLegend ? html`<or-map-legend .assetTypes="${this._assetTypes}" .excludedTypes="${this._excludedTypes}" @or-map-legend-changed="${this._onMapLegendChanged}"></or-map-legend>` : null}

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
        this.addEventListener(OrMapLoadedEvent.NAME, this._onMapLoaded);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this._onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this._onMapClick);
        this.removeEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
        this.removeEventListener(OrMapLoadedEvent.NAME, this._onMapLoaded);
        this.unsubscribeAssets();
    }

    stateChanged(state: MapStateKeyed) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
        this.getRealmState(state);
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

    protected _onMapLoaded(e: OrMapLoadedEvent) {
        this._map?.addAssets(this._assets);
    }

    protected _onMapLegendChanged(e: OrMapLegendEvent) {
        if (this._map) {
            this._excludedTypes = e.detail;
            const assetsToAdd = [];
            const idsToRemove = [];
            for (const asset of this._assets) {
                if (this._excludedTypes.includes(asset.type)) {
                    idsToRemove.push(asset.id);
                } else {
                    assetsToAdd.push(asset);
                }
            }
            this._map.removeAssets(idsToRemove);
            this._map.addAssets(assetsToAdd);
        }
    }
}
