import {css, html, TemplateResult } from "lit";
import { customElement, query } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import "../panels/attributes-chart-panel";
import "../util/settings-panel";
import {i18next} from "@openremote/or-translate";
import {debounce} from "lodash";
import {AttributesChartPanel} from "../panels/attributes-chart-panel";
import {AttributeAction, AttributeActionEvent, AttributesSelectEvent} from "../panels/attributes-panel";
import {Asset, AssetDescriptor, Attribute, AttributeRef} from "@openremote/model";
import {ChartWidgetConfig} from "../widgets/chart-widget";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {when} from "lit/directives/when.js";
import {map} from "lit/directives/map.js";
import moment from "moment/moment";
import {ChartAttributeConfig, OrChart} from "@openremote/or-chart";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`


@customElement("chart-settings")
export class ChartSettings extends WidgetSettings {

    @query('attributes-chart-panel')
    protected _attributesPanelElem?: AttributesChartPanel;

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
        const attrConfig = this.widgetConfig.attributeConfig;
        const min = this.widgetConfig.chartOptions.options?.scales?.y?.min;
        const max = this.widgetConfig.chartOptions.options?.scales?.y?.max;
        const isMultiAxis = (attrConfig?.rightAxisAttributes?.length || 0) > 0;
        const samplingValue = Array.from(this.samplingOptions.entries()).find((entry => entry[1] === this.widgetConfig.datapointQuery.type))![0];

        const attributeIconCallback = (asset: Asset, attribute: Attribute<any>, descriptor?: AssetDescriptor) => {
            let color = this.widgetConfig.attributeColors?.find(a => a[0].id === asset.id && a[0].name === attribute.name)?.[1]?.replace('#', '');
            if(!color) {
                const index = this.widgetConfig.attributeRefs?.findIndex(ref => ref.id === asset.id && ref.name === attribute.name);
                if(index >= 0) color = OrChart.DEFAULT_COLORS?.[index]?.replace('#', '');
            }
            return html`<span>${getAssetDescriptorIconTemplate(descriptor, undefined, undefined, color)}</span>`;
        };
        const attributeLabelCallback = (asset: Asset, attribute: Attribute<any>, attributeLabel: string) => {
            const isOnRightAxis = isMultiAxis && attrConfig?.rightAxisAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isFaint = attrConfig?.faintAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isSmooth = attrConfig?.smoothAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isStepped = attrConfig?.steppedAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isArea = attrConfig?.areaAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const isExtended = attrConfig?.extendedAttributes?.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            const strings: string[] = [];
            if(isOnRightAxis) strings.push(i18next.t("right"));
            if(isFaint) strings.push(i18next.t("dashboard.faint"));
            if(isSmooth) strings.push(i18next.t("dashboard.smooth"));
            if(isStepped) strings.push(i18next.t("dashboard.stepped"));
            if(isArea) strings.push(i18next.t("dashboard.fill"));
            if(isExtended) strings.push(i18next.t("dashboard.extendData"));
            return html`
                <span>${asset.name}</span>
                <span style="font-size:14px; color:grey;">${attributeLabel}</span>
                ${when(strings.length > 0, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;">${strings.join(', ')}</span>
                `)}
            `;
        };
        const attributeActionCallback = (attributeRef: AttributeRef): AttributeAction[] => {
            const isOnRightAxis = this.widgetConfig?.attributeConfig?.rightAxisAttributes?.find(a => a.id === attributeRef.id && a.name === attributeRef.name) !== undefined;
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
                    icon: isOnRightAxis ? "arrow-right-bold" : "arrow-left-bold",
                    tooltip: i18next.t('dashboard.toggleAxis'),
                    disabled: false
                }
            ];
        };
        return html`
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-chart-panel .attributeRefs="${this.widgetConfig.attributeRefs}" multi="${true}" onlyDataAttrs="${true}" .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      .attributeIconCallback="${attributeIconCallback}" .attributeLabelCallback="${attributeLabelCallback}" .attributeActionCallback="${attributeActionCallback}"
                                      @attribute-action="${(ev: AttributeActionEvent) => this.onAttributeAction(ev)}"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-chart-panel>
                </settings-panel>

                <!-- Time options -->
                <settings-panel displayName="time" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 6px;">
                        <!-- This/last selection of timeframe -->
                        <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('prefixDefault')}" style="width: 100%;"
                                      .options="${this.timePrefixOptions}" value="${this.widgetConfig.defaultTimePrefixKey}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimePreFixSelect(ev)}"
                        ></or-mwc-input>
                        <!-- Select time frame -->
                        <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('timeframeDefault')}" style="width: 100%;"
                                      .options="${Array.from(this.timeWindowOptions.keys())}" value="${this.widgetConfig.defaultTimeWindowKey}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeWindowSelect(ev)}"
                        ></or-mwc-input>

                    </div>  
               </settings-panel>
               <!-- Display options --> 
               <settings-panel displayName="display" expanded="${false}">
                   <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 6px;">
                            <div class="switch-container">
                                <span><or-translate value="dashboard.allowTimerangeSelect"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.showTimestampControls}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimestampControlsToggle(ev)}"
                                ></or-mwc-input>
                            </div>
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
                            <!-- Show Symbol Treshold -->    
                            <div class="number-container">
                                <or-mwc-input .type="${InputType.NUMBER}" .min="1" .max="200" .step="1" label="dashboard.showSymbolMaxDatapoints" style="width: 100%;" .value="${this.widgetConfig.showSymbolMaxDatapoints}"
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
                            <or-mwc-input .type="${InputType.SELECT}" style="width: 100%" .options="${Array.from(this.samplingOptions.keys())}" .value="${samplingValue}" disabled=${true}
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
        const {attributeRef, action } = ev.detail;
        switch (action.icon) {
            case "palette":
                this.openColorPickDialog(attributeRef);
                break;
            case "arrow-right-bold":
            case "arrow-left-bold":
                this._toggleAttributeSetting("rightAxisAttributes", attributeRef);
                break;
            case "chart-bell-curve-cumulative":
                this._toggleAttributeSetting("smoothAttributes", attributeRef);
                break;
            case "square-wave":
                this._toggleAttributeSetting("steppedAttributes", attributeRef);
                break;
            case "chart-areaspline-variant":
                this._toggleAttributeSetting("areaAttributes", attributeRef);
                break;
            case "arrange-send-backward":
                this._toggleAttributeSetting("faintAttributes", attributeRef);
                break;
            case "arrow-expand-right":
                this._toggleAttributeSetting("extendedAttributes", attributeRef);
                break;
            default:
                console.warn("Unknown attribute panel action:", action);
        }
    }

    // When the list of attributeRefs is changed by the asset selector,
    // we should remove the settings references for the attributes that got removed.
    // Also update the WidgetConfig attributeRefs field as usual
    protected onAttributesSelect(ev: AttributesSelectEvent) {
        const removedAttributeRefs = this.widgetConfig.attributeRefs.filter(ar => !ev.detail.attributeRefs.includes(ar));

        removedAttributeRefs.forEach(raf => {
            this.removeFromAttributeConfig(raf);
            this.removeFromAttributeColors(raf);
        });

        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }

    protected removeFromAttributeConfig(attributeRef: AttributeRef) {
        const config = this.widgetConfig.attributeConfig;
        if(config) {
            (Object.keys(config) as (keyof typeof config)[]).forEach(key => {
                config[key] = config[key]?.filter((ar: AttributeRef) => ar.id !== attributeRef.id || ar.name !== attributeRef.name);
            });
        }
    }

    /**
     * Internal function that removes/adds an {@link AttributeRef} to a category of the {@link AttributeConfig}.
     * For example, you can add an attribute to the array of 'rightAxisAttributes'.
     * If the attribute is already present, it will be removed automatically.
     *
     * @param setting The key / category to add the attribute to. (for example, 'rightAxisAttributes')
     * @param attributeRef The asset-attribute combination to add/remove.
     * @protected
     */
    protected _toggleAttributeSetting(setting: keyof ChartAttributeConfig, attributeRef: AttributeRef) {
        this.widgetConfig.attributeConfig ??= {};
        this.widgetConfig.attributeConfig[setting] ??= [];
        const attributes = this.widgetConfig.attributeConfig[setting]!;
        const index = attributes.findIndex(
            (item: AttributeRef) => item.id === attributeRef.id && item.name === attributeRef.name
        );
        if (index < 0) {
            // Adding the attribute setting
            attributes.push(attributeRef);
        } else {
            // Removing the attribute setting
            if (attributes.length === 1) {
                delete this.widgetConfig.attributeConfig[setting];
            } else {
                attributes.splice(index, 1);
            }
        }
        this.notifyConfigUpdate();
    }

    protected removeFromAttributeColors(attributeRef: AttributeRef) {
        this.widgetConfig.attributeColors = this.widgetConfig.attributeColors.filter(x => x[0] !== attributeRef);
    }

    protected openColorPickDialog(attributeRef: AttributeRef) {
        const inputElem = this._attributesPanelElem?.shadowRoot?.querySelector(`#chart-color-${attributeRef.id}-${attributeRef.name}`) as HTMLInputElement | undefined;
        if(inputElem) {
            let oldColor = this.widgetConfig.attributeColors?.find(x => x[0] === attributeRef)?.[1];
            if(!oldColor) {
                const index = this.widgetConfig.attributeRefs.indexOf(attributeRef);
                if(index >= 0) {
                    oldColor = OrChart.DEFAULT_COLORS?.[index];
                }
            }
            // Update value
            inputElem.value = oldColor ?? "";

            // Listen for changes
            inputElem.addEventListener("input", debounce(() => {
                const color = inputElem.value;
                const existingColor = this.widgetConfig.attributeColors.find(x => x[0] === attributeRef);
                if(existingColor) {
                    existingColor[1] = color;
                } else {
                    this.widgetConfig.attributeColors.push([attributeRef, color]);
                }
                this.notifyConfigUpdate();
            }, 200));

            // Open color picker
            inputElem.click();
        }
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
