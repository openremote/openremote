import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import "../panels/attributes-panel";
import "../util/settings-panel";
import {i18next} from "@openremote/or-translate";
import {AttributeAction, AttributeActionEvent, AttributesSelectEvent} from "../panels/attributes-panel";
import {Asset, AssetDatapointIntervalQuery, AssetDatapointIntervalQueryFormula, Attribute, AttributeRef} from "@openremote/model";
import {ChartWidgetConfig} from "../widgets/chart-widget";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {when} from "lit/directives/when.js";
import moment from "moment/moment";

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`


@customElement("chart-settings")
export class ChartSettings extends WidgetSettings {

    protected readonly widgetConfig!: ChartWidgetConfig;



    protected timeWindowOptions: Map<string, [moment.unitOfTime.DurationConstructor, number]> = new Map<string, [moment.unitOfTime.DurationConstructor, number]>;
    protected timePrefixOptions: string[] = [];
    protected samplingOptions: Map<string, string> = new Map<string, string>();

    public setTimeWindowOptions(options: Map<string, [moment.unitOfTime.DurationConstructor, number]>) {
        this.timeWindowOptions = options;
    }

    public setTimePrefixOptions(options: string[]) {
        this.timePrefixOptions = options;
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
        const attrSettings = this.widgetConfig.attributeSettings;
        const min = this.widgetConfig.chartOptions.options?.scales?.y?.min;
        const max = this.widgetConfig.chartOptions.options?.scales?.y?.max;
        const isMultiAxis = attrSettings.rightAxisAttributes.length > 0;
        const samplingValue = Array.from(this.samplingOptions.entries()).find((entry => entry[1] === this.widgetConfig.datapointQuery.type))![0]
        const attributeLabelCallback = (asset: Asset, attribute: Attribute<any>, attributeLabel: string) => {
            const isOnRightAxis = isMultiAxis && attrSettings.rightAxisAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isFaint = attrSettings.faintAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isSmooth = attrSettings.smoothAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isStepped = attrSettings.steppedAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isArea = attrSettings.areaAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isExtended = attrSettings.extendedAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            return html`
                <span>${asset.name}</span>
                <span style="font-size:14px; color:grey;">${attributeLabel}</span>
                ${when(isOnRightAxis, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="right"></or-translate></span>   
                `)}
                ${when(isFaint, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="dashboard.faint"></or-translate></span>
                `)}
                ${when(isSmooth, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="dashboard.smooth"></or-translate></span>
                `)}
                ${when(isStepped, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="dashboard.stepped"></or-translate></span>
                `)}
                ${when(isArea, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="dashboard.fill"></or-translate></span>
                `)}
                ${when(isExtended, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="dashboard.extendData"></or-translate></span>
                `)}
            `
        }


        const attributeActionCallback = (attributeRef: AttributeRef): AttributeAction[] => {
            return [
                {
                  icon: 'palette',
                  tooltip: i18next.t('dashboard.lineColor'),
                  disabled: false
                },
                {
                    icon: 'chart-bell-curve-cumulative',
                    tooltip: i18next.t("dashboard.smooth"),
                    disabled: false
                },
                {
                    icon: 'square-wave',
                    tooltip: i18next.t('dashboard.stepped'),
                    disabled: false
                },
                {
                    icon: 'chart-areaspline-variant',
                    tooltip: i18next.t('dashboard.fill'),
                    disabled: false
                },
                {
                    icon: 'arrange-send-backward',
                    tooltip: i18next.t('dashboard.faint'),
                    disabled: false
                },
                {
                    icon: 'arrow-expand-right',
                    tooltip: i18next.t('dashboard.extendData'),
                    disabled: false
                },

                {
                    icon: this.widgetConfig.attributeSettings.rightAxisAttributes.includes(attributeRef) ? "arrow-right-bold" : "arrow-left-bold",
                    tooltip: i18next.t('dashboard.toggleAxis'),
                    disabled: false
                },
                {
                    icon: 'mdi-blank',
                    tooltip: '',
                    disabled: true
                }
            ]
        }
        return html`
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" multi="${true}" onlyDataAttrs="${true}" .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      .attributeLabelCallback="${attributeLabelCallback}" .attributeActionCallback="${attributeActionCallback}"
                                      @attribute-action="${(ev: AttributeActionEvent) => this.onAttributeAction(ev)}"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Time options -->
                <settings-panel displayName="time" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 6px;">
                        <!-- Timeframe -->
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('prefixDefault')}" style="width: 100%;"
                                          .options="${this.timePrefixOptions}" value="${this.widgetConfig.defaultTimePrefixKey}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimePreFixSelect(ev)}"
                            ></or-mwc-input>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('timeframeDefault')}" style="width: 100%;"
                                          .options="${Array.from(this.timeWindowOptions.keys())}" value="${this.widgetConfig.defaultTimeWindowKey}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeWindowSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Time range selection -->
                        <div>
                            <div class="switch-container">
                                <span><or-translate value="dashboard.allowTimerangeSelect"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.showTimestampControls}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimestampControlsToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        </div> 
                    </div>  
               </settings-panel>
               <!-- Display options --> 
               <settings-panel displayName="display" expanded="${false}">
                   <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 16px;">
                            <div class="switch-container">
                                <span><or-translate value="dashboard.showLegend"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showLegend}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowLegendToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        <!-- Datazoombar -->
                            <div class="switch-container">
                                <span><or-translate value="dashboard.showZoomBar"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showZoomBar}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowZoomBarToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        <!-- Toolbox -->
                            <div class="switch-container">
                                <span><or-translate value="dashboard.showToolBox"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showToolBox}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowToolBoxToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        <!-- Show Symbol Treshold -->    
                            <div class="number-container">
                                <or-mwc-input .type="${InputType.NUMBER}" .min="1" .max="200" .step="1" label="${i18next.t('dashboard.showSymbolMaxDatapoints')}" style="width: 100%;" .value="${this.widgetConfig.showSymbolMaxDatapoints}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowSymbolMaxDatapointsValueChange(ev)}"
                                ></or-mwc-input>
                            </div>
                    </div>
                </settings-panel>

                <!-- Axis configuration -->
                <settings-panel displayName="dashboard.axisConfig" expanded="${false}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 16px;">
                        <!-- Left axis configuration -->
                        <div>
                            ${when(isMultiAxis, () => html`
                                <div style="margin-bottom: 8px;">
                                    <span><or-translate value="dashboard.leftAxis"></or-translate></span>
                                </div>
                            `)}
                            <div style="display: flex;">
                                ${max !== undefined ? html`
                                    <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" .value="${max}" style="width: 100%;"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('left', 'max', ev)}"
                                    ></or-mwc-input>
                                ` : html`
                                    <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                                `}
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${max !== undefined}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueToggle('left', 'max', ev)}"
                                ></or-mwc-input>
                            </div>
                            <div style="display: flex; margin-top: 12px;">
                                ${min !== undefined ? html`
                                    <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" .value="${min}" style="width: 100%;"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('left', 'min', ev)}"
                                    ></or-mwc-input>
                                ` : html`
                                    <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" disabled="true" value="auto" style="width: 100%;"></or-mwc-input>
                                `}
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${min !== undefined}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueToggle('left', 'min', ev)}"
                                ></or-mwc-input>
                            </div>
                        </div>

                        <!-- Right axis configuration -->
                        ${when(isMultiAxis, () => {
                            const rightMin = this.widgetConfig.chartOptions.options?.scales?.y1?.min;
                            const rightMax = this.widgetConfig.chartOptions.options?.scales?.y1?.max;
                            return html`
                                <div>
                                    <div style="margin-bottom: 8px;">
                                        <span><or-translate value="dashboard.rightAxis"></or-translate></span>
                                    </div>
                                    <div style="display: flex;">
                                        ${rightMax !== undefined ? html`
                                            <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" .value="${rightMax}" style="width: 100%;"
                                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('right', 'max', ev)}"
                                            ></or-mwc-input>
                                        ` : html`
                                            <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('max')}" disabled="true" value="auto"
                                                          style="width: 100%;"></or-mwc-input>
                                        `}
                                        <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${rightMax !== undefined}"
                                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueToggle('right', 'max', ev)}"
                                        ></or-mwc-input>
                                    </div>
                                    <div style="display: flex; margin-top: 12px;">
                                        ${rightMin !== undefined ? html`
                                            <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" .value="${rightMin}" style="width: 100%;"
                                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('right', 'min', ev)}"
                                            ></or-mwc-input>
                                        ` : html`
                                            <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('yAxis') + ' ' + i18next.t('min')}" disabled="true" value="auto"
                                                          style="width: 100%;"></or-mwc-input>
                                        `}
                                        <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px 0 0;" .value="${rightMin !== undefined}"
                                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueToggle('right', 'min', ev)}"
                                        ></or-mwc-input>
                                    </div>
                                </div>
                            `
                        })}
                    </div>
                </settings-panel>

                <!-- Data sampling options -->
                <settings-panel displayName="dataSampling" expanded="${false}">
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
            case 'lttb': {
                return html `
                    <or-mwc-input .type="${InputType.NUMBER}" .min="10" .max="1000" .step="1" label="${i18next.t('dashboard.maxConcurrentDatapoints')}" .value="${this.widgetConfig.maxConcurrentDatapoints}" style="width: 100%;"
                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMaxConcurrentDatapointsValueChange(ev)}"
                    ></or-mwc-input>
                `;
            }
            default:
                return html``;
        }
    }

    // Check which icon was pressed and act accordingly.
    protected onAttributeAction(ev: AttributeActionEvent) {
        const { asset ,attributeRef, action } = ev.detail;

        const findAttributeIndex = (array: AttributeRef[], ref: AttributeRef) => {
            return array.findIndex(item => item.id === ref.id && item.name === ref.name);
        };

        switch (action.icon) {
            case "palette":    // Change color
                const colorInput = document.createElement('input');
                colorInput.type = 'color';
                colorInput.style.border = 'none';
                colorInput.style.height = '31px';
                colorInput.style.width = '31px';
                colorInput.style.padding = '1px 3px';
                colorInput.style.minHeight = '22px';
                colorInput.style.minWidth = '30px';
                colorInput.style.cursor = 'pointer';
                colorInput.addEventListener('change', (e: any) => {
                    const color = e.target.value;
                    const existingIndex = this.widgetConfig.colorPickedAttributes.findIndex(item =>
                        item.attributeRef.id === attributeRef.id && item.attributeRef.name === attributeRef.name
                    );
                    if (existingIndex >= 0) {
                        this.widgetConfig.colorPickedAttributes[existingIndex].color = color;
                    } else {
                        this.widgetConfig.colorPickedAttributes.push({ attributeRef, color });
                    }
                    this.notifyConfigUpdate();
                });
                colorInput.click();
                break;
            case "arrow-right-bold":
            case "arrow-left-bold":
                this.toggleAttributeSetting("rightAxisAttributes", attributeRef);
                break;
            case "chart-bell-curve-cumulative":
                this.toggleAttributeSetting("smoothAttributes", attributeRef);
                break;
            case "square-wave":
                this.toggleAttributeSetting("steppedAttributes", attributeRef);
                break;
            case "chart-areaspline-variant":
                this.toggleAttributeSetting("areaAttributes", attributeRef);
                break;
            case "arrange-send-backward":
                this.toggleAttributeSetting("faintAttributes", attributeRef);
                break;
            case "arrow-expand-right":
                this.toggleAttributeSetting("extendedAttributes", attributeRef);
                break;
            default:
                console.warn('Unknown attribute panel action:', action);
        }
        console.log("end of onAttributeAction" + JSON.stringify(this.widgetConfig.attributeSettings));
    }

    // When the list of attributeRefs is changed by the asset selector,
    // we should remove the settings references for the attributes that got removed.
    // Also update the WidgetConfig attributeRefs field as usual
    protected onAttributesSelect(ev: AttributesSelectEvent) {
        const removedAttributeRefs = this.widgetConfig.attributeRefs.filter(ar => !ev.detail.attributeRefs.includes(ar));

        removedAttributeRefs.forEach(raf => {
            this.removeFromAttributeSettings(raf);
            this.removeFromColorPickedAttributes(raf);
        });

        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }

    protected removeFromAttributeSettings(attributeRef: AttributeRef) {
        const settings = this.widgetConfig.attributeSettings;
        (Object.keys(settings) as (keyof typeof settings)[]).forEach(key => {
            settings[key] = settings[key].filter((ar: AttributeRef) => ar.id !== attributeRef.id || ar.name !== attributeRef.name);
        });
    }

    protected toggleAttributeSetting(
        setting: keyof ChartWidgetConfig["attributeSettings"],
        attributeRef: AttributeRef,
    ): void {
        const attributes = this.widgetConfig.attributeSettings[setting];
        const index = attributes.findIndex(
            (item: AttributeRef) => item.id === attributeRef.id && item.name === attributeRef.name
        );
        if (index < 0) {
            attributes.push(attributeRef);
        } else {
            attributes.splice(index, 1);
        }
        this.notifyConfigUpdate();
    }

    protected removeFromColorPickedAttributes(attributeRef: AttributeRef) {
        this.widgetConfig.colorPickedAttributes = this.widgetConfig.colorPickedAttributes.filter(
            item => item.attributeRef.id !== attributeRef.id || item.attributeRef.name !== attributeRef.name
        );
    }

    protected onTimePreFixSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.defaultTimePrefixKey = ev.detail.value.toString();
        this.notifyConfigUpdate();
    }

    protected onTimeWindowSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.defaultTimeWindowKey = ev.detail.value.toString();
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

    protected onShowZoomBarToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showZoomBar = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onShowToolBoxToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showToolBox = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected setAxisMinMaxValue(axis: 'left' | 'right', type: 'min' | 'max', value?: number) {
        if(axis === 'left') {
            if(type === 'min') {
                this.widgetConfig.chartOptions.options.scales.y.min = value;
            } else {
                this.widgetConfig.chartOptions.options.scales.y.max = value;
            }
        } else {
            if(type === 'min') {
                this.widgetConfig.chartOptions.options.scales.y1.min = value;
            } else {
                this.widgetConfig.chartOptions.options.scales.y1.max = value;
            }
        }
        this.notifyConfigUpdate();
    }

    protected onMinMaxValueChange(axis: 'left' | 'right', type: 'min' | 'max', ev: OrInputChangedEvent) {
        this.setAxisMinMaxValue(axis, type, ev.detail.value);
    }

    protected onMinMaxValueToggle(axis: 'left' | 'right', type: 'min' | 'max', ev: OrInputChangedEvent) {
        this.setAxisMinMaxValue(axis, type, (ev.detail.value ? (type === 'min' ? 0 : 100) : undefined));
    }

    protected onSamplingQueryChange(ev: OrInputChangedEvent) {
        this.widgetConfig.datapointQuery.type = this.samplingOptions.get(ev.detail.value)! as any;
        this.notifyConfigUpdate();
    }

    protected onMaxConcurrentDatapointsValueChange(ev: OrInputChangedEvent) {
        this.widgetConfig.maxConcurrentDatapoints = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onShowSymbolMaxDatapointsValueChange(ev: OrInputChangedEvent) {
        this.widgetConfig.showSymbolMaxDatapoints = ev.detail.value;
        this.notifyConfigUpdate();
    }
}
