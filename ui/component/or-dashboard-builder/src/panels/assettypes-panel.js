var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { AssetModelUtil } from "@openremote/model";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { i18next } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import manager, { Util } from "@openremote/core";
import { when } from "lit/directives/when.js";
import { createRef, ref } from 'lit/directives/ref.js';
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
export class AssetTypeSelectEvent extends CustomEvent {
    constructor(assetTypeName) {
        super(AssetTypeSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetTypeName
        });
    }
}
AssetTypeSelectEvent.NAME = "assettype-select";
export class AssetIdsSelectEvent extends CustomEvent {
    constructor(assetIds) {
        super(AssetIdsSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetIds
        });
    }
}
AssetIdsSelectEvent.NAME = "assetids-select";
export class AttributeNamesSelectEvent extends CustomEvent {
    constructor(attributeNames) {
        super(AttributeNamesSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: attributeNames
        });
    }
}
AttributeNamesSelectEvent.NAME = "attributenames-select";
const styling = css `
  .switchMwcInputContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
`;
let AssettypesPanel = class AssettypesPanel extends LitElement {
    constructor() {
        super(...arguments);
        this.config = {
            attributes: {
                enabled: true
            }
        };
        /* ----------- */
        this._attributeSelectList = [];
        this._loadedAssetTypes = AssetModelUtil.getAssetDescriptors().filter((t) => t.descriptorType === "asset");
        this.assetTreeDataProvider = () => __awaiter(this, void 0, void 0, function* () {
            const assetQuery = {
                realm: {
                    name: manager.displayRealm
                },
                select: {
                    attributes: []
                }
            };
            // At first, just fetch all accessible assets without attribute info...
            const assets = (yield manager.rest.api.AssetResource.queryAssets(assetQuery)).data;
            // After fetching, narrow down the list to assets with the same assetType.
            // Since it is a tree, we also include the parents of those assets, based on the 'asset.path' variable.
            const pathsOfAssetType = assets.filter(a => a.type === this.assetType).map(a => a.path);
            const filteredAssetIds = [...new Set([].concat(...pathsOfAssetType))];
            return assets.filter(a => filteredAssetIds.includes(a.id));
        });
    }
    static get styles() {
        return [styling];
    }
    willUpdate(changedProps) {
        super.willUpdate(changedProps);
        if (changedProps.has("assetType") && this.assetType) {
            this._attributeSelectList = this.getAttributesByType(this.assetType);
            this.dispatchEvent(new AssetTypeSelectEvent(this.assetType));
        }
        if (changedProps.has("assetIds") && this.assetIds) {
            this.dispatchEvent(new AssetIdsSelectEvent(this.assetIds));
        }
        if (changedProps.has("attributeNames") && this.attributeNames) {
            this.dispatchEvent(new AttributeNamesSelectEvent(this.attributeNames));
        }
    }
    render() {
        var _a, _b;
        return html `
            <div style="display: flex; flex-direction: column; gap: 8px;">

                <!-- Select asset type -->
                <div>
                    ${this._loadedAssetTypes.length > 0 ? getContentWithMenuTemplate(this.getAssetTypeTemplate(), this.mapDescriptors(this._loadedAssetTypes, {
            text: i18next.t("filter.assetTypeMenuNone"),
            value: "",
            icon: "selection-ellipse"
        }), undefined, (v) => {
            this.assetType = v;
        }, undefined, false, true, true, true) : html ``}
                </div>

                <!-- Select one or more assets -->
                ${when((_a = this.config.assets) === null || _a === void 0 ? void 0 : _a.enabled, () => {
            var _a;
            const assetIds = (typeof this.assetIds === 'string') ? [this.assetIds] : this.assetIds;
            return html `
                        <div>
                            <or-mwc-input .type="${InputType.BUTTON}" .label="${(((_a = this.assetIds) === null || _a === void 0 ? void 0 : _a.length) || 0) + ' ' + i18next.t('assets')}" .disabled="${!this.assetType}" fullWidth outlined comfortable style="width: 100%;"
                                          @or-mwc-input-changed="${(ev) => { var _a; return this._openAssetSelector(this.assetType, assetIds, (_a = this.config.assets) === null || _a === void 0 ? void 0 : _a.multi); }}"
                            ></or-mwc-input>
                        </div>
                    `;
        })}

                <!-- Select one or more attributes -->
                ${when((_b = this.config.attributes) === null || _b === void 0 ? void 0 : _b.enabled, () => {
            var _a;
            const options = this._attributeSelectList.map(al => [al[0], al[1]]);
            const searchProvider = (search) => __awaiter(this, void 0, void 0, function* () {
                return search ? options.filter(o => o[1].toLowerCase().includes(search.toLowerCase())) : options;
            });
            return html `
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t("filter.attributeLabel")}" .disabled="${!this.assetType}" style="width: 100%;"
                                          .options="${options}" .searchProvider="${searchProvider}" .multiple="${(_a = this.config.attributes) === null || _a === void 0 ? void 0 : _a.multi}" .value="${this.attributeNames}"
                                          @or-mwc-input-changed="${(ev) => {
                this.attributeNames = ev.detail.value;
            }}"
                            ></or-mwc-input>
                        </div>
                    `;
        })}
            </div>
        `;
    }
    /* ----------- */
    getAssetTypeTemplate() {
        if (this.assetType) {
            const descriptor = this._loadedAssetTypes.find((at) => at.name === this.assetType);
            if (descriptor) {
                return this.getSelectedHeader(descriptor);
            }
            else {
                return this.getSelectHeader();
            }
        }
        else {
            return this.getSelectHeader();
        }
    }
    getSelectHeader() {
        return html `
            <or-mwc-input style="width:100%;" type="${InputType.TEXT}" readonly .label="${i18next.t("filter.assetTypeLabel")}"
                          iconTrailing="menu-down" iconColor="rgba(0, 0, 0, 0.87)" icon="selection-ellipse"
                          value="${i18next.t("filter.assetTypeNone")}">
            </or-mwc-input>
        `;
    }
    getSelectedHeader(descriptor) {
        return html `
            <or-mwc-input style="width:100%;" type="${InputType.TEXT}" readonly .label="${i18next.t("filter.assetTypeLabel")}"
                          .iconColor="${descriptor.colour}" iconTrailing="menu-down" icon="${descriptor.icon}"
                          value="${Util.getAssetTypeLabel(descriptor)}">
            </or-mwc-input>
        `;
    }
    mapDescriptors(descriptors, withNoneValue) {
        const items = descriptors.map((descriptor) => {
            return {
                styleMap: {
                    "--or-icon-fill": descriptor.colour ? "#" + descriptor.colour : "unset"
                },
                icon: descriptor.icon,
                text: Util.getAssetTypeLabel(descriptor),
                value: descriptor.name,
                data: descriptor
            };
        }).sort(Util.sortByString((listItem) => listItem.text));
        if (withNoneValue) {
            items.splice(0, 0, withNoneValue);
        }
        return items;
    }
    getAttributesByType(type) {
        var _a;
        const descriptor = AssetModelUtil.getAssetDescriptor(type);
        if (descriptor) {
            const typeInfo = AssetModelUtil.getAssetTypeInfo(descriptor);
            if (typeInfo === null || typeInfo === void 0 ? void 0 : typeInfo.attributeDescriptors) {
                const valueTypes = (_a = this.config.attributes) === null || _a === void 0 ? void 0 : _a.valueTypes;
                const filtered = valueTypes ? typeInfo.attributeDescriptors.filter(ad => valueTypes.indexOf(ad.type) > -1) : typeInfo.attributeDescriptors;
                return filtered
                    .map((ad) => {
                    const label = Util.getAttributeLabel(ad, undefined, type, false);
                    return [ad.name, label];
                })
                    .sort(Util.sortByString((attr) => attr[1]));
            }
        }
    }
    _openAssetSelector(assetType, assetIds, multi = false) {
        const assetTreeRef = createRef();
        const config = {
            select: {
                types: [assetType],
                multiSelect: multi
            }
        };
        const dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("linkedAssets"))
            .setContent(html `
                <div style="width: 400px;">
                    <or-asset-tree ${ref(assetTreeRef)} .dataProvider="${this.assetTreeDataProvider}" expandAllNodes
                                   id="chart-asset-tree" readonly .config="${config}" .selectedIds="${assetIds}"
                                   .showSortBtn="${false}" .showFilter="${false}" .checkboxes="${multi}"
                    ></or-asset-tree>
                </div>
            `)
            .setActions([
            {
                default: true,
                actionName: "cancel",
                content: "cancel",
            },
            {
                actionName: "ok",
                content: "ok",
                action: () => {
                    const tree = assetTreeRef.value;
                    if (tree === null || tree === void 0 ? void 0 : tree.selectedIds) {
                        if (multi) {
                            this.assetIds = tree.selectedIds;
                        }
                        else {
                            this.assetIds = tree.selectedIds[0];
                        }
                    }
                }
            }
        ])
            .setDismissAction({
            actionName: "cancel",
        }));
    }
};
__decorate([
    property() // selected asset type
], AssettypesPanel.prototype, "assetType", void 0);
__decorate([
    property()
], AssettypesPanel.prototype, "config", void 0);
__decorate([
    property() // IDs of assets; either undefined, a single entry, or multi select
], AssettypesPanel.prototype, "assetIds", void 0);
__decorate([
    property() // names of selected attributes; either undefined, a single entry, or multi select
], AssettypesPanel.prototype, "attributeNames", void 0);
__decorate([
    state()
], AssettypesPanel.prototype, "_attributeSelectList", void 0);
__decorate([
    state()
], AssettypesPanel.prototype, "_loadedAssetTypes", void 0);
AssettypesPanel = __decorate([
    customElement("assettypes-panel")
], AssettypesPanel);
export { AssettypesPanel };
//# sourceMappingURL=assettypes-panel.js.map