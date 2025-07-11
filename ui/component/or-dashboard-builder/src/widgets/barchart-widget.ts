import {
    AssetDatapointIntervalQuery,
    AssetDatapointQueryUnion,
    Attribute,
    AttributeRef,
    DatapointInterval
} from "@openremote/model";
import {html, PropertyValues, TemplateResult } from "lit";
import { when } from "lit/directives/when.js";
import moment from "moment";
import {OrAssetWidget} from "../util/or-asset-widget";
import { customElement, state } from "lit/decorators.js";
import {WidgetConfig} from "../util/widget-config";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {BarChartSettings} from "../settings/barchart-settings";
import {WidgetSettings} from "../util/widget-settings";
import "@openremote/or-attribute-barchart";
import {IntervalConfig} from "@openremote/or-attribute-barchart";

export interface BarChartWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    colorPickedAttributes: Array<{ attributeRef: AttributeRef; color: string }>;
    attributeSettings: {
        rightAxisAttributes: AttributeRef[],
        methodAvgAttributes: AttributeRef[],
        methodMinAttributes: AttributeRef[],
        methodMaxAttributes: AttributeRef[],
        methodDeltaAttributes: AttributeRef[],
        methodMedianAttributes: AttributeRef[],
        methodModeAttributes: AttributeRef[],
        methodSumAttributes: AttributeRef[],
        methodCountAttributes: AttributeRef[],
    },
    datapointQuery: AssetDatapointQueryUnion;
    chartOptions?: any;
    showTimestampControls: boolean;
    defaultTimeWindowKey: string;
    defaultTimePrefixKey: string;
    defaultInterval: string;
    chartSettings: {
        showLegend: boolean;
        showToolBox: boolean;
        defaultStacked: boolean;
    };
    decimals: number;
}

