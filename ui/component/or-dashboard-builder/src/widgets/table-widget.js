var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var TableWidget_1;
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { OrAssetWidget } from "../util/or-asset-widget";
import { TableSettings } from "../settings/table-settings";
import { i18next } from "@openremote/or-translate";
import { Util } from "@openremote/core";
import { AssetModelUtil } from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-table";
function getDefaultConfig() {
    return {
        assetType: undefined,
        assetIds: [],
        attributeNames: [],
        tableSize: 10,
        tableOptions: [10, 25, 100]
    };
}
const styling = css `
    #widget-wrapper {
      height: 100%;
      overflow: hidden;
    }
`;
let TableWidget = TableWidget_1 = class TableWidget extends OrAssetWidget {
    static getManifest() {
        return {
            displayName: "Table",
            displayIcon: "table",
            getContentHtml(config) {
                return new TableWidget_1(config);
            },
            getDefaultConfig() {
                return getDefaultConfig();
            },
            getSettingsHtml(config) {
                return new TableSettings(config);
            }
        };
    }
    static get styles() {
        return [...super.styles, styling];
    }
    // TODO: Improve this to be more efficient
    refreshContent(force) {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig));
    }
    // Lit Lifecycle
    willUpdate(changedProps) {
        if (changedProps.has('widgetConfig') && this.widgetConfig) {
            this.loadAssets();
        }
        return super.willUpdate(changedProps);
    }
    /* --------------------------------------- */
    loadAssets() {
        if (this.widgetConfig.assetIds.find(id => !this.isAssetLoaded(id))) {
            this.queryAssets({
                ids: this.widgetConfig.assetIds,
                select: {
                    attributes: this.widgetConfig.attributeNames
                }
            }).then((assets) => {
                this.loadedAssets = assets;
            });
        }
    }
    getColumns(attributeNames) {
        const referenceAsset = this.loadedAssets[0];
        const attrColumns = attributeNames.map(attrName => {
            var _a, _b;
            let text = attrName;
            let numeric = false;
            if (this.widgetConfig.assetType && referenceAsset && referenceAsset.attributes && referenceAsset.attributes[attrName]) {
                const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, this.widgetConfig.assetType);
                text = Util.getAttributeLabel(referenceAsset.attributes[attrName], attributeDescriptor, this.widgetConfig.assetType, true);
                numeric = ((_a = attributeDescriptor === null || attributeDescriptor === void 0 ? void 0 : attributeDescriptor.format) === null || _a === void 0 ? void 0 : _a.asNumber) || ((_b = attributeDescriptor === null || attributeDescriptor === void 0 ? void 0 : attributeDescriptor.format) === null || _b === void 0 ? void 0 : _b.asSlider) || false;
            }
            return {
                title: text,
                isSortable: true,
                isNumeric: numeric
            };
        });
        return Array.of({ title: i18next.t('assetName'), isSortable: true }, ...attrColumns);
    }
    getRows(attributeNames) {
        return this.loadedAssets.map(asset => {
            const attrEntries = attributeNames.map(attrName => {
                if (asset.attributes && asset.attributes[attrName]) {
                    const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, asset.type);
                    const strValue = Util.getAttributeValueAsString(asset.attributes[attrName], attributeDescriptor, asset.type, false);
                    const numValue = parseFloat(strValue);
                    return isNaN(numValue) ? strValue : numValue;
                }
                else {
                    return 'N.A.';
                }
            });
            const content = Array.of(asset.name, ...attrEntries);
            return {
                content: content
            };
        });
    }
    render() {
        const tableConfig = {
            fullHeight: true,
            pagination: {
                enable: true,
                options: this.widgetConfig.tableOptions,
            }
        };
        const columns = this.getColumns(this.widgetConfig.attributeNames);
        const rows = this.getRows(this.widgetConfig.attributeNames);
        return html `
            <div id="widget-wrapper">
                <or-mwc-table .columns="${columns}" .rows="${rows}" .config="${tableConfig}" .paginationSize="${this.widgetConfig.tableSize}"
                              @or-mwc-table-row-click="${(ev) => this.onTableRowClick(ev)}"
                ></or-mwc-table>
            </div>
        `;
    }
    onTableRowClick(ev) {
    }
};
TableWidget = TableWidget_1 = __decorate([
    customElement("table-widget")
], TableWidget);
export { TableWidget };
//# sourceMappingURL=table-widget.js.map