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
import {AssetAttribute, AttributeRef, DatapointInterval, ValueDatapoint, ValueType, Asset, MetaItemType, Attribute, AssetEvent} from "@openremote/model";
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
import "@material/dialog";
import {MDCDialog} from '@material/dialog';
import "@openremote/or-translate";
import Chart, {ChartTooltipCallback, ChartDataSets} from "chart.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {MDCDataTable} from "@material/data-table";
import {JSONPath} from "jsonpath-plus";
import moment from "moment";
import {styleMap} from "lit-html/directives/style-map";
import { OrAssetTreeSelectionChangedEvent } from "@openremote/or-asset-tree";


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


// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

// language=CSS
const style = css`
    :host {
        --internal-or-chart-dataset-1-color: #9257A9;
        --internal-or-chart-dataset-2-color: #CC9423;
        --internal-or-chart-dataset-3-color: #8A273B;
        --internal-or-chart-dataset-4-color: #1C7C8A;
        


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

    mwc-dialog {
        z-index: 9999;
        --mdc-dialog-min-width: 600px;
        --mdc-dialog-max-height: calc(100vh - 50%);
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
    
    #controls {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        margin: var(--internal-or-chart-controls-margin);
        min-width: 300px;
        padding-left: 40px;
        flex-direction: column;
        margin: 0;
    }

    #attribute-list {
        overflow: auto;
        flex: 1 1 0;

        width: 100%;
        display: flex;
        flex-direction: column;

    }
    
    .attribute-list-item {
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
    }

    .attribute-list-item-label {
        display: flex;
        flex: 1 1 0;
        flex-direction: column;
    }

    .attribute-list-item-bullet {
        width: 14px;
        height: 14px;
        border-radius: 7px;
        margin-right: 10px;
    }

    #controls > * {
        margin: 5px 0;
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
    }
`;


@customElement("or-chart")
export class OrChart extends translate(i18next)(LitElement) {

    public static DEFAULT_TIMESTAMP_FORMAT = "L HH:mm:ss";

    static get styles() {
        return [
            css`${unsafeCSS(tableStyle)}`,
            style
        ];
    }

    @property({type: Object})
    private _assets: Asset[] = [];

    @property({type: Object})
    private _activeAsset?: Asset;

