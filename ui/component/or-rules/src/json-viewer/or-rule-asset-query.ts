import {css, html, LitElement, PropertyValues} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {
    Asset,
    AssetDescriptor,
    AssetQueryMatch,
    AssetQueryOperator as AQO,
    AssetTypeInfo,
    Attribute,
    AttributeDescriptor,
    AttributePredicate,
    LogicGroup,
    LogicGroupOperator,
    RuleCondition,
    ValueDescriptor,
    ValuePredicateUnion,
    WellknownMetaItems,
    WellknownValueTypes,
    AssetModelUtil
} from "@openremote/model";
import {AssetQueryOperator, getAssetIdsFromQuery, getAssetTypeFromQuery, RulesConfig} from "../index";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-attribute-input";
import {Util} from "@openremote/core";
import i18next from "i18next";
import {buttonStyle} from "../style";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {translate} from "@openremote/or-translate";
import {OrAttributeInputChangedEvent} from "@openremote/or-attribute-input";
import "./modals/or-rule-radial-modal";
import { ifDefined } from "lit/directives/if-defined.js";
import {when} from 'lit/directives/when.js';
import {getWhenTypesMenu} from "./or-rule-condition";

// language=CSS
const style = css`
    
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

@customElement("or-rule-asset-query")
export class OrRuleAssetQuery extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public condition!: RuleCondition;

    public readonly?: boolean;

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Object})
    public assetInfos?: AssetTypeInfo[];

    @property({type: Object})
    public assetProvider!: (type: string) => Promise<Asset[] | undefined>

    @state()
    protected _assets?: Asset[];

    // Value predicates for specific value descriptors
    protected _queryOperatorsMap: {[type: string]: AssetQueryOperator[]} = {};

    constructor() {
        super();

        this._queryOperatorsMap[WellknownValueTypes.GEOJSONPOINT] = [
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

    public refresh() {
        // Clear assets
        this._assets = undefined;
    }

    protected attributePredicateEditorTemplate(assetTypeInfo: AssetTypeInfo, asset: Asset | undefined, attributePredicate: AttributePredicate) {

        const assetDescriptor = assetTypeInfo.assetDescriptor!;
        const operator = this.getOperator(attributePredicate);
        const attributeName = this.getAttributeName(attributePredicate);
        let attribute = asset && asset.attributes && attributeName ? asset.attributes[attributeName] : undefined;
        let attributes: [string, string][] = [];
        const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset ? asset.type : assetDescriptor.name, attribute || attributeName, attribute);

        if (asset && asset.attributes) {
            attributes = Object.values(asset.attributes)
                .filter((attribute) => attribute.meta && (attribute.meta.hasOwnProperty(WellknownMetaItems.RULESTATE) ? attribute.meta[WellknownMetaItems.RULESTATE] : attribute.meta.hasOwnProperty(WellknownMetaItems.AGENTLINK)))
                .map((attr) => {
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attr.name, attr);
                    const label = Util.getAttributeLabel(attr, descriptors[0], asset.type, false);
                    return [attr.name!, label];
                });
        } else {
            attributes = !assetTypeInfo || !assetTypeInfo.attributeDescriptors ? [] :
                assetTypeInfo.attributeDescriptors
                    .map((ad) => {
                        const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(assetDescriptor.name, ad);
                        const label = Util.getAttributeLabel(undefined, descriptors ? descriptors[0] : undefined, assetDescriptor.name, false);
                        return [ad.name!, label];
                    });
        }  
        
        attributes.sort(Util.sortByString((attr) => attr[1]));

        const operators = attributeName ? this.getOperators(assetDescriptor, descriptors ? descriptors[0] : undefined, descriptors ? descriptors[1] : undefined, attribute, attributeName) : [];

        return html`
            <or-mwc-input type="${InputType.SELECT}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setAttributeName(attributePredicate, e.detail.value)}" .readonly="${this.readonly || false}" .options="${attributes}" .value="${attributeName}" .label="${i18next.t("attribute")}"></or-mwc-input>
            ${attributeName ? html`<or-mwc-input type="${InputType.SELECT}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setOperator(assetDescriptor, attribute, attributeName, attributePredicate, e.detail.value)}" .readonly="${this.readonly || false}" .options="${operators}" .value="${operator}" .label="${i18next.t("operator")}"></or-mwc-input>` : ``}
            ${attributePredicate ? this.attributePredicateValueEditorTemplate(assetDescriptor, asset, attributePredicate) : ``}
        `;
    }

    protected attributePredicateValueEditorTemplate(assetDescriptor: AssetDescriptor, asset: Asset | undefined, attributePredicate: AttributePredicate) {

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
                return html`<or-attribute-input @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("value")}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>`;
            case "boolean":
                return html ``; // Handled by the operator IS_TRUE or IS_FALSE
            case "datetime":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "number":
                if (valuePredicate.operator === AQO.BETWEEN) {
                    return html`
                        <or-attribute-input .inputType="${InputType.NUMBER}" @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("between")}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>
                        <span style="display: inline-flex; align-items: center;">&</span>
                        <or-attribute-input .inputType="${InputType.NUMBER}" @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "rangeValue", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="${i18next.t("and")}" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${valuePredicate.rangeValue}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>
                    `;
                }            
                let inputType;
                if(descriptors[0]?.format?.asSlider) inputType = InputType.NUMBER;
                return html`<or-attribute-input .inputType="${ifDefined(inputType)}" @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}"></or-attribute-input>`;
            case "radial":
                return html`<or-rule-radial-modal .query="${this.query}" .assetDescriptor="${assetDescriptor}" .attributePredicate="${attributePredicate}"></or-rule-radial-modal>`;
            case "rect":
                return html `<span>NOT IMPLEMENTED</span>`;
            case "value-empty":
                return ``;
            case "array":
                // TODO: Update once we can determine inner type of array
                // Assume string array
                return html`<or-attribute-input @or-attribute-input-changed="${(ev: OrAttributeInputChangedEvent) => this.setValuePredicateProperty(valuePredicate, "value", ev.detail.value)}" .customProvider="${this.config?.inputProvider}" .label="" .assetType="${assetType}" .attributeDescriptor="${descriptors[0]}" .attributeValueDescriptor="${descriptors[1]}" .value="${value}" .readonly="${this.readonly || false}" .fullWidth="${true}></or-attribute-input>`;
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
            return html`<span class="invalidLabel">${i18next.t('errorOccurred')}</span>`;
        }

        const assetTypeInfo = this.assetInfos ? this.assetInfos.find((assetTypeInfo) => assetTypeInfo.assetDescriptor!.name === assetType) : undefined;

        if (!assetTypeInfo) {
            return html`<span class="invalidLabel">${i18next.t('errorOccurred')}</span>`;
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
        const idOptions: [string, string] [] = [
            ["*", i18next.t("anyOfThisType")]
        ];
        let searchProvider: (search?: string) => Promise<[any, string][]>;

        return html`
            <div class="attribute-group">
            
                <!-- Show SELECT input with 'loading' until the assets are retrieved -->
                ${when((!this._assets), () => html`
                    <or-mwc-input id="idSelect" class="min-width" type="${InputType.SELECT}" .readonly="${true}" .label="${i18next.t('loading')}"></or-mwc-input>
                `, () => {

                    // Set list of displayed assets, and filtering assets out if needed.
                    // If <= 25 assets: display everything
                    // If between 25 and 100 assets: display everything with search functionality 
                    // If > 100 assets: only display if in line with search input
                    if(this._assets!.length <= 25) {
                        idOptions.push(...this._assets!.map((asset) => [asset.id!, asset.name!] as [string, string]));
                    } else {
                        searchProvider = async (search?: string) => {
                            if(search) {
                                return this._assets!.filter((asset) => asset.name?.toLowerCase().includes(search.toLowerCase())).map((asset) => [asset.id!, asset.name!] as [string, string]);
                            } else if (this._assets!.length <= 100) {
                                idOptions.push(...this._assets!.map((asset) => [asset.id!, asset.name!] as [string, string]));
                                return idOptions;
                            } else {
                                const asset = this._assets?.find((asset) => asset.id == idValue);
                                if(asset && idOptions.find(([id, _value]) => id == asset.id) == undefined) {
                                    idOptions.push([asset.id!, asset.name!]); // add selected asset if there is one.
                                }
                                return idOptions;
                            }
                        }
                    }

                    const showAddAttribute = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddAttribute !== true);
                    
                    return html`
                        <or-mwc-input id="idSelect" class="min-width filledSelect" type="${InputType.SELECT}" .readonly="${this.readonly || false}" .label="${i18next.t("asset")}" 
                                      .options="${idOptions}" .value="${idValue}" .searchProvider="${searchProvider}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => { this._assetId = (e.detail.value); this.refresh(); }}"
                        ></or-mwc-input>
                        <div class="attributes">
                            ${this.query.attributes && this.query.attributes.items ? this.query.attributes.items.map((attributePredicate, index) => {
                                return html`
                                    ${index > 0 ? html`<or-icon class="small" icon="ampersand"></or-icon>` : ``}
                                    <div class="attribute">
                                        <div>
                                    ${this.attributePredicateEditorTemplate(assetTypeInfo, idValue !== "*" ? this._assets!.find((asset) => asset.id === idValue) : undefined, attributePredicate)}
                                        </div>
                                    ${showRemoveAttribute ? html`
                                        <button class="button-clear" @click="${() => this.removeAttributePredicate(this.query!.attributes!, attributePredicate)}"><or-icon icon="close-circle"></or-icon></input>
                                    </div>` : ``}
                                `;
                            }) : ``}
                            ${showAddAttribute ? html`
                                <or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"
                                              label="rulesEditorAddAttribute" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.addAttributePredicate(this.query!.attributes!)}"></or-mwc-input>
                            `: ``}
                        </div>
                    `;
                })}
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

    protected getOperatorMapValue(operatorMap: {[type: string]: AssetQueryOperator[]}, assetType?: string, attributeName?: string, attributeDescriptor?: AttributeDescriptor, valueDescriptor?: ValueDescriptor) {

        let assetAttributeMatch: AssetQueryOperator[] | undefined;
        let attributeDescriptorMatch: AssetQueryOperator[] | undefined;
        let attributeValueDescriptorMatch: AssetQueryOperator[] | undefined;

        if (assetType && attributeName) {
            if (operatorMap[assetType + ":" + attributeName]) {
                return operatorMap[assetType + ":" + attributeName];
            }
        }
        if (valueDescriptor) {
            if (operatorMap[valueDescriptor.name!]) {
                return operatorMap[valueDescriptor.name!];
            }
            if (operatorMap[valueDescriptor.jsonType!]) {
                return operatorMap[valueDescriptor.jsonType!];
            }
            if (valueDescriptor.arrayDimensions) {
                return operatorMap["array"];
            }
        }
    }
    
    protected getOperators(assetDescriptor: AssetDescriptor, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: ValueDescriptor | undefined, attribute: Attribute<any> | undefined, attributeName: string): [string, string][] {

        let operators: AssetQueryOperator[] | undefined;

        if (this.config && this.config.controls && this.config.controls.allowedAssetQueryOperators) {
            operators = this.getOperatorMapValue(this.config.controls.allowedAssetQueryOperators, getAssetTypeFromQuery(this.query), attributeName, attributeDescriptor, valueDescriptor);
        }

        if (!operators) {
            operators = this.getOperatorMapValue(this._queryOperatorsMap, getAssetTypeFromQuery(this.query), attributeName, attributeDescriptor, valueDescriptor);
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

    protected setOperator(assetDescriptor: AssetDescriptor, attribute: Attribute<any> | undefined, attributeName: string, attributePredicate: AttributePredicate, operator: string | undefined) {

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
        const value = operator as AssetQueryOperator;

        if (!descriptors || !descriptors[1] || !value) {
            attributePredicate.value = undefined;
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
            return;
        }

        const valueDescriptor = descriptors[1];
        let predicate: ValuePredicateUnion | undefined;

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
                        match: AssetQueryMatch.BEGIN
                    };
                }
                break;
            case AssetQueryOperator.ENDS_WITH:
            case AssetQueryOperator.NOT_ENDS_WITH:
                if (valueDescriptor.jsonType === "string") {
                    predicate = {
                        predicateType: "string",
                        negate: value === AssetQueryOperator.NOT_ENDS_WITH,
                        match: AssetQueryMatch.END
                    };
                }
                break;

            // number or datetime
            case AssetQueryOperator.NOT_BETWEEN:
                if (valueDescriptor.jsonType === "number" || valueDescriptor.jsonType === "bigint") {
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

                if (valueDescriptor.jsonType === "number") {
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
                if (valueDescriptor.jsonType === "date") {
                    predicate = {
                        predicateType: "datetime",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        operator: AQO.EQUALS
                    };
                } else if (valueDescriptor.jsonType === "number") {
                    predicate = {
                        predicateType: "number",
                        negate: value === AssetQueryOperator.NOT_EQUALS,
                        operator: AQO.EQUALS
                    };
                } else if (valueDescriptor.jsonType === "string") {
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
                } else if (valueDescriptor.jsonType === "string") {
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
        this.assetProvider(type).then(assets => {
            this._assets = assets;
        })
    }
}
