import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import "../panels/attributes-panel";
import "../util/settings-panel";
import {i18next} from "@openremote/or-translate";
import {AttributeAction, AttributeActionEvent, AttributesSelectEvent} from "../panels/attributes-panel";
import {Asset, AssetDatapointIntervalQuery, AssetDatapointIntervalQueryFormula, Attribute, AttributeRef} from "@openremote/model";
import {ReportWidgetConfig} from "../widgets/report-widget";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {when} from "lit/directives/when.js";
import moment from "moment/moment";
import {ListItem, ListType, OrMwcList, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {showDialog, OrMwcDialog, DialogAction} from "@openremote/or-mwc-components/or-mwc-dialog";

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`


@customElement("report-settings")
export class ReportSettings extends WidgetSettings {

    protected readonly widgetConfig!: ReportWidgetConfig;



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
        const attributeLabelCallback = (asset: Asset, attribute: Attribute<any>, attributeLabel: string) => {
            const isOnRightAxis = isMultiAxis && attrSettings.rightAxisAttributes.find(ar => ar.id === asset.id && ar.name === attribute.name) !== undefined;
            //Find which calculation methods are active
            const methodList = Object.entries(this.widgetConfig.attributeSettings)
                .filter(([key]) => key.includes('method'))
                .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
                .reduce((activeKeys, [key, attributeRefs]) => {
                    const isActive = attributeRefs.some(
                        (ref: AttributeRef) => ref.id === asset?.id && ref.name === attribute.name
                    );
                    if (isActive) {
                        activeKeys.push(i18next.t(key));
                    }
                    return activeKeys;
                }, [] as any[]);

            return html`
                <span>${asset.name}</span>
                <span style="font-size:14px; color:grey;">${attributeLabel}</span>
                ${when(isOnRightAxis, () => html`
                    <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value="right"></or-translate></span>   
                `)}
                <span style="font-size:14px; font-style:italic; color:grey;"><or-translate value=${methodList}></or-translate></span>
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
                    icon: 'calculator-variant-outline',
                    tooltip: i18next.t('algorithmMethod'),
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
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.chartSettings.showLegend}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowLegendToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                        <!-- Toolbox -->
                            <div class="switch-container">
                                <span><or-translate value="dashboard.showToolBox"></or-translate></span>
                                <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.chartSettings.showToolBox}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onShowToolBoxToggle(ev)}"
                                ></or-mwc-input>
                            </div>
                       <!-- Stacked -->
                       <div class="switch-container">
                           <span><or-translate value="dashboard.defaultStacked"></or-translate></span>
                           <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.chartSettings.defaultStacked}"
                                         @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onDefaultStackedToggle(ev)}"
                           ></or-mwc-input>
                       </div>
                       <!-- Chart or Table -->
                       <div class="switch-container">
                           <span><or-translate value="dashboard.isChart"></or-translate></span>
                           <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.isChart}"
                                         @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onIsChartToggle(ev)}"
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
                ${when(this.widgetConfig.isChart, () => html`
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
                        })        }
                         </div>
                     </settings-panel>
                `)}
            </div>
        `;
    }

    // Check which icon was pressed and act accordingly.
    protected onAttributeAction(ev: AttributeActionEvent) {
        const { asset ,attributeRef, action } = ev.detail;

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
            default:
                console.warn('Unknown attribute panel action:', action);
        }
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
            settings[key] = settings[key].filter((ar: AttributeRef) => (ar.id !== attributeRef.id && ar.name !== attributeRef.name));
        });
    }

    protected toggleAttributeSetting(
        setting: keyof ReportWidgetConfig["attributeSettings"],
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

    protected openColorPickDialog(attributeRef: AttributeRef) {
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
    }



    protected removeFromColorPickedAttributes(attributeRef: AttributeRef) {
        this.widgetConfig.colorPickedAttributes = this.widgetConfig.colorPickedAttributes.filter(
            item => item.attributeRef.id !== attributeRef.id || item.attributeRef.name !== attributeRef.name
        );
    }


    protected openAlgorithmMethodsDialog(attributeRef: AttributeRef) {
        const methodList: ListItem[] = Object.entries(this.widgetConfig.attributeSettings)
            .filter(([key]) => key.includes('method'))
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
                    translate: true,
                };
            });
        let selected: ListItem[] = [];

        showDialog(new OrMwcDialog()
            .setContent(html`
                <div id="method-creator">
                    <or-mwc-list id="method-creator-list" .type="${ListType.MULTI_CHECKBOX}" 
                                 .listItems="${methodList}" 
                                 .values="${methodList.map(item => item.data)}"
                                 @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => {selected = Array.from(ev.detail.values());
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
                                    item.value as keyof ReportWidgetConfig["attributeSettings"],
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
        this.widgetConfig.chartSettings.showLegend = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onShowToolBoxToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.chartSettings.showToolBox = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onDefaultStackedToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.chartSettings.defaultStacked = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onIsChartToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.isChart = ev.detail.value;
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

    protected onDecimalsChange(ev: OrInputChangedEvent) {
        this.widgetConfig.decimals = ev.detail.value;
        this.notifyConfigUpdate();
    }

}
