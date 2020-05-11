import {css, customElement, html, LitElement, property, PropertyValues, query, unsafeCSS} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import i18next from "i18next";
import {Asset, DatapointInterval, MetaItemType, ValueDatapoint} from "@openremote/model";
import {manager, DefaultColor4, DefaultColor5} from "@openremote/core";
import Chart, {ChartTooltipCallback} from "chart.js";
import {getContentWithMenuTemplate} from "@openremote/or-chart";
import {InputType} from "@openremote/or-input";
import {getMetaValue} from "@openremote/core/dist/util";
import moment from "moment";

export class OrAttributeCardEvent extends CustomEvent<OrAttributeCardEventDetail> {

    public static readonly NAME = "or-attribute-history-event";

    constructor(value?: any, previousValue?: any) {
        super(OrAttributeCardEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface OrAttributeCardEventDetail {
    value?: any;
    previousValue?: any;
}

declare global {
    export interface HTMLElementEventMap {
        [OrAttributeCardEvent.NAME]: OrAttributeCardEvent;
    }
}

// language=CSS
const style = css`
    
    :host {
        width: 100%;
        
        --internal-or-attribute-history-graph-line-color: var(--or-attribute-history-graph-line-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
    }
    
    :host([hidden]) {
        display: none;
    }
    
    .panel {
        background-color: var(--internal-or-asset-viewer-panel-color);     
        border: 1px solid #e5e5e5;
        border-radius: 5px;
        max-width: 100%;
        position: relative;
        padding: var(--internal-or-asset-viewer-panel-padding);
    }
    .panel.panel-empty {
        display: flex;
    }
    .panel.panel-empty .panel-content-wrapper {
        align-items: center;
        width: 100%;
    }
    .panel.panel-empty .panel-content {
        align-items: center;
    }
    
    .panel-content-wrapper {
        height: 200px;
        display: flex;
        flex-direction: column;
    }
        
    .panel-title {
        display: flex;
        align-items: center;
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        flex: 0 0 auto;
    }
    
    .panel-title-text {
        flex: 1;
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
    }
    
    .panel-content {
        display: flex;
        flex-direction: column;
        width: 100%;
        flex: 1;
    }
    
    .top-row {
        width: 100%;
        display: flex;
        flex: 0 0 60px;
        align-items: center;
        justify-content: center;
    }
    
    .center-row {
        width: 100%;
        display: flex;
        flex: 1;
        align-items: center;
    }
    
    .bottom-row {
        width: 100%;
        display: flex;
        flex: 0 0 24px;
        align-items: center;
    }
    
    .now {
        color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
    }
    
    .main-number {
        font-size: 24px;
    }
    
    .main-number-unit {
        font-size: 24px;
        color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
    }
    
    .chart-wrapper {
        flex: 1;
    }
    
    .delta-wrapper {
        flex: 0 0 80px;
        text-align: right;
    }
    
    .date-range-wrapper {
        flex: 1;
        display: flex;
        justify-content: space-between;
    }
    
    .period-selector-wrapper {
        flex: 0 0 80px;
    }
    .period-selector {
        position: absolute;
        right: 15px;
        bottom: 17px;
    }
    
    .delta {
        font-weight: bold;
    }
    .delta.delta-min {
        color: red;
    }
    .delta.delta-plus {     
       color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
    }
    
`;

@customElement("or-attribute-card")
export class OrAttributeCard extends LitElement {

    @property()
    public assetId: string | undefined;

    @property()
    public attributeName: string | undefined;

    @property()
    private data: ValueDatapoint<any>[] = [];

    @property()
    private mainValue?: number;
    @property()
    private mainValueLastPeriod?: number;
    @property()
    private delta: {val?: number, unit?: string} = {};
    @property()
    private deltaPlus: string = "";

    private period: moment.unitOfTime.Base = "month";
    private now?: Date = new Date();

    private asset: Asset = {};
    private formattedMainValue: {value: number|undefined, unit: string, formattedValue: string} = {
        value: undefined,
        unit: "",
        formattedValue: ""
    };

    @query("#chart")
    private _chartElem!: HTMLCanvasElement;
    private _chart?: Chart;
    private _style!: CSSStyleDeclaration;

    static get styles() {
        return [
            style
        ];
    }

    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);

        if (!this.assetId || !this.attributeName) { return false; };

        this.getData();
    }

    updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        if (!this.data) {
            return;
        }

        if (!this._chart) {
            this._chart = new Chart(this._chartElem, {
                type: "line",
                data: {
                    datasets: [
                        {
                            data: this.data,
                            lineTension: 0.1,
                            spanGaps: true,
                            backgroundColor: "transparent",
                            borderColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-line-color"),
                            pointBorderColor: "transparent",
                        }
                    ]
                },
                options: {
                    onResize: () => this.dispatchEvent(new OrAttributeCardEvent("resize")),
                    responsive: true,
                    maintainAspectRatio: false,
                    legend: {
                        display: false
                    },
                    tooltips: {
                        displayColors: false,
                        callbacks: {
                            title: (tooltipItems, data) => {
                                return "";
                            },
                            label: (tooltipItem, data) => {
                                return tooltipItem.yLabel; // Removes the colon before the label
                            },
                            footer: () => {
                                return ""; // Hack the broken vertical alignment of body with footerFontSize: 0
                            }
                        } as ChartTooltipCallback
                    },
                    scales: {
                        yAxes: [{
                            display: false
                        }],
                        xAxes: [{
                            type: "time",
                            display: false,
                        }]
                    }
                }
            });
        } else {
            if (changedProperties.has("data")) {
                this._chart.data.datasets![0].data = this.data;
                this._chart.update();
            }
        }

        if (changedProperties.has("mainValue") || changedProperties.has("mainValueLastPeriod")) {
            if (this.mainValueLastPeriod && this.mainValue) {
                this.delta = this.getFormattedDelta(this.mainValue, this.mainValueLastPeriod);
                this.deltaPlus = (this.delta.val! > 0) ? "+" : "";
            }
        }
        if (changedProperties.has("mainValue")) {
            this.formattedMainValue = this.getFormattedValue(this.mainValue!);
        }

        this.onCompleted().then(() => {
            this.dispatchEvent(new OrAttributeCardEvent("rendered"));
        });

    }

