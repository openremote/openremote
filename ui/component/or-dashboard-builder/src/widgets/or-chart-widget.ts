import {Asset, AssetDatapointIntervalQuery, AssetDatapointIntervalQueryFormula, AssetDatapointLTTBQuery, AssetDatapointQueryUnion, Attribute, AttributeRef, DashboardWidget} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {html, LitElement, TemplateResult} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {choose} from 'lit/directives/choose.js';
import {OrWidgetConfig, OrWidgetEntity} from "./or-base-widget";
import {SettingsPanelType, widgetSettingsStyling} from "../or-dashboard-settingspanel";
import {style} from "../style";
import manager, { Util } from "@openremote/core";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import moment from "moment";
import {TimePresetCallback} from "@openremote/or-chart";

export interface ChartWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    datapointQuery: AssetDatapointQueryUnion;
    chartOptions?: any; // ChartConfiguration<"line", ScatterDataPoint[]>
    showTimestampControls: boolean;
    defaultTimePresetKey: string;
    showLegend: boolean;
}

const timePresetOptions = new Map<string, TimePresetCallback>([
    ["lastHour", (date: Date) => [moment(date).subtract(1, 'hour').toDate(), date]],
    ["last24Hours", (date: Date) => [moment(date).subtract(24, 'hours').toDate(), date]],
    ["last7Days", (date: Date) => [moment(date).subtract(7, 'days').toDate(), date]],
    ["last30Days", (date: Date) => [moment(date).subtract(30, 'days').toDate(), date]],
    ["last90Days", (date: Date) => [moment(date).subtract(90, 'days').toDate(), date]],
    ["last6Months", (date: Date) => [moment(date).subtract(6, 'months').toDate(), date]],
    ["lastYear", (date: Date) => [moment(date).subtract(1, 'year').toDate(), date]],
    ["thisHour", (date: Date) => [moment(date).startOf('hour').toDate(), moment(date).endOf('hour').toDate()]],
    ["thisDay", (date: Date) => [moment(date).startOf('day').toDate(), moment(date).endOf('day').toDate()]],
    ["thisWeek", (date: Date) => [moment(date).startOf('isoWeek').toDate(), moment(date).endOf('isoWeek').toDate()]],
    ["thisMonth", (date: Date) => [moment(date).startOf('month').toDate(), moment(date).endOf('month').toDate()]],
    ["thisYear", (date: Date) => [moment(date).startOf('year').toDate(), moment(date).endOf('year').toDate()]],
    ["yesterday", (date: Date) => [moment(date).subtract(24, 'hours').startOf('day').toDate(), moment(date).subtract(24, 'hours').endOf('day').toDate()]],
    ["thisDayLastWeek", (date: Date) => [moment(date).subtract(1, 'week').startOf('day').toDate(), moment(date).subtract(1, 'week').endOf('day').toDate()]],
    ["previousWeek", (date: Date) => [moment(date).subtract(1, 'week').startOf('isoWeek').toDate(), moment(date).subtract(1, 'week').endOf('isoWeek').toDate()]],
    ["previousMonth", (date: Date) => [moment(date).subtract(1, 'month').startOf('month').toDate(), moment(date).subtract(1, 'month').endOf('month').toDate()]],
    ["previousYear", (date: Date) => [moment(date).subtract(1, 'year').startOf('year').toDate(), moment(date).subtract(1, 'year').endOf('year').toDate()]]
])

export class OrChartWidget implements OrWidgetEntity {

    // Properties
    readonly DISPLAY_NAME: string = "Line Chart";
    readonly DISPLAY_MDI_ICON: string = "chart-bell-curve-cumulative"; // https://materialdesignicons.com;
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_WIDTH: number = 0;
    readonly MIN_PIXEL_HEIGHT: number = 0;

    getDefaultConfig(widget: DashboardWidget): ChartWidgetConfig {
        const preset = "last24Hours"
        const dateFunc = timePresetOptions.get(preset);
        const dates = dateFunc!(new Date());
        return {
            displayName: widget?.displayName,
            attributeRefs: [],
            datapointQuery: {
                type: "lttb",
                fromTimestamp: dates[0].getTime(),
                toTimestamp: dates[1].getTime(),
                amountOfPoints: 100
            },
            chartOptions: {
                options: {
                    scales: {
                        y: {
                            min: undefined,
                            max: undefined
                        }
                    }
                },
            },
            showTimestampControls: false,
            defaultTimePresetKey: preset,
            showLegend: true
        } as ChartWidgetConfig;
    }

