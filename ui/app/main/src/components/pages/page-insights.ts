import {customElement, html, LitElement, property, TemplateResult, css, unsafeCSS} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-data-viewer";
import {DataViewerConfig} from "@openremote/or-data-viewer";
import {DefaultBoxShadow, DefaultColor1, DefaultColor2, DefaultColor3, DefaultColor5} from "@openremote/core";

// language=CSS
const style = css`

    :host {
        --internal-or-asset-viewer-background-color: var(--or-asset-viewer-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-asset-viewer-panel-margin: var(--or-asset-viewer-panel-margin, 20px);
        --internal-or-asset-viewer-panel-padding: var(--or-asset-viewer-panel-padding, 24px);
        --internal-or-asset-viewer-text-color: var(--or-asset-viewer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-asset-viewer-title-text-color: var(--or-asset-viewer-title-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));       
        --internal-or-asset-viewer-panel-color: var(--or-asset-viewer-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-viewer-line-color: var(--or-asset-viewer-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        
        height: 100%;
        width: 100%;
        background-color: var(--internal-or-asset-viewer-background-color);
    }
   
    *[hidden] {
        display: none;
    }
    
    #wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: auto;
    }
    
    @media only screen and (max-width: 767px) {
        #wrapper {
            position: absolute;
            left: 0;
            right: 0;
        }
    }
    
    #container {
        box-sizing: border-box;
        display: grid;
        padding: 20px 20px;
        grid-gap: 10px;
        grid-template-columns: 1fr 1fr 1fr 1fr;
        
        width: 100%;
        max-width: 1400px;
        margin-left: auto;
        margin-right: auto;

        -webkit-animation: fadein 0.3s; /* Safari, Chrome and Opera > 12.1 */
        -moz-animation: fadein 0.3s; /* Firefox < 16 */
        -ms-animation: fadein 0.3s; /* Internet Explorer */
        -o-animation: fadein 0.3s; /* Opera < 12.1 */
        animation: fadein 0.3s;
    }
    @media only screen and (max-width: 1023px) {
        #container {
            grid-template-columns: 1fr 1fr;
        }
    }
    @media only screen and (max-width: 767px) {
        #container {
            grid-template-columns: 1fr;
        }
    }
    
    .row {
        grid-column-start: 1;
        grid-column-end: -1;
    }
    
    .panel {
        background-color: var(--internal-or-asset-viewer-panel-color);     
        border: 1px solid #e5e5e5;
        border-radius: 5px;
        max-width: 100%;
        position: relative;
    }
    
    .panel-content-wrapper {
        padding: var(--internal-or-asset-viewer-panel-padding);
    }
    
    .panel-content {
        display: flex;
        flex-wrap: wrap;
    }
        
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 25px;
        flex: 0 0 auto;
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
            <div id="wrapper">
                <div id="container">
                    <div class="panel" id="attributeDetail-panel">
                        <div class="panel-content-wrapper">
                            <div class="panel-title">
                                <or-translate value="attributeDetail"></or-translate>
                            </div>
                            <div class="panel-content">
                                <p>hello</p>
                            </div>
                        </div>
                    </div>
                    <div class="panel" id="attributeDetail-panel">
                        <div class="panel-content-wrapper">
                            <div class="panel-title">
                                <or-translate value="attributeDetail"></or-translate>
                            </div>
                            <div class="panel-content">
                                <p>person</p>
                            </div>
                        </div>
                    </div>
                    <div class="panel" id="attributeDetail-panel">
                        <div class="panel-content-wrapper">
                            <div class="panel-title">
                                <or-translate value="attributeDetail"></or-translate>
                            </div>
                            <div class="panel-content">
                                <p>welcome</p>
                            </div>
                        </div>
                    </div>
                    <div class="panel" id="attributeDetail-panel">
                        <div class="panel-content-wrapper">
                            <div class="panel-title">
                                <or-translate value="attributeDetail"></or-translate>
                            </div>
                            <div class="panel-content">
                                <p>to this place</p>
                            </div>
                        </div>
                    </div>
                    <div class="row">
<!--                        <or-data-viewer .config="${viewerConfig}"></or-data-viewer>-->
                    </div>
                </div>
            </div>
        `;
    }

}
