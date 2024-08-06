import { OrAssetWidget } from "../util/or-asset-widget";
import { WidgetManifest } from "../util/or-widget";
import { WidgetConfig } from "../util/widget-config";
import { AttributeRef } from "@openremote/model";
import { TemplateResult } from "lit";
import "@openremote/or-attribute-card";
export interface KpiWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    period?: 'year' | 'month' | 'week' | 'day' | 'hour';
    decimals: number;
    deltaFormat: "absolute" | "percentage";
    showTimestampControls: boolean;
}
export declare class KpiWidget extends OrAssetWidget {
    protected widgetConfig: KpiWidgetConfig;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    protected updated(changedProps: Map<string, any>): void;
    protected loadAssets(attributeRefs: AttributeRef[]): void;
    protected render(): TemplateResult;
}