    // Triggered every update to double check if the specification.
    // It will merge missing values, or you can add custom logic to process here.
    verifyConfigSpec(widget: DashboardWidget): ChartWidgetConfig {
        const defaultConfig = this.getDefaultConfig(widget);
        return Util.mergeObjects(defaultConfig, widget.widgetConfig, false) as ChartWidgetConfig;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string): TemplateResult {
        return html`<or-chart-widget .widget="${widget}" .editMode="${editMode}" .realm="${realm}"></or-chart-widget>`;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string): TemplateResult {
        return html`<or-chart-widgetsettings .widget="${widget}" .realm="${realm}"></or-chart-widgetsettings>`;
    }

}




@customElement('or-chart-widget')
export class OrChartWidgetContent extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    @property()
    public editMode?: boolean;

    @property()
    public realm?: string;

    @state()
    private assets: Asset[] = [];

    @state()
    private assetAttributes: [number, Attribute<any>][] = [];

    /* ---------- */

    render() {
        return html`
            ${when(this.assets && this.assetAttributes && this.assets.length > 0 && this.assetAttributes.length > 0, () => {
                return html`
                    <or-chart .assets="${this.assets}" 
                              .assetAttributes="${this.assetAttributes}" 
                              .realm="${this.realm}" 
                              .showLegend="${(this.widget?.widgetConfig?.showLegend != null) ? this.widget?.widgetConfig?.showLegend : true}" 
                              .attributeControls="${false}" 
                              .timestampControls="${!this.editMode && this.widget?.widgetConfig?.showTimestampControls}" 
                              .algorithm="${this.widget?.widgetConfig?.algorithm}" 
                              .timePresetOptions="${timePresetOptions}" 
                              .timePresetKey="${this.widget?.widgetConfig?.defaultTimePresetKey}" 
                              .datapointQuery="${this.widget?.widgetConfig?.datapointQuery}" 
                              .chartOptions="${this.widget?.widgetConfig?.chartOptions}" 
                              style="height: 100%"
                    ></or-chart>
                `;
            }, () => {
                return html`
                    <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                        <span>${i18next.t('noAttributesConnected')}</span>
                    </div>
                `
            })}
        `
    }

    willUpdate(changedProperties: Map<string, any>) {

        // Add datapointQuery if not set yet (due to migration)
        if(this.widget && this.widget.widgetConfig.datapointQuery == undefined) {
            this.widget.widgetConfig.datapointQuery = {
                type: "lttb",
                fromTimestamp: moment().set('minute', -60).toDate().getTime(),
                toTimestamp: moment().set('minute', 60).toDate().getTime(),
                amountOfPoints: 100
            };
            if(!changedProperties.has("widget")) {
                changedProperties.set("widget", this.widget);
            }
        }
        super.willUpdate(changedProperties);
    }

    updated(changedProperties: Map<string, any>) {

        // Fetch assets
        if(changedProperties.has("widget") || changedProperties.has("editMode")) {
            if(this.assetAttributes.length != this.widget!.widgetConfig.attributeRefs.length) {
                this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
                    this.assets = assets!;
                    this.assetAttributes = this.widget?.widgetConfig.attributeRefs.map((attrRef: AttributeRef) => {
                        const assetIndex = assets!.findIndex((asset) => asset.id === attrRef.id);
                        const foundAsset = assetIndex >= 0 ? assets![assetIndex] : undefined;
                        return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
                    }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                });
            }
        }
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required. TODO: Simplify this to only request data needed for attribute list
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if(config.attributeRefs && config.attributeRefs.length > 0) {
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => x.id) as string[],
                select: {
                    attributes: config.attributeRefs?.map((x: AttributeRef) => x.name) as string[]
                }
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }


    /* --------------------------- */

    // Caching and generation of mockData, to prevent API call to be made.
    // Is currently not used anymore, but can be used with the dataProvider on or-chart.

    @state()
    private cachedMockData?: Map<string, { period: any, data: any[] }> = new Map<string, { period: any, data: any[] }>();

    generateMockData(widget: DashboardWidget, startOfPeriod: number, _endOfPeriod: number, amount: number = 10) {
        const mockTime: number = startOfPeriod;
        const chartData: any[] = [];
        const interval = (Date.now() - startOfPeriod) / amount;

        // Generating random coordinates on the chart
        let data: any[] = [];
        const cached: { period: any, data: any[] } | undefined = this.cachedMockData?.get(widget.id!);
        if(cached && (cached.data.length == widget.widgetConfig?.attributeRefs?.length) && (cached.period == widget.widgetConfig?.period)) {
            data = this.cachedMockData?.get(widget.id!)!.data!;
        } else {
            widget.widgetConfig?.attributeRefs?.forEach((_attrRef: AttributeRef) => {
                let valueEntries: any[] = [];
                let prevValue: number = 100;
                for(let i = 0; i < amount; i++) {
                    const value = Math.floor(Math.random() * ((prevValue + 2) - (prevValue - 2)) + (prevValue - 2))
                    valueEntries.push({
                        x: (mockTime + (i * interval)),
                        y: value
                    });
                    prevValue = value;
                }
                data.push(valueEntries);
            });
            this.cachedMockData?.set(widget.id!, { period: widget.widgetConfig?.period, data: data });
        }

        // Making a line for each attribute
        widget.widgetConfig?.attributeRefs?.forEach((attrRef: AttributeRef) => {
            chartData.push({
                backgroundColor: ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"][chartData.length],
                borderColor: ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"][chartData.length],
                data: data[chartData.length],
                fill: false,
                label: attrRef.name,
                pointRadius: 2
            });
        });
        return chartData;
    }
}







