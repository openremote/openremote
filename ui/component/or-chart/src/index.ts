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
import {ECharts, EChartsOption, init, graphic} from "echarts";
import {

    TimeUnit,

} from "chart.js";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-loading-indicator";
import moment from "moment";
import {OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import "chartjs-adapter-moment";
import {GenericAxiosResponse, isAxiosError} from "@openremote/rest";
import {OrAttributePicker, OrAttributePickerPickedEvent} from "@openremote/or-attribute-picker";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {cache} from "lit/directives/cache.js";
import {debounce, throttle} from "lodash";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import { when } from "lit/directives/when.js";
import {createRef, Ref, ref } from "lit/directives/ref.js";

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
    #chart {
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

    @property({type: Array})
    public colorPickedAttributes: Array<{ attributeRef: AttributeRef; color: string }> = [];

    @property({type: Object})
    public attributeSettings: {
        rightAxisAttributes: AttributeRef[],
        smoothAttributes: AttributeRef[],
        steppedAttributes: AttributeRef[],
        areaAttributes: AttributeRef[],
        faintAttributes: AttributeRef[],
        extendedAttributes: AttributeRef[],
    } = {
        rightAxisAttributes: [],
        smoothAttributes: [],
        steppedAttributes: [],
        areaAttributes: [],
        faintAttributes: [],
        extendedAttributes: [],
    };


    @property()
    public dataProvider?: (startOfPeriod: number, endOfPeriod: number, timeUnits: TimeUnit, stepSize: number) => Promise<[]>

    @property({type: Array})
    public colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    @property({type: Object})
    public readonly datapointQuery!: AssetDatapointQueryUnion;

    @property({type: Object})
    public config?: OrChartConfig;

    @property({type: Object})
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
    protected timePrefixOptions?: string[];

    @property()
    public timeWindowOptions?: Map<string, [moment.unitOfTime.DurationConstructor, number]>;

    @property()
    public timePrefixKey?: string;

    @property()
    public timeWindowKey?: string;




    @property()
    public showLegend: boolean = true;

    @property()
    public denseLegend: boolean = false;

    @property()
    public showZoomBar: boolean = true;

    @property()
    public showToolBox: boolean = true;

    @property()
    public showSymbolMaxDatapoints: number = 30;

    @property()
    public maxConcurrentDatapoints: number = 100;

    @property()
    protected _loading: boolean = false;

    @property()
    protected _zoomChanged: boolean = false;

    @property()
    protected _data?: ValueDatapoint<any>[];

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLDivElement;
    protected _chartOptions: EChartsOption = {};
    protected _chart?: ECharts;
    protected _style!: CSSStyleDeclaration;
    protected _startOfPeriod?: number;
    protected _endOfPeriod?: number;
    protected _zoomStartOfPeriod?: number;
    protected _zoomEndOfPeriod?: number;
    protected _timeUnits?: TimeUnit;
    protected _stepSize?: number;
    protected _latestError?: string;
    protected _dataAbortController?: AbortController;
    protected _zoomHandler?: any;
    protected _resizeHandler?: any;
    protected _containerResizeObserver?: ResizeObserver;

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

        const reloadData = changedProperties.has('colorPickedAttributes') || changedProperties.has("datapointQuery") || changedProperties.has("timeframe") || changedProperties.has("timePrefixKey") || changedProperties.has("timeWindowKey")||
            changedProperties.has("attributeSettings") || changedProperties.has("assetAttributes") || changedProperties.has("realm") || changedProperties.has("dataProvider");

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

            let bgColor = this._style.getPropertyValue("--internal-or-chart-graph-fill-color").trim();
            const opacity = Number(this._style.getPropertyValue("--internal-or-chart-graph-fill-opacity").trim());
            if (!isNaN(opacity)) {
                if (bgColor.startsWith("#") && (bgColor.length === 4 || bgColor.length === 7)) {
                    bgColor += (bgColor.length === 4 ? Math.round(opacity * 255).toString(16).substr(0, 1) : Math.round(opacity * 255).toString(16));
                } else if (bgColor.startsWith("rgb(")) {
                    bgColor = bgColor.substring(0, bgColor.length - 1) + opacity;
                }
            }



            this._chartOptions = {
                animation: false,
                grid: {
                    show: true,
                    backgroundColor: this._style.getPropertyValue("--internal-or-asset-tree-background-color"),
                    borderColor: this._style.getPropertyValue("--internal-or-chart-text-color"),
                    left: 50,//'5%', // 5% padding
                    right: 50,//'5%',
                    top: this.showToolBox ? 28 : 10,
                    bottom: this.showZoomBar ? 68 : 20
                },
                backgroundColor: this._style.getPropertyValue("--internal-or-asset-tree-background-color"),
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'cross'
                    },
                },
                toolbox: {},
                xAxis: {
                    type: 'time',
                    axisLine: {
                        onZero: false,
                        lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}
                    },
                    splitLine: {show: true},
                    min: this._startOfPeriod,
                    max: this._endOfPeriod,
                    axisLabel: {
                        showMinLabel: true,
                        showMaxLabel: true,
                        hideOverlap: true,
                        fontSize: 10,
                        formatter: {
                             year: '{yyyy}-{MMM}',
                             month: '{yy}-{MMM}',
                             day: '{d}-{MMM}',
                             hour: '{HH}:{mm}',
                             minute: '{HH}:{mm}',
                             second: '{HH}:{mm}:{ss}',
                             millisecond: '{d}-{MMM} {HH}:{mm}',
                            // @ts-ignore
                             none: '{MMM}-{dd} {HH}:{mm}'
                        }
                    }
                },
                yAxis: [
                    {
                        type: 'value',
                        axisLine: { lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}},
                        boundaryGap: ['10%', '10%'],
                        scale: true,
                        min: this.chartOptions.options.scales.y.min ? this.chartOptions.options.scales.y.min : undefined, //NOG FIXEN MET MERGEN VAN CHARTOPTIONS
                        max: this.chartOptions.options.scales.y.max ? this.chartOptions.options.scales.y.max : undefined
                    },
                    {
                        type: 'value',
                        show: this.attributeSettings.rightAxisAttributes.length > 0,
                        axisLine: { lineStyle: {color: this._style.getPropertyValue("--internal-or-chart-text-color")}},
                        boundaryGap: ['10%', '10%'],
                        scale: true,
                        min: this.chartOptions.options.scales.y1.min ? this.chartOptions.options.scales.y1.min : undefined,
                        max: this.chartOptions.options.scales.y1.max ? this.chartOptions.options.scales.y1.max : undefined
                    }
                    ],
                dataZoom: [
                    {
                        type: 'inside',
                        start: 0,
                        end: 100
                    }
                ],
                series: [],
            };

            // Add dataZoom bar if enabled
            if(this.showZoomBar) {
                (this._chartOptions!.dataZoom! as any[]).push({
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
                            color: this._style.getPropertyValue("--internal-or-chart-graph-fill-color"),
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
                })
            }

            // Add toolbox if enabled
            if(this.showToolBox) {
                this._chartOptions!.toolbox! = {
                    right: 45,
                    top: 0,
                    feature: {
                        dataView: {readOnly: true},
                        //magicType: {
                        //    type: ['line', 'bar']
                        //},
                        saveAsImage: {}
                    }
                }
            }

            // Initialize echarts instance
            this._chart = init(this._chartElem);
            // Set chart options to default
            this._chart.setOption(this._chartOptions);
            this._toggleChartEventListeners(true);
        }

        if (changedProperties.has("_data")) {
            //Update chart to data from set period
            this._updateChartData();
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

        const disabled = false; // TEMP EDIT this._loading || this._latestError;
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
                    <div id="chart" style="visibility: ${disabled ? 'hidden' : 'visible'}"></div>
                </div>
                
                ${(this.timestampControls || this.attributeControls || this.showLegend) ? html`
                    <div id="chart-controls">
                        <div id="controls">
                            <div class="period-controls">
                                ${this.timePrefixKey && this.timePrefixOptions && this.timeWindowKey && this.timeWindowOptions ? html`
                                    ${this.timestampControls ? html`
                                        <!-- Scroll left button -->
                                        <or-mwc-input .type="${InputType.BUTTON}" icon="chevron-left" @or-mwc-input-changed="${() => this._shiftTimeframe(this.timeframe? this.timeframe[0] : new Date(this._startOfPeriod!), this.timeWindowKey!, "previous")}"></or-mwc-input>
                                        <!-- Time prefix selection -->
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input .type="${InputType.BUTTON}" label="${this.timeframe ? "dashboard.customTimeSpan" : this.timePrefixKey}"></or-mwc-input>`,
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
                                                html`<or-mwc-input .type="${InputType.BUTTON}" label="${this.timeWindowKey}"></or-mwc-input>`,
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
                                        <!-- Scroll right button -->
                                        <or-mwc-input .type="${InputType.BUTTON}" icon="chevron-right" @or-mwc-input-changed="${() => this._shiftTimeframe(this.timeframe? this.timeframe[0] : new Date(this._startOfPeriod!), this.timeWindowKey!, "next")}"></or-mwc-input>
                                    ` : html`
                                        <or-mwc-input .type="${InputType.BUTTON}" label="${this.timePrefixKey} ${this.timeWindowKey}" disabled="true"></or-mwc-input>
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
                                    const color = this.colorPickedAttributes.find(({ attributeRef }) => attributeRef.name === attr.name && attributeRef.id === asset.id)?.color;
                                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset!.type, attr.name, attr);
                                    const label = Util.getAttributeLabel(attr, descriptors[0], asset!.type, true);
                                    const axisNote = (this.attributeSettings.rightAxisAttributes.find(ar => asset!.id === ar.id && attr.name === ar.name)) ? i18next.t('right') : undefined;
                                    const bgColor = ( color ?? this.colors[colourIndex] ) || "";
                                    return html`
                                        <div class="attribute-list-item ${this.denseLegend ? 'attribute-list-item-dense' : undefined}" @mouseenter="${() => this.addDatasetHighlight(this.assets[assetIndex]!.id, attr.name)}" @mouseleave="${()=> this.removeDatasetHighlight()}">
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

    removeDatasetHighlight() {
        if(this._chart){
            let options = this._chart.getOption();
            if (options.series && Array.isArray(options.series)) {
                options.series.forEach(function (series) {
                    if (series.lineStyle.opacity == 0.2 || series.lineStyle.opacity == 0.99) {
                        series.lineStyle.opacity = 0.31;
                    } else {
                        series.lineStyle.opacity = 1;
                    }
                });
            }
            this._chart.setOption(options);
        }
    }

    addDatasetHighlight(assetId?:string, attrName?:string) {
        if (this._chart) {
            let options = this._chart.getOption();
            if (options.series && Array.isArray(options.series)) {
                options.series.forEach(function (series) {
                    if (series.assetId != assetId || series.attrName != attrName) {
                        if (series.lineStyle.opacity == 0.31) { // 0.31 is faint setting, 1 is normal
                            series.lineStyle.opacity = 0.2;
                        } else {
                            series.lineStyle.opacity = 0.3;
                        }
                    } else if (series.lineStyle.opacity == 0.31) { // extra highlight if selected is faint
                        series.lineStyle.opacity = 0.99;
                    }
                });
            }
            this._chart.setOption(options)
        }
    };




    async loadSettings(reset: boolean) {

        if(this.assetAttributes == undefined || reset) {
            this.assetAttributes = [];
        }

        if (!this.realm) {
            this.realm = manager.getRealm();
        }

        if (!this.timePrefixOptions) {
            this.timePrefixOptions = this._getDefaultTimePrefixOptions();
        }

        if (!this.timeWindowOptions) {
            this.timeWindowOptions = this._getDefaultTimeWindowOptions();
        }

        if (!this.timeWindowKey) {
            this.timeWindowKey = this.timeWindowOptions.keys().next().value.toString();
        }

        if (!this.timePrefixKey) {
            this.timePrefixKey = this.timePrefixOptions[1];
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
            //('cleanup found _chart exists so disposing');
            this._toggleChartEventListeners(false);
            this._chart.dispose();
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
        //('getAttributeOptionsOld triggered');
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




    protected _getDefaultTimePrefixOptions(): string[] {
        return ["this", "last"];
    }


    protected _getDefaultTimeWindowOptions(): Map<string, [moment.unitOfTime.DurationConstructor, number]> {
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
    };


    protected _getTimeWindowSelected(timePrefixSelected: string, timeWindowSelected: string): [Date, Date] {
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

    protected _shiftTimeframe(currentStart: Date, timeWindowSelected: string, direction: string) {
        const timeWindow = this.timeWindowOptions!.get(timeWindowSelected);

        if (!timeWindow) {
            throw new Error(`Unsupported time window selected: ${timeWindowSelected}`);
        }

        const [unit, value] = timeWindow;
        let newStart = moment(currentStart);

        direction === "previous" ? newStart.subtract(value, unit as moment.unitOfTime.DurationConstructor) : newStart.add(value, unit as moment.unitOfTime.DurationConstructor);

        let newEnd = moment(newStart).add(value, unit as moment.unitOfTime.DurationConstructor);

        this.timeframe = [newStart.toDate(), newEnd.toDate()];
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

        const dates: [Date, Date] = this._getTimeWindowSelected(this.timePrefixKey!, this.timeWindowKey!);

        if(!this._zoomChanged || !this._startOfPeriod || !this._endOfPeriod) {
            // If zoom has changed, we want to keep the previous start and end of period
            this._startOfPeriod = this.timeframe ? this.timeframe[0].getTime() : dates[0].getTime();
            this._endOfPeriod = this.timeframe ? this.timeframe[1].getTime() : dates[1].getTime();
        }

        const diffInHours = (this._endOfPeriod - this._startOfPeriod) / 1000 / 60 / 60;
        const intervalArr = this._getInterval(diffInHours);

        const stepSize: number = intervalArr[0];
        const interval: DatapointInterval = intervalArr[1];

        const lowerCaseInterval = interval.toLowerCase();
        this._timeUnits =  lowerCaseInterval as TimeUnit;
        this._stepSize = stepSize;
        const now = moment().toDate().getTime();
        let predictedFromTimestamp = now < this._startOfPeriod ? this._startOfPeriod : now;

        const data: any = [];
        let promises;

        try {
            if(this.dataProvider && !this._zoomChanged) {
                await this.dataProvider(this._startOfPeriod, this._endOfPeriod, (interval.toString() as TimeUnit), stepSize).then((dataset) => {
                    dataset.forEach((set) => { data.push(set); });
                });
            } else {
                this._dataAbortController = new AbortController();
                promises = this.assetAttributes.map(async ([assetIndex, attribute], index) => {

                    const asset = this.assets[assetIndex];
                    const shownOnRightAxis = !!this.attributeSettings.rightAxisAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const smooth = !!this.attributeSettings.smoothAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const stepped = !!this.attributeSettings.steppedAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const area = !!this.attributeSettings.areaAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const faint = !!this.attributeSettings.faintAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const extended = !!this.attributeSettings.extendedAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name);
                    const color = this.colorPickedAttributes.find(({ attributeRef }) => attributeRef.name === attribute.name && attributeRef.id === asset.id)?.color;
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attribute.name, attribute);
                    const label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                    const unit = Util.resolveUnits(Util.getAttributeUnits(attribute, descriptors[0], asset.type));
                    const colourIndex = index % this.colors.length;
                    const options = { signal: this._dataAbortController?.signal };
                    //Load Historic Data
                    let dataset = await this._loadAttributeData(asset, attribute, color ?? this.colors[colourIndex], this._startOfPeriod!, this._endOfPeriod!, false, smooth, stepped, area, faint, false, asset.name + " " + label, options, unit);
                    (dataset as any).assetId = asset.id;
                    (dataset as any).attrName = attribute.name;
                    (dataset as any).unit = unit;
                    (dataset as any).yAxisIndex = shownOnRightAxis ? '1' : '0';
                    (dataset as any).color = color ?? this.colors[colourIndex];
                    data.push(dataset);
                    //Load Predicted Data
                    dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, color ?? this.colors[colourIndex], predictedFromTimestamp, this._endOfPeriod!, true, smooth, stepped, area, faint, false , asset.name + " " + label + " " + i18next.t("predicted"), options, unit);
                    data.push(dataset);
                    //Load Extended Data
                    let bsNumber = 1; //inserted in from and to, however these are not used in _loadAttributeData anyway, the function references variables outside of it (bad practice)
                    if (extended) {
                        dataset = await this._loadAttributeData(this.assets[assetIndex], attribute, color ?? this.colors[colourIndex], bsNumber, bsNumber, false, false, false, area, faint, extended, asset.name + " " + label + " " + i18next.t("dashboard.lastKnown"), options, unit);
                        data.push(dataset);
                    }

                    //Is it actually efficient to query three times ? think this can be way more efficient.


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


    protected async _loadAttributeData(asset: Asset, attribute: Attribute<any>, color: string, from: number, to: number, predicted: boolean, smooth: boolean, stepped: boolean, area: boolean, faint: boolean, extended: boolean, label?: string, options?: any, unit?: any) {

        function rgba (color: string, alpha: number) {
            return `rgba(${parseInt(color.slice(-6,-4), 16)}, ${parseInt(color.slice(-4,-2), 16)}, ${parseInt(color.slice(-2), 16)}, ${alpha})`;
        }

        const dataset = {
            name: label,
            type: 'line',
            showSymbol: false,
            data: [] as [any, any][],
            sampling: 'lttb',
            lineStyle: {
                color: color,
                type: predicted ? [2, 4] : extended ? [0.8, 10] : undefined,
                opacity: faint ? 0.31 : 1,
            },
            itemStyle: {
                color: color
            },
            tooltip: {
                // @ts-ignore
                valueFormatter: value => value + unit
            },
            smooth: smooth,
            step: stepped ? 'end' : undefined,
            areaStyle: area ? {color: new graphic.LinearGradient(0, 0, 0, 1, [
                    {
                        offset: 0,
                        color: rgba(color, faint ? 0.1 : 0.5)
                    },
                    {
                        offset: 1,
                        color: rgba(color, 0)
                    }
                ])} as any : undefined,
        }


        if (asset.id && attribute.name && this.datapointQuery) {
            let response: GenericAxiosResponse<ValueDatapoint<any>[]>;
            const query = JSON.parse(JSON.stringify(this.datapointQuery)); // recreating object, since the changes shouldn't apply to parent components; only or-chart itself.

            if (!this._zoomChanged) {
                query.fromTimestamp = this._startOfPeriod;
                query.toTimestamp = this._endOfPeriod;
            } else {
                query.fromTimestamp = this._zoomStartOfPeriod;
                query.toTimestamp = this._zoomEndOfPeriod;
            }

            if(query.type == 'lttb') {
                // If the query type is lttb, we need to limit the amount of points to the maxConcurrentDatapoints
               query.amountOfPoints = this.maxConcurrentDatapoints;

            } else if(query.type === 'interval' && !query.interval) {
                const diffInHours = (this.datapointQuery.toTimestamp! - this.datapointQuery.fromTimestamp!) / 1000 / 60 / 60;
                const intervalArr = this._getInterval(diffInHours);
                query.interval = (intervalArr[0].toString() + " " + intervalArr[1].toString()); // for example: "5 minute"
            }


            if (predicted) {
                response = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(asset.id, attribute.name, query, options);
            } else  {
                    if (extended) {
                        // if request is for extended dataset, we want to get the last known value only
                        query.type = 'nearest';
                        query.timestamp = new Date().toISOString()
                    }

                    response = await manager.rest.api.AssetDatapointResource.getDatapoints(asset.id, attribute.name, query, options);
                }

            let data: ValueDatapoint<any>[] = [];

            if (response.status === 200) {
                data = response.data
                    .filter(value => value.y !== null && value.y !== undefined)
                    .map(point => ({ x: point.x, y: point.y } as ValueDatapoint<any>))

                dataset.data = data.map(point => [point.x, point.y]);
                dataset.showSymbol = data.length <= this.showSymbolMaxDatapoints;
            }


            if (extended) {
                if (dataset.data.length > 0) {
                    // Get the first datapoint's timestamp
                    const firstPointTime = new Date(dataset.data[0][0]).getTime();

                    // If the first point is earlier than startOfPeriod, use startOfPeriod as the starting timestamp
                    const startTimestamp = firstPointTime < query.fromTimestamp! ?
                        new Date(query.fromTimestamp!).toISOString() :
                        dataset.data[0][0];

                    // Use endOfPeriod if it's earlier than now, otherwise use the current time
                    const now = new Date().getTime();
                    const endTimestamp = query.toTimestamp! < now ?
                        new Date(query.toTimestamp!).toISOString() :
                        new Date().toISOString();
                    // Create a clean extended line by removing any existing points and adding just two points:
                    // One at the appropriate start time and one at the current time
                    dataset.data = [
                        [startTimestamp, dataset.data[0][1]],
                        [endTimestamp, dataset.data[0][1]]
                    ];
                }
            }
        }
        return dataset;
    }

    protected _onZoomChange(params: any) {
        this._zoomChanged = true;
        const { start: zoomStartPercentage, end: zoomEndPercentage } = params.batch?.[0] ?? params; // Events triggered by scroll and zoombar return different structures

        //Define the start and end of the period based on the zoomed area
        this._zoomStartOfPeriod = this._startOfPeriod! + ((this._endOfPeriod! - this._startOfPeriod!) * zoomStartPercentage / 100);
        this._zoomEndOfPeriod = this._startOfPeriod! + ((this._endOfPeriod! - this._startOfPeriod!) * zoomEndPercentage / 100);
        this._loadData().then(() => {
            this._updateChartData();
        });

    }

    protected _updateChartData(){
        this._chart!.setOption({
            xAxis: {
                min: this._startOfPeriod,
                max: this._endOfPeriod
            },
            series: this._data!.map(series => ({
                ...series,
                markLine: {
                    symbol: 'circle',
                    silent: true,
                    data: [{ name: '', xAxis: new Date().toISOString(), label: { formatter: '{b}' } }],
                    lineStyle: {
                        color: this._style.getPropertyValue("--internal-or-chart-text-color"),
                        type: 'solid',
                        width: 2,
                        opacity: 1
                    }
                }
            }))
        });;
    }

    protected _toggleChartEventListeners(connect: boolean){
        if (connect) {
            //Connect event listeners
            // Make chart size responsive
            //window.addEventListener("resize", () => this._chart!.resize());
            this._containerResizeObserver = new ResizeObserver(() => this._chart!.resize());
            this._containerResizeObserver.observe(this._chartElem);
            // Add event listener for zooming
            this._zoomHandler = this._chart!.on('datazoom', debounce((params: any) => { this._onZoomChange(params); }, 1500));
            // Add event listener for chart resize
            this._resizeHandler = this._chart!.on('resize', throttle(() => { this.applyChartResponsiveness(); }, 200));
        }
        else if (!connect) {
            //Disconnect event listeners
            this._chart!.off('datazoom', this._zoomHandler);
            this._chart!.off('resize', this._resizeHandler);
            this._containerResizeObserver?.disconnect();
            this._containerResizeObserver = undefined;
        }





    }
}
