var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var GaugeWidget_1;
import { html } from "lit";
import { customElement } from "lit/decorators.js";
import { OrAssetWidget } from "../util/or-asset-widget";
import { GaugeSettings } from "../settings/gauge-settings";
import { when } from "lit/directives/when.js";
import "@openremote/or-gauge";
function getDefaultWidgetConfig() {
    return {
        attributeRefs: [],
        thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]], // colors from https://mui.com/material-ui/customization/palette/ as reference (since material has no official colors)
        decimals: 0,
        min: 0,
        max: 100,
        valueType: 'number',
    };
}
let GaugeWidget = GaugeWidget_1 = class GaugeWidget extends OrAssetWidget {
    static getManifest() {
        return {
            displayName: "Gauge",
            displayIcon: "gauge",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config) {
                return new GaugeWidget_1(config);
            },
            getSettingsHtml(config) {
                return new GaugeSettings(config);
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            }
        };
    }
    refreshContent(force) {
        this.loadAssets(this.widgetConfig.attributeRefs);
    }
    // WebComponent lifecycle method, that occurs AFTER every state update
    updated(changedProps) {
        super.updated(changedProps);
        // If widgetConfig, and the attributeRefs of them have changed...
        if (changedProps.has("widgetConfig") && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;
            // Check if list of attributes has changed, based on the cached assets
            const loadedRefs = attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.filter((attrRef) => this.isAttributeRefLoaded(attrRef));
            if ((loadedRefs === null || loadedRefs === void 0 ? void 0 : loadedRefs.length) !== (attributeRefs ? attributeRefs.length : 0)) {
                // Fetch the new list of assets
                this.loadAssets(attributeRefs);
            }
        }
        return super.updated(changedProps);
    }
    loadAssets(attributeRefs) {
        this.fetchAssets(attributeRefs).then((assets) => {
            this.loadedAssets = assets;
            this.assetAttributes = attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.map((attrRef) => {
                const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name]] : undefined;
            }).filter((indexAndAttr) => !!indexAndAttr);
        });
    }
    render() {
        return html `
            ${when(this.loadedAssets && this.assetAttributes && this.loadedAssets.length > 0 && this.assetAttributes.length > 0, () => {
            return html `
                    <or-gauge .asset="${this.loadedAssets[0]}" .assetAttribute="${this.assetAttributes[0]}" .thresholds="${this.widgetConfig.thresholds}"
                              .decimals="${this.widgetConfig.decimals}" .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                              style="height: 100%; overflow: hidden;">
                    </or-gauge>
                `;
        }, () => {
            return html `
                    <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                        <span><or-translate value="noAttributeConnected"></or-translate></span>
                    </div>
                `;
        })}
        `;
    }
};
GaugeWidget = GaugeWidget_1 = __decorate([
    customElement("gauge-widget")
], GaugeWidget);
export { GaugeWidget };
//# sourceMappingURL=gauge-widget.js.map