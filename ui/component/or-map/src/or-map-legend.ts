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
import {Util} from "@openremote/core";
import {i18next} from "@openremote/or-translate";

@customElement("or-map-legend")
export class OrMapLegend extends LitElement {

    @property({type: Array})
    public assetTypes: string[] = [];

    protected assetTypesInfo: any;

    @query("#legend-content")
    protected _showLegend?: HTMLDivElement;

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("assetTypes")) {
            this.assetTypesInfo = {};

            this.assetTypes.forEach((assetType: string) => {
                const descriptor = AssetModelUtil.getAssetDescriptor(assetType);
                const icon = getMarkerIconAndColorFromAssetType(descriptor)?.icon;
                const color = getMarkerIconAndColorFromAssetType(descriptor)?.color;
                const label = Util.getAssetTypeLabel(descriptor);

                this.assetTypesInfo[assetType] = {
                    icon: icon,
                    color: color,
                    label: label
                }
            });
        }

        if (this._showLegend) {
            this._showLegend.hidden = true;
        }

        return super.shouldUpdate(_changedProperties);
    }

    static get styles(): CSSResultGroup {
        return mapAssetLegendStyle;
    }

    protected _onHeaderClick(evt: MouseEvent | null) {
        if (this._showLegend) {
            this._showLegend.hidden = !this._showLegend?.hidden;
        }
    }

    protected render() {
        return html`
            <div id="legend">
                <div id="legend-title" @click="${(evt: MouseEvent) => this._onHeaderClick(evt)}">
                    <span>${i18next.t("mapPage.legendTitle")}</span><or-icon icon="menu"></or-icon>
                </div>
                <div id="legend-content">
                    <ul>
                        ${this.assetTypes ? this.assetTypes.map((assetType) => {
                            return html`<li id="asset-legend"><or-icon icon="${this.assetTypesInfo[assetType].icon}" style="color: #${this.assetTypesInfo[assetType].color}"></or-icon><span id="asset-label">${this.assetTypesInfo[assetType].label}</span></li>`;
                        }) : ''}
                    </ul>
                </div>
            </div>
        `;
    }
}
