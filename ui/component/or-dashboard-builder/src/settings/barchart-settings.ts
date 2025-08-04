import {css, html, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import "../panels/attributes-chart-panel";
import "../util/settings-panel";
import {i18next} from "@openremote/or-translate";
import {AttributeAction, AttributeActionEvent, AttributesSelectEvent} from "../panels/attributes-panel";
import {Asset, AssetDescriptor, Attribute, AttributeRef} from "@openremote/model";
import {BarChartWidgetConfig} from "../widgets/barchart-widget";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {when} from "lit/directives/when.js";
import moment from "moment/moment";
import {ListItem, ListType, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {showDialog, OrMwcDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {IntervalConfig, OrAttributeBarChart} from "@openremote/or-attribute-barchart";
import {OrChart} from "@openremote/or-chart";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";

const styling = css`
    .switch-container {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
`;


@customElement("barchart-settings")
export class BarChartSettings extends WidgetSettings {

    protected readonly widgetConfig!: BarChartWidgetConfig;

    protected timeWindowOptions: Map<string, [moment.unitOfTime.DurationConstructor, number]> = new Map<string, [moment.unitOfTime.DurationConstructor, number]>;
    protected timePrefixOptions: string[] = [];
    protected intervalOptions: Map<string, IntervalConfig> = new Map<string, IntervalConfig>();

    public setTimeWindowOptions(options: Map<string, [moment.unitOfTime.DurationConstructor, number]>) {
        this.timeWindowOptions = options;
    }

    public setTimePrefixOptions(options: string[]) {
        this.timePrefixOptions = options;
    }

    public setIntervalOptions(options: Map<string, IntervalConfig>) {
        this.intervalOptions = options;
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
        const isMultiAxis = !!attrSettings.rightAxisAttributes?.length;

        const attributeIconCallback = (asset: Asset, attribute: Attribute<any>, descriptor?: AssetDescriptor) => {
            let color = this.widgetConfig.attributeColors?.find(a => a[0].id === asset.id && a[0].name === attribute.name)?.[1]?.replace('#', '');
            if(!color) {
                const index = this.widgetConfig.attributeRefs?.findIndex(ref => ref.id === asset.id && ref.name === attribute.name);
                if(index >= 0) color = OrChart.DEFAULT_COLORS?.[index]?.replace('#', '');
            }
            return html`<span>${getAssetDescriptorIconTemplate(descriptor, undefined, undefined, color)}</span>`;
        };

        const attributeActionCallback = (attributeRef: AttributeRef): AttributeAction[] => {
            const faint = !!this.widgetConfig?.attributeSettings?.faintAttributes?.find(ref => ref.id === attributeRef.id && ref.name === attributeRef.name);
            let color = this.widgetConfig?.attributeColors?.find(([ref, _]) => ref.id === attributeRef.id && ref.name === attributeRef.name)?.[1];
            let customColor = false;
            if (!color) {
                const index = this.widgetConfig?.attributeRefs?.findIndex(ref => ref.id === attributeRef.id && ref.name === attributeRef.name);
                if (index >= 0) color = OrAttributeBarChart.DEFAULT_COLORS[index];
            } else {
                customColor = true;
            }
            return [
                {
                    icon: "arrow-up-circle",
                    tooltip: "Order up",
                    active: false,
                    disabled: this.widgetConfig.attributeRefs.indexOf(attributeRef) === 0
                },
                {
                    icon: "arrow-down-circle",
                    tooltip: "Order down",
                    active: false,
                    disabled: this.widgetConfig.attributeRefs.indexOf(attributeRef) === (this.widgetConfig.attributeRefs.length - 1)
                },
                {
                    icon: "calculator-variant-outline",
                    tooltip: i18next.t("algorithmMethod"),
                    active: true,
                    color: color,
                    disabled: false
                },
                {
                    icon: "palette",
                    tooltip: i18next.t("dashboard.lineColor"),
                    active: customColor,
                    color: color,
                    disabled: false
                },
                {
                    icon: "arrange-send-backward",
                    tooltip: i18next.t("dashboard.faint"),
                    active: faint,
                    color: color,
                    disabled: false
                },
                {
                    icon: this.widgetConfig.attributeSettings.rightAxisAttributes?.includes(attributeRef) ? "arrow-right-bold" : "arrow-left-bold",
                    tooltip: i18next.t("dashboard.toggleAxis"),
                    active: false,
                    color: color,
                    disabled: false
                }
            ];
        };
        return html`
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-chart-panel .attributeRefs="${this.widgetConfig.attributeRefs}" multi onlyDataAttrs .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      .attributeIconCallback=${attributeIconCallback} .attributeActionCallback="${attributeActionCallback}"
                                      @attribute-action="${(ev: AttributeActionEvent) => this.onAttributeAction(ev)}"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-chart-panel>
                </settings-panel>

                <!-- Time options -->
                <settings-panel displayName="time" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 6px;">
                        
                        <!-- Timeframe & interval -->
                        <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('prefixDefault')}" style="width: 100%;"
                                      .options="${this.timePrefixOptions}" value="${this.widgetConfig.defaultTimePrefixKey}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimePrefixSelect(ev)}"
                        ></or-mwc-input>
                        <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('timeframeDefault')}" style="width: 100%;"
                                      .options="${Array.from(this.timeWindowOptions.keys())}" value="${this.widgetConfig.defaultTimeWindowKey}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeWindowSelect(ev)}"
                        ></or-mwc-input>
                        <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('dashboard.withInterval')}" style="width: 100%;"
                                      .options="${Array.from(this.intervalOptions.keys())}" value="${this.widgetConfig.defaultInterval}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onIntervalSelect(ev)}"
                        ></or-mwc-input>
                    </div>
                </settings-panel>
                
                <!-- Display options -->
                <settings-panel displayName="display" expanded="${false}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 16px;">
                        <!-- Stacked -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.defaultStacked"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.stacked}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onDefaultStackedToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Time range selection -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.allowTimerangeSelect"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.showTimestampControls}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimestampControlsToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Show legend -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.showLegend"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showLegend}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowLegendToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Decimal places -->
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;" .value="${this.widgetConfig.decimals}" label="${i18next.t('decimals')}" .min="${0}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onDecimalsChange(ev)}"
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
            </div>
        `;
    }

    // Check which icon was pressed and act accordingly.
    protected onAttributeAction(ev: AttributeActionEvent) {
        const {asset, attributeRef, action} = ev.detail;

        const findAttributeIndex = (array: AttributeRef[], ref: AttributeRef) => {
            return array.findIndex(item => item.id === ref.id && item.name === ref.name);
        };

        switch (action.icon) {
            case "palette":    // Change color
                this.openColorPickDialog(attributeRef);
                break;
            case "arrow-right-bold":
            case "arrow-left-bold":
                this.toggleAttributeSetting("rightAxisAttributes", attributeRef);
                break;
            case "calculator-variant-outline":
                this.openAlgorithmMethodsDialog(attributeRef);
                break;
            case "arrange-send-backward":
                this.onFaintClick(attributeRef);
                break;
            case "arrow-up-circle":
                this.onArrowUpClick(attributeRef);
                break;
            case "arrow-down-circle":
                this.onArrowDownClick(attributeRef);
                break;
            default:
                console.warn('Unknown attribute panel action:', action);
        }
    }

    protected onFaintClick(attrRef: AttributeRef) {
        this.widgetConfig.attributeSettings.faintAttributes ??= [];
        const index = this.widgetConfig.attributeSettings.faintAttributes.findIndex(attr => attr.id === attrRef.id && attr.name === attrRef.name);
        if (index >= 0) {
            this.widgetConfig.attributeSettings.faintAttributes = this.widgetConfig.attributeSettings.faintAttributes.filter(attr => attr.id !== attrRef.id && attr.name !== attrRef.name);
        } else {
            this.widgetConfig.attributeSettings.faintAttributes.push(attrRef);
        }
        this.notifyConfigUpdate();
    }

    protected onArrowUpClick(attrRef: AttributeRef) {
        const index = this.widgetConfig.attributeRefs.indexOf(attrRef);
        if (index > 0) {
            const temp = this.widgetConfig.attributeRefs[index - 1];
            this.widgetConfig.attributeRefs[index - 1] = attrRef;
            this.widgetConfig.attributeRefs[index] = temp;
        }
        this.notifyConfigUpdate();
    }

    protected onArrowDownClick(attrRef: AttributeRef) {
        const index = this.widgetConfig.attributeRefs.indexOf(attrRef);
        if (index < this.widgetConfig.attributeRefs.length - 1) {
            const temp = this.widgetConfig.attributeRefs[index + 1];
            this.widgetConfig.attributeRefs[index + 1] = attrRef;
            this.widgetConfig.attributeRefs[index] = temp;
        }
        this.notifyConfigUpdate();
    }

    // When the list of attributeRefs is changed by the asset selector,
    // we should remove the settings references for the attributes that got removed.
    // Also update the WidgetConfig attributeRefs field as usual
    protected onAttributesSelect(ev: AttributesSelectEvent) {
        const removedAttributeRefs = this.widgetConfig.attributeRefs.filter(ar => !ev.detail.attributeRefs.find(newRefs => newRefs.id === ar.id && newRefs.name === ar.name));
        removedAttributeRefs.forEach(ref => {
            this.removeFromAttributeSettings(ref);
            this.removeFromAttributeColors(ref);
        });
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;

        // Loop through attribute methods (MIN, AVG, MAX etc), and place attributes in 'AVG' if not in any yet.
        const settings = this.widgetConfig.attributeSettings;
        const methodRefsList = [...settings.methodAvgAttributes ?? [], ...settings.methodMinAttributes ?? [], ...settings.methodMaxAttributes ?? [],
            ...settings.methodDeltaAttributes ?? [], ...settings.methodMedianAttributes ?? [], ...settings.methodModeAttributes ?? [],
            ...settings.methodSumAttributes ?? [], ...settings.methodCountAttributes ?? []
        ];
        this.widgetConfig.attributeRefs.forEach(ref => {
            if(!methodRefsList.find(methodRef => methodRef.id === ref.id && methodRef.name === ref.name)) {
                this.widgetConfig.attributeSettings.methodAvgAttributes?.push(ref);
            }
        });
        this.notifyConfigUpdate();
    }

    protected removeFromAttributeSettings(attributeRef: AttributeRef) {
        const settings = this.widgetConfig.attributeSettings;
        (Object.keys(settings) as (keyof typeof settings)[]).forEach(key => {
            settings[key] = settings[key]!.filter((ar: AttributeRef) => (ar.id !== attributeRef.id && ar.name !== attributeRef.name));
        });
    }

    protected toggleAttributeSetting(
        setting: keyof BarChartWidgetConfig["attributeSettings"],
        attributeRef: AttributeRef
    ): void {
        const attributes = this.widgetConfig.attributeSettings[setting] ?? [];
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

    protected openColorPickDialog(attributeRef: AttributeRef) {
        const colorInput = document.createElement("input");
        colorInput.type = "color";
        colorInput.style.border = "none";
        colorInput.style.height = "31px";
        colorInput.style.width = "31px";
        colorInput.style.padding = "1px 3px";
        colorInput.style.minHeight = "22px";
        colorInput.style.minWidth = "30px";
        colorInput.style.cursor = "pointer";
        colorInput.addEventListener("change", (e: any) => {
            const color = e.target.value;
            const existingIndex = this.widgetConfig.attributeColors.findIndex(([ref, _]) => ref.id === attributeRef.id && ref.name === attributeRef.name);
            if (existingIndex >= 0) {
                this.widgetConfig.attributeColors[existingIndex][1] = color;
            } else {
                this.widgetConfig.attributeColors.push([attributeRef, color]);
            }
            this.notifyConfigUpdate();
        });
        colorInput.click();
    }


    protected removeFromAttributeColors(attributeRef: AttributeRef) {
        this.widgetConfig.attributeColors = this.widgetConfig.attributeColors.filter(
            ([ref, _]) => ref.id !== attributeRef.id || ref.name !== attributeRef.name
        );
    }


    protected openAlgorithmMethodsDialog(attributeRef: AttributeRef) {
        const methodList: ListItem[] = Object.entries(this.widgetConfig.attributeSettings)
            .filter(([key]) => key.includes("method"))
            .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
            .map(([key, attributeRefs]) => {
                const isActive = attributeRefs.some(
                    (ref: AttributeRef) =>
                        ref.id === attributeRef.id && ref.name === attributeRef.name
                );

                return {
                    text: key,
                    value: key,
                    data: isActive ? key : undefined,
                    translate: true
                };
            });
        let selected: ListItem[] = [];

        showDialog(new OrMwcDialog()
            .setContent(html`
                <div id="method-creator">
                    <or-mwc-list id="method-creator-list" .type="${ListType.MULTI_CHECKBOX}"
                                 .listItems="${methodList}"
                                 .values="${methodList.map(item => item.data)}"
                                 @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => {
                                     selected = Array.from(ev.detail.values());
                                     selected = selected.map(item => ({...item, data: item.text}));
                                 }}"
                    ></or-mwc-list>
                </div>
            `)
            .setHeading(i18next.t("algorithmMethod"))
            .setActions([
                {
                    actionName: "cancel",
                    content: "cancel"
                },
                {
                    default: true,
                    actionName: "ok",
                    action: () => {
                        // Check which settings need updating
                        const changedMethods = methodList.filter(input => {
                            const selectedItem = selected.find(selected => selected.value === input.value);
                            return (!selectedItem && input.data !== undefined) ||
                                (selectedItem && selectedItem.data === undefined) ||
                                (selectedItem && selectedItem.data !== input.data);
                        });
                        //Update the settings
                        changedMethods.forEach((item: ListItem) => {
                            if (item.value) {
                                this.toggleAttributeSetting(
                                    item.value as keyof BarChartWidgetConfig["attributeSettings"],
                                    attributeRef
                                );
                            }
                        });
                    },
                    content: "ok"
                }
            ])
            .setDismissAction(null));
    }

    protected onTimePrefixSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.defaultTimePrefixKey = ev.detail.value.toString();
        this.notifyConfigUpdate();
    }

    protected onTimeWindowSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.defaultTimeWindowKey = ev.detail.value.toString();
        this.notifyConfigUpdate();
    }

    protected onIntervalSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.defaultInterval = ev.detail.value.toString();
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

    protected onDefaultStackedToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.stacked = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected setAxisMinMaxValue(axis: 'left' | 'right', type: 'min' | 'max', value?: number) {
        if (axis === 'left') {
            if (type === 'min') {
                this.widgetConfig.chartOptions.options.scales.y.min = value;
            } else {
                this.widgetConfig.chartOptions.options.scales.y.max = value;
            }
        } else {
            if (type === 'min') {
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

    protected onDecimalsChange(ev: OrInputChangedEvent) {
        this.widgetConfig.decimals = ev.detail.value;
        this.notifyConfigUpdate();
    }

}
