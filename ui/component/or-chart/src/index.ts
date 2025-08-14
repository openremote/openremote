import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state, query} from "lit/decorators.js";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {
    Asset,
    AssetDatapointQueryUnion,
    AssetEvent,
    AssetModelUtil,
    AssetQuery,
    Attribute,
    AttributeRef, DatapointInterval,
    ReadAssetEvent,
    ValueDatapoint
} from "@openremote/model";
import manager, {DefaultColor2, DefaultColor3, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import "@openremote/or-asset-tree";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import * as echarts from "echarts/core";
import {DatasetComponentOption, DataZoomComponent, DataZoomComponentOption, GridComponent, GridComponentOption, TooltipComponent, TooltipComponentOption} from "echarts/components";
import {LineChart, LineSeriesOption} from "echarts/charts";
import {CanvasRenderer} from "echarts/renderers";
import {UniversalTransition} from "echarts/features";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-loading-indicator";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {OrAssetAttributePicker, OrAssetAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {cache} from "lit/directives/cache.js";
import {debounce} from "lodash";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import { when } from "lit/directives/when.js";
import {createRef, Ref, ref } from "lit/directives/ref.js";

echarts.use([GridComponent, TooltipComponent, DataZoomComponent, LineChart, CanvasRenderer, UniversalTransition]);

export type ECChartOption = echarts.ComposeOption<
    | LineSeriesOption
    | TooltipComponentOption
    | GridComponentOption
    | DatasetComponentOption
    | DataZoomComponentOption
>;

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
    fromTimestamp?: number;
    toTimestamp?: number;
    /*compareOffset?: number;*/
    period?: moment.unitOfTime.Base;
    deltaFormat?: "absolute" | "percentage";
    decimals?: number;
}

export interface ChartAttributeConfig {
    rightAxisAttributes?: AttributeRef[],
    smoothAttributes?: AttributeRef[],
    steppedAttributes?: AttributeRef[],
    areaAttributes?: AttributeRef[],
    faintAttributes?: AttributeRef[],
    extendedAttributes?: AttributeRef[]
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

/**
 * ECharts dataset object with additional optional fields for visualization purposes.
 * For example, {@link assetId} and {@link attrName} can be specified, so their information can be shown alongside the data itself.
 */
export interface LineChartData extends LineSeriesOption {
    assetId?: string;
    attrName?: string;
    unit?: string;
    extended?: boolean;
    predicted?: boolean;
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
        --internal-or-chart-border-color: var(--or-chart-border-color, rgba(76, 76, 76, 0.6));
        --internal-or-chart-text-color: var(--or-chart-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-chart-controls-margin: var(--or-chart-controls-margin, 0 0 20px 0);       
        --internal-or-chart-controls-margin-children: var(--or-chart-controls-margin-children, 0 auto 20px auto);            
        --internal-or-chart-graph-fill-color: var(--or-chart-graph-fill-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));       
        --internal-or-chart-graph-fill-opacity: var(--or-chart-graph-fill-opacity, 0.25);       
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
        padding: 11px 6px;
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
        gap: 8px;
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
    #period-controls {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    

    #controls {
        display: flex;
        flex-wrap: wrap;
        width: 100%;
        flex-direction: column;
        justify-content: center;
    }

    #attribute-list {
        overflow: hidden auto;
        flex: 1 1 50px;
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
        gap: 4px;
        /*min-height: 400px;
        max-height: 550px;*/
    }
    #chart-controls {
        padding: 0 8px;
        display: flex;
        flex-direction: column;
    }
    #chart {
        width: 100% !important;
        height: 100%; !important;
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
        #period-controls {
            flex-direction: row;
            justify-content: left;
            align-items: center;
            gap: 8px;
        }
    }
