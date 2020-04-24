import {customElement, html, LitElement, property, TemplateResult, css, unsafeCSS} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-data-viewer";
import {DataViewerConfig} from "@openremote/or-data-viewer";
import {DefaultBoxShadow} from "@openremote/core";

// language=CSS
const style = css`
    
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
class PageInsights extends connect(store)(LitElement)  {

    static get styles() {
        return style;
    }

    @property()
    protected _assetId;

    protected render(): TemplateResult | void {
        return html`
              <or-data-viewer .config="${viewerConfig}"></or-data-viewer>
        `;
    }

}