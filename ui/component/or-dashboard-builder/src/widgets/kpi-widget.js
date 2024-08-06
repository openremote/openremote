var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var KpiWidget_1;
import { customElement } from "lit/decorators.js";
import { OrAssetWidget } from "../util/or-asset-widget";
import { html } from "lit";
import { KpiSettings } from "../settings/kpi-settings";
import "@openremote/or-attribute-card";
function getDefaultWidgetConfig() {
    return {
        attributeRefs: [],
        period: "day",
        decimals: 0,
        deltaFormat: "absolute",
        showTimestampControls: false
    };
}
let KpiWidget = KpiWidget_1 = class KpiWidget extends OrAssetWidget {
    static getManifest() {
        return {
            displayName: "KPI",
            displayIcon: "label",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config) {
                return new KpiWidget_1(config);
            },
            getSettingsHtml(config) {
                return new KpiSettings(config);
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            }
        };
    }
    refreshContent(force) {
        this.loadAssets(this.widgetConfig.attributeRefs);
    }
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
        var _a;
        return html `
            <div style="height: 100%; overflow: hidden;">
                <or-attribute-card .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}" .period="${this.widgetConfig.period}"
                                   .deltaFormat="${this.widgetConfig.deltaFormat}" .mainValueDecimals="${this.widgetConfig.decimals}"
                                   showControls="${(_a = this.widgetConfig) === null || _a === void 0 ? void 0 : _a.showTimestampControls}" showTitle="${false}" style="height: 100%;">
                </or-attribute-card>
            </div>
        `;
    }
};
KpiWidget = KpiWidget_1 = __decorate([
    customElement("kpi-widget")
], KpiWidget);
export { KpiWidget };
//# sourceMappingURL=kpi-widget.js.map