import { LitElement, PropertyValues } from "lit";
import { Asset, AssetDescriptor, AssetTypeInfo, Attribute, AttributeDescriptor, AttributePredicate, LogicGroup, RuleCondition, ValueDescriptor, ValuePredicateUnion } from "@openremote/model";
import { AssetQueryOperator, RulesConfig } from "../index";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-attribute-input";
import "./modals/or-rule-radial-modal";
declare const OrRuleAssetQuery_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleAssetQuery extends OrRuleAssetQuery_base {
    condition: RuleCondition;
    readonly?: boolean;
    config?: RulesConfig;
    assetInfos?: AssetTypeInfo[];
    assetProvider: (type: string) => Promise<Asset[] | undefined>;
    protected _assets?: Asset[];
    protected _queryOperatorsMap: {
        [type: string]: AssetQueryOperator[];
    };
    constructor();
    refresh(): void;
    protected attributePredicateEditorTemplate(assetTypeInfo: AssetTypeInfo, asset: Asset | undefined, attributePredicate: AttributePredicate): import("lit-html").TemplateResult<1>;
    protected attributePredicateValueEditorTemplate(assetDescriptor: AssetDescriptor, asset: Asset | undefined, attributePredicate: AttributePredicate): import("lit-html").TemplateResult<1> | "";
    static get styles(): import("lit").CSSResult;
    shouldUpdate(_changedProperties: PropertyValues): boolean;
    protected get query(): import("@openremote/model").AssetQuery;
    protected render(): import("lit-html").TemplateResult<1>;
    protected set _assetId(assetId: string | undefined);
    protected getAttributeName(attributePredicate: AttributePredicate): string | undefined;
    protected setAttributeName(attributePredicate: AttributePredicate, attributeName: string | undefined): void;
    protected getOperatorMapValue(operatorMap: {
        [type: string]: AssetQueryOperator[];
    }, assetType?: string, attributeName?: string, attributeDescriptor?: AttributeDescriptor, valueDescriptor?: ValueDescriptor): AssetQueryOperator[] | undefined;
    protected getOperators(assetDescriptor: AssetDescriptor, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: ValueDescriptor | undefined, attribute: Attribute<any> | undefined, attributeName: string): [string, string][];
    protected getOperator(attributePredicate: AttributePredicate): string | undefined;
    protected setOperator(assetDescriptor: AssetDescriptor, attribute: Attribute<any> | undefined, attributeName: string, attributePredicate: AttributePredicate, operator: string | undefined): void;
    protected get attributePredicate(): AttributePredicate | undefined;
    protected setValuePredicateProperty(valuePredicate: ValuePredicateUnion | undefined, propertyName: string, value: any): void;
    protected removeAttributePredicate(group: LogicGroup<AttributePredicate>, attributePredicate: AttributePredicate): void;
    protected addAttributePredicate(group: LogicGroup<AttributePredicate>): void;
    protected toggleAttributeGroup(group: LogicGroup<AttributePredicate>): void;
    protected loadAssets(type: string): void;
}
export {};
