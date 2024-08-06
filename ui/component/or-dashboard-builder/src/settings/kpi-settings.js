var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { i18next } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
const styling = css `
  .switchMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;
let KpiSettings = class KpiSettings extends AssetWidgetSettings {
    static get styles() {
        return [...super.styles, styling];
    }
    render() {
        const attributeFilter = (attr) => {
            return ["positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction"].includes(attr.type);
        };
        return html `
            <div>
                <!-- Attribute selector -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" onlyDataAttrs="${false}" .attributeFilter="${attributeFilter}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Display settings -->
                <settings-panel displayName="display" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;"
                                      .options="${['year', 'month', 'week', 'day', 'hour']}"
                                      .value="${this.widgetConfig.period}" label="${i18next.t('timeframe')}"
                                      @or-mwc-input-changed="${(ev) => this.onTimeframeSelect(ev)}"
                        ></or-mwc-input>
                        <div class="switchMwcInputContainer">
                            <span><or-translate value="dashboard.allowTimerangeSelect"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showTimestampControls}"
                                          @or-mwc-input-changed="${(ev) => this.onTimeframeToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <settings-panel displayName="values" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${['absolute', 'percentage']}" .value="${this.widgetConfig.deltaFormat}"
                                      label="${i18next.t('dashboard.showValueAs')}"
                                      @or-mwc-input-changed="${(ev) => this.onDeltaFormatSelect(ev)}"
                        ></or-mwc-input>
                        <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;" .value="${this.widgetConfig.decimals}" label="${i18next.t('decimals')}"
                                      @or-mwc-input-changed="${(ev) => this.onDecimalsChange(ev)}"
                        ></or-mwc-input>
                    </div>
                </settings-panel>

                <!-- -->
            </div>
        `;
    }
    // When new attributes get selected
    // Update the displayName to the new asset and attribute name.
    onAttributesSelect(ev) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        if (ev.detail.attributeRefs.length === 1) {
            const attributeRef = ev.detail.attributeRefs[0];
            const asset = ev.detail.assets.find((asset) => asset.id === attributeRef.id);
            this.setDisplayName(asset ? `${asset.name} - ${attributeRef.name}` : `${attributeRef.name}`);
        }
        this.notifyConfigUpdate();
    }
    onTimeframeSelect(ev) {
        this.widgetConfig.period = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onTimeframeToggle(ev) {
        this.widgetConfig.showTimestampControls = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onDeltaFormatSelect(ev) {
        this.widgetConfig.deltaFormat = ev.detail.value;
        this.notifyConfigUpdate();
    }
    onDecimalsChange(ev) {
        this.widgetConfig.decimals = ev.detail.value;
        this.notifyConfigUpdate();
    }
};
KpiSettings = __decorate([
    customElement("kpi-settings")
], KpiSettings);
export { KpiSettings };
//# sourceMappingURL=kpi-settings.js.map