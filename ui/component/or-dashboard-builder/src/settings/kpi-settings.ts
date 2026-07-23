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
import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";
import {i18next} from "@openremote/or-translate";
import {KpiWidgetConfig} from "../widgets/kpi-widget";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {Attribute} from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {OrVaadinSelect} from "@openremote/or-vaadin-components/or-vaadin-select";
import {OrVaadinNumberField} from "@openremote/or-vaadin-components/or-vaadin-number-field";

const styling = css`
  .switchMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;

@customElement("kpi-settings")
export class KpiSettings extends AssetWidgetSettings {

    protected widgetConfig!: KpiWidgetConfig;

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        const attributeFilter: (attr: Attribute<any>) => boolean = (attr): boolean => {
            return ["positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction"].includes(attr.type!)
        };
        return html`
            <div>
                <!-- Attribute selector -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" onlyDataAttrs="${false}" .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Display settings -->
                <settings-panel displayName="display" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <or-vaadin-select .items=${['year', 'month', 'week', 'day', 'hour'].map(i => ({value: i, label: i18next.t(i)}))}
                                          value=${this.widgetConfig.period} @change=${(ev: Event) => this.onTimeframeSelect(ev)}>
                            <or-translate slot="label" value="timeframe"></or-translate>
                        </or-vaadin-select>
                        <div class="switchMwcInputContainer">
                            <span><or-translate value="dashboard.allowTimerangeSelect"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showTimestampControls}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeframeToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <settings-panel displayName="values" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <or-vaadin-select .items=${['absolute', 'percentage'].map(i => ({value: i, label: i18next.t(i)}))}
                                          value=${this.widgetConfig.deltaFormat} @change=${(ev: Event) => this.onDeltaFormatSelect(ev)}>
                            <or-translate slot="label" value="dashboard.showValueAs"></or-translate>
                        </or-vaadin-select>
                        <or-vaadin-number-field value=${this.widgetConfig.decimals} min="0"
                                                @change=${(ev: Event) => this.onDecimalsChange(ev)}>
                            <or-translate slot="label" value="decimals"></or-translate>
                        </or-vaadin-number-field>
                    </div>
                </settings-panel>

                <!-- -->
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

    protected onTimeframeSelect(ev: Event) {
        this.widgetConfig.period = (ev.currentTarget as OrVaadinSelect).value as any;
        this.notifyConfigUpdate();
    }

    protected onTimeframeToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showTimestampControls = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onDeltaFormatSelect(ev: Event) {
        this.widgetConfig.deltaFormat = (ev.currentTarget as OrVaadinSelect).value as any;
        this.notifyConfigUpdate();
    }

    protected onDecimalsChange(ev: Event) {
        const elem = ev.currentTarget as OrVaadinNumberField;
        if(elem.checkValidity()) {
            this.widgetConfig.decimals = Number(elem.value);
            this.notifyConfigUpdate();
        }
    }

}
