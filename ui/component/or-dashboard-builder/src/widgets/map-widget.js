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
var MapWidget_1;
import { html } from "lit";
import { customElement } from "lit/decorators.js";
import { OrAssetWidget } from "../util/or-asset-widget";
import { MapSettings } from "../settings/map-settings";
import { when } from "lit/directives/when.js";
import manager, { Util } from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import "@openremote/or-map";
function getDefaultWidgetConfig() {
    return {
        attributeRefs: [],
        showLabels: false,
        showUnits: false,
        showGeoJson: true,
        boolColors: { type: 'boolean', 'false': '#ef5350', 'true': '#4caf50' },
        textColors: [['example', '4caf50'], ['example2', 'ef5350']],
        thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]],
        assetTypes: [],
        assetType: undefined,
        assetIds: [],
        attributes: [],
    };
}
let MapWidget = MapWidget_1 = class MapWidget extends OrAssetWidget {
    constructor() {
        super(...arguments);
        this.markers = {};
    }
    static getManifest() {
        return {
            displayName: "Map",
            displayIcon: "map",
            minColumnWidth: 2,
            minColumnHeight: 2,
            getContentHtml(config) {
                return new MapWidget_1(config);
            },
            getSettingsHtml(config) {
                return new MapSettings(config);
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            }
        };
    }
    refreshContent(force) {
        this.loadAssets();
    }
    updated(changedProps) {
        if (changedProps.has('widgetConfig') && this.widgetConfig) {
            this.loadAssets();
        }
    }
    loadAssets() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this.widgetConfig.assetType && this.widgetConfig.attributeName) {
                this.fetchAssetsByType([this.widgetConfig.assetType], this.widgetConfig.attributeName).then((assets) => {
                    this.loadedAssets = assets;
                });
            }
        });
    }
    fetchAssetsByType(assetTypes, attributeName) {
        return __awaiter(this, void 0, void 0, function* () {
            let assets = [];
            yield manager.rest.api.AssetResource.queryAssets({
                realm: {
                    name: manager.displayRealm
                },
                select: {
                    attributes: [attributeName, 'location']
                },
                types: assetTypes,
            }).then(response => {
                assets = response.data;
                this.markers = {};
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, "errorOccurred");
            });
            return assets;
        });
    }
    render() {
        return html `
            <div style="height: 100%; display: flex; flex-direction: column; overflow: hidden;">
                <or-map id="miniMap" class="or-map" .zoom="${this.widgetConfig.zoom}" .center="${this.widgetConfig.center}" .showGeoJson="${this.widgetConfig.showGeoJson}" style="flex: 1;">
                    ${when(this.loadedAssets, () => {
            return this.getMarkerTemplates();
        })}
                </or-map>
            </div>
        `;
    }
    getMarkerTemplates() {
        return this.loadedAssets.filter((asset) => {
            if (!asset.attributes) {
                return false;
            }
            const attr = asset.attributes["location" /* WellknownAttributes.LOCATION */];
            return !attr || !attr.meta || !attr.meta.hasOwnProperty("showOnDashboard" /* WellknownMetaItems.SHOWONDASHBOARD */) || !!Util.getMetaValue("showOnDashboard" /* WellknownMetaItems.SHOWONDASHBOARD */, attr);
        }).map(asset => {
            if (this.markers) {
                // Configure map marker asset settings
                this.markers[asset.type] = { attributeName: this.widgetConfig.attributeName };
                this.markers[asset.type].showUnits = this.widgetConfig.showUnits;
                this.markers[asset.type].showLabel = this.widgetConfig.showLabels;
                if (this.widgetConfig.valueType == 'boolean') {
                    this.widgetConfig.boolColors.true = this.widgetConfig.boolColors.true.replace("#", "");
                    this.widgetConfig.boolColors.false = this.widgetConfig.boolColors.false.replace("#", "");
                    this.markers[asset.type].colours = this.widgetConfig.boolColors;
                }
                else if (this.widgetConfig.valueType == 'text') {
                    var colors = { type: 'string', };
                    this.widgetConfig.textColors.map((threshold) => {
                        colors[threshold[0]] = threshold[1].replace('#', '');
                    });
                    this.markers[asset.type].colours = colors;
                }
                else {
                    var ranges = [];
                    this.widgetConfig.thresholds.sort((x, y) => (x[0] > y[0]) ? -1 : 1).map((threshold, index) => {
                        var range = {
                            min: threshold[0],
                            colour: threshold[1].replace('#', '')
                        };
                        ranges.push(range);
                    });
                    var colorsNum = {
                        type: 'range',
                        ranges: ranges
                    };
                    this.markers[asset.type].colours = colorsNum;
                }
            }
            return html `
                <or-map-marker-asset .asset="${asset}" .config="${this.markers}"></or-map-marker-asset>
            `;
        });
    }
};
MapWidget = MapWidget_1 = __decorate([
    customElement("map-widget")
], MapWidget);
export { MapWidget };
//# sourceMappingURL=map-widget.js.map