import manager from "@openremote/core";
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface AppState {
    page: string;
    params: {[k in string]: string} | null;
    offline: boolean;
    resolved: boolean,
    drawerOpened: boolean;
    scrollTop: number;
    realm?: string;
}

export interface AppStateKeyed {
    app: AppState;
}

const INITIAL_STATE: AppState = {
    page: "",
    params: null,
    offline: false,
    resolved: false,
    drawerOpened: false,
    scrollTop: 0,
    realm: undefined
};

export interface PageAndParams {
    page: string;
    params: {[k in string]: string} | null;
}

const appSlice = createSlice({
    name: "app",
    initialState: INITIAL_STATE,
    reducers: {
        updatePage(state, action: PayloadAction<PageAndParams | string>) {
            return {
                ...state,
                page: typeof action.payload === "string" ? action.payload : action.payload.page,
                params: typeof action.payload === "string" ? null : action.payload.params
            };
        },
        updateDrawer(state, action: PayloadAction<boolean>) {
            return {
                ...state,
                drawerOpened: action.payload
            };
        },
        scrollToTop(state, action: PayloadAction<number>) {
            return {
                ...state,
                scrollTop: action.payload
            };
        },
        updateRealm(state, action: PayloadAction<string>) {
            return {
                ...state,
                realm: action.payload
            }
        }
    }
});

export const {updatePage, updateDrawer, scrollToTop, updateRealm} = appSlice.actions;
export const appReducer = appSlice.reducer;
