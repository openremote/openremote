import {css, customElement, html, property} from "lit-element";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";

export interface PageLogsConfig {
    viewer?: ViewerConfig
}

export function pageLogsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: PageLogsConfig): PageProvider<S> {
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
class PageLogs<S extends AppStateKeyed> extends Page<S> {

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

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    public stateChanged(state: S) {
    }

    protected render() {
        return html`
            <or-log-viewer .config="${this.config?.viewer}"></or-log-viewer>
        `;
    }
}
