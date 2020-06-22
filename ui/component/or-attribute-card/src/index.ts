import {css, customElement, html, LitElement, property, PropertyValues, query, unsafeCSS} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import i18next from "i18next";
import {Asset, AssetAttribute, Attribute, DatapointInterval, MetaItemType, ValueDatapoint} from "@openremote/model";
import {manager, DefaultColor3, DefaultColor4, Util, AssetModelUtil} from "@openremote/core";
import Chart, {ChartTooltipCallback} from "chart.js";
import {getContentWithMenuTemplate, OrChartConfig} from "@openremote/or-chart";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {getMetaValue} from "@openremote/core/dist/util";
import moment from "moment";
import {OrAssetTreeRequestSelectEvent} from "@openremote/or-asset-tree";
import {DialogAction, OrMwcDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

export type ContextMenuOptions = "editAttribute" | "editDelta" | "editCurrentValue";

const dialogStyle = require("!!raw-loader!@material/dialog/dist/mdc.dialog.css");

// language=CSS
const style = css`
    
    :host {
        --internal-or-attribute-history-graph-line-color: var(--or-attribute-history-graph-line-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        width: 100%;
    }
    
    :host([hidden]) {
        display: none;
    }
    
    .panel {
        position: relative;
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
    
    .mainvalue-wrapper {
        width: 100%;
        display: flex;
        flex: 0 0 60px;
        align-items: center;
        justify-content: center;
    }
    
    .graph-wrapper {
        width: 100%;
        display: flex;
        flex: 1;
        align-items: center;
    }
    
    .main-number {   
        color: var(--internal-or-asset-viewer-title-text-color);
        font-size: 42px;
    }
    
    .main-number-icon {   
        font-size: 24px;
        margin-right: 10px;
    }
    
    .main-number-unit {
        font-size: 42px;
        color: var(--internal-or-asset-viewer-title-text-color);
        font-weight: 200;
        margin-left: 5px;
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
    
    .period-selector {
        position: absolute;
        right: -16px;
        bottom: 0;
    }
    
    .delta {
        color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
        font-weight: bold;
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

    protected _style!: CSSStyleDeclaration;

    @property({type: Object})
    private assetAttributes: AssetAttribute[] = [];

    @property()
    private data: ValueDatapoint<any>[] = [];

    @property()
    private mainValue?: number;
    @property()
    private decimals: number = 2;
    @property()
    private delta: {val?: number, unit?: string} = {};
    @property()
    private deltaPlus: string = "";
    @property()
    private deltaFormat: string = "absolute";

    private error: boolean = false;

    private period: moment.unitOfTime.Base = "day";
    private now: Date = new Date();
    private currentPeriod?: { start: number; end: number };

    private asset: Asset = {};
    private formattedMainValue: {value: number|undefined, unit: string} = {
        value: undefined,
        unit: ""
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

        this._style = window.getComputedStyle(this);
        this.getData();
    }
    
    renderEditAttributeDialogHTML() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog-editattribute") as OrMwcDialog;
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
            this.renderEditAttributeDialogHTML();
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
                            borderColor: this._style.getPropertyValue("--internal-or-attribute-history-graph-line-color"),
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
                this._chart.update();
                this.delta = this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data));
                this.deltaPlus = (this.delta.val && this.delta.val > 0) ? "+" : "";
            }
        }

        if (changedProperties.has("mainValue") || changedProperties.has("decimals")) {
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

    protected _openEditAttributeDialog() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog-editattribute") as OrMwcDialog;

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

    protected _openEditCurrentValueDialog() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog-editcurrentvalue") as OrMwcDialog;

        if (dialog) {
            dialog.dialogContent = html`
                <or-input id="current-value-decimals" .label="${i18next.t("decimals")}" value="${this.decimals}" .type="${InputType.TEXT}"></or-input>
            `;
            dialog.open();
        }
    }

    protected _openEditDeltaDialog() {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog-editdelta") as OrMwcDialog;

        const options = [
            ["percentage", "Percentage"],
            ["absolute", "Absolute"]
        ];

        if (dialog) {
            dialog.dialogContent = html`
                <or-input id="delta-mode-picker" value="${this.deltaFormat}" @or-input-changed="${(evt: OrInputChangedEvent) => this.deltaFormat = evt.detail.value}" .type="${InputType.LIST}" .options="${options}"></or-input>
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
                    this.period = view.period;
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

        const dialogEditAttributeActions: DialogAction[] = [
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
                    const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog-editattribute") as OrMwcDialog;
                    if (dialog.shadowRoot && dialog.shadowRoot.getElementById("attribute-picker")) {
                        const elm = dialog.shadowRoot.getElementById("attribute-picker") as HTMLInputElement;
                        this.dispatchEvent(new OrAttributeCardAddAttributeEvent(elm.value));
                    }
                }
            }
        ];
        
        const dialogEditCurrentValueActions: DialogAction[] = [
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
                content: html`<or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("ok")}" data-mdc-dialog-action="yes"></or-input>`,
                action: () => {
                    const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog-editcurrentvalue") as OrMwcDialog;
                    if (dialog.shadowRoot && dialog.shadowRoot.getElementById("current-value-decimals")) {
                        const elm = dialog.shadowRoot.getElementById("current-value-decimals") as HTMLInputElement;
                        const input = parseInt(elm.value);
                        if (input < 0) {this.decimals = 0;}
                        else if (input > 10) {this.decimals = 10;}
                        else {this.decimals = input;}
                        this.formattedMainValue = this.getFormattedValue(this.mainValue!);
                    }
                }
            }
        ];

        const dialogEditDeltaActions: DialogAction[] = [
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
                content: html`<or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("ok")}" data-mdc-dialog-action="yes"></or-input>`,
                action: () => {
                    this.delta = this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data), this.deltaFormat);
                }
            }
        ];

        if (!this.assetId || !this.attributeName) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openEditAttributeDialog()}"></or-input>
                        </div>
                    </div>
                </div>
                <or-mwc-dialog id="mdc-dialog-editattribute" dialogTitle="addAttribute" .dialogActions="${dialogEditAttributeActions}"></or-mwc-dialog>
            `;
        }

        if (this.error) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <span>${i18next.t("couldNotRetrieveAttribute")}</span>
                            <or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openEditAttributeDialog()}"></or-input>
                        </div>
                    </div>
                </div>
                <or-mwc-dialog id="mdc-dialog-editattribute" dialogTitle="addAttribute" .dialogActions="${dialogEditAttributeActions}"></or-mwc-dialog>
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <span class="panel-title-text">${this.asset.name} - ${i18next.t(this.attributeName)}</span>
                        ${getContentWithMenuTemplate(html`
                            <or-input icon="dots-vertical" type="button"></or-input>
                        `, 
                        [
                            {
                                text: i18next.t("editAttribute"),
                                value: "editAttribute"
                            },
                            {
                                text: i18next.t("editDelta"),
                                value: "editDelta"
                            },
                            {
                                text: i18next.t("editCurrentValue"),
                                value: "editCurrentValue"
                            }
                        ],
                        undefined,
                        (values: string | string[]) => this.handleMenuSelect(values as ContextMenuOptions))}
                    </div>
                    <div class="panel-content">
                        <div class="mainvalue-wrapper">
                            <span class="main-number-icon">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.asset!.type!))}</span>
                            <span class="main-number">${this.formattedMainValue!.value}</span>
                            <span class="main-number-unit">${this.formattedMainValue!.unit}</span>
                        </div>
                        <div class="graph-wrapper">
                            <div class="chart-wrapper">
                                <canvas id="chart"></canvas>
                            </div>
                            <div class="delta-wrapper">
                                <span class="delta">${this.deltaPlus}${this.delta.val}${this.delta.unit}</span>
                            </div>
                            
                            <div class="period-selector-wrapper">
                                ${getContentWithMenuTemplate(
                                    html`<or-input class="period-selector" .type="${InputType.BUTTON}" .label="${i18next.t(this.period ? this.period : "-")}"></or-input>`,
                                    [{value: "hour", text: "hour"}, {value: "day", text: "day"}, {value: "week", text: "week"}, {value: "month", text: "month"}, {value: "year", text: "year"}]
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
            <or-mwc-dialog id="mdc-dialog-editattribute" dialogTitle="addAttribute" .dialogActions="${dialogEditAttributeActions}"></or-mwc-dialog>
            <or-mwc-dialog id="mdc-dialog-editcurrentvalue" dialogTitle="editCurrentValue" .dialogActions="${dialogEditCurrentValueActions}"></or-mwc-dialog>
            <or-mwc-dialog id="mdc-dialog-editdelta" dialogTitle="editDelta" .dialogActions="${dialogEditDeltaActions}"></or-mwc-dialog>
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

    protected async getAttributeValue(assetId: string, attributeName: string): Promise<any> {

        const response = await manager.rest.api.AssetResource.queryAssets({
            ids: [assetId],
            recursive: false
        });

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
        this.currentPeriod = {
            start: thisMoment.clone().subtract(1, this.period).toDate().getTime(),
            end: thisMoment.clone().toDate().getTime()
        };

        this.getAttributeValue(this.assetId, this.attributeName)
            .then((response) => {
                this.mainValue = response[0].attributes[this.attributeName!].value;
                this.formattedMainValue = this.getFormattedValue(this.mainValue!);

                return this.getDatapointsByAttribute(this.assetId!, this.attributeName!, this.currentPeriod!.start, this.currentPeriod!.end);
            })
            .then((datapoints: ValueDatapoint<any>[]) => {
                this.data = datapoints || [];
                this.delta = this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data));

                this.error = false;

            })
            .catch((err) => {
                this.error = true;
                this.requestUpdate();
            });

    }

    protected getTotalValue(data: ValueDatapoint<any>[]): number {
        return data.reduce(( acc: number, val: ValueDatapoint<any> ) => {
            return val.y ? acc + Math.round(val.y) : acc;
        }, 0);
    }

    protected getHighestValue(data: ValueDatapoint<any>[]): number {
        return Math.max.apply(Math, data.map((e: ValueDatapoint<any>) => e.y || false ));
    }

    protected getFormattedValue(value: number): {value: number, unit: string} {
        const attr = this.asset.attributes![this.attributeName!];
        const roundedVal = +value.toFixed(this.decimals); // + operator prevents str return

        const attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(this.attributeName!);
        const unitKey = Util.getMetaValue(MetaItemType.UNIT_TYPE, attr, attributeDescriptor);
        const unit = i18next.t("units." + unitKey);

        if (!unitKey) { return {value: roundedVal, unit: "" }; }

        return {
            value: roundedVal,
            unit: unit
        };
    }

    protected getFirstKnownMeasurement(data: ValueDatapoint<any>[]): number {
        for (let i = 0; i <= data.length; i++) {
            if (data[i] && data[i].y !== undefined)
                return data[i].y;
        }
        return 0;
    }

    protected getLastKnownMeasurement(data: ValueDatapoint<any>[]): number {
        for (let i = data.length - 1; i >= 0; i--) {
            if (data[i] && data[i].y !== undefined)
                return data[i].y;
        }
        return 0;
    }

    protected getFormattedDelta(firstVal: number, lastVal: number, mode?: string): {val?: number, unit?: string} {
        if (mode === "percentage") {
            if (firstVal && lastVal) {
                if (lastVal === 0 && firstVal === 0) {
                    return {val: 0, unit: "%"};
                } else if (lastVal === 0 && firstVal !== 0) {
                    return {val: 100, unit: "%"};
                } else {
                    const math = Math.round((lastVal - firstVal) / firstVal * 100);
                    return {val: math, unit: "%"};
                }
            } else {
                return {val: 0, unit: "%"};
            }
        }

        return {val: Math.round(lastVal - firstVal), unit: ""};
    }

    protected handleMenuSelect(value: ContextMenuOptions) {
        if (value === "editAttribute") {
            this._openEditAttributeDialog()
        }
        else if (value === "editDelta") {
            this._openEditDeltaDialog()
        }
        else if (value === "editCurrentValue") {
            this._openEditCurrentValueDialog()
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

        this.saveSettings();
        this.getData();
        this.requestUpdate();
    }

}
