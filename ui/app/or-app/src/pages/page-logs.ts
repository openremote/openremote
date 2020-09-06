import {css, customElement, html, property} from "lit-element";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {AppStateKeyed} from "../app";
import {Page} from "../types";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageLogsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: ViewerConfig) {
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
    public config?: ViewerConfig;

    get name(): string {
        return "account";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    public stateChanged(state: S) {
    }

    protected render() {
        return html`
            <or-log-viewer .config="${this.config}"></or-log-viewer>
        `;
    }
}
