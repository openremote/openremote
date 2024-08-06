import { LitElement, PropertyValues, TemplateResult } from "lit";
import { Attribute, AttributeRef, ValueDatapoint, ValueDescriptor } from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import "@openremote/or-chart";
import { Chart, ScatterDataPoint, TimeUnit } from "chart.js";
import "chartjs-adapter-moment";
import { MDCDataTable } from "@material/data-table";
import moment from "moment";
export declare class OrAttributeHistoryEvent extends CustomEvent<OrAttributeHistoryEventDetail> {
    static readonly NAME = "or-attribute-history-event";
    constructor(value?: any, previousValue?: any);
}
export interface OrAttributeHistoryEventDetail {
    value?: any;
    previousValue?: any;
}
declare global {
    export interface HTMLElementEventMap {
        [OrAttributeHistoryEvent.NAME]: OrAttributeHistoryEvent;
    }
}
export type TableColumnType = "timestamp" | "prop";
export interface TableColumnConfig {
    type?: TableColumnType;
    header?: string;
    numeric?: boolean;
    path?: string;
    stringify?: boolean;
    styles?: {
        [style: string]: string;
    };
    headerStyles?: {
        [style: string]: string;
    };
    contentProvider?: (datapoint: ValueDatapoint<any>, value: any, config: TableColumnConfig) => TemplateResult | any | undefined;
}
export interface AssetTableConfig {
    timestampFormat?: string;
    autoColumns?: boolean;
    columns?: TableColumnConfig[];
    styles?: {
        [style: string]: string;
    };
}
export interface TableConfig {
    default?: AssetTableConfig;
    assetTypes?: {
        [assetType: string]: {
            attributeNames?: {
                [attributeName: string]: AssetTableConfig;
            };
            attributeValueTypes?: {
                [attributeValueType: string]: AssetTableConfig;
            };
        };
    };
    attributeNames?: {
        [attributeName: string]: AssetTableConfig;
    };
    attributeValueTypes?: {
        [attributeValueType: string]: AssetTableConfig;
    };
}
export interface ChartConfig {
    xLabel?: string;
    yLabel?: string;
}
export interface HistoryConfig {
    table?: TableConfig;
    chart?: ChartConfig;
}
declare const OrAttributeHistory_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrAttributeHistory extends OrAttributeHistory_base {
    static DEFAULT_TIMESTAMP_FORMAT: string;
    static get styles(): import("lit").CSSResult[];
    assetType?: string;
    assetId?: string;
    attribute?: Attribute<any>;
    attributeRef?: AttributeRef;
    period: moment.unitOfTime.Base;
    toTimestamp?: Date;
    config?: HistoryConfig;
    protected _loading: boolean;
    protected _data?: ValueDatapoint<any>[];
    protected _tableTemplate?: TemplateResult;
    protected _chartElem: HTMLCanvasElement;
    protected _tableElem: HTMLDivElement;
    protected _table?: MDCDataTable;
    protected _chart?: Chart<"line", ScatterDataPoint[]>;
    protected _type?: ValueDescriptor;
    protected _style: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _timeUnits?: TimeUnit;
    protected _stepSize?: number;
    protected _updateTimestampTimer?: number;
    protected _dataFirstLoaded: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(_changedProperties: PropertyValues): boolean;
    render(): TemplateResult<1>;
    willUpdate(changedProperties: PropertyValues): void;
    onCompleted(): Promise<void>;
    protected _cleanup(): void;
    protected _getTableTemplate(): TemplateResult;
    protected _getCellValue(datapoint: ValueDatapoint<any>, config: TableColumnConfig, timestampFormat: string | undefined): TemplateResult | string | undefined;
    protected _getIntervalOptions(): [string, string][];
    protected _getPeriodOptions(): string[];
    protected _loadData(): Promise<void>;
    protected _updateTimestamp(timestamp: Date, forward?: boolean, timeout?: number): void;
}
export {};
