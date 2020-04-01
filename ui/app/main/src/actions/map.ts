import {Action, ActionCreator} from "redux";
import {ThunkAction} from "redux-thunk";
import {RootState} from "../store";
import manager from "@openremote/core";
import {AssetEvent, AttributeEvent} from "@openremote/model";

export const ASSET_EVENT_RECEIVED = "ASSET_EVENT_RECEIVED";
export const ATTRIBUTE_EVENT_RECEIVED = "ATTRIBUTE_EVENT_RECEIVED";
export const UPDATE_ASSET_SUBSCRIPTION_ID = "UPDATE_ASSET_SUBSCRIPTION_ID";
export const UPDATE_ATTRIBUTE_SUBSCRIPTION_ID = "UPDATE_ATTRIBUTE_SUBSCRIPTION_ID";
export const SET_CURRENT_ASSET_ID = "SET_CURRENT_ASSET_ID";

export interface MapActionAssetEventReceived extends Action<typeof ASSET_EVENT_RECEIVED> {
    event: AssetEvent;
}

export interface MapActionAttributeEventReceived extends Action<typeof ATTRIBUTE_EVENT_RECEIVED> {
    event: AttributeEvent;
}

export interface MapActionUpdateAssetSubscriptionId extends Action<typeof UPDATE_ASSET_SUBSCRIPTION_ID> {
    id: string;
}

export interface MapActionUpdateAttributeSubscriptionId extends Action<typeof UPDATE_ATTRIBUTE_SUBSCRIPTION_ID> {
    id: string;
}

export interface MapActionSetCurrentAssetId extends Action<typeof SET_CURRENT_ASSET_ID> {
    id: string;
}

export type MapAction = MapActionAssetEventReceived | MapActionAttributeEventReceived | MapActionUpdateAssetSubscriptionId | MapActionUpdateAttributeSubscriptionId | MapActionSetCurrentAssetId;

type ThunkResult = ThunkAction<void, RootState, undefined, MapAction>;

export const subscribeAssets: ActionCreator<ThunkResult> = () => async (dispatch) => {

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

            let subscriptionId = await manager.events.subscribeAssetEvents(ids, true, (event) => {
                dispatch(assetEventReceived(event));
            });
            dispatch(updateAssetSubscriptionId(subscriptionId));

            subscriptionId = await manager.events.subscribeAttributeEvents(ids, false, (event) => {
                dispatch(attributeEventReceived(event));
            });
            dispatch(updateAttributeSubscriptionId(subscriptionId));
        }

    } catch (e) {
        console.error("Failed to subscribe to assets", e)
    }
};

export const assetEventReceived = (event: AssetEvent): MapActionAssetEventReceived => {
    return {
        type: ASSET_EVENT_RECEIVED,
        event: event
    }
};

export const unsubscribeAssets: ActionCreator<ThunkResult> = () => (dispatch, getState) => {
    if (getState().map.assetSubscriptionId) {
        manager.events.unsubscribe(getState().map.assetSubscriptionId);
        dispatch(
            updateAssetSubscriptionId(null)
        );
    }
    if (getState().map.attributeSubscriptionId) {
        manager.events.unsubscribe(getState().map.attributeSubscriptionId);
        dispatch(
            updateAttributeSubscriptionId(null)
        );
    }
};

export const attributeEventReceived = (event: AttributeEvent): MapActionAttributeEventReceived => {
    return {
        type: ATTRIBUTE_EVENT_RECEIVED,
        event: event
    }
};

export const updateAssetSubscriptionId = (id: string): MapActionUpdateAssetSubscriptionId => {
    return {
        type: UPDATE_ASSET_SUBSCRIPTION_ID,
        id: id
    }
};

export const updateAttributeSubscriptionId = (id: string): MapActionUpdateAttributeSubscriptionId => {
    return {
        type: UPDATE_ATTRIBUTE_SUBSCRIPTION_ID,
        id: id
    }
};

export const setCurrentAssetId = (id: string): MapActionSetCurrentAssetId => {
    return {
        type: SET_CURRENT_ASSET_ID,
        id: id
    }
};
