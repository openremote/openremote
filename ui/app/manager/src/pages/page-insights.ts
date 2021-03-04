import {css, customElement, html, property, TemplateResult} from "lit-element";
import "@openremote/or-data-viewer";
import {DataViewerConfig} from "@openremote/or-data-viewer";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import i18next from "i18next";

export interface PageInsightsConfig {
    dataViewer?: DataViewerConfig
}

export function pageInsightsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: PageInsightsConfig): PageProvider<S> {
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
class PageInsights<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`            
            .hideMobile {
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

            or-data-viewer {
                width: 100%;
                max-width: 1400px;
                margin: 0 auto;
            }
        
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
            }
        `;
    }

    @property()
    public config?: PageInsightsConfig;

    @property()
    protected _assetId;

    get name(): string {
        return "insights";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render(): TemplateResult | void {
        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="chart-areaspline"></or-icon>${i18next.t("insights")}
                </div>
                <or-data-viewer .config="${this.config?.dataViewer}"></or-data-viewer>
            </div>
        `;
    }

    public stateChanged(state: S) {
    }
}
