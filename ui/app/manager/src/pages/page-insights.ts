import {css, html, TemplateResult} from "lit";
import {customElement, property, query, state } from "lit/decorators.js";
import "@openremote/or-data-viewer";
import {DataViewerConfig, OrDataViewer} from "@openremote/or-data-viewer";
import {Page, PageProvider, router} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
import i18next from "i18next";
import {createSelector} from "reselect";
import { manager } from "@openremote/core";
import "@openremote/or-dashboard-builder";
import {Dashboard, DashboardTemplate, DashboardWidgetType } from "@openremote/model";
import {getInsightsRoute} from "../routes";

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
            
            /*.hideMobile {
                display: none;
            }

            #wrapper {
                height: 100%;
                width: 100%;
                display: flex;
                flex-direction: column;
                overflow: auto;
            }
                
            #title {
                margin: 20px auto 0;
                padding: 0;
                font-size: 18px;
                font-weight: bold;
                width: 100%;
                max-width: 1360px;
                align-items: center;
                display: flex;
            }

            !*or-data-viewer {
                width: 100%;
                max-width: 1400px;
                margin: 0 auto;
            }*!
        
            #title > or-icon {
                margin-right: 10px;
                margin-left: 14px;
                --or-icon-width: 20px;
                --or-icon-height: 20px;
            }
            
            @media only screen and (min-width: 768px){
                .hideMobile {
                    display: flex;
                }

                #title {
                    padding: 0 20px;
                }
            }*/
        `;
    }

    @property()
    public config?: PageInsightsConfig;

    @query("#data-viewer")
    protected _dataviewer!: OrDataViewer;

    @property()
    protected _editMode: boolean = true;

    @property()
    private _dashboardId: string;

    updated(changedProperties: Map<string, any>) {
        console.log(changedProperties);
        if(changedProperties.has("_dashboardId")) {
            console.log("Changed Properties of _dashboardId!")
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
        }
    )

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public connectedCallback() {
        super.connectedCallback();
    }

    protected render(): TemplateResult | void {
        const template = {
            columns: 8,
            maxScreenWidth: 400,
            screenPresets: [],
            widgets: [
                {widgetType: DashboardWidgetType.CHART, gridItem: { id: 'item1', x: 0, y: 0, w: 2, h: 2, content: "<span>Widget 1</span>" }},
                {widgetType: DashboardWidgetType.CHART, gridItem: { id: 'item2', x: 5, y: 0, w: 2, h: 2, content: "<span>Widget 2</span>" }},
                {widgetType: DashboardWidgetType.MAP, gridItem: { id: 'item3', x: 2, y: 0, w: 3, h: 3, content: "<span>Widget 3</span>" }},
            ]
        } as DashboardTemplate;
        return html`
            <div style="width: 100%;">
                <or-dashboard-builder id="builder" .editMode="${this._editMode}" .selectedId="${this._dashboardId}"
                                      @selected="${(event: CustomEvent) => { console.log(event); this._dashboardId = (event.detail as Dashboard)?.id }}"
                                      @editToggle="${(event: CustomEvent) => { console.log(event); this._editMode = event.detail; this._updateRoute(true); }}"
                ></or-dashboard-builder>
            </div>
            <!--<div id="wrapper">
                <div id="title">
                    <or-icon icon="chart-areaspline"></or-icon>${i18next.t("insights")}
                </div>
                <or-data-viewer id="data-viewer" .config="${this.config?.dataViewer}"></or-data-viewer>
            </div>-->
        `;
    }

    stateChanged(state: AppStateKeyed) {
        // State is only utilised for initial loading
        this.getRealmState(state); // Order is important here!
        this._editMode = (state.app.params && state.app.params.editMode) ? (state.app.params.editMode == "true") : false;
        this._dashboardId = (state.app.params && state.app.params.id) ? state.app.params.id : undefined;
    }

    protected _updateRoute(silent: boolean = true) {
        console.log("Updating the URL Route!");
        router.navigate(getInsightsRoute(this._editMode, this._dashboardId), {
            callHooks: !silent,
            callHandler: !silent
        });
    }
}
