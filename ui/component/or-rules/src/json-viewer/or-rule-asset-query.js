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
import { AssetQueryOperator, getAssetIdsFromQuery, getAssetTypeFromQuery } from "../index";
import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-attribute-input";
import { Util } from "@openremote/core";
import i18next from "i18next";
import { buttonStyle } from "../style";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import { translate } from "@openremote/or-translate";
import "./modals/or-rule-radial-modal";
import { ifDefined } from "lit/directives/if-defined.js";
import { when } from 'lit/directives/when.js';
// language=CSS
const style = css `
    
    ${buttonStyle}
    
    :host {
        display: block;
    }
    
    .attribute-group {
        flex-grow: 1;
        display: flex;
        align-items: start;
        flex-direction: row;
        flex-wrap: wrap;
    }

    .min-width {
        min-width: 200px;
    }
    
    .attribute-group > * {
        margin: 10px 3px 6px 3px;
    }
    .attributes {
        flex: 1 1 min-content;
        display: flex;
        flex-direction: column;
        row-gap: 10px;
    }
    or-icon.small {
        --or-icon-width: 14px;
        --or-icon-height: 14px;
    }
    .attribute {
        display: flex;
        align-items: center;
    }
    .attribute > div {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        align-items: center;
        gap: 6px;
    }    
    .attribute > div > * {
        min-width: 200px;
    }
    .button-clear {
        margin-left: auto;
    }
    .attribute:hover .button-clear {
        visibility: visible;
    }
    
    .invalidLabel {
        display: flex;
        align-items: center;
        margin: 14px 3px auto 0;
        height: 48px; /* Same as the icon size */
    }
`;
let OrRuleAssetQuery = class OrRuleAssetQuery extends translate(i18next)(LitElement) {
    constructor() {
        super();
        // Value predicates for specific value descriptors
        this._queryOperatorsMap = {};
        this._queryOperatorsMap["GEO_JSONPoint" /* WellknownValueTypes.GEOJSONPOINT */] = [
            AssetQueryOperator.EQUALS,
            AssetQueryOperator.NOT_EQUALS,
            AssetQueryOperator.WITHIN_RADIUS,
            AssetQueryOperator.OUTSIDE_RADIUS,
            AssetQueryOperator.WITHIN_RECTANGLE,
            AssetQueryOperator.OUTSIDE_RECTANGLE,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY
        ];
        this._queryOperatorsMap["string"] = [
            AssetQueryOperator.EQUALS,
            AssetQueryOperator.NOT_EQUALS,
            AssetQueryOperator.CONTAINS,
            AssetQueryOperator.NOT_CONTAINS,
            AssetQueryOperator.STARTS_WITH,
            AssetQueryOperator.NOT_STARTS_WITH,
            AssetQueryOperator.ENDS_WITH,
            AssetQueryOperator.NOT_ENDS_WITH,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY
        ];
        this._queryOperatorsMap["number"] = [
            AssetQueryOperator.EQUALS,
            AssetQueryOperator.NOT_EQUALS,
            AssetQueryOperator.GREATER_THAN,
            AssetQueryOperator.GREATER_EQUALS,
            AssetQueryOperator.LESS_THAN,
            AssetQueryOperator.LESS_EQUALS,
            AssetQueryOperator.BETWEEN,
            AssetQueryOperator.NOT_BETWEEN,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY
        ];
        this._queryOperatorsMap["boolean"] = [
            AssetQueryOperator.IS_TRUE,
            AssetQueryOperator.IS_FALSE,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY
        ];
        this._queryOperatorsMap["array"] = [
            AssetQueryOperator.CONTAINS,
            AssetQueryOperator.NOT_CONTAINS,
            AssetQueryOperator.INDEX_CONTAINS,
            AssetQueryOperator.NOT_INDEX_CONTAINS,
            AssetQueryOperator.LENGTH_EQUALS,
            AssetQueryOperator.NOT_LENGTH_EQUALS,
            AssetQueryOperator.LENGTH_LESS_THAN,
            AssetQueryOperator.LENGTH_GREATER_THAN,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY
        ];
        this._queryOperatorsMap["object"] = [
            AssetQueryOperator.CONTAINS_KEY,
            AssetQueryOperator.NOT_CONTAINS_KEY,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY
        ];
    }
    refresh() {
        // Clear assets
        this._assets = undefined;
    }
    attributePredicateEditorTemplate(assetTypeInfo, asset, attributePredicate) {
        const assetDescriptor = assetTypeInfo.assetDescriptor;
        const operator = this.getOperator(attributePredicate);
        const attributeName = this.getAttributeName(attributePredicate);
        let attribute = asset && asset.attributes && attributeName ? asset.attributes[attributeName] : undefined;
        let attributes = [];
        const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset ? asset.type : assetDescriptor.name, attribute || attributeName, attribute);
        if (asset && asset.attributes) {
            attributes = Object.values(asset.attributes)
                .filter((attribute) => attribute.meta && (attribute.meta.hasOwnProperty("ruleState" /* WellknownMetaItems.RULESTATE */) ? attribute.meta["ruleState" /* WellknownMetaItems.RULESTATE */] : attribute.meta.hasOwnProperty("agentLink" /* WellknownMetaItems.AGENTLINK */)))
                .map((attr) => {
                const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attr.name, attr);
                const label = Util.getAttributeLabel(attr, descriptors[0], asset.type, false);
                return [attr.name, label];
            });
        }
        else {
            attributes = !assetTypeInfo || !assetTypeInfo.attributeDescriptors ? [] :
                assetTypeInfo.attributeDescriptors
                    .map((ad) => {
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(assetDescriptor.name, ad);
                    const label = Util.getAttributeLabel(undefined, descriptors ? descriptors[0] : undefined, assetDescriptor.name, false);
                    return [ad.name, label];
                });
        }
        attributes.sort(Util.sortByString((attr) => attr[1]));
        const operators = attributeName ? this.getOperators(assetDescriptor, descriptors ? descriptors[0] : undefined, descriptors ? descriptors[1] : undefined, attribute, attributeName) : [];
        return html `
            <or-mwc-input type="${InputType.SELECT}" @or-mwc-input-changed="${(e) => this.setAttributeName(attributePredicate, e.detail.value)}" .readonly="${this.readonly || false}" .options="${attributes}" .value="${attributeName}" .label="${i18next.t("attribute")}"></or-mwc-input>
            ${attributeName ? html `<or-mwc-input type="${InputType.SELECT}" @or-mwc-input-changed="${(e) => this.setOperator(assetDescriptor, attribute, attributeName, attributePredicate, e.detail.value)}" .readonly="${this.readonly || false}" .options="${operators}" .value="${operator}" .label="${i18next.t("operator")}"></or-mwc-input>` : ``}
            ${attributePredicate ? this.attributePredicateValueEditorTemplate(assetDescriptor, asset, attributePredicate) : ``}
        `;
    }
    attributePredicateValueEditorTemplate(assetDescriptor, asset, attributePredicate) {
        var _a, _b, _c, _d, _e, _f, _g;
        const valuePredicate = attributePredicate.value;
        if (!assetDescriptor || !valuePredicate) {
            return ``;
        }
        const attributeName = this.getAttributeName(attributePredicate);
        const assetType = getAssetTypeFromQuery(this.query);
        const attribute = asset && asset.attributes && attributeName ? asset.attributes[attributeName] : undefined;
        const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(assetType, attributeName, attribute);
        // @ts-ignore
        const value = valuePredicate ? valuePredicate.value : undefined;
        switch (valuePredicate.predicateType) {
            case "string":
                return html `<or-attribute-input @or-attribute-input-changed="${(ev) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${(_a = this.config) === null || _a === void 0 ? void 0 : _a.inputProvider}" .label="${i18next.t("value")}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>`;
            case "boolean":
                return html ``; // Handled by the operator IS_TRUE or IS_FALSE
            case "datetime":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "number":
                if (valuePredicate.operator === "BETWEEN" /* AQO.BETWEEN */) {
                    return html `
                        <or-attribute-input .inputType="${InputType.NUMBER}" @or-attribute-input-changed="${(ev) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${(_b = this.config) === null || _b === void 0 ? void 0 : _b.inputProvider}" .label="${i18next.t("between")}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>
                        <span style="display: inline-flex; align-items: center;">&</span>
                        <or-attribute-input .inputType="${InputType.NUMBER}" @or-attribute-input-changed="${(ev) => this.setValuePredicateProperty(valuePredicate, "rangeValue", ev.detail.value)}" .customProvider="${(_c = this.config) === null || _c === void 0 ? void 0 : _c.inputProvider}" .label="${i18next.t("and")}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${valuePredicate.rangeValue}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>
                    `;
                }
                let inputType;
                if ((_e = (_d = descriptors[0]) === null || _d === void 0 ? void 0 : _d.format) === null || _e === void 0 ? void 0 : _e.asSlider)
                    inputType = InputType.NUMBER;
                return html `<or-attribute-input .inputType="${ifDefined(inputType)}" @or-attribute-input-changed="${(ev) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${(_f = this.config) === null || _f === void 0 ? void 0 : _f.inputProvider}" .label="" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>`;
            case "radial":
                return html `<or-rule-radial-modal .query="${this.query}" .assetDescriptor="${assetDescriptor}" .attributePredicate="${attributePredicate}"></or-rule-radial-modal>`;
            case "rect":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "value-empty":
                return ``;
            case "array":
                // TODO: Update once we can determine inner type of array
                // Assume string array
                return html `<or-attribute-input @or-attribute-input-changed="${(ev) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${(_g = this.config) === null || _g === void 0 ? void 0 : _g.inputProvider}" .label="" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}></or-attribute-input>`;
            default:
                return html `<span>NOT IMPLEMENTED</span>`;
        }
    }
    static get styles() {
        return style;
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("condition")) {
            this._assets = undefined;
        }
        return super.shouldUpdate(_changedProperties);
    }
    get query() {
        return this.condition.assets;
    }
    render() {
        const assetType = getAssetTypeFromQuery(this.query);
        if (!assetType) {
            return html `<span class="invalidLabel">${i18next.t('errorOccurred')}</span>`;
        }
        const assetTypeInfo = this.assetInfos ? this.assetInfos.find((assetTypeInfo) => assetTypeInfo.assetDescriptor.name === assetType) : undefined;
        if (!assetTypeInfo) {
            return html `<span class="invalidLabel">${i18next.t('errorOccurred')}</span>`;
        }
        if (!this._assets) {
            this.loadAssets(assetType);
        }
        if (!this.query.attributes) {
            this.query.attributes = {};
        }
        if (!this.query.attributes.items || this.query.attributes.items.length === 0) {
            this.query.attributes.items = [{}];
        }
        const showRemoveAttribute = !this.readonly && this.query.attributes && this.query.attributes.items && this.query.attributes.items.length > 1;
        // TODO: Add multiselect support
        const ids = getAssetIdsFromQuery(this.query);
        const idValue = ids && ids.length > 0 ? ids[0] : "*";
        const idOptions = [
            ["*", i18next.t("anyOfThisType")]
        ];
        let searchProvider;
        return html `
            <div class="attribute-group">
            
                <!-- Show SELECT input with 'loading' until the assets are retrieved -->
                ${when((!this._assets), () => html `
                    <or-mwc-input id="idSelect" class="min-width" type="${InputType.SELECT}" .readonly="${true}" .label="${i18next.t('loading')}"></or-mwc-input>
                `, () => {
            // Set list of displayed assets, and filtering assets out if needed.
            // If <= 25 assets: display everything
            // If between 25 and 100 assets: display everything with search functionality 
            // If > 100 assets: only display if in line with search input
            if (this._assets.length <= 25) {
                idOptions.push(...this._assets.map((asset) => [asset.id, asset.name]));
            }
            else {
                searchProvider = (search) => __awaiter(this, void 0, void 0, function* () {
                    var _a;
                    if (search) {
                        return this._assets.filter((asset) => { var _a; return (_a = asset.name) === null || _a === void 0 ? void 0 : _a.toLowerCase().includes(search.toLowerCase()); }).map((asset) => [asset.id, asset.name]);
                    }
                    else if (this._assets.length <= 100) {
                        idOptions.push(...this._assets.map((asset) => [asset.id, asset.name]));
                        return idOptions;
                    }
                    else {
                        const asset = (_a = this._assets) === null || _a === void 0 ? void 0 : _a.find((asset) => asset.id == idValue);
                        if (asset && idOptions.find(([id, _value]) => id == asset.id) == undefined) {
                            idOptions.push([asset.id, asset.name]); // add selected asset if there is one.
                        }
                        return idOptions;
                    }
                });
            }
            const showAddAttribute = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddAttribute !== true);
            return html `
                        <or-mwc-input id="idSelect" class="min-width filledSelect" type="${InputType.SELECT}" .readonly="${this.readonly || false}" .label="${i18next.t("asset")}" 
                                      .options="${idOptions}" .value="${idValue}" .searchProvider="${searchProvider}"
                                      @or-mwc-input-changed="${(e) => { this._assetId = (e.detail.value); this.refresh(); }}"
                        ></or-mwc-input>
                        <div class="attributes">
                            ${this.query.attributes && this.query.attributes.items ? this.query.attributes.items.map((attributePredicate, index) => {
                return html `
                                    ${index > 0 ? html `<or-icon class="small" icon="ampersand"></or-icon>` : ``}
                                    <div class="attribute">
                                        <div>
                                    ${this.attributePredicateEditorTemplate(assetTypeInfo, idValue !== "*" ? this._assets.find((asset) => asset.id === idValue) : undefined, attributePredicate)}
                                        </div>
                                    ${showRemoveAttribute ? html `
                                        <button class="button-clear" @click="${() => this.removeAttributePredicate(this.query.attributes, attributePredicate)}"><or-icon icon="close-circle"></or-icon></input>
                                    </div>` : ``}
                                `;
            }) : ``}
                            ${showAddAttribute ? html `
                                <or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"
                                              label="rulesEditorAddAttribute" @or-mwc-input-changed="${(ev) => this.addAttributePredicate(this.query.attributes)}"></or-mwc-input>
                            ` : ``}
                        </div>
                    `;
        })}
            </div>
        `;
    }
    set _assetId(assetId) {
        !assetId || assetId === "*" ? this.query.ids = undefined : this.query.ids = [assetId];
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    getAttributeName(attributePredicate) {
        return attributePredicate && attributePredicate.name ? attributePredicate.name.value : undefined;
    }
    setAttributeName(attributePredicate, attributeName) {
        if (!attributePredicate.name) {
            attributePredicate.name = {
                predicateType: "string"
            };
        }
        attributePredicate.name.match = "EXACT" /* AssetQueryMatch.EXACT */;
        attributePredicate.name.value = attributeName;
        attributePredicate.value = undefined;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    getOperatorMapValue(operatorMap, assetType, attributeName, attributeDescriptor, valueDescriptor) {
        let assetAttributeMatch;
        let attributeDescriptorMatch;
        let attributeValueDescriptorMatch;
        if (assetType && attributeName) {
            if (operatorMap[assetType + ":" + attributeName]) {
                return operatorMap[assetType + ":" + attributeName];
            }
        }
        if (valueDescriptor) {
            if (operatorMap[valueDescriptor.name]) {
                return operatorMap[valueDescriptor.name];
            }
            if (operatorMap[valueDescriptor.jsonType]) {
                return operatorMap[valueDescriptor.jsonType];
            }
            if (valueDescriptor.arrayDimensions) {
                return operatorMap["array"];
            }
        }
    }
    getOperators(assetDescriptor, attributeDescriptor, valueDescriptor, attribute, attributeName) {
        let operators;
        if (this.config && this.config.controls && this.config.controls.allowedAssetQueryOperators) {
            operators = this.getOperatorMapValue(this.config.controls.allowedAssetQueryOperators, getAssetTypeFromQuery(this.query), attributeName, attributeDescriptor, valueDescriptor);
        }
        if (!operators) {
            operators = this.getOperatorMapValue(this._queryOperatorsMap, getAssetTypeFromQuery(this.query), attributeName, attributeDescriptor, valueDescriptor);
        }
        return operators ? operators.map((v) => [v, i18next.t(v)]) : [];
    }
    getOperator(attributePredicate) {
        if (!attributePredicate || !attributePredicate.value) {
            return;
        }
        const valuePredicate = attributePredicate.value;
        switch (valuePredicate.predicateType) {
            case "string":
                switch (valuePredicate.match) {
                    case "EXACT" /* AssetQueryMatch.EXACT */:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_EQUALS : AssetQueryOperator.EQUALS;
                    case "BEGIN" /* AssetQueryMatch.BEGIN */:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_STARTS_WITH : AssetQueryOperator.STARTS_WITH;
                    case "END" /* AssetQueryMatch.END */:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_ENDS_WITH : AssetQueryOperator.ENDS_WITH;
                    case "CONTAINS" /* AssetQueryMatch.CONTAINS */:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_CONTAINS : AssetQueryOperator.CONTAINS;
                }
                return;
            case "boolean":
                return valuePredicate.value ? AssetQueryOperator.IS_TRUE : AssetQueryOperator.IS_FALSE;
            case "datetime":
            case "number":
                switch (valuePredicate.operator) {
                    case "EQUALS" /* AQO.EQUALS */:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_EQUALS : AssetQueryOperator.EQUALS;
                    case "GREATER_THAN" /* AQO.GREATER_THAN */:
                        return valuePredicate.negate ? AssetQueryOperator.LESS_EQUALS : AssetQueryOperator.GREATER_THAN;
                    case "GREATER_EQUALS" /* AQO.GREATER_EQUALS */:
                        return valuePredicate.negate ? AssetQueryOperator.LESS_THAN : AssetQueryOperator.GREATER_EQUALS;
                    case "LESS_THAN" /* AQO.LESS_THAN */:
                        return valuePredicate.negate ? AssetQueryOperator.GREATER_EQUALS : AssetQueryOperator.LESS_THAN;
                    case "LESS_EQUALS" /* AQO.LESS_EQUALS */:
                        return valuePredicate.negate ? AssetQueryOperator.GREATER_THAN : AssetQueryOperator.LESS_EQUALS;
                    case "BETWEEN" /* AQO.BETWEEN */:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_BETWEEN : AssetQueryOperator.BETWEEN;
                }
                return;
            case "radial":
                return valuePredicate.negated ? AssetQueryOperator.OUTSIDE_RADIUS : AssetQueryOperator.WITHIN_RADIUS;
            case "rect":
                return valuePredicate.negated ? AssetQueryOperator.OUTSIDE_RECTANGLE : AssetQueryOperator.WITHIN_RECTANGLE;
            case "array":
                if (valuePredicate.value && valuePredicate.index) {
                    return valuePredicate.negated ? AssetQueryOperator.NOT_INDEX_CONTAINS : AssetQueryOperator.INDEX_CONTAINS;
                }
                if (valuePredicate.lengthEquals) {
                    return valuePredicate.negated ? AssetQueryOperator.NOT_LENGTH_EQUALS : AssetQueryOperator.LENGTH_EQUALS;
                }
                if (valuePredicate.lengthGreaterThan) {
                    return valuePredicate.negated ? AssetQueryOperator.LENGTH_LESS_THAN : AssetQueryOperator.LENGTH_GREATER_THAN;
                }
                if (valuePredicate.lengthLessThan) {
                    return valuePredicate.negated ? AssetQueryOperator.LENGTH_GREATER_THAN : AssetQueryOperator.LENGTH_LESS_THAN;
                }
                return valuePredicate.negated ? AssetQueryOperator.NOT_CONTAINS : AssetQueryOperator.CONTAINS;
            case "value-empty":
                return valuePredicate.negate ? AssetQueryOperator.VALUE_NOT_EMPTY : AssetQueryOperator.VALUE_EMPTY;
        }
    }
    setOperator(assetDescriptor, attribute, attributeName, attributePredicate, operator) {
        if (!this.query
            || !this.query.attributes
            || !this.query.attributes.items
            || this.query.attributes.items.length === 0) {
            return;
        }
        if (!operator) {
            attributePredicate.value = undefined;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
            return;
        }
        const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(assetDescriptor.name, attribute || attributeName);
        const value = operator;
        if (!descriptors || !descriptors[1] || !value) {
            attributePredicate.value = undefined;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
            return;
        }
        const valueDescriptor = descriptors[1];
        let predicate;
        switch (value) {
            // // object
            // case AssetQueryOperator.NOT_CONTAINS_KEY:
            // case AssetQueryOperator.CONTAINS_KEY:
            //     if (valueType === ValueType.OBJECT) {
            //         predicate = {
            //             predicateType: "object-value-key",
            //             negated: value === AssetQueryOperator.NOT_CONTAINS_KEY
            //         };
            //     }
            //     break;
            // array
            case AssetQueryOperator.INDEX_CONTAINS:
            case AssetQueryOperator.NOT_INDEX_CONTAINS:
            case AssetQueryOperator.LENGTH_EQUALS:
            case AssetQueryOperator.NOT_LENGTH_EQUALS:
            case AssetQueryOperator.LENGTH_LESS_THAN:
            case AssetQueryOperator.LENGTH_GREATER_THAN:
                if (valueDescriptor.arrayDimensions) {
                    predicate = {
                        predicateType: "array",
                        negated: value === AssetQueryOperator.NOT_INDEX_CONTAINS || value === AssetQueryOperator.NOT_LENGTH_EQUALS,
                        index: value === AssetQueryOperator.INDEX_CONTAINS || value === AssetQueryOperator.NOT_INDEX_CONTAINS ? 0 : undefined,
                        lengthEquals: value === AssetQueryOperator.LENGTH_EQUALS || value === AssetQueryOperator.NOT_LENGTH_EQUALS ? 0 : undefined,
                        lengthGreaterThan: value === AssetQueryOperator.LENGTH_GREATER_THAN ? 0 : undefined,
                        lengthLessThan: value === AssetQueryOperator.LENGTH_GREATER_THAN ? 0 : undefined,
                    };
                }
                break;
            // geo point
            case AssetQueryOperator.WITHIN_RADIUS:
            case AssetQueryOperator.OUTSIDE_RADIUS:
                predicate = {
                    predicateType: "radial",
                    negated: value === AssetQueryOperator.OUTSIDE_RADIUS,
                    lat: 0,
                    lng: 0,
                    radius: 50
                };
                break;
            case AssetQueryOperator.WITHIN_RECTANGLE:
            case AssetQueryOperator.OUTSIDE_RECTANGLE:
                predicate = {
                    predicateType: "rect",
                    negated: value === AssetQueryOperator.OUTSIDE_RECTANGLE,
                    latMin: -0.1,
                    lngMin: -0.1,
                    latMax: 0.1,
                    lngMax: 0.1
                };
                break;
            // boolean
            case AssetQueryOperator.IS_TRUE:
            case AssetQueryOperator.IS_FALSE:
                if (valueDescriptor.jsonType === "boolean") {
                    predicate = {
                        predicateType: "boolean",
                        value: value === AssetQueryOperator.IS_TRUE
                    };
                    break;
                }
            // string
            case AssetQueryOperator.STARTS_WITH:
            case AssetQueryOperator.NOT_STARTS_WITH:
                if (valueDescriptor.jsonType === "string") {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_STARTS_WITH,
                        match: "BEGIN" /* AssetQueryMatch.BEGIN */
                    };
                }
                break;
            case AssetQueryOperator.ENDS_WITH:
            case AssetQueryOperator.NOT_ENDS_WITH:
                if (valueDescriptor.jsonType === "string") {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_ENDS_WITH,
                        match: "END" /* AssetQueryMatch.END */
                    };
                }
                break;
            // number or datetime
            case AssetQueryOperator.NOT_BETWEEN:
                if (valueDescriptor.jsonType === "number" || valueDescriptor.jsonType === "bigint") {
                    predicate = {
                        predicateType: "number",
                        operator: "BETWEEN" /* AQO.BETWEEN */,
                        negate: true
                    };
                }
                else {
                    // Assume datetime
                    predicate = {
                        predicateType: "datetime",
                        operator: "BETWEEN" /* AQO.BETWEEN */,
                        negate: true
                    };
                }
                break;
            case AssetQueryOperator.GREATER_THAN:
            case AssetQueryOperator.GREATER_EQUALS:
            case AssetQueryOperator.LESS_THAN:
            case AssetQueryOperator.LESS_EQUALS:
            case AssetQueryOperator.BETWEEN:
                const key = Util.getEnumKeyAsString(AssetQueryOperator, value);
                if (valueDescriptor.jsonType === "number") {
                    predicate = {
                        predicateType: "number",
                        operator: key
                    };
                }
                else {
                    // Assume datetime
                    predicate = {
                        predicateType: "datetime",
                        operator: key
                    };
                }
                break;
            // multiple
            case AssetQueryOperator.EQUALS:
            case AssetQueryOperator.NOT_EQUALS:
                if (valueDescriptor.jsonType === "date") {
                    predicate = {
                        predicateType: "datetime",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        operator: "EQUALS" /* AQO.EQUALS */
                    };
                }
                else if (valueDescriptor.jsonType === "number") {
                    predicate = {
                        predicateType: "number",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        operator: "EQUALS" /* AQO.EQUALS */
                    };
                }
                else if (valueDescriptor.jsonType === "string") {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        match: "EXACT" /* AssetQueryMatch.EXACT */
                    };
                }
                break;
            case AssetQueryOperator.VALUE_EMPTY:
                predicate = {
                    predicateType: "value-empty"
                };
                break;
            case AssetQueryOperator.VALUE_NOT_EMPTY:
                predicate = {
                    predicateType: "value-empty",
                    negate: true
                };
                break;
            case AssetQueryOperator.CONTAINS:
            case AssetQueryOperator.NOT_CONTAINS:
                if (valueDescriptor.arrayDimensions) {
                    predicate = {
                        predicateType: "array",
                        negated: value === AssetQueryOperator.NOT_CONTAINS
                    };
                }
                else if (valueDescriptor.jsonType === "string") {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_CONTAINS,
                        match: "CONTAINS" /* AssetQueryMatch.CONTAINS */
                    };
                }
        }
        attributePredicate.value = predicate;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    get attributePredicate() {
        return this.query
            && this.query.attributes
            && this.query.attributes.items
            && this.query.attributes.items.length > 0
            ? this.query.attributes.items[0] : undefined;
    }
    setValuePredicateProperty(valuePredicate, propertyName, value) {
        if (!valuePredicate) {
            return;
        }
        valuePredicate[propertyName] = value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    removeAttributePredicate(group, attributePredicate) {
        const index = group.items.indexOf(attributePredicate);
        if (index >= 0) {
            group.items.splice(index, 1);
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    addAttributePredicate(group) {
        if (!group.items) {
            group.items = [];
        }
        group.items.push({});
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    toggleAttributeGroup(group) {
        if (group.operator === "OR" /* LogicGroupOperator.OR */) {
            group.operator = "AND" /* LogicGroupOperator.AND */;
        }
        else {
            group.operator = "OR" /* LogicGroupOperator.OR */;
        }
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
], OrRuleAssetQuery.prototype, "condition", void 0);
__decorate([
    property({ type: Object })
], OrRuleAssetQuery.prototype, "config", void 0);
__decorate([
    property({ type: Object })
], OrRuleAssetQuery.prototype, "assetInfos", void 0);
__decorate([
    property({ type: Object })
], OrRuleAssetQuery.prototype, "assetProvider", void 0);
__decorate([
    state()
], OrRuleAssetQuery.prototype, "_assets", void 0);
OrRuleAssetQuery = __decorate([
    customElement("or-rule-asset-query")
], OrRuleAssetQuery);
export { OrRuleAssetQuery };
//# sourceMappingURL=or-rule-asset-query.js.map