import {customElement, html, property, query, css} from "lit-element";
import {Action, createSlice, EnhancedStore, PayloadAction, ThunkAction} from "@reduxjs/toolkit";
import "@openremote/or-map";
import "@openremote/or-map/dist/or-map-asset-card";
import {ViewerConfig} from "@openremote/or-map/dist/or-map-asset-card";

import manager, {Util} from "@openremote/core";
import {OrMap, OrMapClickedEvent, OrMapMarkerAsset, OrMapMarkerClickedEvent} from "@openremote/or-map";
import {createSelector} from "reselect";
import {Asset, AssetEvent, AssetEventCause, AttributeEvent, AttributeType, MetaItemType} from "@openremote/model";
import {Page, router} from "../index";
import {AppStateKeyed} from "../app";

export interface MapState {
    assets: Asset[];
    currentAssetId: string;
    assetSubscriptionId: string;
    attributeSubscriptionId: string;
}

export interface MapStateKeyed extends AppStateKeyed {
    map: MapState;
}

const INITIAL_STATE: MapState = {
    assets: [],
    currentAssetId: undefined,
    assetSubscriptionId: undefined,
    attributeSubscriptionId: undefined
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
            const assetId = action.payload.attributeState.attributeRef.entityId;
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
        setAssetSubscriptionId(state, action: PayloadAction<string>) {
            return {
                ...state,
                assetSubscriptionId: action.payload
            };
        },
        setAttributeSubscriptionId(state, action: PayloadAction<string>) {
            return {
                ...state,
                attributeSubscriptionId: action.payload
            };
        }
    }
});

const {assetEventReceived, attributeEventReceived, setAssetSubscriptionId, setAttributeSubscriptionId} = pageMapSlice.actions;
export const pageMapReducer = pageMapSlice.reducer;

export function pageMapProvider<S extends MapStateKeyed>(store: EnhancedStore<S>, config?:ViewerConfig) {
    return {
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

const subscribeAssets = (): ThunkAction<void, MapStateKeyed, unknown, Action<string>> => async (dispatch) => {

    try {
        const result = await manager.rest.api.AssetResource.queryAssets({
            select: {
                excludeAttributes: true,
                excludeParentInfo: true,
                excludePath: true
            }
        });

        if (result.data) {
            const ids = result.data.map(
                asset => asset.id
            );

            await manager.events.subscribeAssetEvents(ids, true, (event) => {
                dispatch(assetEventReceived(event));
            });

            await manager.events.subscribeAttributeEvents(ids, false, (event) => {
                dispatch(attributeEventReceived(event));
            });
        }

    } catch (e) {
        console.error("Failed to subscribe to assets", e)
    }
};

const unsubscribeAssets = (): ThunkAction<void, MapStateKeyed, unknown, Action<string>> => (dispatch, getState) => {
    if (getState().map.assetSubscriptionId) {
        manager.events.unsubscribe(getState().map.assetSubscriptionId);
        dispatch(setAssetSubscriptionId(null));
    }
    if (getState().map.attributeSubscriptionId) {
        manager.events.unsubscribe(getState().map.attributeSubscriptionId);
        dispatch(setAttributeSubscriptionId(null));
    }
};

const mapCardConfig: ViewerConfig = {
    default: {
            exclude: ["userNotes"]
    },
    assetTypes: {
        "urn:openremote:asset:iems:weather": {
            exclude: ["location"]
        }
    }
};

@customElement("page-map")
export class PageMap<S extends MapStateKeyed> extends Page<S>  {

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
    public config?: ViewerConfig;

    @query("#map")
    protected _map?: OrMap;

    @property()
    protected _assets: Asset[] = [];

    @property()
    protected _currentAsset?: Asset;

    protected _assetSelector = (state: S) => state.map.assets;
    protected _paramsSelector = (state: S) => state.app.params;

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

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render() {

        return html`
            
            ${this._currentAsset ? html `<or-map-asset-card .config="${this.config ? this.config : mapCardConfig}" .asset="${this._currentAsset}"></or-map-asset-card>` : ``}
            
            <or-map id="map" class="or-map">
                ${
                    this._assets.filter((asset) => {
                        const attr = Util.getAssetAttribute(asset, AttributeType.LOCATION.attributeName!);
                        const showOnMapMeta = Util.getFirstMetaItem(attr, MetaItemType.SHOW_ON_DASHBOARD.urn!);
                        return showOnMapMeta && showOnMapMeta.value;
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
        this._store.dispatch(subscribeAssets());
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this.onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this.onMapClick);
        this._store.dispatch(unsubscribeAssets());
    }

    stateChanged(state: S) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
    }

    protected onMapMarkerClick(e: OrMapMarkerClickedEvent) {
        const asset = (e.detail.marker as OrMapMarkerAsset).asset;
        router.navigate("map/" + asset.id);
    }

    protected onMapClick(e: OrMapClickedEvent) {
        router.navigate('map');
    }

    protected getCurrentAsset() {
        this._getCurrentAsset(this._store.getState());
    }
}
