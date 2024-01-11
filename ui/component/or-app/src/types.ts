import {HeaderConfig, Languages} from "./or-header";
import {AppStateKeyed} from "./app";
import {LitElement, TemplateResult} from "lit";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
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
