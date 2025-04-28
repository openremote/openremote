import {css, html, PropertyValues} from "lit";
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


export interface ExternalService {
    label: string;
    name: string;
    iframe_url: string;
    health_url: string;
    tenancy: boolean;
}

@customElement("page-services")
export class PageServices extends Page<AppStateKeyed> {

    services: ExternalService[] = [
        {
            label: "ML Forecast Service",
            name: "ml-forecast",
            iframe_url: "https://test3.openremote.app/services/ml-forecast/ui",
            health_url: "https://test3.openremote.app/services/ml-forecast/api/system/health",
            tenancy: true,
        }
    ]

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


    getIframePath(service: ExternalService) {
        const isSuperUser = manager.isSuperUser();

        // If no tenancy, we can just use the iframe_url
        if (!service.tenancy) {
            return service.iframe_url;
        }

        // If its super user use realm as param
        if (isSuperUser) {
            return `${service.iframe_url}/${this.realmName}`;
        }

        // If not its a query param
        return `${service.iframe_url}?realm=${this.realmName}`;
    }

    public stateChanged(state: AppStateKeyed) {
        this.getRealmState(state);
    }

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.config.realm;

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm) => {
            this.realmName = realm;
        }
    )


    @state()
    protected realmName: string;

    protected render() {
        // Super user doesnt need to provide realm query param
        console.log("Loading service via: ", this.getIframePath(this.services[0]));

        return html`
            <div class="sidebar-placeholder"></div>
            <iframe id="services-iframe" src="${this.getIframePath(this.services[0])}"></iframe>
        `;
    }
}
