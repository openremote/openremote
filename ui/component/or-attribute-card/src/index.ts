import {css, customElement, html, LitElement, property, PropertyValues, query, unsafeCSS} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import i18next from "i18next";
import {Asset, AssetAttribute, Attribute, DatapointInterval, MetaItemType, ValueDatapoint} from "@openremote/model";
import {manager, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import Chart, {ChartTooltipCallback} from "chart.js";
import {getContentWithMenuTemplate, OrChartConfig} from "@openremote/or-chart";
import {InputType} from "@openremote/or-input";
import {getMetaValue} from "@openremote/core/dist/util";
import moment from "moment";
import {MDCDialog} from "@material/dialog";

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

const dialogStyle = require("!!raw-loader!@material/dialog/dist/mdc.dialog.css");

// language=CSS
const style = css`
    
    :host {
        width: 100%;
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
        flex-direction: row;
        align-items: center;
        justify-content: center;
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
    
    .period-label {
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
        width: 65%;
        height: 75%;
    }
    
    .delta-wrapper {
        flex: 0 0 75px;
        text-align: right;
        
        /*position: absolute;*/
        /*right: var(--internal-or-asset-viewer-panel-padding);*/
    }
    
    .date-range-wrapper {
        flex: 1;
        display: flex;
        justify-content: space-between;
    }
    
    .period-selector-wrapper {
        flex: 0 0 75px;
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
        color: #4D9D2A;
    }
    
    .mdc-dialog .mdc-dialog__surface {
        min-width: 600px;
        height: calc(100vh - 50%);
    }

    .dialog-container {
        display: flex;
        flex-direction: row;
        flex: 1 1 0;
    }

    .dialog-container > * {
        flex: 1 1 0;
    }
    
    .dialog-container > or-input{
        background-color: var(--or-app-color2);
        border-left: 3px solid var(--or-app-color4);
    }
        @media screen and (max-width: 769px) {
        .mdc-dialog .mdc-dialog__surface {
            min-width: auto;

            max-width: calc(100vw - 32px);
            max-height: calc(100% - 32px);
        }
    }
`;

@customElement("or-attribute-card")
export class OrAttributeCard extends LitElement {

    @property()
    public assetId: string | undefined;

    @property()
    public attributeName: string | undefined;

    @property({type: Object})
    private assetAttributes: AssetAttribute[] = [];

    @property()
    private data: ValueDatapoint<any>[] = [];
    @property()
    private graphColour: "#4D9D2A" | "#FF0000" = "#4D9D2A";

    @property()
    private mainValue?: number;
    @property()
    private mainValueLastPeriod?: number;
    @property()
    private delta: {val?: number, unit?: string} = {};
    @property()
    private deltaPlus: string = "";

    @property({type: Array})
    private colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    private period: moment.unitOfTime.Base = "month";
    private now?: Date = new Date();
    private currentPeriod?: { start: number; end: number };

    private asset: Asset = {};
    private formattedMainValue: {value: number|undefined, unit: string, formattedValue: string} = {
        value: undefined,
        unit: "",
        formattedValue: ""
    };

    @query("#chart")
    private _chartElem!: HTMLCanvasElement;
    private _chart?: Chart;

    static get styles() {
        return [
            css`${unsafeCSS(dialogStyle)}`,
            style
        ];
    }

    connectedCallback() {
        super.connectedCallback();

        if (!this.assetId || !this.attributeName) { return false; }

        this.getData();
    }

    updated(changedProperties: PropertyValues) {

        super.updated(changedProperties);
        console.log(changedProperties);

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
                            borderColor: this.graphColour,
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
                this._chart.data.datasets![0].borderColor = this.graphColour;
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

        if(changedProperties.has("assetAttributes") ) {
            this.assetAttributes.forEach((attr, index) => {
                if(this._getAttrColor(attr)) return;
                this._setAttrColor(attr)
            });
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

    protected _openDialog() {
        const component = this.shadowRoot!.getElementById("mdc-dialog");
        if (component) {
            const dialog = new MDCDialog(component);
            if (dialog) {
                dialog.open();
            }
        }
    }

    protected _getAttributeOptions() {
        if (!this.asset || !this.asset.attributes) {
            return;
        }

        if (this.shadowRoot && this.shadowRoot.getElementById("chart-attribute-picker")) {
            const elm = this.shadowRoot.getElementById("chart-attribute-picker") as HTMLInputElement;
            elm.value = "";
        }

        let attributes = [...Util.getAssetAttributes(this.asset)];
        if (attributes && attributes.length > 0) {
            attributes = attributes
                .filter((attr: AssetAttribute) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!))
                .filter((attr: AssetAttribute) => (this.assetAttributes && !this.assetAttributes.some((assetAttr: AssetAttribute) => (assetAttr.name === attr.name) && (assetAttr.assetId === attr.assetId))));
            const options = attributes.map((attr: AssetAttribute) => [attr.name, Util.getAttributeLabel(attr, undefined)]);
            return options;
        }
    }

    private _setAttribute(e:Event) {
        if (this.shadowRoot && this.shadowRoot.getElementById("chart-attribute-picker")) {
            const elm = this.shadowRoot.getElementById("chart-attribute-picker") as HTMLInputElement;
            if (this.asset) {
                const attr = Util.getAssetAttribute(this.asset, elm.value);
                if (attr) {
                    this.assetId = this.asset.id;
                    this.attributeName = attr.name;

                    this.getData();
                    this.requestUpdate();
                }
            }
        }
    }

    private _saveSettings() {
        // const viewSelector = this.activeAssetId ? this.activeAssetId : window.location.hash;
        // const assets: Asset[] = this.assets.filter(asset => 'id' in asset && typeof asset.id === "string");
        // const assetIds = assets.map(asset => asset.id);
        // const attributes = this.assetAttributes.map(attr => attr.name);
        // const configStr = window.localStorage.getItem('OrChartConfig')
        // let config:OrChartConfig;
        // if(configStr) {
        //     config = JSON.parse(configStr);
        // } else {
        //     config = {
        //         views: {
        //             [viewSelector]: {}
        //         }
        //     }
        // }
        //
        // config.views[viewSelector] = {
        //     assetIds: assetIds,
        //     attributes: attributes,
        //     period: this.period
        // };
        // window.localStorage.setItem('OrChartConfig', JSON.stringify(config))
    }

    protected _cleanup() {
        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
    }

    protected render() {

        const dialogHTML = html`
            <div id="mdc-dialog"
                class="mdc-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="my-dialog-title"
                aria-describedby="my-dialog-content">
                <div class="mdc-dialog__container">
                    <div class="mdc-dialog__surface">
                    <h2 class="mdc-dialog__title" id="my-dialog-title">${i18next.t("addAttribute")}</h2>
                    <div class="dialog-container mdc-dialog__content" id="my-dialog-content">
                        <or-asset-tree id="chart-asset-tree" .selectedIds="${this.asset ? [this.asset.id] : null}]"></or-asset-tree>
                            ${this.asset && this.asset.attributes ? html`
                                <or-input id="chart-attribute-picker" 
                                        style="display:flex;"
                                        .label="${i18next.t("attribute")}" 
                                        .type="${InputType.LIST}"
                                        .options="${this._getAttributeOptions()}"></or-input>
                            ` : ``}
                    </div>
                    <footer class="mdc-dialog__actions">
                        <or-input class="button" 
                                slot="secondaryAction"
                                .type="${InputType.BUTTON}" 
                                label="${i18next.t("cancel")}" 
                                class="mdc-button mdc-dialog__button" 
                                data-mdc-dialog-action="no"></or-input>

                        <or-input class="button" 
                            slot="primaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("add")}" 
                            class="mdc-button mdc-dialog__button" 
                            data-mdc-dialog-action="yes"
                            @click="${this._setAttribute}"></or-input>

                    </footer>
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;

        if (!this.assetId || !this.attributeName) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content" @click="${() => this._openDialog()}">
                            <or-icon icon="plus"></or-icon>
                            <span>${i18next.t("addAttribute")}</span>
                        </div>
                    </div>
                </div>
                ${dialogHTML}
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <span class="panel-title-text">${this.asset.name} - ${i18next.t(this.attributeName)}</span>
                        <or-icon icon="plus-minus" @click="${() => this._openDialog()}" />
                    </div>
                    <div class="panel-content">
                        <div class="top-row">
                            <span class="main-number">${this.formattedMainValue!.value}</span>
                            <span class="main-number-unit">${this.formattedMainValue!.unit}</span>
                        </div>
                        <div class="center-row">
                            <div class="chart-wrapper">
                                <canvas id="chart"></canvas>
                            </div>
                            <div class="delta-wrapper">
                                <span class=${classMap({"delta": true, "delta-min": this.delta.val! < 0, "delta-plus": this.delta.val! > 0})}>${this.deltaPlus}${this.delta.val}${this.delta.unit}</span>
                            </div>
                        </div>
                        <div class="bottom-row">
                            <div class="date-range-wrapper">
                                <span class="period-label">${Intl.DateTimeFormat(manager.language).format(this.currentPeriod!.start)}</span>
                                <span class="period-label">${Intl.DateTimeFormat(manager.language).format(this.currentPeriod!.end)}</span>
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
            ${dialogHTML}
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

        this.currentPeriod = {
            start: thisMoment.startOf(this.period).toDate().getTime(),
            end: thisMoment.endOf(this.period).toDate().getTime()
        };
        const lastPeriod = {
            start: thisMoment.clone().subtract(1, this.period).startOf(this.period).toDate().getTime(),
            end: thisMoment.clone().subtract(1, this.period).endOf(this.period).toDate().getTime()
        };

        const p1 = this.getDatapointsByAttribute(this.assetId!, this.currentPeriod.start, this.currentPeriod.end)
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
                this.graphColour = (this.delta.val! < 0) ? "#FF0000" : "#4D9D2A";
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

    protected _setAttrColor(attr: Attribute) {
        const usedColors = this.assetAttributes.map(attr => this._getAttrColor(attr));
        const color = this.colors.filter(color => usedColors.indexOf(color) < 0)[0];
        const meta = {name: "color", value: color};
        if (attr.meta) {
            attr.meta.push(meta);
        }
    }

    protected _getAttrColor(attr: Attribute) {
        return  Util.getMetaValue("color", attr, undefined);
    }

}