`;

@customElement("or-chart")
export class OrChart extends translate(i18next)(LitElement) {

    public static readonly DEFAULT_COLORS = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

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

    /**
     * The list of HEX colors representing the line color for each {@link AttributeRef}.
     * Acts as an override, and will fall back to the {@link colors} attribute if not specified.
     * The {@link AttributeRef} object with Asset ID and Attribute name need to be present in the Chart to work.
     * The HEX color should be put in without '#' prefix. For example, you'd use '4d9d2a' instead of '#4d9d2a'.
     */
    @property({type: Array})
    public attributeColors: [AttributeRef, string][] = [];

    /**
     * Chart attribute configuration object, specifying characteristics for each Chart line.
     * For example, what {@link AttributeRef} is aligned to the right side, or what {@link AttributeRef} is using a fill.
     * Check for {@link ChartAttributeConfig} for specification. This HTML attribute expects JSON string input.
     */
    @property({type: Object})
    public attributeConfig: ChartAttributeConfig = this._getDefaultAttributeConfig();

    @property()
    public dataProvider?: (startOfPeriod: number, endOfPeriod: number) => Promise<LineChartData[]>;

    @property({type: Array})
    public colors: string[] = OrChart.DEFAULT_COLORS;

    @property({type: Object})
    public readonly datapointQuery!: AssetDatapointQueryUnion;

    @property({type: Object})
    public config?: OrChartConfig;

    @property({type: Object})
    public chartOptions?: ECChartOption;

    @property({type: String})
    public realm?: string;

    @property()
    public panelName?: string;

    @property({type: Boolean})
    public attributeControls = true;

    @property()
    public timeframe?: [Date, Date];

    @property({type: Boolean})
    public timestampControls = true;

    /**
     * List of 'time prefix' options like 'this' or 'last', for the user to select from.
     * In combination with the {@link timeWindowOptions} attribute, it becomes a string like 'last 6 hours'.
     * @protected
     */
    @property({type: Array})
    public timePrefixOptions?: string[];

    /**
     * Selected 'time prefix' of the available {@link timePrefixOptions}.
     * This string attribute will only accept keys of the {@link timeWindowOptions} Array.
     */
    @property({type: String})
    public timePrefixKey?: string;

    /**
     * List of timeframe options like '6 hours' for the user to select from.
     * In combination with the {@link timePrefixKey} attribute, it becomes a string like 'last 6 hours'.
     * Expects a JSON string input, that is formatted as an JavaScript {@link Map} object.
     * The map is identified with a unique key, and a combination of `[duration, length]`.
     * For example `['6hours', ['hours', 6]]`.
     */
    @property({type: Object})
    public timeWindowOptions?: Map<string, [moment.unitOfTime.DurationConstructor, number]>;

    /**
     * Selected 'time window' of the available {@link timeWindowOptions}.
     * This string attribute will only accept keys of the {@link timeWindowOptions} Map.
     */
    @property({type: String})
    public timeWindowKey?: string;

    /**
     * Boolean attribute to enable/disable stacking the data vertically. (compound line chart)
     * On the same axis, it will display a cumulative effect of all data series on top of each other.
     */
    @property({type: Boolean})
    public stacked = false;

    @property({type: Boolean})
    public showLegend = true;

    @property({type: Boolean})
    public denseLegend = false;

    @property({type: Boolean})
    public showZoomBar = true;

    @state()
    protected _loading = false;

    @state()
    protected _data?: LineChartData[];

    @property()
    protected _tableTemplate?: TemplateResult;

    @state()
    protected _zoomChanged = false;

    @state()
    protected _isCustomWindow = false;

    @query("#chart")
    protected _chartElem!: HTMLDivElement;

    protected _chart?: echarts.ECharts;
    protected _style!: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _zoomStartOfPeriod?: number;
    protected _zoomEndOfPeriod?: number;
    protected _latestError?: string;
    protected _dataAbortController?: AbortController;
    protected _zoomHandler?: any;
    protected _containerResizeObserver?: ResizeObserver;
    protected _tooltipCache: [xTime: number, content: string] = [0, ""];


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

        const reloadData = changedProperties.has('attributeColors') || changedProperties.has("datapointQuery") || changedProperties.has("timeframe") || changedProperties.has("timePrefixKey") || changedProperties.has("timeWindowKey")||
            changedProperties.has("attributeConfig") || changedProperties.has("assetAttributes") || changedProperties.has("realm") || changedProperties.has("dataProvider");

        if (reloadData) {
            this._data = undefined;
            if (this._chart) {
                // Remove event listeners
                this._toggleChartEventListeners(false);
                this._chart.dispose();
                this._chart = undefined;
            }
            this._loadData();
        }

        if (!this._data) {
            return;
        }

        if (!this._chart) {

            // Parse the background color from CSS variables
            let bgColor = this._style.getPropertyValue("--internal-or-chart-graph-fill-color").trim();
            const opacity = Number(this._style.getPropertyValue("--internal-or-chart-graph-fill-opacity").trim());
            if (!isNaN(opacity)) {
                if (bgColor.startsWith("#") && (bgColor.length === 4 || bgColor.length === 7)) {
                    bgColor += (bgColor.length === 4 ? Math.round(opacity * 255).toString(16).substr(0, 1) : Math.round(opacity * 255).toString(16));
                } else if (bgColor.startsWith("rgb(")) {
                    bgColor = bgColor.substring(0, bgColor.length - 1) + opacity;
                }
            }
            // Define default EChart configration / options
            this.chartOptions = {
                animation: false,
                grid: {
                    show: true,
                    backgroundColor: this._style.getPropertyValue("--internal-or-asset-tree-background-color"),
                    borderColor: this._style.getPropertyValue("--internal-or-chart-border-color"),
                    left: 10,
                    right: 10,
                    top: 10,
                    bottom: this.showZoomBar ? 68 : 10,
                    containLabel: true
                },
                backgroundColor: this._style.getPropertyValue("--internal-or-asset-tree-background-color"),
                tooltip: {
                    trigger: "axis",
                    confine: true,
                    axisPointer: {
                        type: "cross",
                    },
                    formatter: (params: any) => {
                        const xTime = params[0].axisValue as number;
                        if (xTime !== this._tooltipCache[0]) {
                            // use global var to store current time selection, avoiding replicate calculation loops
                            this._tooltipCache[0] = xTime;
                            this._tooltipCache[1] = this._getTooltipData(xTime);
                        }
                        return this._tooltipCache[1];
                    }
                },
                toolbox: {},
                xAxis: {
                    type: "time",
                    axisLine: {
                        onZero: false,
                        lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}
                    },
                    splitLine: {show: true},
                    min: this._startOfPeriod,
                    max: this._endOfPeriod,
                    axisLabel: {
                        hideOverlap: true,
                        fontSize: 10,
                         formatter: {
                             year: "{yyyy}-{MMM}",
                             month: "{yy}-{MMM}",
                             day: "{d}-{MMM}",
                             hour: "{HH}:{mm}",
                             minute: "{HH}:{mm}",
                             second: "{HH}:{mm}:{ss}",
                             millisecond: "{d}-{MMM} {HH}:{mm}",
                             none: "{MMM}-{dd} {HH}:{mm}"
                        }
                    }
                },
                yAxis: [
                    {
                        type: "value",
                        axisLine: { lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}},
                        boundaryGap: ["10%", "10%"],
                        scale: true,
                        min: (this.chartOptions?.options as any)?.scales?.y?.min,
                        max: (this.chartOptions?.options as any)?.scales?.y?.max,
                        axisLabel: { hideOverlap: true }
                    },
                    {
                        type: "value",
                        show: (this.attributeConfig?.rightAxisAttributes?.length ?? 0) > 0,
                        axisLine: { lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}},
                        boundaryGap: ["10%", "10%"],
                        scale: true,
                        min: (this.chartOptions?.options as any)?.scales?.y1?.min,
                        max: (this.chartOptions?.options as any)?.scales?.y1?.max,
                        axisLabel: { hideOverlap: true }
                    }
                ],
                dataZoom: [
                    {
                        type: "inside",
                        start: 0,
                        end: 100
                    }
                ],
                series: []
            } as ECChartOption;

            // Add dataZoom bar if enabled
            if(this.showZoomBar) {
                (this.chartOptions.dataZoom as DataZoomComponentOption[]).push({
                    start: 0,
                    end: 100,
                    backgroundColor: bgColor,
                    fillerColor: bgColor,
                    dataBackground: {
                        areaStyle: {
                            color: this._style.getPropertyValue("--internal-or-chart-graph-fill-color")
                        }
                    },
                    selectedDataBackground: {
                        areaStyle: {
                            color: this._style.getPropertyValue("--internal-or-chart-graph-fill-color")
                        }
                    },
                    moveHandleStyle: {
                        color: this._style.getPropertyValue("--internal-or-chart-graph-fill-color")
                    },
                    emphasis: {
                        moveHandleStyle: {
                            color: this._style.getPropertyValue("--internal-or-chart-graph-fill-color")
                        },
                        handleLabel: {
                            show: false
                        }
                    },
                    handleLabel: {
                        show: false
                    }
                });
            }

            // Initialize echarts instance
            this._chart = echarts.init(this._chartElem);
            // Set chart options to default
            this._chart.setOption(this.chartOptions);
            this._toggleChartEventListeners(true);
        }

        if (changedProperties.has("_data")) {
            //Update chart to data from set period
            this._updateChartData();
        }

        this.getUpdateComplete().then(() => {
            this.dispatchEvent(new OrChartEvent("rendered"));
        });

    }

    // Not the best implementation, but it changes the legend & controls to wrap under the chart.
    // Also sorts the attribute lists horizontally when it is below the chart
    applyChartResponsiveness(): void {
        if(this.shadowRoot) {
            const container = this.shadowRoot.getElementById("container");
            if(container) {
                const bottomLegend: boolean = (container.clientWidth < 600);
                container.style.flexDirection = bottomLegend ? "column" : "row";
                const controls = this.shadowRoot.getElementById("controls");
                if(controls) {
                    controls.style.flexDirection = bottomLegend ? "row" : "column";
                }
                const periodControls = this.shadowRoot.getElementById("period-controls");
                if(periodControls) {
                    periodControls.style.flexDirection = bottomLegend ? "row" : "column";
                }
                const attributeList = this.shadowRoot.getElementById("attribute-list");
                if(attributeList) {
                    attributeList.style.gap = bottomLegend ? "4px 12px" : "";
                    attributeList.style.maxHeight = bottomLegend ? "90px" : "";
                    attributeList.style.flexFlow = bottomLegend ? "row wrap" : "column nowrap";
                    attributeList.style.padding = bottomLegend ? "0" : "5px 0";
                }
                this.shadowRoot.querySelectorAll(".attribute-list-item").forEach((item: Element) => {
                    (item as HTMLElement).style.minHeight = bottomLegend ? "0px" : "44px";
                    (item as HTMLElement).style.paddingLeft = bottomLegend ? "" : "0";
                    (item.children[1] as HTMLElement).style.flexDirection = bottomLegend ? "row" : "column";
                    (item.children[1] as HTMLElement).style.gap = bottomLegend ? "4px" : "";
                });
            }
        }
    }

    render() {
        const disabled = this._latestError;
        return html`
            <div id="container">
                <div id="chart-container" style="opacity: ${this._loading ? '0.7' : '1'};">
                    ${when(this._loading && !this._data, () => html`
                        <div style="position: absolute; height: 100%; width: 100%;">
                            <or-loading-indicator ?overlay="false"></or-loading-indicator>
                        </div>
                    `)}
                    ${when(this._latestError, () => html`
                        <div style="position: absolute; height: 100%; width: 100%; display: flex; justify-content: center; align-items: center;">
                            <or-translate .value="${this._latestError || 'errorOccurred'}"></or-translate>
                        </div>
                    `)}
                    <div id="chart" style="visibility: ${disabled ? 'hidden' : 'visible'};"></div>
                </div>
                
                ${(this.timestampControls || this.attributeControls || this.showLegend) ? html`
                    <div id="chart-controls">
                        <div id="controls">
                                ${this.timePrefixKey && this.timePrefixOptions && this.timeWindowKey && this.timeWindowOptions ? html`
                                    ${this.timestampControls ? html`
                                        <div id="period-controls">
                                            <!-- Time prefix selection -->
                                            ${getContentWithMenuTemplate(
                                                    html`<or-mwc-input .type="${InputType.BUTTON}" label="${this.timeframe ? "dashboard.customTimeSpan" : this.timePrefixKey.toLowerCase()}"></or-mwc-input>`,
                                                    this.timePrefixOptions.map(option => ({value: option, text: option.toLowerCase() } as ListItem)),
                                                    this.timePrefixKey,
                                                    (value: string | string[]) => {
                                                        this.timeframe = undefined; // remove any custom start & end times
                                                        this.timePrefixKey = value.toString();
                                                    },
                                                    undefined,
                                                    undefined,
                                                    undefined,
                                                    true
                                            )}
                                            <!-- Time window selection -->
                                            ${getContentWithMenuTemplate(
                                                    html`<or-mwc-input .type="${InputType.BUTTON}" label="${this._isCustomWindow ? "timeframe" : this.timeWindowKey.toLowerCase()}"></or-mwc-input>`,
                                                    Array.from(this.timeWindowOptions!.keys()).map(key => ({ value: key, text: key.toLowerCase() } as ListItem)),
                                                    this.timeWindowKey,
                                                    (value: string | string[]) => {
                                                        this.timeframe = undefined; // remove any custom start & end times
                                                        this.timeWindowKey = value.toString();
                                                    },
                                                    undefined,
                                                    undefined,
                                                    undefined,
                                                    true
                                            )}
                                        </div>
                                        <div style="text-align: center">
                                            <!-- Scroll left button -->
                                            <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-left"
                                                     @click="${() => this._shiftTimeframe(this.timeframe?.[0] ?? new Date(this._startOfPeriod!), this.timeframe?.[1] ?? new Date(this._endOfPeriod!), this.timeWindowKey!, "previous")}"
                                            ></or-icon>
                                            <!-- Scroll right button -->
                                            <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-right"
                                                     @click="${() => this._shiftTimeframe(this.timeframe?.[0] ?? new Date(this._startOfPeriod!), this.timeframe?.[1] ?? new Date(this._endOfPeriod!), this.timeWindowKey!, "next")}"
                                            ></or-icon>
                                            <!-- Button that opens custom time selection or restores to widget setting-->
                                            <or-icon class="button button-icon" ?disabled="${disabled}" icon="${this.timeframe ? 'restore' : 'calendar-clock'}"
                                                     @click="${() => this.timeframe ? (this._isCustomWindow = false, this.timeframe = undefined) : this._openTimeDialog(this._startOfPeriod, this._endOfPeriod)}"
                                            ></or-icon>
                                        </div>
                                    ` : html`
                                        <div style = "display: flex; flex-direction: column; align-items: center">
                                            <or-mwc-input .type="${InputType.BUTTON}" label="${this.timePrefixKey}" disabled></or-mwc-input>
                                            <or-mwc-input .type="${InputType.BUTTON}" label="${this.timeWindowKey}" disabled></or-mwc-input>
                                        </div>
                                    `}
                                ` : undefined}
                            
                            ${this.timeframe ? html`
                                <div style="font-size: 12px; display: flex; justify-content: flex-end;">
                                    <table style="text-align: right;">
                                        <thead>
                                        <tr>
                                            <th style="font-weight: normal;">${i18next.t('from')}:</th>
                                            <th style="font-weight: normal;">${moment(this.timeframe[0]).format("lll")}</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <tr>
                                            <td>${i18next.t('to')}:</td>
                                            <td>${moment(this.timeframe[1]).format("lll")}</td>
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
                                    const color = this.attributeColors.find(x => x[0].id === asset.id && x[0].name === attr.name)?.[1];
                                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset!.type, attr.name, attr);
                                    const label = Util.getAttributeLabel(attr, descriptors[0], asset!.type, true);
                                    const axisNote = (this.attributeConfig.rightAxisAttributes?.find(ar => asset!.id === ar.id && attr.name === ar.name)) ? i18next.t('right') : undefined;
                                    const bgColor = (color ?? this.colors[colourIndex]) || "";
                                    return html`
                                        <div class="attribute-list-item ${this.denseLegend ? 'attribute-list-item-dense' : undefined}"
                                             @mouseenter="${() => this._addDatasetHighlight({id: this.assets[assetIndex]!.id, name: attr.name})}"
                                             @mouseleave="${()=> this._removeDatasetHighlights()}">
                                            <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[assetIndex]!.type!), undefined, undefined, bgColor.split('#')[1])}</span>
                                            <div class="attribute-list-item-label ${this.denseLegend ? 'attribute-list-item-label-dense' : undefined}">
                                                <div style="display: flex; gap: 4px;">
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

    /**
     * Removes all active Chart line color highlights, by reducing/increasing opacity.
     * @protected
     */
    protected _removeDatasetHighlights(chart = this._chart) {
        if(chart){
            const options = chart.getOption();
            if (options.series && Array.isArray(options.series)) {
                options.series.forEach(function (series) {
                    if (series.lineStyle.opacity === 0.2 || series.lineStyle.opacity === 0.99) {
                        series.lineStyle.opacity = 0.31;
                        series.showSymbol = false;
                    } else {
                        series.lineStyle.opacity = 1;
                        series.showSymbol = true;
                    }
                });
            }
            chart.setOption(options);
        }
    }

    /**
     * Adds a Chart line color highlight, by reducing/increasing opacity.
     * So the given line (represented by {@link assetId} and {@link attrName} will be emphasized, while others are less visible.
     * @param attrRef - Asset ID and attribute name to be highlighted
     * @param chart - ECharts instance to add the highlight to.
     */
    _addDatasetHighlight(attrRef: AttributeRef, chart = this._chart) {
        if (chart) {
            const options = chart.getOption();
            if (options.series && Array.isArray(options.series)) {
                options.series.forEach(series => {
                    if (series.assetId !== attrRef.id || series.attrName !== attrRef.name) {
                        if (series.lineStyle.opacity === 0.31) { // 0.31 is faint setting, 1 is normal
                            series.lineStyle.opacity = 0.2;
                        } else {
                            series.lineStyle.opacity = 0.3;
                            series.showSymbol = false;
                        }
                    } else if (series.lineStyle.opacity === 0.31) { // extra highlight if selected is faint
                        series.lineStyle.opacity = 0.99;
                        series.showSymbol = true;
                    }
                });
            }
            chart.setOption(options);
        }
    }

    async loadSettings(reset: boolean) {

        if(this.assetAttributes === undefined || reset) {
            this.assetAttributes = [];
        }

        this.realm ??= manager.getRealm();
        this.timePrefixOptions ??= this._getDefaultTimePrefixOptions();
        this.timeWindowOptions ??= this._getDefaultTimeWindowOptions();
        this.timeWindowKey ??= this.timeWindowOptions.keys().next().value!.toString();
        this.timePrefixKey ??= this.timePrefixOptions[1];

        if (!this.panelName) {
            return;
        }

        const viewSelector = window.location.hash;
        const allConfigs: OrChartConfig[] = await manager.console.retrieveData("OrChartConfig") || [];

        if (!Array.isArray(allConfigs)) {
            manager.console.storeData("OrChartConfig", [allConfigs]);
        }

        const config: OrChartConfig | undefined = allConfigs.find(e => e.realm === this.realm);

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
                view.attributeRefs = view.attributeRefs.filter(attrRef =>
                    !!assets.find(asset => asset.id === attrRef.id && asset.attributes && asset.attributes.hasOwnProperty(attrRef.name!)));

                manager.console.storeData("OrChartConfig", [...allConfigs.filter(e => e.realm !== this.realm), config]);
                this.assets = assets.filter(asset => view.attributeRefs!.find(attrRef => attrRef.id === asset.id));
            } catch (e) {
                console.error("Failed to get assets requested in settings", e);
            }

            this._loading = false;

            if (this.assets && this.assets.length > 0) {
                this.assetAttributes = view.attributeRefs.map(attrRef => {
                    const assetIndex = this.assets.findIndex(asset => asset.id === attrRef.id);
                    const asset = assetIndex >= 0 ? this.assets[assetIndex] : undefined;
                    return asset && asset.attributes ? [assetIndex!, asset.attributes[attrRef.name!]] : undefined;
                }).filter(indexAndAttr => !!indexAndAttr) as [number, Attribute<any>][];
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
        config ??= {realm: this.realm, views: {}};

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
        const dialog = showDialog(new OrAssetAttributePicker()
            .setShowOnlyDatapointAttrs(true)
            .setMultiSelect(true)
            .setSelectedAttributes(this._getSelectedAttributes()));

        dialog.addEventListener(OrAssetAttributePickerPickedEvent.NAME, (ev: any) => this._addAttribute(ev.detail));
    }

    protected _openTimeDialog(startTimestamp?: number, endTimestamp?: number) {
        const startRef: Ref<OrMwcInput> = createRef();
        const endRef: Ref<OrMwcInput> = createRef();
        showDialog(new OrMwcDialog()
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
                    if(startRef.value?.value && endRef.value?.value) {
                        this._isCustomWindow = true;
                        this.timeframe = [new Date(startRef.value.value), new Date(endRef.value.value)];
                    }
                }
            }])
        );
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

    protected _cleanup() {
        if (this._chart) {
            this._toggleChartEventListeners(false);
            this._chart.dispose();
            this._chart = undefined;
            this.requestUpdate();
        }
    }

    protected _getDefaultTimePrefixOptions(): string[] {
        return ["this", "last"];
    }

    protected _getDefaultTimeWindowOptions(): Map<string, [moment.unitOfTime.DurationConstructor, number]> {
        return new Map<string, [moment.unitOfTime.DurationConstructor, number]>([
            ["Hour", ["hours", 1]],
            ["6Hours", ["hours", 6]],
            ["24Hours", ["hours", 24]],
            ["Day", ["days", 1]],
            ["7Days", ["days", 7]],
            ["Week", ["weeks", 1]],
            ["30Days", ["days", 30]],
            ["Month", ["months", 1]],
            ["365Days", ["days", 365]],
            ["Year", ["years", 1]]
        ]);
    };

    /**
     * Internal function for retrieving a start/end date, based on the selected time frame.
     * Given the {@link selectedTimePrefix} (say 'last') and the {@link selectedTimeWindow} (say '6 hours'),
     * it will return the respected JavaScript {@link Date} objects.
     * @param selectedTimePrefix - Time prefix string, for example 'last'.
     * @param selectedTimeWindow - Time window string, for example '6 hours'.
     * @protected
     */
    protected _getTimeSelectionDates(selectedTimePrefix: string, selectedTimeWindow: string): [Date, Date] {
        let startDate = moment();
        let endDate = moment();
        const timeWindow: [moment.unitOfTime.DurationConstructor, number] | undefined = this.timeWindowOptions!.get(selectedTimeWindow);
        if (!timeWindow) {
            throw new Error(`Unsupported time window selected: ${selectedTimeWindow}`);
        }
        const [unit , value]: [moment.unitOfTime.DurationConstructor, number] = timeWindow;
        switch (selectedTimePrefix) {
            case "this":
                if (value === 1) { // For singulars like this hour
                    startDate = moment().startOf(unit);
                    endDate = moment().endOf(unit);
                } else { // For multiples like this 5 min, put now in the middle
                    startDate = moment().subtract(value*0.5, unit);
                    endDate = moment().add(value*0.5, unit);
                }
                break;
            case "last":
                startDate = moment().subtract(value, unit).startOf(unit);
                if (value === 1) { // For singulars like last hour
                    endDate = moment().startOf(unit);
                } else { //For multiples like last 5 min
                    endDate = moment();
                }
                break;
        }
        return [startDate.toDate(), endDate.toDate()];
    }

    /**
     * Internal function for shifting a timeframe in a specific {@link direction}.
     * Based on the given {@link currentStart} and {@link currentEnd} dates, it will move forward/backwards for the {@link selectedTimeWindow}.
     * For example, when viewing '1 week' of data, and calling this function with the 'previous' direction, it will update {@link timeframe} with dates one week in advance.
     *
     * @param currentStart - Start date of the current viewport
     * @param currentEnd - End date of the current viewport
     * @param selectedTimeWindow - Time window to subtract/add from the viewport
     * @param direction - Whether to advance or reverse the viewport (whether to add or remove time)
     * @protected
     */
    protected _shiftTimeframe(currentStart: Date, currentEnd: Date, selectedTimeWindow: string, direction: "previous" | "next") {
        const timeWindow = this.timeWindowOptions!.get(selectedTimeWindow);
        if (!timeWindow) {
            throw new Error(`Unsupported time window selected: ${selectedTimeWindow}`);
        }
        const [unit, value] = timeWindow;
        let newStart = moment(currentStart);
        direction === "previous" ? newStart.subtract(value, unit as moment.unitOfTime.DurationConstructor) : newStart.add(value, unit as moment.unitOfTime.DurationConstructor);
        let newEnd = moment(currentEnd)
        direction === "previous" ? newEnd.subtract(value, unit as moment.unitOfTime.DurationConstructor) : newEnd.add(value, unit as moment.unitOfTime.DurationConstructor);
        this.timeframe = [newStart.toDate(), newEnd.toDate()];
    }

    protected _getInterval(diffInHours: number): [number, DatapointInterval] {

        if (diffInHours <= 1) {
            return [5, DatapointInterval.MINUTE];
        } else if (diffInHours <= 3) {
            return [10, DatapointInterval.MINUTE];
        } else if (diffInHours <= 6) {
            return [30, DatapointInterval.MINUTE];
        } else if (diffInHours <= 24) { // one day
            return [1, DatapointInterval.HOUR];
        } else if (diffInHours <= 48) { // two days
            return [3, DatapointInterval.HOUR];
        } else if (diffInHours <= 96) {
            return [12, DatapointInterval.HOUR];
        } else if (diffInHours <= 744) { // one month
            return [1, DatapointInterval.DAY];
        } else {
            return [1, DatapointInterval.MONTH];
        }
    }

    protected async _loadData() {
        if ((this._data && !this._zoomChanged) || !this.assetAttributes || !this.assets || (this.assets.length === 0 && !this.dataProvider) || (this.assetAttributes.length === 0 && !this.dataProvider) || !this.datapointQuery) {
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
        const dates: [Date, Date] = this._getTimeSelectionDates(this.timePrefixKey!, this.timeWindowKey!);
        const data: LineChartData[] = [];
        let promises;

        if(!this._zoomChanged || !this._startOfPeriod || !this._endOfPeriod) {
            // If zoom has changed, we want to keep the previous start and end of period
            this._startOfPeriod = this.timeframe ? this.timeframe[0].getTime() : dates[0].getTime();
            this._endOfPeriod = this.timeframe ? this.timeframe[1].getTime() : dates[1].getTime();
        }

        try {
            if(this.dataProvider && !this._zoomChanged) {
                await this.dataProvider(this._startOfPeriod, this._endOfPeriod).then(dataset => {
                    dataset.forEach(set => data.push(set));
                });
            } else {
                this._dataAbortController = new AbortController();
                promises = this.assetAttributes.map(async ([assetIndex, attribute], index) => {

                    const asset = this.assets[assetIndex];
                    const shownOnRightAxis = !!this.attributeConfig?.rightAxisAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const smooth = !!this.attributeConfig?.smoothAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const stacked = this.stacked;
                    const stepped = !!this.attributeConfig?.steppedAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const area = !!this.attributeConfig?.areaAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const faint = !!this.attributeConfig?.faintAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const extended = !!this.attributeConfig?.extendedAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const color = this.attributeColors.find(x => x[0].id === asset.id && x[0].name === attribute.name)?.[1];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
                    const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                    const unit = Util.resolveUnits(Util.getAttributeUnits(attribute, descriptors[0], asset.type));
                    const colourIndex = index % this.colors.length;
                    const options = { signal: this._dataAbortController?.signal };

                    // Load Historic Data
                    let dataset = await this._loadAttributeData(asset, attribute, color ?? this.colors[colourIndex], false, smooth, stacked, stepped, area, faint, false, `${asset.name} ${label}`, options, unit);
                    dataset.assetId = asset.id;
                    dataset.attrName = attribute.name;
                    dataset.unit = unit;
                    dataset.yAxisIndex = shownOnRightAxis ? 1 : 0;
                    data.push(dataset);

                    // Load Predicted Data
                    dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, color ?? this.colors[colourIndex], true, smooth, stacked, stepped, area, faint, false , `${asset.name} ${label} ${i18next.t("predicted")}`, options, unit);
                    data.push(dataset);

                    // If necessary, load Extended Data
                    if (extended) {
                        dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, color ?? this.colors[colourIndex], false, false, stacked, false, area, faint, extended, `${asset.name} ${label} lastKnown`, options, unit);
                        dataset.yAxisIndex = shownOnRightAxis ? 1 : 0;
                        data.push(dataset);
                    }

                });
            }

            if(promises) {
                await Promise.all(promises);
            }

            this._data = data;
            this._loading = false;
            this._zoomChanged = false;

        } catch (ex) {
            console.error(ex);
            if((ex as Error)?.message === "canceled") {
                return; // If request has been canceled (using AbortController); return, and prevent _loading is set to false.
            }
            this._loading = false;
            this._zoomChanged = false;

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

    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, color: string, predicted: boolean, smooth: boolean, stacked: boolean, stepped: boolean, area: boolean, faint: boolean, extended: boolean, label?: string, options?: any, unit?: any): Promise<LineChartData> {

        function rgba (color: string, alpha: number) {
            return `rgba(${parseInt(color.slice(-6,-4), 16)}, ${parseInt(color.slice(-4,-2), 16)}, ${parseInt(color.slice(-2), 16)}, ${alpha})`;
        }

        const dataset = {
            name: label,
            type: "line",
            data: [],
            sampling: "lttb",
            lineStyle: {
                color: color,
                type: predicted ? [2, 4] : extended ? [10, 2] : undefined,
                opacity: faint ? 0.31 : 1,
            },
            symbol: "circle",
            showSymbol: !faint,
            itemStyle: {
                color: color
            },
            stack: stacked ? (extended ? "extended" : "total") : undefined,
            smooth: smooth,
            step: stepped ? "end" : undefined,
            extended: extended,
            predicted: predicted,
            areaStyle: area ? {color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0,
                        color: rgba(color, faint ? 0.1 : 0.5)
                    },
                    {
                        offset: 1,
                        color: rgba(color, 0)
                    }
                ])} : undefined,
        } as LineChartData;

        if (asset.id && attribute.name && this.datapointQuery) {
            let response: GenericAxiosResponse<ValueDatapoint<any>[]>;
            const query = JSON.parse(JSON.stringify(this.datapointQuery)); // recreating object, since the changes shouldn't apply to parent components; only or-chart itself.

            if(query.type === "lttb") {
                // If number of data points is set, only allow a maximum of 1 point per pixel in width
                // Otherwise, dynamically set number of data points based on chart width (1000px = 100 data points)
                if(query.amountOfPoints) {
                    if(this._chartElem?.clientWidth > 0) {
                        query.amountOfPoints = Math.min(query.amountOfPoints, this._chartElem?.clientWidth);
                    }
                } else {
                    if(this._chartElem?.clientWidth > 0) {
                        query.amountOfPoints = Math.round(this._chartElem.clientWidth / 10);
                    } else {
                        console.warn("Could not grab width of the Chart for estimating amount of data points. Using 100 points instead.");
                        query.amountOfPoints = 100;
                    }
                }
            } else if(query.type === "interval" && !query.interval) {
                const diffInHours = (this.datapointQuery.toTimestamp! - this.datapointQuery.fromTimestamp!) / 1000 / 60 / 60;
                const intervalArr = this._getInterval(diffInHours);
                query.interval = (intervalArr[0].toString() + " " + intervalArr[1].toString()); // for example: "5 minute"
            }

            // Update start/end dates in DatapointQuery object
            if (!this._zoomChanged) {
                query.fromTimestamp = this._startOfPeriod;
                query.toTimestamp = this._endOfPeriod;
            } else {
                query.fromTimestamp = this._zoomStartOfPeriod;
                query.toTimestamp = this._zoomEndOfPeriod;
            }

            // Request data using HTTP
            if (predicted) {
                response = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(asset.id, attribute.name, query, options);
            } else {
                if (extended) {
                    // if request is for extended dataset, we want to get the last known value only
                    query.type = "nearest";
                    query.timestamp = new Date().toISOString();
                }
                response = await manager.rest.api.AssetDatapointResource.getDatapoints(asset.id, attribute.name, query, options);
            }

            let data: ValueDatapoint<any>[] = [];

            if (response.status === 200) {
                data = response.data
                    .filter(value => value.y !== null && value.y !== undefined)
                    .map(point => ({ x: point.x, y: point.y } as ValueDatapoint<any>));

                dataset.data = data.map(point => [point.x, point.y]);
            }

            if (extended) {
                const firstPoint = dataset.data?.[0] as any[] | undefined;
                if (firstPoint !== undefined) {
                    // Get the first datapoint's timestamp
                    const firstPointTime = new Date(firstPoint[0]).getTime();

                    // If the first point is earlier than startOfPeriod, use startOfPeriod as the starting timestamp
                    const startTimestamp = firstPointTime < query.fromTimestamp!
                        ? new Date(query.fromTimestamp!).toISOString()
                        : firstPoint[0];

                    // Use endOfPeriod if it's earlier than now, otherwise use the current time
                    const now = new Date().getTime();
                    const endTimestamp = query.toTimestamp! < now
                        ? new Date(query.toTimestamp!).toISOString()
                        : new Date().toISOString();

                    // Create a clean extended line by removing any existing points and adding just two points:
                    // One at the appropriate start time and one at the current time
                    dataset.data = [
                        [startTimestamp, firstPoint[1]],
                        [endTimestamp, firstPoint[1]]
                    ];
                }
            }
        }
        return dataset;
    }

    /**
     * Callback event for the 'datazoom' event of ECharts.
     * Whenever a user zooms in the chart, we update the start/end time, and refetch chart data.
     * So, if user is zooming in from a year of data, to only view data of September, we refetch the data of September.
     * This is also to improve data accuracy, since we're using the LTTB algorithm by default.
     *
     * @param event - Payload of the 'datazoom' event
     * @protected
     */
    protected _onZoomChange(event: any) {
        this._zoomChanged = true;
        const { start: zoomStartPercentage, end: zoomEndPercentage } = event.batch?.[0] ?? event; // Events triggered by scroll and zoombar return different structures

        //Define the start and end of the period based on the zoomed area
        this._zoomStartOfPeriod = this._startOfPeriod! + ((this._endOfPeriod! - this._startOfPeriod!) * zoomStartPercentage / 100);
        this._zoomEndOfPeriod = this._startOfPeriod! + ((this._endOfPeriod! - this._startOfPeriod!) * zoomEndPercentage / 100);
        this._loadData().then(() => {
            this._updateChartData();
        });

    }

    /**
     * Updates the data in the chart. It will replace existing data.
     * @param data - Time series data to insert
     * @param start - Start timestamp
     * @param end - End timestamp
     * @protected
     */
    protected _updateChartData(data = this._data, start = this._startOfPeriod, end = this._endOfPeriod){
        if(!this._chart) {
            console.error("Could not update chart data; the chart is not initialized yet.");
            return;
        }
        this._chart.setOption({
            xAxis: {
                min: start,
                max: end
            },
            series: data?.map(series => ({
                ...series,
                markLine: {
                    symbol: "none",
                    silent: true,
                    data: [{ name: "", xAxis: new Date().toISOString(), label: { formatter: "{b}" } }],
                    lineStyle: {
                        color: this._style.getPropertyValue("--internal-or-chart-text-color"),
                        type: "solid",
                        width: 1,
                        opacity: 1
                    }
                }
            }))
        });
    }

    /**
     * Adds/removes the event listeners for the Chart element.
     * For example, subscribing / unsubscribing from the element resize or zoom event.
     *
     * @param connect - Whether to connect or disconnect event listeners.
     * @protected
     */
    protected _toggleChartEventListeners(connect: boolean){
        if (connect) {
            // Add resize eventlisteners to make chart size responsive
            window.addEventListener("resize", () => this._chart!.resize());
            this._containerResizeObserver = new ResizeObserver(() => { this._chart!.resize(); this.applyChartResponsiveness();});
            if (this.shadowRoot) {
                this._containerResizeObserver.observe(this.shadowRoot!.getElementById('container') as HTMLElement);
            }
            // Add event listener for zooming
            this._zoomHandler = this._chart!.on('datazoom', debounce((params: any) => { this._onZoomChange(params); }, 750));
        }
        else if (!connect) {
            //Disconnect event listeners
            this._chart!.off('datazoom', this._zoomHandler);
            this._containerResizeObserver?.disconnect();
            this._containerResizeObserver = undefined;
        }
    }

    /**
     * Internal function to retrieve a string for the time series data tooltip.
     * Based on {@link xTime}, it will retrieve data from the dataset, and display the correct value in a tooltip.
     * It uses the format `{asset + attribute name}: {value} {unit}`. For example, "Light 1 brightness: 30 %"
     *
     * @param xTime - Timestamp to use for generating the tooltip
     * @protected
     */
    protected _getTooltipData(xTime: number) {
        type DataPoint = { timestamp: number; value: number };
        type tooltipRow = {value: number; text: string};
        const tooltipArray: tooltipRow[] = [];
        this._data?.forEach(dataset => {
            const xTimeIsFuture: boolean = xTime > moment().toDate().getTime();
            // Load datasets to be shown. Show historic or predicted based on cursor location, dont show extended datasets.
            if (dataset.data && dataset.data.length > 0 && !dataset.extended && (dataset.predicted === xTimeIsFuture)) {
                const name = dataset.name;
                let left = 0;
                let right = dataset.data.length - 1;
                let pastDatapoint: DataPoint | null = null;
                let futureDatapoint: DataPoint | null = null;
                let displayValue: number | null = null;
                let exactMatch: boolean = false;

                // Find closest past and future timestamps to given time
                while (left <= right) {
                    const mid = Math.floor((left + right) / 2);
                    const [timestamp, value] = dataset.data[mid] as [number, number];
                    if (timestamp === xTime) {
                        displayValue = value;
                        exactMatch = true;
                        break;
                    } else if (timestamp < xTime) {
                        pastDatapoint = {timestamp, value};
                        left = mid + 1;
                    } else {
                        futureDatapoint = {timestamp, value};
                        right = mid - 1;
                    }
                }

                // Clear past/future if they are at dataset boundaries (ensuring they remain null if no valid data exists)
                if (pastDatapoint && pastDatapoint.timestamp > xTime) pastDatapoint = null;
                if (futureDatapoint && futureDatapoint.timestamp < xTime) futureDatapoint = null;

                // Interpolate or show one of the closest datapoints.
                if (!exactMatch) {
                    if (pastDatapoint && futureDatapoint && !dataset.step) {
                        // Interpolate between past and future datapoint if they exist, keep up to 2 decimals
                        displayValue = parseFloat((pastDatapoint.value + ((xTime - pastDatapoint.timestamp) / (futureDatapoint.timestamp - pastDatapoint.timestamp)) * (futureDatapoint.value - pastDatapoint.value)).toFixed(2));
                    } else if (!pastDatapoint && futureDatapoint) {
                        //Show nearest future value if at start of dataset
                        displayValue = futureDatapoint.value;
                    } else if (pastDatapoint && (!futureDatapoint || dataset.step == "end")) {
                        //Show nearest past value if: at end of dataset or the stepped setting is active
                        displayValue = pastDatapoint.value;
                    }
                }
                if (displayValue) {
                    tooltipArray.push({
                        value: displayValue,
                        text: `<div><span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color: ${dataset.lineStyle?.color}"></span> ${name}: <b>${displayValue}${dataset.unit ?? ""}</b></div>`
                    });
                }
            }
        });
        // Sort by value for better readability
        tooltipArray.sort((a, b) => b.value - a.value);
        return tooltipArray.map(t => t.text).join('');
    }

    /**
     * Internal function to get the default Chart attribute config.
     * @protected
     */
    protected _getDefaultAttributeConfig(): ChartAttributeConfig {
        return {
            rightAxisAttributes: [],
            smoothAttributes: [],
            steppedAttributes: [],
            areaAttributes: [],
            faintAttributes: [],
            extendedAttributes: []
        };
    }

}
