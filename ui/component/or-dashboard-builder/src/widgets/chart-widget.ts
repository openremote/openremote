import {AssetDatapointLTTBQuery, AssetDatapointQueryUnion, Attribute, AttributeRef} from "@openremote/model";
import {html, PropertyValues, TemplateResult } from "lit";
import { when } from "lit/directives/when.js";
import moment from "moment";
import {OrAssetWidget} from "../util/or-asset-widget";
import { customElement, state } from "lit/decorators.js";
import {WidgetConfig} from "../util/widget-config";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {ChartSettings} from "../settings/chart-settings";
import {WidgetSettings} from "../util/widget-settings";
import "@openremote/or-chart";

export interface ChartWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    colorPickedAttributes: Array<{ attributeRef: AttributeRef; color: string }>;
    attributeSettings: {
        rightAxisAttributes: AttributeRef[],
        smoothAttributes: AttributeRef[],
        steppedAttributes: AttributeRef[],
        areaAttributes: AttributeRef[],
        faintAttributes: AttributeRef[],
        extendedAttributes: AttributeRef[],
    },
    datapointQuery: AssetDatapointQueryUnion;
    chartOptions?: any;
    showTimestampControls: boolean;
    defaultTimeWindowKey: string;
    defaultTimePrefixKey: string;
    showLegend: boolean;
    showZoomBar: boolean;
    showToolBox: boolean;
    showSymbolMaxDatapoints: number;
    maxConcurrentDatapoints: number;
}

function getDefaultTimeWindowOptions(): Map<string, [moment.unitOfTime.DurationConstructor, number]> {
    return new Map<string, [moment.unitOfTime.DurationConstructor, number]>([
        ["5Minutes", ['minutes', 5]],
        ["20Minutes", ['minutes', 20]],
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

function getDefaultSamplingOptions(): Map<string, string> {
    return new Map<string, string>([["lttb", 'lttb'], ["withInterval", 'interval']]);
}

function getDefaultWidgetConfig(): ChartWidgetConfig {
    const preset = "24Hours";  // Default time preset, "last" prefix is hardcoded in startDate and endDate below.
    const dateFunc = getDefaultTimeWindowOptions().get(preset);
    const startDate = moment().subtract(dateFunc![1], dateFunc![0]).startOf(dateFunc![0]);
    const endDate = dateFunc![1]== 1 ? moment().endOf(dateFunc![0]) : moment();
    return {
        attributeRefs: [],
        colorPickedAttributes: [],
        attributeSettings: {
            rightAxisAttributes: [],
            smoothAttributes: [],
            steppedAttributes: [],
            areaAttributes: [],
            faintAttributes: [],
            extendedAttributes: [],
        },
        datapointQuery: {
            type: "lttb",
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
        showLegend: true,
        showZoomBar: false,
        showToolBox: false,
        showSymbolMaxDatapoints: 30,
        maxConcurrentDatapoints: 100

    };
}

/* --------------------------------------------------- */

@customElement('chart-widget')
export class ChartWidget extends OrAssetWidget {

    @state()
    protected datapointQuery!: AssetDatapointQueryUnion;

    @state()
    protected _loading = false;

    // Override of widgetConfig with extended type
    protected widgetConfig!: ChartWidgetConfig;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Line Chart",
            displayIcon: "chart-line",
            minColumnWidth: 2,
            minColumnHeight: 2,
            getContentHtml(config: ChartWidgetConfig): OrWidget {
                return new ChartWidget(config);
            },
            getSettingsHtml(config: ChartWidgetConfig): WidgetSettings {
                const settings = new ChartSettings(config);
                settings.setTimeWindowOptions(getDefaultTimeWindowOptions());
                settings.setTimePrefixOptions(getDefaultTimePreFixOptions());
                settings.setSamplingOptions(getDefaultSamplingOptions());
                return settings;
            },
            getDefaultConfig(): ChartWidgetConfig {
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
            this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as ChartWidgetConfig;
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
                    <or-chart .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}"
                              .colorPickedAttributes="${this.widgetConfig?.colorPickedAttributes != null ? this.widgetConfig?.colorPickedAttributes : []}"
                              .attributeSettings="${this.widgetConfig?.attributeSettings != null ? this.widgetConfig.attributeSettings : {}}"
                              .showLegend="${(this.widgetConfig?.showLegend != null) ? this.widgetConfig?.showLegend : true}"
                              .showZoomBar="${(this.widgetConfig?.showZoomBar != null) ? this.widgetConfig?.showZoomBar : true}"
                              .showToolBox="${(this.widgetConfig?.showToolBox != null) ? this.widgetConfig?.showToolBox : true}"
                              .attributeControls="${false}" .timestampControls="${!this.widgetConfig?.showTimestampControls}"
                              .timeWindowOptions="${getDefaultTimeWindowOptions()}"
                              .timePrefixOptions="${getDefaultTimePreFixOptions()}"
                              .timePrefixKey="${this.widgetConfig?.defaultTimePrefixKey}"
                              .timeWindowKey="${this.widgetConfig?.defaultTimeWindowKey}"
                              .datapointQuery="${this.datapointQuery}" .chartOptions="${this.widgetConfig?.chartOptions}"
                              .showSymbolMaxDatapoints="${this.widgetConfig?.showSymbolMaxDatapoints}"
                              .maxConcurrentDatapoints="${this.widgetConfig?.maxConcurrentDatapoints}"
                              style="height: 100%"
                    ></or-chart>
                `;
            }))}
        `;
    }

    protected getDefaultQuery(): AssetDatapointLTTBQuery {
        return {
            type: "lttb",
            fromTimestamp: moment().set('minute', -60).toDate().getTime(),
            toTimestamp: moment().set('minute', 60).toDate().getTime()
        }
    }
}
