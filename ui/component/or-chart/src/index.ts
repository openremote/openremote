import {
    css,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    query,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {Asset, Attribute, AttributeRef, DatapointInterval, WellknownMetaItems, ReadAssetEvent, AssetEvent, ValueDatapoint} from "@openremote/model";
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
import "@openremote/or-panel";
import {MDCDialog} from '@material/dialog';
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
    assetIds?: string[];
    attributes?: string[];
    timestamp?: number;
    compareTimestamp?: number;
    period?: moment.unitOfTime.Base;
    deltaFormat?: string;
    decimals?: number;
    periodCompare?: boolean;
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
    views: {[name: string]: {
        [panelName: string]: ChartViewConfig
    }};
}
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

    @property({type: String})
    public activeAssetId?:string;

    @property({type: Object})
    private activeAsset?: Asset;

    @property({type: Object})
    public assetAttributes: Attribute<any>[] = [];

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: Array})
    public colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    @property({type: String})
    public period?: moment.unitOfTime.Base = "day";

    @property({type: Number})
    public timestamp?: Date = moment().set('minute', 0).toDate();

    @property({type: Number})
    public compareTimestamp?: Date = moment().set('minute', 0).toDate();

    @property({type: Object})
    public config?: OrChartConfig;

    @property()
    public panelName?: string;

    @property()
    protected periodCompare: boolean = false;

    @property()
    protected _loading: boolean = false;

    @property()
    protected _data?: ChartDataset<"line", ScatterDataPoint[]>[] = undefined;

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLCanvasElement;

    protected _chart?: Chart;

    @query("#mdc-dialog")
    protected _dialogElem!: HTMLElement;

    protected _dialog!: MDCDialog;
    protected _style!: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _timeUnits?: TimeUnit;
    protected _stepSize?: number;
    protected _updateTimestampTimer: number | null = null;
    
    firstUpdated() {
        if (this._dialogElem) {
            this._dialog = new MDCDialog(this._dialogElem);
            if(!this.activeAssetId) {
                this.getSettings();
            }
        }
    }

    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionEvent) {
        // Need to fully load the asset
        if (!manager.events) {
            return;
        }

        const selectedNode = event.detail && event.detail.newNodes.length > 0 ? event.detail.newNodes[0] : undefined;

        if (!selectedNode) {
            this.activeAsset = undefined;
        } else {
            // fully load the asset
            manager.events.sendEventWithReply({
                event: {
                    eventType: "read-asset",
                    assetId: selectedNode.asset!.id
                } as ReadAssetEvent
            }).then((ev) => {
                this.activeAsset = (ev as AssetEvent).asset;
            }).catch(() => this.activeAsset = undefined);
        }
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
        this.addEventListener(OrAssetTreeSelectionEvent.NAME, this._onTreeSelectionChanged);
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
        this.removeEventListener(OrAssetTreeSelectionEvent.NAME, this._onTreeSelectionChanged);
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

                        ${this.periodCompare ? html `
                                <or-mwc-input style="margin-left:auto;" .type="${InputType.BUTTON}" .label="${i18next.t("period")}" @click="${() => this.setPeriodCompare(false)}" icon="minus"></or-mwc-input>
                        ` : html`
                                <or-mwc-input style="margin-left:auto;" .type="${InputType.BUTTON}" .label="${i18next.t("period")}" @click="${() => this.setPeriodCompare(true)}" icon="plus"></or-mwc-input>
                        `}
                    </div>
                  
                    <div class="period-controls">

                        ${this.periodCompare ? html `
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
                    ${this.periodCompare ? html `
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
                        ${this.assetAttributes && this.assetAttributes.map((attr, index) => {

                            const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attr.name!, this.assets[index]!.type!);
                            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.assets[index]!.type, attr.name, attr);
                            const label = Util.getAttributeLabel(attr, descriptors[0], this.assets[index]!.type, true);
                            const bgColor = Util.getMetaValue('color', attr, undefined) ? Util.getMetaValue('color', attr, undefined) : "";
                            return html`
                                <div class="attribute-list-item" @mouseover="${()=> this.addDatasetHighlight(bgColor)}" @mouseout="${()=> this.removeDatasetHighlight(bgColor)}">
                                    <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[index]!.type!), undefined, undefined, bgColor.split('#')[1])}</span>
                                    <div class="attribute-list-item-label">
                                        <span>${this.assets[index].name}</span>
                                        <span style="font-size:14px; color:grey;">${label}</span>
                                    </div>
                                    <button class="button-clear" @click="${() => this._deleteAttribute(index)}"><or-icon icon="close-circle"></or-icon></button>
                                </div>
                            `
                        })}
                    </div>
                    <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-mwc-input>

                </div>
            </div>
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
                        <or-asset-tree id="chart-asset-tree" readonly .selectedIds="${this.activeAsset ? [this.activeAsset.id] : undefined}"></or-asset-tree>
                            ${this.activeAsset && this.activeAsset.attributes ? html`
                                <or-mwc-input id="chart-attribute-picker" 
                                        style="display:flex;"
                                        .label="${i18next.t("attribute")}" 
                                        .type="${InputType.LIST}"
                                        .options="${this._getAttributeOptions()}"></or-mwc-input>
                            `:``}
                    </div>
                    <footer class="mdc-dialog__actions">
                        <or-mwc-input class="button" 
                                slot="secondaryAction"
                                .type="${InputType.BUTTON}" 
                                label="${i18next.t("cancel")}" 
                                class="mdc-button mdc-dialog__button" 
                                data-mdc-dialog-action="no"></or-mwc-input>

                        <or-mwc-input class="button" 
                            slot="primaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("add")}" 
                            class="mdc-button mdc-dialog__button" 
                            data-mdc-dialog-action="yes"
                            @click="${this.addAttribute}"></or-mwc-input>

                    </footer>
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
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

    updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);
        if (!this._loading && changedProperties.has("activeAsset") && this.activeAsset && changedProperties.get("activeAsset") !== this.activeAsset) {
            this.getSettings();
        }

        if(changedProperties.has("assetAttributes") ) {
            this.assetAttributes.forEach((attr, index) => {
                if(this.getAttrColor(attr)) return;
                this.setAttrColor(attr)
            });
        }

        const reloadData = changedProperties.has("period") || changedProperties.has("compareTimestamp") || changedProperties.has("timestamp") || changedProperties.has("assetAttributes");

        if (reloadData) {
            this._data = undefined;
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
                (this._chart.options!.plugins!.annotation!.annotations![0] as AnnotationOptions<"line">).xMin = now;
                (this._chart.options!.plugins!.annotation!.annotations![0] as AnnotationOptions<"line">).xMax = now;
                this._chart.data.datasets = this._data;
                this._chart.update();
            }
        }
        this.onCompleted().then(() => {
            this.dispatchEvent(new OrChartEvent('rendered'));
        });

    }

    async getSettings() {
        const configStr = await manager.console.retrieveData("OrChartConfig");

        if(!configStr || !this.panelName) {
            return;
        }

        const viewSelector = this.activeAssetId ? this.activeAssetId : window.location.hash;
        const config = JSON.parse(configStr) as OrChartConfig;
        const view = config.views[viewSelector][this.panelName];
        if (!view) {
            return;
        }
        const query = {
            ids: view.assetIds
        }
        if (view.assetIds && this.assets && view.assetIds.every(id => !!this.assets.find(asset => asset.id === id))) return;
        this._loading = true;

        manager.rest.api.AssetResource.queryAssets(query).then((response) => {
            this._loading = false;
            const assets = response.data;
            if (!view || !view.assetIds || !view.attributes || assets.length !== view.assetIds.length || assets.length !== view.attributes.length) return;

            this.assets = view.assetIds.map((assetId: string)  => assets.find(x => x.id === assetId)!);
            this.assetAttributes = view.attributes.map((attr: string, index: number) => assets[index] && assets[index].attributes ? assets[index].attributes![attr] : undefined).filter(attr => !!attr) as Attribute<any>[];
            this.period = view.period;
            this.timestamp = view.timestamp ? new Date(view.timestamp) : new Date();
            this.compareTimestamp = view.compareTimestamp ? new Date(view.compareTimestamp) : new Date();
            this.periodCompare = !!view.periodCompare;
        });
    }

    async saveSettings() {
        if(!this.panelName) {
            return;
        }
        const viewSelector = this.activeAssetId ? this.activeAssetId : window.location.hash;
        const assets: Asset[] = this.assets ? this.assets.filter(asset => !!asset.id) : [];
        const assetIds = assets.map(asset => asset.id!);
        const attributes = this.assetAttributes ? this.assetAttributes.map(attr => attr.name!) : [];
        const configStr = await manager.console.retrieveData("OrChartConfig");

        let config:OrChartConfig;
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
            periodCompare: this.periodCompare,
            timestamp: this.timestamp ? this.timestamp.getTime() : undefined,
            compareTimestamp: this.compareTimestamp ? this.compareTimestamp.getTime() : undefined
        };
        manager.console.storeData("OrChartConfig", JSON.stringify(config));
    }
    
    _openDialog() {
        const component = this.shadowRoot!.getElementById("mdc-dialog");
        if(component){
            const dialog = new MDCDialog(component);
            if(dialog){
                dialog.open();
            }
        }
    }

    addAttribute(e:Event) {
        if (this.shadowRoot && this.shadowRoot.getElementById('chart-attribute-picker')) {
            const elm = this.shadowRoot.getElementById('chart-attribute-picker') as HTMLInputElement;
            if (this.activeAsset) {
                const attr = this.activeAsset.attributes ? this.activeAsset.attributes[elm.value] : undefined;
                if (attr) {
                    this.setAttrColor(attr);
                    this.assetAttributes = [...this.assetAttributes, attr];

                    this.assets = [...this.assets, this.activeAsset];
                    this.saveSettings();
                }
            }
        }
    }
    
    setAttrColor(attr: Attribute<any>) {
        const usedColors = this.assetAttributes.map(attr => this.getAttrColor(attr));
        const color = this.colors.filter(color => usedColors.indexOf(color) < 0)[0];
        if (attr.meta) {
            attr.meta["color"] = color;
        }
    }
    
    getAttrColor(attr: Attribute<any>) {
        return  Util.getMetaValue('color', attr, undefined);
    }

    async onCompleted() {
        await this.updateComplete;
    }

    protected _cleanup() {
        if (this._chart) {
            this._chart.destroy();
            this._chart = undefined;
        }
        if (this._dialog) {
            this._dialog.destroy();
        }
    }

    protected _deleteAttribute (index:number) {
        this.assets = [...this.assets.slice(0, index).concat(this.assets.slice(index + 1, this.assets.length))];
        this.assetAttributes = [...this.assetAttributes.slice(0, index).concat(this.assetAttributes.slice(index + 1, this.assetAttributes.length))];

        this.saveSettings();
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
                .filter((attr) => (this.assetAttributes && !this.assetAttributes.some((assetAttr, index) => (assetAttr.name === attr.name) && this.assets[index].id === this.activeAsset!.id)))
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
        this.periodCompare = periodCompare;
        this.saveSettings();

        if (periodCompare) {
            this._data = undefined;
            this._loadData();
        } else {
            this._data = this._data ? this._data.map((dataset) => (dataset as any).isComparisonDataset ? undefined : dataset).filter((dataset) => !!dataset) as ChartDataset<"line", ScatterDataPoint[]>[] : undefined;
        }
    }

    protected async _loadData() {
        if(this._loading || this._data || !this.assetAttributes || !this.assets || this.assets.length === 0 || this.assetAttributes.length !== this.assets.length || !this.period || !this.timestamp) {
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
        const promises = this.assetAttributes.map(async (attribute, index) => {

            const asset = this.assets[index];
            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
            const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);

            let dataset = await this._loadAttributeData(asset, attribute, interval, this._startOfPeriod!, this._endOfPeriod!, false, asset.name + " " + label);
            data.push(dataset);

            dataset =  await this._loadAttributeData(this.assets[index], attribute, interval, predictedFromTimestamp, this._endOfPeriod!, true, asset.name + " " + label + " " + i18next.t("predicted"));
            data.push(dataset);

            if (this.periodCompare) {
                const startOfPeriod = moment(this.compareTimestamp).startOf(this.period!).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
                const endOfPeriod = moment(this.compareTimestamp).endOf(this.period!).startOf(lowerCaseInterval as moment.unitOfTime.StartOf).add(1, lowerCaseInterval as moment.unitOfTime.Base).toDate().getTime();
                const offset =  this._startOfPeriod! - startOfPeriod;

                dataset = await this._loadAttributeData(this.assets[index], attribute, interval, startOfPeriod, endOfPeriod, false,  asset.name + " " + label + " " + i18next.t("compare"));
                dataset.data.forEach((dp) => dp.x += offset);
                dataset.borderDash = [10, 10];
                (dataset as any).isComparisonDataset = true;
                data.push(dataset);

                predictedFromTimestamp = now < startOfPeriod ? startOfPeriod : now;
                dataset = await this._loadAttributeData(this.assets[index], attribute, interval, startOfPeriod, endOfPeriod, true,  asset.name + " " + label + " " + i18next.t("compare") + " " + i18next.t("predicted"));
                dataset.data.forEach((dp) => dp.x += offset);
                dataset.borderDash = [6, 8];
                (dataset as any).isComparisonDataset = true;
                data.push(dataset);
            }
        });

        await Promise.all(promises);
        this._data = data;
    }

    protected _timestampLabel(timestamp: Date | number | undefined) {
        let newMoment = moment.utc(timestamp).local();

        if(this.periodCompare) {
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
    
    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, interval: DatapointInterval, from: number, to: number, predicted: boolean, label: string | undefined): Promise<ChartDataset<"line", ScatterDataPoint[]>> {

        const bgColor = this.getAttrColor(attribute);

        const dataset: ChartDataset<"line", ScatterDataPoint[]> = {
            label: label,
            borderColor: bgColor,
            pointRadius: 2,
            backgroundColor: bgColor,
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

            this._loading = false;

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
                    this.saveSettings();
                } else {
                    this.timestamp = newMoment.toDate()
                }
        }, timeout);
    }
        
}
