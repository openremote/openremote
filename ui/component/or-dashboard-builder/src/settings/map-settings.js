var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { i18next } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import "../panels/assettypes-panel";
import "../panels/thresholds-panel";
import { LngLat } from "maplibre-gl"; // TODO: Replace this import
import { when } from "lit/directives/when.js";
import manager from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
const styling = css `
  .switchMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;
let MapSettings = class MapSettings extends AssetWidgetSettings {
    static get styles() {
        return [...super.styles, styling];
    }
    render() {
        const allowedValueTypes = ["boolean", "number", "positiveInteger", "positiveNumber", "negativeInteger", "negativeNumber", "text"];
        const config = {
            attributes: {
                enabled: true,
                valueTypes: allowedValueTypes
            }
        };
        return html `
            <div>

                <!-- Map settings -->
                <settings-panel displayName="configuration.mapSettings" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;"
                                          .value="${this.widgetConfig.zoom}" label="${i18next.t('dashboard.zoom')}"
                                          @or-mwc-input-changed="${(ev) => this.onZoomUpdate(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;"
                                          .value="${this.widgetConfig.center ? (Object.values(this.widgetConfig.center))[0] + ', ' + (Object.values(this.widgetConfig.center))[1] : undefined}"
                                          label="${i18next.t('dashboard.center')}"
                                          @or-mwc-input-changed="${(ev) => this.onCenterUpdate(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><or-translate value="dashboard.showGeoJson"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showGeoJson}"
                                          @or-mwc-input-changed="${(ev) => this.onGeoJsonToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- Panel where Asset type and the selected attribute can be customized -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <assettypes-panel .assetType="${this.widgetConfig.assetType}" .attributeNames="${this.widgetConfig.attributeName}" .config="${config}"
                                      @assettype-select="${(ev) => this.onAssetTypeSelect(ev)}"
                                      @attributenames-select="${(ev) => this.onAttributeNameSelect(ev)}"
                    ></assettypes-panel>

                    <!-- Other settings like labels and units-->
                    <div>
                        <div class="switchMwcInputContainer">
                            <span><or-translate value="dashboard.showLabels"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showLabels}" .disabled="${!this.widgetConfig.assetType}"
                                          @or-mwc-input-changed="${(ev) => this.onShowLabelsToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div class="switchMwcInputContainer">
                            <span><or-translate value="dashboard.showUnits"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showUnits}" .disabled="${!this.widgetConfig.showLabels || !this.widgetConfig.assetType}"
                                          @or-mwc-input-changed="${(ev) => this.onShowUnitsToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- List of customizable thresholds -->
                ${when(this.widgetConfig.assetIds.length > 0, () => html `
                    <settings-panel displayName="thresholds" expanded="${true}">
                        <thresholds-panel .thresholds="${this.widgetConfig.thresholds}" .valueType="${this.widgetConfig.valueType}" style="padding-bottom: 12px;"
                                          .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                                          @threshold-change="${(ev) => this.onThresholdsChange(ev)}">
                        </thresholds-panel>
                    </settings-panel>
                `)}
            </div>
        `;
    }
    onZoomUpdate(ev) {
        this.widgetConfig.zoom = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onCenterUpdate(ev) {
        if (ev.detail.value) {
            const lngLatArr = ev.detail.value.split(/[, ]/).filter(v => !!v);
            if (lngLatArr.length === 2) {
                const value = new LngLat(Number.parseFloat(lngLatArr[0]), Number.parseFloat(lngLatArr[1]));
                this.widgetConfig.center = value;
                this.notifyConfigUpdate();
            }
        }
    }
    onGeoJsonToggle(ev) {
        this.widgetConfig.showGeoJson = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onAssetTypeSelect(ev) {
        if (this.widgetConfig.assetType !== ev.detail) {
            this.widgetConfig.attributeName = undefined;
            this.widgetConfig.assetIds = [];
            this.widgetConfig.showLabels = false;
            this.widgetConfig.showUnits = false;
            this.widgetConfig.boolColors = { type: 'boolean', 'false': '#ef5350', 'true': '#4caf50' };
            this.widgetConfig.textColors = [['example', "#4caf50"], ['example2', "#ff9800"]];
            this.widgetConfig.thresholds = [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]];
            this.widgetConfig.assetType = ev.detail;
            this.notifyConfigUpdate();
        }
    }
    onAttributeNameSelect(ev) {
        return __awaiter(this, void 0, void 0, function* () {
            const attrName = ev.detail;
            this.widgetConfig.attributeName = attrName;
            yield manager.rest.api.AssetResource.queryAssets({
                realm: {
                    name: manager.displayRealm
                },
                select: {
                    attributes: [attrName, 'location']
                },
                types: [this.widgetConfig.assetType],
            }).then(response => {
                this.widgetConfig.assetIds = response.data.map((a) => a.id);
                this.widgetConfig.valueType = (response.data.length > 0) ? response.data[0].attributes[attrName].type : "text"; // sometimes no asset exists of that assetType, so using 'text' as fallback.
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, "errorOccurred");
            });
            this.notifyConfigUpdate();
        });
    }
    onShowLabelsToggle(ev) {
        this.widgetConfig.showLabels = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onShowUnitsToggle(ev) {
        this.widgetConfig.showUnits = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onThresholdsChange(ev) {
        this.widgetConfig.thresholds = ev.detail;
        this.notifyConfigUpdate();
    }
};
MapSettings = __decorate([
    customElement("map-settings")
], MapSettings);
export { MapSettings };
//# sourceMappingURL=map-settings.js.map