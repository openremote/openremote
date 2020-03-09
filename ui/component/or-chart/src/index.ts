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
import {Asset, AssetAttribute, Attribute, AttributeRef, DatapointInterval, MetaItemType} from "@openremote/model";
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
import Chart, {ChartDataSets} from "chart.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import moment, {unitOfTime} from "moment";
import {OrAssetTreeSelectionChangedEvent} from "@openremote/or-asset-tree";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {MenuItem, OrMwcMenu, OrMwcMenuChangedEvent} from "@openremote/or-mwc-components/dist/or-mwc-menu";

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
    
    #attribute-list {
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

    @property({type: Object})
    private activeAsset?: Asset;

    @property({type: Object})
    public assetAttributes: AssetAttribute[] = [];

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: Array})
    public colors: string[] = ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"];

    @property({type: Array})
    public usedColors: string[] = [];

    @property({type: String})
    public interval?: DatapointInterval = DatapointInterval.DAY;

    @property({type: Number})
    public timestamp?: Date = moment().set('minute', 0).toDate();

    @property({type: Number})
    public compareTimestamp?: Date = moment().set('minute', 0).toDate();

    @property({type: Object})
    public config?: OrChartConfig;

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
        if(this.shadowRoot) {
            const assetTreeElement = this.shadowRoot.getElementById('chart-asset-tree');
            if(assetTreeElement){
                assetTreeElement.addEventListener(OrAssetTreeSelectionChangedEvent.NAME, (evt) => this._onTreeSelectionChanged(evt));
            }
            this._dialog = new MDCDialog(this._dialogElem);

            this.assetAttributes.map((attr, index) => {
                const meta = {name: "color", value: this.colors[index]};
                if(attr.meta){
                    attr.meta.push(meta);
                }
                this.usedColors = [...this.usedColors, this.colors[index]];
            });
        }
    }

    
    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionChangedEvent) {
        const nodes = event.detail;
        if(nodes[0] && nodes[0].asset){
           this.activeAsset = nodes[0].asset;
        }
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._style = window.getComputedStyle(this);
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        this._cleanup();
    }

    shouldUpdate(_changedProperties: PropertyValues): boolean {

        let reloadData = _changedProperties.has("interval") || _changedProperties.has("compareTimestamp") || _changedProperties.has("timestamp") || _changedProperties.has("assetAttributes");

        if (reloadData) {
            this._data = [];
            this._loadData();
        }

        return super.shouldUpdate(_changedProperties);
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
                            html`<or-input .type="${InputType.BUTTON}" .label="${i18next.t("timeframe")}: ${i18next.t(this.interval ? this.interval : "-")}"></or-input>`,
                            this._getIntervalOptions(),
                            this.interval,
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
                                .type="${endDateInputType}" 
                                ?disabled="${disabled}" 
                                .value="${this.compareTimestamp}" 
                                @or-input-changed="${(evt: OrInputChangedEvent) => this.compareTimestamp = this._updateTimestamp(moment(evt.detail.value as string).toDate())}"></or-input>
                            <or-icon class="button-icon" icon="chevron-left" @click="${() => this.compareTimestamp = this._updateTimestamp(this.compareTimestamp!, false)}"></or-icon>
                            <or-icon class="button-icon" icon="chevron-right" @click="${() => this.compareTimestamp = this._updateTimestamp(this.compareTimestamp!, true)}"></or-icon>
                        </div>
                    ` : html``}

                    <div id="attribute-list">
                        ${this.assetAttributes.map((attr, index) => {
                            const bgColor = Util.getMetaValue('color', attr, undefined) ? Util.getMetaValue('color', attr, undefined) : "";
                            return html`
                                <div class="attribute-list-item" @mouseover="${()=> this.addDatasetHighlight(bgColor)}" @mouseout="${()=> this.removeDatasetHighlight(bgColor)}">
                                    <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(this.assets[index]!.type!), undefined, undefined, bgColor.split('#')[1])}</span>
                                    <div class="attribute-list-item-label">
                                        <span>${this.assets[index].name}</span>
                                        <span style="font-size:14px; color:grey;">${Util.getAttributeLabel(attr, undefined)}</span>
                                    </div>
                                    <button class="button-clear" @click="${() => this._deleteAttribute(index)}"><or-icon icon="close-circle"></or-icon></button>
                                </div>
                            `
                        })}
                    </div>
                    <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" label="Add attribute" icon="plus" @click="${() => this._openDialog()}"></or-input>

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
                    <h2 class="mdc-dialog__title" id="my-dialog-title">Add attribute</h2>
                    <div class="dialog-container mdc-dialog__content" id="my-dialog-content">
                        <or-asset-tree id="chart-asset-tree" .selectedIds="${this.activeAsset ? [this.activeAsset.id] : null}]"></or-asset-tree>
                            ${this.activeAsset && this.activeAsset.attributes ? html`
                                <or-input id="chart-attribute-picker" 
                                        .label="${i18next.t("attribute")}" 
                                        .type="${InputType.LIST}"
                                        .options="${this._getAttributeOptions()}"></or-input>
                            `:``}
                    </div>
                    <footer class="mdc-dialog__actions">
                        <or-input class="button" 
                                slot="secondaryAction"
                                .type="${InputType.BUTTON}" 
                                label="${i18next.t("Cancel")}" 
                                class="mdc-button mdc-dialog__button" 
                                data-mdc-dialog-action="no"></or-input>

                        <or-input class="button" 
                            slot="primaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("Add")}" 
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
        this.interval = value;
        this.requestUpdate();
    }
 
    getInputType() {
        switch(this.interval) {
            case DatapointInterval.HOUR:
                return InputType.DATETIME;
              break;
            case DatapointInterval.DAY:
                return InputType.DATE;
              break;
            case DatapointInterval.WEEK:
                return InputType.WEEK;
              break;
            case DatapointInterval.MONTH:
                return InputType.MONTH;
              break;
            case DatapointInterval.YEAR:
                return InputType.MONTH;
                break;
          }
    }

    removeDatasetHighlight(bgColor:string) {
        if(this._chart && this._chart.data && this._chart.data.datasets){
            this._chart.data.datasets.map((dataset, idx) => {
                if (dataset.borderColor === bgColor) {
                    return
                }
                if (dataset.borderColor && typeof dataset.borderColor === "string") {
                    dataset.borderColor = dataset.borderColor.slice(0, -2);
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
            });
            this._chart.update();
        }
    }

    updated(changedProperties: PropertyValues) {
        super.updated(changedProperties);

        if (!this._data) {
            return;
        }
        if (!this._chart) {
            this._chart = new Chart(this._chartElem, {
                type: "line",
                data: {
                    datasets: this._data
                },
                options: {
                    showLines: true,
                    maintainAspectRatio: false,
                    // REMOVED AS DOESN'T SIZE CORRECTLY 
                    responsive: true,
                    onResize:() => this.dispatchEvent(new OrChartEvent('resize')),
                    legend: {
                        display: false
                    },
                    tooltips: {
                        mode: 'index',
                        intersect: true
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
                }
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
                    const color = this.colors.filter(color => this.usedColors.indexOf( color ) < 0)[0]
                    const meta = {name: "color", value: color};
                    if(attr.meta){
                        attr.meta.push(meta);
                    }
                    this.usedColors = [...this.usedColors, color];
                    this.assets = [...this.assets, this.activeAsset];
                    this.assetAttributes = [...this.assetAttributes, attr];
                }
            }
        }
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

    protected _deleteAttribute (index:number) {
        this.assets = [...this.assets.slice(0, index).concat(this.assets.slice(index + 1, this.assets.length))];
        this.assetAttributes = [...this.assetAttributes.slice(0, index).concat(this.assetAttributes.slice(index + 1, this.assetAttributes.length))];
        this.usedColors = [...this.usedColors.slice(0, index).concat(this.usedColors.slice(index + 1, this.usedColors.length))];
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

    protected _getIntervalOptions(){
        return [
            {
                text: i18next.t(DatapointInterval.HOUR),
                value:  DatapointInterval.HOUR
            },
            {
                text: i18next.t(DatapointInterval.DAY),
                value:  DatapointInterval.DAY
            },
            {
                text: i18next.t(DatapointInterval.WEEK),
                value:  DatapointInterval.WEEK
            },
            {
                text: i18next.t(DatapointInterval.MONTH),
                value:  DatapointInterval.MONTH
            },
            {
                text: i18next.t(DatapointInterval.YEAR),
                value:  DatapointInterval.YEAR
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

        let data = this.assetAttributes.map(async (attribute, index) => {
            const valuepoints = await this._loadAttributeData(attribute, this.timestamp);
            const bgColor = Util.getMetaValue('color', attribute, undefined);
            const dataset: ChartDataSets = {
                data: valuepoints,
                label: this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined),
                borderColor: bgColor,
                pointRadius: 2,
                backgroundColor: bgColor,
                fill: false
            };
            return dataset;
        });


        Promise.all(data).then((completed => {
            this._baseData = [...completed];
        }));

        let predictedData = this.assetAttributes.map(async (attribute, index) => {
            const valuepoints = await this._loadPredictedAttributeData(attribute, this.timestamp);
            const bgColor = Util.getMetaValue('color', attribute, undefined);
            const dataset: ChartDataSets = {
                data: valuepoints,
                label: this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined)+" "+i18next.t("predicted"),
                borderColor: bgColor,
                borderDash: [2, 4],
                pointRadius: 2,
                backgroundColor: bgColor,
                fill: false
            };
            return dataset;
        });
        
        data = data.concat(predictedData);

        if(this.periodCompare) {
            const cData = this.assetAttributes.map(async (attribute, index) => {
                const valuepoints = await this._loadAttributeData(attribute, this.compareTimestamp);
                const bgColor = Util.getMetaValue('color', attribute, undefined);
                const dataset: ChartDataSets = {
                    data: valuepoints,
                    label: this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined)+" "+i18next.t("compare"),
                    borderColor: Util.getMetaValue('color', attribute, undefined),
                    borderDash: [10, 10],
                    pointRadius: 2,
                    backgroundColor: bgColor,
                    fill: false
                };
                return dataset;
            });
            
            let cPredictedData = this.assetAttributes.map(async (attribute, index) => {
                const valuepoints = await this._loadPredictedAttributeData(attribute, this.compareTimestamp);
                const bgColor = Util.getMetaValue('color', attribute, undefined);
                const dataset: ChartDataSets = {
                    data: valuepoints,
                    label: this.assets[index]!.name +" "+ Util.getAttributeLabel(attribute, undefined)+" "+i18next.t("compare")+" "+i18next.t("predicted"),
                    borderColor: bgColor,
                    borderDash: [2, 4],
                    pointRadius: 2,
                    backgroundColor: bgColor,
                    fill: false
                };
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
        let newMoment = moment(timestamp);

        if(this.periodCompare) {
            const initialTimestamp = moment(this.timestamp);
            switch (this.interval) {
                case DatapointInterval.HOUR:
                    newMoment = moment(timestamp)
                    break;
                case DatapointInterval.DAY:
                    newMoment = moment(timestamp).set('day', initialTimestamp.day())
                    break;
                case DatapointInterval.WEEK:
                    newMoment = moment(timestamp).set('week', initialTimestamp.week())
                    break;
                case DatapointInterval.MONTH:
                    newMoment = moment(timestamp).set('month', initialTimestamp.month())
                    break;
                case DatapointInterval.YEAR:
                    newMoment = moment(timestamp).set('year', initialTimestamp.year())
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

        if (!this.interval || !timestamp) {
            this._loading = false;
            return [];
        }

        const period = this._getUnitOfTime();
        const forwardTime = this._updateTimestamp(timestamp, true);
        const startOfPeriod = moment(forwardTime).startOf(period).toDate().getTime();
        if(attribute.assetId &&  attribute.name){
            const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
                attribute.assetId,
                attribute.name,
                {
                    interval: this.interval || DatapointInterval.DAY,
                    timestamp: startOfPeriod
                }
            );

            const data = response.data;
          
            this._loading = false;

            if (response.status === 200) {
                data.map((datapoint:any) => {
                    if(datapoint['x']) {
                        datapoint['x'] = this._timestampLabel(datapoint['x'])
                    }
                    if(datapoint['y']) {
                        datapoint['y'] = Math.round(datapoint['y'] * 100)/100
                    } else {
                        delete datapoint['y']
                    }
                })
                return data;
            }
        }
    }

    protected async _loadPredictedAttributeData(attribute:AssetAttribute, timestamp: Date | undefined) {
        if (!attribute) {
            return [];
        }

        this._loading = true;

        if (!this.interval || !timestamp) {
            this._loading = false;
            return [];
        }
        const period = this._getUnitOfTime();
        const now = moment().toDate().getTime();
        const startOfPeriod = moment(timestamp).startOf(period).toDate().getTime();
        const endOfPeriod = moment(timestamp).endOf(period).toDate().getTime();
        const fromTimestamp = now < startOfPeriod ? startOfPeriod : now;

        if(attribute.assetId &&  attribute.name && endOfPeriod){
            const response = await manager.rest.api.AssetPredictedDatapointResource.getPredictedDatapoints(
                attribute.assetId,
                attribute.name,
                {
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
                data.map((datapoint:any) => {
                    if(datapoint['x']) {
                        datapoint['x'] = this._timestampLabel(datapoint['x'])
                    }
                    if(datapoint['y']) {
                        datapoint['y'] = Math.round(datapoint['y'] * 100)/100
                    } else {
                        delete datapoint['y']
                    }
                })
                return data;
            }
        }
    }

    protected _getUnitOfTime() {
        let unit:unitOfTime.All = 'day';
        switch (this.interval) {
            case DatapointInterval.HOUR:
                unit = "hour";
                break;
            case DatapointInterval.DAY:
                unit = 'day';
                break;
            case DatapointInterval.WEEK:
                unit = 'week';
                break;
            case DatapointInterval.MONTH:
                unit = 'month';
                break;
            case DatapointInterval.YEAR:
                unit = 'year';
                break;
        }
        return unit;
    }
   

    protected _updateTimestamp(timestamp: Date, forward?: boolean) {
        if (!this.interval) { 
            return;
        }

        const newMoment = moment(timestamp);

        if (forward !== undefined) {
            switch (this.interval) {
                case DatapointInterval.HOUR:
                    newMoment.add(forward ? 1 : -1, "hour");
                    break;
                case DatapointInterval.DAY:
                    newMoment.add(forward ? 1 : -1, "day");
                    break;
                case DatapointInterval.WEEK:
                    newMoment.add(forward ? 1 : -1, "week");
                    break;
                case DatapointInterval.MONTH:
                    newMoment.add(forward ? 1 : -1, "month");
                    break;
                case DatapointInterval.YEAR:
                    newMoment.add(forward ? 1 : -1, "year");
                    break;
            }
        }

        return newMoment.toDate();
    }
}
