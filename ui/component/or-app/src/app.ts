/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface AppState {
    page: string;
    params: {[k in string]: string} | null;
    offline: boolean;
    visible: boolean;
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
    visible: true,
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
        },
        setOffline(state, action: PayloadAction<boolean>) {
            return {
                ...state,
                offline: action.payload
            }
        },
        setVisibility(state, action: PayloadAction<boolean>) {
            return {
                ...state,
                visible: action.payload
            }
        }
    }
});

export const {updatePage, updateDrawer, scrollToTop, updateRealm, setOffline, setVisibility} = appSlice.actions;
export const appReducer = appSlice.reducer;
