/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {html, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import {GaugeWidgetConfig} from "../widgets/gauge-widget";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {Attribute} from "@openremote/model";
import {ThresholdChangeEvent} from "../panels/thresholds-panel";
import {OrVaadinNumberField} from "@openremote/or-vaadin-components/or-vaadin-number-field";

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
                            <or-vaadin-number-field value=${this.widgetConfig.min} max=${this.widgetConfig.max}
                                                    @change=${(ev: Event) => this.onMinMaxValueChange("min", ev)}>
                                <or-translate slot="label" value="min"></or-translate>
                            </or-vaadin-number-field>
                            <or-vaadin-number-field value=${this.widgetConfig.max} min=${this.widgetConfig.min}
                                                    @change=${(ev: Event) => this.onMinMaxValueChange("max", ev)}>
                                <or-translate slot="label" value="max"></or-translate>
                            </or-vaadin-number-field>
                        </div>
                        <div>
                            <or-vaadin-number-field value=${this.widgetConfig.decimals} min="0"
                                                    @change=${(ev: Event) => this.onDecimalsChange(ev)}>
                                <or-translate slot="label" value="decimals"></or-translate>
                            </or-vaadin-number-field>
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

    protected onMinMaxValueChange(type: 'min' | 'max', ev: Event) {
        const elem = ev.currentTarget as OrVaadinNumberField;
        if(!elem.checkValidity()) {
            return;
        }
        const newValue = Number(elem.value);
        switch (type) {
            case "max": {
                this.widgetConfig.max = newValue;

                // Make sure every threshold value is not higher than what is allowed.
                const sortedThresholds = this.widgetConfig.thresholds.sort((x, y) => y[0] - x[0]);
                sortedThresholds.forEach((t, index) => {
                    t[0] = Math.max(Math.min(t[0], (newValue - index - 1)), this.widgetConfig.min);
                });
                break;
            }
            case "min": {
                this.widgetConfig.min = newValue;

                // Update the lowest (locked) threshold value to minimum value, and make sure every threshold value is not lower than what is allowed.
                const sortedThresholds = this.widgetConfig.thresholds.sort((x, y) => (x[0] < y[0]) ? -1 : 1);
                sortedThresholds[0][0] = newValue;
                sortedThresholds.forEach((t, index) => {
                    t[0] = Math.min(Math.max(t[0], (newValue + index)), this.widgetConfig.max);
                })
                break;
            }
        }
        this.notifyConfigUpdate();
    }

    protected onDecimalsChange(ev: Event) {
        const elem = ev.currentTarget as OrVaadinNumberField;
        if(elem.checkValidity()) {
            this.widgetConfig.decimals = Number(elem.value);
            this.notifyConfigUpdate();
        }
    }

    protected onThresholdChange(ev: ThresholdChangeEvent) {
        this.widgetConfig.thresholds = ev.detail;
        this.notifyConfigUpdate();
    }

}
