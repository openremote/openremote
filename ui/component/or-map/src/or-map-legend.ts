import {
    CSSResultGroup,
    html,
    LitElement,
    PropertyValues
} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {mapAssetLegendStyle} from "./style";
import {AssetModelUtil} from "@openremote/model";
import {getMarkerIconAndColorFromAssetType} from "./util";

@customElement("or-map-legend")
export class OrMapLegend extends LitElement {

    @property({type: Array})
    public assetTypes: string[] = [];

    protected assetTypesIcon: any;
    protected assetTypesColor: any;

    @query("#legend-content")
    protected _showLegend?: HTMLDivElement;

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("assetTypes")) {
            this.assetTypesIcon = {};
            this.assetTypesColor = {};

            this.assetTypes.forEach((assetType: string) => {
                const descriptor = AssetModelUtil.getAssetDescriptor(assetType);
                const icon = getMarkerIconAndColorFromAssetType(descriptor)?.icon;
                const color = getMarkerIconAndColorFromAssetType(descriptor)?.color;

                this.assetTypesIcon[assetType] = icon;
                this.assetTypesColor[assetType] = color;
            });
        }

        return super.shouldUpdate(_changedProperties);
    }

    static get styles(): CSSResultGroup {
        return mapAssetLegendStyle;
    }

    protected _onHeaderClick(evt: MouseEvent | null) {
        console.log('click');
        if (this._showLegend) {
            this._showLegend.hidden = !this._showLegend?.hidden;
        }
    }

    protected render() {
        return html`
            <div id="legend">
                <div id="legend-title" @click="${(evt: MouseEvent) => this._onHeaderClick(evt)}">
                    <span>Legend</span><span><or-icon icon="menu"></or-icon></span>
                </div>
                <div id="legend-content">
                    <ul>
                        ${this.assetTypes ? this.assetTypes.map((assetType) => {
                            return html`<li><or-icon icon="${this.assetTypesIcon[assetType]}" style="color: #${this.assetTypesColor[assetType]}"></or-icon>${assetType}</li>`;
                        }) : ''}
                    </ul>
                </div>
            </div>
        `;
    }
}
