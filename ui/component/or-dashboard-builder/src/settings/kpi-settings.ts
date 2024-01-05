import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";
import {i18next} from "@openremote/or-translate";
import {KpiWidgetConfig} from "../widgets/kpi-widget";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {Attribute, AttributeRef} from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";

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
                        <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;"
                                      .options="${['year', 'month', 'week', 'day', 'hour']}"
                                      .value="${this.widgetConfig.period}" label="${i18next.t('timeframe')}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTimeframeSelect(ev)}"
                        ></or-mwc-input>
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
                        <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${['absolute', 'percentage']}" .value="${this.widgetConfig.deltaFormat}"
                                      label="${i18next.t('dashboard.showValueAs')}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onDeltaFormatSelect(ev)}"
                        ></or-mwc-input>
                        <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;" .value="${this.widgetConfig.decimals}" label="${i18next.t('decimals')}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onDecimalsChange(ev)}"
                        ></or-mwc-input>
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

    protected onTimeframeSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.period = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onTimeframeToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showTimestampControls = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onDeltaFormatSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.deltaFormat = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onDecimalsChange(ev: OrInputChangedEvent) {
        this.widgetConfig.decimals = ev.detail.value;
        this.notifyConfigUpdate();
    }

}
