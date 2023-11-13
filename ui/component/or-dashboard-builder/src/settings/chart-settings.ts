import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import "../panels/attributes-panel";
import "../util/settings-panel";
import {i18next} from "@openremote/or-translate";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {AssetDatapointIntervalQuery, AssetDatapointIntervalQueryFormula, Attribute, AttributeRef} from "@openremote/model";
import {ChartWidgetConfig} from "../widgets/chart-widget";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {TimePresetCallback} from "@openremote/or-chart";

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`


@customElement("chart-settings")
export class ChartSettings extends WidgetSettings {

    protected readonly widgetConfig!: ChartWidgetConfig

    protected timePresetOptions: Map<string, TimePresetCallback> = new Map<string, TimePresetCallback>();
    protected samplingOptions: Map<string, string> = new Map<string, string>();

    public setTimePresetOptions(options: Map<string, TimePresetCallback>) {
        this.timePresetOptions = options;
    }

    public setSamplingOptions(options: Map<string, string>) {
        this.samplingOptions = options;
    }

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        const attributeFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
            return ["boolean", "positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction"].includes(attr.type!)
        };
        const min = this.widgetConfig.chartOptions.options?.scales?.y?.min;
        const max = this.widgetConfig.chartOptions.options?.scales?.y?.max;
        const samplingValue = Array.from(this.samplingOptions.entries()).find((entry => entry[1] === this.widgetConfig.datapointQuery.type))![0]
        return html`
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="${i18next.t('attributes')}" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" onlyDataAttrs="${true}" .attributeFilter="${attributeFilter}" multi="${true}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Display options -->
                <settings-panel displayName="${i18next.t('display')}" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 6px;">
                        <!-- Timeframe -->
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('timeframeDefault')}" style="width: 100%;"
                                          .options="${Array.from(this.timePresetOptions.keys())}" value="${this.widgetConfig.defaultTimePresetKey}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimePresetSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Y Min/max options -->
                        <div>
                            <div class="switch-container">
                                <span>${i18next.t('dashboard.allowTimerangeSelect')}</span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.showTimestampControls}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimestampControlsToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                            <div class="switch-container">
                                <span>${i18next.t('dashboard.showLegend')}</span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showLegend}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowLegendToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </settings-panel>

                <!-- Chart configuration -->
                <settings-panel displayName="${i18next.t('dashboard.chartConfig')}" expanded="${true}">
                    <div style="padding-bottom: 12px;">
                        <div style="display: flex;">
                            ${max !== undefined ? html`
                                <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" .value="${max}" style="width: 100%;"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('max', ev)}"
                                ></or-mwc-input>
                            ` : html`
                                <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                            `}
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${max !== undefined}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueToggle('max', ev)}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; margin-top: 12px;">
                            ${min !== undefined ? html`
                                <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" .value="${min}" style="width: 100%;"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('min', ev)}"
                                ></or-mwc-input>
                            ` : html`
                                <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                            `}
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${min !== undefined}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueToggle('min', ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- Data sampling options -->
                <settings-panel displayName="${i18next.t('dataSampling')}" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 12px;">
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" style="width: 100%" .options="${Array.from(this.samplingOptions.keys())}" .value="${samplingValue}"
                                          label="${i18next.t('algorithm')}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onSamplingQueryChange(ev)}"
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

    protected getSamplingOptionsTemplate(type: any): TemplateResult {
        switch (type) {
            case 'interval': {
                const intervalQuery = this.widgetConfig.datapointQuery as AssetDatapointIntervalQuery;
                const formulaOptions = [AssetDatapointIntervalQueryFormula.AVG, AssetDatapointIntervalQueryFormula.MIN, AssetDatapointIntervalQueryFormula.MAX];
                return html`
                    <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${formulaOptions}"
                                  .value="${intervalQuery.formula}" label="${i18next.t('algorithmMethod')}" @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                      intervalQuery.formula = event.detail.value;
                                      this.notifyConfigUpdate();
                                  }}"
                    ></or-mwc-input>
                `;
            }
            default:
                return html``;
        }
    }


    protected onAttributesSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.attributeRefs = ev.detail as AttributeRef[];
        this.notifyConfigUpdate();
    }

    protected onTimePresetSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.defaultTimePresetKey = ev.detail.value.toString();
        this.notifyConfigUpdate();
    }

    protected onTimestampControlsToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showTimestampControls = !ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onShowLegendToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showLegend = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onMinMaxValueChange(type: 'min' | 'max', ev: OrInputChangedEvent) {
        switch (type) {
            case "max":
                this.widgetConfig.chartOptions.options.scales.y.max = ev.detail.value; break;
            case "min":
                this.widgetConfig.chartOptions.options.scales.y.min = ev.detail.value; break;
        }
        this.notifyConfigUpdate();
    }

    protected onMinMaxValueToggle(type: 'min' | 'max', ev: OrInputChangedEvent) {
        switch (type) {
            case "max":
                this.widgetConfig.chartOptions.options.scales.y.max = (ev.detail.value ? 100 : undefined); break;
            case "min":
                this.widgetConfig.chartOptions.options.scales.y.min = (ev.detail.value ? 0 : undefined); break;
        }
        this.notifyConfigUpdate();
    }

    protected onSamplingQueryChange(ev: OrInputChangedEvent) {
        this.widgetConfig.datapointQuery.type = this.samplingOptions.get(ev.detail.value)! as any;
        this.notifyConfigUpdate();
    }
}
