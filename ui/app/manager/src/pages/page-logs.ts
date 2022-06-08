import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
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

    get name(): string {
        return "logs";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public stateChanged(state: AppStateKeyed) {
    }

    protected render() {
        return html`
            <or-log-viewer .config="${this.config?.viewer}"></or-log-viewer>
        `;
    }
}
