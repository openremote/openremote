import {Action, ActionCreator} from "redux";
import {ThunkAction} from "redux-thunk";
import {RootState} from "../store";

export const UPDATE_PAGE = "UPDATE_PAGE";
export const UPDATE_RULE = "UPDATE_RULE";
export const RESOLVE_APP = "RESOLVE_APP";
export const UPDATE_DRAWER = "UPDATE_DRAWER";
export const SET_SCROLL_TOP = "SET_SCROLL_TOP";
export const SET_ACTIVE_ASSET = "SET_ACTIVE_ASSET";


export interface AppActionUpdatePage extends Action<typeof UPDATE_PAGE> {
    page: string;
}

export interface AppActionSetActiveAsset extends Action<typeof SET_ACTIVE_ASSET> {
    assetId: string;
}

export interface AppActionSetScrollTop extends Action<typeof SET_SCROLL_TOP> {
    top: number;
}

export interface AppActionUpdateRule extends Action<typeof UPDATE_RULE> {
    id: string;
}

export interface AppActionResolveApp extends Action<typeof RESOLVE_APP> {
    resolved: boolean;
}

export interface AppActionUpdateDrawer extends Action<typeof UPDATE_DRAWER> {
    opened: boolean;
}

export type AppAction =
    AppActionUpdatePage
    | AppActionSetActiveAsset
    | AppActionSetScrollTop
    | AppActionUpdateRule
    | AppActionUpdateDrawer
    | AppActionResolveApp;

type ThunkResult = ThunkAction<void, RootState, undefined, AppAction>;

export const setScrollTop: ActionCreator<ThunkResult> = (top: number) => (dispatch) => {
    dispatch({
        type: SET_SCROLL_TOP,
        top
    });
};

export const setActiveAsset: ActionCreator<ThunkResult> = (assetId: string) => (dispatch) => {
    dispatch({
        type: SET_ACTIVE_ASSET,
        assetId
    });
};

export const updatePage: ActionCreator<ThunkResult> = (page: string) => (dispatch) => {
    dispatch({
        type: UPDATE_PAGE,
        page
    });

    // Close the drawer - in case the *path* change came from a link in the drawer.
    dispatch(updateDrawer(false));
};


export const updateRule: ActionCreator<ThunkResult> = (id: string) => (dispatch) => {
    dispatch({
        type: UPDATE_RULE,
        id
    });
};

export const resolveApp: ActionCreator<ThunkResult> = (resolved: boolean) => (dispatch) => {
    dispatch({
        type: RESOLVE_APP,
        resolved
    });
};

export const updateDrawer: ActionCreator<ThunkResult> = (opened: boolean) => (dispatch) => {
    dispatch({
        type: UPDATE_DRAWER,
        opened
    });
};
