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
import {html, css, TemplateResult} from "lit";
import {customElement, state} from "lit/decorators.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {AssetWidgetConfig} from "../util/widget-config";
import {Attribute, AttributeRef} from "@openremote/model";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {GaugeSettings} from "../settings/gauge-settings";
import {when} from "lit/directives/when.js";
import "@openremote/or-gauge";

export interface GaugeWidgetConfig extends AssetWidgetConfig {
    thresholds: [number, string][];
    decimals: number;
    min: number;
    max: number;
    valueType: string;
}

function getDefaultWidgetConfig(): GaugeWidgetConfig {
    return {
        attributeRefs: [],
        thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]], // colors from https://mui.com/material-ui/customization/palette/ as reference (since material has no official colors)
        decimals: 0,
        min: 0,
        max: 100,
        valueType: 'number',
    };
}

const styling = css`
  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }
`;

@customElement("gauge-widget")
export class GaugeWidget extends OrAssetWidget {

    // Override of widgetConfig with extended type
    protected widgetConfig!: GaugeWidgetConfig;

    @state()
    protected _loading = false;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Gauge",
            displayIcon: "gauge",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: GaugeWidgetConfig): OrWidget {
                return new GaugeWidget(config);
            },
            getSettingsHtml(config: GaugeWidgetConfig): WidgetSettings {
                return new GaugeSettings(config);
            },
            getDefaultConfig(): GaugeWidgetConfig {
                return getDefaultWidgetConfig();
            }
        };
    }

    public refreshContent(force: boolean) {
        this.loadAssets(this.widgetConfig.attributeRefs);
    }

    static get styles() {
        return [...super.styles, styling];
    }

    // WebComponent lifecycle method, that occurs DURING every state update
    protected willUpdate(changedProps: Map<string, any>) {

        // If widgetConfig, and the attributeRefs of them have changed...
        if(changedProps.has("widgetConfig") && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;

            // Check if list of attributes has changed, based on the cached assets
            const loadedRefs: AttributeRef[] = attributeRefs?.filter((attrRef: AttributeRef) => this.isAttributeRefLoaded(attrRef));
            if (loadedRefs?.length !== (attributeRefs ? attributeRefs.length : 0)) {

                // Fetch the new list of assets
                this.loadAssets(attributeRefs);

            }
        }
        return super.willUpdate(changedProps);
    }

    protected loadAssets(attributeRefs: AttributeRef[]) {
        if(attributeRefs.length === 0) {
            this._error = "noAttributesConnected";
            return;
        }
        this._loading = true;
        this._error = undefined;
        this.fetchAssets(attributeRefs).then((assets) => {
            this.loadedAssets = assets;
            this.assetAttributes = attributeRefs?.map((attrRef: AttributeRef) => {
                const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
            }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
        }).catch(e => {
            this._error = e.message;
        }).finally(() => {
            this._loading = false;
        });
    }


    protected render(): TemplateResult {
        return html`
            ${when(this._loading || this._error, () => {
                if(this._loading) {
                    return html`<or-loading-indicator></or-loading-indicator>`;
                } else {
                    return html`<or-translate id="error-txt" .value="${this._error}"></or-translate>`;
                }
            }, () => when(this.loadedAssets && this.assetAttributes && this.loadedAssets.length > 0 && this.assetAttributes.length > 0, () => {
                return html`
                    <or-gauge .asset="${this.loadedAssets[0]}" .assetAttribute="${this.assetAttributes[0]}" .thresholds="${this.widgetConfig.thresholds}"
                              .decimals="${this.widgetConfig.decimals}" .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                              style="height: 100%; overflow: hidden;">
                    </or-gauge>
                `;
            }, () => {
                return html`
                    <div style="height: 100%; display: flex; justify-content: center; align-items: center; text-align: center;">
                        <span><or-translate value="noAttributeConnected"></or-translate></span>
                    </div>
                `;
            }))}
        `;
    }

}
