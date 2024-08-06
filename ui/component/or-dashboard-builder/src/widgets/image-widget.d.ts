import { WidgetConfig } from "../util/widget-config";
import { AttributeRef } from "@openremote/model";
import { WidgetManifest } from "../util/or-widget";
import { CSSResult, PropertyValues, TemplateResult } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
export interface ImageAssetMarker {
    attributeRef: AttributeRef;
    coordinates: [number, number];
}
export interface ImageWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    markers: ImageAssetMarker[];
    showTimestampControls: boolean;
    imagePath: string;
}
export declare class ImageWidget extends OrAssetWidget {
    protected readonly widgetConfig: ImageWidgetConfig;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    static get styles(): CSSResult[];
    updated(changedProps: PropertyValues): void;
    protected loadAssets(): void;
    protected handleMarkerPlacement(config: ImageWidgetConfig): TemplateResult<1>[] | undefined;
    protected render(): TemplateResult;
}
