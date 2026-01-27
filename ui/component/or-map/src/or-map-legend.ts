/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { CSSResultGroup, html, LitElement, PropertyValues } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { mapAssetLegendStyle } from "./style";
import { AssetModelUtil } from "@openremote/model";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { Util } from "@openremote/core";
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

    static get styles(): CSSResultGroup {
        return mapAssetLegendStyle;
    }

    @property({ type: Array })
    public assetTypes: string[] = [];

    protected _assetTypesInfo: any;

    protected _excludedTypes: string[] = [];

    @query("#legend-content")
    protected _showLegend?: HTMLDivElement;

    protected shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("assetTypes")) {
            this._assetTypesInfo = {};

            this.assetTypes.forEach((assetType: string) => {
                const descriptor = AssetModelUtil.getAssetDescriptor(assetType);
                const icon = getMarkerIconAndColorFromAssetType(descriptor)?.icon;
                const color = getMarkerIconAndColorFromAssetType(descriptor)?.color;
                const label = Util.getAssetTypeLabel(descriptor);

                this._assetTypesInfo[assetType] = {
                    icon: icon,
                    color: color,
                    label: label,
                };
            });
        }

        return super.shouldUpdate(changedProperties);
    }

    protected _onHeaderClick() {
        if (this._showLegend) {
            this._showLegend.hidden = !this._showLegend.hidden;
        }
    }

    protected render() {
        return html`
            <div id="legend">
                <div id="legend-title" @click="${() => this._onHeaderClick()}">
                    <or-translate value="mapPage.legendTitle"></or-translate><or-icon style="cursor: pointer" icon="menu"></or-icon>
                </div>
                <div id="legend-content" hidden>
                    <ul>
                        ${this.assetTypes.map(
                            (assetType) => html` <li id="asset-legend" data-asset-type="${assetType}" style="display: flex;">
                                <or-icon
                                    icon="${this._assetTypesInfo[assetType].icon}"
                                    style="color: #${this._assetTypesInfo[assetType].color}"
                                ></or-icon>
                                <span id="asset-label" style="flex: 1">${this._assetTypesInfo[assetType].label}</span>
                                <or-mwc-input
                                    .type="${InputType.CHECKBOX}"
                                    .value="${!this._excludedTypes.includes(assetType)}"
                                    @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                        if (ev.detail.value) {
                                            this._excludedTypes.splice(this._excludedTypes.indexOf(assetType), 1);
                                        } else {
                                            this._excludedTypes.push(assetType);
                                        }
                                        this.dispatchEvent(new OrMapLegendEvent(this._excludedTypes));
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
