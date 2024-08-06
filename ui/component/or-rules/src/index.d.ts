import { LitElement, TemplateResult } from "lit";
import "@openremote/or-icon";
import { AssetQuery, AssetTypeInfo, AttributeDescriptor, JsonRule, LogicGroup, NotificationTargetType, RuleActionUnion, RuleCondition, RulesetLang, RulesetUnion, Asset } from "@openremote/model";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-drawer";
import "./or-rule-list";
import "./or-rule-viewer";
import "./flow-viewer/flow-viewer";
import { RecurrenceOption } from "./json-viewer/or-rule-then-otherwise";
import { ValueInputProviderGenerator } from "@openremote/or-mwc-components/or-mwc-input";
export { buttonStyle } from "./style";
export declare const enum ConditionType {
    AGENT_QUERY = "agentQuery",
    ASSET_QUERY = "assetQuery",
    TIME = "time"
}
export declare const enum ActionType {
    WAIT = "wait",
    EMAIL = "email",
    PUSH_NOTIFICATION = "push",
    ATTRIBUTE = "attribute",
    WEBHOOK = "webhook"
}
export declare enum TimeTriggerType {
    TIME_OF_DAY = "TIME_OF_DAY"
}
export declare enum AssetQueryOperator {
    VALUE_EMPTY = "empty",
    VALUE_NOT_EMPTY = "notEmpty",
    EQUALS = "equals",
    NOT_EQUALS = "notEquals",
    GREATER_THAN = "greaterThan",
    GREATER_EQUALS = "greaterEquals",
    LESS_THAN = "lessThan",
    LESS_EQUALS = "lessEquals",
    BETWEEN = "between",
    NOT_BETWEEN = "notBetween",
    CONTAINS = "contains",
    NOT_CONTAINS = "notContains",
    STARTS_WITH = "startsWith",
    NOT_STARTS_WITH = "notStartsWith",
    ENDS_WITH = "endsWith",
    NOT_ENDS_WITH = "notEndsWith",
    CONTAINS_KEY = "containsKey",
    NOT_CONTAINS_KEY = "notContainsKey",
    INDEX_CONTAINS = "indexContains",
    NOT_INDEX_CONTAINS = "notIndexContains",
    LENGTH_EQUALS = "lengthEquals",
    NOT_LENGTH_EQUALS = "notLengthEquals",
    LENGTH_GREATER_THAN = "lengthGreaterThan",
    LENGTH_LESS_THAN = "lengthLessThan",
    IS_TRUE = "true",
    IS_FALSE = "false",
    WITHIN_RADIUS = "withinRadius",
    OUTSIDE_RADIUS = "outsideRadius",
    WITHIN_RECTANGLE = "withinRectangle",
    OUTSIDE_RECTANGLE = "outsideRectangle"
}
export interface AllowedActionTargetTypes {
    default?: NotificationTargetType[];
    actions?: {
        [actionType in ActionType]?: NotificationTargetType[];
    };
}
export interface RulesConfig {
    controls?: {
        allowedLanguages?: RulesetLang[];
        allowedConditionTypes?: ConditionType[];
        allowedActionTypes?: ActionType[];
        allowedAssetQueryOperators?: {
            [name: string]: AssetQueryOperator[];
        };
        allowedRecurrenceOptions?: RecurrenceOption[];
        allowedActionTargetTypes?: AllowedActionTargetTypes;
        hideActionTypeOptions?: boolean;
        hideActionTargetOptions?: boolean;
        hideActionUpdateOptions?: boolean;
        hideConditionTypeOptions?: boolean;
        hideThenAddAction?: boolean;
        hideWhenAddCondition?: boolean;
        hideWhenAddAttribute?: boolean;
        hideWhenAddGroup?: boolean;
        multiSelect?: boolean;
    };
    inputProvider?: ValueInputProviderGenerator;
    descriptors?: {
        all?: RulesDescriptorSection;
        when?: RulesDescriptorSection;
        action?: RulesDescriptorSection;
    };
    rulesetTemplates?: {
        [key in RulesetLang]?: string;
    };
    rulesetAddHandler?: (ruleset: RulesetUnion) => boolean;
    rulesetDeleteHandler?: (ruleset: RulesetUnion) => boolean;
    rulesetCopyHandler?: (ruleset: RulesetUnion) => boolean;
    rulesetSaveHandler?: (ruleset: RulesetUnion) => boolean;
    json?: {
        rule?: JsonRule;
        whenGroup?: LogicGroup<RuleCondition>;
        whenCondition?: RuleCondition;
        whenAssetQuery?: AssetQuery;
        then?: RuleActionUnion;
        otherwise?: RuleActionUnion;
    };
}
export interface RulesDescriptorSection {
    includeAssets?: string[];
    excludeAssets?: string[];
    attributeDescriptors?: {
        [attributeName: string]: RulesConfigAttribute;
    };
    /**
     * Asset type specific config; '*' key will be used as a default fallback if no asset type specific entry exists
     */
    assets?: {
        [assetType: string]: RulesConfigAsset;
    };
}
export interface RulesConfigAsset {
    includeAttributes?: string[];
    excludeAttributes?: string[];
    name?: string;
    icon?: string;
    color?: string;
    attributeDescriptors?: {
        [attributeName: string]: RulesConfigAttribute;
    };
}
export interface RulesConfigAttribute extends AttributeDescriptor {
}
export interface RulesetNode {
    ruleset: RulesetUnion;
    selected: boolean;
}
export interface RequestEventDetail<T> {
    allow: boolean;
    detail: T;
}
export interface RuleView {
    validate: () => boolean;
    beforeSave: () => void;
    ruleset?: RulesetUnion;
    readonly?: boolean;
    config?: RulesConfig;
}
export interface RuleViewInfo {
    viewTemplateProvider: (ruleset: RulesetUnion, config: RulesConfig | undefined, readonly: boolean) => TemplateResult;
    viewRulesetTemplate?: string;
}
export declare const RuleViewInfoMap: {
    [key in RulesetLang]: RuleViewInfo;
};
export declare function getAssetTypeFromQuery(query?: AssetQuery): string | undefined;
export declare function getAssetIdsFromQuery(query?: AssetQuery): any[] | undefined;
export declare const getAssetTypes: () => Promise<string[]>;
export declare function getAssetInfos(config: RulesConfig | undefined, useActionConfig: boolean): Promise<AssetTypeInfo[]>;
export declare function getAssetsByType(type: string, realm?: string, loadedAssets?: Map<string, Asset[]>): Promise<{
    assets?: Asset[];
    loadedAssets?: Map<string, Asset[]>;
}>;
export declare class OrRulesRuleChangedEvent extends CustomEvent<boolean> {
    static readonly NAME = "or-rules-rule-changed";
    constructor(valid: boolean);
}
export declare class OrRulesRuleUnsupportedEvent extends CustomEvent<void> {
    static readonly NAME = "or-rules-rule-unsupported";
    constructor();
}
export interface NodeSelectEventDetail {
    oldNodes: RulesetNode[];
    newNodes: RulesetNode[];
}
export declare class OrRulesRequestSelectionEvent extends CustomEvent<RequestEventDetail<NodeSelectEventDetail>> {
    static readonly NAME = "or-rules-request-selection";
    constructor(request: NodeSelectEventDetail);
}
export declare class OrRulesSelectionEvent extends CustomEvent<NodeSelectEventDetail> {
    static readonly NAME = "or-rules-selection";
    constructor(nodes: NodeSelectEventDetail);
}
export type AddEventDetail = {
    ruleset: RulesetUnion;
    sourceRuleset?: RulesetUnion;
};
export declare class OrRulesRequestAddEvent extends CustomEvent<RequestEventDetail<AddEventDetail>> {
    static readonly NAME = "or-rules-request-add";
    constructor(detail: AddEventDetail);
}
export declare class OrRulesAddEvent extends CustomEvent<AddEventDetail> {
    static readonly NAME = "or-rules-add";
    constructor(detail: AddEventDetail);
}
export declare class OrRulesRequestDeleteEvent extends CustomEvent<RequestEventDetail<RulesetNode[]>> {
    static readonly NAME = "or-rules-request-delete";
    constructor(request: RulesetNode[]);
}
export type SaveResult = {
    success: boolean;
    ruleset: RulesetUnion;
    isNew: boolean;
};
export declare class OrRulesRequestSaveEvent extends CustomEvent<RequestEventDetail<RulesetUnion>> {
    static readonly NAME = "or-rules-request-save";
    constructor(ruleset: RulesetUnion);
}
export declare class OrRulesSaveEvent extends CustomEvent<SaveResult> {
    static readonly NAME = "or-rules-save";
    constructor(result: SaveResult);
}
export declare class OrRulesDeleteEvent extends CustomEvent<RulesetUnion[]> {
    static readonly NAME = "or-rules-delete";
    constructor(rulesets: RulesetUnion[]);
}
declare global {
    export interface HTMLElementEventMap {
        [OrRulesRuleUnsupportedEvent.NAME]: OrRulesRuleUnsupportedEvent;
        [OrRulesRuleChangedEvent.NAME]: OrRulesRuleChangedEvent;
        [OrRulesRequestSelectionEvent.NAME]: OrRulesRequestSelectionEvent;
        [OrRulesSelectionEvent.NAME]: OrRulesSelectionEvent;
        [OrRulesRequestAddEvent.NAME]: OrRulesRequestAddEvent;
        [OrRulesAddEvent.NAME]: OrRulesAddEvent;
        [OrRulesRequestDeleteEvent.NAME]: OrRulesRequestDeleteEvent;
        [OrRulesRequestSaveEvent.NAME]: OrRulesRequestSaveEvent;
        [OrRulesSaveEvent.NAME]: OrRulesSaveEvent;
        [OrRulesDeleteEvent.NAME]: OrRulesDeleteEvent;
    }
}
export declare const style: import("lit").CSSResult;
declare const OrRules_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrRules extends OrRules_base {
    static DEFAULT_RULESET_NAME: string;
    static get styles(): import("lit").CSSResult[];
    readonly?: boolean;
    config?: RulesConfig;
    realm?: string;
    language?: RulesetLang;
    selectedIds?: number[];
    private _isValidRule?;
    private _rulesList;
    private _viewer;
    constructor();
    protected render(): TemplateResult<1>;
    refresh(): void;
    protected isReadonly(): boolean;
    protected _confirmContinue(action: () => void): void;
    protected _onRuleSelectionRequested(event: OrRulesRequestSelectionEvent): void;
    protected _onRuleSelectionChanged(event: OrRulesSelectionEvent): void;
    protected _onRuleAdd(event: OrRulesAddEvent): void;
    protected _onRuleSave(event: OrRulesSaveEvent): Promise<void>;
}
