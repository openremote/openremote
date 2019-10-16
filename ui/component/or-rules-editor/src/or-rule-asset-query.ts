import {customElement, html, LitElement, property} from "lit-element";
import {
    AttributeDescriptor,
    AttributePredicate,
    AttributeValueType,
    AssetQueryMatch,
    AssetQueryOperator as AQO,
    AssetQuery,
    ValuePredicateUnion,
    ValueType,
    AttributeValueDescriptor,
    AssetDescriptor,
    LogicGroupOperator,
    LogicGroup,
    MetaItemType
} from "@openremote/model";
import {
    AssetQueryOperator, AssetTypeAttributeName,
    getAssetTypeFromQuery,
    getDescriptorValueType, OrRuleChangedEvent,
    RulesConfig
} from "./index";
import {OrSelectChangedEvent} from "@openremote/or-select";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {Util} from "@openremote/core";
import {assetQueryEditorStyle} from "./style";
import i18next from "i18next";
import {AssetModelUtil} from "@openremote/core";

@customElement("or-rule-asset-query")
class OrRuleAssetQuery extends LitElement {

    @property({type: Object})
    public query?: AssetQuery;

    public readonly?: boolean;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    // Value predicates for specific attribute value descriptors
    protected _queryOperatorsMap: Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]> = new Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]>([
        [AttributeValueType.GEO_JSON_POINT, [AssetQueryOperator.EQUALS, AssetQueryOperator.NOT_EQUALS, AssetQueryOperator.WITHIN_RADIUS, AssetQueryOperator.OUTSIDE_RADIUS, AssetQueryOperator.WITHIN_RECTANGLE, AssetQueryOperator.OUTSIDE_RECTANGLE]],
        [ValueType.STRING, [AssetQueryOperator.EQUALS, AssetQueryOperator.NOT_EQUALS, AssetQueryOperator.CONTAINS, AssetQueryOperator.NOT_CONTAINS, AssetQueryOperator.STARTS_WITH, AssetQueryOperator.NOT_STARTS_WITH, AssetQueryOperator.ENDS_WITH, AssetQueryOperator.NOT_ENDS_WITH, AssetQueryOperator.VALUE_EMPTY, AssetQueryOperator.VALUE_NOT_EMPTY]],
        [ValueType.NUMBER, [AssetQueryOperator.EQUALS, AssetQueryOperator.NOT_EQUALS, AssetQueryOperator.GREATER_THAN, AssetQueryOperator.GREATER_EQUALS, AssetQueryOperator.LESS_THAN, AssetQueryOperator.LESS_EQUALS, AssetQueryOperator.BETWEEN, AssetQueryOperator.NOT_BETWEEN, AssetQueryOperator.VALUE_EMPTY, AssetQueryOperator.VALUE_NOT_EMPTY]],
        [ValueType.BOOLEAN, [AssetQueryOperator.IS_TRUE, AssetQueryOperator.IS_FALSE, AssetQueryOperator.VALUE_EMPTY, AssetQueryOperator.VALUE_NOT_EMPTY]],
        [ValueType.ARRAY, [AssetQueryOperator.CONTAINS, AssetQueryOperator.NOT_CONTAINS, AssetQueryOperator.INDEX_CONTAINS, AssetQueryOperator.NOT_INDEX_CONTAINS, AssetQueryOperator.LENGTH_EQUALS, AssetQueryOperator.NOT_LENGTH_EQUALS, AssetQueryOperator.LENGTH_LESS_THAN, AssetQueryOperator.LENGTH_GREATER_THAN, AssetQueryOperator.VALUE_EMPTY, AssetQueryOperator.VALUE_NOT_EMPTY]],
        [ValueType.OBJECT, [AssetQueryOperator.CONTAINS_KEY, AssetQueryOperator.NOT_CONTAINS_KEY, AssetQueryOperator.VALUE_EMPTY, AssetQueryOperator.VALUE_NOT_EMPTY]]
    ]);

    protected attributePredicateEditorTemplate(assetDescriptor: AssetDescriptor, attributePredicate: AttributePredicate) {

        const operator = this.getOperator(attributePredicate);
        const attributeName = this.getAttributeName(attributePredicate);
        const attributes = this.getAttributes(assetDescriptor).map((a) => [a, i18next.t(a)]);

        return html`
            <div class="attribute-editor">
                <or-select @or-select-changed="${(e: OrSelectChangedEvent) => this.setAttributeName(attributePredicate, e.detail.value)}" ?readonly="${this.readonly}" .options="${attributes}" .value="${attributeName}"></or-select>
                
                ${attributeName ? html`<or-select @or-select-changed="${(e: OrSelectChangedEvent) => this.setOperator(assetDescriptor, attributePredicate, e.detail.value)}" ?readonly="${this.readonly}" .options="${this.getOperators(assetDescriptor, attributeName)}" .value="${operator}"></or-select>` : ``}
                
                ${attributePredicate ? this.attributePredicateValueEditorTemplate(assetDescriptor, attributePredicate) : ``}
            </div>
        `;
    }

    protected attributePredicateValueEditorTemplate(assetDescriptor: AssetDescriptor, attributePredicate: AttributePredicate) {

        const valuePredicate = attributePredicate.value;
        const attributeName = this.getAttributeName(attributePredicate);
        const assetType = getAssetTypeFromQuery(this.query);
        const descriptor = assetDescriptor.attributeDescriptors ? assetDescriptor.attributeDescriptors.find((ad) => ad.attributeName === attributeName) : undefined;
        // @ts-ignore
        const value = valuePredicate && valuePredicate.value ? valuePredicate.value : null;

        const attributeInput = this.config && this.config.inputProvider ? this.config.inputProvider(assetType!, attributeName!, descriptor, value, (v: any) => this.setValuePredicateProperty(valuePredicate, "value", v), this.readonly || false, false) : undefined;

        if (attributeInput) {
            return attributeInput;
        }

        if (!assetDescriptor || !valuePredicate) {
            return ``;
        }

        // TODO: Update to use 'effective' meta so not dependent on meta items being defined on the asset itself
        const min = AssetModelUtil.getMetaValue(MetaItemType.RANGE_MIN, undefined, descriptor);
        const max = AssetModelUtil.getMetaValue(MetaItemType.RANGE_MAX, undefined, descriptor);
        let label = AssetModelUtil.getMetaValue(MetaItemType.LABEL, undefined, descriptor);
        label = i18next.t([attributeName, label || attributeName]);
        const readonly = AssetModelUtil.getMetaValue(MetaItemType.READ_ONLY, undefined, descriptor);
        const options = AssetModelUtil.getMetaValue(MetaItemType.ALLOWED_VALUES, undefined, descriptor);

        switch (valuePredicate.predicateType) {
            case "string":
                return html`
                    <or-input required type="${options ? InputType.SELECT : InputType.TEXT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", e.detail.value)}" .options="${options}" .label="${label}" .value="${valuePredicate.value ? valuePredicate.value : null}" ?readonly="${readonly || this.readonly}"></or-input>
                `;
            case "boolean":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "datetime":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "number":
                if (valuePredicate.operator === AQO.BETWEEN) {
                    return html`
                        <or-input required type="${InputType.NUMBER}" @or-input-changed="${(e: OrInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", e.detail.value)}" .min="${min}" .max="${max}" .label="${label}" .value="${valuePredicate.value ? valuePredicate.value : null}" ?readonly="${readonly || this.readonly}"></or-input>
                        <span style="display: inline-flex; align-items: center;">&</span>
                        <or-input required type="${InputType.NUMBER}" @or-input-changed="${(e: OrInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "rangeValue", e.detail.value)}" .min="${min}" .max="${max}" .label="${label}" .value="${valuePredicate.rangeValue ? valuePredicate.rangeValue : null}" ?readonly="${readonly || this.readonly}"></or-input>
                    `;
                }
                return html`
                    <or-input required type="${InputType.NUMBER}" @or-input-changed="${(e: OrInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", e.detail.value)}" .min="${min}" .max="${max}" .label="${label}" .value="${valuePredicate.value ? valuePredicate.value : null}" ?readonly="${readonly || this.readonly}"></or-input>
                `;
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
                return html`<or-input required type="${options ? InputType.SELECT : InputType.TEXT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", e.detail.value)}" .options="${options}" .label="${label}" .value="${valuePredicate.value ? valuePredicate.value : null}" ?readonly="${readonly || this.readonly}"></or-input>`;
            default:
                return html `<span>NOT IMPLEMENTED</span>`;
        }
    }

    static get styles() {
        return assetQueryEditorStyle;
    }

    protected render() {
        if (!this.query) {
            return;
        }

        const assetType = getAssetTypeFromQuery(this.query);

        if (!assetType) {
            return;
        }

        const assetDescriptor = this.assetDescriptors ? this.assetDescriptors.find((ad) => ad.type === assetType) : undefined;

        if (!assetDescriptor) {
            return;
        }

        if (!this.query.attributes) {
            this.query.attributes = {};
        }

        if (!this.query.attributes.items || this.query.attributes.items.length === 0) {
            this.query.attributes.items = [{}];
        }

        const showAddAttribute = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddAttribute !== true);
        const showRemoveAttribute = !this.readonly && this.query.attributes && this.query.attributes.items && this.query.attributes.items.length > 1;

        return html`
            <div class="attribute-group">
                <div class="attribute-items">
                    ${this.query.attributes && this.query.attributes.items ? this.query.attributes.items.map((attributePredicate) => {
                        
                        return html`
                           <div class="attribute-item">
                                <button @click="${() => this.toggleAttributeGroup(this.query!.attributes!)}" class="button-clear operator"><span>${i18next.t(!this.query!.attributes!.operator || this.query!.attributes!.operator === LogicGroupOperator.AND ? "and" : "or")}</span></button>
                                <div class="attribute">
                                    ${this.attributePredicateEditorTemplate(assetDescriptor, attributePredicate)}
                                    ${showRemoveAttribute ? html`
                                        <button class="button-clear" @click="${() => this.removeAttributePredicate(this.query!.attributes!, attributePredicate)}"><or-icon icon="close-circle"></or-icon></input>
                                    ` : ``}
                                </div>
                            </div>
                        `;
                    }) : ``}
                </div>
                
                ${showAddAttribute ? html`
                    <div class="add-buttons-container">
                        ${showAddAttribute ? html`<button class="button-clear add-button" @click="${() => this.addAttributePredicate(this.query!.attributes!)}"><or-icon icon="plus-circle"></or-icon><or-translate value="rulesEditorAddCondition"></or-translate></button>` : ``}
                    </div>
                ` : ``}
            </div>
        `;
    }

    protected getAttributes(descriptor?: AssetDescriptor) {
        let attributes: string[] = [];

        if (descriptor && descriptor.attributeDescriptors) {
            attributes = descriptor.attributeDescriptors.map((ad) => ad.attributeName!);
        }

        return attributes;
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
        this.dispatchEvent(new OrRuleChangedEvent());
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
    
    protected getOperators(assetDescriptor: AssetDescriptor, attributeName: string | undefined) {
        if (!attributeName) {
            return [];
        }

        const descriptor = assetDescriptor.attributeDescriptors ? assetDescriptor.attributeDescriptors.find((ad) => ad.attributeName === attributeName) : undefined;
        const valueType = getDescriptorValueType(descriptor);
        let operators: AssetQueryOperator[] | undefined;

        if (this.config && this.config.controls && this.config.controls.allowedAssetQueryOperators) {
            operators = this.getOperatorMapValue(this.config.controls.allowedAssetQueryOperators, getAssetTypeFromQuery(this.query), attributeName, descriptor, descriptor ? descriptor.valueDescriptor : undefined, valueType);
        }

        if (!operators) {
            operators = this.getOperatorMapValue(this._queryOperatorsMap, getAssetTypeFromQuery(this.query), attributeName, descriptor, descriptor ? descriptor.valueDescriptor : undefined, valueType);
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

    protected setOperator(assetDescriptor: AssetDescriptor, attributePredicate: AttributePredicate, operator: string | undefined) {

        if (!this.query
            || !this.query.attributes
            || !this.query.attributes.items
            || this.query.attributes.items.length === 0) {
            return;
        }

        if (!operator) {
            attributePredicate.value = undefined;
            this.dispatchEvent(new OrRuleChangedEvent());
            this.requestUpdate();
            return;
        }

        const attributeName = this.getAttributeName(attributePredicate);
        const attributeDescriptor = assetDescriptor.attributeDescriptors ? assetDescriptor.attributeDescriptors.find((ad) => ad.attributeName === attributeName) : undefined;
        const valueType = getDescriptorValueType(attributeDescriptor);
        const value = operator as AssetQueryOperator;

        if (!valueType || !value) {
            attributePredicate.value = undefined;
            this.dispatchEvent(new OrRuleChangedEvent());
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
                if (attributeDescriptor && attributeDescriptor.valueDescriptor && attributeDescriptor.valueDescriptor.name === AttributeValueType.DATETIME.name) {
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
        this.dispatchEvent(new OrRuleChangedEvent());
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
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected removeAttributePredicate(group: LogicGroup<AttributePredicate>, attributePredicate: AttributePredicate) {
        const index = group.items!.indexOf(attributePredicate);
        if (index >= 0) {
            group.items!.splice(index, 1);
        }
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected addAttributePredicate(group: LogicGroup<AttributePredicate>) {
        if (!group.items) {
            group.items = [];
        }
        group.items.push({});
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected toggleAttributeGroup(group: LogicGroup<AttributePredicate>) {
        if (group.operator === LogicGroupOperator.OR) {
            group.operator = LogicGroupOperator.AND;
        } else {
            group.operator = LogicGroupOperator.OR;
        }
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }
}
