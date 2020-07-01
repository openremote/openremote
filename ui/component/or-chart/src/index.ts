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
import {Asset, AssetAttribute, AttributeRef, DatapointInterval, MetaItemType, Attribute, ReadAssetEvent, AssetEvent} from "@openremote/model";
import manager, {
    AssetModelUtil,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    Util
} from "@openremote/core";
import "@openremote/or-asset-tree";
import "@openremote/or-input";
import "@openremote/or-panel";
import {MDCDialog} from '@material/dialog';
import "@openremote/or-translate";
import Chart, {ChartDataSets, ChartOptions} from "chart.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import moment from "moment";
import {OrAssetTreeSelectionChangedEvent} from "@openremote/or-asset-tree";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {MenuItem, OrMwcMenu, OrMwcMenuChangedEvent} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import * as ChartAnnotation from "chartjs-plugin-annotation";
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
    assetIds?: (string|undefined)[];
    attributes?: (string|undefined)[];
    timestamp?: Date;
    compareTimestamp?: Date;
    period?: moment.unitOfTime.Base;
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
const dialogStyle = require("!!raw-loader!@material/dialog/dist/mdc.dialog.css");
const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

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
    
    .dialog-container > or-input{
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

export function getContentWithMenuTemplate(content: TemplateResult, menuItems: (MenuItem | MenuItem[] | null)[], selectedValues: string[] | string | undefined, valueChangedCallback: (values: string[] | string) => void, closedCallback?: () => void, multiSelect = false): TemplateResult {

    const openMenu = (evt: Event) => {
        if (!menuItems) {
            return;
        }

        ((evt.currentTarget as Element).parentElement!.lastElementChild as OrMwcMenu).open();
    };

    return html`
        <span>
            <span @click="${openMenu}">${content}</span>
            ${menuItems ? html`<or-mwc-menu ?multiselect="${multiSelect}" @or-mwc-menu-closed="${() => {if (closedCallback) { closedCallback(); }} }" @or-mwc-menu-changed="${(evt: OrMwcMenuChangedEvent) => {if (valueChangedCallback) { valueChangedCallback(evt.detail); }} }" .values="${selectedValues}" .menuItems="${menuItems}" id="menu"></or-mwc-menu>` : ``}
        </span>
    `;
}
export interface ChartValueDatapoint<T> {
    x?: number | string;
    y?: T;
}
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
    public assetAttributes: AssetAttribute[] = [];

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: Array})
    public colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    @property({type: String})
    public period: moment.unitOfTime.Base = "day";

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
    protected _baseData?: ChartValueDatapoint<any>[] | any = [];

    @property()
    protected _data?: ChartValueDatapoint<any>[] | any = [];

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLCanvasElement;

    protected _chart?: Chart;

    @query("#mdc-dialog")
    protected _dialogElem!: HTMLElement;

    protected _dialog!: MDCDialog;

    protected _style!: CSSStyleDeclaration;

    firstUpdated() {
        if (this._dialogElem) {
            this._dialog = new MDCDialog(this._dialogElem);
            if(!this.activeAssetId) {
                this.getSettings();
            }
        }
    }

    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionChangedEvent) {
        // Need to fully load the asset
        if (!manager.events) {
            return;
        }

        const selectedNode = event.detail && event.detail.length > 0 ? event.detail[0] : undefined;

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
        this.addEventListener(OrAssetTreeSelectionChangedEvent.NAME, this._onTreeSelectionChanged);
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
        this.removeEventListener(OrAssetTreeSelectionChangedEvent.NAME, this._onTreeSelectionChanged);
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
            html`<or-input .type="${InputType.BUTTON}" .label="${i18next.t("timeframe")}: ${i18next.t(this.period ? this.period : "-")}"></or-input>`,
            this._getPeriodOptions(),
            this.period,
            (value) => this.setPeriodOption(value))}

                        ${this.periodCompare ? html `
                                <or-input style="margin-left:auto;" .type="${InputType.BUTTON}" .label="${i18next.t("period")}" @click="${() => this.setPeriodCompare(false)}" icon="minus"></or-input>
                        ` : html`
                                <or-input style="margin-left:auto;" .type="${InputType.BUTTON}" .label="${i18next.t("period")}" @click="${() => this.setPeriodCompare(true)}" icon="plus"></or-input>
                        `}
                    </div>
                  
                    <div class="period-controls">

                        ${this.periodCompare ? html `
                            <span class="line-label solid"></span>
                        `: ``}
                        <or-input id="ending-date" 
                            .checkAssetWrite="${false}"
                            .type="${endDateInputType}" 
                            ?disabled="${disabled}" 
                            .value="${this.timestamp}" 
                            @or-input-changed="${(evt: OrInputChangedEvent) => this.timestamp = this._updateTimestamp(moment(evt.detail.value as string).toDate())}"></or-input>
                        <or-icon class="button-icon" icon="chevron-left" @click="${() => this.timestamp = this._updateTimestamp(this.timestamp!, false)}"></or-icon>
                        <or-icon class="button-icon" icon="chevron-right" @click="${() => this.timestamp = this._updateTimestamp(this.timestamp!, true)}"></or-icon>
                    </div>
                    ${this.periodCompare ? html `
                        <div class="period-controls">
                        <span class="line-label dashed"></span>
                            <or-input id="ending-date" 
                                .checkAssetWrite="${false}"
                                .type="${endDateInputType}" 
                                ?disabled="${disabled}" 
                                .value="${this.compareTimestamp}" 
                                @or-input-changed="${(evt: OrInputChangedEvent) => this.compareTimestamp = this._updateTimestamp(moment(evt.detail.value as string).toDate())}"></or-input>
                            <or-icon class="button-icon" icon="chevron-left" @click="${() => this.compareTimestamp = this._updateTimestamp(this.compareTimestamp!, false)}"></or-icon>
                            <or-icon class="button-icon" icon="chevron-right" @click="${() => this.compareTimestamp = this._updateTimestamp(this.compareTimestamp!, true)}"></or-icon>
                        </div>
                    ` : html``}

                    <div id="attribute-list">
                        ${this.assetAttributes && this.assetAttributes.map((attr, index) => {

                            const attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(attr.name!);
                            let label = Util.getAttributeLabel(attr, attributeDescriptor);
                            let unit = Util.getMetaValue(MetaItemType.UNIT_TYPE, attr, attributeDescriptor);
                            if(unit) {
                                 label = label + " ("+i18next.t(unit)+")";
                            }
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
                    <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-input>

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
                        <or-asset-tree id="chart-asset-tree" .selectedIds="${this.activeAsset ? [this.activeAsset.id] : undefined}"></or-asset-tree>
                            ${this.activeAsset && this.activeAsset.attributes ? html`
                                <or-input id="chart-attribute-picker" 
                                        style="display:flex;"
                                        .label="${i18next.t("attribute")}" 
                                        .type="${InputType.LIST}"
                                        .options="${this._getAttributeOptions()}"></or-input>
                            `:``}
                    </div>
                    <footer class="mdc-dialog__actions">
                        <or-input class="button" 
                                slot="secondaryAction"
                                .type="${InputType.BUTTON}" 
                                label="${i18next.t("cancel")}" 
                                class="mdc-button mdc-dialog__button" 
                                data-mdc-dialog-action="no"></or-input>

                        <or-input class="button" 
                            slot="primaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("add")}" 
                            class="mdc-button mdc-dialog__button" 
                            data-mdc-dialog-action="yes"
                            @click="${this.addAttribute}"></or-input>

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

        let reloadData = changedProperties.has("period") || changedProperties.has("compareTimestamp") || changedProperties.has("timestamp") || changedProperties.has("assetAttributes");
        if (reloadData) {
            this._data = [];
            this._loadData();
        }

        if (!this._data) {
            return;
        }
        if (!this._chart) {
            this._chart = new Chart(this._chartElem, {
                type: "line",
                data: {
                    datasets: this._data
                },
                plugins: [
                    ChartAnnotation
                ],
                options: {
                    annotation: {
                        annotations: [
                            {
                                type: 'line',
                                mode: 'vertical',
                                scaleID: 'x-axis-0',
                                value: moment(),
                                borderColor: "#275582",
                                borderWidth: 2
                            }
                        ]
                    },
                    showLines: true,
                    maintainAspectRatio: false,
                    // REMOVED AS DOESN'T SIZE CORRECTLY 
                    responsive: true,
                    onResize:() => this.dispatchEvent(new OrChartEvent('resize')),
                    legend: {
                        display: false
                    },
                    tooltips: {
                        mode: 'x',
                        intersect: false
                    },
                    hover: {
                        mode: 'x',
                        intersect: false
                    },
                    scales: {
                        yAxes: [{
                            ticks: {
                                beginAtZero: true
                            },
                            gridLines: {
                                color: "#cccccc"
                            }
                        }],
                        xAxes: [{
                            type: "time",
                            time: {
                                displayFormats: {
                                    quarter: 'MMM YYYY',
                                    minute: "HH:mm",
                                    hour: "HH:mm",
                                    week: "w"
                                }
                            },
                            ticks: {
                                autoSkip: true,
                                maxTicksLimit: 30,
                                fontColor: "#000",
                                fontFamily: "'Open Sans', Helvetica, Arial, Lucida, sans-serif",
                                fontSize: 9,
                                fontStyle: "normal"
                            },
                            gridLines: {
                                color: "#cccccc"
                            }
                        }]
                    }
                } as ChartOptions
            });
        } else {
            if (changedProperties.has("_data")) {
                this._chart.data.datasets = this._data;
                this._chart.update();
            }
        }
        this.onCompleted().then(() => {
            this.dispatchEvent(new OrChartEvent('rendered'));
        });

    }

    getSettings() {
        const configStr = window.localStorage.getItem('OrChartConfig')
        if(!configStr || !this.panelName) return

        const viewSelector = this.activeAssetId ? this.activeAssetId : window.location.hash;
        const config = JSON.parse(configStr);
        const view = config.views[viewSelector][this.panelName];
        if(!view) return
        const query = {
            ids: view.assetIds
        }
        if(view.assetIds === this.assets.map(asset => asset.id)) return 
        this._loading = true;

        manager.rest.api.AssetResource.queryAssets(query).then((response) => {
            const assets = response.data;
            if(assets.length > 0) {
                this.assets = view.assetIds.map((assetId: string)  => assets.find(x => x.id === assetId));
                this.assetAttributes = view.attributes.map((attr: string, index: number)  => Util.getAssetAttribute(this.assets[index], attr));
                this.period = view.period;
                this._loading = false;
            }
        });

    }

    saveSettings() {
        const viewSelector = this.activeAssetId ? this.activeAssetId : window.location.hash;
        const assets: Asset[] = this.assets.filter(asset => 'id' in asset && typeof asset.id === "string");
        const assetIds = assets.map(asset => asset.id);
        const attributes = this.assetAttributes.map(attr => attr.name);
        const configStr = window.localStorage.getItem('OrChartConfig')
        if(!this.panelName) return

        let config:OrChartConfig;
        if(configStr) {
            config = JSON.parse(configStr);
        } else {
            config = {
                views: {
                    [viewSelector]: {
                        [name] : {

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
        if(this.shadowRoot && this.shadowRoot.getElementById('chart-attribute-picker')){
            const elm = this.shadowRoot.getElementById('chart-attribute-picker') as HTMLInputElement;
            if(this.activeAsset){
                const attr = Util.getAssetAttribute(this.activeAsset, elm.value);
                if(attr){
                    this.setAttrColor(attr);
                    this.assetAttributes = [...this.assetAttributes, attr];

                    this.assets = [...this.assets, this.activeAsset];
                    this.saveSettings();
                }
            }
        }
    }
    
    setAttrColor(attr: Attribute) {
        const usedColors = this.assetAttributes.map(attr => this.getAttrColor(attr));
        const color = this.colors.filter(color => usedColors.indexOf(color) < 0)[0];
        const meta = {name: "color", value: color};
        if(attr.meta){
            attr.meta.push(meta);
        }
    }
    
    getAttrColor(attr: Attribute) {
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

    protected _getAttributeOptions() {
        if(!this.activeAsset || !this.activeAsset.attributes) {
            return;
        }

        if(this.shadowRoot && this.shadowRoot.getElementById('chart-attribute-picker')) {
            const elm = this.shadowRoot.getElementById('chart-attribute-picker') as HTMLInputElement;
            elm.value = '';
        }

        let attributes = [...Util.getAssetAttributes(this.activeAsset)];
        if(attributes && attributes.length > 0) {
            attributes = attributes
                .filter((attr: AssetAttribute) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!))
                .filter((attr: AssetAttribute) => (this.assetAttributes && !this.assetAttributes.some(assetAttr => (assetAttr.name === attr.name) && (assetAttr.assetId === attr.assetId))));
            const options = attributes.map((attr: AssetAttribute) => [attr.name, Util.getAttributeLabel(attr, undefined)]);
            return options
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
        this._loadData();
    }

    protected async _loadData() {
        if(!this.assetAttributes || !this.assets || this.assets.length === 0) {
            return;
        }

        const datasetBases = this.assetAttributes.map(attribute => {
            const bgColor = this.getAttrColor(attribute);
            const dataset: ChartDataSets = {
                borderColor: bgColor,
                pointRadius: 2,
                backgroundColor: bgColor,
                fill: false
            };
            return dataset;
        });

        let data = this.assetAttributes.map(async (attribute, index) => {
            const valuepoints = await this._loadAttributeData(attribute, this.timestamp);
            const dataset = {...datasetBases[index],
                data: valuepoints
            };
            if(this.assets[index]) 
                dataset['label'] = this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined);
                    
            return dataset;
        });


        Promise.all(data).then((completed => {
            this._baseData = [...completed];
        }));

        let predictedData = this.assetAttributes.map(async (attribute, index) => {
            const valuepoints = await this._loadPredictedAttributeData(attribute, this.timestamp);
            const dataset = {...datasetBases[index],
                data: valuepoints,
                borderDash: [2, 4]
            };

            if(this.assets[index]) 
                dataset['label'] = this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined)+" "+i18next.t("predicted");
            return dataset;
        });
        
        data = data.concat(predictedData);

        if(this.periodCompare) {
            const cData = this.assetAttributes.map(async (attribute, index) => {
                const valuepoints = await this._loadAttributeData(attribute, this.compareTimestamp);
                const dataset = {...datasetBases[index],
                    data: valuepoints,
                    borderDash: [10, 10]
                };

                if(this.assets[index]) 
                    dataset['label'] = this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined)+" "+i18next.t("compare");

                return dataset;
            });
            
            let cPredictedData = this.assetAttributes.map(async (attribute, index) => {
                const valuepoints = await this._loadPredictedAttributeData(attribute, this.compareTimestamp);
                const dataset = {...datasetBases[index],
                    data: valuepoints,
                    borderDash: [2, 4]
                };
                if(this.assets[index]) 
                    dataset['label'] = this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined)+" "+i18next.t("compare")+" "+i18next.t("predicted")
                return dataset;
            });

            data = data.concat(cPredictedData);
          
            data = data.concat(cData);
        }
        
        Promise.all(data).then((completed=> {
            this._data = completed;
        }))
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
    
    protected async _loadAttributeData(attribute:AssetAttribute, timestamp: Date | undefined) {
        if (!attribute) {
            return [];
        }

        this._loading = true;

        if (!this.period || !timestamp) {
            this._loading = false;
            return [];
        }

        const startOfPeriod = moment(timestamp).startOf(this.period).toDate().getTime();
        const endOfPeriod = moment(timestamp).endOf(this.period).toDate().getTime();

        if (attribute.assetId && attribute.name) {
            const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
                attribute.assetId,
                attribute.name,
                {
                    interval: this._getInterval(),
                    fromTimestamp: startOfPeriod,
                    toTimestamp: endOfPeriod
                }
            );

            const data = response.data;
          
            this._loading = false;

            if (response.status === 200) {
                data.map((datapoint: any) => {
                    if (datapoint['x']) {
                        datapoint['x'] = this._timestampLabel(datapoint['x'])
                    }

                    if (typeof datapoint['y'] !== 'undefined') {
                        datapoint['y'] = Math.round(datapoint['y'] * 100) / 100
                    } else {
                        delete datapoint['y']
                    }
                });
                return data;
            }
        }
    }

    protected async _loadPredictedAttributeData(attribute:AssetAttribute, timestamp: Date | undefined) {
        if (!attribute) {
            return [];
        }

        this._loading = true;

        if (!this.period || !timestamp) {
            this._loading = false;
            return [];
        }
    
        const now = moment().toDate().valueOf();
        const startOfPeriod = moment(timestamp).startOf(this.period).toDate().valueOf();
        const endOfPeriod = moment(timestamp).endOf(this.period).toDate().valueOf();
        const fromTimestamp = now < startOfPeriod ? startOfPeriod : now;

        if(attribute.assetId &&  attribute.name && endOfPeriod){
            const response = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(
                attribute.assetId,
                attribute.name,
                {
                    interval: this._getInterval(),
                    fromTimestamp: fromTimestamp,
                    toTimestamp: endOfPeriod
                }
            );

            this._loading = false;
            const data = response.data;
            if (response.status === 200) {
                data.sort((a, b) => {
                    if(a.x && b.x) {
                        return (a.x - b.x);
                    } else {
                        return 0;
                    }
                });
                data.map((datapoint: any) => {
                    if (datapoint['x']) {
                        datapoint['x'] = this._timestampLabel(datapoint['x'])
                    }
                    if (typeof datapoint['y'] !== 'undefined') {
                        datapoint['y'] = Math.round(datapoint['y'] * 100) / 100
                    } else {
                        delete datapoint['y'];
                    }
                });
                return data;
            }
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

    protected _updateTimestamp(timestamp: Date, forward?: boolean) {
        const newMoment = moment(timestamp);

        if (forward !== undefined) {
            newMoment.add(forward ? 1 : -1, this.period);
        }

        return newMoment.toDate();
    }
}