function getDefaultTimeWindowOptions(): Map<string, [moment.unitOfTime.DurationConstructor, number]> {
    return new Map<string, [moment.unitOfTime.DurationConstructor, number]>([
        ["5Minutes", ['minutes', 5]],
        ["30Minutes", ['minutes', 30]],
        ["60Minutes", ['minutes', 60]],
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

function getDefaultTimePreFixOptions(): string[] {
    return ["this", "last"];
}

function getDefaultIntervalOptions(): Map<string, IntervalConfig> {
    return new Map<string, IntervalConfig>([
        ["auto", {intervalName:"auto", steps: 1, orFormat: DatapointInterval.MINUTE, momentFormat: "minutes", millis: 60000}],
        ["one", {intervalName:"one", steps:1, orFormat: DatapointInterval.MINUTE,momentFormat:"minutes", millis: 60000}],
        ["1Minute", {intervalName:"1Minute", steps:1, orFormat:DatapointInterval.MINUTE,momentFormat:"minutes", millis: 60000}],
        ["5Minutes", {intervalName:"5Minutes", steps:5, orFormat:DatapointInterval.MINUTE,momentFormat:"minutes", millis: 300000}],
        ["30Minutes", {intervalName:"30Minutes", steps:30, orFormat:DatapointInterval.MINUTE,momentFormat:"minutes", millis: 1800000}],
        ["hour", {intervalName:"hour", steps:1, orFormat:DatapointInterval.HOUR,momentFormat:"hours", millis: 3600000}],
        ["day", {intervalName:"day", steps:1, orFormat:DatapointInterval.DAY,momentFormat:"days", millis: 86400000}],
        ["week", {intervalName:"week", steps:1, orFormat:DatapointInterval.WEEK,momentFormat:"weeks", millis: 604800000}],
        ["month", {intervalName:"month", steps:1, orFormat:DatapointInterval.MONTH,momentFormat:"months", millis: 2592000000}],
        ["year", {intervalName:"year", steps:1, orFormat:DatapointInterval.MINUTE,momentFormat:"years", millis: 31536000000}]
    ]);
}


function getDefaultWidgetConfig(): BarChartWidgetConfig {
    const preset = "30Days";  // Default time preset, "last" prefix is hardcoded in startDate and endDate below.
    const dateFunc = getDefaultTimeWindowOptions().get(preset);
    const startDate = moment().subtract(dateFunc![1], dateFunc![0]).startOf(dateFunc![0]);
    const endDate = dateFunc![1]== 1 ? moment().endOf(dateFunc![0]) : moment();
    return {
        attributeRefs: [],
        colorPickedAttributes: [],
        attributeSettings: {
            rightAxisAttributes: [],
            methodAvgAttributes: [],
            methodMinAttributes: [],
            methodMaxAttributes: [],
            methodDeltaAttributes: [],
            methodMedianAttributes: [],
            methodModeAttributes: [],
            methodSumAttributes: [],
            methodCountAttributes: []
        },
        datapointQuery: {
            type: "interval",
            fromTimestamp: startDate.toDate().getTime(),
            toTimestamp: endDate.toDate().getTime(),
        },
        chartOptions: {
            options: {
                scales: {
                    y: {
                        min: undefined,
                        max: undefined
                    },
                    y1: {
                        min: undefined,
                        max: undefined
                    }
                }
            },
        },
        showTimestampControls: false,
        defaultTimeWindowKey: preset,
        defaultTimePrefixKey: "last",
        defaultInterval: 'auto',
        chartSettings: {
            showLegend: true,
            showToolBox: false,
            defaultStacked: false,
        },
        decimals: 2
    };
}

/* --------------------------------------------------- */

@customElement('barchart-widget')
export class BarChartWidget extends OrAssetWidget {

    @state()
    protected datapointQuery!: AssetDatapointQueryUnion;

    @state()
    protected _loading = false;

    // Override of widgetConfig with extended type
    protected widgetConfig!: BarChartWidgetConfig;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Interval-Bar-Chart",
            displayIcon: "chart-bar",
            minColumnWidth: 2,
            minColumnHeight: 2,
            getContentHtml(config: BarChartWidgetConfig): OrWidget {
                return new BarChartWidget(config);
            },
            getSettingsHtml(config: BarChartWidgetConfig): WidgetSettings {
                const settings = new BarChartSettings(config);
                settings.setTimeWindowOptions(getDefaultTimeWindowOptions());
                settings.setTimePrefixOptions(getDefaultTimePreFixOptions());
                settings.setIntervalOptions(getDefaultIntervalOptions());
                return settings;
            },
            getDefaultConfig(): BarChartWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    // Method called on every refresh/reload of the widget
    // We either refresh the datapointQuery or the full widgetConfig depending on the force parameter.
    // TODO: Improve this to a more efficient approach, instead of duplicating the object
    public refreshContent(force: boolean) {
        if(!force) {
            const datapointQuery = JSON.parse(JSON.stringify(this.widgetConfig.datapointQuery)) as AssetDatapointQueryUnion;
            datapointQuery.fromTimestamp = undefined;
            datapointQuery.toTimestamp = undefined;
            this.datapointQuery = datapointQuery;
        } else {
            this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as BarChartWidgetConfig;
        }
    }


    /* ---------------------------------- */

    // WebComponent lifecycle method, that occurs DURING every state update
    protected willUpdate(changedProps: PropertyValues) {

        // Add datapointQuery if not set yet (due to migration)
        if(!this.widgetConfig.datapointQuery) {
            this.widgetConfig.datapointQuery = this.getDefaultQuery();
            if(!changedProps.has("widgetConfig")) {
                changedProps.set("widgetConfig", this.widgetConfig);
            }
        }

        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.datapointQuery = this.widgetConfig.datapointQuery;

            const attributeRefs = this.widgetConfig.attributeRefs;
            if(attributeRefs.length === 0) {
                this._error = "noAttributesConnected";
            } else {
                const missingAssets = attributeRefs?.filter((attrRef: AttributeRef) => !this.isAttributeRefLoaded(attrRef));
                if (missingAssets.length > 0) {
                    this.loadAssets(attributeRefs);
                }
            }
        }

        return super.willUpdate(changedProps);
    }

    protected loadAssets(attributeRefs: AttributeRef[]): void {
        if(attributeRefs.length === 0) {
            this._error = "noAttributesConnected";
            return;
        }
        this._loading = true;
        this._error = undefined;
        this.fetchAssets(attributeRefs).then((assets) => {
            this.loadedAssets = assets;
            this.assetAttributes = attributeRefs?.map((attrRef: AttributeRef) => {
                const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
            }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
        }).catch(e => {
            this._error = e.message;
        }).finally(() => {
            this._loading = false;
        });
    }

    protected render(): TemplateResult {
        return html`
            ${when(this._loading, () => html`
                <or-loading-indicator></or-loading-indicator>
                
            `, () => when(this._error, () => html`
                <div style="height: 100%; display: flex; justify-content: center; align-items: center; text-align: center;">
                    <span><or-translate .value="${this._error}"></or-translate></span>
                </div>
                
            `, () => {
                return html`
                    <or-attribute-barchart .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}"
                              .colorPickedAttributes="${this.widgetConfig?.colorPickedAttributes != null ? this.widgetConfig?.colorPickedAttributes : []}"
                              .attributeSettings="${this.widgetConfig?.attributeSettings != null ? this.widgetConfig.attributeSettings : {}}"
                              .chartSettings="${this.widgetConfig?.chartSettings}"
                              .attributeControls="${false}" .timestampControls="${!this.widgetConfig?.showTimestampControls}"
                              .timeWindowOptions="${getDefaultTimeWindowOptions()}"
                              .timePrefixOptions="${getDefaultTimePreFixOptions()}"
                              .intervalOptions="${getDefaultIntervalOptions()}"
                              .interval="${this.widgetConfig?.defaultInterval}"
                              .timePrefixKey="${this.widgetConfig?.defaultTimePrefixKey}"
                              .timeWindowKey="${this.widgetConfig?.defaultTimeWindowKey}"
                              .datapointQuery="${this.datapointQuery}" .chartOptions="${this.widgetConfig?.chartOptions}"
                              .decimals="${this.widgetConfig?.decimals}"
                              style="height: 100%"
                    ></or-attribute-barchart>
                `;
            }))}
        `;
    }

    protected getDefaultQuery(): AssetDatapointIntervalQuery {
        return {
            type: "interval",
            fromTimestamp: moment().set('day', -30).toDate().getTime(),
            toTimestamp: moment().toDate().getTime()
        }
    }
}
