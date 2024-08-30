import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {AttributeInputWidgetConfig} from "../widgets/attribute-input-widget";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {AssetWidgetSettings} from "../util/or-asset-widget";

const styling = css`
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;

@customElement("attribute-input-settings")
export class AttributeInputSettings extends AssetWidgetSettings {

    protected readonly widgetConfig!: AttributeInputWidgetConfig;

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        return html`
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Other settings -->
                <settings-panel displayName="settings" expanded="${true}">
                    <div>
                        <!-- Toggle readonly -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.userCanEdit"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.readonly}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onReadonlyToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Toggle helper text -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.showHelperText"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showHelperText}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onHelperTextToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
            </div>
        `;
    }

    protected onAttributesSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        if(ev.detail.attributeRefs.length === 1) {
            const asset = ev.detail.assets.find((asset) => asset.id === ev.detail.attributeRefs[0].id);
            if(asset) {
                this.setDisplayName!(asset.name);
            }
        }
        this.notifyConfigUpdate();
    }

    protected onReadonlyToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.readonly = !ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onHelperTextToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showHelperText = ev.detail.value;
        this.notifyConfigUpdate();
    }

}
