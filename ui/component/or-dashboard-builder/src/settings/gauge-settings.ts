import {html, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import {i18next} from "@openremote/or-translate";
import {GaugeWidgetConfig} from "../widgets/gauge-widget";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {Attribute} from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {ThresholdChangeEvent} from "../panels/thresholds-panel";

@customElement("gauge-settings")
export class GaugeSettings extends WidgetSettings {

    // Override of widgetConfig with extended type
    protected widgetConfig!: GaugeWidgetConfig;

    protected render(): TemplateResult {
        const attributeFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
            return ["positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction"].includes(attr.type!)
        };
        return html`
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>
                
                <!-- Min/max and decimals options-->
                <settings-panel displayName="values" expanded="${true}">
                    <div style="padding-bottom: 12px; display: flex; flex-direction: column; gap: 12px;">
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('min')}" .max="${this.widgetConfig.max}" .value="${this.widgetConfig.min}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('min', ev)}"
                            ></or-mwc-input>
                            <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('max')}" .min="${this.widgetConfig.min}" .value="${this.widgetConfig.max}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onMinMaxValueChange('max', ev)}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;" .value="${this.widgetConfig.decimals}" label="${i18next.t('decimals')}" .min="${0}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onDecimalsChange(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
                
                <!-- Thresholds panel -->
                <settings-panel displayName="thresholds" expanded="${true}">
                    <thresholds-panel .thresholds="${this.widgetConfig.thresholds}" .valueType="${'number'}" style="padding-bottom: 12px;"
                                      .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                                      @threshold-change="${(ev: ThresholdChangeEvent) => this.onThresholdChange(ev)}">
                    </thresholds-panel>
                </settings-panel>
            </div>
        `;
    }

    // When new attributes get selected
    // Update the displayName to the new asset and attribute name.
    protected onAttributesSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        if(ev.detail.attributeRefs.length === 1) {
            const attributeRef = ev.detail.attributeRefs[0];
            const asset = ev.detail.assets.find((asset) => asset.id === attributeRef.id);
            this.setDisplayName!(asset ? `${asset.name} - ${attributeRef.name}` : `${attributeRef.name}`);
        }
        this.notifyConfigUpdate();
    }

    protected onMinMaxValueChange(type: 'min' | 'max', ev: OrInputChangedEvent) {
        switch (type) {
            case "max": {
                this.widgetConfig.max = ev.detail.value;

                // Make sure every threshold value is not higher than what is allowed.
                const sortedThresholds = this.widgetConfig.thresholds.sort((x, y) => y[0] - x[0]);
                sortedThresholds.forEach((t, index) => {
                    t[0] = Math.max(Math.min(t[0], (ev.detail.value - index - 1)), this.widgetConfig.min);
                });
                break;
            }
            case "min": {
                this.widgetConfig.min = ev.detail.value;

                // Update the lowest (locked) threshold value to minimum value, and make sure every threshold value is not lower than what is allowed.
                const sortedThresholds = this.widgetConfig.thresholds.sort((x, y) => (x[0] < y[0]) ? -1 : 1);
                sortedThresholds[0][0] = ev.detail.value;
                sortedThresholds.forEach((t, index) => {
                    t[0] = Math.min(Math.max(t[0], (ev.detail.value + index)), this.widgetConfig.max);
                })
                break;
            }
        }
        this.notifyConfigUpdate();
    }

    protected onDecimalsChange(ev: OrInputChangedEvent) {
        this.widgetConfig.decimals = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onThresholdChange(ev: ThresholdChangeEvent) {
        this.widgetConfig.thresholds = ev.detail;
        this.notifyConfigUpdate();
    }

}
