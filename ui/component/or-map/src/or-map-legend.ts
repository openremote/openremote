import { CSSResultGroup, html, LitElement, PropertyValues } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { mapAssetLegendStyle } from "./style";
import { AssetModelUtil } from "@openremote/model";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { Util } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";

export class OrMapLegendEvent extends CustomEvent<string[]> {
    public static readonly NAME = "or-map-legend-changed";

    constructor(assetTypes: string[]) {
        super(OrMapLegendEvent.NAME, {
            bubbles: false,
            composed: false,
            detail: assetTypes,
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapLegendEvent.NAME]: OrMapLegendEvent;
    }
}

@customElement("or-map-legend")
export class OrMapLegend extends LitElement {
    @property({ type: Array })
    public assetTypes: string[] = [];

    protected assetTypesInfo: any;

    protected excluded: string[] = [];

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
                    label: label,
                };
            });
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
                    <span>${i18next.t("mapPage.legendTitle")}</span><or-icon style="cursor: pointer" icon="menu"></or-icon>
                </div>
                <div id="legend-content">
                    <ul>
                        ${this.assetTypes.map(
                            (assetType) => html` <li id="asset-legend" style="display: flex;">
                                <or-icon
                                    icon="${this.assetTypesInfo[assetType].icon}"
                                    style="color: #${this.assetTypesInfo[assetType].color}"
                                ></or-icon>
                                <span id="asset-label" style="flex: 1">${this.assetTypesInfo[assetType].label}</span>
                                <or-mwc-input
                                    .type="${InputType.CHECKBOX}"
                                    .value="${!this.excluded.includes(assetType)}"
                                    @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                        if (ev.detail.value) {
                                            this.excluded.splice(this.excluded.indexOf(assetType), 1);
                                        } else {
                                            this.excluded.push(assetType);
                                        }
                                        this.dispatchEvent(new OrMapLegendEvent(this.excluded));
                                    }}"
                                ></or-mwc-input>
                            </li>`
                        )}
                    </ul>
                </div>
            </div>
        `;
    }
}
