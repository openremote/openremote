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
    AssetModelUtil,
    AssetDatapointLTTBQuery
} from "@openremote/model";
import {DefaultColor3, DefaultColor4, manager, Util} from "@openremote/core";
import {Chart, ScatterDataPoint, LineController, LineElement, PointElement, LinearScale, TimeSeriesScale, Title} from "chart.js";
import "chartjs-adapter-moment";
import {OrChartConfig} from "@openremote/or-chart";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-attribute-picker";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {debounce} from "lodash";

export type ContextMenuOption = "editAttribute" | "editDelta" | "editCurrentValue" | "delete";

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
        height: 100%;
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
        height: 100%;
        display: flex;
        flex-direction: column;
    }
        
    .panel-title {
        display: flex;
        align-items: center;
        /*margin: -15px -15px 0 0;*/ /* compensate for the click-space of the plusminus button */
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
        display: flex;
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
        flex: 1 1 auto;
        width: 65%;
        height: 75%;
    }
    .controls-wrapper {
        height: 100%;
        display: flex;
        flex-direction: column;
        place-content: space-between;
    }
    
    .delta-wrapper {
        /*flex: 0 0 50px;*/
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

    @property({type: String})
    public realm?: string;

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
    public showControls: boolean = true;
    @property()
    public showTitle: boolean = true;
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
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    private resizeObserver?: ResizeObserver;

    static get styles() {
        return [
            style
        ];
    }

    constructor() {
        super();
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

        if (changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
            this.assets = [];
            this.loadSettings();
        }

        const reloadData = changedProperties.has("period") || changedProperties.has("compareTimestamp")
            || changedProperties.has("timestamp") || changedProperties.has("assetAttributes") || changedProperties.has("realm");

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
                            min: this._startOfPeriod,
                            max: this._endOfPeriod,
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

    shouldShowControls(): boolean { // Checking for string input as well since that was not working
        return (this.showControls && this.showControls.toString() == "true");
    }
    shouldShowTitle(): boolean {
        return (this.showTitle && this.showTitle.toString() == "true");
    }

    protected render() {

        if (!this.assets || !this.assetAttributes || this.assetAttributes.length === 0) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            ${this.shouldShowControls() ? html`
                                <or-mwc-input class="button" .type="${InputType.BUTTON}" label="selectAttribute" icon="plus" @or-mwc-input-changed="${() => this._openDialog("editAttribute")}"></or-mwc-input>
                            ` : html`
                                <span>${i18next.t('noAttributeConnected')}</span>
                            `}
                        </div>
                    </div>
                </div>
            `;
        }

        if (this.error) {
            return html`
                <div class="panel panel-empty">
                    <div class="panel-content-wrapper">
                        <div class="panel-content">
                            <span>${i18next.t("couldNotRetrieveAttribute")}</span>
                            ${this.shouldShowControls() ? html`
                                <or-mwc-input class="button" .type="${InputType.BUTTON}" label="selectAttribute" icon="plus" @or-mwc-input-changed="${() => this._openDialog("editAttribute")}"></or-mwc-input>
                            ` : undefined}
                        </div>
                    </div>
                </div>
            `;
        }

        this.updateComplete.then(() => {
            this.resizeObserver = new ResizeObserver(debounce((entries: ResizeObserverEntry[]) => {
                const elemSize = entries[0].devicePixelContentBoxSize[0].blockSize;
                this.setLabelSizeByWidth(elemSize);
            }, 200))
            this.resizeObserver.observe(this.shadowRoot!.querySelector(".graph-wrapper")!);
        })

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        ${this.shouldShowTitle() ? html`<span class="panel-title-text">${this.assets[0].name + " - " + i18next.t(this.assetAttributes[0][1].name!)}</span>` : undefined}
                        ${this.shouldShowTitle() ? getContentWithMenuTemplate(html`
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
                        (option) => this.handleMenuSelect(option as ContextMenuOption)) : undefined}
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
                            <div class="controls-wrapper">
                                <div class="delta-wrapper">
                                    <span class="delta">${this.delta ? this.deltaPlus + (this.delta.val !== undefined && this.delta.val !== null ? this.delta.val : "") + (this.delta.unit || "") : ""}</span>
                                </div>
                                ${this.shouldShowControls() ? html`
                                    <div class="period-selector-wrapper">
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input class="period-selector" .type="${InputType.BUTTON}" label="${this.period ? this.period : '-'}"></or-mwc-input>`,
                                                [{value: "hour", text: "hour"}, {value: "day", text: "day"}, {value: "week", text: "week"}, {value: "month", text: "month"}, {value: "year", text: "year"}].map((option) => {
                                                    option.text = i18next.t(option.value);
                                                    return option;
                                                }),
                                                this.period,
                                                (value) => this._setPeriodOption(value)
                                        )}
                                    </div>
                                ` : html`
                                    <or-mwc-input class="period-selector" .type="${InputType.BUTTON}" disabled .label="${i18next.t(this.period ? this.period : "-")}"></or-mwc-input>
                                `}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    protected _openDialog(dialogContent?: ContextMenuOption) {

        let dialog: OrMwcDialog | undefined;

        switch (dialogContent) {
            case "editDelta":
            case "editCurrentValue":
                dialog = new OrMwcDialog()
                    .setHeading(i18next.t(dialogContent))
                    .setDismissAction(null)
                    .setContent(
                        dialogContent === "editDelta"
                            ? html`
                                <or-mwc-input id="delta-mode-picker" value="${this.deltaFormat}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => {this.deltaFormat = evt.detail.value;this.saveSettings();}}" .type="${InputType.LIST}" .options="${[
                                            ["percentage", "Percentage"],
                                            ["absolute", "Absolute"]
                                        ]}"></or-mwc-input>                
                            `
                            : html`
                                <or-mwc-input id="current-value-decimals" .label="${i18next.t("decimals")}" value="${this.mainValueDecimals}" .type="${InputType.TEXT}"></or-mwc-input>
                            `
                    )
                    .setActions([
                        {
                            actionName: "cancel",
                            content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                            action: () => {
                                // Nothing to do here
                            }
                        },
                        {
                            actionName: "yes",
                            default: true,
                            content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="ok" data-mdc-dialog-action="yes"></or-mwc-input>`,
                            action: () => {
                                if (dialogContent === "editDelta") {
                                    this.delta = this.data ? this.getFormattedDelta(this.getFirstKnownMeasurement(this.data), this.getLastKnownMeasurement(this.data)) : undefined;
                                } else {
                                    if (dialog && dialog.shadowRoot && dialog.shadowRoot.getElementById("current-value-decimals")) {
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
                        }
                    ]);

                dialog.addEventListener(OrAssetTreeSelectionEvent.NAME, async (ev: OrAssetTreeSelectionEvent) => {
                    const selectedNode = ev.detail && ev.detail.newNodes.length > 0 ? ev.detail.newNodes[0] : undefined;

                    if (!selectedNode) {
                        this.asset = undefined;
                    } else {
                        // fully load the asset
                        const assetEvent: AssetEvent = await manager.events!.sendEventWithReply({
                            event: {
                                eventType: "read-asset",
                                assetId: selectedNode.asset!.id
                            }
                        });
                        this.asset = assetEvent.asset;
                    }
                });
                break;
            default:
                dialog = new OrAttributePicker()
                    .setHeading(i18next.t("selectAttribute"));

                dialog.addEventListener(OrAttributePickerPickedEvent.NAME, async (ev: OrAttributePickerPickedEvent) => {
                    // handle selected attrs
                    const attrRef = ev.detail[0];
                    try {
                        const response = await manager.rest.api.AssetResource.get(attrRef.id!);
                        this.asset = response.data;
                        this._setAttribute(attrRef.name as string);
                    } catch (e) {
                        console.error("Failed to get assets requested in settings", e);
                    }
                });
                break;
        }

        if (dialog) {
            showDialog(dialog);
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

    protected async loadSettings(reset: boolean = false) {

        this.assetAttributes = [];
        if(!this.period || reset) { this.period = "day"; }
        if(!this.deltaFormat || reset) { this.deltaFormat = "absolute"; }
        if(!this.mainValueDecimals || reset) { this.mainValueDecimals = 0; }

        if (!this.realm) {
            this.realm = manager.getRealm();
        }

        let allConfigs: OrChartConfig[] = await manager.console.retrieveData("OrChartConfig") || [];
        if (!Array.isArray(allConfigs)) {
            allConfigs = [allConfigs];
        }

        if (allConfigs.length === 0 || !this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        let config: OrChartConfig = allConfigs.find(e => e.realm === this.realm) as OrChartConfig;

        const view = config && config.views && config.views[viewSelector] ? config.views[viewSelector][this.panelName] : undefined;

        if (!view) {
            return;
        }

        if (!view.attributeRefs) {
            // Old/invalid config format remove it
            delete config.views[viewSelector][this.panelName];
            const cleanData = [...allConfigs.filter(e => e.realm !== this.realm), config];
            manager.console.storeData("OrChartConfig", cleanData);
            return;
        }

        const assetIds = view.attributeRefs.map((attrRef) => attrRef.id!);

        if (assetIds.length === 0) {
            return;
        }

        this._loading = true;

        if (!assetIds.every(id => !!this.assets.find(asset => asset.id === id))) {
            const query = {
                ids: assetIds
            } as AssetQuery;

            try {
                const response = await manager.rest.api.AssetResource.queryAssets(query);
                const assets = response.data || [];
                view.attributeRefs = view.attributeRefs.filter((attrRef) => !!assets.find((asset) => asset.id === attrRef.id && asset.attributes && asset.attributes.hasOwnProperty(attrRef.name!)));

                allConfigs = [...allConfigs.filter(e => e.realm !== this.realm), config];
                manager.console.storeData("OrChartConfig", allConfigs);
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
        let allConfigs: OrChartConfig[] = await manager.console.retrieveData("OrChartConfig") || [];
        let config: OrChartConfig | undefined = allConfigs.find(e => e.realm === this.realm);

        if (!config) {
            config = {
                realm: this.realm,
                views: {
                }
            }
        }

        if (!config.views[viewSelector]) {
            config.views[viewSelector] = {};
        }

        if (!this.assets || !this.assetAttributes || this.assets.length === 0 || this.assetAttributes.length === 0) {
            delete config.views[viewSelector][this.panelName];
        } else {
            config.realm = this.realm;
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

        allConfigs = [...allConfigs.filter(e => e.realm !== this.realm), config];
        manager.console.storeData("OrChartConfig", allConfigs);
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
        this._startOfPeriod = moment().subtract(1, this.period).toDate().getTime();
        this._endOfPeriod = moment().toDate().getTime();
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

        this.mainValue = currentValue.value;
        this.formattedMainValue = this.getFormattedValue(this.mainValue!);

        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            assetId,
            attributeName,
            {
                type: "lttb",
                fromTimestamp: this._startOfPeriod,
                toTimestamp: this._endOfPeriod,
                amountOfPoints: 20,
            } as AssetDatapointLTTBQuery
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
        this.setLabelSizeByLength(roundedVal.toString());

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

    protected handleMenuSelect(value: ContextMenuOption) {
        if (value === "delete") {
            this.assets = [];
            this.assetAttributes = [];
            this.saveSettings();
        } else {
            this._openDialog(value);
        }
    }

    protected setLabelSizeByLength(value: string) {
        if (value.length >= 20) { this.mainValueSize = "xs" }
        if (value.length < 20) { this.mainValueSize = "s" }
        if (value.length < 15) { this.mainValueSize = "m" }
        if (value.length < 10) { this.mainValueSize = "l" }
        if (value.length < 5) { this.mainValueSize = "xl" }
    }

    protected setLabelSizeByWidth(blockSize: number) {
        if(blockSize < 60) { this.mainValueSize = "s"; }
        else if(blockSize < 100) { this.mainValueSize = "m"; }
        else if(blockSize < 200) { this.mainValueSize = "l"; }
        else { this.mainValueSize = "xl"; }
    }

    protected _setPeriodOption(value: any) {
        this.period = value;
        this.saveSettings();
    }

}
