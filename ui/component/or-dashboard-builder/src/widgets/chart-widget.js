var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var ChartWidget_1;
import { html } from "lit";
import { when } from "lit/directives/when.js";
import moment from "moment";
import { OrAssetWidget } from "../util/or-asset-widget";
import { customElement, state } from "lit/decorators.js";
import { ChartSettings } from "../settings/chart-settings";
import "@openremote/or-chart";
function getDefaultTimePresetOptions() {
    return new Map([
        ["lastHour", (date) => [moment(date).subtract(1, 'hour').toDate(), date]],
        ["last24Hours", (date) => [moment(date).subtract(24, 'hours').toDate(), date]],
        ["last7Days", (date) => [moment(date).subtract(7, 'days').toDate(), date]],
        ["last30Days", (date) => [moment(date).subtract(30, 'days').toDate(), date]],
        ["last90Days", (date) => [moment(date).subtract(90, 'days').toDate(), date]],
        ["last6Months", (date) => [moment(date).subtract(6, 'months').toDate(), date]],
        ["lastYear", (date) => [moment(date).subtract(1, 'year').toDate(), date]],
        ["thisHour", (date) => [moment(date).startOf('hour').toDate(), moment(date).endOf('hour').toDate()]],
        ["thisDay", (date) => [moment(date).startOf('day').toDate(), moment(date).endOf('day').toDate()]],
        ["thisWeek", (date) => [moment(date).startOf('isoWeek').toDate(), moment(date).endOf('isoWeek').toDate()]],
        ["thisMonth", (date) => [moment(date).startOf('month').toDate(), moment(date).endOf('month').toDate()]],
        ["thisYear", (date) => [moment(date).startOf('year').toDate(), moment(date).endOf('year').toDate()]],
        ["yesterday", (date) => [moment(date).subtract(24, 'hours').startOf('day').toDate(), moment(date).subtract(24, 'hours').endOf('day').toDate()]],
        ["thisDayLastWeek", (date) => [moment(date).subtract(1, 'week').startOf('day').toDate(), moment(date).subtract(1, 'week').endOf('day').toDate()]],
        ["previousWeek", (date) => [moment(date).subtract(1, 'week').startOf('isoWeek').toDate(), moment(date).subtract(1, 'week').endOf('isoWeek').toDate()]],
        ["previousMonth", (date) => [moment(date).subtract(1, 'month').startOf('month').toDate(), moment(date).subtract(1, 'month').endOf('month').toDate()]],
        ["previousYear", (date) => [moment(date).subtract(1, 'year').startOf('year').toDate(), moment(date).subtract(1, 'year').endOf('year').toDate()]]
    ]);
}
function getDefaultSamplingOptions() {
    return new Map([["lttb", 'lttb'], ["withInterval", 'interval']]);
}
function getDefaultWidgetConfig() {
    const preset = "last24Hours";
    const dateFunc = getDefaultTimePresetOptions().get(preset);
    const dates = dateFunc(new Date());
    return {
        attributeRefs: [],
        rightAxisAttributes: [],
        datapointQuery: {
            type: "lttb",
            fromTimestamp: dates[0].getTime(),
            toTimestamp: dates[1].getTime()
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
        defaultTimePresetKey: preset,
        showLegend: true
    };
}
/* --------------------------------------------------- */
let ChartWidget = ChartWidget_1 = class ChartWidget extends OrAssetWidget {
    static getManifest() {
        return {
            displayName: "Line Chart",
            displayIcon: "chart-line",
            minColumnWidth: 2,
            minColumnHeight: 2,
            getContentHtml(config) {
                return new ChartWidget_1(config);
            },
            getSettingsHtml(config) {
                const settings = new ChartSettings(config);
                settings.setTimePresetOptions(getDefaultTimePresetOptions());
                settings.setSamplingOptions(getDefaultSamplingOptions());
                return settings;
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            }
        };
    }
    // Method called on every refresh/reload of the widget
    // We either refresh the datapointQuery or the full widgetConfig depending on the force parameter.
    // TODO: Improve this to a more efficient approach, instead of duplicating the object
    refreshContent(force) {
        if (!force) {
            const datapointQuery = JSON.parse(JSON.stringify(this.widgetConfig.datapointQuery));
            datapointQuery.fromTimestamp = undefined;
            datapointQuery.toTimestamp = undefined;
            this.datapointQuery = datapointQuery;
        }
        else {
            this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig));
        }
    }
    /* ---------------------------------- */
    // WebComponent lifecycle method, that occurs DURING every state update
    willUpdate(changedProps) {
        // Add datapointQuery if not set yet (due to migration)
        if (!this.widgetConfig.datapointQuery) {
            this.widgetConfig.datapointQuery = this.getDefaultQuery();
            if (!changedProps.has("widgetConfig")) {
                changedProps.set("widgetConfig", this.widgetConfig);
            }
        }
        if (changedProps.has('widgetConfig') && this.widgetConfig) {
            this.datapointQuery = this.widgetConfig.datapointQuery;
        }
        return super.willUpdate(changedProps);
    }
    // WebComponent lifecycle method, that occurs AFTER every state update
    updated(changedProps) {
        super.updated(changedProps);
        // If widgetConfig, and the attributeRefs of them have changed...
        if (changedProps.has("widgetConfig") && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;
            const missingAssets = attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.filter((attrRef) => !this.isAttributeRefLoaded(attrRef));
            if (missingAssets.length > 0) {
                // Fetch the new list of assets
                this.fetchAssets(attributeRefs).then((assets) => {
                    this.loadedAssets = assets;
                    this.assetAttributes = attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.map((attrRef) => {
                        const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                        const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                        return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name]] : undefined;
                    }).filter((indexAndAttr) => !!indexAndAttr);
                });
            }
        }
        return super.updated(changedProps);
    }
    render() {
        return html `
            ${when(this.loadedAssets && this.assetAttributes && this.loadedAssets.length > 0 && this.assetAttributes.length > 0, () => {
            var _a, _b, _c, _d, _e;
            return html `
                    <or-chart .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}" .rightAxisAttributes="${this.widgetConfig.rightAxisAttributes}"
                              .showLegend="${(((_a = this.widgetConfig) === null || _a === void 0 ? void 0 : _a.showLegend) != null) ? (_b = this.widgetConfig) === null || _b === void 0 ? void 0 : _b.showLegend : true}"
                              .attributeControls="${false}" .timestampControls="${!((_c = this.widgetConfig) === null || _c === void 0 ? void 0 : _c.showTimestampControls)}"
                              .timePresetOptions="${getDefaultTimePresetOptions()}" .timePresetKey="${(_d = this.widgetConfig) === null || _d === void 0 ? void 0 : _d.defaultTimePresetKey}"
                              .datapointQuery="${this.datapointQuery}" .chartOptions="${(_e = this.widgetConfig) === null || _e === void 0 ? void 0 : _e.chartOptions}"
                              style="height: 100%"
                    ></or-chart>
                `;
        }, () => {
            return html `
                    <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                        <span><or-translate value="noAttributesConnected"></or-translate></span>
                    </div>
                `;
        })}
        `;
    }
    getDefaultQuery() {
        return {
            type: "lttb",
            fromTimestamp: moment().set('minute', -60).toDate().getTime(),
            toTimestamp: moment().set('minute', 60).toDate().getTime()
        };
    }
};
__decorate([
    state()
], ChartWidget.prototype, "datapointQuery", void 0);
ChartWidget = ChartWidget_1 = __decorate([
    customElement('chart-widget')
], ChartWidget);
export { ChartWidget };
//# sourceMappingURL=chart-widget.js.map