@customElement("or-chart-widgetsettings")
class OrChartWidgetSettings extends LitElement {

    @property()
    public widget?: DashboardWidget;

    // Default values
    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('dataSampling'), i18next.t('display'), i18next.t('dashboard.chartConfig')];


    static get styles() {
        return [style, widgetSettingsStyling];
    }

    willUpdate(changedProperties: Map<string, any>) {

        // Add datapointQuery if not set yet (due to migration)
        if(this.widget && this.widget.widgetConfig.datapointQuery == undefined) {
            this.widget.widgetConfig.datapointQuery = {
                type: "lttb",
                fromTimestamp: moment().set('minute', -60).toDate().getTime(),
                toTimestamp: moment().set('minute', 60).toDate().getTime(),
                amountOfPoints: 100
            };
            if(!changedProperties.has("widget")) {
                changedProperties.set("widget", this.widget);
            }
        }
        super.willUpdate(changedProperties);
    }

    // UI Rendering
    render() {
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as ChartWidgetConfig; // duplicate to edit, to prevent parent updates. Please trigger updateConfig()
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.MULTI_ATTRIBUTE}" .widgetConfig="${this.widget?.widgetConfig}"
                                                .attributeFilter="${(attribute: Attribute<any>) => {
                                                    return ["positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction", "boolean"]
                                                      .includes(attribute.type!)}}"
                                                @updated="${(event: CustomEvent) => { this.updateConfig(this.widget!, event.detail.changes.get('config')) }}"
                    ></or-dashboard-settingspanel>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('display'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('display')) ? html`
                    <div class="expanded-panel">
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('timeframeDefault')}" style="width: 100%;"
                                          .options="${Array.from(timePresetOptions.keys())}" value="${config.defaultTimePresetKey}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              config.defaultTimePresetKey = event.detail.value.toString();
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <div class="switchMwcInputContainer">
                                <span>${i18next.t('dashboard.allowTimerangeSelect')}</span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${config.showTimestampControls}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  config.showTimestampControls = event.detail.value;
                                                  this.updateConfig(this.widget!, config);
                                              }}"
                                ></or-mwc-input>
                            </div>
                            <div class="switchMwcInputContainer">
                                <span>${i18next.t('dashboard.showLegend')}</span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${config.showLegend}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  config.showLegend = event.detail.value;
                                                  this.updateConfig(this.widget!, config);
                                              }}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </div>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('dashboard.chartConfig'))}
            </div>
            <div>
                ${when(this.expandedPanels.includes(i18next.t('dashboard.chartConfig')), () => {
                    const min = config.chartOptions.options?.scales?.y?.min;
                    const max = config.chartOptions.options?.scales?.y?.max;
                    return html`
                        <div class="expanded-panel">
                            <div>
                                <div style="display: flex;">
                                    ${max != undefined ? html`
                                        <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" .value="${max}" style="width: 100%;"
                                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                          config.chartOptions.options.scales.y.max = event.detail.value;
                                                          this.updateConfig(this.widget!, config);
                                                      }}"
                                        ></or-mwc-input>
                                    ` : html`
                                        <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                                    `}
                                    <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${max != undefined}"
                                                  @or-mwc-input-changed="${() => {
                                                      config.chartOptions.options!.scales!.y!.max = (max == undefined) ? 100 : undefined;
                                                      this.updateConfig(this.widget!, config);
                                                  }}"
                                    ></or-mwc-input>
                                </div>
                                <div style="display: flex; margin-top: 12px;">
                                    ${min != undefined ? html`
                                        <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" .value="${min}" style="width: 100%;"
                                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                          config.chartOptions.options.scales.y.min = event.detail.value;
                                                          this.updateConfig(this.widget!, config);
                                                      }}"
                                        ></or-mwc-input>
                                    ` : html`
                                        <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                                    `}
                                    <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${min != undefined}"
                                                  @or-mwc-input-changed="${() => {
                                                      config.chartOptions.options!.scales!.y!.min = (min == undefined) ? 0 : undefined;
                                                      this.updateConfig(this.widget!, config);
                                                  }}"
                                    ></or-mwc-input>
                                </div>
                            </div>
                        </div>
                    `
                })}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('dataSampling'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('dataSampling')) ? html`
                    ${when(config.datapointQuery, () => {
                        const typeOptions = new Map<string, string>([["lttb", 'lttb'], ["withInterval", 'interval']]);
                        const typeValue = Array.from(typeOptions.entries()).find((entry => entry[1] == config.datapointQuery.type))![0]
                        return html`
                            <div class="expanded-panel">
                                <or-mwc-input .type="${InputType.SELECT}" style="width: 100%" .options="${Array.from(typeOptions.keys())}"
                                              .value="${typeValue}" label="${i18next.t('algorithm')}" @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  config.datapointQuery.type = typeOptions.get(event.detail.value)! as any;
                                                  this.updateConfig(this.widget!, config, true);
                                              }}"
                                ></or-mwc-input>
                                ${choose(config.datapointQuery.type, [
                                    ['interval', () => {
                                        const intervalQuery = config.datapointQuery as AssetDatapointIntervalQuery;
                                        const formulaOptions = [AssetDatapointIntervalQueryFormula.AVG, AssetDatapointIntervalQueryFormula.MIN, AssetDatapointIntervalQueryFormula.MAX];
                                        return html`
                                            <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${formulaOptions}"
                                                          .value="${intervalQuery.formula}" label="${i18next.t('algorithmMethod')}" @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                              intervalQuery.formula = event.detail.value;
                                                              this.updateConfig(this.widget!, config, true);
                                                          }}"
                                            ></or-mwc-input>
                                        `;
                                    }]
                                ])}
                            </div>
                        `
                    }, () => {
                        console.error(config);
                        return html`${i18next.t('errorOccurred')}`;
                    })}
                ` : null}
            </div>
        `
    }

    updateConfig(widget: DashboardWidget, config: OrWidgetConfig | any, force: boolean = false) {
        const oldWidget = JSON.parse(JSON.stringify(widget)) as DashboardWidget;

        if(config.datapointQuery) {
            switch (config.datapointQuery.type) {
                case 'lttb': {
                    const query = config.datapointQuery as AssetDatapointLTTBQuery;
                    if(!query.amountOfPoints) { query.amountOfPoints = 100; }
                    break;
                }
                case 'interval': {
                    const query = config.datapointQuery as AssetDatapointIntervalQuery;
                    if(!query.interval) { query.interval = "1 hour"; }
                    if(!query.gapFill) { query.gapFill = false; }
                    if(!query.formula) { query.formula = AssetDatapointIntervalQueryFormula.AVG; }
                }
            }
        }

        widget.widgetConfig = config;
        this.requestUpdate("widget", oldWidget);
        this.forceParentUpdate(new Map<string, any>([["widget", widget]]), force);
    }

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px; height: 25px; line-height: 25px;">${name}</span>
            </span>
        `
    }
    expandPanel(panelName: string): void {
        if (this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if (indexOf > -1) {
                this.expandedPanels.splice(indexOf, 1);
            }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }
}
