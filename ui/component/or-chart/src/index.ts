import {
    css,
    html,
    LitElement,
    PropertyValues,
    TemplateResult,
    unsafeCSS
} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {Asset, Attribute, AttributeRef, DatapointInterval, WellknownMetaItems, ReadAssetEvent, AssetEvent, ValueDatapoint, AssetQuery} from "@openremote/model";
import manager, {
    AssetModelUtil,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    Util
} from "@openremote/core";
import "@openremote/or-asset-tree";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Chart, ChartDataset, TimeUnit, ScatterDataPoint, ScatterController, LineController, LineElement, PointElement, LinearScale, TimeScale,
    Filler,
    Legend,
    Title,
    Tooltip,
    ChartConfiguration,
    TimeScaleOptions} from "chart.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import ChartAnnotation, { AnnotationOptions } from "chartjs-plugin-annotation";
import "chartjs-adapter-moment";
import {GenericAxiosResponse } from "@openremote/rest";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";

Chart.register(LineController, ScatterController, LineElement, PointElement, LinearScale, TimeScale, Title, Filler, Legend, Tooltip, ChartAnnotation);

export class OrChartEvent extends CustomEvent<OrChartEventDetail> {

    public static readonly NAME = "or-chart-event";

    constructor(value?: any, previousValue?: any) {
        super(OrChartEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface ChartViewConfig {
    attributeRefs?: AttributeRef[];
    timestamp?: number;
    compareOffset?: number;
    period?: moment.unitOfTime.Base;
    deltaFormat?: "absolute" | "percentage";
    decimals?: number;
}

export interface OrChartEventDetail {
    value?: any;
    previousValue?: any;
}

declare global {
    export interface HTMLElementEventMap {
        [OrChartEvent.NAME]: OrChartEvent;
    }
}

export interface ChartConfig {
    xLabel?: string;
    yLabel?: string;
}

export interface OrChartConfig {
    chart?: ChartConfig;
    realm?: string;
    views: {[name: string]: {
        [panelName: string]: ChartViewConfig
    }};
}

// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const dialogStyle = require("@material/dialog/dist/mdc.dialog.css");
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

// language=CSS
const style = css`
    :host {
        
        --internal-or-chart-background-color: var(--or-chart-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-chart-text-color: var(--or-chart-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-chart-controls-margin: var(--or-chart-controls-margin, 0 0 20px 0);       
        --internal-or-chart-controls-margin-children: var(--or-chart-controls-margin-children, 0 auto 20px auto);            
        --internal-or-chart-graph-fill-color: var(--or-chart-graph-fill-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-chart-graph-fill-opacity: var(--or-chart-graph-fill-opacity, 1);       
        --internal-or-chart-graph-line-color: var(--or-chart-graph-line-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-chart-graph-point-color: var(--or-chart-graph-point-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-chart-graph-point-border-color: var(--or-chart-graph-point-border-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        --internal-or-chart-graph-point-radius: var(--or-chart-graph-point-radius, 4);
        --internal-or-chart-graph-point-hit-radius: var(--or-chart-graph-point-hit-radius, 20);       
        --internal-or-chart-graph-point-border-width: var(--or-chart-graph-point-border-width, 2);
        --internal-or-chart-graph-point-hover-color: var(--or-chart-graph-point-hover-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));       
        --internal-or-chart-graph-point-hover-border-color: var(--or-chart-graph-point-hover-border-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-chart-graph-point-hover-radius: var(--or-chart-graph-point-hover-radius, 4);      
        --internal-or-chart-graph-point-hover-border-width: var(--or-chart-graph-point-hover-border-width, 2);
        
        width: 100%;
        display: block; 
    }

    .line-label {
        border-width: 1px;
        border-color: var(--or-app-color3);
        margin-right: 5px;
    }

    .line-label.solid {
        border-style: solid;
    }

    .line-label.dashed {
        background-image: linear-gradient(to bottom, var(--or-app-color3) 50%, white 50%);
        width: 2px;
        border: none;
        background-size: 10px 16px;
        background-repeat: repeat-y;
    }
    
    .button-icon {
        align-self: center;
        padding: 10px;
        cursor: pointer;
    }

    a {
        display: flex;
        cursor: pointer;
        text-decoration: underline;
        font-weight: bold;
        color: var(--or-app-color1);
        --or-icon-width: 12px;
    }

    .mdc-dialog .mdc-dialog__surface {
        min-width: 600px;
        height: calc(100vh - 50%);
    }
    
    :host([hidden]) {
        display: none;
    }
    
    #container {
        display: flex;
        min-width: 0;
        flex-direction: row;
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
    .interval-controls,
    .period-controls {
        display: flex;
        flex-wrap: wrap;
        flex-direction: row;
    }

    .period-controls {
        --or-icon-fill: var(--or-app-color3);
    }

    #controls {
        display: flex;
        flex-wrap: wrap;
        margin: var(--internal-or-chart-controls-margin);
        min-width: 320px;
        padding-left: 10px;
        flex-direction: column;
        margin: 0;
    }

    #attribute-list {
        overflow: auto;
        flex: 1 1 0;
        min-height: 150px;
        width: 100%;
        display: flex;
        flex-direction: column;
    }
    
    .attribute-list-item {
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
        min-height: 50px;
    }

    .button-clear {
        background: none;
        visibility: hidden;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .attribute-list-item:hover .button-clear {
        visibility: visible;
    }

    .button-clear:hover {
        --or-icon-fill: var(--or-app-color4);
    }
    
    .attribute-list-item-label {
        display: flex;
        flex: 1 1 0;
        line-height: 16px;
        flex-direction: column;
    }

    .attribute-list-item-bullet {
        width: 14px;
        height: 14px;
        border-radius: 7px;
        margin-right: 10px;
    }

    .attribute-list-item .button.delete {
        display: none;
    }

    .attribute-list-item:hover .button.delete {
        display: block;
    }

    #controls > * {
        margin-top: 5px;
        margin-bottom: 5px;
    }

    .dialog-container {
        display: flex;
        flex-direction: row;
        flex: 1 1 0;
    }

    .dialog-container > * {
        flex: 1 1 0;
    }
    
    .dialog-container > or-mwc-input {
        background-color: var(--or-app-color2);
        border-left: 3px solid var(--or-app-color4);
    }

    #chart-container {
        flex: 1 1 0;
        position: relative;
        overflow: auto;
        min-height: 400px;
        max-height: 550px;
    }

    canvas {
        width: 100% !important;
    }

    @media screen and (max-width: 1280px) {
        #chart-container {
            max-height: 330px;
        }
    }

    @media screen and (max-width: 769px) {
        .mdc-dialog .mdc-dialog__surface {
            min-width: auto;

            max-width: calc(100vw - 32px);
            max-height: calc(100% - 32px);
        }

        #container {
            flex-direction: column;
        }

        #controls {
            min-width: 100%;
            padding-left: 0;
        }
    }
`;

@customElement("or-chart")
export class OrChart extends translate(i18next)(LitElement) {

    public static DEFAULT_TIMESTAMP_FORMAT = "L HH:mm:ss";

    static get styles() {
        return [
            css`${unsafeCSS(tableStyle)}`,
            css`${unsafeCSS(dialogStyle)}`,
            style
        ];
    }

    @property({type: Object})
    public assets: Asset[] = [];

    @property({type: Object})
    private activeAsset?: Asset;

    @property({type: Object})
    public assetAttributes: [number, Attribute<any>][] = []; 

    @property({type: Array})
    public colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    @property({type: String})
    public period: moment.unitOfTime.Base = "day";

    @property({type: Number})
    public timestamp: Date = moment().set('minute', 0).toDate();

    @property({type: Number})
    public compareTimestamp?: Date = moment().set('minute', 0).toDate();

    @property({type: Object})
    public config?: OrChartConfig;

    @property({type: String})
    public realm?: string;

    @property()
    public panelName?: string;

    @property()
    protected _loading: boolean = false;

    @property()
    protected _data?: ChartDataset<"line", ScatterDataPoint[]>[] = undefined;

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLCanvasElement;

    protected _dialogElem!: HTMLElement;

    protected _chart?: Chart;
    protected _style!: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _timeUnits?: TimeUnit;
    protected _stepSize?: number;
    protected _updateTimestampTimer: number | null = null;

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
        super.updated(changedProperties);

        if (changedProperties.has("realm")) {
            this.assets = [];
            this.loadSettings();
        }

        const reloadData = changedProperties.has("period") || changedProperties.has("compareTimestamp")
            || changedProperties.has("timestamp") || changedProperties.has("assetAttributes") || changedProperties.has("realm");

        if (reloadData) {
            this._data = undefined;
            if (this._chart) {
                this._chart.destroy();
                this._chart = undefined;
            }
            this._loadData();
        }

        if (!this._data) {
            return;
        }

        const now = moment().toDate().getTime();

        if (!this._chart) {
            const options = {
                type: "line",
                data: {
                    datasets: this._data
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    onResize:() => this.dispatchEvent(new OrChartEvent("resize")),
                    showLines: true,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            mode: "x",
                            intersect: false,
                            xPadding: 10,
                            yPadding: 10,
                            titleMarginBottom: 10
                        },
                        annotation: {
                            annotations: [
                                {
                                    type: "line",
                                    xMin: now,
                                    xMax: now,
                                    borderColor: "#275582",
                                    borderWidth: 2
                                }
                            ]
                        },
                    },
                    hover: {
                        mode: 'x',
                        intersect: false
                    },
                    scales: {
                        y: {
                            ticks: {
                                beginAtZero: true
                            },
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
                                autoSkip: true,
                                maxTicksLimit: 30,
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
            } as ChartConfiguration<"line", ScatterDataPoint[]>;

            this._chart = new Chart<"line", ScatterDataPoint[]>(this._chartElem.getContext("2d")!, options);
        } else {
            if (changedProperties.has("_data")) {
                this._chart.options.scales!.x!.min = this._startOfPeriod;
                this._chart.options!.scales!.x!.max = this._endOfPeriod;
                (this._chart.options!.scales!.x! as TimeScaleOptions).time!.unit = this._timeUnits!;
                (this._chart.options!.scales!.x! as TimeScaleOptions).time!.stepSize = this._stepSize!;
                (this._chart.options!.plugins!.annotation!.annotations! as AnnotationOptions<"line">[])[0].xMin = now;
                (this._chart.options!.plugins!.annotation!.annotations! as AnnotationOptions<"line">[])[0].xMax = now;
                this._chart.data.datasets = this._data;
                this._chart.update();
            }
        }
        this.onCompleted().then(() => {
            this.dispatchEvent(new OrChartEvent('rendered'));
        });

    }

    render() {
        const disabled = this._loading;
        const endDateInputType = this.getInputType();
        return html`
            <div id="container">
                <div id="chart-container">
                    <canvas id="chart"></canvas>
                </div>

                <div id="controls">
                    <div class="interval-controls" style="margin-right: 6px;">
                        ${getContentWithMenuTemplate(
                            html`<or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("timeframe")}: ${i18next.t(this.period ? this.period : "-")}"></or-mwc-input>`,
                            this._getPeriodOptions(),
                            this.period,
                            (value) => this.setPeriodOption(value))}

                        ${!!this.compareTimestamp ? html `
                                <or-mwc-input style="margin-left:auto;" .type="${InputType.BUTTON}" .label="${i18next.t("period")}" @click="${() => this.setPeriodCompare(false)}" icon="minus"></or-mwc-input>
                        ` : html`
                                <or-mwc-input style="margin-left:auto;" .type="${InputType.BUTTON}" .label="${i18next.t("period")}" @click="${() => this.setPeriodCompare(true)}" icon="plus"></or-mwc-input>
                        `}
                    </div>
                  
                    <div class="period-controls">

                        ${!!this.compareTimestamp ? html `
                            <span class="line-label solid"></span>
                        `: ``}
                        <or-mwc-input id="ending-date" 
                            .checkAssetWrite="${false}"
                            .type="${endDateInputType}" 
                            ?disabled="${disabled}" 
                            .value="${this.timestamp}" 
                            @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this._updateTimestamp(moment(evt.detail.value as string).toDate())}"></or-mwc-input>
                        <or-icon class="button-icon" icon="chevron-left" @click="${() => this._updateTimestamp(this.timestamp!, false, undefined, 0)}"></or-icon>
                        <or-icon class="button-icon" icon="chevron-right" @click="${() =>this._updateTimestamp(this.timestamp!, true, undefined, 0)}"></or-icon>
                    </div>
                    ${!!this.compareTimestamp ? html `
                        <div class="period-controls">
                        <span class="line-label dashed"></span>
                            <or-mwc-input id="ending-date" 
                                .checkAssetWrite="${false}"
                                .type="${endDateInputType}" 
                                ?disabled="${disabled}" 
                                .value="${this.compareTimestamp}" 
                                @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this._updateTimestamp(moment(evt.detail.value as string).toDate(), undefined, true)}"></or-mwc-input>
                            <or-icon class="button-icon" icon="chevron-left" @click="${() =>  this._updateTimestamp(this.compareTimestamp!, false, true, 0)}"></or-icon>
                            <or-icon class="button-icon" icon="chevron-right" @click="${() => this._updateTimestamp(this.compareTimestamp!, true, true, 0)}"></or-icon>
                        </div>
                    ` : html``}