    async onCompleted() {
        await this.updateComplete;
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
    }

    protected _cleanup() {
        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
    }

    protected render() {

        if (!this.assetId || !this.attributeName) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <or-icon icon="plus"></or-icon>
                            <span>${i18next.t("addAttribute")}</span>
                        </div>
                    </div>
                </div>
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <span class="panel-title-text">${this.asset.name} - ${i18next.t(this.attributeName)}</span>
                        <or-icon icon="plus-minus"/>
                    </div>
                    <div class="panel-content">
                        <div class="top-row">
                            <span class="main-number">${this.formattedMainValue!.value}</span>
                            <span class="main-number-unit">${this.formattedMainValue!.unit}</span>
                        </div>
                        <div class="center-row">
                            <div class="chart-wrapper" style="width: 80%;height: 50%;">
                                <canvas id="chart"></canvas>
                            </div>
                            <div class="delta-wrapper">
                                <span class=${classMap({"delta": true, "delta-min": this.delta.val! < 0, "delta-plus": this.delta.val! > 0})}>${this.deltaPlus}${this.delta.val}${this.delta.unit}</span>
                            </div>
                        </div>
                        <div class="bottom-row">
                            <div class="date-range-wrapper">
                                <span class="now">${Intl.DateTimeFormat(manager.language).format(this.now)}</span>
                                <span class="now">${Intl.DateTimeFormat(manager.language).format(this.now)}</span>
                            </div>
                            <div class="period-selector-wrapper">
                                ${getContentWithMenuTemplate(
                                    html`<or-input class="period-selector" .type="${InputType.BUTTON}" .label="${i18next.t(this.period ? this.period : "-")}"></or-input>`,
                                    [{value: "hour", text: ""}, {value: "day", text: ""}, {value: "week", text: ""}, {value: "month", text: ""}, {value: "year", text: ""}]
                                        .map((option) => {
                                            option.text = i18next.t(option.value);
                                            return option;
                                        }),
                                    this.period,
                                    (value) => this._setPeriodOption(value))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    protected async getAssetById(id: string): Promise<Asset> {
        const response = await manager.rest.api.AssetResource.queryAssets({
            ids: [id],
            recursive: false
        });

        if (response.status !== 200 || !response.data || !response.data.length) {
            return {};
        }

        return response.data[0];
    }

    protected async getDatapointsByAttribute(id: string, startOfPeriod: number, endOfPeriod: number): Promise<ValueDatapoint<any>[]> {

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            id,
            this.attributeName!,
            {
                interval: this._getInterval(),
                fromTimestamp: startOfPeriod,
                toTimestamp: endOfPeriod
            }
        );

        if (response.status !== 200 || !response.data) {
            return [];
        }

        return response.data;
    }

    protected async getData() {
        const thisMoment = moment(this.now);

        this.asset = await this.getAssetById(this.assetId!);

        const currentPeriod = {
            start: thisMoment.startOf(this.period).toDate().getTime(),
            end: thisMoment.endOf(this.period).toDate().getTime()
        };
        const lastPeriod = {
            start: thisMoment.clone().subtract(1, this.period).startOf(this.period).toDate().getTime(),
            end: thisMoment.clone().subtract(1, this.period).endOf(this.period).toDate().getTime()
        };

        const p1 = this.getDatapointsByAttribute(this.assetId!, currentPeriod.start, currentPeriod.end)
            .then((datapoints: ValueDatapoint<any>[]) => {
                this.data = datapoints || [];
                this.mainValue = this.getHighestValue(this.sanitiseDataPoints(this.data));
                return this.mainValue;
            });

        const p2 = this.getDatapointsByAttribute(this.assetId!, lastPeriod.start, lastPeriod.end)
            .then((datapoints: ValueDatapoint<any>[]) => {
                this.mainValueLastPeriod = this.getHighestValue(this.sanitiseDataPoints(datapoints));
                return this.mainValueLastPeriod;
            });

        Promise.all([p1, p2])
            .then((returnvalues) => {
                this.delta = this.getFormattedDelta(returnvalues[0], returnvalues[1]);
            });

    }

    protected sanitiseDataPoints(data: ValueDatapoint<any>[]): ValueDatapoint<any>[] {

        // if there's no measurement for the first data point in time, assume 0
        if (data[0] && !data[0].y) {
            data[0].y = 0;
        }

        // if there's no measurement for the last data point, use the highest available
        if (data[(data.length - 1)] && !data[(data.length - 1)].y) {
            data[(data.length - 1)].y = this.getHighestValue(data);
        }

        return data;
    }

    protected getTotalValue(data: ValueDatapoint<any>[]): number {
        return data.reduce(( acc: number, val: ValueDatapoint<any> ) => {
            return val.y ? acc + Math.round(val.y) : acc;
        }, 0);
    }

    protected getHighestValue(data: ValueDatapoint<any>[]): number {
        return Math.max.apply(Math, data.map((e: ValueDatapoint<any>) => e.y || false ));
    }

    protected getFormattedValue(value: number): {value: number, unit: string, formattedValue: string} {
        const format = getMetaValue(MetaItemType.FORMAT, this.asset.attributes![this.attributeName!], undefined);
        const unit = format.split(" ").pop();
        return {
            value: value,
            unit: unit,
            formattedValue: i18next.t(format, { postProcess: "sprintf", sprintf: [value] }).trim()
        };
    }

    protected getFormattedDelta(currentPeriodVal: number, lastPeriodVal: number): {val?: number, unit?: string} {
        if (currentPeriodVal && lastPeriodVal) {
            if (lastPeriodVal === 0 && currentPeriodVal === 0) {
                return {val: 0, unit: "%"};
            } else if (lastPeriodVal === 0 && currentPeriodVal !== 0) {
                return {val: 100, unit: "%"};
            } else {
                const math = Math.round((currentPeriodVal - lastPeriodVal) / lastPeriodVal * 100);
                return {val: math, unit: "%"};
            }
        } else {
            return {};
        }
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

    protected _setPeriodOption(value: any) {
        this.period = value;

        this.getData();
        this.requestUpdate();
    }

}
