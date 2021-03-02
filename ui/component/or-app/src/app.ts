import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface AppState {
    page: string;
    params: {[k in string]: any} | undefined;
    offline: boolean;
    resolved: boolean,
    drawerOpened: boolean;
    scrollTop: number;
}

export interface AppStateKeyed {
    app: AppState;
}

const INITIAL_STATE: AppState = {
    page: "",
    params: undefined,
    offline: false,
    resolved: false,
    drawerOpened: false,
    scrollTop: 0,
};

export interface PageAndParams {
    page: string;
    params?: {[k in string]: any};
}

const appSlice = createSlice({
    name: "app",
    initialState: INITIAL_STATE,
    reducers: {
        updatePage(state, action: PayloadAction<PageAndParams | string>) {
            return {
                ...state,
                page: typeof action.payload === "string" ? action.payload : action.payload.page,
                params: typeof action.payload === "string" ? undefined : action.payload.params
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
        }
    }
});

export const {updatePage, updateDrawer, scrollToTop} = appSlice.actions;
export const appReducer = appSlice.reducer;
