import {css, customElement, html, property} from "lit-element";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageLogsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: ViewerConfig) {
    return {
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

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    public stateChanged(state: S) {
    }

  
    @property()
    public config?: ViewerConfig;

    protected render() {
        return html`
            <or-log-viewer .config="${this.config}"></or-log-viewer>
        `;
    }
}
