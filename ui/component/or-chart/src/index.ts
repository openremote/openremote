import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {
    Asset,
    AssetDatapointQueryUnion,
    AssetEvent,
    AssetModelUtil,
    AssetQuery,
    Attribute,
    AttributeRef,
    DatapointInterval,
    ReadAssetEvent,
    ValueDatapoint,
    WellknownMetaItems
} from "@openremote/model";
import manager, {DefaultColor2, DefaultColor3, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import "@openremote/or-asset-tree";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {
    Chart,
    ChartConfiguration,
    ChartDataset,
    Filler,
    Legend,
    LinearScale,
    LineController,
    LineElement,
    PointElement,
    ScatterController,
    ScatterDataPoint,
    TimeScale,
    TimeScaleOptions,
    TimeUnit,
    Title,
    Tooltip
} from "chart.js";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-loading-indicator";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import ChartAnnotation, {AnnotationOptions} from "chartjs-plugin-annotation";
import "chartjs-adapter-moment";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {cache} from "lit/directives/cache.js";
import {throttle} from "lodash";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import { when } from "lit/directives/when.js";
import {createRef, Ref, ref } from "lit/directives/ref.js";

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

export type TimePresetCallback = (date: Date) => [Date, Date];

export interface ChartViewConfig {
    attributeRefs?: AttributeRef[];
    fromTimestamp?: number;
    toTimestamp?: number;
    /*compareOffset?: number;*/
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
        height: 100%;
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
    .period-controls {
        display: flex;
        min-width: 180px;
        align-items: center;
    }

    #controls {
        display: flex;
        flex-wrap: wrap;
        margin: var(--internal-or-chart-controls-margin);
        width: 100%;
        flex-direction: column;
        margin: 0;
    }

    #attribute-list {
        overflow: hidden auto;
        min-height: 50px;
        flex: 1 1 0;
        width: 100%;
        display: flex;
        flex-direction: column;
    }
    .attribute-list-dense {
        flex-wrap: wrap;
    }
    
    .attribute-list-item {
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
        min-height: 50px;
    }
    .attribute-list-item-dense {
        min-height: 28px;
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
        width: 12px;
        height: 12px;
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
        overflow: hidden;
        /*min-height: 400px;
        max-height: 550px;*/
    }
    #chart-controls {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    canvas {
        width: 100% !important;
        height: 100%; !important;
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
        .interval-controls,
        .period-controls {
            flex-direction: row;
            justify-content: left;
            align-items: center;
            gap: 8px;
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

    @property({type: Array}) // List of AttributeRef that are shown on the right axis instead.
    public rightAxisAttributes: AttributeRef[] = [];

    @property()
    public dataProvider?: (startOfPeriod: number, endOfPeriod: number, timeUnits: TimeUnit, stepSize: number) => Promise<ChartDataset<"line", ScatterDataPoint[]>[]>

    @property({type: Array})
    public colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    @property({type: Object})
    public readonly datapointQuery!: AssetDatapointQueryUnion;

    @property({type: Object})
    public config?: OrChartConfig;

    @property({type: Object}) // options that will get merged with our default chartjs configuration.
    public chartOptions?: any

    @property({type: String})
    public realm?: string;

    @property()
    public panelName?: string;

    @property()
    public attributeControls: boolean = true;

    @property()
    public timeframe?: [Date, Date];

    @property()
    public timestampControls: boolean = true;

    @property()
    public timePresetOptions?: Map<string, TimePresetCallback>;

    @property()
    public timePresetKey?: string;

    @property()
    public showLegend: boolean = true;

    @property()
    public denseLegend: boolean = false;

    @property()
    protected _loading: boolean = false;

    @property()
    protected _data?: ChartDataset<"line", ScatterDataPoint[]>[] = undefined;

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLCanvasElement;

    protected _chart?: Chart;
    protected _style!: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _timeUnits?: TimeUnit;
    protected _stepSize?: number;
    protected _latestError?: string;
    protected _dataAbortController?: AbortController;

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
        this.loadSettings(false);
    }

    updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        if (changedProperties.has("realm")) {
            if(changedProperties.get("realm") != undefined) { // Checking whether it was undefined previously, to prevent loading 2 times and resetting attribute properties.
                this.assets = [];
                this.loadSettings(true);
            }
        }

        const reloadData = changedProperties.has("datapointQuery") || changedProperties.has("timePresetKey") || changedProperties.has("timeframe") ||
            changedProperties.has("rightAxisAttributes") || changedProperties.has("assetAttributes") || changedProperties.has("realm") || changedProperties.has("dataProvider");

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
                    onResize: throttle(() => { this.dispatchEvent(new OrChartEvent("resize")); this.applyChartResponsiveness(); }, 200),
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
                            titleMarginBottom: 10,
                            callbacks: {
                                label: (tooltipItem: any) => tooltipItem.dataset.label + ': ' + tooltipItem.formattedValue + tooltipItem.dataset.unit,
                            }
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
                        y1: {
                            display: this.rightAxisAttributes.length > 0,
                            position: 'right',
                            ticks: {
                                beginAtZero: true
                            },
                            grid: {
                                drawOnChartArea: false
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
                                    hour: (this._endOfPeriod && this._startOfPeriod && this._endOfPeriod - this._startOfPeriod > 86400000) ? "MMM DD, HH:mm" : "HH:mm",
                                    day: "MMM DD",
                                    week: "w"
                                },
                                unit: this._timeUnits,
                                stepSize: this._stepSize
                            },
                            ticks: {
                                autoSkip: true,
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

            const mergedOptions = Util.mergeObjects(options, this.chartOptions, false);

            this._chart = new Chart<"line", ScatterDataPoint[]>(this._chartElem.getContext("2d")!, mergedOptions as ChartConfiguration<"line", ScatterDataPoint[]>);
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

    // Not the best implementation, but it changes the legend & controls to wrap under the chart.
    // Also sorts the attribute lists horizontally when it is below the chart
    applyChartResponsiveness(): void {
        if(this.shadowRoot) {
            const container = this.shadowRoot.getElementById('container');
            if(container) {
                const bottomLegend: boolean = (container.clientWidth < 600);
                container.style.flexDirection = bottomLegend ? 'column' : 'row';
                const periodControls = this.shadowRoot.querySelector('.period-controls') as HTMLElement;
                if(periodControls) {
                    periodControls.style.justifyContent = bottomLegend ? 'center' : 'space-between';
                    periodControls.style.paddingLeft = bottomLegend ? '' : '18px';
                }
                const attributeList = this.shadowRoot.getElementById('attribute-list');
                if(attributeList) {
                    attributeList.style.gap = bottomLegend ? '4px 12px' : '';
                    attributeList.style.maxHeight = bottomLegend ? '90px' : '';
                    attributeList.style.flexFlow = bottomLegend ? 'row wrap' : 'column nowrap';
                    attributeList.style.padding = bottomLegend ? '0' : '12px 0';
                }
                this.shadowRoot.querySelectorAll('.attribute-list-item').forEach((item: Element) => {
                    (item as HTMLElement).style.minHeight = bottomLegend ? '0px' : '44px';
                    (item as HTMLElement).style.paddingLeft = bottomLegend ? '' : '16px';
                    (item.children[1] as HTMLElement).style.flexDirection = bottomLegend ? 'row' : 'column';
                    (item.children[1] as HTMLElement).style.gap = bottomLegend ? '4px' : '';
                });
            }
        }
    }

    render() {
        const disabled = this._loading || this._latestError;
        return html`
            <div id="container">
                <div id="chart-container">
                    ${when(this._loading, () => html`
                        <div style="position: absolute; height: 100%; width: 100%;">
                            <or-loading-indicator ?overlay="false"></or-loading-indicator>
                        </div>
                    `)}
                    ${when(this._latestError, () => html`
                        <div style="position: absolute; height: 100%; width: 100%; display: flex; justify-content: center; align-items: center;">
                            <or-translate .value="${this._latestError || 'errorOccurred'}"></or-translate>
                        </div>
                    `)}
                    <canvas id="chart" style="visibility: ${disabled ? 'hidden' : 'visible'}"></canvas>
                </div>
                
                ${(this.timestampControls || this.attributeControls || this.showLegend) ? html`
                    <div id="chart-controls">
                        <div id="controls">
                            <div class="period-controls">
                                ${this.timePresetOptions && this.timePresetKey ? html`
                                    ${this.timestampControls ? html`
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input .type="${InputType.BUTTON}" label="${this.timeframe ? "dashboard.customTimeSpan" : this.timePresetKey}"></or-mwc-input>`,
                                                Array.from(this.timePresetOptions!.keys()).map((key) => ({ value: key } as ListItem)),
                                                this.timePresetKey,
                                                (value: string | string[]) => {
                                                    this.timeframe = undefined; // remove any custom start & end times
                                                    this.timePresetKey = value.toString();
                                                },
                                                undefined,
                                                undefined,
                                                undefined,
                                                true
                                        )}
                                        <!-- Button that opens custom time selection -->
                                        <or-mwc-input .type="${InputType.BUTTON}" icon="calendar-clock" @or-mwc-input-changed="${() => this._openTimeDialog(this._startOfPeriod, this._endOfPeriod)}"></or-mwc-input>
                                    ` : html`
                                        <or-mwc-input .type="${InputType.BUTTON}" label="${this.timePresetKey}" disabled="true"></or-mwc-input>
                                    `}
                                ` : undefined}
                            </div>
                            ${this.timeframe ? html`
                                <div style="margin-left: 18px; font-size: 12px;">
                                    <table style="width: 100%;">
                                        <thead>
                                        <tr>
                                            <th style="font-weight: normal; text-align: left;">${i18next.t('from')}:</th>
                                            <th style="font-weight: normal; text-align: left;">${moment(this.timeframe[0]).format("L HH:mm")}</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <tr>
                                            <td>${i18next.t('to')}:</td>
                                            <td>${moment(this.timeframe[1]).format("L HH:mm")}</td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </div>
                            ` : undefined}
                            ${this.attributeControls ? html`
                                <or-mwc-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" label="selectAttributes" icon="plus" @or-mwc-input-changed="${() => this._openDialog()}"></or-mwc-input>
                            ` : undefined}
                        </div>
                        ${cache(this.showLegend ? html`
                            <div id="attribute-list" class="${this.denseLegend ? 'attribute-list-dense' : undefined}">
                                ${this.assetAttributes == null || this.assetAttributes.length == 0 ? html`
                                    <div>
                                        <span>${i18next.t('noAttributesConnected')}</span>
                                    </div>
                                ` : undefined}
                                ${this.assetAttributes && this.assetAttributes.map(([assetIndex, attr], index) => {
                                    const asset: Asset | undefined = this.assets[assetIndex];
                                    const colourIndex = index % this.colors.length;
                                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset!.type, attr.name, attr);
                                    const label = Util.getAttributeLabel(attr, descriptors[0], asset!.type, true);
                                    const axisNote = (this.rightAxisAttributes.find(ar => asset!.id === ar.id && attr.name === ar.name)) ? i18next.t('right') : undefined;
                                    const bgColor = this.colors[colourIndex] || "";
                                    return html`
                                        <div class="attribute-list-item ${this.denseLegend ? 'attribute-list-item-dense' : undefined}" @mouseover="${()=> this.addDatasetHighlight(this.assets[assetIndex]!.id, attr.name)}" @mouseout="${()=> this.removeDatasetHighlight(bgColor)}">
                                            <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[assetIndex]!.type!), undefined, undefined, bgColor.split('#')[1])}</span>
                                            <div class="attribute-list-item-label ${this.denseLegend ? 'attribute-list-item-label-dense' : undefined}">
                                                <div style="display: flex; justify-content: space-between;">
                                                    <span style="font-size:12px; ${this.denseLegend ? 'margin-right: 8px' : undefined}">${this.assets[assetIndex].name}</span>
                                                    ${when(axisNote, () => html`<span style="font-size:12px; color:grey">(${axisNote})</span>`)}
                                                </div>
                                                <span style="font-size:12px; color:grey;">${label}</span>
                                            </div>
                                        </div>
                                    `
                                })}
                            </div>
                        ` : undefined)}
                    </div>
                ` : undefined}
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
                eventType: "read-asset",
                assetId: selectedNode.asset!.id
            } as ReadAssetEvent);
            this.activeAsset = assetEvent.asset;
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

    addDatasetHighlight(assetId?:string, attrName?:string) {
        if (!assetId || !attrName) return;

        if(this._chart && this._chart.data && this._chart.data.datasets){
            this._chart.data.datasets.map((dataset, idx) => {
                if ((dataset as any).assetId === assetId && (dataset as any).attrName === attrName) {
                    return
                }
                dataset.borderColor = dataset.borderColor + "36";
                dataset.backgroundColor = dataset.borderColor;
            });
            this._chart.update();
        }
    }

    async loadSettings(reset: boolean) {

        if(this.assetAttributes == undefined || reset) {
            this.assetAttributes = [];
        }

        if (!this.realm) {
            this.realm = manager.getRealm();
        }

        if (!this.timePresetOptions) {
            this.timePresetOptions = this._getDefaultTimestampOptions();
        }
        if (!this.timePresetKey) {
            this.timePresetKey = this.timePresetOptions.keys().next().value?.toString();
        }

        if (!this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        const allConfigs: OrChartConfig[] = await manager.console.retrieveData("OrChartConfig") || [];

        if (!Array.isArray(allConfigs)) {
            manager.console.storeData("OrChartConfig", [allConfigs]);
        }

        let config: OrChartConfig | undefined = allConfigs.find(e => e.realm === this.realm);

        if (!config) {
            return;
        }

        const view = config.views && config.views[viewSelector] ? config.views[viewSelector][this.panelName] : undefined;

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

                manager.console.storeData("OrChartConfig", [...allConfigs.filter(e => e.realm !== this.realm), config]);
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
            }
        }
    }

    async saveSettings() {

        if (!this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        const allConfigs: OrChartConfig[] = await manager.console.retrieveData("OrChartConfig") || [];
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
            };
        }

        manager.console.storeData("OrChartConfig", [...allConfigs.filter(e => e.realm !== this.realm), config]);
    }

    protected _openDialog() {
        const dialog = showDialog(new OrAttributePicker()
            .setShowOnlyDatapointAttrs(true)
            .setMultiSelect(true)
            .setSelectedAttributes(this._getSelectedAttributes()));

        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (ev: any) => this._addAttribute(ev.detail));
    }

    protected _openTimeDialog(startTimestamp?: number, endTimestamp?: number) {
        const startRef: Ref<OrMwcInput> = createRef();
        const endRef: Ref<OrMwcInput> = createRef();
        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t('timeframe'))
            .setContent(() => html`
                <div>
                    <or-mwc-input ${ref(startRef)} type="${InputType.DATETIME}" required label="${i18next.t('start')}" .value="${startTimestamp}"></or-mwc-input>
                    <or-mwc-input ${ref(endRef)} type="${InputType.DATETIME}" required label="${i18next.t('ending')}" .value="${endTimestamp}"></or-mwc-input>
                </div>
            `)
            .setActions([{
                actionName: "cancel",
                content: "cancel"
            }, {
                actionName: "ok",
                content: "ok",
                action: () => {
                    if(this.timePresetOptions && startRef.value?.value && endRef.value?.value) {
                        this.timeframe = [new Date(startRef.value.value), new Date(endRef.value.value)];
                    }
                }
            }])
        )
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
            this.requestUpdate();
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

    protected _getDefaultTimestampOptions(): Map<string, TimePresetCallback> {
        return new Map<string, TimePresetCallback>([
            ["lastHour", (date) => [moment(date).subtract(1, 'hour').toDate(), date]],
            ["last24Hours", (date) => [moment(date).subtract(24, 'hours').toDate(), date]],
            ["last7Days", (date) => [moment(date).subtract(7, 'days').toDate(), date]],
            ["last30Days", (date) => [moment(date).subtract(30, 'days').toDate(), date]],
            ["last90Days", (date) => [moment(date).subtract(90, 'days').toDate(), date]],
            ["last6Months", (date) => [moment(date).subtract(6, 'months').toDate(), date]],
            ["lastYear", (date) => [moment(date).subtract(1, 'year').toDate(), date]]
        ]);
    }

    protected _getInterval(diffInHours: number): [number, DatapointInterval] {

        if(diffInHours <= 1) {
            return [5, DatapointInterval.MINUTE];
        } else if(diffInHours <= 3) {
            return [10, DatapointInterval.MINUTE];
        } else if(diffInHours <= 6) {
            return [30, DatapointInterval.MINUTE];
        } else if(diffInHours <= 24) { // one day
            return [1, DatapointInterval.HOUR];
        } else if(diffInHours <= 48) { // two days
            return [3, DatapointInterval.HOUR];
        } else if(diffInHours <= 96) {
            return [12, DatapointInterval.HOUR];
        } else if(diffInHours <= 744) { // one month
            return [1, DatapointInterval.DAY];
        } else {
            return [1, DatapointInterval.MONTH];
        }
    }

    protected async _loadData() {
        if (this._data || !this.assetAttributes || !this.assets || (this.assets.length === 0 && !this.dataProvider) || (this.assetAttributes.length === 0 && !this.dataProvider) || !this.datapointQuery) {
            return;
        }

        if(this._loading) {
            if(this._dataAbortController) {
                this._dataAbortController.abort("Data request overridden");
                delete this._dataAbortController;
            } else {
                return;
            }
        }

        this._loading = true;

        const dates: [Date, Date] = this.timePresetOptions!.get(this.timePresetKey!)!(new Date());
        this._startOfPeriod = this.timeframe ? this.timeframe[0].getTime() : dates[0].getTime();
        this._endOfPeriod = this.timeframe ? this.timeframe[1].getTime() : dates[1].getTime();

        const diffInHours = (this._endOfPeriod - this._startOfPeriod) / 1000 / 60 / 60;
        const intervalArr = this._getInterval(diffInHours);

        const stepSize: number = intervalArr[0];
        const interval: DatapointInterval = intervalArr[1];

        const lowerCaseInterval = interval.toLowerCase();
        this._timeUnits =  lowerCaseInterval as TimeUnit;
        this._stepSize = stepSize;
        const now = moment().toDate().getTime();
        let predictedFromTimestamp = now < this._startOfPeriod ? this._startOfPeriod : now;

        const data: ChartDataset<"line", ScatterDataPoint[]>[] = [];
        let promises;

        try {
            if(this.dataProvider) {
                await this.dataProvider(this._startOfPeriod, this._endOfPeriod, (interval.toString() as TimeUnit), stepSize).then((dataset) => {
                    dataset.forEach((set) => { data.push(set); });
                });
            } else {
                this._dataAbortController = new AbortController();
                promises = this.assetAttributes.map(async ([assetIndex, attribute], index) => {

                    const asset = this.assets[assetIndex];
                    const shownOnRightAxis = !!this.rightAxisAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
                    const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                    const unit = Util.resolveUnits(Util.getAttributeUnits(attribute, descriptors[0], asset.type));
                    const colourIndex = index % this.colors.length;
                    const options = { signal: this._dataAbortController?.signal };
                    let dataset = await this._loadAttributeData(asset, attribute, this.colors[colourIndex], this._startOfPeriod!, this._endOfPeriod!, false, asset.name + " " + label, options);
                    (dataset as any).assetId = asset.id;
                    (dataset as any).attrName = attribute.name;
                    (dataset as any).unit = unit;
                    (dataset as any).yAxisID = shownOnRightAxis ? 'y1' : 'y';
                    data.push(dataset);

                    dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, this.colors[colourIndex], predictedFromTimestamp, this._endOfPeriod!, true, asset.name + " " + label + " " + i18next.t("predicted"), options);
                    (dataset as any).unit = unit;
                    data.push(dataset);
                });
            }

            if(promises) {
                await Promise.all(promises);
            }

            this._data = data;
            this._loading = false;

        } catch (ex) {
            console.error(ex);
            if((ex as Error)?.message === "canceled") {
                return; // If request has been canceled (using AbortController); return, and prevent _loading is set to false.
            }
            this._loading = false;

            if(isAxiosError(ex)) {
                if(ex.message.includes("timeout")) {
                    this._latestError = "noAttributeDataTimeout";
                    return;
                } else if(ex.response?.status === 413) {
                    this._latestError = "datapointRequestTooLarge";
                    return;
                }
            }
            this._latestError = "errorOccurred";
        }
    }


    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, color: string | undefined, from: number, to: number, predicted: boolean, label?: string, options?: any): Promise<ChartDataset<"line", ScatterDataPoint[]>> {

        const dataset: ChartDataset<"line", ScatterDataPoint[]> = {
            borderColor: color,
            backgroundColor: color,
            label: label,
            pointRadius: 2,
            fill: false,
            data: [],
            borderDash: predicted ? [2, 4] : undefined
        };

        if (asset.id && attribute.name && this.datapointQuery) {
            let response: GenericAxiosResponse<ValueDatapoint<any>[]>;
            const query = JSON.parse(JSON.stringify(this.datapointQuery)); // recreating object, since the changes shouldn't apply to parent components; only or-chart itself.
            query.fromTimestamp = this._startOfPeriod;
            query.toTimestamp = this._endOfPeriod;

            if(query.type == 'lttb') {

                // If amount of data points is set, only allow a maximum of 1 points per pixel in width
                // Otherwise, dynamically set amount of data points based on chart width (1000px = 200 data points)
                if(query.amountOfPoints) {
                    if(this._chartElem?.clientWidth > 0) {
                        query.amountOfPoints = Math.min(query.amountOfPoints, this._chartElem?.clientWidth)
                    }
                } else {
                    if(this._chartElem?.clientWidth > 0) {
                        query.amountOfPoints = Math.round(this._chartElem.clientWidth / 5)
                    } else {
                        console.warn("Could not grab width of the Chart for estimating amount of data points. Using 100 points instead.")
                        query.amountOfPoints = 100;
                    }
                }

            } else if(query.type === 'interval' && !query.interval) {
                const diffInHours = (this.datapointQuery.toTimestamp! - this.datapointQuery.fromTimestamp!) / 1000 / 60 / 60;
                const intervalArr = this._getInterval(diffInHours);
                query.interval = (intervalArr[0].toString() + " " + intervalArr[1].toString()); // for example: "5 minute"
            }

            if(!predicted) {
                response = await manager.rest.api.AssetDatapointResource.getDatapoints(asset.id, attribute.name, query, options)
            } else {
                response = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(asset.id, attribute.name, query, options)
            }

            if (response.status === 200) {
                dataset.data = response.data.filter(value => value.y !== null && value.y !== undefined) as ScatterDataPoint[];
            }
        }

        return dataset;
    }

}
