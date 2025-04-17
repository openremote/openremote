import {css, html} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {createSelector, Store} from "@reduxjs/toolkit";
import { manager } from "@openremote/core";

export function pageServicesProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "services",
        routes: [
            "services"
        ],
        pageCreator: () => {
            const page = new PageServices(store);
            return page;
        }
    };
}

@customElement("page-services")
export class PageServices extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;
            }

            .sidebar-placeholder {
                background: #fff;
                width: 300px;
                min-width: 300px;
                box-shadow: rgba(0, 0, 0, 0.21) 0px 1px 3px 0px;
                z-index: 1;
                }

            iframe {
                width: 100%;
                border: none;
                }
        `;
    }

    get name(): string {
        return "logs";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);


        this.realmName = manager.displayRealm;
    }

    public stateChanged(state: AppStateKeyed) {
        this.getRealmState(state);
    }

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm) => {
            this.realmName = realm;
        }
    )



    @state()
    protected realmName: string;

    protected render() {


        return html`
            <div class="sidebar-placeholder"></div>
            <!-- Hardcoded for testing -->
            <iframe src="https://test3.openremote.app/services/ml-forecast/ui/${this.realmName}">hello</iframe>
        `;
    }
}
