import {css, html, TemplateResult} from "lit";
import {customElement, property, query, state } from "lit/decorators.js";
import "@openremote/or-data-viewer";
import {DataViewerConfig, OrDataViewer} from "@openremote/or-data-viewer";
import {Page, PageProvider, router} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import {createSelector} from "reselect";
import { manager } from "@openremote/core";
import "@openremote/or-dashboard-builder";
import {ClientRole, Dashboard} from "@openremote/model";
import {getInsightsRoute} from "../routes";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";

export interface PageInsightsConfig {
    dataViewer?: DataViewerConfig
}

export function pageInsightsProvider(store: Store<AppStateKeyed>, config?: PageInsightsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "insights",
        routes: [
            "insights",
            "insights/:editMode",
            "insights/:editMode/:id"
        ],
        pageCreator: () => {
            const page = new PageInsights(store);
            if(config) page.config = config;
            return page;
        }
    };
}

@customElement("page-insights")
export class PageInsights extends Page<AppStateKeyed>  {

    static get styles() {
        // language=CSS
        return css`

            :host {
                overflow: hidden; 
            }
            
            #builder {
                z-index: 0;
                background: transparent;
            }
        `;
    }

    @property()
    public config?: PageInsightsConfig;

    @query("#data-viewer")
    protected _dataviewer!: OrDataViewer;

    @property()
    protected _editMode: boolean = true;

    @property()
    protected _fullscreen: boolean = true;

    @property()
    private _dashboardId: string;

    @state()
    private _userId?: string;


    /* ------------------ */

    updated(changedProperties: Map<string, any>) {
        if(changedProperties.has("_dashboardId")) {
            this._updateRoute();
        }
    }

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

    get name(): string {
        return "insights";
    }

    protected getRealmState = createSelector(
        [this._realmSelector],
        async () => {
            if (this._dataviewer) this._dataviewer.refresh();
            this._updateRoute(true);
            this.requestUpdate();
        }
    )

    constructor(store: Store<AppStateKeyed>) {
        super(store);
        manager.rest.api.UserResource.getCurrent().then((response: any) => {
            this._userId = response.data.id;
        }).catch((ex) => {
            console.error(ex);
            showSnackbar(undefined, "errorOccurred");
        })
    }

    public connectedCallback() {
        super.connectedCallback();
    }

    protected render(): TemplateResult | void {
        return html`
            <div style="width: 100%;">
                <or-dashboard-builder id="builder" .editMode="${this._editMode}" .fullscreen="${this._fullscreen}" .selectedId="${this._dashboardId}"
                                      .realm="${manager.displayRealm}" .userId="${this._userId}" .readonly="${!manager.hasRole(ClientRole.WRITE_INSIGHTS)}"
                                      @selected="${(event: CustomEvent) => { this._dashboardId = (event.detail as Dashboard)?.id }}"
                                      @editToggle="${(event: CustomEvent) => { this._editMode = event.detail; this._fullscreen = true; this._updateRoute(true); }}"
                                      @fullscreenToggle="${(event: CustomEvent) => { this._fullscreen = event.detail; }}"
                ></or-dashboard-builder>
            </div>
        `;
    }

    stateChanged(state: AppStateKeyed) {
        // State is only utilised for initial loading
        this.getRealmState(state); // Order is important here!
        this._editMode = (state.app.params && state.app.params.editMode) ? (state.app.params.editMode == "true") : false;
        this._dashboardId = (state.app.params && state.app.params.id) ? state.app.params.id : undefined;
    }

    protected _updateRoute(silent: boolean = true) {
        router.navigate(getInsightsRoute(this._editMode, this._dashboardId), {
            callHooks: !silent,
            callHandler: !silent
        });
    }
}
