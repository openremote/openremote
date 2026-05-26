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
import { customElement, state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {AssetWidgetConfig} from "../util/widget-config";
import {Attribute, AttributeRef} from "@openremote/model";
import {html, TemplateResult } from "lit";
import {KpiSettings} from "../settings/kpi-settings";
import "@openremote/or-attribute-card";

export interface KpiWidgetConfig extends AssetWidgetConfig {
    period?: 'year' | 'month' | 'week' | 'day' | 'hour';
    decimals: number;
    deltaFormat: "absolute" | "percentage";
    showTimestampControls: boolean;
}

function getDefaultWidgetConfig(): KpiWidgetConfig {
    return {
        attributeRefs: [],
        period: "day",
        decimals: 0,
        deltaFormat: "absolute",
        showTimestampControls: false
    };
}

@customElement("kpi-widget")
export class KpiWidget extends OrAssetWidget {

    protected widgetConfig!: KpiWidgetConfig;

    @state()
    protected _loading = false;

    static getManifest(): WidgetManifest {
        return {
            displayName: "KPI",
            displayIcon: "label",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: KpiWidgetConfig): OrWidget {
                return new KpiWidget(config);
            },
            getSettingsHtml(config: KpiWidgetConfig): WidgetSettings {
                return new KpiSettings(config);
            },
            getDefaultConfig(): KpiWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    refreshContent(force: boolean): void {
        this.loadAssets(this.widgetConfig.attributeRefs);
    }

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
            <div style="position: relative; height: 100%; overflow: hidden;">
                ${when(this._loading || this._error, () => {
                    // Have to use `position: absolute` with white background due to rendering inconsistencies in or-attribute-card
                    return html`
                        <div style="position: absolute; top: -5%; width: 100%; height: 105%; background: white; z-index: 1; display: flex; justify-content: center; align-items: center; text-align: center;">
                            ${when(this._loading, () => html`
                                <or-loading-indicator></or-loading-indicator>
                            `, () => html`
                                <or-translate .value="${this._error}"></or-translate>
                            `)}
                        </div>
                    `;
                })}
                <or-attribute-card .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}" .period="${this.widgetConfig.period}"
                                   .deltaFormat="${this.widgetConfig.deltaFormat}" .mainValueDecimals="${this.widgetConfig.decimals}"
                                   showControls="${this.widgetConfig?.showTimestampControls}" showTitle="${false}" hideAttributePicker="${true}" style="height: 100%;">
                </or-attribute-card>
            </div>
        `;
    }

}
