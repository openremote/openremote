import {css, html, TemplateResult} from "lit";
import {customElement, property, query, state } from "lit/decorators.js";
import "@openremote/or-data-viewer";
import {DataViewerConfig, OrDataViewer} from "@openremote/or-data-viewer";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import i18next from "i18next";
import {createSelector} from "reselect";
import { manager } from "@openremote/core";
import "../../../../component/or-dashboard-builder";

export interface PageInsightsConfig {
    dataViewer?: DataViewerConfig
}

export function pageInsightsProvider(store: EnhancedStore<AppStateKeyed>, config?: PageInsightsConfig): PageProvider<AppStateKeyed> {
    return {
        name: "insights",
        routes: [
            "insights",
            "insights/:id"
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

            #container {
                display: flex;
                width: 100%;
            }
            #tree {
                flex-grow: 1;
                align-items: stretch;
                z-index: 1;
                max-width: 300px;
                box-shadow: rgb(0 0 0 / 21%) 0px 1px 3px 0px;
            }
            #builder {
                flex-grow: 2;
                align-items: stretch;
                z-index: 0;
                background: transparent;
            }

            .hideMobile {
                display: none;
            }

            @media only screen and (min-width: 768px){
                #tree {
                    width: 300px;
                    min-width: 300px;
                }

                .hideMobile {
                    display: flex;
                }

                #builder,
                #builder.hideMobile {
                    display: initial;
                    max-width: calc(100vw - 300px);
                }
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

    @property()
    protected _assetId;

    @query("#data-viewer")
    protected _dataviewer!: OrDataViewer;

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

    get name(): string {
        return "insights";
    }

    protected getRealmState = createSelector(
        [this._realmSelector],
        async () => {
            if (this._dataviewer) this._dataviewer.refresh();
        }
    )

    constructor(store: EnhancedStore<AppStateKeyed>) {
        super(store);
    }

    public connectedCallback() {
        super.connectedCallback();
    }

    @state()
    private selectedDashboard: any;

    protected render(): TemplateResult | void {
        return html`
            <div id="container">
                <or-dashboard-tree id="tree" @created="${() => { this.selectedDashboard = undefined; }}" @select="${(event: CustomEvent) => { this.selectedDashboard = event.detail; }}"></or-dashboard-tree>
                <or-dashboard-builder id="builder" .dashboard="${this.selectedDashboard}"></or-dashboard-builder>
            </div>
            <!--<div id="wrapper">
                <div id="title">
                    <or-icon icon="chart-areaspline"></or-icon>${i18next.t("insights")}
                </div>
                <or-dashboard-builder id="data-viewer"></or-dashboard-builder>
                <or-data-viewer id="data-viewer" .config="${this.config?.dataViewer}"></or-data-viewer>
            </div>-->
        `;
    }

    stateChanged(state: AppStateKeyed) {
        // State is only utilised for initial loading
        this.getRealmState(state); // Order is important here!
    }
}
