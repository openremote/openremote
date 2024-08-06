import { PropertyValues, TemplateResult } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { WidgetManifest } from "../util/or-widget";
import { WidgetConfig } from "../util/widget-config";
import { Asset, AssetDescriptor, AttributeRef } from "@openremote/model";
import { LngLatLike, MapMarkerColours } from "@openremote/or-map";
import "@openremote/or-map";
export interface MapWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    zoom?: number;
    center?: LngLatLike;
    lat?: number;
    lng?: number;
    showLabels: boolean;
    showUnits: boolean;
    showGeoJson: boolean;
    boolColors: MapMarkerColours;
    textColors: [string, string][];
    thresholds: [number, string][];
    min?: number;
    max?: number;
    assetType?: string;
    valueType?: string;
    attributeName?: string;
    assetTypes: AssetDescriptor[];
    assetIds: string[];
    attributes: string[];
}
export declare class MapWidget extends OrAssetWidget {
    protected widgetConfig: MapWidgetConfig;
    private markers;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    protected updated(changedProps: PropertyValues): void;
    protected loadAssets(): Promise<void>;
    protected fetchAssetsByType(assetTypes: string[], attributeName: string): Promise<Asset[]>;
    protected render(): TemplateResult;
    protected getMarkerTemplates(): TemplateResult[];
}
