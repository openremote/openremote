import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import i18next from "i18next";
import {
    Asset,
    AssetEvent,
    AssetQuery,
    Attribute,
    AttributeEvent,
    AttributeRef,
    DatapointInterval,
    ReadAttributeEvent,
    ValueDatapoint,
    WellknownMetaItems
} from "@openremote/model";
import {AssetModelUtil, DefaultColor3, DefaultColor4, manager, Util} from "@openremote/core";
import {Chart, ScatterDataPoint, LineController, LineElement, PointElement, LinearScale, TimeSeriesScale, Title} from "chart.js";
import "chartjs-adapter-moment";
import {OrChartConfig} from "@openremote/or-chart";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import "@openremote/or-mwc-components/or-mwc-dialog";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {OrMwcDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";

export type ContextMenuOptions = "editAttribute" | "editDelta" | "editCurrentValue" | "delete";

Chart.register(LineController, LineElement, PointElement, LinearScale, Title, TimeSeriesScale);

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

@customElement("or-attribute-card")
export class OrAttributeCard extends LitElement {

    @property()
    public panelName?: string;

    protected _style!: CSSStyleDeclaration;

    @property({type: Object})
    public assets: Asset[] = [];

    @property({type: Object})
    private assetAttributes: [number, Attribute<any>][] = [];

    @property()
    private data?: ValueDatapoint<any>[] = undefined;

    @property()
    private mainValue?: number;
    @property()
    private mainValueDecimals: number = 2;
    @property()
    private mainValueSize: "xs" | "s" | "m" | "l" | "xl" = "m";
    @property()
    private delta?: {val?: number, unit?: string} = undefined;
    @property()
    private deltaPlus: string = "";
    @property()
    private deltaFormat: "absolute" | "percentage" = "absolute";
    @property()
    protected _loading: boolean = false;

    private error: boolean = false;

    @property()
    private period: moment.unitOfTime.Base = "day";
    private asset?: Asset;
    private formattedMainValue?: {value: number|undefined, unit: string};

    @query("#chart")
    private _chartElem!: HTMLCanvasElement;
    private _chart?: Chart<"line", ScatterDataPoint[]>;

    @query("#mdc-dialog")
    private _dialog!: OrMwcDialog;

    static get styles() {
        return [
            style
        ];
    }

    constructor() {
        super();
        this.addEventListener(OrAssetTreeSelectionEvent.NAME, this._onTreeSelectionChanged);
    }

    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
    }

    firstUpdated() {
        this.loadSettings();
    }

    updated(changedProperties: PropertyValues) {
        // if (changedProperties.has("assetAttributes")) {
        //     if (this._dialog && this._dialog.isOpen) {
        //         this._refreshDialog();
        //     }
        // }

        const reloadData = changedProperties.has("period") || changedProperties.has("compareTimestamp") || changedProperties.has("timestamp") || changedProperties.has("assetAttributes");

        if (reloadData) {
            this.data = undefined;
            if (this._chart) {
                this._chart.destroy();
                this._chart = undefined;
            }
            this.loadData();
        }

        if (!this.data || this.data.length === 0) {
            return;
        }

        if (!this._chart) {
            this._chart = new Chart(this._chartElem.getContext("2d")!, {
                type: "line",
                data: {
                    datasets: [
                        {
                            data: this.data.filter(value => value.y != null) as ScatterDataPoint[],
                            tension: 0.1,
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
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            displayColors: false,
                            callbacks: {
                                title: (context) => {
                                    return "";
                                },
                                // label: (context) => {
                                //     return context.parsed.y; // Removes the colon before the label
                                // },
                                footer: () => {
                                    return ""; // Hack the broken vertical alignment of body with footerFontSize: 0
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            display: false
                        },
                        x: {
                            type: "time",
                            display: false,
                        }
                    }
                }
            });
        } else {
            if (changedProperties.has("data")) {
                this._chart.data.datasets![0].data = this.data.filter(value => value.y != null) as ScatterDataPoint[];
                this._chart.update();
                this.delta = this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data));
            }
        }

        if (changedProperties.has("mainValue") || changedProperties.has("mainValueDecimals")) {
            this.formattedMainValue = this.getFormattedValue(this.mainValue!);
        }

        if (changedProperties.has("delta")) {
            this.deltaPlus = this .delta && this.delta.val! > 0 ? "+" : "";
        }
    }

    protected render() {

        if (!this.assets || !this.assetAttributes || this.assetAttributes.length === 0) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-mwc-input>
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
                            <or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-mwc-input>
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
                        <span class="panel-title-text">${this.assets[0].name + " - " + i18next.t(this.assetAttributes[0][1].name!)}</span>
                        ${getContentWithMenuTemplate(html`
                            <or-mwc-input icon="dots-vertical" type="button"></or-mwc-input>
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
                },
                {
                    text: i18next.t("delete"),
                    value: "delete"
                }
            ],
            undefined,
            (values: string | string[]) => this.handleMenuSelect(values as ContextMenuOptions))}
                    </div>
                    <div class="panel-content">
                        <div class="mainvalue-wrapper">
                            <span class="main-number-icon">${this.assets && this.assets.length === 1 ? getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[0].type!)) : ""}</span>
                            <span class="main-number ${this.mainValueSize}">${this.formattedMainValue ? this.formattedMainValue.value : ""}</span>
                            <span class="main-number-unit ${this.mainValueSize}">${this.formattedMainValue ? this.formattedMainValue.unit : ""}</span>      
                        </div>
                        <div class="graph-wrapper">
                            <div class="chart-wrapper">
                                <canvas id="chart"></canvas>
                            </div>
                            <div class="delta-wrapper">
                                <span class="delta">${this.delta ? this.deltaPlus + (this.delta.val !== undefined && this.delta.val !== null ? this.delta.val : "") + (this.delta.unit || "") : ""}</span>
                            </div>
                            
                            <div class="period-selector-wrapper">
                                ${getContentWithMenuTemplate(
            html`<or-mwc-input class="period-selector" .type="${InputType.BUTTON}" .label="${i18next.t(this.period ? this.period : "-")}"></or-mwc-input>`,
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

    protected _refreshDialog(dialogContent?: ContextMenuOptions) {
        if (this._dialog) {
            if (dialogContent === "editDelta") {
                const options = [
                    ["percentage", "Percentage"],
                    ["absolute", "Absolute"]
                ];

                this._dialog.dialogTitle = i18next.t("editDelta");

                this._dialog.dialogActions = [
                    {
                        actionName: "cancel",
                        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-mwc-input>`,
                        action: () => {
                            // Nothing to do here
                        }
                    },
                    {
                        actionName: "yes",
                        default: true,
                        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("ok")}" data-mdc-dialog-action="yes"></or-mwc-input>`,
                        action: () => {
                            this.delta = this.data ? this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data)) : undefined;
                        }
                    }
                ];

                this._dialog.dialogContent = html`
                    <or-mwc-input id="delta-mode-picker" value="${this.deltaFormat}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => {this.deltaFormat = evt.detail.value;this.saveSettings();}}" .type="${InputType.LIST}" .options="${options}"></or-mwc-input>                
                `;

                this._dialog.dismissAction = null;
            }
            else if (dialogContent === "editCurrentValue") {

                this._dialog.dialogTitle = i18next.t("editCurrentValue");

                this._dialog.dialogActions = [
                    {
                        actionName: "cancel",
                        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-mwc-input>`,
                        action: () => {
                            // Nothing to do here
                        }
                    },
                    {
                        actionName: "yes",
                        default: true,
                        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("ok")}" data-mdc-dialog-action="yes"></or-mwc-input>`,
                        action: () => {
                            const dialog: OrMwcDialog = this._dialog as OrMwcDialog;
                            if (dialog.shadowRoot && dialog.shadowRoot.getElementById("current-value-decimals")) {
                                const elm = dialog.shadowRoot.getElementById("current-value-decimals") as HTMLInputElement;
                                const input = parseInt(elm.value, 10);
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
                    <or-mwc-input id="current-value-decimals" .label="${i18next.t("decimals")}" value="${this.mainValueDecimals}" .type="${InputType.TEXT}"></or-mwc-input>
                `;

                this._dialog.dismissAction = null;
            }
            else {

                this._dialog.dialogTitle = i18next.t("addAttribute");

                this._dialog.dialogActions = [
                    {
                        actionName: "cancel",
                        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-mwc-input>`,
                        action: () => {
                            // Nothing to do here
                        }
                    },
                    {
                        actionName: "yes",
                        default: true,
                        content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("add")}" data-mdc-dialog-action="yes"></or-mwc-input>`,
                        action: () => {
                            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog") as OrMwcDialog;
                            if (dialog.shadowRoot && dialog.shadowRoot.getElementById("attribute-picker")) {
                                const elm = dialog.shadowRoot.getElementById("attribute-picker") as OrMwcInput;
                                this._setAttribute(elm.value as string);
                            }
                        }
                    }
                ];

                this._dialog.dialogContent = html`
                    <or-asset-tree id="chart-asset-tree"  readonly
                        .selectedIds="${this.asset ? [this.asset.id] : null}"></or-asset-tree>
                    ${this.asset && this.asset.attributes ? html`
                        <or-mwc-input id="attribute-picker" 
                            style="display:flex;"
                            .label="${i18next.t("attribute")}" 
                            .type="${InputType.LIST}"
                            .options="${this._getAttributeOptions()}"
                            .value="${this.assetAttributes && this.assetAttributes.length > 0 ? this.assetAttributes[0][1].name : undefined}"></or-mwc-input>
                    ` : ``}
                `;

                this._dialog.dismissAction = null;
            }
        }
    }

    protected _openDialog(dialogContent?: ContextMenuOptions) {
        if (this._dialog) {
            this._refreshDialog(dialogContent);
            this._dialog.open();
        }
    }

    protected async _onTreeSelectionChanged(event: OrAssetTreeSelectionEvent) {
        // Need to fully load the asset
        if (!manager.events) {
            return;
        }

        const selectedNode = event.detail && event.detail.newNodes.length > 0 ? event.detail.newNodes[0] : undefined;

        if (!selectedNode) {
            this.asset = undefined;
        } else {
            // fully load the asset
            const assetEvent: AssetEvent = await manager.events.sendEventWithReply({
                event: {
                    eventType: "read-asset",
                    assetId: selectedNode.asset!.id
                }
            });
            this.asset = assetEvent.asset;
            this._refreshDialog();
        }
    }

    protected _getAttributeOptions(): [string, string][] | undefined {
        if(!this.asset || !this.asset.attributes) {
            return;
        }

        const attributes = Object.values(this.asset.attributes);
        if (attributes && attributes.length > 0) {
            return attributes
                .filter((attribute) => attribute.meta && (attribute.meta.hasOwnProperty(WellknownMetaItems.STOREDATAPOINTS) ? attribute.meta[WellknownMetaItems.STOREDATAPOINTS] : attribute.meta.hasOwnProperty(WellknownMetaItems.AGENTLINK)))
                .filter((attr) => (this.assetAttributes && !this.assetAttributes.some(([index, assetAttr]) => (assetAttr.name === attr.name) && this.assets[index].id === this.asset!.id)))
                .map((attr) => {
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.asset!.type, attr.name, attr);
                    const label = Util.getAttributeLabel(attr, descriptors[0], this.asset!.type, false);
                    return [attr.name!, label];
                });
        }
    }

    private _setAttribute(attributeName: string) {
        if (this.asset && attributeName) {
            const attr = this.asset.attributes ? this.asset.attributes[attributeName] : undefined;
            this.assets = [this.asset];
            this.assetAttributes = attr ? [[0,attr]] : [];
            this.saveSettings();
        }
    }

    protected _cleanup() {
        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
    }

    protected async loadSettings() {

        this.assetAttributes = [];
        this.period = "day";
        this.deltaFormat = "absolute";
        this.mainValueDecimals = 0;

        const configStr = await manager.console.retrieveData("OrChartConfig");

        if (!configStr || !this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        let config: OrChartConfig;

        try {
            config = JSON.parse(configStr) as OrChartConfig;
        } catch (e) {
            console.error("Failed to load chart config", e);
            manager.console.storeData("OrChartConfig", null);
            return;
        }

        const view = config && config.views ? config.views[viewSelector][this.panelName] : undefined;

        if (!view) {
            return;
        }

        if (!view.attributeRefs) {
            // Old/invalid config format remove it
            delete config.views[viewSelector][this.panelName];
            manager.console.storeData("OrChartConfig", JSON.stringify(config));
            return;
        }

        const assetIds = view.attributeRefs.map((attrRef) => attrRef.id!);

        if (assetIds.length === 0) {
            return;
        }

        this._loading = true;

        if (!assetIds.every(id => !!this.assets.find(asset => asset.id === id))) {
            const query = {
                select: {
                    excludePath: true,
                    excludeParentInfo: true
                },
                ids: assetIds
            } as AssetQuery;

            try {
                const response = await manager.rest.api.AssetResource.queryAssets(query);
                const assets = response.data || [];
                view.attributeRefs = view.attributeRefs.filter((attrRef) => !!assets.find((asset) => asset.id === attrRef.id && asset.attributes && asset.attributes.hasOwnProperty(attrRef.name!)));
                manager.console.storeData("OrChartConfig", JSON.stringify(config));
                this.assets = assets.filter((asset) => view.attributeRefs!.find((attrRef) => attrRef.id === asset.id));
            } catch (e) {
                console.error("Failed to get assets requested in settings", e);
            }

            this._loading = false;

            if (this.assets && this.assets.length > 0) {
                this.assetAttributes = view.attributeRefs.map((attrRef) => {
                    const assetIndex = this.assets.findIndex((asset) => asset.id === attrRef.id);
                    const asset = assetIndex >= 0 ? this.assets[assetIndex] : undefined;
                    return asset && asset.attributes ? [assetIndex!, asset.attributes[attrRef.name!]] : undefined;
                }).filter((indexAndAttr) => !!indexAndAttr) as [number, Attribute<any>][];
                this.period = view.period || "day";
                this.mainValueDecimals = view.decimals || 0;
                this.deltaFormat = view.deltaFormat || "absolute";
            }
        }
    }

    async saveSettings() {

        if (!this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        const configStr = await manager.console.retrieveData("OrChartConfig");
        let config: OrChartConfig | undefined;

        if (configStr) {
            try {
                config = JSON.parse(configStr) as OrChartConfig;
            } catch (e) {
                console.error("Failed to load chart config", e);
            }
        }

        if (!config) {
            config = {
                views: {
                    [viewSelector]: {
                    }
                }
            }
        }

        if (!this.assets || !this.assetAttributes || this.assets.length === 0 || this.assetAttributes.length === 0) {
            delete config.views[viewSelector][this.panelName];
        } else {
            config.views[viewSelector][this.panelName] = {
                attributeRefs: this.assetAttributes.map(([index, attr]) => {
                    const asset = this.assets[index];
                    return !!asset ? {id: asset.id, name: attr.name} as AttributeRef : undefined;
                }).filter((attrRef) => !!attrRef) as AttributeRef[],
                period: this.period,
                deltaFormat: this.deltaFormat,
                decimals: this.mainValueDecimals
            };
        }

        manager.console.storeData("OrChartConfig", JSON.stringify(config));
    }

    protected async loadData() {

        if (this._loading || this.data || !this.assetAttributes || !this.assets || this.assets.length === 0 || this.assetAttributes.length === 0 || !this.period) {
            return;
        }

        this._loading = true;

        let interval: DatapointInterval = DatapointInterval.HOUR;
        let stepSize = 1;

        switch (this.period) {
            case "hour":
                interval = DatapointInterval.MINUTE;
                stepSize = 5;
                break;
            case "day":
                interval = DatapointInterval.HOUR;
                stepSize = 1;
                break;
            case "week":
                interval = DatapointInterval.HOUR;
                stepSize = 6;
                break;
            case "month":
                interval = DatapointInterval.DAY;
                stepSize = 1;
                break;
            case "year":
                interval = DatapointInterval.MONTH;
                stepSize = 1;
                break;
        }

        const lowerCaseInterval = interval.toLowerCase();
        const startOfPeriod = moment().startOf(this.period).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime(); moment().clone().subtract(1, this.period).toDate().getTime();
        const endOfPeriod = moment().endOf(this.period).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
        this.mainValue = undefined;
        this.formattedMainValue = undefined;
        const assetId = this.assets[0].id!;
        const attributeName = this.assetAttributes[0][1].name!;

        const currentValue: AttributeEvent = await manager.events!.sendEventWithReply({
            event: {
                eventType: "read-asset-attribute",
                ref: {
                    id: assetId,
                    name: attributeName
                }
            } as ReadAttributeEvent
        });

        this.mainValue = currentValue.attributeState!.value;
        this.formattedMainValue = this.getFormattedValue(this.mainValue!);

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            assetId,
            attributeName,
            {
                interval: interval,
                fromTimestamp: startOfPeriod,
                toTimestamp: endOfPeriod
            }
        );

        this._loading = false;

        if (response.status === 200) {
            this.data = response.data;
            this.delta = this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data));
            this.deltaPlus = this.delta && this.delta.val! > 0 ? "+" : "";
            this.error = false;
        }
    }

    protected getTotalValue(data: ValueDatapoint<any>[]): number {
        return data.reduce(( acc: number, val: ValueDatapoint<any> ) => {
            return val.y ? acc + Math.round(val.y) : acc;
        }, 0);
    }

    protected getHighestValue(data: ValueDatapoint<any>[]): number {
        return Math.max.apply(Math, data.map((e: ValueDatapoint<any>) => e.y || false ));
    }

    protected getFormattedValue(value: number | undefined): {value: number, unit: string} | undefined {
        if (value === undefined || !this.assets || !this.assetAttributes || this.assets.length === 0 || this.assetAttributes.length === 0) {
            return;
        }

        const attr = this.assetAttributes[0][1];
        const roundedVal = +value.toFixed(this.mainValueDecimals); // + operator prevents str return
        const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attr.name!, this.assets[0].type!);
        const units = Util.resolveUnits(Util.getAttributeUnits(attr, attributeDescriptor, this.assets[0].type));
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

    protected handleMenuSelect(value: ContextMenuOptions) {
        if (value === "delete") {
            this.assets = [];
            this.assetAttributes = [];
            this.saveSettings();
        } else {
            this._openDialog(value);
        }
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
    }

}
