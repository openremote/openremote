import {css, html} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import manager from "@openremote/core";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";

export interface PageLogsConfig {
    viewer?: ViewerConfig
}

export function pageLogsProvider(store: Store<AppStateKeyed>, config?: PageLogsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "logs",
        routes: [
            "logs"
        ],
        pageCreator: () => {
            const page = new PageLogs(store);
            if(config) page.config = config;
            return page;
        }
    };
}

@customElement("page-logs")
export class PageLogs extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;
            }

            or-log-viewer {
                width: 100%;
            }
        `;
    }

    @property()
    public config?: PageLogsConfig;

    @state()
    protected _realm?: string;

    get name(): string {
        return "logs";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public stateChanged(state: AppStateKeyed) {
        this._realm = state.app.realm || manager.displayRealm;
    }

    protected _getViewerRealm(): string | undefined {
        // A superuser viewing their own (master) realm sees logs of all realms including system
        // logs (no realm); non-superusers are restricted to their own realm by the server anyway
        if (manager.isSuperUser() && this._realm === manager.config.realm) {
            return undefined;
        }
        return this._realm;
    }

    protected render() {
        return html`
            <or-log-viewer .realm="${this._getViewerRealm()}" .config="${this.config?.viewer}"></or-log-viewer>
        `;
    }
}
