import { LitElement, PropertyValues, TemplateResult } from "lit";
import { RulesConfig, RuleView } from "../index";
import { Asset, AssetQuery, AssetTypeInfo, JsonRule, LogicGroup, RuleActionTarget, RuleActionUnion, RuleCondition, RulesetUnion, ValuePredicateUnion } from "@openremote/model";
import "./or-rule-when";
import "./or-rule-then-otherwise";
import "@openremote/or-components/or-panel";
export declare class OrRulesJsonRuleChangedEvent extends CustomEvent<void> {
    static readonly NAME = "or-rules-json-rule-changed";
    constructor();
}
declare global {
    export interface HTMLElementEventMap {
        [OrRulesJsonRuleChangedEvent.NAME]: OrRulesJsonRuleChangedEvent;
    }
}
export declare function getTargetTypeMap(rule: JsonRule): [string, string?][];
declare const OrRuleJsonViewer_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRuleJsonViewer extends OrRuleJsonViewer_base implements RuleView {
    static get styles(): import("lit").CSSResult;
    readonly?: boolean;
    config?: RulesConfig;
    protected _ruleset: RulesetUnion;
    protected _loadedAssets: Map<string, Asset[]>;
    protected _rule: JsonRule;
    protected _unsupported: boolean;
    protected _activeAssetPromises: Map<string, Promise<any>>;
    protected _assetTypeInfo?: AssetTypeInfo[];
    _whenAssetInfos?: AssetTypeInfo[];
    _actionAssetInfos?: AssetTypeInfo[];
    constructor();
    connectedCallback(): void;
    set ruleset(ruleset: RulesetUnion);
    updated(changedProperties: PropertyValues): void;
    protected render(): TemplateResult | void;
    protected loadAssets(type: string): Promise<Asset[] | undefined>;
    protected loadAssetDescriptors(useActionConfig: boolean): void;
    beforeSave(): void;
    validate(): boolean;
    protected _onJsonRuleChanged(): void;
    protected _validateConditionGroup(group: LogicGroup<RuleCondition>): boolean;
    protected _validateRuleActions(rule: JsonRule, actions?: RuleActionUnion[]): boolean;
    protected _validateAssetTarget(rule: JsonRule, target?: RuleActionTarget): boolean;
    protected _validateAssetQuery(query: AssetQuery, isWhen: boolean, isMatchedAssets: boolean): boolean;
    protected _validateValuePredicate(valuePredicate: ValuePredicateUnion): boolean;
}
export {};
