import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state, query} from "lit/decorators.js";
import i18next from "i18next";
import {
    Asset,
    AssetDatapointIntervalQueryFormula,
    AssetDatapointQueryUnion,
    AssetModelUtil,
    Attribute,
    AttributeRef,
    DatapointInterval,
    ValueDatapoint
} from "@openremote/model";
import manager, {DefaultColor3, DefaultColor4, Util} from "@openremote/core";
import * as echarts from "echarts/core";
import {DatasetComponentOption, DataZoomComponent, DataZoomComponentOption, GridComponent, GridComponentOption, TooltipComponent, TooltipComponentOption} from "echarts/components";
import {BarChart, BarSeriesOption} from "echarts/charts";
import {CanvasRenderer} from "echarts/renderers";
import {UniversalTransition} from "echarts/features";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import moment from "moment";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {cache} from "lit/directives/cache.js";
import "@openremote/or-mwc-components/or-mwc-menu";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {when} from "lit/directives/when.js";
import {createRef, Ref, ref} from "lit/directives/ref.js";

echarts.use([GridComponent, TooltipComponent, DataZoomComponent, BarChart, CanvasRenderer, UniversalTransition]);

export class OrAttributeBarChartRenderEvent extends CustomEvent<OrAttributeBarChartRenderEventDetail> {

    public static readonly NAME = "or-barchart-render";

