import {css, html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";
import {TableWidgetConfig} from "../widgets/table-widget";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {AssetIdsSelectEvent, AssetTypeSelectEvent, AssetTypesFilterConfig, AttributeNamesSelectEvent} from "../panels/assettypes-panel";

const styling = css`
  .customMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;

@customElement("table-settings")
export class TableSettings extends AssetWidgetSettings {

    protected widgetConfig!: TableWidgetConfig;

    static get styles() {
        return [...super.styles, styling];
    }

    protected render(): TemplateResult {
        const config = {
            assets: {
                enabled: true,
                multi: true
            },
            attributes: {
                enabled: true,
                multi: true
            }
        } as AssetTypesFilterConfig
        return html`
            <div>
                <!-- Asset type, assets, and attribute picker -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <div style="padding-bottom: 12px;">
                        <assettypes-panel .assetType="${this.widgetConfig.assetType}" .config="${config}"
                                          .assetIds="${this.widgetConfig.assetIds}" .attributeNames="${this.widgetConfig.attributeNames}"
                                          @assettype-select="${(ev: AssetTypeSelectEvent) => this.onAssetTypeSelect(ev)}"
                                          @assetids-select="${(ev: AssetIdsSelectEvent) => this.onAssetIdsSelect(ev)}"
                                          @attributenames-select="${(ev: AttributeNamesSelectEvent) => this.onAttributesSelect(ev)}"
                        ></assettypes-panel>
                    </div>
                </settings-panel>
                
                <!-- Table settings like amount of rows -->
                <settings-panel displayName="dashboard.tableSettings" expanded="${true}">
                    <div style="padding-bottom: 12px;">
                        <div class="customMwcInputContainer">
                            <span style="min-width: 180px"><or-translate value="dashboard.numberOfRows"></or-translate></span>
                            <or-mwc-input type="${InputType.SELECT}" .options="${[10, 25, 100]}" .value="${this.widgetConfig.tableSize}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTableSizeSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
            </div>
        `
    }

    protected onAssetTypeSelect(ev: AssetTypeSelectEvent) {
        this.widgetConfig.assetType = ev.detail;
        this.widgetConfig.assetIds = [];
        this.widgetConfig.attributeNames = [];
        this.notifyConfigUpdate();
    }

    protected onAssetIdsSelect(ev: AssetIdsSelectEvent) {
        this.widgetConfig.assetIds = ev.detail as string[];
        this.notifyConfigUpdate();
    }

    protected onAttributesSelect(ev: AttributeNamesSelectEvent) {
        this.widgetConfig.attributeNames = ev.detail as string[];
        this.notifyConfigUpdate();
    }

    protected onTableSizeSelect(ev: OrInputChangedEvent) {
        const value = ev.detail.value || 10;
        this.widgetConfig.tableSize = value;
        if(value !== 10) {
            this.widgetConfig.tableOptions = [value];
        } else {
            this.widgetConfig.tableOptions = [10, 25, 100]
        }
        this.notifyConfigUpdate();
    }
}
