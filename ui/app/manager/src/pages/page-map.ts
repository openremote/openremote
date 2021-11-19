import {css, html} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {createSlice, EnhancedStore, PayloadAction} from "@reduxjs/toolkit";
import "@openremote/or-map";
import {
    MapAssetCardConfig,
    OrMap,
    OrMapAssetCardLoadAssetEvent,
    OrMapClickedEvent,
    OrMapMarkerAsset,
    OrMapMarkerClickedEvent
} from "@openremote/or-map";
import manager, {Util} from "@openremote/core";
import {createSelector} from "reselect";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    Attribute,
    AttributeEvent,
    GeoJSONPoint,
    WellknownAttributes,
    WellknownMetaItems
} from "@openremote/model";
import {getAssetsRoute} from "./page-assets";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";

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
                    state.assets.push(action.payload.asset);
                }
            }

            return state;
        },
        attributeEventReceived(state: MapState, action: PayloadAction<AttributeEvent>) {
            const assets = state.assets;
            const assetId = action.payload.attributeState.ref.id;
            const index = assets.findIndex((asst) => asst.id === assetId);
            const asset = index >= 0 ? assets[index] : null;

            if (!asset) {
                return state;
            }

            if (action.payload.attributeState.deleted) {
                assets.splice(index, 1);
            } else {
                assets[index] = Util.updateAsset({...asset}, action.payload);
            }
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
    card?: MapAssetCardConfig
}

export function pageMapProvider<S extends MapStateKeyed>(store: EnhancedStore<S>, config?: PageMapConfig): PageProvider<S> {
    return {
        name: "map",
        routes: [
            "map",
            "map/:id"
        ],
        pageCreator: () => {
            const page = new PageMap(store);
            if(config) page.config = config;
            return page
        }
    };
}

export function getMapRoute(assetId?: string) {
    let route = "map";
    if (assetId) {
        route += "/" + assetId;
    }

    return route;
}

@customElement("page-map")
export class PageMap<S extends MapStateKeyed> extends Page<S> {

    static get styles() {
        // language=CSS
        return css`
           or-map-asset-card {
                height: 166px;
                position: absolute;
                bottom: 0;
                right: 0;
                width: calc(100vw - 10px);
                margin: 5px;
                z-index: 99;
            }
        
            or-map {
                display: block;
                height: 100%;
                width: 100%;
            }
        
            @media only screen and (min-width: 415px){
                or-map-asset-card {
                    position: absolute;
                    top: 20px;
                    right: 50px;
                    width: 320px;
                    margin: 0;
                    height: 400px; /* fallback for IE */
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

    @property()
    protected _assets: Asset[] = [];

    @property()
    protected _currentAsset?: Asset;

    protected _assetSelector = (state: S) => state.map.assets;
    protected _paramsSelector = (state: S) => state.app.params;
    protected _realmSelector = (state: S) => state.app.realm || manager.displayRealm;

    protected assetSubscriptionId: string;
    protected attributeSubscriptionId: string;

    protected subscribeAssets = async (realm: string) => {

        try {
            const result = await manager.rest.api.AssetResource.queryAssets({
                tenant: {
                    realm: realm
                },
                select: {
                    attributes: [WellknownAttributes.LOCATION],
                    excludeParentInfo: true,
                    excludePath: true
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
            });

            if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                // No longer connected or realm has changed
                return;
            }

            if (result.data) {
                const assets = result.data;

                this._store.dispatch(setAssets(assets));

                const assetSubscriptionId = await manager.events.subscribeAssetEvents(undefined, false, undefined, (event) => {
                    this._store.dispatch(assetEventReceived(event));
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    manager.events.unsubscribe(assetSubscriptionId);
                    return;
                }

                this.assetSubscriptionId = assetSubscriptionId;

                const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(undefined, false, (event) => {
                    this._store.dispatch(attributeEventReceived(event));
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

    get name(): string {
        return "map";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
        this.addEventListener(OrMapAssetCardLoadAssetEvent.NAME, this.onLoadAssetEvent);
    }

    protected render() {

        return html`
            
            ${this._currentAsset ? html `<or-map-asset-card .config="${this.config?.card}" .assetId="${this._currentAsset.id}"></or-map-asset-card>` : ``}
            
            <or-map id="map" class="or-map">
                ${
                    this._assets.filter((asset) => {
                        if (!asset.attributes) {
                            return false;
                        }
                        const attr = asset.attributes[WellknownAttributes.LOCATION] as Attribute<GeoJSONPoint>;
                        const showOnMap = !attr.meta || !attr.meta.hasOwnProperty(WellknownMetaItems.SHOWONDASHBOARD) || !!Util.getMetaValue(WellknownMetaItems.SHOWONDASHBOARD, attr); 
                        return showOnMap;
                    }).map((asset) => {
                        return html`
                            <or-map-marker-asset ?active="${this._currentAsset && this._currentAsset.id === asset.id}" .asset="${asset}"></or-map-marker-asset>
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

    stateChanged(state: S) {
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
