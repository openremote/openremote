/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {css, html, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {MapWidgetConfig} from "../widgets/map-widget";
import {LngLatLike, MapMarkerColours, LngLat} from "@openremote/or-map";
import "../panels/assettypes-panel";
import "../panels/thresholds-panel";
import {when} from "lit/directives/when.js";
import {AssetIdsSelectEvent, AssetTypeSelectEvent, AssetAllOfTypeSwitchEvent, AssetTypesFilterConfig, AttributeNamesSelectEvent} from "../panels/assettypes-panel";
import manager from "@openremote/core";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {BoolColorsChangeEvent, TextColorsChangeEvent, ThresholdChangeEvent} from "../panels/thresholds-panel";
import {OrVaadinNumberField} from "@openremote/or-vaadin-components/or-vaadin-number-field";
import {OrVaadinTextField} from "@openremote/or-vaadin-components/or-vaadin-text-field";

const styling = css`
  .switchMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;

@customElement("map-settings")
export class MapSettings extends AssetWidgetSettings {

    protected static _allowedValueTypes = ["boolean", "number", "integer", "positiveInteger", "positiveNumber", "negativeInteger", "negativeNumber", "text"];
    protected static _config: AssetTypesFilterConfig = {
        assets: {
            enabled: true,
            multi: true,
            allOfTypeOption: true
        },
        attributes: {
            enabled: true,
            valueTypes: MapSettings._allowedValueTypes
        }
    };

    protected widgetConfig!: MapWidgetConfig;

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        return html`
            <div>
                <!-- Map settings -->
                <settings-panel displayName="configuration.mapSettings" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <!-- Zoom level -->
                        <or-vaadin-number-field value=${this.widgetConfig.zoom} min="0" @change=${(ev: Event) => this.onZoomUpdate(ev)}>
                            <or-translate slot="label" value="dashboard.zoom"></or-translate>
                        </or-vaadin-number-field>
                        <or-vaadin-text-field value=${this.widgetConfig.center ? (Object.values(this.widgetConfig.center))[0] + ', ' + (Object.values(this.widgetConfig.center))[1] : undefined}
                                              @change=${(ev: Event) => this.onCenterUpdate(ev)}>
                            <or-translate slot="label" value="dashboard.center"></or-translate>
                        </or-vaadin-text-field>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><or-translate value="dashboard.showGeoJson"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showGeoJson}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onGeoJsonToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- Panel where Asset type and the selected attribute can be customized -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <assettypes-panel .assetType="${this.widgetConfig.assetType}" .attributeNames="${[this.widgetConfig.attributeName]}" .config="${MapSettings._config}"
                                      .allOfType="${this.widgetConfig.allOfType}" .assetIds="${this.widgetConfig.assetIds}"
                                      @assettype-select="${(ev: AssetTypeSelectEvent) => this.onAssetTypeSelect(ev)}"
                                      @alloftype-switch="${(ev: AssetAllOfTypeSwitchEvent) => this.onAssetAllOfTypeSwitch(ev)}"
                                      @assetids-select="${(ev: AssetIdsSelectEvent) => this.onAssetIdsSelect(ev)}"
                                      @attributenames-select="${(ev: AttributeNamesSelectEvent) => this.onAttributeNameSelect(ev)}"
                    ></assettypes-panel>

                    <!-- Other settings like labels and units-->
                    <div>
                        <div class="switchMwcInputContainer">
                            <span><or-translate value="dashboard.showLabels"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showLabels}" .disabled="${!this.widgetConfig.assetType}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowLabelsToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div class="switchMwcInputContainer">
                            <span><or-translate value="dashboard.showUnits"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showUnits}" .disabled="${!this.widgetConfig.showLabels || !this.widgetConfig.assetType}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowUnitsToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- List of customizable thresholds -->
                ${when(this.widgetConfig.attributeName, () => html`
                    <settings-panel displayName="thresholds" expanded="${true}">
                        <thresholds-panel .thresholds="${this.widgetConfig.thresholds}"
                                          .boolColors="${this.widgetConfig.boolColors}"
                                          .textColors="${this.widgetConfig.textColors}"
                                          .valueType="${this.widgetConfig.valueType}" style="padding-bottom: 12px;"
                                          .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                                          @threshold-change="${(ev: ThresholdChangeEvent) => this.onThresholdsChange(ev)}"
                                          @bool-colors-change="${(ev: BoolColorsChangeEvent) => this.onBoolColorsChange(ev)}"
                                          @text-colors-change="${(ev: TextColorsChangeEvent) => this.onTextColorsChange(ev)}">
                        </thresholds-panel>
                    </settings-panel>
                `)}
            </div>
        `;
    }

    protected onZoomUpdate(ev: Event) {
        const elem = ev.currentTarget as OrVaadinNumberField;
        if(elem.checkValidity()) {
            this.widgetConfig.zoom = Number(elem.value);
            this.notifyConfigUpdate();
        }
    }

    protected onCenterUpdate(ev: Event) {
        const elem = ev.currentTarget as OrVaadinTextField;
        if(!elem.checkValidity()) {
            return;
        }
        if (elem.value) {
            const lngLatArr = elem.value.split(/[, ]/).filter(v => !!v);
            if (lngLatArr.length === 2) {
                const value = new LngLat(
                    Number.parseFloat(lngLatArr[0]),
                    Number.parseFloat(lngLatArr[1])
                );
                this.widgetConfig.center = value as LngLatLike;
                this.notifyConfigUpdate();
            }
        } else {
            this.widgetConfig.center = undefined;
            this.notifyConfigUpdate();
        }
    }

    protected onGeoJsonToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showGeoJson = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onAssetTypeSelect(ev: AssetTypeSelectEvent) {
        if (this.widgetConfig.assetType !== ev.detail) {
            this.widgetConfig.attributeName = undefined;
            this.widgetConfig.assetIds = [];
            this.widgetConfig.showLabels = false;
            this.widgetConfig.showUnits = false;
            this.widgetConfig.boolColors = {type: 'boolean', 'false': '#ef5350', 'true': '#4caf50'};
            this.widgetConfig.textColors = [['example1', "#4caf50"], ['example2', "#ff9800"]];
            this.widgetConfig.thresholds = [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]];
            this.widgetConfig.assetType = ev.detail;
            this.notifyConfigUpdate();
        }
    }

    protected onAssetAllOfTypeSwitch(ev: AssetAllOfTypeSwitchEvent) {
        this.widgetConfig.allOfType = ev.detail as boolean;
        this.notifyConfigUpdate();
    }

    protected onAssetIdsSelect(ev: AssetIdsSelectEvent) {
        this.widgetConfig.assetIds = ev.detail as string[];
        this.notifyConfigUpdate();
    }

    protected async onAttributeNameSelect(ev: AttributeNamesSelectEvent) {
        const attrName = (ev.detail as string[])[0];
        if(this.widgetConfig.attributeName !== attrName) {
            this.widgetConfig.attributeName = attrName;

            const queryAssets = async (ids?: string[]) => {
                try {
                    const response = await manager.rest.api.AssetResource.queryAssets({
                        realm: { name: manager.displayRealm },
                        select: { attributes: [attrName, 'location'] },
                        types: [this.widgetConfig.assetType!],
                        ids: ids,
                        limit: ids?.length ?? 1
                    });
                    this.widgetConfig.assetIds = ids?.length ? response.data.map((a) => a.id!) : [];
                    this.widgetConfig.valueType = response.data.length ? response.data[0].attributes![attrName].type : "text";
                    if (!response.data[0].attributes![attrName].type) {
                        throw new TypeError("Data does not contain property 'attributes' or 'type'.")
                    }
                } catch (reason) {
                    console.error(reason);
                    if (reason instanceof TypeError) {
                        showSnackbar(undefined, "noAttributesToShow");
                    } else {
                        showSnackbar(undefined, "errorOccurred");
                    }
                }
            };

            if (this.widgetConfig.allOfType) {
                await queryAssets();
            } else {
                await queryAssets(this.widgetConfig.assetIds!);
            }

            this.notifyConfigUpdate();
        }
     }

    protected onShowLabelsToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showLabels = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onShowUnitsToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showUnits = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onThresholdsChange(ev: ThresholdChangeEvent) {
        this.widgetConfig.thresholds = ev.detail;
        this.notifyConfigUpdate();
    }

    protected onBoolColorsChange(ev: BoolColorsChangeEvent) {
        this.widgetConfig.boolColors = ev.detail as MapMarkerColours;
        this.notifyConfigUpdate();
    }

    protected onTextColorsChange(ev: TextColorsChangeEvent) {
        this.widgetConfig.textColors = ev.detail;
        this.notifyConfigUpdate();
    }
}
