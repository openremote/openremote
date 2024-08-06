var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { WidgetSettings } from "../util/widget-settings";
import "../panels/attributes-panel";
import "../util/settings-panel";
import { i18next } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { when } from "lit/directives/when.js";
const styling = css `
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;
let ChartSettings = class ChartSettings extends WidgetSettings {
    constructor() {
        super(...arguments);
        this.timePresetOptions = new Map();
        this.samplingOptions = new Map();
    }
    setTimePresetOptions(options) {
        this.timePresetOptions = options;
    }
    setSamplingOptions(options) {
        this.samplingOptions = options;
    }
    static get styles() {
        return [...super.styles, styling];
    }
    render() {
        var _a, _b, _c, _d, _e, _f;
        const attributeFilter = (attr) => {
            return ["boolean", "positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction"].includes(attr.type);
        };
        const min = (_c = (_b = (_a = this.widgetConfig.chartOptions.options) === null || _a === void 0 ? void 0 : _a.scales) === null || _b === void 0 ? void 0 : _b.y) === null || _c === void 0 ? void 0 : _c.min;
        const max = (_f = (_e = (_d = this.widgetConfig.chartOptions.options) === null || _d === void 0 ? void 0 : _d.scales) === null || _e === void 0 ? void 0 : _e.y) === null || _f === void 0 ? void 0 : _f.max;
        const isMultiAxis = this.widgetConfig.rightAxisAttributes.length > 0;
        const samplingValue = Array.from(this.samplingOptions.entries()).find((entry => entry[1] === this.widgetConfig.datapointQuery.type))[0];
        const attributeLabelCallback = (asset, attribute, attributeLabel) => {
            const isOnRightAxis = isMultiAxis && this.widgetConfig.rightAxisAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            return html `
                <span>${asset.name}</span>
                <span style="font-size:14px; color:grey;">${attributeLabel}</span>
                ${when(isOnRightAxis, () => html `
                    <span style="position: absolute; right: 0; margin-bottom: 16px; font-size:14px; color:grey;"><or-translate value="right"></or-translate></span>
                `)}
            `;
        };
        const attributeActionCallback = (attributeRef) => {
            return [{
                    icon: this.widgetConfig.rightAxisAttributes.includes(attributeRef) ? "arrow-right-bold" : "arrow-left-bold",
                    tooltip: i18next.t('dashboard.toggleAxis'),
                    disabled: false
                }];
        };
        return html `
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" multi="${true}" onlyDataAttrs="${true}" .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      .attributeLabelCallback="${attributeLabelCallback}" .attributeActionCallback="${attributeActionCallback}"
                                      @attribute-action="${(ev) => this.onAttributeAction(ev)}"
                                      @attribute-select="${(ev) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Display options -->
                <settings-panel displayName="display" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 6px;">
                        <!-- Timeframe -->
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('timeframeDefault')}" style="width: 100%;"
                                          .options="${Array.from(this.timePresetOptions.keys())}" value="${this.widgetConfig.defaultTimePresetKey}"
                                          @or-mwc-input-changed="${(ev) => this.onTimePresetSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Y Min/max options -->
                        <div>
                            <div class="switch-container">
                                <span><or-translate value="dashboard.allowTimerangeSelect"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.showTimestampControls}"
                                              @or-mwc-input-changed="${(ev) => this.onTimestampControlsToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                            <div class="switch-container">
                                <span><or-translate value="dashboard.showLegend"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showLegend}"
                                              @or-mwc-input-changed="${(ev) => this.onShowLegendToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </settings-panel>

                <!-- Axis configuration -->
                <settings-panel displayName="dashboard.axisConfig" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 16px;">

                        <!-- Left axis configuration -->
                        <div>
                            ${when(isMultiAxis, () => html `
                                <div style="margin-bottom: 8px;">
                                    <span><or-translate value="dashboard.leftAxis"></or-translate></span>
                                </div>
                            `)}
                            <div style="display: flex;">
                                ${max !== undefined ? html `
                                    <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" .value="${max}" style="width: 100%;"
                                                  @or-mwc-input-changed="${(ev) => this.onMinMaxValueChange('left', 'max', ev)}"
                                    ></or-mwc-input>
                                ` : html `
                                    <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                                `}
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${max !== undefined}"
                                              @or-mwc-input-changed="${(ev) => this.onMinMaxValueToggle('left', 'max', ev)}"
                                ></or-mwc-input>
                            </div>
                            <div style="display: flex; margin-top: 12px;">
                                ${min !== undefined ? html `
                                    <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" .value="${min}" style="width: 100%;"
                                                  @or-mwc-input-changed="${(ev) => this.onMinMaxValueChange('left', 'min', ev)}"
                                    ></or-mwc-input>
                                ` : html `
                                    <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                                `}
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${min !== undefined}"
                                              @or-mwc-input-changed="${(ev) => this.onMinMaxValueToggle('left', 'min', ev)}"
                                ></or-mwc-input>
                            </div>
                        </div>

                        <!-- Right axis configuration -->
                        ${when(isMultiAxis, () => {
            var _a, _b, _c, _d, _e, _f;
            const rightMin = (_c = (_b = (_a = this.widgetConfig.chartOptions.options) === null || _a === void 0 ? void 0 : _a.scales) === null || _b === void 0 ? void 0 : _b.y1) === null || _c === void 0 ? void 0 : _c.min;
            const rightMax = (_f = (_e = (_d = this.widgetConfig.chartOptions.options) === null || _d === void 0 ? void 0 : _d.scales) === null || _e === void 0 ? void 0 : _e.y1) === null || _f === void 0 ? void 0 : _f.max;
            return html `
                                <div>
                                    <div style="margin-bottom: 8px;">
                                        <span><or-translate value="dashboard.rightAxis"></or-translate></span>
                                    </div>
                                    <div style="display: flex;">
                                        ${rightMax !== undefined ? html `
                                            <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" .value="${rightMax}" style="width: 100%;"
                                                          @or-mwc-input-changed="${(ev) => this.onMinMaxValueChange('right', 'max', ev)}"
                                            ></or-mwc-input>
                                        ` : html `
                                            <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" disabled="true" value="auto"
                                                          style="width: 100%;"></or-mwc-input>
                                        `}
                                        <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${rightMax !== undefined}"
                                                      @or-mwc-input-changed="${(ev) => this.onMinMaxValueToggle('right', 'max', ev)}"
                                        ></or-mwc-input>
                                    </div>
                                    <div style="display: flex; margin-top: 12px;">
                                        ${rightMin !== undefined ? html `
                                            <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" .value="${rightMin}" style="width: 100%;"
                                                          @or-mwc-input-changed="${(ev) => this.onMinMaxValueChange('right', 'min', ev)}"
                                            ></or-mwc-input>
                                        ` : html `
                                            <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" disabled="true" value="auto"
                                                          style="width: 100%;"></or-mwc-input>
                                        `}
                                        <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${rightMin !== undefined}"
                                                      @or-mwc-input-changed="${(ev) => this.onMinMaxValueToggle('right', 'min', ev)}"
                                        ></or-mwc-input>
                                    </div>
                                </div>
                            `;
        })}
                    </div>
                </settings-panel>

                <!-- Data sampling options -->
                <settings-panel displayName="dataSampling" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 12px;">
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" style="width: 100%" .options="${Array.from(this.samplingOptions.keys())}" .value="${samplingValue}"
                                          label="${i18next.t('algorithm')}" @or-mwc-input-changed="${(ev) => this.onSamplingQueryChange(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            ${this.getSamplingOptionsTemplate(this.widgetConfig.datapointQuery.type)}
                        </div>
                    </div>
                </settings-panel>
            </div>
        `;
    }
    getSamplingOptionsTemplate(type) {
        switch (type) {
            case 'interval': {
                const intervalQuery = this.widgetConfig.datapointQuery;
                const formulaOptions = ["AVG" /* AssetDatapointIntervalQueryFormula.AVG */, "MIN" /* AssetDatapointIntervalQueryFormula.MIN */, "MAX" /* AssetDatapointIntervalQueryFormula.MAX */];
                return html `
                    <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${formulaOptions}"
                                  .value="${intervalQuery.formula}" label="${i18next.t('algorithmMethod')}" @or-mwc-input-changed="${(event) => {
                    intervalQuery.formula = event.detail.value;
                    this.notifyConfigUpdate();
                }}"
                    ></or-mwc-input>
                `;
            }
            default:
                return html ``;
        }
    }
    // When a user clicks on ANY action in the attribute list, we want to switch between LEFT and RIGHT axis.
    // Since that is the only action, there is no need to check the ev.action variable.
    onAttributeAction(ev) {
        if (this.widgetConfig.attributeRefs.indexOf(ev.detail.attributeRef) >= 0) {
            if (this.widgetConfig.rightAxisAttributes.includes(ev.detail.attributeRef)) {
                this.removeFromRightAxis(ev.detail.attributeRef);
            }
            else {
                this.addToRightAxis(ev.detail.attributeRef);
            }
            this.notifyConfigUpdate();
        }
    }
    // When the list of attributeRefs is changed by the asset selector,
    // we should remove the "right axis" references for the attributes that got removed.
    // Also update the WidgetConfig attributeRefs field as usual
    onAttributesSelect(ev) {
        const removedAttributeRefs = this.widgetConfig.attributeRefs.filter(ar => !ev.detail.attributeRefs.includes(ar));
        removedAttributeRefs.forEach(raf => this.removeFromRightAxis(raf));
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }
    addToRightAxis(attributeRef, notify = false) {
        if (!this.widgetConfig.rightAxisAttributes.includes(attributeRef)) {
            this.widgetConfig.rightAxisAttributes.push(attributeRef);
            if (notify) {
                this.notifyConfigUpdate();
            }
        }
    }
    removeFromRightAxis(attributeRef, notify = false) {
        if (this.widgetConfig.rightAxisAttributes.includes(attributeRef)) {
            this.widgetConfig.rightAxisAttributes.splice(this.widgetConfig.rightAxisAttributes.indexOf(attributeRef), 1);
            if (notify) {
                this.notifyConfigUpdate();
            }
        }
    }
    onTimePresetSelect(ev) {
        this.widgetConfig.defaultTimePresetKey = ev.detail.value.toString();
        this.notifyConfigUpdate();
    }
    onTimestampControlsToggle(ev) {
        this.widgetConfig.showTimestampControls = !ev.detail.value;
        this.notifyConfigUpdate();
    }
    onShowLegendToggle(ev) {
        this.widgetConfig.showLegend = ev.detail.value;
        this.notifyConfigUpdate();
    }
    setAxisMinMaxValue(axis, type, value) {
        if (axis === 'left') {
            if (type === 'min') {
                this.widgetConfig.chartOptions.options.scales.y.min = value;
            }
            else {
                this.widgetConfig.chartOptions.options.scales.y.max = value;
            }
        }
        else {
            if (type === 'min') {
                this.widgetConfig.chartOptions.options.scales.y1.min = value;
            }
            else {
                this.widgetConfig.chartOptions.options.scales.y1.max = value;
            }
        }
        this.notifyConfigUpdate();
    }
    onMinMaxValueChange(axis, type, ev) {
        this.setAxisMinMaxValue(axis, type, ev.detail.value);
    }
    onMinMaxValueToggle(axis, type, ev) {
        this.setAxisMinMaxValue(axis, type, (ev.detail.value ? (type === 'min' ? 0 : 100) : undefined));
    }
    onSamplingQueryChange(ev) {
        this.widgetConfig.datapointQuery.type = this.samplingOptions.get(ev.detail.value);
        this.notifyConfigUpdate();
    }
};
ChartSettings = __decorate([
    customElement("chart-settings")
], ChartSettings);
export { ChartSettings };
//# sourceMappingURL=chart-settings.js.map