    @property({type: Object})
    private _attributes: AssetAttribute[] = [];

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: String})
    public interval?: DatapointInterval = DatapointInterval.DAY;

    @property({type: Number})
    public timestamp?: Date = new Date();

    @property({type: Object})
    public config?: ChartConfig;

    @property()
    protected _loading: boolean = false;
    
    @property()
    protected _data?: ValueDatapoint<any>[] | any = [];

    @property()
    protected _tableTemplate?: TemplateResult;

    @query("#chart")
    protected _chartElem!: HTMLCanvasElement;

    protected _chart?: Chart;
    protected _style!: CSSStyleDeclaration;

    firstUpdated() {
        
        if(this.shadowRoot) {
            const assetTreeElement = this.shadowRoot.getElementById('chart-asset-tree');
            if(assetTreeElement){
                assetTreeElement.addEventListener(OrAssetTreeSelectionChangedEvent.NAME, (evt) => this._onTreeSelectionChanged(evt));
            }
        }
    }

    
    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionChangedEvent) {
        const nodes = event.detail;
        if(nodes[0] && nodes[0].asset){
           this._activeAsset = nodes[0].asset;
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

        let reloadData = _changedProperties.has("interval") || _changedProperties.has("timestamp") || _changedProperties.has("_attributes");

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
                    <or-input .type="${InputType.SELECT}" ?disabled="${disabled}"
                    .label="${i18next.t("period")}"
                    .value="${this.interval}" 
                    .options="${this._getIntervalOptions()}"
                    @or-input-changed="${(evt: OrInputChangedEvent) => this.interval = evt.detail.value}"></or-input>
                  
                    <div id="ending-controls">
                        <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" icon="chevron-left" @click="${() => this._updateTimestamp(this.timestamp!, false)}"></or-input>
                        <or-input id="ending-date" 
                        .type="${endDateInputType}" 
                        ?disabled="${disabled}" 
                        label="${i18next.t("ending")}" 
                        .value="${this.timestamp}" 
                        @or-input-changed="${(evt: OrInputChangedEvent) => this._updateTimestamp(moment(evt.detail.value as string).toDate())}"></or-input>
                        <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" icon="chevron-right" @click="${() => this._updateTimestamp(this.timestamp!, true)}"></or-input>
                    </div>
                    <div id="attribute-list">
                        ${this._attributes?.map((attr, index) => {
                            return html`
                                <span class="attribute-list-item">
                                    <span class="attribute-list-item-bullet" style="background-color:${this._data[index].borderColor};"></span>
                                    <div class="attribute-list-item-label">
                                        <strong>${this._assets[index].name}</strong>
                                        <span>${i18next.t(attr.name ? attr.name : "")}</span>
                                    </div>
                                    <or-input style="--or-icon-width: 20px;" class="button" .type="${InputType.BUTTON}" icon="close" @click="${() => this._deleteAttribute(index)}"></or-input>

                                </div>
                            `
                        })}
                    </div>
                    <or-input class="button" .type="${InputType.BUTTON}" ?disabled="${disabled}" label="Add attribute" icon="plus" @click="${() => this._openDialog()}"></or-input>

                </div>
            </div>
            <div id="mdc-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="my-dialog-title"
                aria-describedby="my-dialog-content">
                <div class="mdc-dialog__container">
                    <div class="mdc-dialog__surface">
                    <h2 class="mdc-dialog__title" id="my-dialog-title">Add attribute</h2>
                    <div class="dialog-container mdc-dialog__content" id="my-dialog-content">
                        <or-asset-tree id="chart-asset-tree"></or-asset-tree>
                            ${this._activeAsset && this._activeAsset.attributes ? html`
                                <or-input id="chart-attribute-picker" 
                                        .label="${i18next.t("attribute")}" 
                                        .type="${InputType.LIST}"
                                        .options="${this._getAttributeOptions()}"></or-input>
                            `:``}
                    </div>
                    <footer class="mdc-dialog__actions">
                        <or-input class="button" 
                            slot="primaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("Add")}" 
                            class="mdc-button mdc-dialog__button" data-mdc-dialog-action="yes"
                            @click="${this.addAttribute}"></or-input>

                        <or-input class="button" 
                            slot="secondaryAction"
                            .type="${InputType.BUTTON}" 
                            label="${i18next.t("Cancel")}" 
                            class="mdc-button mdc-dialog__button" data-mdc-dialog-action="no"></or-input>
                    </footer>
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
    }
    
 
    getInputType() {
        switch(this.interval) {
            case DatapointInterval.HOUR:
                return InputType.TIME
              break;
            case DatapointInterval.DAY:
                return InputType.DATE
              break;
            case DatapointInterval.WEEK:
                return InputType.WEEK
              break;
            case DatapointInterval.MONTH:
                return InputType.MONTH
              break;
            case DatapointInterval.YEAR:
                return InputType.MONTH
                break;
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
                    // maintainAspectRatio: false,
                    // REMOVED AS DOESN'T SIZE CORRECTLY responsive: true,
                    onResize:() => this.dispatchEvent(new OrChartEvent('resize')),
                    legend: {
                        display: false
                    },
                    tooltips: {
                        displayColors: false,
                        callbacks: {
                            label: (tooltipItem, data) => {
                                return tooltipItem.yLabel; // Removes the colon before the label
                            },
                            footer: () => {
                                return " "; // Hack the broken vertical alignment of body with footerFontSize: 0
                            }
                        } as ChartTooltipCallback
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
                                    millisecond: 'HH:mm:ss.SSS',
                                    second: 'HH:mm:ss',
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

    addAttribute() {
        if(this.shadowRoot && this.shadowRoot.getElementById('chart-attribute-picker')){
            const elm = this.shadowRoot.getElementById('chart-attribute-picker') as HTMLInputElement;
            if(this._activeAsset){
                const attr = Util.getAssetAttribute(this._activeAsset, elm.value);

                if(attr){
                    this._assets = [...this._assets, this._activeAsset];
                    this._attributes = [...this._attributes, attr];
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
        this._attributes = [...this._attributes.slice(0, index).concat(this._attributes.slice(index + 1, this._attributes.length))]
    }

    protected _getAttributeOptions() {
        if(!this._activeAsset || !this._activeAsset.attributes) {
            return;
        }

        let attributes = [...Util.getAssetAttributes(this._activeAsset)];
        if(attributes && attributes.length > 0) {
            // TODO change when assets have store_data_points
            // attributes = attributes.filter((attr:Attribute) => Util.getFirstMetaItem(attr, MetaItemType.STORE_DATA_POINTS.urn!));
        
            const options = attributes.map((attr:Attribute) => [attr.name, Util.getAttributeLabel(attr, undefined)]);
            return options
        }
    }

    protected _getIntervalOptions(): [string, string][] {
        return [
            DatapointInterval.HOUR,
            DatapointInterval.DAY,
            DatapointInterval.WEEK,
            DatapointInterval.MONTH,
            DatapointInterval.YEAR
        ].map((interval) => {
            return [interval, i18next.t(interval.toLowerCase())];
        });
    }

    protected async _loadData() {
        if(!this._attributes) {
            return;
        }

        const data = this._attributes.map(async (attribute, index) => {
            const valuepoints = await this._loadAttributeData(attribute);
            let bgColor = this._style.getPropertyValue("--internal-or-chart-dataset-"+(index+1)+"-color").trim();
            const dataset: ChartDataSets = {
                data: valuepoints,
                borderColor: bgColor,
                backgroundColor: "transparent"
            }
            return dataset;
        });

        Promise.all(data).then((completed=> {
            this._data = completed;

        }))
    }
    
    protected async _loadAttributeData(attribute:AssetAttribute) {
        if (!attribute) {
            return [];
        }

        this._loading = true;

        if (!this.interval || !this.timestamp) {
            this._loading = false;
            return [];
        }

        if(attribute.assetId &&  attribute.name){
            const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
                attribute.assetId,
                attribute.name,
                {
                    interval: this.interval || DatapointInterval.DAY,
                    timestamp: this.timestamp.getTime()
                }
            );
            this._loading = false;

            if (response.status === 200) {
                if(response.data) {
                    response.data.map(item => {
                        item.y = Math.random();
                    });
                }
                return response.data;
            }
        }
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

        this.timestamp = newMoment.toDate();
    }
}
