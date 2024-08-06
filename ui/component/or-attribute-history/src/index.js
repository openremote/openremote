var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var OrAttributeHistory_1;
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import i18next from "i18next";
import { translate } from "@openremote/or-translate";
import { AssetModelUtil } from "@openremote/model";
import manager, { DefaultColor2, DefaultColor3, DefaultColor4 } from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import "@openremote/or-chart";
import { Chart } from "chart.js";
import "chartjs-adapter-moment";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { JSONPath } from "jsonpath-plus";
import moment from "moment";
import { styleMap } from "lit/directives/style-map.js";
export class OrAttributeHistoryEvent extends CustomEvent {
    constructor(value, previousValue) {
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
OrAttributeHistoryEvent.NAME = "or-attribute-history-event";
// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");
// language=CSS
const style = css `
    :host {
        --internal-or-attribute-history-background-color: var(--or-attribute-history-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-attribute-history-text-color: var(--or-attribute-history-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-attribute-history-controls-margin: var(--or-attribute-history-controls-margin, 10px 0);
        --internal-or-attribute-history-controls-justify-content: var(--or-attribute-history-controls-justify-content, flex-end);
        --internal-or-attribute-history-graph-fill-color: var(--or-attribute-history-graph-fill-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-attribute-history-graph-fill-opacity: var(--or-attribute-history-graph-fill-opacity, 1);       
        --internal-or-attribute-history-graph-line-color: var(--or-attribute-history-graph-line-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-attribute-history-graph-point-color: var(--or-attribute-history-graph-point-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-attribute-history-graph-point-border-color: var(--or-attribute-history-graph-point-border-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-attribute-history-graph-point-radius: var(--or-attribute-history-graph-point-radius, 2);
        --internal-or-attribute-history-graph-point-hit-radius: var(--or-attribute-history-graph-point-hit-radius, 20);       
        --internal-or-attribute-history-graph-point-border-width: var(--or-attribute-history-graph-point-border-width, 2);
        --internal-or-attribute-history-graph-point-hover-color: var(--or-attribute-history-graph-point-hover-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-attribute-history-graph-point-hover-border-color: var(--or-attribute-history-graph-point-hover-border-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
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
        justify-content: var(--internal-or-attribute-history-controls-justify-content);
        margin: var(--internal-or-attribute-history-controls-margin);        
        flex-direction: row;
    }
    
    #time-picker {
        width: 150px;
        padding: 0 5px;
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
        width: 200px;
        padding-left: 5px;
    }
    
    #chart-container {
        position: relative;
        min-height: 250px;
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
let OrAttributeHistory = OrAttributeHistory_1 = class OrAttributeHistory extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.period = "day";
        this._loading = false;
        this._dataFirstLoaded = false;
    }
    static get styles() {
        return [
            css `${unsafeCSS(tableStyle)}`,
            style
        ];
    }
    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        this._cleanup();
    }
    shouldUpdate(_changedProperties) {
        let reloadData = _changedProperties.has("period") || _changedProperties.has("toTimestamp") || _changedProperties.has("attribute");
        if (this._dataFirstLoaded && !_changedProperties.get('_loading') && !reloadData) {
            return false;
        }
        if (!this.toTimestamp) {
            this.toTimestamp = new Date();
            return false;
        }
        if (!this._type && this.attribute) {
            this._type = AssetModelUtil.getValueDescriptor(this.attribute.type) || {};
        }
        if (reloadData) {
            this._type = undefined;
            this._data = undefined;
            this._loadData();
        }
        return super.shouldUpdate(_changedProperties);
    }
    render() {
        const isChart = this._type && (this._type.jsonType === "number" || this._type.jsonType === "boolean");
        const disabled = this._loading || !this._type;
        return html `
            <div id="container">
                <div id="controls">
                    <or-mwc-input id="time-picker" .type="${InputType.SELECT}" ?disabled="${disabled}" .label="${i18next.t("timeframe")}" @or-mwc-input-changed="${(evt) => this.period = evt.detail.value}" .value="${this.period}" .options="${this._getPeriodOptions()}"></or-mwc-input>
                    <div id="ending-controls">
                        <or-mwc-input id="ending-date" .type="${InputType.DATETIME}" ?disabled="${disabled}" label="${i18next.t("ending")}" .value="${this.toTimestamp}" @or-mwc-input-changed="${(evt) => this._updateTimestamp(moment(evt.detail.value).toDate())}"></or-mwc-input>
                        <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-left" @click="${() => this._updateTimestamp(this.toTimestamp, false, 0)}"></or-icon>
                        <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-right" @click="${() => this._updateTimestamp(this.toTimestamp, true, 0)}"></or-icon>
                        <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-double-right" @click="${() => this._updateTimestamp(new Date())}"></or-icon>
                    </div>
                </div>
                
                ${!this._type ? html `
                    <div id="msg">
                        <or-translate value="invalidAttribute"></or-translate>
                    </div>
                ` : isChart ? html `
                        <div id="chart-container">
                            <canvas id="chart"></canvas>
                        </div>
                    ` : html `
                        <div id="table-container">
                            ${this._tableTemplate || ``}
                        </div>
                    `}                
            </div>
        `;
    }
    // Method that is executed during an update, to compute (additional) values before rendering.
    // Used instead of updated() to prevent a second render when properties get changed.
    willUpdate(changedProperties) {
        super.willUpdate(changedProperties);
        if (!this._type || !this._data) {
            return;
        }
        const isChart = this._type && (this._type.jsonType === "number" || this._type.jsonType === "boolean");
        if (isChart) {
            const data = this._data;
            if (!this._chart) {
                let bgColor = this._style.getPropertyValue("--internal-or-attribute-history-graph-fill-color").trim();
                const opacity = Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-fill-opacity").trim());
                if (!isNaN(opacity)) {
                    if (bgColor.startsWith("#") && (bgColor.length === 4 || bgColor.length === 7)) {
                        bgColor += (bgColor.length === 4 ? Math.round(opacity * 255).toString(16).substr(0, 1) : Math.round(opacity * 255).toString(16));
                    }
                    else if (bgColor.startsWith("rgb(")) {
                        bgColor = bgColor.substring(0, bgColor.length - 1) + opacity;
                    }
                }
                const options = {
                    type: "line",
                    data: {
                        datasets: [
                            {
                                data: data,
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
                                pointHitRadius: Number(this._style.getPropertyValue("--internal-or-attribute-history-graph-point-hit-radius")),
                                fill: false
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        onResize: () => this.dispatchEvent(new OrAttributeHistoryEvent('resize')),
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                displayColors: false,
                                xPadding: 10,
                                yPadding: 10,
                                titleMarginBottom: 10
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: {
                                    color: "#cccccc"
                                }
                            },
                            x: {
                                type: "time",
                                min: this._startOfPeriod,
                                max: this._endOfPeriod,
                                time: {
                                    tooltipFormat: 'MMM D, YYYY, HH:mm:ss',
                                    displayFormats: {
                                        millisecond: 'HH:mm:ss.SSS',
                                        second: 'HH:mm:ss',
                                        minute: "HH:mm",
                                        hour: "HH:mm",
                                        week: "w"
                                    },
                                    unit: this._timeUnits,
                                    stepSize: this._stepSize
                                },
                                ticks: {
                                    color: "#000",
                                    font: {
                                        family: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                                        size: 9,
                                        style: "normal"
                                    }
                                },
                                gridLines: {
                                    color: "#cccccc"
                                }
                            }
                        }
                    }
                };
                this._chart = new Chart(this._chartElem.getContext("2d"), options);
            }
            else {
                if (changedProperties.has("_data")) {
                    this._chart.options.scales.x.min = this._startOfPeriod;
                    this._chart.options.scales.x.max = this._endOfPeriod;
                    this._chart.options.scales.x.time.unit = this._timeUnits;
                    this._chart.options.scales.x.time.stepSize = this._stepSize;
                    this._chart.data.datasets[0].data = data;
                    this._chart.update();
                }
            }
        }
        else {
            // Updating the table template before render, if necessary, to prevent additional update
            if (!this._tableTemplate || changedProperties.has("_data")) {
                this._tableTemplate = this._getTableTemplate();
            }
        }
        this.onCompleted().then(() => {
            this.dispatchEvent(new OrAttributeHistoryEvent('rendered'));
        });
    }
    onCompleted() {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.updateComplete;
        });
    }
    _cleanup() {
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
    _getTableTemplate() {
        const assetType = this.assetType;
        const attributeName = this.attribute ? this.attribute.name : this.attributeRef ? this.attributeRef.name : undefined;
        const attributeType = this.attribute ? this.attribute.type : undefined;
        if (!this._data || !assetType || !attributeName || !attributeType) {
            return html ``;
        }
        let config = {
            autoColumns: true
        };
        if (this.config && this.config.table) {
            if (this.config.table.assetTypes
                && this.config.table.assetTypes[assetType]
                && ((this.config.table.assetTypes[assetType].attributeNames && this.config.table.assetTypes[assetType].attributeNames[attributeName])
                    || (attributeType && this.config.table.assetTypes[assetType].attributeValueTypes && this.config.table.assetTypes[assetType].attributeValueTypes[attributeType]))) {
                config = this.config.table.assetTypes[assetType].attributeNames && this.config.table.assetTypes[assetType].attributeNames[attributeName] || this.config.table.assetTypes[assetType].attributeValueTypes[attributeType];
            }
            else if (this.config.table.attributeNames && this.config.table.attributeNames[attributeName]) {
                config = this.config.table.attributeNames[attributeName];
            }
            else if (attributeType && this.config.table.attributeValueTypes && this.config.table.attributeValueTypes[attributeType]) {
                config = this.config.table.attributeValueTypes[attributeType];
            }
            else if (this.config.table.default) {
                config = this.config.table.default;
            }
            if (!config) {
                config = {
                    autoColumns: true
                };
            }
        }
        if (config.autoColumns) {
            config = Object.assign({}, config);
            config.columns = [];
            const dp = this._data.find((dp) => dp.y !== undefined);
            if (dp) {
                if (typeof (dp.y) === "object" && !Array.isArray(dp.y)) {
                    config.columns = Object.entries(dp.y).map(([prop, value]) => {
                        return {
                            type: "prop",
                            header: prop,
                            path: "$.['" + prop + "']",
                            stringify: typeof (value) === "object",
                            numeric: !isNaN(Number(value))
                        };
                    });
                }
                else {
                    config.columns.push({
                        type: "prop",
                        header: "value",
                        stringify: typeof (dp.y) === "object",
                        numeric: typeof (dp.y) === "number"
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
            return html `
            <div id="table" class="mdc-data-table">
                <table style="${config.styles ? styleMap(config.styles) : ""}" class="mdc-data-table__table" aria-label="${attributeName + " history"}">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            ${config.columns.map((c) => html `<th style="${c.headerStyles ? styleMap(c.headerStyles) : ""}" class="mdc-data-table__header-cell ${c.numeric ? "mdc-data-table__header-cell--numeric" : ""}" role="columnheader" scope="col"><or-translate value="${c.header}"></or-translate></th>`)}
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this._data.map((dp) => {
                return html `
                                <tr class="mdc-data-table__row">
                                    ${config.columns.map((c) => {
                    const value = this._getCellValue(dp, c, config.timestampFormat);
                    return html `<td style="${c.styles ? styleMap(c.styles) : ""}" class="mdc-data-table__cell ${c.numeric ? "mdc-data-table__cell--numeric" : ""}" title="${value}">${value}</td>`;
                })}
                                </tr>
                            `;
            })}
                    </tbody>
                </table>
            </div>
            `;
        }
        console.warn("OrAttributeHistory: No columns configured so nothing to show");
        return html ``;
    }
    _getCellValue(datapoint, config, timestampFormat) {
        switch (config.type) {
            case "timestamp":
                const value = moment(datapoint.x).format(timestampFormat || OrAttributeHistory_1.DEFAULT_TIMESTAMP_FORMAT);
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
    _getIntervalOptions() {
        return [
            "HOUR" /* DatapointInterval.HOUR */,
            "DAY" /* DatapointInterval.DAY */,
            "WEEK" /* DatapointInterval.WEEK */,
            "MONTH" /* DatapointInterval.MONTH */,
            "YEAR" /* DatapointInterval.YEAR */
        ].map((interval) => {
            return [interval, i18next.t(interval.toLowerCase())];
        });
    }
    _getPeriodOptions() {
        return [
            "HOUR" /* DatapointInterval.HOUR */.toLowerCase(),
            "DAY" /* DatapointInterval.DAY */.toLowerCase(),
            "WEEK" /* DatapointInterval.WEEK */.toLowerCase(),
            "MONTH" /* DatapointInterval.MONTH */.toLowerCase(),
            "YEAR" /* DatapointInterval.YEAR */.toLowerCase()
        ];
    }
    _loadData() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this._loading || this._data || !this.assetType || !this.assetId || (!this.attribute && !this.attributeRef) || !this.period || !this.toTimestamp) {
                return;
            }
            this._loading = true;
            const assetId = this.assetId;
            const attributeName = this.attribute ? this.attribute.name : this.attributeRef.name;
            if (!this._type) {
                let attr = this.attribute;
                if (!attr) {
                    const response = yield manager.rest.api.AssetResource.queryAssets({
                        ids: [assetId],
                        select: {
                            attributes: [
                                attributeName
                            ]
                        }
                    });
                    if (response.status === 200 && response.data.length > 0) {
                        attr = response.data[0].attributes ? response.data[0].attributes[attributeName] : undefined;
                    }
                }
                if (attr) {
                    this._type = AssetModelUtil.getValueDescriptor(attr.type) || {};
                }
            }
            if (!this._type || !this._type.name) {
                this._loading = false;
                return;
            }
            let interval = "HOUR" /* DatapointInterval.HOUR */;
            let stepSize = 1;
            switch (this.period) {
                case "hour":
                    interval = "MINUTE" /* DatapointInterval.MINUTE */;
                    stepSize = 5;
                    break;
                case "day":
                    interval = "HOUR" /* DatapointInterval.HOUR */;
                    stepSize = 1;
                    break;
                case "week":
                    interval = "HOUR" /* DatapointInterval.HOUR */;
                    stepSize = 6;
                    break;
                case "month":
                    interval = "DAY" /* DatapointInterval.DAY */;
                    stepSize = 1;
                    break;
                case "year":
                    interval = "MONTH" /* DatapointInterval.MONTH */;
                    stepSize = 1;
                    break;
            }
            const lowerCaseInterval = interval.toLowerCase();
            this._startOfPeriod = moment(this.toTimestamp).subtract(1, this.period).startOf(lowerCaseInterval).add(1, lowerCaseInterval).toDate().getTime();
            this._endOfPeriod = moment(this.toTimestamp).startOf(lowerCaseInterval).add(1, lowerCaseInterval).toDate().getTime();
            this._timeUnits = lowerCaseInterval;
            this._stepSize = stepSize;
            const isChart = this._type && (this._type.jsonType === "number" || this._type.jsonType === "boolean");
            let response;
            if (isChart) {
                response = yield manager.rest.api.AssetDatapointResource.getDatapoints(assetId, attributeName, {
                    type: "lttb",
                    fromTimestamp: this._startOfPeriod,
                    toTimestamp: this._endOfPeriod,
                    amountOfPoints: 100
                });
            }
            else {
                response = yield manager.rest.api.AssetDatapointResource.getDatapoints(assetId, attributeName, {
                    type: "all",
                    fromTimestamp: this._startOfPeriod,
                    toTimestamp: this._endOfPeriod
                });
            }
            this._loading = false;
            if (response.status === 200) {
                this._data = response.data.filter(value => value.y !== null && value.y !== undefined);
                this._dataFirstLoaded = true;
            }
        });
    }
    _updateTimestamp(timestamp, forward, timeout = 300) {
        if (this._updateTimestampTimer) {
            window.clearTimeout(this._updateTimestampTimer);
            this._updateTimestampTimer = undefined;
        }
        this._updateTimestampTimer = window.setTimeout(() => {
            const newMoment = moment(timestamp);
            if (forward !== undefined) {
                newMoment.add(forward ? 1 : -1, this.period);
            }
            this.toTimestamp = newMoment.toDate();
        }, timeout);
    }
};
OrAttributeHistory.DEFAULT_TIMESTAMP_FORMAT = "L HH:mm:ss";
__decorate([
    property({ type: String })
], OrAttributeHistory.prototype, "assetType", void 0);
__decorate([
    property({ type: String })
], OrAttributeHistory.prototype, "assetId", void 0);
__decorate([
    property({ type: Object })
], OrAttributeHistory.prototype, "attribute", void 0);
__decorate([
    property({ type: Object })
], OrAttributeHistory.prototype, "attributeRef", void 0);
__decorate([
    property({ type: String })
], OrAttributeHistory.prototype, "period", void 0);
__decorate([
    property({ type: Number })
], OrAttributeHistory.prototype, "toTimestamp", void 0);
__decorate([
    property({ type: Object })
], OrAttributeHistory.prototype, "config", void 0);
__decorate([
    property()
], OrAttributeHistory.prototype, "_loading", void 0);
__decorate([
    property()
], OrAttributeHistory.prototype, "_data", void 0);
__decorate([
    property()
], OrAttributeHistory.prototype, "_tableTemplate", void 0);
__decorate([
    query("#chart")
], OrAttributeHistory.prototype, "_chartElem", void 0);
__decorate([
    query("#table")
], OrAttributeHistory.prototype, "_tableElem", void 0);
OrAttributeHistory = OrAttributeHistory_1 = __decorate([
    customElement("or-attribute-history")
], OrAttributeHistory);
export { OrAttributeHistory };
//# sourceMappingURL=index.js.map