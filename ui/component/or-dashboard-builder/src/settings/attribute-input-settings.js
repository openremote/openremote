var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetWidgetSettings } from "../util/or-asset-widget";
const styling = css `
  .switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;
let AttributeInputSettings = class AttributeInputSettings extends AssetWidgetSettings {
    static get styles() {
        return [...super.styles, styling];
    }
    render() {
        return html `
            <div>
                <!-- Attribute selection -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" style="padding-bottom: 12px;"
                                      @attribute-select="${(ev) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>

                <!-- Other settings -->
                <settings-panel displayName="settings" expanded="${true}">
                    <div>
                        <!-- Toggle readonly -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.userCanEdit"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${!this.widgetConfig.readonly}"
                                          @or-mwc-input-changed="${(ev) => this.onReadonlyToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                        <!-- Toggle helper text -->
                        <div class="switch-container">
                            <span><or-translate value="dashboard.showHelperText"></or-translate></span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${this.widgetConfig.showHelperText}"
                                          @or-mwc-input-changed="${(ev) => this.onHelperTextToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
            </div>
        `;
    }
    onAttributesSelect(ev) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        if (ev.detail.attributeRefs.length === 1) {
            const asset = ev.detail.assets.find((asset) => asset.id === ev.detail.attributeRefs[0].id);
            if (asset) {
                this.setDisplayName(asset.name);
            }
        }
        this.notifyConfigUpdate();
    }
    onReadonlyToggle(ev) {
        this.widgetConfig.readonly = !ev.detail.value;
        this.notifyConfigUpdate();
    }
    onHelperTextToggle(ev) {
        this.widgetConfig.showHelperText = ev.detail.value;
        this.notifyConfigUpdate();
    }
};
AttributeInputSettings = __decorate([
    customElement("attribute-input-settings")
], AttributeInputSettings);
export { AttributeInputSettings };
//# sourceMappingURL=attribute-input-settings.js.map