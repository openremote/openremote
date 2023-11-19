import {
    CSSResultGroup,
    html,
    LitElement
} from "lit";
import {customElement, property} from "lit/decorators.js";
import {mapAssetLegendStyle} from "./style";

@customElement("or-map-legend")
export class OrMapLegend extends LitElement {

    @property({type: String})
    public assetTypes: string[] = [];

    static get styles(): CSSResultGroup {
        return mapAssetLegendStyle;
    }

    protected render() {
        console.log("render legend !");
        return html`
            <div id="card-container">
                <div id="header">
                    <span id="title">Legend</span>
                </div>
                <div id="attribute-list">
                    <ul>
                        <li>Test</li>
                        ${this.assetTypes ? this.assetTypes.map((assetType) => {
                            return html`<li>${assetType}</li>`;
                        }) : ''}
                    </ul>
                </div>
            </div>
        `;
    }
}
