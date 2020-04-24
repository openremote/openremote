import {css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import {classMap} from "lit-html/directives/class-map";

import i18next from "i18next";

// language=CSS
const style = css`
    
    :host {
        width: 100%
    }
    
    :host([hidden]) {
        display: none;
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

@customElement("or-asset-detail")
export class OrAssetDetail extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    protected render() {
        return html`
            <div class="panel">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <or-translate value="attributeDetail"></or-translate>
                    </div>
                    <div class="panel-content">
                        <p>person</p>
                    </div>
                </div>
            </div>
        `;
    }

}
