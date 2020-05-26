import {css, customElement, html, property, TemplateResult, unsafeCSS} from "lit-element";
import "@openremote/or-data-viewer";
import {DataViewerConfig} from "@openremote/or-data-viewer";
import {DefaultBoxShadow} from "@openremote/core";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageInsightsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>) {
    return {
        routes: [
            "insights",
            "insights/:id"
        ],
        pageCreator: () => {
            return new PageInsights(store);
        }
    };
}

const viewerConfig: DataViewerConfig = {
   panels: {
    "chart": {
        type: "chart",
        hideOnMobile: true,
        panelStyles: {
            gridColumn: "1 / -1",
            gridRowStart: "1"
        },
        defaults: [{
            assetId:"5442Ite2XVp3Ky9X2y647a",
            attributes: ["UVIndex"]
        }]
    }
   }
};

@customElement("page-insights")
class PageInsights<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`            
            or-asset-tree {
                align-items: stretch;
                z-index: 1;
            }
            
            .hideMobile {
                display: none;
            }
                
            or-asset-viewer {
                align-items: stretch;
                z-index: 0;
            }
            
            @media only screen and (min-width: 768px){
                or-asset-tree {
                    width: 300px;
                    min-width: 300px;
                    box-shadow: ${unsafeCSS(DefaultBoxShadow)} 
                }
                
                .hideMobile {
                    display: flex;
                }
                
                or-asset-viewer,
                or-asset-viewer.hideMobile {
                    display: initial;
                }
            }
        `;
    }

    @property()
    protected _assetId;

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render(): TemplateResult | void {
        return html`
              <or-data-viewer .config="${viewerConfig}"></or-data-viewer>
        `;
    }

    public stateChanged(state: S) {
    }
}
