import {css, customElement, html, property, TemplateResult} from "lit-element";
import "@openremote/or-data-viewer";
import {DataViewerConfig} from "@openremote/or-data-viewer";
import {AppStateKeyed} from "../app";
import {Page} from "../types";
import {EnhancedStore} from "@reduxjs/toolkit";
import i18next from "i18next";

export function pageInsightsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?:DataViewerConfig) {
    return {
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
            }

            or-data-viewer {
                width: 100%;
                max-width: 1400px;
                margin: 0 auto;
            }
        
            #title > or-icon {
                margin-right: 10px;
                margin-left: 14px;
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
    public config?: DataViewerConfig;

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
                <or-data-viewer></or-data-viewer>
            </div>
        `;
    }

    public stateChanged(state: S) {
    }
}
