var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
const styling = css `
  .customMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;
let TableSettings = class TableSettings extends AssetWidgetSettings {
    static get styles() {
        return [...super.styles, styling];
    }
    render() {
        const config = {
            assets: {
                enabled: true,
                multi: true
            },
            attributes: {
                enabled: true,
                multi: true
            }
        };
        return html `
            <div>
                <!-- Asset type, assets, and attribute picker -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <div style="padding-bottom: 12px;">
                        <assettypes-panel .assetType="${this.widgetConfig.assetType}" .config="${config}"
                                          .assetIds="${this.widgetConfig.assetIds}" .attributeNames="${this.widgetConfig.attributeNames}"
                                          @assettype-select="${(ev) => this.onAssetTypeSelect(ev)}"
                                          @assetids-select="${(ev) => this.onAssetIdsSelect(ev)}"
                                          @attributenames-select="${(ev) => this.onAttributesSelect(ev)}"
                        ></assettypes-panel>
                    </div>
                </settings-panel>
                
                <!-- Table settings like amount of rows -->
                <settings-panel displayName="dashboard.tableSettings" expanded="${true}">
                    <div style="padding-bottom: 12px;">
                        <div class="customMwcInputContainer">
                            <span style="min-width: 180px"><or-translate value="dashboard.numberOfRows"></or-translate></span>
                            <or-mwc-input type="${InputType.SELECT}" .options="${[10, 25, 100]}" .value="${this.widgetConfig.tableSize}"
                                          @or-mwc-input-changed="${(ev) => this.onTableSizeSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
            </div>
        `;
    }
    onAssetTypeSelect(ev) {
        this.widgetConfig.assetType = ev.detail;
        this.widgetConfig.assetIds = [];
        this.widgetConfig.attributeNames = [];
        this.notifyConfigUpdate();
    }
    onAssetIdsSelect(ev) {
        this.widgetConfig.assetIds = ev.detail;
        this.notifyConfigUpdate();
    }
    onAttributesSelect(ev) {
        this.widgetConfig.attributeNames = ev.detail;
        this.notifyConfigUpdate();
    }
    onTableSizeSelect(ev) {
        const value = ev.detail.value || 10;
        this.widgetConfig.tableSize = value;
        if (value !== 10) {
            this.widgetConfig.tableOptions = [value];
        }
        else {
            this.widgetConfig.tableOptions = [10, 25, 100];
        }
        this.notifyConfigUpdate();
    }
};
TableSettings = __decorate([
    customElement("table-settings")
], TableSettings);
export { TableSettings };
//# sourceMappingURL=table-settings.js.map