                    <div id="attribute-list">
                        ${this.assetAttributes && this.assetAttributes.map(([assetIndex, attr], index) => {
                            const colourIndex = index % this.colors.length;
                            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.assets[assetIndex]!.type, attr.name, attr);
                            const label = Util.getAttributeLabel(attr, descriptors[0], this.assets[assetIndex]!.type, true);
                            const bgColor = this.colors[colourIndex] || "";
                            return html`
                                <div class="attribute-list-item" @mouseover="${()=> this.addDatasetHighlight(bgColor)}" @mouseout="${()=> this.removeDatasetHighlight(bgColor)}">
                                    <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[assetIndex]!.type!), undefined, undefined, bgColor.split('#')[1])}</span>
                                    <div class="attribute-list-item-label">
                                        <span>${this.assets[assetIndex].name}</span>
                                        <span style="font-size:14px; color:grey;">${label}</span>
                                    </div>
                                    <button class="button-clear" @click="${() => this._deleteAttribute(index)}"><or-icon icon="close-circle"></or-icon></button>
                                </div>
                            `
                        })}
                    </div>
                    <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" label="${i18next.t("selectAttributes")}" icon="plus" @click="${() => this._openDialog()}"></or-mwc-input>
                </div>
            </div>
        `;
    }

    protected async _onTreeSelectionChanged(event: OrAssetTreeSelectionEvent) {
        // Need to fully load the asset
        if (!manager.events) {
            return;
        }

        const selectedNode = event.detail && event.detail.newNodes.length > 0 ? event.detail.newNodes[0] : undefined;

        if (!selectedNode) {
            this.activeAsset = undefined;
        } else {
            // fully load the asset
            const assetEvent: AssetEvent = await manager.events.sendEventWithReply({
                event: {
                    eventType: "read-asset",
                    assetId: selectedNode.asset!.id
                } as ReadAssetEvent
            });
            this.activeAsset = assetEvent.asset;
        }
    }

    setPeriodOption(value:any) {
        this.period = value;

        this.saveSettings();
        this.requestUpdate();
    }
 
    getInputType() {
        switch (this.period) {
            case "hour":
                return InputType.DATETIME;
            case "day":
                return InputType.DATE;
            case "week":
                return InputType.WEEK;
            case "month":
                return InputType.MONTH;
            case "year":
                return InputType.MONTH;
          }
    }

    removeDatasetHighlight(bgColor:string) {
        if(this._chart && this._chart.data && this._chart.data.datasets){
            this._chart.data.datasets.map((dataset, idx) => {
                if (dataset.borderColor && typeof dataset.borderColor === "string" && dataset.borderColor.length === 9) {
                    dataset.borderColor = dataset.borderColor.slice(0, -2);
                    dataset.backgroundColor = dataset.borderColor;
                }
            });
            this._chart.update();
        }
    }

    addDatasetHighlight(bgColor:string) {

        if(this._chart && this._chart.data && this._chart.data.datasets){
            this._chart.data.datasets.map((dataset, idx) => {
                if (dataset.borderColor === bgColor) {
                    return
                }
                dataset.borderColor = dataset.borderColor + "36";
                dataset.backgroundColor = dataset.borderColor;
            });
            this._chart.update();
        }
    }

    async loadSettings() {

        this.assetAttributes = [];
        this.period = "day";
        this.timestamp = moment().set('minute', 0).toDate();
        this.compareTimestamp = undefined;

        if (!this.realm) {
            this.realm = manager.getRealm();
        }

        const configStr = await manager.console.retrieveData("OrChartConfig");

        if (!configStr || !this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        let allConfigs: OrChartConfig[] = [];
        let config: OrChartConfig;

        try {
            allConfigs = JSON.parse(configStr);
            if (!Array.isArray(allConfigs)) {
                manager.console.storeData("OrChartConfig", JSON.stringify([allConfigs]));
            }
            config = allConfigs.find(e => e.realm === this.realm) as OrChartConfig;
        } catch (e) {
            console.error("Failed to load chart config", e);
            manager.console.storeData("OrChartConfig", null);
            return;
        }

        const view = config && config.views && config.views[viewSelector] ? config.views[viewSelector][this.panelName] : undefined;

        if (!view) {
            return;
        }

        if (!view.attributeRefs) {
            // Old/invalid config format remove it
            delete config.views[viewSelector][this.panelName];
            const cleanData = [...allConfigs.filter(e => e.realm !== this.realm), config];
            manager.console.storeData("OrChartConfig", JSON.stringify(cleanData));
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

                allConfigs = [...allConfigs.filter(e => e.realm !== this.realm), config];
                manager.console.storeData("OrChartConfig", JSON.stringify(allConfigs));
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
                this.timestamp = new Date();
                this.compareTimestamp = view.compareOffset ? new Date(new Date().getTime() + view.compareOffset) : undefined;
            }
        }
    }

    async saveSettings() {

        if (!this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;        
        const configStr = await manager.console.retrieveData("OrChartConfig");
        let allConfigs: OrChartConfig[] = [];
        let config: OrChartConfig | undefined;

        if (configStr) {
            try {
                allConfigs = JSON.parse(configStr);
                config = allConfigs.find(e => e.realm === this.realm);
            } catch (e) {
                console.error("Failed to load chart config", e);
            }
        }
        
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
                compareOffset: this.timestamp && this.compareTimestamp ? this.compareTimestamp.getTime() - this.timestamp.getTime() : undefined
            };
        }

        allConfigs = [...allConfigs.filter(e => e.realm !== this.realm), config];
        manager.console.storeData("OrChartConfig", JSON.stringify(allConfigs));
    }
    
    _openDialog() {
        const dialog = showDialog(new OrAttributePicker()
            .setShowOnlyDatapointAttrs(true)
            .setMultiSelect(true)
            .setSelectedAttributes(this._getSelectedAttributes()));

        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (ev: any) => this._addAttribute(ev.detail));
    }

    protected async _addAttribute(selectedAttrs?: AttributeRef[]) {
        if (!selectedAttrs) return;

        this.assetAttributes = [];
        for (const attrRef of selectedAttrs) {
            const response = await manager.rest.api.AssetResource.get(attrRef.id!);
            this.activeAsset = response.data;
            if (this.activeAsset) {
                let assetIndex = this.assets.findIndex((asset) => asset.id === attrRef.id);
                if (assetIndex < 0) {
                    assetIndex = this.assets.length;
                    this.assets = [...this.assets, this.activeAsset];
                }
                this.assetAttributes.push([assetIndex, attrRef]);
            }
        }
        this.assetAttributes = [...this.assetAttributes];
        this.saveSettings();
    }

    protected _getSelectedAttributes() {
        return this.assetAttributes.map(([assetIndex, attr]) => {
            return {id: this.assets[assetIndex].id, name: attr.name};
        });
    }

    async onCompleted() {
        await this.updateComplete;
    }

    protected _cleanup() {
        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
    }

    protected _deleteAttribute (index: number) {
        const removed = this.assetAttributes.splice(index, 1)[0];
        const assetIndex = removed[0];
        this.assetAttributes = [...this.assetAttributes];
        if (!this.assetAttributes.some(([index, attrRef]) => index === assetIndex)) {
            // Asset no longer referenced
            this.assets.splice(index, 1);
            this.assetAttributes.forEach((indexRef) => {
                if (indexRef[0] >= assetIndex) {
                    indexRef[0] -= 1;
                }
            });
        }
        this.saveSettings();
    }

    protected _getAttributeOptionsOld(): [string, string][] | undefined {
        if(!this.activeAsset || !this.activeAsset.attributes) {
            return;
        }

        if(this.shadowRoot && this.shadowRoot.getElementById('chart-attribute-picker')) {
            const elm = this.shadowRoot.getElementById('chart-attribute-picker') as HTMLInputElement;
            elm.value = '';
        }

        const attributes = Object.values(this.activeAsset.attributes);
        if (attributes && attributes.length > 0) {
            return attributes
                .filter((attribute) => attribute.meta && (attribute.meta.hasOwnProperty(WellknownMetaItems.STOREDATAPOINTS) ? attribute.meta[WellknownMetaItems.STOREDATAPOINTS] : attribute.meta.hasOwnProperty(WellknownMetaItems.AGENTLINK)))
                .filter((attr) => (this.assetAttributes && !this.assetAttributes.some(([index, assetAttr]) => (assetAttr.name === attr.name) && this.assets[index].id === this.activeAsset!.id)))
                .map((attr) => {
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.activeAsset!.type, attr.name, attr);
                    const label = Util.getAttributeLabel(attr, descriptors[0], this.activeAsset!.type, false);
                    return [attr.name!, label];
                });
        }
    }

    protected _getAttributeOptions(): [string, string][] | undefined {
        if(!this.activeAsset || !this.activeAsset.attributes) {
            return;
        }

        if(this.shadowRoot && this.shadowRoot.getElementById('chart-attribute-picker')) {
            const elm = this.shadowRoot.getElementById('chart-attribute-picker') as HTMLInputElement;
            elm.value = '';
        }

        const attributes = Object.values(this.activeAsset.attributes);
        if (attributes && attributes.length > 0) {
            return attributes
                .filter((attribute) => attribute.meta && (attribute.meta.hasOwnProperty(WellknownMetaItems.STOREDATAPOINTS) ? attribute.meta[WellknownMetaItems.STOREDATAPOINTS] : attribute.meta.hasOwnProperty(WellknownMetaItems.AGENTLINK)))
                .filter((attr) => (this.assetAttributes && !this.assetAttributes.some(([index, assetAttr]) => (assetAttr.name === attr.name) && this.assets[index].id === this.activeAsset!.id)))
                .map((attr) => {
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.activeAsset!.type, attr.name, attr);
                    const label = Util.getAttributeLabel(attr, descriptors[0], this.activeAsset!.type, false);
                    return [attr.name!, label];
                });
        }
    }

    protected _getPeriodOptions() {
        return [
            {
                text: "hour",
                value: "hour"
            },
            {
                text: "day",
                value: "day"
            },
            {
                text:  "week",
                value: "week"
            },
            {
                text:  "month",
                value: "month"
            },
            {
                text: "year",
                value: "year"
            }
        ];
    }

    setPeriodCompare(periodCompare:boolean) {
        if (periodCompare) {
            this.compareTimestamp = this.timestamp;
        } else {
            this.compareTimestamp = undefined
        }

        this.saveSettings();
    }

    protected async _loadData() {

        if (this._loading || this._data || !this.assetAttributes || !this.assets || this.assets.length === 0 || this.assetAttributes.length === 0 || !this.period || !this.timestamp) {
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
        this._startOfPeriod = moment(this.timestamp).startOf(this.period).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
        this._endOfPeriod = moment(this.timestamp).endOf(this.period).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
        this._timeUnits =  lowerCaseInterval as TimeUnit;
        this._stepSize = stepSize;
        const now = moment().toDate().getTime();
        let predictedFromTimestamp = now < this._startOfPeriod ? this._startOfPeriod : now;

        const data: ChartDataset<"line", ScatterDataPoint[]>[] = [];
        const promises = this.assetAttributes.map(async ([assetIndex, attribute], index) => {

            const asset = this.assets[assetIndex];
            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
            const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
            const colourIndex = index % this.colors.length;
            let dataset = await this._loadAttributeData(asset, attribute, this.colors[colourIndex], interval, this._startOfPeriod!, this._endOfPeriod!, false, asset.name + " " + label);
            data.push(dataset);

            dataset =  await this._loadAttributeData(this.assets[assetIndex], attribute, this.colors[colourIndex], interval, predictedFromTimestamp, this._endOfPeriod!, true, asset.name + " " + label + " " + i18next.t("predicted"));
            data.push(dataset);

            if (this.compareTimestamp) {
                const startOfPeriod = moment(this.compareTimestamp).startOf(this.period!).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
                const endOfPeriod = moment(this.compareTimestamp).endOf(this.period!).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
                const offset =  this._startOfPeriod! - startOfPeriod;

                dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, this.colors[colourIndex], interval, startOfPeriod, endOfPeriod, false,  asset.name + " " + label + " " + i18next.t("compare"));
                dataset.data.forEach((dp) => dp.x += offset);
                dataset.borderDash = [10, 10];
                (dataset as any).isComparisonDataset = true;
                data.push(dataset);

                predictedFromTimestamp = now < startOfPeriod ? startOfPeriod : now;
                dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, this.colors[colourIndex], interval, startOfPeriod, endOfPeriod, true,  asset.name + " " + label + " " + i18next.t("compare") + " " + i18next.t("predicted"));
                dataset.data.forEach((dp) => dp.x += offset);
                dataset.borderDash = [6, 8];
                (dataset as any).isComparisonDataset = true;
                data.push(dataset);
            }
        });

        this._loading = false;
        await Promise.all(promises);
        this._data = data;
    }

    protected _timestampLabel(timestamp: Date | number | undefined) {
        let newMoment = moment.utc(timestamp).local();

        if(this.compareTimestamp) {
            const initialTimestamp = moment(this.timestamp);
            switch (this.period) {
                case "hour":
                    newMoment = moment.utc(timestamp).local();
                    break;
                case "day":
                    newMoment = moment.utc(timestamp).local().set('day', initialTimestamp.day());
                    break;
                case "week":
                    newMoment = moment.utc(timestamp).local().set('week', initialTimestamp.week());
                    break;
                case "month":
                    newMoment = moment.utc(timestamp).local().set('month', initialTimestamp.month());
                    break;
                case "year":
                    newMoment = moment.utc(timestamp).local().set('year', initialTimestamp.year());
                    break;
            }
        }

        return newMoment.format();
    }
    
    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, color: string | undefined, interval: DatapointInterval, from: number, to: number, predicted: boolean, label: string | undefined): Promise<ChartDataset<"line", ScatterDataPoint[]>> {

        const dataset: ChartDataset<"line", ScatterDataPoint[]> = {
            borderColor: color,
            backgroundColor: color,
            label: label,
            pointRadius: 2,
            fill: false,
            data: [],
            borderDash: predicted ? [2, 4] : undefined
        };

        if (asset.id && attribute.name) {
            const queryParams = {
                interval: interval,
                fromTimestamp: from,
                toTimestamp: to
            };

            let response: GenericAxiosResponse<ValueDatapoint<any>[]>;

            if (!predicted) {
                response = await manager.rest.api.AssetDatapointResource.getDatapoints(
                    asset.id,
                    attribute.name,
                    queryParams
                );
            } else {
                response = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(
                    asset.id,
                    attribute.name,
                    queryParams
                );
            }

            if (response.status === 200) {
                dataset.data = response.data.filter(value => value.y !== null && value.y !== undefined) as ScatterDataPoint[];
            }
        }

        return dataset;
    }

    protected _updateTimestamp(timestamp: Date, forward?: boolean, compare= false, timeout= 1500) {

        if (this._updateTimestampTimer) {
            window.clearTimeout(this._updateTimestampTimer);
            this._updateTimestampTimer = null;
        }
        this._updateTimestampTimer = window.setTimeout(() => {
                const newMoment = moment(timestamp);

                if (forward !== undefined) {
                    newMoment.add(forward ? 1 : -1, this.period);
                }
                if (compare) {
                    this.compareTimestamp = newMoment.toDate()
                } else {
                    this.timestamp = newMoment.toDate()
                }
                this.saveSettings();
        }, timeout);
    }
        
}
