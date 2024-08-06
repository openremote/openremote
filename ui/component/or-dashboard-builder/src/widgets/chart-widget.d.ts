import { AssetDatapointLTTBQuery, AssetDatapointQueryUnion, AttributeRef } from "@openremote/model";
import { PropertyValues, TemplateResult } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { WidgetConfig } from "../util/widget-config";
import { WidgetManifest } from "../util/or-widget";
import "@openremote/or-chart";
export interface ChartWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    rightAxisAttributes: AttributeRef[];
    datapointQuery: AssetDatapointQueryUnion;
    chartOptions?: any;
    showTimestampControls: boolean;
    defaultTimePresetKey: string;
    showLegend: boolean;
}
export declare class ChartWidget extends OrAssetWidget {
    protected datapointQuery: AssetDatapointQueryUnion;
    protected widgetConfig: ChartWidgetConfig;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    protected willUpdate(changedProps: PropertyValues): void;
    protected updated(changedProps: Map<string, any>): void;
    protected render(): TemplateResult;
    protected getDefaultQuery(): AssetDatapointLTTBQuery;
}
