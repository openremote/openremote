import {css, customElement, html, LitElement, property, PropertyValues, query, unsafeCSS} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import i18next from "i18next";
import {Asset, AssetAttribute, Attribute, DatapointInterval, MetaItemType, ValueDatapoint} from "@openremote/model";
import {manager, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import Chart, {ChartTooltipCallback} from "chart.js";
import {getContentWithMenuTemplate, OrChartConfig} from "@openremote/or-chart";
import {InputType} from "@openremote/or-input";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {getMetaValue} from "@openremote/core/dist/util";
import moment from "moment";
import {OrAssetTreeRequestSelectEvent} from "@openremote/or-asset-tree";
import {DialogAction, OrMwcDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

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
        align-items: center;
        justify-content: center;
        flex-direction: column;
    }
    
    .panel-content-wrapper {
        height: 200px;
        display: flex;
        flex-direction: column;
    }
        
    .panel-title {
        display: flex;
        align-items: center;
        margin: -15px -15px 0 0; /* compensate for the click-space of the plusminus button */
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
`;
export class OrAttributeCardAddAttributeEvent extends CustomEvent<string> {

    public static readonly NAME = "or-attribute-card-add-attribute";

    constructor(value:string) {
        super(OrAttributeCardAddAttributeEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: value
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAttributeCardAddAttributeEvent.NAME]: OrAttributeCardAddAttributeEvent;
    }
}

@customElement("or-attribute-card")
export class OrAttributeCard extends LitElement {

    @property()
    public assetId: string | undefined;

    @property()
    public attributeName: string | undefined;

    @property()
    public panelName?: string;

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

    private error: boolean = false;

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

    constructor(){
        super();
        this.addEventListener(OrAttributeCardAddAttributeEvent.NAME, this._setAttribute)
    }

    connectedCallback() {
        super.connectedCallback();

        this.getData();
    }
    
    renderDialogHTML() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog") as OrMwcDialog;
        if(!this.shadowRoot) return

        if (dialog) {
            dialog.dialogContent =html`
                <or-asset-tree id="chart-asset-tree" 
                    .selectedIds="${this.asset ? [this.asset.id] : null}]"
                    @or-asset-tree-request-select="${(e: OrAssetTreeRequestSelectEvent) => {
                        this.asset = {...e.detail.detail.node.asset!}
                        this._getAttributeOptions();
                        let attributes = [...Util.getAssetAttributes(this.asset)];
                        this.assetAttributes = [...attributes];
                    }}">
                </or-asset-tree>
                ${this.asset && this.asset.attributes ? html`
                    <or-input id="attribute-picker" 
                        style="display:flex;"
                        .label="${i18next.t("attribute")}" 
                        .type="${InputType.LIST}"
                        .options="${this._getAttributeOptions()}"
                        .value="${this.attributeName}"></or-input>
                ` : ``}
            `;
            this.requestUpdate();
        }
    }

    updated(changedProperties: PropertyValues) {
        if (changedProperties.has("assetAttributes")) {
            this.renderDialogHTML();
        }

        if (changedProperties.has("assetId") && this.assetId && changedProperties.get("assetId") !== this.assetId) {
            this.getSettings();
        }


        if (!this.data || !this.data.length) {
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
            if (this.mainValueLastPeriod !== undefined && this.mainValue !== undefined) {
                this.delta = this.getFormattedDelta(this.mainValue, this.mainValueLastPeriod);
                this.deltaPlus = (this.delta.val && this.delta.val > 0) ? "+" : "";
            }
        }
        if (changedProperties.has("mainValue")) {
            this.formattedMainValue = this.getFormattedValue(this.mainValue!);
        }
      
    }

    async onCompleted() {
        await this.updateComplete;
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
    }

    protected _openDialog() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog") as OrMwcDialog;

        if (dialog) {
            dialog.dialogContent = html`
                <or-asset-tree id="chart-asset-tree" 
                    .selectedIds="${this.asset ? [this.asset.id] : null}]"
                    @or-asset-tree-request-select="${(e: OrAssetTreeRequestSelectEvent) => {
                        this.asset = {...e.detail.detail.node.asset!};
                        let attributes = [...Util.getAssetAttributes(this.asset)];
                        this.assetAttributes = [...attributes];
                        this._getAttributeOptions();
                    }}">
                </or-asset-tree>
                ${this.asset && this.asset.attributes ? html`
                    <or-input id="attribute-picker" 
                        style="display:flex;"
                        .label="${i18next.t("attribute")}" 
                        .type="${InputType.LIST}"
                        .options="${this._getAttributeOptions()}"
                        .value="${this.attributeName}"></or-input>
                ` : ``}
            `;
            dialog.open();
        }
    }

    protected _getAttributeOptions() {
        if (!this.asset || !this.assetAttributes) {
            return;
        }

        if (this.shadowRoot && this.shadowRoot.getElementById("attribute-picker")) {
            const elm = this.shadowRoot.getElementById("attribute-picker") as HTMLInputElement;
            elm.value = "";
        }

        let attributes = [...Util.getAssetAttributes(this.asset)];
        if (attributes && attributes.length > 0) {
            attributes = attributes
                .filter((attr: AssetAttribute) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!));
            const options = attributes.map((attr: AssetAttribute) => [attr.name, Util.getAttributeLabel(attr, undefined)]);
            return options;
        }
    }

    private _setAttribute(event:OrAttributeCardAddAttributeEvent) {
        if (this.asset && event) {
            const attr = Util.getAssetAttribute(this.asset , event.detail);
            if (attr) {
                this.assetId = this.asset.id;
                this.attributeName = attr.name;

                this.getData();
                this.saveSettings();
                this.requestUpdate();
            }
        }
    }

    protected _cleanup() {
        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
    }

    getSettings() {
        const configStr = window.localStorage.getItem('OrChartConfig')
        if(!configStr || !this.panelName) return

        const viewSelector = this.assetId ? this.assetId : window.location.hash;
        const config = JSON.parse(configStr);
        const view = config.views[viewSelector][this.panelName];
        if(!view) return
        const query = {
            ids: view.assetIds
        }
        if(view.assetIds === this.assetAttributes.map(attr => attr.assetId)) return 

        manager.rest.api.AssetResource.queryAssets(query).then((response) => {
            const assets = response.data;
            if(assets.length > 0) {
                this.assetAttributes = view.attributes.map((attr: string, index: number)  => Util.getAssetAttribute(assets[index], attr));
                if(this.assetAttributes && this.assetAttributes.length > 0) {
                    this.assetId = assets[0].id;
                    this.attributeName = this.assetAttributes[0].name;
                    this.getData();
                }
            }
        });

    }

    saveSettings() {
        const viewSelector = window.location.hash;
        const attributes = [this.attributeName];
        const assetIds = [this.assetId];
        const configStr = window.localStorage.getItem('OrChartConfig')
        if(!this.panelName) return

        let config:OrChartConfig;
        if(configStr) {
            config = JSON.parse(configStr);
        } else {
            config = {
                views: {
                    [viewSelector]: {
                        [this.panelName] : {

                        }
                    }
                }
            }
        }   

        config.views[viewSelector][this.panelName] = {
            assetIds: assetIds,
            attributes: attributes,
            period: this.period
        };
        const message = {
            provider: "STORAGE",
            action: "STORE",
            key: "OrChartConfig",
            value: JSON.stringify(config)

        }
        manager.console._doSendProviderMessage(message)
    }

    protected render() {

        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
            {
                actionName: "yes",
                default: true,
                content: html`<or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("add")}" data-mdc-dialog-action="yes"></or-input>`,
                action: () => {
                    const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog") as OrMwcDialog;
                    if (dialog.shadowRoot && dialog.shadowRoot.getElementById("attribute-picker")) {
                        const elm = dialog.shadowRoot.getElementById("attribute-picker") as HTMLInputElement;
                        this.dispatchEvent(new OrAttributeCardAddAttributeEvent(elm.value));
                    }
                }
            }
        ];

        if (!this.assetId || !this.attributeName) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-input>
                        </div>
                    </div>
                </div>
                <or-mwc-dialog id="mdc-dialog" dialogTitle="addAttribute" .dialogActions="${dialogActions}"></or-mwc-dialog>
            `;
        }

        if (this.error) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <span>${i18next.t("couldNotRetrieveAttribute")}</span>
                            <or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-input>
                        </div>
                    </div>
                </div>
                <or-mwc-dialog id="mdc-dialog" dialogTitle="addAttribute" .dialogActions="${dialogActions}"></or-mwc-dialog>
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <span class="panel-title-text">${this.asset.name} - ${i18next.t(this.attributeName)}</span>
                        <or-input icon="plus-minus" type="button" @click="${() => this._openDialog()}"></or-input>
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
            <or-mwc-dialog id="mdc-dialog" dialogTitle="addAttribute" .dialogActions="${dialogActions}"></or-mwc-dialog>
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

    protected async getDatapointsByAttribute(id: string, attributeName: string, startOfPeriod: number, endOfPeriod: number): Promise<ValueDatapoint<any>[]> {

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            id,
            attributeName,
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

        if (!this.assetId || !this.attributeName) {
            this.error = true;
            this.getSettings();
            return false;
        }

        const thisMoment = moment(this.now);

        this.asset = await this.getAssetById(this.assetId);
        console.log(this.asset);
        this.currentPeriod = {
            start: thisMoment.startOf(this.period).toDate().getTime(),
            end: thisMoment.endOf(this.period).toDate().getTime()
        };
        const lastPeriod = {
            start: thisMoment.clone().subtract(1, this.period).startOf(this.period).toDate().getTime(),
            end: thisMoment.clone().subtract(1, this.period).endOf(this.period).toDate().getTime()
        };

        const p1 = this.getDatapointsByAttribute(this.assetId, this.attributeName, this.currentPeriod.start, this.currentPeriod.end)
            .then((datapoints: ValueDatapoint<any>[]) => {
                this.data = datapoints || [];
                this.mainValue = this.getHighestValue(this.sanitiseDataPoints(this.data));
                return this.mainValue;
            });

        const p2 = this.getDatapointsByAttribute(this.assetId, this.attributeName, lastPeriod.start, lastPeriod.end)
            .then((datapoints: ValueDatapoint<any>[]) => {
                this.mainValueLastPeriod = this.getHighestValue(this.sanitiseDataPoints(datapoints));
                return this.mainValueLastPeriod;
            });

        Promise.all([p1, p2])
            .then((returnvalues) => {
                this.delta = this.getFormattedDelta(returnvalues[0], returnvalues[1]);
                this.graphColour = (this.delta.val! < 0) ? "#FF0000" : "#4D9D2A";
                this.error = false;
            })
            .catch((err) => {
                this.error = true;
                this.requestUpdate();
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