    constructor(value?: any, previousValue?: any) {
        super(OrAttributeBarChartRenderEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface OrAttributeBarChartRenderEventDetail {
    value?: any;
    previousValue?: any;
}

/**
 * Bar chart configuration with options for individual attributes.
 * For example, a list of attributes aligned to the axis on the right side.
 */
export interface BarChartAttributeConfig {
    rightAxisAttributes?: AttributeRef[];
    methodMaxAttributes?: AttributeRef[];
    methodMinAttributes?: AttributeRef[];
    methodAvgAttributes?: AttributeRef[];
    methodDeltaAttributes?: AttributeRef[];
    methodMedianAttributes?: AttributeRef[];
    methodModeAttributes?: AttributeRef[];
    methodSumAttributes?: AttributeRef[];
    methodCountAttributes?: AttributeRef[];
}

/**
 * Interval configuration for a user to select from.
 * It contains a {@link displayName}, together with a step+interval like "6 hours".
 */
export interface IntervalConfig {
    displayName: string;
    steps: number;
    orFormat: DatapointInterval;
    momentFormat: moment.unitOfTime.DurationConstructor;
    millis: number;
}

export type ECChartOption = echarts.ComposeOption<
    | BarSeriesOption
    | TooltipComponentOption
    | GridComponentOption
    | DatasetComponentOption
    | DataZoomComponentOption
>;

// language=CSS
const style = css`
    :host {
        --internal-or-chart-text-color: var(--or-chart-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-chart-graph-fill-color: var(--or-chart-graph-fill-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-chart-graph-point-hover-border-color: var(--or-chart-graph-point-hover-border-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        width: 100%;
        display: block;
    }

    .button-icon {
        align-self: center;
        padding: 10px;
        cursor: pointer;
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

    .period-controls {
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .navigate {
        flex-direction: column;
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
        margin-top: 10px;
    }

    #chart {
        width: 100%;
        height: 100%;
    }

    @media screen and (max-width: 1280px) {
        #chart-container {
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

@customElement("or-attribute-barchart")
export class OrAttributeBarChart extends LitElement {

    /**
     * Asset data to display in the bar chart. This array needs to be populated.
     * Used for the chart legend, information about units, and retrieval of data points.
     */
    @property({type: Object})
    public assets: Asset[] = [];

    /**
     * List of attributes to display in the bar chart. This array needs to be populated.
     * Used for the chart legend, and the retrieval of data points.
     * Formatted as a JSON array of;
     *
     * ```[<index of {@link assets}>, {@link Attribute}]```
     */
    @property({type: Object})
    public assetAttributes: [number, Attribute<any>][] = [];

    /**
     * Realm name to associate all the chart data with.
     */
    @property({type: String})
    public realm: string | undefined = manager?.displayRealm;

    /**
     * The list of HEX colors representing the line color for each {@link AttributeRef}.
     * Acts as an override, and will fall back to the {@link colors} attribute if not specified.
     * The {@link AttributeRef} object with Asset ID and Attribute name need to be present in the Chart to work.
     * The HEX color should be put in without '#' prefix. For example, you'd use '4d9d2a' instead of '#4d9d2a'.
     */
    @property({type: Array})
    public readonly attributeColors: [AttributeRef, string][] = [];

    /**
     * The attribute configuration object with, for example, a list of right-axis attributes.
     * See {@link BarChartAttributeConfig} for full documentation on the options.
     */
    @property({type: Object})
    public readonly attributeConfig?: BarChartAttributeConfig;

    /**
     * List of colors to display attributes values with inside the bar chart.
     * The array corresponds with {@link assetAttributes}, so the 1st color will be used for the 1st attribute.
     */
    @property({type: Array})
    public readonly colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    /**
     * The query object to request attribute data / data points with.
     * Normally, it's of type {@link AssetDatapointIntervalQuery}, including a start- and end time.
     */
    @property({type: Object})
    public readonly datapointQuery!: AssetDatapointQueryUnion;

    /**
     * Selected timeframe using a [start date, end date] array format.
     */
    @property({type: Array})
    public timeframe?: [Date, Date];

    /**
     * Custom ECharts options object to configure the chart appearance.
     * See their documentation for more info: https://echarts.apache.org/en/option.html
     */
    @property({type: Object})
    public chartOptions?: ECChartOption;

    /**
     * Shows a right-side legend with the list of visible attributes.
     */
    @property({type: Boolean})
    public showLegend = false;

    /**
     * Shows the right-side legend (toggled with {@link showLegend}) in a compact / dense format.
     */
    @property({type: Boolean})
    public denseLegend = false;

    /**
     * Displays the bar chart in a vertically "stacked" configuration.
     * It will display a cumulative effect of all data series on top of each other.
     */
    @property({type: Boolean})
    public stacked = false;

    /**
     * Enables time control for the user, allowing them to customize the {@link timeframe} shown.
     * Leaving this disabled/unset will show them in a "read-only" state.
     */
    @property({type: Boolean})
    public timestampControls = true;

    /**
     * Number of decimals to display for attributes.
     */
    @property({type: Number})
    public decimals = 2;

    @property({type: Array})
    public timePrefixOptions: string[] = OrAttributeBarChart.getDefaultTimePrefixOptions();

    @property({type: String})
    public timePrefixKey: string = this.timePrefixOptions[0];

    @property({type: Object})
    public timeWindowOptions: Map<string, [moment.unitOfTime.DurationConstructor, number]> = OrAttributeBarChart.getDefaultTimeWindowOptions();

    @property({type: String})
    public timeWindowKey?: string;

    @property({type: Object})
    public intervalOptions: Map<string, IntervalConfig> = OrAttributeBarChart.getDefaultIntervalOptions();

    @property({type: String})
    public interval?: string;

    @state()
    protected _isCustomWindow = false;

    @state()
    protected _loading = false;

    @state()
    protected _data?: any[];

    @query("#chart")
    protected _chartElem!: HTMLDivElement;

    protected _chartOptions: ECChartOption = {};
    protected _chart?: echarts.ECharts;
    protected _style!: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _latestError?: string;
    protected _dataAbortController?: AbortController;
    protected _zoomHandler? :any;
    protected _containerResizeObserver?: ResizeObserver;
    protected _markAreaData: { xAxis: number }[][] = [];

    protected _intervalConfig?: IntervalConfig;

    static get styles() {
        return [style];
    }

    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();

    }

    willUpdate(changedProps: PropertyValues) {
        if(changedProps.has("timeWindowOptions") && this.timeWindowOptions) {
            this.timeWindowKey = Array.from(this.timeWindowOptions.keys())[0];
        }
        if(changedProps.has("intervalOptions") && this.intervalOptions) {
            this.interval = Array.from(this.intervalOptions.keys())[0];
        }
        return super.willUpdate(changedProps);
    }

    updated(changedProperties: PropertyValues) {

        super.updated(changedProperties);

        if (changedProperties.has("realm")) {
            if(changedProperties.get("realm") != undefined) { // Checking whether it was undefined previously, to prevent loading 2 times and resetting attribute properties.
                this.assets = [];
                this.assetAttributes = [];
            }
        }

        const reloadData = changedProperties.has('attributeColors') || changedProperties.has("datapointQuery") || changedProperties.has("timeframe") || changedProperties.has("interval") || changedProperties.has("timePrefixKey") || changedProperties.has("timeWindowKey")||
            changedProperties.has("attributeConfig") || changedProperties.has("assetAttributes") || changedProperties.has("realm");

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

        this._intervalConfig = this._validateInterval(this._startOfPeriod!,this._endOfPeriod!,  this.interval!);

        if (!this._chart) {
            this._chartOptions = {
                //animation: false,
                grid: {
                    show: true,
                    backgroundColor: this._style.getPropertyValue("--internal-or-asset-tree-background-color"),
                    borderColor: this._style.getPropertyValue("--internal-or-chart-text-color"),
                    left: 25,
                    right: 25,
                    top: 10,
                    bottom:  25,
                    containLabel: true
                },
                backgroundColor: this._style.getPropertyValue("--internal-or-asset-tree-background-color"),
                tooltip: {
                    trigger: 'axis',
                    confine: true, //make tooltip not go outside frame bounds
                    //transitionDuration: 0.2,
                    axisPointer: {
                        type: 'none',
                        label: {
                            formatter: (params: any) => {
                                const startTime = new Date(params.value - 0.5 * this._intervalConfig!.millis).toLocaleString();
                                const endTime = new Date(params.value + 0.5 * this._intervalConfig!.millis).toLocaleString();
                                return `Interval: ${startTime} to ${endTime}`
                            }
                        }
                    },
                },
                xAxis: {
                    type: 'time',
                    axisLine: {
                        lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}
                    },
                    //splitLine: {show: true},
                    //minorSplitLine: {show: true},
                    splitNumber: (this._endOfPeriod! - this._startOfPeriod!)/this._intervalConfig!.millis - 1,
                    //minorTick: {show: true},
                    min: this._startOfPeriod,
                    max: this._endOfPeriod,
                    boundaryGap: false,
                    axisLabel: {
                        hideOverlap: true,
                        //rotate: 25,
                        interval: this._intervalConfig!.millis,
                        fontSize: 10,
                        formatter: {
                            year: "{yyyy}",
                            month: "{MMMM} '{yy}",
                            day: "{MMM} {d}th",
                            hour: "{HH}:{mm}",
                            minute: "{HH}:{mm}",
                            second: "{HH}:{mm}:{ss}",
                            millisecond: "{d}-{MMM} {HH}:{mm}",
                            // @ts-ignore
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
                        axisPointer: {
                            show: true, // Ensure it's visible
                            type: "line", // Only a line
                            label: { show: false }, // Hide label
                            triggerTooltip: false
                        },
                    },
                    {
                        type: 'value',
                        show: (this.attributeConfig?.rightAxisAttributes?.length || 0) > 0,
                        axisLine: { lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}},
                        boundaryGap: ['10%', '10%'],
                        scale: true,
                        min: (this.chartOptions?.options as any)?.scales?.y1?.min,
                        max: (this.chartOptions?.options as any)?.scales?.y1?.max
                    }
                    ],
                dataZoom: [
                    {
                        type: 'inside',
                        start: 0,
                        end: 100,
                        minValueSpan: this._intervalConfig!.millis
                    }
                ],
                series: [],
            };

            // Initialize echarts instance
            this._chart = echarts.init(this._chartElem);
            // Set chart options to default
            this._chart.setOption(this._chartOptions);
            this._toggleChartEventListeners(true);
        }

        if (changedProperties.has("_data")) {
            //Update chart to data from set period
            if (this._chart && this._data[0] && this._data[0].data.length > 0) {
                this._updateChartData();
            }
        }

        this.onCompleted().then(() => {
            this.dispatchEvent(new OrAttributeBarChartRenderEvent('rendered'));
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
                const attributeList = this.shadowRoot.getElementById("attribute-list");
                if(attributeList) {
                    attributeList.style.gap = bottomLegend ? "4px 12px" : "";
                    attributeList.style.maxHeight = bottomLegend ? "90px" : "";
                    attributeList.style.flexFlow = bottomLegend ? "row wrap" : "column nowrap";
                    attributeList.style.padding = bottomLegend ? "0" : "5px 0";
                }
                this.shadowRoot.querySelectorAll(".attribute-list-item").forEach((item: Element) => {
                    (item as HTMLElement).style.minHeight = bottomLegend ? "0px" : "44px";
                    (item as HTMLElement).style.paddingLeft = bottomLegend ? "" : "16px";
                    (item.children[1] as HTMLElement).style.flexDirection = bottomLegend ? "row" : "column";
                    (item.children[1] as HTMLElement).style.gap = bottomLegend ? "4px" : "";
                });
                this._chart?.resize();
            }
        }
    }

    render() {
        const disabled =  this._loading || this._latestError;

        return html`
            <div id="container">
                    ${when(this._loading, () => html`
                        <div style="position: absolute; height: 100%; width: 100%;">
                            <or-loading-indicator ?overlay="false"></or-loading-indicator>
                        </div>
                    `)}
                    ${when(this._latestError, () => html`
                        <div style="position: absolute; height: 100%; width: 100%; display: flex; justify-content: center; align-items: center;">
                            <or-translate .value="${this._latestError || "errorOccurred"}"></or-translate>
                        </div>
                    `)}
                    ${when(Object.keys(this.attributeConfig || {}).filter(key => key.startsWith('method')).every(key => (this.attributeConfig as any)[key].length === 0), () => html`
                        <div style="position: inherit; height: 100%; width: 100%; display: flex; justify-content: center; align-items: center; z-index: 1; pointer-events: none;">
                            <or-translate .value="${'dashboard.selectMethod'}"></or-translate>
                        </div>
                    `)}
                    ${when(this._data?.every(entry => entry.data.length === 0), () => html`
                        <div style="position: inherit; height: 100%; width: 100%; display: flex; justify-content: center; align-items: center; z-index: 1; pointer-events: none;">
                            <or-translate .value="${'dashboard.noData'}"></or-translate>
                        </div>
                    `)}
                <div id="chart-container">
                    <div id="chart" style="visibility: ${disabled ? 'hidden' : 'visible'}"></div>
                </div>
                
                ${(this.timestampControls || this.showLegend) ? html`
                    <div id="chart-controls">
                        <div id="controls">
                                ${this.timePrefixKey && this.timePrefixOptions && this.timeWindowKey && this.timeWindowOptions ? html`
                                    ${this.timestampControls ? html`
                                        <div class="period-controls">
                                            <!-- Time prefix selection -->
                                            ${getContentWithMenuTemplate(
                                                    html`<or-mwc-input .type="${InputType.BUTTON}" label="${this.timeframe ? "dashboard.customTimeSpan" : this.timePrefixKey}" ?disabled="${!!this.timeframe}"></or-mwc-input>`,
                                                    this.timePrefixOptions.map((option) => ({ value: option } as ListItem)),
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
                                                    html`<or-mwc-input .type="${InputType.BUTTON}" label="${this._isCustomWindow ? "timeframe" : this.timeWindowKey}" ?disabled="${!!this.timeframe}"></or-mwc-input>`,
                                                    Array.from(this.timeWindowOptions!.keys()).map((key) => ({ value: key } as ListItem)),
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
                                            <!-- Interval selection -->
                                            ${getContentWithMenuTemplate(
                                                    html`<or-mwc-input .type="${InputType.BUTTON}" label="interval: ${this._intervalConfig?.displayName}" ?disabled="${false}"></or-mwc-input>`,
                                                    Array.from(this.intervalOptions?.keys()).map((key) => ({ value: key } as ListItem)),
                                                    this.interval,
                                                    (value: string | string[]) => {
                                                        this.interval = value.toString();
                                                    },
                                                    undefined,
                                                    undefined,
                                                    undefined,
                                                    true
                                            )}
                                            
                                        </div>
                                        <div class="navigate" style = "text-align: right">
                                            <!-- Scroll left button -->
                                            <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-left" @click="${() => this._shiftTimeframe(this.timeframe? this.timeframe[0] : new Date(this._startOfPeriod!),this.timeframe? this.timeframe[1] : new Date(this._endOfPeriod!), this.timeWindowKey!, "previous")}"></or-icon>
                                            <!-- Scroll right button -->
                                            <or-icon class="button button-icon" ?disabled="${disabled}" icon="chevron-right" @click="${() => this._shiftTimeframe(this.timeframe? this.timeframe[0] : new Date(this._startOfPeriod!),this.timeframe? this.timeframe[1] : new Date(this._endOfPeriod!), this.timeWindowKey!, "next")}"></or-icon>
                                            <!-- Button that opens custom time selection or restores to widget setting-->
                                            <or-icon class="button button-icon" ?disabled="${disabled}" icon="${this.timeframe ? 'restore' : 'calendar-clock'}" @click="${() => this.timeframe ? (this._isCustomWindow = false, this.timeframe = undefined)  : this._openTimeDialog(this._startOfPeriod, this._endOfPeriod)}"></or-icon>
                                        </div>
                                    ` : html`
                                        <div style = "display: ruby; flex-direction: column; align-items: center">
                                        <or-mwc-input .type="${InputType.BUTTON}" label="${this.timePrefixKey}" disabled="true"></or-mwc-input>
                                        <or-mwc-input .type="${InputType.BUTTON}" label="${this.timeWindowKey}" disabled="true"></or-mwc-input>
                                        </div>
                                    `}
                                ` : undefined}
                            
                            ${this.timeframe ? html`
                                <div style="margin-left: 18px; font-size: 12px; display: flex; justify-content: flex-end;">
                                    <table style="text-align: right;">
                                        <thead>
                                        <tr>
                                            <th style="font-weight: normal; text-align: right;">${i18next.t('from')}:</th>
                                            <th style="font-weight: normal; text-align: right;">${moment(this.timeframe[0]).format("lll")}</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <tr>
                                            <td style="text-align: right;">${i18next.t('to')}:</td>
                                            <td style="text-align: right;">${moment(this.timeframe[1]).format("lll")}</td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </div>
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
                                    if(asset) {
                                        const colourIndex = index % this.colors.length;
                                        const color = this.attributeColors.find(x => x[0].id === asset.id && x[0].name === attr.name)?.[1];
                                        const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attr.name, attr);
                                        const label = Util.getAttributeLabel(attr, descriptors[0], asset.type, true);
                                        const axisNote = (this.attributeConfig?.rightAxisAttributes?.find(ar => asset.id === ar.id && attr.name === ar.name)) ? i18next.t('right') : undefined;
                                        const bgColor = ( color ?? this.colors[colourIndex] ) || "";
                                        //Find which aggregation methods are active
                                        const methodList: { data: string }[] = Object.entries(this.attributeConfig || {})
                                                .filter(([key]) => key.includes('method'))
                                                .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
                                                .reduce<{ data: string }[]>((list, [key, attributeRefs]) => {
                                                    const isActive = attributeRefs.some(
                                                            (ref: AttributeRef) => ref.id === asset.id && ref.name === attr.name
                                                    );
                                                    if (isActive) {
                                                        list.push({ data: `(${i18next.t(key)})` });
                                                    }
                                                    return list;
                                                }, []);

                                        return html`
                                            <div class="attribute-list-item ${this.denseLegend ? 'attribute-list-item-dense' : undefined}">
                                                <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[assetIndex].type), undefined, undefined, bgColor.split('#')[1])}</span>
                                                <div class="attribute-list-item-label ${this.denseLegend ? 'attribute-list-item-label-dense' : undefined}">
                                                    <div style="display: flex; justify-content: space-between;">
                                                        <span style="font-size:12px; ${this.denseLegend ? 'margin-right: 8px' : undefined}">${this.assets[assetIndex].name}</span>
                                                        ${when(axisNote, () => html`<span style="font-size:12px; color:grey">(${axisNote})</span>`)}
                                                    </div>
                                                    <span style="font-size:12px; color:grey; white-space:pre-line;">${label} <br> ${methodList.map(item => item?.data).join('\n')}</span>
                                                </div>
                                            </div>
                                        `;
                                    }
                                })}
                            </div>
                        ` : undefined)}
                    </div>
                ` : undefined}
            </div>
        `;
    }

    protected _openTimeDialog(startTimestamp?: number, endTimestamp?: number) {
        this._isCustomWindow = true;
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
                    if(startRef.value?.value && endRef.value?.value) {
                        this.timeframe = [new Date(startRef.value.value), new Date(endRef.value.value)];
                    }
                }
            }])
        )
    }

    async onCompleted() {
        await this.updateComplete;
    }

    protected _cleanup() {
        if (this._chart) {
            //('cleanup found _chart exists so disposing');
            this._toggleChartEventListeners(false);
            this._chart.dispose();
            this._chart = undefined;
            this.requestUpdate();
        }
    }

    public static getDefaultTimePrefixOptions(): string[] {
        return ["this", "last"];
    }

    public static getDefaultTimeWindowOptions(): Map<string, [moment.unitOfTime.DurationConstructor, number]> {
        return new Map<string, [moment.unitOfTime.DurationConstructor, number]>([
            ["hour", ['hours', 1]],
            ["6Hours", ['hours', 6]],
            ["24Hours", ['hours', 24]],
            ["day", ['days', 1]],
            ["7Days", ['days', 7]],
            ["week", ['weeks', 1]],
            ["30Days", ['days', 30]],
            ["month", ['months', 1]],
            ["365Days", ['days', 365]],
            ["year", ['years', 1]]
        ]);
    }

    public static getDefaultIntervalOptions(): Map<string, IntervalConfig> {
        return new Map<string, IntervalConfig>([
            ["auto", {displayName:"auto", steps: 1, orFormat: DatapointInterval.MINUTE, momentFormat: "minutes", millis: 60000}],
            ["one", {displayName:"one", steps:1, orFormat: DatapointInterval.MINUTE,momentFormat:"minutes", millis: 60000}],
            ["1Minute", {displayName:"1Minute", steps:1, orFormat:DatapointInterval.MINUTE,momentFormat:"minutes", millis: 60000}],
            ["5Minutes", {displayName:"5Minutes", steps:5, orFormat:DatapointInterval.MINUTE,momentFormat:"minutes", millis: 300000}],
            ["30Minutes", {displayName:"30Minutes", steps:30, orFormat:DatapointInterval.MINUTE,momentFormat:"minutes", millis: 1800000}],
            ["hour", {displayName:"hour", steps:1, orFormat:DatapointInterval.HOUR,momentFormat:"hours", millis: 3600000}],
            ["day", {displayName:"day", steps:1, orFormat:DatapointInterval.DAY,momentFormat:"days", millis: 86400000}],
            ["week", {displayName:"week", steps:1, orFormat:DatapointInterval.WEEK,momentFormat:"weeks", millis: 604800000}],
            ["month", {displayName:"month", steps:1, orFormat:DatapointInterval.MONTH,momentFormat:"months", millis: 2592000000}],
            ["year", {displayName:"year", steps:1, orFormat:DatapointInterval.MINUTE,momentFormat:"years", millis: 31536000000}]
        ]);
    };


    protected _getTimeSelectionDates(timePrefixSelected: string, timeWindowSelected: string): [Date, Date] {
        let startDate = moment();
        let endDate = moment();

        const timeWindow: [moment.unitOfTime.DurationConstructor, number] | undefined = this.timeWindowOptions!.get(timeWindowSelected);

        if (!timeWindow) {
            throw new Error(`Unsupported time window selected: ${timeWindowSelected}`);
        }

        const [unit , value]: [moment.unitOfTime.DurationConstructor, number] = timeWindow;

        switch (timePrefixSelected) {
            case "this":
                if (value == 1) { // For singulars like this hour
                    startDate = moment().startOf(unit);
                    endDate = moment().endOf(unit);
                } else { // For multiples like this 5 min, put now in the middle
                    startDate = moment().subtract(value*0.5, unit);
                    endDate = moment().add(value*0.5, unit);
                }
                break;
            case "last":
                startDate = moment().subtract(value, unit).startOf(unit);
                if (value == 1) { // For singulars like last hour
                    endDate = moment().startOf(unit);
                } else { //For multiples like last 5 min
                    endDate = moment();
                }
                break;
        }
        return [startDate.toDate(), endDate.toDate()];
    }


    protected _shiftTimeframe(currentStart: Date, currentEnd: Date, timeWindowSelected: string, direction: string) {
        const timeWindow = this.timeWindowOptions!.get(timeWindowSelected);

        if (!timeWindow) {
            throw new Error(`Unsupported time window selected: ${timeWindowSelected}`);
        }

        const [unit, value] = timeWindow;
        let newStart = moment(currentStart);
        direction === "previous" ? newStart.subtract(value, unit as moment.unitOfTime.DurationConstructor) : newStart.add(value, unit as moment.unitOfTime.DurationConstructor);
        let newEnd = moment(currentEnd)
        direction === "previous" ? newEnd.subtract(value, unit as moment.unitOfTime.DurationConstructor) : newEnd.add(value, unit as moment.unitOfTime.DurationConstructor);
        this.timeframe = [newStart.toDate(), newEnd.toDate()];
    }

    protected _validateInterval(start: number, end:number, selectedInterval: string): IntervalConfig {
        const diffInHours = (end - start) / 1000 / 60 / 60;
        if (selectedInterval == "auto") {
            //Returns amount of steps, interval size and moment.js time format
            if(diffInHours <= 1) {
                return { displayName: "auto(5m)", steps: 5, orFormat: DatapointInterval.MINUTE, momentFormat: 'minutes', millis: 300000 };
            } else if(diffInHours <= 6) {
                return {displayName: "auto(30m)", steps: 30, orFormat: DatapointInterval.MINUTE, momentFormat:'minutes', millis: 1800000};
            } else if(diffInHours <= 24) { // hour if up to one day
                return {displayName: "auto(1hr)", steps: 1, orFormat: DatapointInterval.HOUR, momentFormat:'hours', millis: 3600000};
            } else if(diffInHours <= 744) { // one day if up to one month
                return {displayName: "auto(day)", steps: 1, orFormat: DatapointInterval.DAY, momentFormat:'days', millis: 86400000};
            } else if(diffInHours <= 8760) { // one week if up to 1 year
                return {displayName: "auto(week)", steps: 1, orFormat: DatapointInterval.WEEK, momentFormat:'weeks', millis: 604800000};
            } else { // one month if more than a year
                return {displayName: "auto(month)", steps: 1, orFormat: DatapointInterval.MONTH, momentFormat:'months', millis: 2592000000};
            }
        } else if (selectedInterval == "one") {
            //Set interval to total time span
            const millis = this._endOfPeriod!-this._startOfPeriod!
            const steps = Math.ceil(millis / 60000)
            return {displayName: "one", steps: steps, orFormat: DatapointInterval.MINUTE, momentFormat:'minutes', millis: millis};
        }
        // Otherwise, check if select interval is a valid combination with set time window
        const intervalProp: IntervalConfig = this.intervalOptions!.get(selectedInterval)!
        const selectedIntervalHours = moment.duration(intervalProp.steps, intervalProp.momentFormat).asHours();

        if (selectedIntervalHours <= diffInHours) {
            return intervalProp; // Already valid so quit
        }


        //If no selected interval is larger than timeframe, switch to the first next valid timeframe.
        const intervalOptions = Array.from(this.intervalOptions!.entries());

        for (let i = intervalOptions.length - 1; i >= 0; i--) {
            const [key, value] = intervalOptions[i];
            const intervalHours = moment.duration(value.steps, value.momentFormat).asHours();

            if (intervalHours <= diffInHours) {
                return value; // Found a valid option
            }
        }
        // If no valid option is found, return the smallest available option
        return intervalOptions[0][1];
    }

    protected async _loadData() {
        if ( this._data  || !this.assetAttributes || !this.assets || (this.assets.length === 0) || (this.assetAttributes.length === 0) || !this.datapointQuery) {
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
        this._startOfPeriod = this.timeframe ? this.timeframe[0].getTime() : dates[0].getTime();
        this._endOfPeriod = this.timeframe ? this.timeframe[1].getTime() : dates[1].getTime();
        const data: any = [];
        let promises;

        try {
                this._dataAbortController = new AbortController();
                promises = this.assetAttributes.map(async ([assetIndex, attribute], index) => {

                    const asset = this.assets[assetIndex];
                    const shownOnRightAxis = !!this.attributeConfig?.rightAxisAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const color =  this.attributeColors.find(x => x[0].id === asset.id && x[0].name === attribute.name)?.[1];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
                    const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                    const unit = Util.resolveUnits(Util.getAttributeUnits(attribute, descriptors[0], asset.type));
                    const colourIndex = index % this.colors.length;
                    const options = { signal: this._dataAbortController?.signal };


                    // Map calculation methods to their corresponding attribute arrays and formulas
                    const methodMapping: { [key: string]: { active: boolean; formula: AssetDatapointIntervalQueryFormula } } = {
                        AVG: { active: !!this.attributeConfig?.methodAvgAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.AVG },
                        COUNT: { active: !!this.attributeConfig?.methodCountAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.COUNT },
                        DELTA: { active: !!this.attributeConfig?.methodDeltaAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.DELTA },
                        MAX: { active: !!this.attributeConfig?.methodMaxAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.MAX },
                        MEDIAN: { active: !!this.attributeConfig?.methodMedianAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.MEDIAN },
                        MIN: { active: !!this.attributeConfig?.methodMinAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.MIN },
                        MODE: { active: !!this.attributeConfig?.methodModeAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.MODE },
                        SUM: { active: !!this.attributeConfig?.methodSumAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name), formula: AssetDatapointIntervalQueryFormula.SUM }
                        };

                    // Iterate over the mapping, make a dataset for every active method
                    for (const [key, value] of (Object.entries(methodMapping)).sort(([keyA], [keyB]) => keyA.localeCompare(keyB))) {
                        if (value.active) {
                            //Initiate query Attribute Data
                            let dataset = await this._loadAttributeData(asset, attribute, color ?? this.colors[colourIndex], this._startOfPeriod!, this._endOfPeriod!, value.formula, asset.name + " " + label + " \n" + i18next.t(value.formula), options, unit);
                            (dataset as any).assetId = asset.id;
                            (dataset as any).attrName = attribute.name;
                            (dataset as any).unit = unit;
                            (dataset as any).yAxisIndex = shownOnRightAxis ? '1' : '0';
                            (dataset as any).color = color ?? this.colors[colourIndex];

                            data.push(dataset);

                        }
                    }


                });


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


    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, color: string, from: number, to: number, formula: AssetDatapointIntervalQueryFormula, label?: string, options?: any, unit?: any) {

        const dataset = {
            name: label,
            type: 'bar',
            data: [] as [any, any][],
            stack: this.stacked ? `${formula}` : undefined,
            barWidth: undefined,
            lineStyle: {
                color: color,
            },
            tooltip: {
                // @ts-ignore
                valueFormatter: value => value + ' ' + unit
            },
            emphasis: {
            itemStyle: {
                borderColor: this._style.getPropertyValue("--internal-or-chart-graph-point-hover-border-color"), // Highlighted border color
                borderWidth: 2, // Makes the emphasis stand out
                opacity: 1, // Ensures full visibility
                shadowBlur: 10, // Adds a glow effect
                shadowColor: this._style.getPropertyValue("--internal-or-chart-graph-point-hover-border-color") // Glow color when highlighted
                },
            },
            label: {
                show: true,
                align: 'left',
                verticalAlign: 'middle',
                position: 'top',
                fontStyle: 'italic',
                fontSize: 10,
                rotate: '90',
                distance: 15,
                formatter: (params: { dataIndex: number; value: number }): string => {
                    // Show labels only for the first index (index 0)
                    return params.dataIndex === 0 ? `${formula}` : '';  //Or make it i18next.t(formula) to display longer text
                }}
        }


        if (asset.id && attribute.name && this.datapointQuery) {
            let response: GenericAxiosResponse<ValueDatapoint<any>[]>;
            const query = JSON.parse(JSON.stringify(this.datapointQuery)); // recreating object, since the changes shouldn't apply to parent components; only or-chart itself.


            query.fromTimestamp = this._startOfPeriod;
            query.toTimestamp = this._endOfPeriod;
            query.formula = formula;

            this._intervalConfig = this._validateInterval(this._startOfPeriod!,this._endOfPeriod!,  this.interval!);
            query.interval = (this._intervalConfig!.steps.toString() + " " + this._intervalConfig!.orFormat.toString()); // for example: "5 minute"

            response = await manager.rest.api.AssetDatapointResource.getDatapoints(asset.id, attribute.name, query, options);


            let data: ValueDatapoint<any>[] = [];

            if (response.status === 200) {
                if (response.data.length > 0 && (response.data.length === 1 || (response.data[1].x! - response.data[0].x!) == this._intervalConfig!.millis) ) { //only push through if returned data interval is equal to requested interval or if if only one datapoint is returned
                    data = response.data
                        .filter(value => value.y !== null && value.y !== undefined)
                        .map(point => ({x: point.x, y: point.y} as ValueDatapoint<any>))
                    // map to dataset and position to middle of interval instead of start time
                    dataset.data = data.map(point => [(point.x ?? 0) + 0.5 * this._intervalConfig!.millis, +point.y.toFixed(this.decimals)]);
                } else {
                    console.log("Returned data interval is larger than requested interval, data will not be shown.")
                    dataset.data = [];
                }
            }



        }
        return dataset;
    }


    protected _updateChartData() {

        //---Recreate interval highlight background---
        this._markAreaData = [];
        //Start at the first interval for which data is available
        const markStartPoint = Math.min(this._data?.find(entry => entry.data.length > 0)?.data?.[0][0]) - 0.5 * this._intervalConfig!.millis
        // End at the last available interval
        const latestIntervalData = Math.max(...this._data!.map(dataset => dataset.data.at(-1)?.[0] || 0));


        // For months or years, use moment to get the right non-constant interval sizes
        if (this._intervalConfig!.orFormat == DatapointInterval.MONTH) {
            let current = moment(markStartPoint).startOf('month').valueOf()
            while (current <= latestIntervalData) {
                this._markAreaData.push([{ xAxis: current }, { xAxis: current + this._intervalConfig!.millis }]);
                current = moment(current).add(1, 'month').valueOf()
            } // For years, use moment to get the right non constant interval sizes
        } else if (this._intervalConfig!.orFormat == DatapointInterval.YEAR) {
            let current = moment(markStartPoint).startOf('year').valueOf()
            while (current <= latestIntervalData) {
                this._markAreaData.push([{ xAxis: current }, { xAxis: current + this._intervalConfig!.millis }]);
                current = moment(current).add(1, 'year').valueOf()
            }  //Otherwise arithmetic calculate highlights when reasonable amount of intervals are to be shown (avoiding clutter and performance issues)
        } else if ((this._endOfPeriod!-this._startOfPeriod!)/this._intervalConfig!.millis < 300) {
            let current = markStartPoint;
            while (current <= latestIntervalData) {
                this._markAreaData.push([{ xAxis: current }, { xAxis: current + this._intervalConfig!.millis }]);
                current += this._intervalConfig!.millis * 2;
            }
        }

        // Update chart
        this._chart!.setOption({
            series: [
                ...this._data!.map(series => ({
                    ...series
                })),
                {
                    name: "Background1",
                    type: "line",
                    data: [],
                    markArea: {
                        silent: true,
                        z: 0,
                        itemStyle: { color: "rgba(0, 0, 0, 0.05)"},
                        data: this._markAreaData
                    },
                },
                {
                    name: "Background2",
                    type: "line",
                    data: [],
                    markArea: {
                        silent: true,
                        z: 0,
                        itemStyle: { color: "rgba(0, 0, 0, 0)" ,borderColor: this._style.getPropertyValue("--internal-or-chart-graph-point-hover-border-color"), borderWidth: 0.1},
                        data: this._markAreaData.map(group => group.map(entry => ({ ...entry, xAxis: entry.xAxis + this._intervalConfig!.millis })))
                    },
                }

            ]
        });
    }


    protected _toggleChartEventListeners(connect: boolean){
        if (connect) {
            //Connect event listeners
            // Make chart size responsive
            window.addEventListener("resize", () => this._chart!.resize());
            this._zoomHandler = this._chart!.on('datazoom', () => {this.updateBars();});
            this._containerResizeObserver = new ResizeObserver(() => {this.applyChartResponsiveness(); this.updateBars();});
            if (this.shadowRoot) {
                this._containerResizeObserver.observe(this.shadowRoot!.getElementById('container') as HTMLElement);
            }
        }
        else if (!connect) {
            //Disconnect event listeners
            this._chart!.off('datazoom', this._zoomHandler);
            this._containerResizeObserver?.disconnect();
            this._containerResizeObserver = undefined;
        }





    }

    protected updateBars() {
        //Function to update dynamic bar positions and widths
        if (this._data) {
            const barAmount = this._data.length
            if (this._data.every(entry => entry.data.length < 1)) {
                return;
            } else if (this._data.every(entry => entry.data.length == 1) && this._intervalConfig!.millis == (this._endOfPeriod! - this._startOfPeriod!)) {
                //For single intervals, eCharts convertToPixel bugs.
               this._data.forEach((value) => {
                   let width = 50 / (barAmount * 1.2)
                   value.barWidth = `${width}%`;
               });

             } else {

                this._data.forEach((value) => {
                    const startTime = this._data?.find(entry => entry.data.length > 0)?.data?.[0][0]; //find some dataset that has a timestamp
                    const endTime = startTime + this._intervalConfig!.millis;
                    const pixelStart = this._chart!.convertToPixel({xAxisIndex: 0}, startTime);
                    const pixelEnd = this._chart!.convertToPixel({xAxisIndex: 0}, endTime);
                    const magicRatio = 0.8; //fill ratio
                    const availableWidth = (pixelEnd - pixelStart) * magicRatio;
                    value.barWidth = availableWidth / barAmount;

                });
            }

            this._updateChartData()

        }

    }

}
