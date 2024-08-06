import { TemplateResult } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { WidgetConfig } from "../util/widget-config";
import { AttributeRef } from "@openremote/model";
import { WidgetManifest } from "../util/or-widget";
import "@openremote/or-gauge";
export interface GaugeWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    thresholds: [number, string][];
    decimals: number;
    min: number;
    max: number;
    valueType: string;
}
export declare class GaugeWidget extends OrAssetWidget {
    protected widgetConfig: GaugeWidgetConfig;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    protected updated(changedProps: Map<string, any>): void;
    protected loadAssets(attributeRefs: AttributeRef[]): void;
    protected render(): TemplateResult;
}
