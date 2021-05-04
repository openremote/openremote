import {css, customElement, html, property, query} from "lit-element";
import {Action, createSlice, EnhancedStore, PayloadAction, ThunkAction} from "@reduxjs/toolkit";
import "@openremote/or-map";
import {
    MapAssetCardConfig,
    OrMap,
    OrMapAssetCardLoadAssetEvent,
    OrMapClickedEvent,
    OrMapMarkerAsset,
    OrMapMarkerClickedEvent
} from "@openremote/or-map";
import manager, {OREvent, Util} from "@openremote/core";
import {createSelector} from "reselect";
import {Asset, AssetEvent, AssetEventCause, AttributeEvent, GeoJSONPoint, Attribute, WellknownMetaItems, WellknownAttributes, LogicGroupOperator} from "@openremote/model";
import {getAssetsRoute} from "./page-assets";
import {Page, PageProvider, router} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";

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
            let assets = state.assets.filter((asst) => asst.id !== action.payload.asset.id);
            if (action.payload.cause !== AssetEventCause.DELETE) {
                assets.push(action.payload.asset);
            }
            return {
                ...state,
                assets: assets
            };
        },
        attributeEventReceived(state: MapState, action: PayloadAction<AttributeEvent>) {
            let assets = state.assets;
            const assetId = action.payload.attributeState.ref.id;
            const index = assets.findIndex((asst) => asst.id === assetId);
            let asset = index >= 0 ? assets[index] : null;

            if (!asset) {
                return state;
            }

            asset = Util.updateAsset(asset, action.payload);

            return {
                ...state,
                assets: [
                    ...assets.slice(0, index),
                    asset,
                    ...assets.slice(index + 1)
                ]
            };
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
                bottom: 0px;
                right: 0px;
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
                    right: 20px;
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
    protected subscribedRealm?: string;
    protected assetSubscriptionId: string;
    protected attributeSubscriptionId: string;

    protected subscribeAssets = (): ThunkAction<void, MapStateKeyed, unknown, Action<string>> => async (dispatch) => {

        const realm = manager.displayRealm;
        this.subscribedRealm = realm;

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

            if (realm !== this.subscribedRealm) {
                // Realm has changed
                return;
            }

            if (!this.isConnected) {
                return;
            }

            if (result.data) {
                const assets = result.data;

                dispatch(setAssets(assets));

                const ids = result.data.map(
                    asset => asset.id
                );

                const assetSubscriptionId = await manager.events.subscribeAssetEvents(ids, false, undefined, (event) => {
                    dispatch(assetEventReceived(event));
                });

                if (!this.isConnected || realm !== this.subscribedRealm) {
                    manager.events.unsubscribe(assetSubscriptionId);
                    return;
                }

                this.assetSubscriptionId = assetSubscriptionId;

                const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(ids, false, (event) => {
                    dispatch(attributeEventReceived(event));
                });

                if (!this.isConnected || realm !== this.subscribedRealm) {
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

    protected unsubscribeAssets = (): ThunkAction<void, MapStateKeyed, unknown, Action<string>> => (dispatch, getState) => {
        if (this.assetSubscriptionId) {
            manager.events.unsubscribe(this.assetSubscriptionId);
            this.assetSubscriptionId = undefined;
        }
        if (this.attributeSubscriptionId) {
            manager.events.unsubscribe(this.attributeSubscriptionId);
            this.attributeSubscriptionId = undefined;
        }
    };

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
        manager.addListener(this.onManagerEvent);
        this._store.dispatch(this.subscribeAssets());
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this.onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this.onMapClick);
        manager.removeListener(this.onManagerEvent);
        this._store.dispatch(this.unsubscribeAssets());
    }

    protected onManagerEvent = (event: OREvent) => {
        switch (event) {
            case OREvent.DISPLAY_REALM_CHANGED:
                this._store.dispatch(this.unsubscribeAssets());
                this._store.dispatch(this.subscribeAssets());
                break;
        }
    }

    stateChanged(state: S) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
    }

    protected onMapMarkerClick(e: OrMapMarkerClickedEvent) {
        const asset = (e.detail.marker as OrMapMarkerAsset).asset;
        router.navigate(getMapRoute(asset.id));
    }

    protected onMapClick(e: OrMapClickedEvent) {
        router.navigate(getMapRoute());
    }

    protected getCurrentAsset() {
        this._getCurrentAsset(this._store.getState());
    }

    protected onLoadAssetEvent(loadAssetEvent: OrMapAssetCardLoadAssetEvent) {
        router.navigate(getAssetsRoute(false, loadAssetEvent.detail));
    }
}
