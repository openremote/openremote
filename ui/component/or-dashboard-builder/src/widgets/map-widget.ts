import {html, PropertyValues, TemplateResult} from "lit";
import {customElement, query, state} from "lit/decorators.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {MapSettings} from "../settings/map-settings";
import {AssetWidgetConfig} from "../util/widget-config";
import {Asset, AssetDescriptor} from "@openremote/model";
import {LngLatLike, MapMarkerColours, MapMarkerAssetConfig, Util as MapUtil, OrMap, AssetWithLocation, OrMapMarkersChangedEvent, MapMarkerConfig} from "@openremote/or-map";
import {map} from "lit/directives/map.js";
import manager from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import "@openremote/or-map";

export interface MapWidgetConfig extends AssetWidgetConfig {
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
    allOfType?: boolean,
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
        textColors: [['example1', '4caf50'], ['example2', 'ef5350']],
        thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]],
        assetTypes: [],
        assetType: undefined,
        allOfType: true,
        assetIds: [],
        attributes: [],
    } as MapWidgetConfig;
}

@customElement("map-widget")
export class MapWidget extends OrAssetWidget {

    protected widgetConfig!: MapWidgetConfig;

    @state()
    protected _assetsOnScreen: AssetWithLocation[] = [];

    @query("or-map")
    protected _map?: OrMap;

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
        };
    }

    connectedCallback() {
        this.addEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
        return super.connectedCallback();
    }

    disconnectedCallback() {
        this.removeEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
        return super.disconnectedCallback();
    }

    refreshContent(force: boolean): void {
        this._loadAssets();
    }

    protected updated(changedProps: PropertyValues) {
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this._loadAssets();
        }
        if(changedProps.has("loadedAssets") && this.loadedAssets) {
            this._loadMarkers();
        }
    }

    protected _loadAssets() {
        if(!this.widgetConfig.assetType || !this.widgetConfig.attributeName) {
            console.debug("Could not load assets for Map widget, as the configuration is not complete! (no asset type, or no attribute)");
            return;
        }
        this._fetchAssets([this.widgetConfig.assetType], this.widgetConfig.attributeName, this.widgetConfig.allOfType ? undefined : this.widgetConfig.assetIds).then(assets => {
            this.loadedAssets = assets;
        });
    }

    protected async _fetchAssets(assetTypes: string[], attributeName: string, assetIds?: string[]): Promise<Asset[]> {
        let assets: Asset[] = [];
        try {
            const response = await manager.rest.api.AssetResource.queryAssets({
                realm: {
                    name: manager.displayRealm
                },
                select: {
                    attributes: [attributeName, "location"]
                },
                types: assetTypes,
                ids: assetIds?.length ? assetIds : undefined
            });
            assets = response.data;

        } catch (reason) {
            console.error(reason);
            showSnackbar(undefined, "errorOccurred");
        }
        return assets;
    }

    protected render(): TemplateResult {
        return html`
            <div style="height: 100%; display: flex; flex-direction: column; overflow: hidden;">
                <or-map id="miniMap" class="or-map" .zoom="${this.widgetConfig.zoom}" .center="${this.widgetConfig.center}" .showGeoJson="${this.widgetConfig.showGeoJson}" style="flex: 1;">
                    ${map(this._assetsOnScreen, (asset) => html`
                        <or-map-marker-asset .asset="${asset}" .config="${this.markers}"></or-map-marker-asset>
                    `)}
                </or-map>
            </div>
        `;
    }

    protected _onMapMarkersChanged(e: OrMapMarkersChangedEvent) {
        this._assetsOnScreen = e.detail;
    }

    protected _loadMarkers(): void {
        this.markers = {};

        // Loop through each asset type, and configure the marker
        for (const assetType of new Set(this.loadedAssets.map(a => a.type).filter(Boolean))) {
            const marker: MapMarkerConfig = {
                attributeName: this.widgetConfig.attributeName!,
                showUnits: this.widgetConfig.showUnits,
                showLabel: this.widgetConfig.showLabels
            };
            switch (this.widgetConfig.valueType) {
                case "boolean": {
                    const boolColors = this.widgetConfig.boolColors as any;
                    marker.colours = {
                        type: "boolean",
                        true: boolColors.true.replace("#", ""),
                        false: boolColors.false.replace("#", "")
                    };
                    break;
                }
                case "text": {
                    marker.colours = this.widgetConfig.textColors.reduce((colors: any, [key, val]) => {
                        colors[key] = val.replace("#", "");
                        return colors;
                    }, { type: "string" });
                    break;
                }
                default: {
                    marker.colours = {
                        type: "range",
                        ranges: this.widgetConfig.thresholds
                            .sort((a, b) => b[0] - a[0])
                            .map(([min, colour]) => ({min, colour: colour.replace('#', '')}))
                    };
                    break;
                }
            }
            this.markers[assetType!] = marker;
        }
        // Load the markers onto the map
        if (this._map) {
            this._map.cleanUpAssetMarkers();
            const assetType = this.widgetConfig.allOfType ? undefined : this.widgetConfig.assetType;
            this.loadedAssets
                .filter(asset => MapUtil.isAssetWithLocation(asset) && (!assetType || asset.type === assetType))
                .forEach((asset: Asset) => this._map!.addAssetMarker(asset as AssetWithLocation));

            this._map?.reload();
        }
    }
}
