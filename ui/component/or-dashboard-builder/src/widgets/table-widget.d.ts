import { PropertyValues, TemplateResult } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { WidgetManifest } from "../util/or-widget";
import { WidgetConfig } from "../util/widget-config";
import { OrMwcTableRowClickEvent, TableColumn, TableRow } from "@openremote/or-mwc-components/or-mwc-table";
import "@openremote/or-mwc-components/or-mwc-table";
export interface TableWidgetConfig extends WidgetConfig {
    assetType?: string;
    assetIds: string[];
    attributeNames: string[];
    tableSize: number;
    tableOptions: number[];
}
export declare class TableWidget extends OrAssetWidget {
    protected widgetConfig: TableWidgetConfig;
    static getManifest(): WidgetManifest;
    static get styles(): import("lit").CSSResult[];
    refreshContent(force: boolean): void;
    protected willUpdate(changedProps: PropertyValues): void;
    protected loadAssets(): void;
    protected getColumns(attributeNames: string[]): TableColumn[];
    protected getRows(attributeNames: string[]): TableRow[];
    protected render(): TemplateResult;
    protected onTableRowClick(ev: OrMwcTableRowClickEvent): void;
}
