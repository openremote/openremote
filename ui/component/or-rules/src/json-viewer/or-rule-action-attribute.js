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
import { customElement, property } from "lit/decorators.js";
import { getAssetIdsFromQuery, getAssetTypeFromQuery } from "../index";
import { AssetModelUtil } from "@openremote/model";
import { Util } from "@openremote/core";
import "@openremote/or-attribute-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import { translate } from "@openremote/or-translate";
import { ifDefined } from "lit/directives/if-defined.js";
import { when } from "lit/directives/when.js";
// language=CSS
const style = css `
    :host {
        display: flex;
        align-items: center;

        flex-wrap: wrap;
    }

    :host > * {
        margin: 0 3px 6px;
    }

    .min-width {
        min-width: 200px;
    }
`;
let OrRuleActionAttribute = class OrRuleActionAttribute extends translate(i18next)(LitElement) {
    static get styles() {
        return style;
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("action")) {
            this._assets = undefined;
        }
        return super.shouldUpdate(_changedProperties);
    }
    refresh() {
        // Clear assets
        this._assets = undefined;
    }
    _getAssetType() {
        if (!this.action.target) {
            return;
        }
        const query = this.action.target.assets ? this.action.target.assets : this.action.target.matchedAssets ? this.action.target.matchedAssets : undefined;
        return query ? getAssetTypeFromQuery(query) : undefined;
    }
    render() {
        if (!this.action.target) {
            return html ``;
        }
        const assetType = this._getAssetType();
        if (!assetType) {
            return html ``;
        }
        const query = this.action.target.assets ? this.action.target.assets : this.action.target.matchedAssets ? this.action.target.matchedAssets : undefined;
        const assetDescriptor = this.assetInfos ? this.assetInfos.find((assetTypeInfo) => assetTypeInfo.assetDescriptor.name === assetType) : undefined;
        if (!assetDescriptor) {
            return html ``;
        }
        if (!this._assets) {
            this.loadAssets(assetType);
        }
        // TODO: Add multiselect support
        const ids = getAssetIdsFromQuery(query);
        const idValue = ids && ids.length > 0 ? ids[0] : "*";
        const idOptions = [
            ["*", i18next.t("matched")]
        ];
        let searchProvider;
        return html `
            
            <!-- Show SELECT input with 'loading' until the assets are retrieved -->
            ${when((!this._assets), () => html `
                <or-mwc-input id="matchSelect" class="min-width" type="${InputType.SELECT}" .readonly="${true}" .label="${i18next.t('loading')}"></or-mwc-input>
            `, () => {
            var _a, _b, _c;
            // Set list of displayed assets, and filtering assets out if needed.
            // If <= 25 assets: display everything
            // If between 25 and 100 assets: display everything with search functionality 
            // If > 100 assets: only display if in line with search input
            if (this._assets.length <= 25) {
                idOptions.push(...this._assets.map((asset) => [asset.id, asset.name]));
            }
            else {
                searchProvider = (search) => __awaiter(this, void 0, void 0, function* () {
                    var _d;
                    if (search) {
                        return this._assets.filter((asset) => { var _a; return (_a = asset.name) === null || _a === void 0 ? void 0 : _a.toLowerCase().includes(search.toLowerCase()); }).map((asset) => [asset.id, asset.name]);
                    }
                    else if (this._assets.length <= 100) {
                        idOptions.push(...this._assets.map((asset) => [asset.id, asset.name]));
                        return idOptions;
                    }
                    else {
                        const asset = (_d = this._assets) === null || _d === void 0 ? void 0 : _d.find((asset) => asset.id == idValue);
                        if (asset && idOptions.find(([id, _value]) => id == asset.id) == undefined) {
                            idOptions.push([asset.id, asset.name]); // add selected asset if there is one.
                        }
                        return idOptions;
                    }
                });
            }
            // Get selected asset and its descriptors
            const asset = idValue && idValue !== "*" ? this._assets.find(a => a.id === idValue) : undefined;
            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(assetType, this.action.attributeName, asset && asset.attributes && this.action.attributeName ? asset.attributes[this.action.attributeName] : undefined);
            // Only RW attributes can be used in actions
            let attributes = [];
            if (asset && asset.attributes) {
                attributes = Object.values(asset.attributes)
                    .map((attr) => {
                    const label = Util.getAttributeLabel(attr, descriptors[0], assetType, false);
                    return [attr.name, label];
                });
            }
            else if (assetDescriptor) {
                const assetTypeInfo = AssetModelUtil.getAssetTypeInfo(assetDescriptor);
                attributes =
                    !assetTypeInfo || !assetTypeInfo.attributeDescriptors
                        ? []
                        : assetTypeInfo.attributeDescriptors.map((ad) => {
                            const label = Util.getAttributeLabel(ad, descriptors[0], assetType, false);
                            return [ad.name, label];
                        });
            }
            attributes.sort(Util.sortByString((attr) => attr[1]));
            let attributeInput;
            if (this.action.attributeName) {
                const label = descriptors[1] && (descriptors[1].name === "boolean" /* WellknownValueTypes.BOOLEAN */) ? "" : i18next.t("value");
                let inputType;
                if ((_b = (_a = descriptors[0]) === null || _a === void 0 ? void 0 : _a.format) === null || _b === void 0 ? void 0 : _b.asSlider)
                    inputType = InputType.NUMBER;
                attributeInput = html `<or-attribute-input ?compact=${descriptors[1] && (descriptors[1].name === "GEO_JSONPoint" /* WellknownValueTypes.GEOJSONPOINT */)} .inputType="${ifDefined(inputType)}" @or-attribute-input-changed="${(ev) => this.setActionAttributeValue(ev.detail.value)}" .customProvider="${(_c = this.config) === null || _c === void 0 ? void 0 : _c.inputProvider}" .label="${label}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${this.action.value}" .readonly="${this.readonly || false}"></or-attribute-input>`;
            }
            return html `
                    <or-mwc-input id="matchSelect" class="min-width" .label="${i18next.t("asset")}" .type="${InputType.SELECT}"
                                  .options="${idOptions}" .searchProvider="${searchProvider}" .value="${idValue}" .readonly="${this.readonly || false}"
                                  @or-mwc-input-changed="${(e) => { this._assetId = (e.detail.value); this.refresh(); }}"
                    ></or-mwc-input>
                    ${attributes.length > 0 ? html `
                        <or-mwc-input id="attributeSelect" class="min-width" .label="${i18next.t("attribute")}" .type="${InputType.SELECT}" @or-mwc-input-changed="${(e) => this.setActionAttributeName(e.detail.value)}" .readonly="${this.readonly || false}" ?searchable="${(attributes.length >= 25)}" .options="${attributes}" .value="${this.action.attributeName}"></or-mwc-input>
                        ${attributeInput}
                    ` : html `
                        <or-translate value="No attributes with write permission"></or-translate>
                    `}
                `;
        })}
        `;
    }
    set _assetId(assetId) {
        const assetType = this._getAssetType();
        if (assetId === "*") {
            this.action.target.assets = undefined;
            this.action.target = {
                matchedAssets: {
                    types: [
                        assetType || ""
                    ]
                }
            };
        }
        else {
            this.action.target.matchedAssets = undefined;
            this.action.target = {
                assets: {
                    ids: [
                        assetId
                    ],
                    types: [
                        assetType || ""
                    ]
                }
            };
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    setActionAttributeName(name) {
        this.action.attributeName = name;
        this.action.value = undefined;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    setActionAttributeValue(value) {
        this.action.value = value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    loadAssets(type) {
        this.assetProvider(type).then(assets => {
            this._assets = assets;
        });
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleActionAttribute.prototype, "action", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrRuleActionAttribute.prototype, "targetTypeMap", void 0);
__decorate([
    property({ type: Object })
], OrRuleActionAttribute.prototype, "config", void 0);
__decorate([
    property({ type: Object })
], OrRuleActionAttribute.prototype, "assetInfos", void 0);
__decorate([
    property({ type: Object })
], OrRuleActionAttribute.prototype, "assetProvider", void 0);
__decorate([
    property({ type: Array, attribute: false })
], OrRuleActionAttribute.prototype, "_assets", void 0);
OrRuleActionAttribute = __decorate([
    customElement("or-rule-action-attribute")
], OrRuleActionAttribute);
export { OrRuleActionAttribute };
//# sourceMappingURL=or-rule-action-attribute.js.map