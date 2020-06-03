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

const appSlice = createSlice({
    name: "app",
    initialState: INITIAL_STATE,
    reducers: {
        updatePage(state, action: PayloadAction<string>) {
            return {
                ...state,
                page: action.payload
            };
        },
        updateParams(state, action: PayloadAction<{[k in string]: any}>) {
            return {
                ...state,
                params: action.payload
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

export const {updatePage, updateParams, updateDrawer, scrollToTop} = appSlice.actions;
export const appReducer = appSlice.reducer;
