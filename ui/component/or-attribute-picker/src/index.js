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
import { html, unsafeCSS } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import "@openremote/or-asset-tree";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-mwc-components/or-mwc-list";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import manager, { DefaultColor2, DefaultColor4, DefaultColor5, Util } from "@openremote/core";
import { ListType } from "@openremote/or-mwc-components/or-mwc-list";
import { OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
export class OrAttributePickerPickedEvent extends CustomEvent {
    constructor(detail) {
        super(OrAttributePickerPickedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}
OrAttributePickerPickedEvent.NAME = "or-attribute-picker-picked";
let OrAttributePicker = class OrAttributePicker extends OrMwcDialog {
    constructor() {
        super();
        this.showOnlyDatapointAttrs = false;
        this.showOnlyRuleStateAttrs = false;
        this.multiSelect = false;
        this.selectedAttributes = [];
        this.selectedAssets = [];
        this.heading = i18next.t("selectAttributes");
        this.setDialogContent();
        this.setDialogActions();
        this.dismissAction = null;
        this.styles = `
            .attributes-header {
                line-height: 48px;
                padding: 0 15px;
                background-color: ${unsafeCSS(DefaultColor2)};
                font-weight: bold;
                border-bottom: 1px solid ${unsafeCSS(DefaultColor2)};
            }
            footer.mdc-dialog__actions {
                border-top: 1px solid ${unsafeCSS(DefaultColor5)};
            }
            #header {
                background-color: ${unsafeCSS(DefaultColor4)} !important;
            }
            #dialog-content {
                padding: 0;
            }
        `;
    }
    setShowOnlyDatapointAttrs(showOnlyDatapointAttrs) {
        this.showOnlyDatapointAttrs = showOnlyDatapointAttrs;
        return this;
    }
    setShowOnlyRuleStateAttrs(showOnlyRuleStateAttrs) {
        this.showOnlyRuleStateAttrs = showOnlyRuleStateAttrs;
        return this;
    }
    setAttributeFilter(attributeFilter) {
        this.attributeFilter = attributeFilter;
        return this;
    }
    setMultiSelect(multiSelect) {
        this.multiSelect = multiSelect;
        return this;
    }
    setSelectedAttributes(selectedAttributes) {
        this.selectedAttributes = selectedAttributes;
        return this;
    }
    setSelectedAssets(selectedAssets) {
        this.selectedAssets = selectedAssets;
        this.setDialogContent();
        return this;
    }
    setOpen(isOpen) {
        super.setOpen(isOpen);
        return this;
    }
    setHeading(heading) {
        super.setHeading(heading);
        return this;
    }
    setContent(content) {
        throw new Error("Cannot modify attribute picker content");
    }
    setActions(actions) {
        throw new Error("Cannot modify attribute picker actions");
    }
    setDismissAction(action) {
        throw new Error("Cannot modify attribute picker dismiss action");
    }
    setStyles(styles) {
        throw new Error("Cannot modify attribute picker styles");
    }
    setAvatar(avatar) {
        throw new Error("Cannot modify attribute picker avatar setting");
    }
    setDialogActions() {
        this.actions = [
            {
                actionName: "cancel",
                content: "cancel"
            },
            {
                actionName: "add",
                content: html `<or-mwc-input id="add-btn" class="button" label="add"
                                            .type="${InputType.BUTTON}" ?disabled="${!this.selectedAttributes.length}"
                                            @or-mwc-input-changed="${(ev) => { if (!this.selectedAttributes.length) {
                    ev.stopPropagation();
                    return false;
                } }}"></or-mwc-input>`,
                action: () => {
                    if (!this.selectedAttributes.length) {
                        return;
                    }
                    this.dispatchEvent(new OrAttributePickerPickedEvent(this.selectedAttributes));
                }
            }
        ];
    }
    setDialogContent() {
        const getListItems = () => {
            if (!this.assetAttributes) {
                return [];
            }
            return this.assetAttributes.map((attribute) => {
                return {
                    text: Util.getAttributeLabel(undefined, attribute, undefined, true),
                    value: attribute.name
                };
            });
        };
        let selectedAttribute = undefined;
        if (!this.multiSelect && this.selectedAttributes.length === 1 && this.selectedAttributes[0].name) {
            selectedAttribute = {
                text: Util.getAttributeLabel(undefined, this.selectedAttributes[0], undefined, true),
                value: this.selectedAttributes[0].name
            };
        }
        this.content = () => html `
            <div class="row" style="display: flex;height: 600px;width: 800px;border-top: 1px solid ${unsafeCSS(DefaultColor5)};">
                <div class="col" style="width: 260px;overflow: auto;border-right: 1px solid ${unsafeCSS(DefaultColor5)};">
                    <or-asset-tree id="chart-asset-tree" readonly
                                   .selectedIds="${this.selectedAssets.length > 0 ? this.selectedAssets : null}"
                                   @or-asset-tree-selection="${(ev) => this._onAssetSelectionChanged(ev)}">
                    </or-asset-tree>
                </div>
                <div class="col" style="flex: 1 1 auto;width: 260px;overflow: auto;">
                ${this.assetAttributes && this.assetAttributes.length > 0 ? html `
                    <div class="attributes-header">
                        <or-translate value="attribute_plural"></or-translate>
                    </div>
                    ${this.multiSelect
            ?
                html `<div style="display: grid">
                                            <or-mwc-list
                                                    id="attribute-selector" .type="${ListType.MULTI_CHECKBOX}" .listItems="${getListItems()}"
                                                    .values="${this.selectedAttributes.filter(attributeRef => attributeRef.id === this.asset.id).map(attributeRef => attributeRef.name)}"
                                                    @or-mwc-list-changed="${(ev) => this._onAttributeSelectionChanged([...this.selectedAttributes.filter(attributeRef => attributeRef.id !== this.asset.id), ...ev.detail.map(li => { return { id: this.asset.id, name: li.value }; })])}"></or-mwc-list>
                                        </div>`
            :
                html `<or-mwc-input id="attribute-selector"
                                                style="display:flex;"
                                                .label="${i18next.t("attribute")}"
                                                .type="${InputType.LIST}"
                                                .options="${getListItems().map(item => ([item, item.text]))}"
                                                @or-mwc-input-changed="${(ev) => {
                    this._onAttributeSelectionChanged([
                        {
                            id: this.asset.id,
                            name: ev.detail.value.value
                        }
                    ]);
                }}"></or-mwc-input>`}
                ` : html `<div style="display: flex;align-items: center;text-align: center;height: 100%;padding: 0 20px;"><span style="width:100%">
                            <or-translate value="${(this.assetAttributes && this.assetAttributes.length === 0) ?
            ((this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) ? "noDatapointsOrRuleStateAttributes" :
                this.showOnlyDatapointAttrs ? "noDatapointsAttributes" :
                    this.showOnlyRuleStateAttrs ? "noRuleStateAttributes" : "noAttributesToShow") : "selectAssetOnTheLeft"}">
                            </or-translate></span></div>`}
                </div>
        `;
    }
    _onAssetSelectionChanged(event) {
        return __awaiter(this, void 0, void 0, function* () {
            this.assetAttributes = undefined;
            if (!this.multiSelect) {
                this.selectedAttributes = [];
            }
            this.addBtn.disabled = this.selectedAttributes.length === 0;
            const assetTree = event.target;
            assetTree.disabled = true;
            let selectedAsset = event.detail.newNodes.length === 0 ? undefined : event.detail.newNodes[0].asset;
            this.asset = selectedAsset;
            if (selectedAsset) {
                // Load the asset attributes
                const assetResponse = yield manager.rest.api.AssetResource.get(selectedAsset.id);
                selectedAsset = assetResponse.data;
                if (selectedAsset) {
                    this.assetAttributes = Object.values(selectedAsset.attributes).map(attr => { return Object.assign(Object.assign({}, attr), { id: selectedAsset.id }); })
                        .sort(Util.sortByString((attribute) => attribute.name));
                    if (this.attributeFilter) {
                        this.assetAttributes = this.assetAttributes.filter((attr) => this.attributeFilter(attr));
                    }
                    if (this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) {
                        this.assetAttributes = this.assetAttributes
                            .filter(e => e.meta && (e.meta["storeDataPoints" /* WellknownMetaItems.STOREDATAPOINTS */] || e.meta["ruleState" /* WellknownMetaItems.RULESTATE */] || e.meta["agentLink" /* WellknownMetaItems.AGENTLINK */]));
                    }
                    else if (this.showOnlyDatapointAttrs) {
                        this.assetAttributes = this.assetAttributes
                            .filter(e => e.meta && (e.meta["storeDataPoints" /* WellknownMetaItems.STOREDATAPOINTS */] || e.meta["agentLink" /* WellknownMetaItems.AGENTLINK */]));
                    }
                    else if (this.showOnlyRuleStateAttrs) {
                        this.assetAttributes = this.assetAttributes
                            .filter(e => e.meta && (e.meta["ruleState" /* WellknownMetaItems.RULESTATE */] || e.meta["agentLink" /* WellknownMetaItems.AGENTLINK */]));
                    }
                }
            }
            assetTree.disabled = false;
        });
    }
    _onAttributeSelectionChanged(attributeRefs) {
        this.selectedAttributes = attributeRefs;
        this.addBtn.disabled = this.selectedAttributes.length === 0;
    }
};
__decorate([
    property({ type: Boolean })
], OrAttributePicker.prototype, "showOnlyDatapointAttrs", void 0);
__decorate([
    property({ type: Boolean })
], OrAttributePicker.prototype, "showOnlyRuleStateAttrs", void 0);
__decorate([
    property()
], OrAttributePicker.prototype, "attributeFilter", void 0);
__decorate([
    property({ type: Boolean })
], OrAttributePicker.prototype, "multiSelect", void 0);
__decorate([
    state()
], OrAttributePicker.prototype, "assetAttributes", void 0);
__decorate([
    query("#add-btn")
], OrAttributePicker.prototype, "addBtn", void 0);
OrAttributePicker = __decorate([
    customElement("or-attribute-picker")
], OrAttributePicker);
export { OrAttributePicker };
//# sourceMappingURL=index.js.map