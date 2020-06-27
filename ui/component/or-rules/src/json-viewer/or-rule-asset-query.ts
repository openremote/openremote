import {css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import {
    Asset,
    AssetDescriptor,
    AssetQueryMatch,
    AssetQueryOperator as AQO,
    AssetQueryOrderBy$Property,
    Attribute,
    AttributeDescriptor,
    AttributePredicate,
    AttributeValueDescriptor,
    AttributeValueType,
    LogicGroup,
    LogicGroupOperator,
    MetaItemType,
    RuleCondition,
    ValuePredicateUnion,
    ValueType
} from "@openremote/model";
import {
    AssetQueryOperator,
    AssetTypeAttributeName,
    getAssetIdsFromQuery,
    getAssetTypeFromQuery,
    RulesConfig
} from "../index";
import {OrSelectChangedEvent} from "@openremote/or-select";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import "@openremote/or-attribute-input";
import manager, {AssetModelUtil, Util} from "@openremote/core";
import i18next from "i18next";
import {buttonStyle} from "../style";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {translate} from "@openremote/or-translate";
import { OrAttributeInputChangedEvent } from "@openremote/or-attribute-input";

// language=CSS
const style = css`
    
    ${buttonStyle}
    
    :host {
        display: block;
    }
    
    .attribute-group {
        flex-grow: 1;
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
    }
    
    .attribute-group > * {
        margin: 0 3px 6px;
    }
`;

@customElement("or-rule-asset-query")
export class OrRuleAssetQuery extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public condition!: RuleCondition;

    public readonly?: boolean;

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Object})
    public assetDescriptors?: AssetDescriptor[];

    @property({type: Array, attribute: false})
    protected _assets?: Asset[];

    // Value predicates for specific attribute value descriptors
    protected _queryOperatorsMap: Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]> = new Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]>([
        [AttributeValueType.GEO_JSON_POINT, [
            AssetQueryOperator.EQUALS,
            AssetQueryOperator.NOT_EQUALS,
            AssetQueryOperator.WITHIN_RADIUS,
            AssetQueryOperator.OUTSIDE_RADIUS,
            AssetQueryOperator.WITHIN_RECTANGLE,
            AssetQueryOperator.OUTSIDE_RECTANGLE,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY]
        ],
        [ValueType.STRING, [
            AssetQueryOperator.EQUALS,
            AssetQueryOperator.NOT_EQUALS,
            AssetQueryOperator.CONTAINS,
            AssetQueryOperator.NOT_CONTAINS,
            AssetQueryOperator.STARTS_WITH,
            AssetQueryOperator.NOT_STARTS_WITH,
            AssetQueryOperator.ENDS_WITH,
            AssetQueryOperator.NOT_ENDS_WITH,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY]
        ],
        [ValueType.NUMBER, [
            AssetQueryOperator.EQUALS,
            AssetQueryOperator.NOT_EQUALS,
            AssetQueryOperator.GREATER_THAN,
            AssetQueryOperator.GREATER_EQUALS,
            AssetQueryOperator.LESS_THAN,
            AssetQueryOperator.LESS_EQUALS,
            AssetQueryOperator.BETWEEN,
            AssetQueryOperator.NOT_BETWEEN,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY]
        ],
        [ValueType.BOOLEAN, [
            AssetQueryOperator.IS_TRUE,
            AssetQueryOperator.IS_FALSE,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY]
        ],
        [ValueType.ARRAY, [
            AssetQueryOperator.CONTAINS,
            AssetQueryOperator.NOT_CONTAINS,
            AssetQueryOperator.INDEX_CONTAINS,
            AssetQueryOperator.NOT_INDEX_CONTAINS,
            AssetQueryOperator.LENGTH_EQUALS,
            AssetQueryOperator.NOT_LENGTH_EQUALS,
            AssetQueryOperator.LENGTH_LESS_THAN,
            AssetQueryOperator.LENGTH_GREATER_THAN,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY]
        ],
        [ValueType.OBJECT, [
            AssetQueryOperator.CONTAINS_KEY,
            AssetQueryOperator.NOT_CONTAINS_KEY,
            AssetQueryOperator.VALUE_EMPTY,
            AssetQueryOperator.VALUE_NOT_EMPTY]
        ]
    ]);

    public refresh() {
        // Clear assets
        this._assets = undefined;
    }

    protected attributePredicateEditorTemplate(assetDescriptor: AssetDescriptor, asset: Asset | undefined, attributePredicate: AttributePredicate) {

        const operator = this.getOperator(attributePredicate);
        const attributeName = this.getAttributeName(attributePredicate);
        let attribute: Attribute | undefined;
        let attributes: [string, string][];

        if (asset) {
            attribute = attributeName ? Util.getAssetAttribute(asset, attributeName) : undefined;
            attributes = Util.getAssetAttributes(asset)
                .filter((attr) => Util.hasMetaItem(attr, MetaItemType.RULE_STATE.urn!))
                .map((attr) => {
                    const attrDescriptor = AssetModelUtil.getAssetAttributeDescriptor(assetDescriptor, attr.name);
                    return [attr.name!, Util.getAttributeLabel(attr, attrDescriptor)];
                });
        } else {
            attributes = !assetDescriptor || !assetDescriptor.attributeDescriptors ? []
                : assetDescriptor.attributeDescriptors
                    .map((ad) => [ad.attributeName!, Util.getAttributeLabel(undefined, ad)]);
        }

        const operators = this.getOperators(assetDescriptor, attribute, attributeName);

        return html`
            <or-input type="${InputType.SELECT}" @or-input-changed="${(e: OrSelectChangedEvent) => this.setAttributeName(attributePredicate, e.detail.value)}" .readonly="${this.readonly || false}" .options="${attributes}" .value="${attributeName}" .label="${i18next.t("attribute")}"></or-input>
            
            ${attributeName ? html`<or-input type="${InputType.SELECT}" @or-input-changed="${(e: OrSelectChangedEvent) => this.setOperator(assetDescriptor, attribute, attributeName, attributePredicate, e.detail.value)}" .readonly="${this.readonly || false}" .options="${operators}" .value="${operator}" .label="${i18next.t("operator")}"></or-input>` : ``}
            
            ${attributePredicate ? this.attributePredicateValueEditorTemplate(assetDescriptor, attributePredicate) : ``}
        `;
    }

    protected attributePredicateValueEditorTemplate(assetDescriptor: AssetDescriptor, attributePredicate: AttributePredicate) {

        const valuePredicate = attributePredicate.value;

        if (!assetDescriptor || !valuePredicate) {
            return ``;
        }

        const attributeName = this.getAttributeName(attributePredicate);
        const assetType = getAssetTypeFromQuery(this.query);
        const attributeDescriptor = AssetModelUtil.getAssetAttributeDescriptor(assetDescriptor, attributeName);
        const attributeValueDescriptor = attributeDescriptor && attributeDescriptor.valueDescriptor ? typeof attributeDescriptor.valueDescriptor === "string" ? AssetModelUtil.getAttributeValueDescriptor(attributeDescriptor.valueDescriptor as string) : attributeDescriptor.valueDescriptor : undefined;

        // @ts-ignore
        const value = valuePredicate ? valuePredicate.value : undefined;

        switch (valuePredicate.predicateType) {
            case "string":
                return html`<or-attribute-input @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("string")}" .assetType="${assetType}" .attributeDescriptor="${attributeDescriptor}" .attributeValueDescriptor="${attributeValueDescriptor}" .value="${value}" .readonly="${this.readonly || false}"></or-attribute-input>`;
            case "boolean":
                return html ``; // Handled by the operator IS_TRUE or IS_FALSE
            case "datetime":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "number":
                if (valuePredicate.operator === AQO.BETWEEN) {
                    return html`
                        <or-attribute-input .inputType="${InputType.NUMBER}" @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("between")}" .assetType="${assetType}" .attributeDescriptor="${attributeDescriptor}" .attributeValueDescriptor="${attributeValueDescriptor}" .value="${value}" .readonly="${this.readonly || false}"></or-attribute-input>
                        <span style="display: inline-flex; align-items: center;">&</span>
                        <or-attribute-input .inputType="${InputType.NUMBER}" @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "rangeValue", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("and")}" .assetType="${assetType}" .attributeDescriptor="${attributeDescriptor}" .attributeValueDescriptor="${attributeValueDescriptor}" .value="${valuePredicate.rangeValue}" .readonly="${this.readonly || false}"></or-attribute-input>
                    `;
                }
                return html`<or-attribute-input @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("number")}" .assetType="${assetType}" .attributeDescriptor="${attributeDescriptor}" .attributeValueDescriptor="${attributeValueDescriptor}" .value="${value}" .readonly="${this.readonly || false}"></or-attribute-input>`;
            case "string-array":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "radial":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "rect":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "object-value-key":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "value-empty":
                return ``;
            case "value-not-empty":
                return ``;
            case "array":
                // TODO: Update once we can determine inner type of array
                // Assume string array
                return html`<or-attribute-input @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("array")}" .assetType="${assetType}" .attributeDescriptor="${attributeDescriptor}" .attributeValueDescriptor="${attributeValueDescriptor}" .value="${value}" .readonly="${this.readonly || false}"></or-attribute-input>`;
            default:
                return html `<span>NOT IMPLEMENTED</span>`;
        }
    }

    static get styles() {
        return style;
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {

        if (_changedProperties.has("condition")) {
            this._assets = undefined;
        }

        return super.shouldUpdate(_changedProperties);
    }

    protected get query() {
        return this.condition.assets!;
    }

    protected render() {

        const assetType = getAssetTypeFromQuery(this.query);

        if (!assetType) {
            return html``;
        }

        const assetDescriptor = this.assetDescriptors ? this.assetDescriptors.find((ad) => ad.type === assetType) : undefined;

        if (!assetDescriptor) {
            return html``;
        }

        if (!this._assets) {
            this.loadAssets(assetType);
            return html``;
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
        const idOptions: [string, string] [] = [
            ["*", i18next.t("anyOfThisType")]
        ];

        idOptions.push(...this._assets!.map((asset) => [asset.id!, asset.name!] as [string, string]));
        return html`
            <div class="attribute-group">
            
                <or-input id="idSelect" type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this._assetId = (e.detail.value)}" .readonly="${this.readonly || false}" .options="${idOptions}" .value="${idValue}" .label="${i18next.t("asset")}"></or-input>
            
                ${this.query.attributes && this.query.attributes.items ? this.query.attributes.items.map((attributePredicate) => {
                    
                    return html`
                        ${this.attributePredicateEditorTemplate(assetDescriptor, idValue !== "*" ? this._assets!.find((asset) => asset.id === idValue) : undefined, attributePredicate)}
                        ${showRemoveAttribute ? html`
                            <button class="button-clear" @click="${() => this.removeAttributePredicate(this.query!.attributes!, attributePredicate)}"><or-icon icon="close-circle"></or-icon></input>
                        ` : ``}
                    `;
                }) : ``}
            </div>
        `;
    }

    protected set _assetId(assetId: string | undefined) {
        !assetId || assetId === "*" ? this.query.ids = undefined : this.query.ids! = [assetId];
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected getAttributeName(attributePredicate: AttributePredicate): string | undefined {
        return attributePredicate && attributePredicate.name ? attributePredicate.name.value : undefined;
    }

    protected setAttributeName(attributePredicate: AttributePredicate, attributeName: string | undefined) {

        if (!attributePredicate!.name) {
            attributePredicate!.name = {
                predicateType: "string"
            };
        }

        attributePredicate!.name.match = AssetQueryMatch.EXACT;
        attributePredicate!.name.value = attributeName;
        attributePredicate!.value = undefined;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected getOperatorMapValue(operatorMap: Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]>, assetType?: string, attributeName?: string, attributeDescriptor?: AttributeDescriptor, valueDescriptor?: AttributeValueDescriptor, valueType?: ValueType) {

        let assetAttributeMatch: AssetQueryOperator[] | undefined;
        let attributeDescriptorMatch: AssetQueryOperator[] | undefined;
        let attributeValueDescriptorMatch: AssetQueryOperator[] | undefined;
        let valueTypeMatch: AssetQueryOperator[] | undefined;

        operatorMap.forEach((v, k) => {

            if ((k as AssetTypeAttributeName).assetType && assetType && attributeName) {
                if ((k as AssetTypeAttributeName).assetType === assetType && (k as AssetTypeAttributeName).attributeName === attributeName) {
                    assetAttributeMatch = v;
                }
            } else if ((k as AttributeDescriptor).attributeName && (k as AttributeDescriptor).valueDescriptor && attributeDescriptor && attributeDescriptor.valueDescriptor) {
                if ((k as AttributeDescriptor).attributeName === attributeDescriptor.attributeName && (k as AttributeDescriptor).valueDescriptor!.name === attributeDescriptor.valueDescriptor.name) {
                    attributeDescriptorMatch = v;
                }
            } else if ((k as AttributeValueDescriptor).valueType && valueDescriptor) {
                if ((k as AttributeValueDescriptor).name === valueDescriptor.name && (k as AttributeValueDescriptor).valueType === valueDescriptor.valueType) {
                    attributeValueDescriptorMatch = v;
                }
            } else if ((k as ValueType).length && valueType) {
                if ((k as ValueType) === valueType) {
                    valueTypeMatch = v;
                }
            }
        });

        if (assetAttributeMatch) {
            return assetAttributeMatch;
        }
        if (attributeDescriptorMatch) {
            return attributeDescriptorMatch;
        }
        if (attributeValueDescriptorMatch) {
            return attributeValueDescriptorMatch;
        }
        return valueTypeMatch;
    }
    
    protected getOperators(assetDescriptor: AssetDescriptor, attribute: Attribute | undefined, attributeName: string | undefined) {
        if (!attributeName) {
            return [];
        }

        const descriptor = assetDescriptor.attributeDescriptors ? assetDescriptor.attributeDescriptors.find((ad) => ad.attributeName === attributeName) : undefined;
        const valueDescriptor = attribute ? AssetModelUtil.getAttributeValueDescriptor(attribute.type as string) : descriptor ? descriptor.valueDescriptor : undefined;
        const valueType = valueDescriptor ? valueDescriptor.valueType : undefined;
        let operators: AssetQueryOperator[] | undefined;

        if (this.config && this.config.controls && this.config.controls.allowedAssetQueryOperators) {
            operators = this.getOperatorMapValue(this.config.controls.allowedAssetQueryOperators, getAssetTypeFromQuery(this.query), attributeName, descriptor, valueDescriptor, valueType);
        }

        if (!operators) {
            operators = this.getOperatorMapValue(this._queryOperatorsMap, getAssetTypeFromQuery(this.query), attributeName, descriptor, valueDescriptor, valueType);
        }

        return operators ? operators.map((v) => [v, i18next.t(v)]) : [];
    }

    protected getOperator(attributePredicate: AttributePredicate): string | undefined {

        if (!attributePredicate || !attributePredicate.value) {
            return;
        }

        const valuePredicate = attributePredicate.value;

        switch (valuePredicate.predicateType) {
            case "string":
                switch (valuePredicate.match) {
                    case AssetQueryMatch.EXACT:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_EQUALS : AssetQueryOperator.EQUALS;
                    case AssetQueryMatch.BEGIN:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_STARTS_WITH : AssetQueryOperator.STARTS_WITH;
                    case AssetQueryMatch.END:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_ENDS_WITH : AssetQueryOperator.ENDS_WITH;
                    case AssetQueryMatch.CONTAINS:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_CONTAINS : AssetQueryOperator.CONTAINS;
                }
                return;
            case "boolean":
                return valuePredicate.value ? AssetQueryOperator.IS_TRUE : AssetQueryOperator.IS_FALSE;
            case "string-array":
                return;
            case "datetime":
            case "number":
                switch (valuePredicate.operator) {
                    case AQO.EQUALS:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_EQUALS : AssetQueryOperator.EQUALS;
                    case AQO.GREATER_THAN:
                        return valuePredicate.negate ? AssetQueryOperator.LESS_EQUALS : AssetQueryOperator.GREATER_THAN;
                    case AQO.GREATER_EQUALS:
                        return valuePredicate.negate ? AssetQueryOperator.LESS_THAN : AssetQueryOperator.GREATER_EQUALS;
                    case AQO.LESS_THAN:
                        return valuePredicate.negate ? AssetQueryOperator.GREATER_EQUALS : AssetQueryOperator.LESS_THAN;
                    case AQO.LESS_EQUALS:
                        return valuePredicate.negate ? AssetQueryOperator.GREATER_THAN : AssetQueryOperator.LESS_EQUALS;
                    case AQO.BETWEEN:
                        return valuePredicate.negate ? AssetQueryOperator.NOT_BETWEEN : AssetQueryOperator.BETWEEN;
                }
                return;
            case "radial":
                return valuePredicate.negated ? AssetQueryOperator.OUTSIDE_RADIUS : AssetQueryOperator.WITHIN_RADIUS;
            case "rect":
                return valuePredicate.negated ? AssetQueryOperator.OUTSIDE_RECTANGLE : AssetQueryOperator.WITHIN_RECTANGLE;
            case "object-value-key":
                return valuePredicate.negated ? AssetQueryOperator.NOT_CONTAINS_KEY : AssetQueryOperator.CONTAINS_KEY;
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
                return AssetQueryOperator.VALUE_EMPTY;
            case "value-not-empty":
                return AssetQueryOperator.VALUE_NOT_EMPTY;
        }
    }

    protected setOperator(assetDescriptor: AssetDescriptor, attribute: Attribute | undefined, attributeName: string, attributePredicate: AttributePredicate, operator: string | undefined) {

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

        const descriptor = assetDescriptor.attributeDescriptors ? assetDescriptor.attributeDescriptors.find((ad) => ad.attributeName === attributeName) : undefined;
        const valueDescriptor = attribute ? AssetModelUtil.getAttributeValueDescriptor(attribute.type as string) : descriptor ? descriptor.valueDescriptor : undefined;
        const valueType = valueDescriptor ? valueDescriptor.valueType : undefined;
        const value = operator as AssetQueryOperator;

        if (!valueType || !value) {
            attributePredicate.value = undefined;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
            return;
        }

        let predicate: ValuePredicateUnion | undefined;

        switch (value) {

            // object
            case AssetQueryOperator.NOT_CONTAINS_KEY:
            case AssetQueryOperator.CONTAINS_KEY:
                if (valueType === ValueType.OBJECT) {
                    predicate = {
                        predicateType: "object-value-key",
                        negated: value === AssetQueryOperator.NOT_CONTAINS_KEY
                    };
                }
                break;

            // array
            case AssetQueryOperator.INDEX_CONTAINS:
            case AssetQueryOperator.NOT_INDEX_CONTAINS:
            case AssetQueryOperator.LENGTH_EQUALS:
            case AssetQueryOperator.NOT_LENGTH_EQUALS:
            case AssetQueryOperator.LENGTH_LESS_THAN:
            case AssetQueryOperator.LENGTH_GREATER_THAN:
                if (valueType === ValueType.ARRAY) {
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
                if (valueType === ValueType.BOOLEAN) {
                    predicate = {
                        predicateType: "boolean",
                        value: value === AssetQueryOperator.IS_TRUE
                    };
                    break;
                }

            // string
            case AssetQueryOperator.STARTS_WITH:
            case AssetQueryOperator.NOT_STARTS_WITH:
                if (valueType === ValueType.STRING) {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_STARTS_WITH,
                        match: AssetQueryMatch.BEGIN
                    };
                }
                break;
            case AssetQueryOperator.ENDS_WITH:
            case AssetQueryOperator.NOT_ENDS_WITH:
                if (valueType === ValueType.STRING) {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_ENDS_WITH,
                        match: AssetQueryMatch.END
                    };
                }
                break;

            // number or datetime
            case AssetQueryOperator.NOT_BETWEEN:
                if (valueType === ValueType.NUMBER) {
                    predicate = {
                        predicateType: "number",
                        operator: AQO.BETWEEN,
                        negate: true
                    };
                } else {
                    // Assume datetime
                    predicate = {
                        predicateType: "datetime",
                        operator: AQO.BETWEEN,
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

                if (valueType === ValueType.NUMBER) {
                    predicate = {
                        predicateType: "number",
                        operator: key as AQO
                    };
                } else {
                    // Assume datetime
                    predicate = {
                        predicateType: "datetime",
                        operator: key as AQO
                    };
                }
                break;

            // multiple
            case AssetQueryOperator.EQUALS:
            case AssetQueryOperator.NOT_EQUALS:
                if (descriptor && descriptor.valueDescriptor && descriptor.valueDescriptor.name === AttributeValueType.TIMESTAMP_ISO8601.name) {
                    predicate = {
                        predicateType: "datetime",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        operator: AQO.EQUALS
                    };
                } else if (valueType === ValueType.NUMBER) {
                    predicate = {
                        predicateType: "number",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        operator: AQO.EQUALS
                    };
                } else if (valueType === ValueType.STRING) {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        match: AssetQueryMatch.EXACT
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
                    predicateType: "value-not-empty"
                };
                break;
            case AssetQueryOperator.CONTAINS:
            case AssetQueryOperator.NOT_CONTAINS:
                if (valueType === ValueType.ARRAY) {
                    predicate = {
                        predicateType: "array",
                        negated: value === AssetQueryOperator.NOT_CONTAINS
                    };
                } else if (valueType === ValueType.STRING) {
                predicate = {
                    predicateType: "string",
                    negate: value === AssetQueryOperator.NOT_CONTAINS,
                    match: AssetQueryMatch.CONTAINS
                };
            }
        }

        attributePredicate.value = predicate;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected get attributePredicate(): AttributePredicate | undefined {
        return this.query
        && this.query.attributes
        && this.query.attributes.items
        && this.query.attributes.items.length > 0
            ? this.query.attributes.items[0] : undefined;
    }

    protected setValuePredicateProperty(valuePredicate: ValuePredicateUnion | undefined, propertyName: string, value: any) {
        if (!valuePredicate) {
            return;
        }

        (valuePredicate as any)[propertyName] = value;
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected removeAttributePredicate(group: LogicGroup<AttributePredicate>, attributePredicate: AttributePredicate) {
        const index = group.items!.indexOf(attributePredicate);
        if (index >= 0) {
            group.items!.splice(index, 1);
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected addAttributePredicate(group: LogicGroup<AttributePredicate>) {
        if (!group.items) {
            group.items = [];
        }
        group.items.push({});
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected toggleAttributeGroup(group: LogicGroup<AttributePredicate>) {
        if (group.operator === LogicGroupOperator.OR) {
            group.operator = LogicGroupOperator.AND;
        } else {
            group.operator = LogicGroupOperator.OR;
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected loadAssets(type: string) {
        manager.rest.api.AssetResource.queryAssets({
            types: [
                {
                    predicateType: "string",
                    value: type
                }
            ],
            select: {
                excludeAttributeTimestamp: true,
                excludeAttributeValue: true,
                excludeParentInfo: true,
                excludePath: true
            },
            orderBy: {
                property: AssetQueryOrderBy$Property.NAME
            }
        }).then((response) => this._assets = response.data);
    }
}
