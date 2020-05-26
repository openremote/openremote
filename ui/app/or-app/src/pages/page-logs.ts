import {css, customElement, html} from "lit-element";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageLogsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>) {
    return {
        routes: [
            "logs"
        ],
        pageCreator: () => {
            return new PageLogs(store);
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

    protected config: ViewerConfig = {
    };

    protected render() {
        return html`
            <or-log-viewer .config="${this.config}"></or-log-viewer>
        `;
    }
}
