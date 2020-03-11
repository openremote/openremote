import {Reducer} from "redux";
import {MapAction, ASSET_EVENT_RECEIVED, SET_CURRENT_ASSET_ID, ATTRIBUTE_EVENT_RECEIVED} from "../actions/map";
import {Asset, AssetEventCause} from "@openremote/model";
import {Util} from "@openremote/core";

export interface MapState {
    assets: Asset[];
    currentAssetId: string;
    assetSubscriptionId: string;
    attributeSubscriptionId: string;
}

const INITIAL_STATE: MapState = {
    assets: [],
    currentAssetId: undefined,
    assetSubscriptionId: undefined,
    attributeSubscriptionId: undefined
};

const reducer: Reducer<MapState, MapAction> = (state = INITIAL_STATE, action) => {
    switch (action.type) {

        case ASSET_EVENT_RECEIVED: {
            let assets = state.assets.filter((asst) => asst.id !== action.event.asset.id);
            if (action.event.cause !== AssetEventCause.DELETE) {
                assets.push(action.event.asset);
            }
            return {
                ...state,
                assets: assets
            };
        }
        case ATTRIBUTE_EVENT_RECEIVED: {
            let assets = state.assets;
            const assetId = action.event.attributeState.attributeRef.entityId;
            const index = assets.findIndex((asst) => asst.id === assetId);
            let asset = index >= 0 ? assets[index] : null;

            if (!asset) {
                return state;
            }

            asset = Util.updateAsset(asset, action.event);

            return {
                ...state,
                assets: [
                    ...assets.slice(0, index),
                    asset,
                    ...assets.slice(index + 1)
                ]
            };
        }
        case SET_CURRENT_ASSET_ID:
            return {
                ...state,
                currentAssetId: action.id
            };
        default:
            return state;
    }
};

export default reducer;
