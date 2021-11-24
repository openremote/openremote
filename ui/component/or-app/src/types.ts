import {HeaderConfig} from "./or-header";
import {AppStateKeyed} from "./app";
import { TemplateResult, LitElement } from "lit";
import i18next from "i18next";
import { translate } from "@openremote/or-translate";
import { EnhancedStore, AnyAction, Unsubscribe } from "@reduxjs/toolkit";
import {ThunkMiddleware} from "redux-thunk";
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
    pageCreator: () => Page<S>;
}

export abstract class Page<S extends AppStateKeyed> extends translate(i18next)(LitElement) {

    abstract get name(): string;

    protected _store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>;

    protected _storeUnsubscribe!: Unsubscribe;

    constructor(store: EnhancedStore<S, AnyAction, ReadonlyArray<ThunkMiddleware<S>>>) {
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
