import {html, PropertyValues, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {MapSettings} from "../settings/map-settings";
import {WidgetConfig} from "../util/widget-config";
import {Asset, AssetDescriptor, Attribute, AttributeRef, GeoJSONPoint, WellknownAttributes, WellknownMetaItems} from "@openremote/model";
import {
    LngLatLike,
    AttributeMarkerColours,
    RangeAttributeMarkerColours,
    AttributeMarkerColoursRange,
    MapMarkerColours, MapMarkerAssetConfig,
} from "@openremote/or-map";
import {when} from "lit/directives/when.js";
import manager, {Util} from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import "@openremote/or-map";

export interface MapWidgetConfig extends WidgetConfig {
    // General values
    attributeRefs: AttributeRef[];
    // Map related values
    zoom?: number,
    center?: LngLatLike,
    lat?: number,
    lng?: number,
    // Map marker related values
    showLabels: boolean,
    showUnits: boolean,
    showGeoJson: boolean,
    boolColors: MapMarkerColours,
    textColors: [string, string][],
    // Threshold related values
    thresholds: [number, string][],
    min?: number,
    max?: number,
    // Asset type related values
    assetType?: string,
    valueType?: string,
    attributeName?: string,
    assetTypes: AssetDescriptor[],
    assetIds: string[],
    attributes: string[],
}

function getDefaultWidgetConfig(): MapWidgetConfig {
    return {
        attributeRefs: [],
        showLabels: false,
        showUnits: false,
        showGeoJson: true,
        boolColors: {type: 'boolean', 'false': '#ef5350', 'true': '#4caf50'},
        textColors: [['example', '4caf50'], ['example2', 'ef5350']],
        thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]],
        assetTypes: [],
        assetType: undefined,
        assetIds: [],
        attributes: [],
    } as MapWidgetConfig;
}

@customElement("map-widget")
export class MapWidget extends OrAssetWidget {

    protected widgetConfig!: MapWidgetConfig

    private markers: MapMarkerAssetConfig = {};

    static getManifest(): WidgetManifest {
        return {
            displayName: "Map",
            displayIcon: "map",
            minColumnWidth: 2,
            minColumnHeight: 2,
            getContentHtml(config: MapWidgetConfig): OrWidget {
                return new MapWidget(config);
            },
            getSettingsHtml(config: MapWidgetConfig): WidgetSettings {
                return new MapSettings(config);
            },
            getDefaultConfig(): MapWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    refreshContent(force: boolean): void {
        this.loadAssets();
    }

    protected updated(changedProps: PropertyValues) {
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.loadAssets();
        }
    }

    protected async loadAssets() {
        if(this.widgetConfig.assetType && this.widgetConfig.attributeName) {
            this.fetchAssetsByType([this.widgetConfig.assetType], this.widgetConfig.attributeName).then((assets) => {
                this.loadedAssets = assets;
            });
        }
    }

    protected async fetchAssetsByType(assetTypes: string[], attributeName: string) {
        let assets: Asset[] = [];
        await manager.rest.api.AssetResource.queryAssets({
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
    }


    protected render(): TemplateResult {
        return html`
            <div style="height: 100%; display: flex; flex-direction: column; overflow: hidden;">
                <or-map id="miniMap" class="or-map" .zoom="${this.widgetConfig.zoom}" .center="${this.widgetConfig.center}" .showGeoJson="${this.widgetConfig.showGeoJson}" style="flex: 1;">
                    ${when(this.loadedAssets, () => {
                        return this.getMarkerTemplates();
                    })}
                </or-map>
            </div>
        `;
    }

    protected getMarkerTemplates(): TemplateResult[] {
        return this.loadedAssets.filter((asset: Asset) => {
            if (!asset.attributes) {
                return false;
            }
            const attr = asset.attributes[WellknownAttributes.LOCATION] as Attribute<GeoJSONPoint>;
            return !attr || !attr.meta || !attr.meta.hasOwnProperty(WellknownMetaItems.SHOWONDASHBOARD) || !!Util.getMetaValue(WellknownMetaItems.SHOWONDASHBOARD, attr);
        }).map(asset => {
            if (this.markers) {
                // Configure map marker asset settings
                this.markers[asset.type!] = {attributeName: this.widgetConfig.attributeName!};
                this.markers[asset.type!].showUnits = this.widgetConfig.showUnits;
                this.markers[asset.type!].showLabel = this.widgetConfig.showLabels;
                if (this.widgetConfig.valueType == 'boolean') {
                    (this.widgetConfig.boolColors as any).true = (this.widgetConfig.boolColors as any).true.replace("#", "");
                    (this.widgetConfig.boolColors as any).false = (this.widgetConfig.boolColors as any).false.replace("#", "");
                    this.markers[asset.type!].colours = this.widgetConfig.boolColors;
                } else if (this.widgetConfig.valueType == 'text') {
                    var colors: AttributeMarkerColours = {type: 'string',};
                    (this.widgetConfig.textColors as [string, string][]).map((threshold) => {
                        colors[threshold[0] as string] = (threshold[1] as string).replace('#', '');
                    })
                    this.markers[asset.type!].colours = colors;
                } else {
                    var ranges: AttributeMarkerColoursRange[] = [];
                    (this.widgetConfig.thresholds as [number, string][]).sort((x, y) => (x[0] > y[0]) ? -1 : 1).map((threshold, index) => {
                        var range: AttributeMarkerColoursRange = {
                            min: threshold[0],
                            colour: threshold[1].replace('#', '')
                        }
                        ranges.push(range);
                    })
                    var colorsNum: RangeAttributeMarkerColours = {
                        type: 'range',
                        ranges: ranges
                    };
                    this.markers[asset.type!].colours = colorsNum;
                }
            }
            return html`
                <or-map-marker-asset .asset="${asset}" .config="${this.markers}"></or-map-marker-asset>
            `
        })
    }


}
