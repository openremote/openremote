import {
    css,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    query,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {AssetAttribute, AttributeRef, DatapointInterval, ValueDatapoint, ValueType} from "@openremote/model";
import manager, {
    AssetModelUtil,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    Util
} from "@openremote/core";
import "@openremote/or-input";
import "@openremote/or-panel";
import "@openremote/or-translate";
import Chart, {ChartTooltipCallback} from "chart.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {MDCDataTable} from "@material/data-table";
import {JSONPath} from "jsonpath-plus";
import moment from "moment";
import {styleMap} from "lit-html/directives/style-map";

export class OrAttributeHistoryEvent extends CustomEvent<OrAttributeHistoryEventDetail> {

    public static readonly NAME = "or-attribute-history-event";

    constructor(value?: any, previousValue?: any) {
        super(OrAttributeHistoryEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
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
    styles?: { [style: string]: string };
    headerStyles?: { [style: string]: string };
    contentProvider?: (datapoint: ValueDatapoint<any>, value: any, config: TableColumnConfig) => TemplateResult | any | undefined;
}

export interface AssetTableConfig {
    timestampFormat?: string;
    /* Supports extracting columns automatically from the keys of object data, for strings */
    autoColumns?: boolean;
    columns?: TableColumnConfig[];
    styles?: { [style: string]: string };
}

export interface TableConfig {
    default?: AssetTableConfig;
    assetTypes?: {
        [assetType: string]: {
            attributeNames?: {[attributeName: string]: AssetTableConfig};
            attributeValueTypes?: {[attributeValueType: string]: AssetTableConfig};
        };
    };
    attributeNames?: {[attributeName: string]: AssetTableConfig};
    attributeValueTypes?: {[attributeValueType: string]: AssetTableConfig};
}

export interface ChartConfig {
    xLabel?: string;
    yLabel?: string;
}

export interface HistoryConfig {
    table?: TableConfig;
    chart?: ChartConfig;
}

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

// language=CSS
const style = css`
    :host {
        --internal-or-attribute-history-background-color: var(--or-attribute-history-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-attribute-history-text-color: var(--or-attribute-history-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-attribute-history-controls-margin: var(--or-attribute-history-controls-margin, 0 0 20px 0);       
        --internal-or-attribute-history-controls-margin-children: var(--or-attribute-history-controls-margin-children, 0 auto 20px auto);            
        --internal-or-attribute-history-graph-fill-color: var(--or-attribute-history-graph-fill-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-attribute-history-graph-fill-opacity: var(--or-attribute-history-graph-fill-opacity, 1);       
        --internal-or-attribute-history-graph-line-color: var(--or-attribute-history-graph-line-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-attribute-history-graph-point-color: var(--or-attribute-history-graph-point-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-attribute-history-graph-point-border-color: var(--or-attribute-history-graph-point-border-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        --internal-or-attribute-history-graph-point-radius: var(--or-attribute-history-graph-point-radius, 4);
        --internal-or-attribute-history-graph-point-hit-radius: var(--or-attribute-history-graph-point-hit-radius, 20);       
        --internal-or-attribute-history-graph-point-border-width: var(--or-attribute-history-graph-point-border-width, 2);
        --internal-or-attribute-history-graph-point-hover-color: var(--or-attribute-history-graph-point-hover-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));       
        --internal-or-attribute-history-graph-point-hover-border-color: var(--or-attribute-history-graph-point-hover-border-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-attribute-history-graph-point-hover-radius: var(--or-attribute-history-graph-point-hover-radius, 4);      
        --internal-or-attribute-history-graph-point-hover-border-width: var(--or-attribute-history-graph-point-hover-border-width, 2);
        
        display: block;                
    }
    
    :host([hidden]) {
        display: none;
    }
    
    #container {
        display: flex;
        min-width: 0;
        width: 100%;
        height: 100%;
        flex-direction: column;
    }
       
    .button-icon {
        align-self: center;
        padding: 10px;
        cursor: pointer;
    }


    #msg {
        height: 100%;
        width: 100%;
        justify-content: center;
        align-items: center;
        text-align: center;
    }
    
    #msg:not([hidden]) {
        display: flex;    
    }
    
    #controls {
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;
        margin: var(--internal-or-attribute-history-controls-margin);
        
        flex-direction: row;
    }
    
    #controls > * {
        margin: var(--internal-or-attribute-history-controls-margin-children);
    }
    
    #ending-controls {
        max-width: 100%;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
    }
    
    #ending-controls > * {
        padding: 0 3px;
    }
    
    #ending-date {
        min-width: 0;
    }
    
    #chart-container {
        position: relative;
    }
        
    #table-container {
        height: 100%;
    }
    
    #table {
        width: 100%;
        margin-bottom: 10px;
    }
    
    #table > table {
        width: 100%;
        table-layout: fixed;
    }
    
    #table th, #table td {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
`;

@customElement("or-attribute-history")
export class OrAttributeHistory extends translate(i18next)(LitElement) {

    public static DEFAULT_TIMESTAMP_FORMAT = "L HH:mm:ss";

    static get styles() {
        return [
            css`${unsafeCSS(tableStyle)}`,
            style
        ];
    }

    @property({type: String})
    public assetType?: string;

    @property({type: Object})
    public attribute?: AssetAttribute;

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: String})
    public period: moment.unitOfTime.Base = "day";

    @property({type: Number})
    public fromTimestamp?: Date;

    @property({type: Number})
    public toTimestamp?: Date;

    @property({type: Object})
    public config?: HistoryConfig;

    @property()
    protected _loading: boolean = false;

    @property()
    protected _data?: ValueDatapoint<any>[];

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLCanvasElement;
    @query("#table")
    protected _tableElem!: HTMLDivElement;
    protected _table?: MDCDataTable;
    protected _chart?: Chart;
    protected _type?: ValueType | null;
    protected _style!: CSSStyleDeclaration;

    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
    }

    shouldUpdate(_changedProperties: PropertyValues): boolean {

        let returnFalse = false;

        if (!this.fromTimestamp) {
            this.fromTimestamp = new Date();
            returnFalse = true;
        }

        if (returnFalse) {
            // Will update on next pass
            return false;
        }

        let reloadData = _changedProperties.has("period") || _changedProperties.has("fromTimestamp");

        if (_changedProperties.has("attributeRef") || _changedProperties.has("attribute")) {
            this._type = undefined;
            this._cleanup();
            reloadData = true;
        }

        if (reloadData) {
            this._data = undefined;
            this._loadData();
        }

        return super.shouldUpdate(_changedProperties);
    }

    render() {

        const isChart = this._type === ValueType.NUMBER || this._type === ValueType.BOOLEAN;
        const disabled = this._loading || !this._type;

        return html`
            <div id="container">
                <div id="controls">
                    <or-input  .type="${InputType.SELECT}" ?disabled="${disabled}" .label="${i18next.t("timeframe")}" @or-input-changed="${(evt: OrInputChangedEvent) => this.period = evt.detail.value}" .value="${this.period}" .options="${this._getPeriodOptions().map(item => item.value)}"></or-input>
                    <div id="ending-controls">
                        <or-input id="ending-date" .type="${InputType.DATETIME}" ?disabled="${disabled}" label="${i18next.t("ending")}" .value="${this.fromTimestamp}" @or-input-changed="${(evt: OrInputChangedEvent) => this._updateTimestamp(moment(evt.detail.value as string).toDate())}"></or-input>
                        <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-left" @click="${() => this.fromTimestamp = this._updateTimestamp(this.fromTimestamp!, false)}"></or-icon>
                        <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-right" @click="${() => this.fromTimestamp = this._updateTimestamp(this.fromTimestamp!, true)}"></or-icon>
                        <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-double-right" @click="${() => this.fromTimestamp = this._updateTimestamp(new Date())}"></or-icon>
                    </div>
                </div>
                
                ${!this._type ? html`
                    <div id="msg">
                        <or-translate value="invalidAttribute"></or-translate>
                    </div>
                ` : isChart
            ? html`
                        <div id="chart-container">
                            <canvas id="chart"></canvas>
                        </div>
                    ` : html`
                        <div id="table-container">
                            ${this._tableTemplate || ``}
                        </div>
                    `}                
            </div>
        `;
    }

    updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        if (!this._type || !this._data) {
            return;
        }

        const isChart = this._type === ValueType.NUMBER || this._type === ValueType.BOOLEAN;

        if (isChart) {
            if (!this._chart) {
                let bgColor = this._style.getPropertyValue("--internal-or-attribute-history-graph-fill-color").trim();
                const opacity = Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-fill-opacity").trim());
                if (!isNaN(opacity)) {
                    if (bgColor.startsWith("#") && (bgColor.length === 4 || bgColor.length === 7)) {
                        bgColor += (bgColor.length === 4 ? Math.round(opacity * 255).toString(16).substr(0, 1) : Math.round(opacity * 255).toString(16));
                    } else if (bgColor.startsWith("rgb(")) {
                        bgColor = bgColor.substring(0, bgColor.length - 1) + opacity;
                    }
                }

                this._chart = new Chart(this._chartElem, {
                    type: "bar",
                    data: {
                        datasets: [
                            {
                                data: this._data,
                                backgroundColor: bgColor,
                                borderColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-line-color"),
                                pointBorderColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-point-border-color"),
                                pointBackgroundColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-point-color"),
                                pointRadius: Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-point-radius")),
                                pointBorderWidth: Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-point-border-width")),
                                pointHoverBackgroundColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-point-hover-color"),
                                pointHoverBorderColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-point-hover-border-color"),
                                pointHoverRadius: Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-point-hover-radius")),
                                pointHoverBorderWidth: Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-point-hover-border-width")),
                                pointHitRadius: Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-point-hit-radius"))
                            }
                        ]
                    },
                    options: {
                        // maintainAspectRatio: false,
                        // REMOVED AS DOESN'T SIZE CORRECTLY responsive: true,
                        onResize:() => this.dispatchEvent(new OrAttributeHistoryEvent('resize')),
                        legend: {
                            display: false
                        },
                        tooltips: {
                            displayColors: false,
                            callbacks: {
                                label: (tooltipItem, data) => {
                                    return tooltipItem.yLabel; // Removes the colon before the label
                                },
                                footer: () => {
                                    return " "; // Hack the broken vertical alignment of body with footerFontSize: 0
                                }
                            } as ChartTooltipCallback
                        },
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero: true
                                },
                                gridLines: {
                                    color: "#cccccc"
                                }
                            }],
                            xAxes: [{
                                type: "time",
                                time: {
                                    displayFormats: {
                                        millisecond: 'HH:mm:ss.SSS',
                                        second: 'HH:mm:ss',
                                        minute: "HH:mm",
                                        hour: "HH:mm",
                                        week: "w"
                                    }
                                },
                                ticks: {
                                    autoSkip: true,
                                    maxTicksLimit: 30,
                                    fontColor: "#000",
                                    fontFamily: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                                    fontSize: 9,
                                    fontStyle: "normal"
                                },
                                gridLines: {
                                    color: "#cccccc"
                                }
                            }]
                        }
                    }
                });
            } else {
                if (changedProperties.has("_data")) {
                    this._chart.data.datasets![0].data = this._data;
                    this._chart.update();
                }
            }
        } else {
            if (!this._tableTemplate || changedProperties.has("_data")) {
                this._tableTemplate = this._getTableTemplate();
            }
        }

        this.onCompleted().then(() => {
            this.dispatchEvent(new OrAttributeHistoryEvent('rendered'));
        });

    }

    async onCompleted() {
        await this.updateComplete;
    }

    protected _cleanup() {
        this._tableTemplate = undefined;

        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
        if (this._table) {
            this._table.destroy();
            this._table = undefined;
        }
    }

    protected _getTableTemplate(): TemplateResult {

        const assetType = this.assetType!;
        const attributeName = this.attribute ? this.attribute.name! : this.attributeRef!.attributeName!;
        const attributeType = this.attribute ? this.attribute.type as string : undefined;

        if (!this._data) {
            return html``;
        }

        let config: AssetTableConfig = {
            autoColumns: true
        };

        if (this.config && this.config.table) {
            if (this.config.table.assetTypes
                && this.config.table.assetTypes[assetType]
                && ((this.config.table.assetTypes[assetType].attributeNames && this.config.table.assetTypes[assetType].attributeNames![attributeName])
                    || (attributeType && this.config.table.assetTypes[assetType].attributeValueTypes && this.config.table.assetTypes[assetType].attributeValueTypes![attributeType]))) {
                config = this.config.table.assetTypes[assetType].attributeNames && this.config.table.assetTypes[assetType].attributeNames![attributeName] || this.config.table.assetTypes[assetType].attributeValueTypes![attributeType!];
            } else if (this.config.table.attributeNames && this.config.table.attributeNames[attributeName]) {
                config = this.config.table.attributeNames[attributeName];
            } else if (attributeType && this.config.table.attributeValueTypes && this.config.table.attributeValueTypes[attributeType]) {
                config = this.config.table.attributeValueTypes[attributeType];
            } else if (this.config.table.default) {
                config = this.config.table.default;
            }

            if (!config) {
                config = {
                    autoColumns: true
                };
            }
        }

        if (config.autoColumns) {
            config = {...config};

            config.columns = [];

            const dp = this._data!.find((dp) => dp.y !== undefined || dp.y !== null);
            if (dp) {
                if (typeof(dp.y) === "object" && !Array.isArray(dp.y)) {
                    config.columns = Object.entries(dp.y as {}).map(([prop, value]) => {
                        return {
                            type: "prop",
                            header: prop,
                            path: "$." + prop,
                            stringify: typeof(value) === "object",
                            numeric: !isNaN(Number(value))
                        } as TableColumnConfig;
                    });
                } else {
                    config.columns.push({
                        type: "prop",
                        stringify: true,
                        numeric: typeof(dp.y) === "number"
                    });
                }
            }

            if (config.columns.length > 0) {
                config.columns.push({
                    header: "timestamp",
                    type: "timestamp"
                });
            }
        }

        if (config.columns && config.columns.length > 0) {
            return html`
            <div id="table" class="mdc-data-table">
                <table style="${config.styles ? styleMap(config.styles) : ""}" class="mdc-data-table__table" aria-label="${attributeName + " history"}">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            ${config.columns.map((c) => html`<th style="${c.headerStyles ? styleMap(c.headerStyles) : ""}" class="mdc-data-table__header-cell ${c.numeric ? "mdc-data-table__header-cell--numeric" : ""}" role="columnheader" scope="col"><or-translate value="${c.header}"></or-translate></th>`)}
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this._data!.map((dp) => {
                            return html`
                                <tr class="mdc-data-table__row">
                                    ${config.columns!.map((c) => html`<td style="${c.styles ? styleMap(c.styles) : ""}" class="mdc-data-table__cell ${c.numeric ? "mdc-data-table__cell--numeric" : ""}">${this._getCellValue(dp, c, config.timestampFormat)}</td>`)}
                                </tr>
                            `;            
                        })}
                    </tbody>
                </table>
            </div>
            `;
        }

        console.warn("OrAttributeHistory: No columns configured so nothing to show");
        return html``;
    }

    protected _getCellValue(datapoint: ValueDatapoint<any>, config: TableColumnConfig, timestampFormat: string | undefined): TemplateResult | string | undefined {

        switch (config.type) {
            case "timestamp":
                const value = moment(datapoint.x).format(timestampFormat || OrAttributeHistory.DEFAULT_TIMESTAMP_FORMAT);
                if (config && config.contentProvider) {
                    const template = config.contentProvider(datapoint, value, config);
                    if (template) {
                        return template;
                    }
                }
                return value;
            case "prop":
                let data = datapoint.y;

                if (config.path) {
                    data = JSONPath({
                        path: config.path,
                        json: datapoint.y,
                        wrap: false
                    });
                    // TODO: Remove once JSONPath updated https://github.com/s3u/JSONPath/issues/86)
                    if (Array.isArray(data) && data.length === 1) {
                        data = data[0];
                    }
                }

                if (config && config.contentProvider) {
                    const template = config.contentProvider(datapoint, data, config);
                    if (template) {
                        return template;
                    }
                }

                if (config.stringify) {
                    return JSON.stringify(data);
                }

                return data;
        }
    }

    protected _getIntervalOptions(): [string, string][] {
        return [
            DatapointInterval.HOUR,
            DatapointInterval.DAY,
            DatapointInterval.WEEK,
            DatapointInterval.MONTH,
            DatapointInterval.YEAR
        ].map((interval) => {
            return [interval, i18next.t(interval.toLowerCase())];
        });
    }
    protected _getPeriodOptions() {
        return [
            {
                text: i18next.t(DatapointInterval.HOUR),
                value: "hour"
            },
            {
                text: i18next.t(DatapointInterval.DAY),
                value: "day"
            },
            {
                text: i18next.t(DatapointInterval.WEEK),
                value: "week"
            },
            {
                text: i18next.t(DatapointInterval.MONTH),
                value: "month"
            },
            {
                text: i18next.t(DatapointInterval.YEAR),
                value: "year"
            }
        ];
    }

    protected _getInterval() {
        let interval: DatapointInterval = DatapointInterval.HOUR;
        switch (this.period) {
            case "hour":
                interval = DatapointInterval.MINUTE;
                break;
            case "day":
                interval = DatapointInterval.HOUR;
                break;
            case "week":
                interval = DatapointInterval.HOUR;
                break;
            case "month":
                interval = DatapointInterval.DAY;
                break;
            case "year":
                interval = DatapointInterval.MONTH;
                break;
        }
        return interval;
    }

    protected async _loadData() {
        if (this._loading || this._type === null || !this.assetType || (!this.attribute && !this.attributeRef)) {
            return;
        }

        this._loading = true;
        const assetId = this.attribute ? this.attribute.assetId! : this.attributeRef!.entityId!;
        const attributeName = this.attribute ? this.attribute.name! : this.attributeRef!.attributeName!;

        if (this._type === undefined) {
            let attr = this.attribute;

            if (!attr) {
                const response = await manager.rest.api.AssetResource.queryAssets({
                    ids: [assetId],
                    select: {
                        excludeParentInfo: true,
                        excludePath: true,
                        excludeAttributeMeta: true,
                        attributes: [
                            attributeName
                        ]
                    }
                });
                if (response.status === 200 && response.data.length > 0) {
                    attr = Util.getAssetAttribute(response.data[0], attributeName);
                }
            }

            if (attr) {
                const attributeType = attr.type as string;
                const attrDescriptor = AssetModelUtil.getAttributeValueDescriptorFromAsset(attributeType, this.assetType, attributeName);

                if (attrDescriptor && attrDescriptor.valueType) {
                    this._type = attrDescriptor.valueType;
                } else {
                    this._type = null;
                }
            } else {
                this._type = null;
            }
        }

        if (!this._type) {
            this._loading = false;
            return;
        }

        if (!this.period || !this.fromTimestamp) {
            this._loading = false;
            return;
        }
        const startOfPeriod = moment(this.fromTimestamp).subtract(1, this.period).toDate().getTime();
        const endOfPeriod = moment(this.fromTimestamp).toDate().getTime();

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            assetId,
            attributeName,
            {
                interval: this._getInterval(),
                fromTimestamp: startOfPeriod,
                toTimestamp: endOfPeriod
            }
        );

        this._loading = false;

        if (response.status === 200) {
            this._data = response.data;
        }
    }
    protected _updateTimestamp(timestamp: Date, forward?: boolean) {
        const newMoment = moment(timestamp);

        if (forward !== undefined) {
            newMoment.add(forward ? 1 : -1, this.period);
        }

        return newMoment.toDate();
    }
    
}
