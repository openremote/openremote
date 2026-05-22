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
import {HeaderConfig, Languages} from "./or-header";
import {AppStateKeyed} from "./app";
import {LitElement, TemplateResult} from "lit";
import {i18next, translate} from "@openremote/or-translate"
import {AnyAction, Store, Unsubscribe} from "@reduxjs/toolkit";
import Navigo from "navigo";

// Configure routing
export const router = new Navigo("/", {hash: true});

export interface RealmAppConfig {
    appTitle?: string;
    logo?: HTMLTemplateElement | string;
    logoMobile?: HTMLTemplateElement | string;
    favicon?: HTMLTemplateElement | string;
    language?: string;
    header?: HeaderConfig;
    styles?: TemplateResult | string;
}

export interface AppConfig<S extends AppStateKeyed> {
    pages: PageProvider<S>[];
    offlinePage?: PageProvider<S>; // override for fallback page when user is offline/disconnected
    offlineTimeout?: number;
    languages?: Languages;
    superUserHeader?: HeaderConfig;
    realms?: {
        // @ts-ignore
        default?: RealmAppConfig;
        [realm: string]: RealmAppConfig;
    };
}

export interface PageProvider<S extends AppStateKeyed> {
    name: string;
    routes: string[];
    allowOffline?: boolean; // allow use during offline/disconnected state. Default is false.
    pageCreator: () => Page<S>;
}

export abstract class Page<S extends AppStateKeyed> extends translate(i18next)(LitElement) {

    abstract get name(): string;

    protected _store: Store<S, AnyAction>;

    protected _storeUnsubscribe!: Unsubscribe;

    // onRefresh() gets called by or-app to (as silent as possible) refresh the content of the page.
    // If undefined (default), it will fully rerender the Web Component.
    onRefresh?: () => void;

    constructor(store: Store<S, AnyAction>) {
        super();
        this._store = store;
    }

    connectedCallback() {
        super.connectedCallback();
        this._storeUnsubscribe = this._store.subscribe(() => this.stateChanged(this._store.getState()));
        this.stateChanged(this._store.getState());
    }

    disconnectedCallback() {
        this._storeUnsubscribe();
        super.disconnectedCallback();
    }

    protected getState(): S {
        return this._store.getState();
    }

    abstract stateChanged(state: S): void;
}
