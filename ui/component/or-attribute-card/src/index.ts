import {css, customElement, html, LitElement, property, PropertyValues, query, unsafeCSS} from "lit-element";
import i18next from "i18next";
import {
    Asset,
    AssetEvent,
    AssetQuery,
    Attribute,
    DatapointInterval,
    ValueDatapoint,
    WellknownMetaItems
} from "@openremote/model";
import {AssetModelUtil, DefaultColor3, DefaultColor4, manager, Util} from "@openremote/core";
import Chart, {ChartTooltipCallback} from "chart.js";
import {OrChartConfig} from "@openremote/or-chart";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {OrMwcDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/dist/or-mwc-menu";

export type ContextMenuOptions = "editAttribute" | "editDelta" | "editCurrentValue";

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
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        flex: 0 0 auto;
    }
    
    .panel-title-text {
        flex: 1;
        text-transform: uppercase;
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
    
    .main-number.xs, .main-number-unit.xs {
        font-size: 18px;
    }
    .main-number.s, .main-number-unit.s {
        font-size: 24px;
    }
    .main-number.m, .main-number-unit.m {
        font-size: 30px;
    }
    .main-number.l, .main-number-unit.l {
        font-size: 36px;
    }
    .main-number.xl, .main-number-unit.xl {
        font-size: 42px;
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
    private assetAttributes: Attribute<any>[] = [];

    @property()
    private data: ValueDatapoint<any>[] = [];

    @property()
    private mainValue?: number;
    @property()
    private mainValueDecimals: number = 2;
    @property()
    private mainValueSize: "xs" | "s" | "m" | "l" | "xl" = "m";
    @property()
    private delta: {val?: number, unit?: string} = {};
    @property()
    private deltaPlus: string = "";
    @property()
    private deltaFormat: string = "absolute";

    private error: boolean = false;

    private period?: moment.unitOfTime.Base = "day";
    private now: Date = new Date();
    private currentPeriod?: { start: number; end: number };

    private asset?: Asset;
    private formattedMainValue?: {value: number|undefined, unit: string};

    @query("#chart")
    private _chartElem!: HTMLCanvasElement;
    private _chart?: Chart;

    @query("#mdc-dialog")
    private _dialog!: OrMwcDialog;

    static get styles() {
        return [
            style
        ];
    }

    constructor() {
        super();
        this.addEventListener(OrAttributeCardAddAttributeEvent.NAME, this._setAttribute);
        this.addEventListener(OrAssetTreeSelectionEvent.NAME, this._onTreeSelectionChanged);
    }

    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
        this.getData();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
    }

    updated(changedProperties: PropertyValues) {
        if (changedProperties.has("asset") || changedProperties.has("assetAttributes")) {
            if (this._dialog && this._dialog.isOpen) {
                this._refreshDialog();
            }
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
            }
        }

        if (changedProperties.has("mainValue") || changedProperties.has("mainValueDecimals")) {
            this.formattedMainValue = this.getFormattedValue(this.mainValue!);
        }

        if (changedProperties.has("delta")) {
            this.deltaPlus = (this.delta.val && this.delta.val > 0) ? "+" : "";
        }
    }

    async onCompleted() {
        await this.updateComplete;
    }

    protected _refreshDialog(dialogContent?: ContextMenuOptions) {
        if (this._dialog) {
            if (dialogContent === "editDelta") {
                const options = [
                    ["percentage", "Percentage"],
                    ["absolute", "Absolute"]
                ];

                this._dialog.dialogTitle= i18next.t("editDelta");

                this._dialog.dialogActions = [
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
                            this.delta = this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data));
                        }
                    }
                ];

                this._dialog.dialogContent = html`
                    <or-input id="delta-mode-picker" value="${this.deltaFormat}" @or-input-changed="${(evt: OrInputChangedEvent) => {this.deltaFormat = evt.detail.value;this.saveSettings();}}" .type="${InputType.LIST}" .options="${options}"></or-input>                
                `;
            }
            else if (dialogContent === "editCurrentValue") {

                this._dialog.dialogTitle= i18next.t("editCurrentValue");

                this._dialog.dialogActions = [
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
                            const dialog: OrMwcDialog = this._dialog as OrMwcDialog;
                            if (dialog.shadowRoot && dialog.shadowRoot.getElementById("current-value-decimals")) {
                                const elm = dialog.shadowRoot.getElementById("current-value-decimals") as HTMLInputElement;
                                const input = parseInt(elm.value);
                                if (input < 0) {this.mainValueDecimals = 0;}
                                else if (input > 10) {this.mainValueDecimals = 10;}
                                else {this.mainValueDecimals = input;}
                                this.formattedMainValue = this.getFormattedValue(this.mainValue!);
                                this.saveSettings();
                            }
                        }
                    }
                ];

                this._dialog.dialogContent = html`
                    <or-input id="current-value-decimals" .label="${i18next.t("decimals")}" value="${this.mainValueDecimals}" .type="${InputType.TEXT}"></or-input>
                `;
            }
            else {

                this._dialog.dialogTitle= i18next.t("addAttribute");

                this._dialog.dialogActions = [
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

                this._dialog.dialogContent = html`
                    <or-asset-tree id="chart-asset-tree"  readonly
                        .selectedIds="${this.asset ? [this.asset.id] : null}"></or-asset-tree>
                    ${this.asset && this.asset.attributes ? html`
                        <or-input id="attribute-picker" 
                            style="display:flex;"
                            .label="${i18next.t("attribute")}" 
                            .type="${InputType.LIST}"
                            .options="${this._getAttributeOptions()}"
                            .value="${this.attributeName}"></or-input>
                    ` : ``}
                `;
            }
        }
    }

    protected _openDialog(dialogContent?: ContextMenuOptions) {
        if (this._dialog) {
            this._refreshDialog(dialogContent);
            this._dialog.open();
        }
    }

    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionEvent) {
        // Need to fully load the asset
        if (!manager.events) {
            return;
        }

        const selectedNode = event.detail && event.detail.newNodes.length > 0 ? event.detail.newNodes[0] : undefined;

        if (!selectedNode) {
            this.asset = undefined;
        } else {
            // fully load the asset
            manager.events.sendEventWithReply({
                event: {
                    eventType: "read-asset",
                    assetId: selectedNode.asset!.id
                }
            }).then((ev) => {
                this.asset = (ev as AssetEvent).asset;
                this.assetAttributes = this.asset!.attributes ? Object.values(this.asset!.attributes!) : [];
            }).catch(() => this.asset = undefined);
        }
    }

    protected _getAttributeOptions(): [string, string][] | undefined {
        if (!this.asset || !this.assetAttributes) {
            return;
        }

        if (this.shadowRoot && this.shadowRoot.getElementById("attribute-picker")) {
            const elm = this.shadowRoot.getElementById("attribute-picker") as HTMLInputElement;
            elm.value = "";
        }

        let attributes = this.assetAttributes;

        if (attributes && attributes.length > 0) {
            return attributes
                .filter((attribute) => attribute.meta && (attribute.meta.hasOwnProperty(WellknownMetaItems.STOREDATAPOINTS) ? attribute.meta[WellknownMetaItems.STOREDATAPOINTS] : attribute.meta.hasOwnProperty(WellknownMetaItems.AGENTLINK)))
                .map((attr: Attribute<any>) => {
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.asset!.type, attr.name, attr);
                    return [attr.name!, Util.getAttributeLabel(attr, descriptors[0], this.asset!.type, false)];
                });
        }
    }

    private _setAttribute(event:OrAttributeCardAddAttributeEvent) {
        if (this.asset && event) {
            const attr = this.asset.attributes ? this.asset.attributes[event.detail] : undefined;
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
        if (!configStr || !this.panelName) return;

        const viewSelector = this.assetId ? this.assetId : window.location.hash;
        const config = JSON.parse(configStr) as OrChartConfig;
        const view = config.views[viewSelector][this.panelName];
        if (!view) return;
        if (view.assetIds && view.assetIds.length === 1 && view.assetIds[0] === this.assetId) return;

        if (view.assetIds && view.attributes) {
            const query: AssetQuery = {
                ids: view.assetIds
            }

            manager.rest.api.AssetResource.queryAssets(query).then((response) => {
                const assets = response.data;
                if (!view || !view.assetIds || !view.attributes || assets.length !== view.assetIds.length || assets.length !== view.attributes.length) return;

                this.assetAttributes = view.attributes.map((attr: string, index: number) => assets[index] && assets[index].attributes ? assets[index].attributes![attr] : undefined).filter(attr => !!attr) as Attribute<any>[];

                if (this.assetAttributes && this.assetAttributes.length > 0) {
                    this.assetId = assets[0].id;
                    this.period = view.period;
                    if(view.deltaFormat) this.deltaFormat = view.deltaFormat;
                    if(view.decimals) this.mainValueDecimals = view.decimals;
                    this.attributeName = this.assetAttributes[0].name;
                    this.getData();
                }
            });
        }
    }

    saveSettings() {
        const viewSelector = window.location.hash;
        const attributes = this.attributeName ? [this.attributeName] : undefined;
        const assetIds = this.assetId ? [this.assetId] : undefined;
        const configStr = window.localStorage.getItem('OrChartConfig')
        if (!this.panelName) return;

        let config: OrChartConfig;
        if (configStr) {
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
            period: this.period,
            deltaFormat: this.deltaFormat,
            decimals: this.mainValueDecimals
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

        if (!this.assetId || !this.attributeName) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-input>
                        </div>
                    </div>
                </div>
                <or-mwc-dialog id="mdc-dialog"></or-mwc-dialog>
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
                <or-mwc-dialog id="mdc-dialog"><or-mwc-dialog>
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        <span class="panel-title-text">${this.asset ? (this.asset.name + " - " + i18next.t(this.attributeName)) : ""}</span>
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
                            <span class="main-number ${this.mainValueSize}">${this.formattedMainValue!.value}</span>
                            <span class="main-number-unit ${this.mainValueSize}">${this.formattedMainValue!.unit}</span>                        
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
            <or-mwc-dialog id="mdc-dialog"></or-mwc-dialog>
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
                this.deltaPlus = (this.delta.val && this.delta.val > 0) ? "+" : "";

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

    protected getFormattedValue(value: number): {value: number, unit: string} | undefined {
        if (!this.asset) {
            return;
        }

        const attr = this.asset.attributes![this.attributeName!];
        const roundedVal = +value.toFixed(this.mainValueDecimals); // + operator prevents str return

        const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(this.attributeName!, this.asset!.type);
        const units = Util.resolveUnits(Util.getAttributeUnits(attr, attributeDescriptor, this.asset!.type));
        this.setMainValueSize(roundedVal.toString());

        if (!units) { return {value: roundedVal, unit: "" }; }
        return {
            value: roundedVal,
            unit: units
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

    protected getFormattedDelta(firstVal: number, lastVal: number): {val?: number, unit?: string} {
        if (this.deltaFormat === "percentage") {
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

    protected handleMenuSelect(value: ContextMenuOptions) {
        this._openDialog(value);
    }

    protected setMainValueSize(value: string) {
        if (value.length >= 20) { this.mainValueSize = "xs" }
        if (value.length < 20) { this.mainValueSize = "s" }
        if (value.length < 15) { this.mainValueSize = "m" }
        if (value.length < 10) { this.mainValueSize = "l" }
        if (value.length < 5) { this.mainValueSize = "xl" }
    }

    protected _setPeriodOption(value: any) {
        this.period = value;

        this.saveSettings();
        this.getData();
        this.requestUpdate();
    }

}
