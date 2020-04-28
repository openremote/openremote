import {css, customElement, html, LitElement, property, PropertyValues, query, unsafeCSS} from "lit-element";

import i18next from "i18next";
import {Asset, DatapointInterval, MetaItemType, ValueDatapoint} from "@openremote/model";
import {manager, DefaultColor4, Util} from "@openremote/core";
import Chart, {ChartTooltipCallback} from "chart.js";
import {getContentWithMenuTemplate} from "@openremote/or-chart";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {OrAttributeHistoryEvent} from "@openremote/or-attribute-history";
import {getMetaValue} from "@openremote/core/dist/util";
import moment from "moment";

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
    }
    
    .panel-content-wrapper {
        padding: var(--internal-or-asset-viewer-panel-padding);
    }
    
    .panel-content {
        display: flex;
        flex-wrap: wrap;
    }
        
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 25px;
        flex: 0 0 auto;
    }
`;

@customElement("or-attribute-card")
export class OrAttributeCard extends LitElement {

    @property()
    public assetId: string = "";

    @property()
    public attributeName: string = "";

    @property()
    private data: ValueDatapoint<any>[] = [];

    @property()
    private mainValue?: number;
    @property()
    private mainValueLastPeriod?: number;

    private period: moment.unitOfTime.Base = "month";
    private fromTimestamp?: Date = new Date();

    private asset: Asset = {};
    private formattedMainValue?: string;
    private delta?: number;

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
        this.getData();
    }

    updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        if (!this.data) {
            return;
        }

        this._chart = new Chart(this._chartElem, {
            type: "line",
            data: {
                datasets: [
                    {
                        data: this.data,
                        spanGaps: true,
                        backgroundColor: "transparent",
                        borderColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-line-color"),
                        pointBorderColor: "transparent",
                    }
                ]
            },
            options: {
                onResize: () => this.dispatchEvent(new OrAttributeHistoryEvent("resize")),
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
                        display: false
                    }],
                    xAxes: [{
                        type: "time",
                        display: false,
                    }]
                }
            }
        });

        if (changedProperties.has("data")) {
            this._chart.data.datasets![0].data = this.data;
            this._chart.update();
        }

        if (changedProperties.has("mainValue") || changedProperties.has("mainValueLastPeriod")) {
            if (this.mainValueLastPeriod && this.mainValue) {
                this.delta = (this.mainValueLastPeriod! - this.mainValue!);
            }
        }
        if (changedProperties.has("mainValue")) {
            this.formattedMainValue = this.getFormattedValue(this.mainValue!);
        }

    }

    protected render() {

        if (this.assetId === "" || this.attributeName === "") {
            return html`
                <div class="panel">
                    <div class="panel-content-wrapper">
                        <div class="panel-title">
                            <or-translate value="error"></or-translate>
                        </div>
                        <div class="panel-content">
                            <or-translate value="attributeNotFound"></or-translate>
                        </div>
                    </div>
                </div>
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        ${this.asset.name} - ${i18next.t(this.attributeName)}
                    </div>
                    <div class="panel-content">
                        <canvas id="chart"></canvas>
                        <span>total: ${this.formattedMainValue}</span>
                        <span>delta: ${this.delta}</span>
                        ${getContentWithMenuTemplate(
                            html`<or-input .type="${InputType.BUTTON}" .label="${i18next.t(this.period ? this.period : "-")}"></or-input>`,
                            this._getPeriodOptions(),
                            this.period,
                            (value) => this._setPeriodOption(value))}
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

        if (response.status !== 200 || !response.data) {
            return {};
        }

        return response.data[0];
    }

    protected async getDatapointsByAttribute(id: string, startOfPeriod: number, endOfPeriod: number): Promise<ValueDatapoint<any>[]> {

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            id,
            this.attributeName,
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

    protected getData = () => {
        this.getAssetById(this.assetId)
            .then((data: Asset) => {
                this.asset = data;

                const thisMoment = moment(this.fromTimestamp);
                const currentPeriod = {
                    start: thisMoment.startOf(this.period).toDate().getTime(),
                    end: thisMoment.endOf(this.period).toDate().getTime()
                };
                const lastPeriod = {
                    start: thisMoment.clone().subtract(1, this.period).startOf(this.period).toDate().getTime(),
                    end: thisMoment.clone().subtract(1, this.period).endOf(this.period).toDate().getTime()
                };

                this.getDatapointsByAttribute(data.id!, lastPeriod.start, lastPeriod.end)
                    .then((lastPeriodDatapoints: ValueDatapoint<any>[]) => {
                        const datapoints = lastPeriodDatapoints || [];
                        this.mainValueLastPeriod = this.getHighestValue(this.sanitizeDataPoints(datapoints));
                    });

                return this.getDatapointsByAttribute(data.id!, currentPeriod.start, currentPeriod.end);
            })
            .then((datapoints: ValueDatapoint<any>[]) => {
                this.data = datapoints || [];
                this.mainValue = this.getHighestValue(this.sanitizeDataPoints(this.data));
            });
    }

    protected sanitizeDataPoints(data: ValueDatapoint<any>[]): ValueDatapoint<any>[] {

        // if there's no measurement for the first data point in time, assume 0
        if (data[0] && !data[0].y) {
            data[0].y = 0;
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

    protected getFormattedValue(value: number): string {
        const format = getMetaValue(MetaItemType.FORMAT, this.asset.attributes![this.attributeName], undefined);
        return i18next.t(format, { postProcess: "sprintf", sprintf: [value] }).trim();
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

    protected _setPeriodOption(value: any) {
        this.period = value;

        this.getData();
        this.requestUpdate();
    }

}
