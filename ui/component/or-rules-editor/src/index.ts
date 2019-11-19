import {customElement, html, css, unsafeCSS, LitElement, property, TemplateResult, query} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultBoxShadow, DefaultColor4, DefaultColor6, DefaultDisabledOpacity} from "@openremote/core";
import i18next from "i18next";
import "@openremote/or-select";
import "@openremote/or-icon";
import {
    AssetDescriptor,
    AttributeDescriptor,
    AttributeValueDescriptor,
    AssetQueryOperator as AQO,
    JsonRulesetDefinition,
    LogicGroup,
    MetaItemDescriptor,
    MetaItemType,
    AssetQuery,
    JsonRule,
    RuleActionTarget,
    RuleActionUnion,
    RuleCondition,
    RulesetLang,
    TenantRuleset,
    ValuePredicateUnion,
    ValueType,
    Attribute
} from "@openremote/model";
import manager, {AssetModelUtil} from "@openremote/core";
import {Util} from "@openremote/core";
import "@openremote/or-translate";
import {translate} from "@openremote/or-translate";
import "./or-rule-list";
import "./or-rule-when";
import "./or-rule-then-otherwise";
import "./or-rule-header";
import "@openremote/or-panel";
import {OrRulesList} from "./or-rule-list";
import {Menu, OrMwcMenu, OrMwcMenuChangedEvent} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {InputType, OrInput} from "@openremote/or-input";

export const enum ConditionType {
    ASSET_QUERY = "assetQuery",
    DATE_TIME = "datetime",
    TIMER = "timer"
}

export const enum ActionType {
    WAIT = "wait",
    NOTIFICATION = "notification",
    ATTRIBUTE = "attribute"
}

export enum AssetQueryOperator {
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

export interface AssetTypeAttributeName {
    assetType: string;
    attributeName: string;
}

export interface RulesConfig {
    controls?: {
        whenReadonly?: boolean;
        thenReadonly?: boolean;
        headerReadonly?: boolean;
        allowedConditionTypes?: ConditionType[];
        allowedActionTypes?: ActionType[];
        allowedAssetQueryOperators?: Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]>;
        hideActionTypeOptions?: boolean;
        hideActionTargetOptions?: boolean;
        hideActionUpdateOptions?: boolean;
        hideConditionTypeOptions?: boolean;
        hideThenAddAction?: boolean;
        hideWhenAddCondition?: boolean;
        hideWhenAddAttribute?: boolean;
        hideWhenAddGroup?: boolean;
    };
    inputProvider?: (assetType: string | undefined, attribute: Attribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean | undefined, disabled: boolean | undefined) => ((value: any) => TemplateResult) | undefined;
    descriptors?: {
        all?: RulesDescriptorSection;
        when?: RulesDescriptorSection;
        action?: RulesDescriptorSection;
    };
    templates?: {
        rule?: JsonRule;
        rulesetName?: string;
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
    assets?: { [assetType: string]: RulesConfigAsset };
    attributeDescriptors?: {[attributeName: string]: RulesConfigAttribute };
}

export interface RulesConfigAsset {
    includeAttributes?: string[];
    excludeAttributes?: string[];
    name?: string;
    icon?: string;
    attributeDescriptors?: {[attributeName: string]: RulesConfigAttribute };
}

export interface RulesConfigAttribute {
    icon?: string;
    valueDescriptor?: RulesConfigAttribute;
    initialValue?: any;
    allowedValues?: any[];
    allowedMin?: any;
    allowedMax?: any;
}

export interface RulesConfigAttributeValue {
    name?: string;
    icon?: string;
    valueType?: ValueType;
}

export interface RulesetNode {
    ruleset: TenantRuleset;
    selected: boolean;
}

export interface RequestEventDetail<T> {
    allow: boolean,
    detail: T
}

function getAssetDescriptorFromSection(assetTpe: string, config: RulesConfig | undefined, useActionConfig: boolean) {
    if (!config || !config.descriptors) {
        return;
    }

    const section = useActionConfig ? config.descriptors.action : config.descriptors.when;
    const allSection = config.descriptors.all;

    const descriptor = section && section.assets ? section.assets[assetTpe] : undefined;
    if (descriptor) {
        return descriptor;
    }

    return allSection && allSection.assets ? allSection.assets[assetTpe] : undefined;
}

export function getDescriptorValueType(descriptor?: AttributeDescriptor) {
    if (!descriptor || !descriptor.valueDescriptor) {
        return;
    }

    return descriptor.valueDescriptor.valueType;
}

export function getAssetTypeFromQuery(query?: AssetQuery) {
    return query && query.types && query.types.length > 0 && query.types[0] ? query.types[0].value : undefined;
}

export function getAssetIdsFromQuery(query?: AssetQuery) {
    return query && query.ids ? [...query.ids] : undefined;
}

export function getAttributeNames(descriptor?: AssetDescriptor) {
    let attributes: string[] = [];

    if (descriptor && descriptor.attributeDescriptors) {
        attributes = descriptor.attributeDescriptors.map((ad) => ad.attributeName!);
    }

    return attributes;
}

export function getAssetDescriptors(config: RulesConfig | undefined, useActionConfig: boolean) {
    const assetDescriptors: AssetDescriptor[] = AssetModelUtil.getAssetDescriptors();
    let allowedAssetTypes: string[] = [];
    let excludedAssetTypes: string[] = [];

    if (!config || !config.descriptors) {
        return assetDescriptors;
    }

    const section = useActionConfig ? config.descriptors.action : config.descriptors.when;

    if (section && section.includeAssets) {
        allowedAssetTypes = [...section.includeAssets];
    }

    if (config.descriptors.all && config.descriptors.all.includeAssets) {
        allowedAssetTypes = allowedAssetTypes.concat(config.descriptors.all.includeAssets);
    }

    if (section && section.excludeAssets) {
        excludedAssetTypes = [...section.excludeAssets];
    }

    if (config.descriptors.all && config.descriptors.all.excludeAssets) {
        excludedAssetTypes = excludedAssetTypes.concat(config.descriptors.all.excludeAssets);
    }

    return assetDescriptors.filter((ad) => {
        if (allowedAssetTypes.length > 0 && allowedAssetTypes.indexOf(ad.type!) < 0) {
            return false;
        }
        return excludedAssetTypes.indexOf(ad.type!) < 0;

    }).map((ad) => {
        // Amalgamate matching descriptor from config if defined
        const configDescriptor = getAssetDescriptorFromSection(ad.type!, config, useActionConfig);
        if (!configDescriptor) {
            return ad;
        }

        const modifiedDescriptor: AssetDescriptor = {
            name: ad.name,
            type: ad.type,
            icon: ad.icon,
            attributeDescriptors: ad.attributeDescriptors ? [...ad.attributeDescriptors] : undefined
        };

        if (configDescriptor.icon) {
            modifiedDescriptor.icon = configDescriptor.icon;
        }
        if (configDescriptor.name) {
            modifiedDescriptor.name = configDescriptor.name;
        }

        // Remove any excluded attributes
        if (modifiedDescriptor.attributeDescriptors) {
            const inc = configDescriptor.includeAttributes !== undefined ? configDescriptor.includeAttributes : undefined;
            const exc = configDescriptor.excludeAttributes !== undefined ? configDescriptor.excludeAttributes : undefined;

            if (inc || exc) {
                modifiedDescriptor.attributeDescriptors = modifiedDescriptor.attributeDescriptors.filter((mad) => {
                    if (exc && exc.indexOf(mad.attributeName!) >= 0) {
                        return false;
                    }
                    if (inc && inc.indexOf(mad.attributeName!) < 0) {
                        return false;
                    }
                    return true;
                });
            }

            // Override any attribute descriptors
            if (configDescriptor.attributeDescriptors) {
                modifiedDescriptor.attributeDescriptors.map((attributeDescriptor) => {
                    let configAttributeDescriptor: RulesConfigAttribute | undefined = configDescriptor.attributeDescriptors![attributeDescriptor.attributeName!];
                    if (!configAttributeDescriptor) {
                        configAttributeDescriptor = section && section.attributeDescriptors ? section.attributeDescriptors[attributeDescriptor.attributeName!] : undefined;
                    }
                    if (!configAttributeDescriptor) {
                        configAttributeDescriptor = config.descriptors!.all && config.descriptors!.all.attributeDescriptors ? config.descriptors!.all.attributeDescriptors[attributeDescriptor.attributeName!] : undefined;
                    }
                    if (configAttributeDescriptor) {
                        let metaItemDescriptors = attributeDescriptor.metaItemDescriptors;
                        if (!metaItemDescriptors) {
                            attributeDescriptor.metaItemDescriptors = [];
                            metaItemDescriptors = attributeDescriptor.metaItemDescriptors;
                        }

                        if (configAttributeDescriptor.allowedValues) {
                            addOrReplaceMetaItemDescriptor(metaItemDescriptors, AssetModelUtil.getMetaItemDescriptorInitialValue(MetaItemType.ALLOWED_VALUES, configAttributeDescriptor.allowedValues));
                        }
                        if (configAttributeDescriptor.allowedMin) {
                            addOrReplaceMetaItemDescriptor(metaItemDescriptors, AssetModelUtil.getMetaItemDescriptorInitialValue(MetaItemType.RANGE_MIN, configAttributeDescriptor.allowedMin));
                        }
                        if (configAttributeDescriptor.allowedMax) {
                            addOrReplaceMetaItemDescriptor(metaItemDescriptors, AssetModelUtil.getMetaItemDescriptorInitialValue(MetaItemType.RANGE_MAX, configAttributeDescriptor.allowedMax));
                        }

                        return {
                            attributeName: attributeDescriptor.attributeName,
                            valueDescriptor: configAttributeDescriptor.valueDescriptor ? configAttributeDescriptor.valueDescriptor : attributeDescriptor.valueDescriptor,
                            initialValue: configAttributeDescriptor.initialValue ? configAttributeDescriptor.initialValue : attributeDescriptor.initialValue,
                            metaItemDescriptors: attributeDescriptor.metaItemDescriptors
                        };
                    }

                    return attributeDescriptor;
                });
            }
        }
        return modifiedDescriptor;
    });
}

export function getButtonWithMenuTemplate(menu: Menu, icon: string, value: string | undefined, valueChangedCallback: (v: string) => void): TemplateResult {

    const openMenu = (evt: Event) => {
        if (!menu) {
            return;
        }

        ((evt.currentTarget as OrInput).parentElement!.lastElementChild as OrMwcMenu).open();
    };

    return html`
        <div style="display: table-cell; vertical-align: middle;">
            <or-input type="${InputType.BUTTON}" .icon="${icon}" @click="${openMenu}"></or-input>
            ${menu ? html`<or-mwc-menu @or-mwc-menu-changed="${(evt: OrMwcMenuChangedEvent) => valueChangedCallback(evt.detail)}" .value="${value}" .menu="${menu}" id="menu"></or-mwc-menu>` : ``}
        </div>
    `;
}

function addOrReplaceMetaItemDescriptor(metaItemDescriptors: MetaItemDescriptor[], metaItemDescriptor: MetaItemDescriptor) {
    const index = metaItemDescriptors.findIndex((mid) => mid.urn === metaItemDescriptor.urn);
    if (index >= 0) {
        metaItemDescriptors.splice(index, 1);

    }
    metaItemDescriptors.push(metaItemDescriptor);
}

export class OrRulesEditorRuleChangedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-editor-rule-changed";

    constructor() {
        super(OrRulesEditorRuleChangedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrRulesEditorRuleInvalidEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-editor-rule-invalid";

    constructor() {
        super(OrRulesEditorRuleInvalidEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrRulesEditorRuleUnsupportedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-editor-rule-unsupported";

    constructor() {
        super(OrRulesEditorRuleUnsupportedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrRulesEditorRequestSelectEvent extends CustomEvent<RequestEventDetail<TenantRuleset[]>> {

    public static readonly NAME = "or-rules-editor-request-select";

    constructor(request: TenantRuleset[]) {
        super(OrRulesEditorRequestSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: request
            }
        });
    }
}

export class OrRulesEditorRequestAddEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-editor-request-add";

    constructor() {
        super(OrRulesEditorRequestAddEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrRulesEditorRequestDeleteEvent extends CustomEvent<TenantRuleset[]> {

    public static readonly NAME = "or-rules-editor-request-delete";

    constructor(request: TenantRuleset[]) {
        super(OrRulesEditorRequestDeleteEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: request
        });
    }
}

export class OrRulesEditorRequestCopyEvent extends CustomEvent<TenantRuleset> {

    public static readonly NAME = "or-rules-editor-request-copy";

    constructor(request: TenantRuleset) {
        super(OrRulesEditorRequestCopyEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: request
        });
    }
}

export class OrRulesEditorRequestSaveEvent extends CustomEvent<TenantRuleset> {

    public static readonly NAME = "or-rules-editor-request-save";

    constructor(request: TenantRuleset) {
        super(OrRulesEditorRequestSaveEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: request
        });
    }
}

export class OrRulesEditorSelectionChangedEvent extends CustomEvent<TenantRuleset[]> {

    public static readonly NAME = "or-rules-editor-selection-changed";

    constructor(nodes: TenantRuleset[]) {
        super(OrRulesEditorSelectionChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: nodes
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrRulesEditorRuleInvalidEvent.NAME]: OrRulesEditorRuleInvalidEvent;
        [OrRulesEditorRuleUnsupportedEvent.NAME]: OrRulesEditorRuleUnsupportedEvent;
        [OrRulesEditorRequestSelectEvent.NAME]: OrRulesEditorRequestSelectEvent;
        [OrRulesEditorRequestAddEvent.NAME]: OrRulesEditorRequestAddEvent;
        [OrRulesEditorRequestDeleteEvent.NAME]: OrRulesEditorRequestDeleteEvent;
        [OrRulesEditorRequestCopyEvent.NAME]: OrRulesEditorRequestCopyEvent;
        [OrRulesEditorRequestSaveEvent.NAME]: OrRulesEditorRequestSaveEvent;
        [OrRulesEditorRuleChangedEvent.NAME]: OrRulesEditorRuleChangedEvent;
        [OrRulesEditorSelectionChangedEvent.NAME]: OrRulesEditorSelectionChangedEvent;
    }
}

// language=CSS
export const style = css`

    :host {
        display: flex;
        height: 100%;
        width: 100%;
        
        --internal-or-rules-editor-background-color: var(--or-rules-editor-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-rules-editor-text-color: var(--or-rules-editor-text-color, inherit);
        --internal-or-rules-editor-button-color: var(--or-rules-editor-button-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-rules-editor-invalid-color: var(--or-rules-editor-invalid-color, var(--or-app-color6, ${unsafeCSS(DefaultColor6)}));        
        --internal-or-rules-editor-panel-color: var(--or-rules-editor-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-rules-editor-line-color: var(--or-rules-editor-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        
        --internal-or-rules-editor-list-selected-color: var(--or-rules-editor-list-selected-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-rules-editor-list-text-color: var(--or-rules-editor-list-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-rules-editor-list-text-size: var(--or-rules-editor-list-text-size, 15px);

        --internal-or-rules-editor-list-button-size: var(--or-rules-editor-list-button-size, 24px);
        
        --internal-or-rules-editor-header-background-color: var(--or-rules-editor-header-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-rules-editor-header-height: var(--or-rules-editor-header-height, 80px);
        
        --or-panel-background-color: var(--internal-or-rules-editor-panel-color);
    }

    .shadow {
        -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        -moz-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
    }

    #rule-list-container {
        min-width: 300px;
        width: 300px;
        z-index: 2;
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-rules-editor-panel-color);
        color: var(--internal-or-rules-editor-list-text-color);

    }

    .rule-container {
        display: flex;
        flex-direction: column;
        flex-grow: 1;
    }

    or-rule-header {
        min-height: var(--internal-or-rules-editor-header-height);
        height: var(--internal-or-rules-editor-header-height);
    }
    
    .rule-editor-panel {
        display: flex;
        flex-grow: 1;
        background-color: var(--internal-or-rules-editor-background-color);
    }

    .section-container {
        flex: 1 0 0px;
    }

    .msg {
        display: flex;
        justify-content: center;
        align-items: center;        
        flex-direction: column;
        text-align: center;
        margin: auto;
    }
    
    @media only screen 
    and (min-device-width : 768px) 
    and (max-device-width : 1024px)  { 
        side-menu {
            min-width: 150px;
            width: 150px;
        }
    }
`;

@customElement("or-rules-editor")
class OrRulesEditor extends translate(i18next)(LitElement) {

    public static DEFAULT_RULESET_NAME = "New rule";

    static get styles() {
        return [
            style
        ];
    }

    public config?: RulesConfig;

    @property({type: String})
    public realm?: string;

    @property({type: Array})
    public selectedIds?: number[];

    @property({type: Boolean, attribute: false})
    private _isValidRule?: boolean;

    @query("#rules-list")
    private _rulesList!: OrRulesList;

    private _activeRuleset?: TenantRuleset;
    private _activeRules?: JsonRule[];
    private _activeModified: boolean = false;
    private _whenAssetDescriptors?: AssetDescriptor[];
    private _actionAssetDescriptors?: AssetDescriptor[];
    private _saveInProgress = false;

    private set activeRuleset(ruleset: TenantRuleset | undefined) {
        this._activeRuleset = ruleset;
        this._activeRules = undefined;
        this._activeModified = !!(ruleset && !ruleset.id);

        if (ruleset && ruleset.rules) {
            const rules = JSON.parse(ruleset.rules) as JsonRulesetDefinition;
            this._activeRules = rules.rules;
        }

        this.requestUpdate();
    }

    private get activeRuleset() {
        return this._activeRuleset;
    }

    private get activeRule() {
        return this._activeRules && this._activeRules.length > 0 ? this._activeRules[0] : undefined;
    }

    constructor() {
        super();

        this.addEventListener(OrRulesEditorRuleChangedEvent.NAME, this._onRuleChanged);
        this.addEventListener(OrRulesEditorRequestSelectEvent.NAME, this._onRequestSelect);
        this.addEventListener(OrRulesEditorRequestAddEvent.NAME, this._onRequestAdd);
        this.addEventListener(OrRulesEditorRequestDeleteEvent.NAME, this._onRequestDelete);
        this.addEventListener(OrRulesEditorRequestCopyEvent.NAME, this._onRequestCopy);
        this.addEventListener(OrRulesEditorRequestSaveEvent.NAME, this._onRequestSave);
        this.addEventListener(OrRulesEditorSelectionChangedEvent.NAME, this._onRuleSelectionChanged);
    }

    protected ruleEditorPanelTemplate(rule: JsonRule) {
        const targetTypeMap = this.getTargetTypeMap();
        const whenDescriptors = this.getAssetDescriptors(false);
        const actionDescriptors = this.getAssetDescriptors(true);

        return html`
            <div class="section-container">                                    
                <or-rule-when .rule="${rule}" .config="${this.config}" .assetDescriptors="${whenDescriptors}" ?readonly="${this.isReadonly("when")}"></or-rule-when>
            </div>
        
            <div class="section-container">              
                <or-rule-then-otherwise .rule="${rule}" .config="${this.config}" .targetTypeMap="${targetTypeMap}" .assetDescriptors="${actionDescriptors}" ?readonly="${this.isReadonly("then")}"></or-rule-then-otherwise>
            </div>
        `;
    }

    protected render() {

        return html`
          <div id="rule-list-container" class="shadow">
                <or-rule-list id="rules-list" .realm="${this.realm}" .selectedIds="${this.selectedIds}"></or-rule-list>
          </div>
          ${this.activeRule ? html`
                <div class="rule-container">
                    <or-rule-header class="shadow" .readonly="${this.isReadonly("header")}" .ruleset="${this.activeRuleset}" .rule="${this.activeRule}" .saveEnabled="${!this._saveInProgress && this._isValidRule && this._activeModified}"></or-rule-header>
                    <div class="rule-editor-panel">                    
                        ${this.ruleEditorPanelTemplate(this.activeRule)}
                    </div>
                </div>
          ` : html`
            <div class="msg"><or-translate value="noRuleSelected"></or-translate></div>
          `}
        `;
    }

    protected isReadonly(sectionName: string) {

        if (!manager.hasRole("write:rules")) {
            return true;
        }
        if (!this.config || !this.config.controls) {
            return false;
        }

        if (sectionName === "when") {
            return this.config.controls.whenReadonly === true;
        }

        if (sectionName === "then") {
            return this.config.controls.thenReadonly === true;
        }

        if (sectionName === "header") {
            return this.config.controls.headerReadonly === true;
        }
    }

    protected _onRuleChanged() {
        this._activeModified = true;
        let isValid = false;
        const rule  = this.activeRule;

        if (rule) {
            isValid = this._validateRuleset();
        }

        this._isValidRule = isValid;
        this.requestUpdate();
    }

    protected _validateRuleset(): boolean {

        const ruleset = this.activeRuleset;
        const rule = this.activeRule;

        if (!ruleset || !rule) {
            return false;
        }

        if (!ruleset.name || ruleset.name.length < 3 || ruleset.name.length > 255) {
            return false;
        }

        if (!rule.when) {
            return false;
        }

        if (!rule.then || rule.then.length === 0) {
            return false;
        }

        if (!this.validateConditionGroup(rule.when)) {
            return false;
        }

        if (!this.validateRuleActions(rule, rule.then)) {
            return false;
        }

        // TODO: Validate other rule sections
        return true;
    }

    protected validateConditionGroup(group: LogicGroup<RuleCondition>): boolean {
        if ((!group.items || group.items.length === 0) && (!group.groups || group.groups.length === 0)) {
            return false;
        }

        if (group.items) {
            for (const condition of group.items) {
                if (!condition.assets && !condition.datetime && !condition.timer) {
                    return false;
                }

                if (condition.assets && !this.validateAssetQuery(condition.assets, true, false)) {
                    return false;
                }

                if (condition.datetime && !this.validateValuePredicate(condition.datetime)) {
                    return false;
                }

                if (condition.timer && !Util.isTimeDuration(condition.timer)) {
                    return false;
                }
            }
        }

        if (group.groups) {
            for (const childGroup of group.groups) {
                if (!this.validateConditionGroup(childGroup)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected validateRuleActions(rule: JsonRule, actions?: RuleActionUnion[]): boolean {
        if (!actions) {
            return true;
        }

        for (const action of actions) {
            switch (action.action) {
                case "wait":
                    if (!action.millis) {
                        return false;
                    }
                    break;
                case "write-attribute":
                    if (!action.attributeName) {
                        return false;
                    }
                    if (!this.validateAssetTarget(rule, action.target)) {
                        return false;
                    }
                    break;
                case "notification":
                    // TODO: validate notification rule action
                    break;
                case "update-attribute":
                    if (!action.attributeName) {
                        return false;
                    }
                    if (!action.updateAction) {
                        return false;
                    }
                    if (!this.validateAssetTarget(rule, action.target)) {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
        }

        return true;
    }

    protected validateAssetTarget(rule: JsonRule, target?: RuleActionTarget): boolean {
        if (!target) {
            return true;
        }

        if (!target.assets && !target.ruleConditionTag) {
            return false;
        }

        if (target.assets) {
            return this.validateAssetQuery(target.assets, false, false);
        }

        if (target.matchedAssets) {
            return this.validateAssetQuery(target.matchedAssets, false, true);
        }

        const typesAndTags = this.getTypeAndTagsFromGroup(rule.when!);
        if (typesAndTags.findIndex((typeAndTag) => target.ruleConditionTag === typeAndTag[1]) < 0) {
            return false;
        }

        return true;
    }

    protected validateAssetQuery(query: AssetQuery, isWhen: boolean, isMatchedAssets: boolean): boolean {

        if (!query.types || query.types.length === 0) {
            return false;
        }

        if (isWhen) {
            if (!query.attributes || !query.attributes.items || query.attributes.items.length === 0) {
                return false;
            }
        } else {
            if (!query.ids || query.ids.length === 0 && !isMatchedAssets) {
                return false;
            }
        }

        if (query.types) {
            for (const type of query.types) {
                if (!type.value || !type.predicateType) {
                    return false;
                }
            }
        }

        if (query.attributes && query.attributes.items) {
            for (const attribute of query.attributes.items) {
                if (!attribute.name || !attribute.name.match || !attribute.name.value) {
                    return false;
                }
                if (!attribute.value || !this.validateValuePredicate(attribute.value)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected validateValuePredicate(valuePredicate: ValuePredicateUnion): boolean {
        switch (valuePredicate.predicateType) {
            case "string":
                return valuePredicate.match !== undefined && valuePredicate.value !== undefined;
            case "boolean":
                return valuePredicate.value !== undefined;
            case "string-array":
                return valuePredicate.predicates !== undefined && valuePredicate.predicates.length > 0 && valuePredicate.predicates.findIndex((p) => !p.match || p.value === undefined) === 0;
            case "datetime":
            case "number":
                return valuePredicate.operator !== undefined && valuePredicate.value !== undefined && (valuePredicate.operator !== AQO.BETWEEN || valuePredicate.rangeValue !== undefined);
            case "radial":
                return valuePredicate.radius !== undefined && valuePredicate.lat !== undefined && valuePredicate.lng !== undefined;
            case "rect":
                return valuePredicate.lngMax !== undefined && valuePredicate.latMax !== undefined && valuePredicate.lngMin !== undefined && valuePredicate.latMin !== undefined;
            case "object-value-key":
                return valuePredicate.key !== undefined;
            case "array":
                return (valuePredicate.index && !valuePredicate.value) || valuePredicate.value || valuePredicate.lengthEquals || valuePredicate.lengthLessThan || valuePredicate.lengthGreaterThan;
            case "value-empty":
            case "value-not-empty":
                return true;
            default:
                return false;
        }
    }

    protected _onRequestAdd() {
        const shouldContinue = this.okToLeaveActiveRule();

        if (!shouldContinue) {
            return;
        }
        let rule = this.config && this.config.templates && this.config.templates.rule ? JSON.parse(JSON.stringify(this.config.templates.rule)) as JsonRule : undefined;

        if (!rule) {
            rule = {

            };
        }

        const name = this.config && this.config.templates && this.config.templates.rulesetName ? this.config.templates.rulesetName : OrRulesEditor.DEFAULT_RULESET_NAME;
        rule.name = name;
        const rules: JsonRulesetDefinition = {
            rules: [rule]
        };

        const ruleset: TenantRuleset = {
            id: 0,
            type: "tenant",
            name: name,
            lang: RulesetLang.JSON,
            realm: manager.getRealm(),
            rules: JSON.stringify(rules)
        };
        this.activeRuleset = ruleset;
    }

    protected _onRequestSave(e: OrRulesEditorRequestSaveEvent) {
        if (!this.activeRuleset || !this.activeRule) {
            return;
        }

        this._saveInProgress = true;
        this.activeRule.name = this.activeRuleset.name;
        this._activeRules![0] = this.activeRule;
        const ruleset = this.activeRuleset;

        const rules: JsonRulesetDefinition = {
            rules: this._activeRules
        };

        ruleset.rules = JSON.stringify(rules);
        delete ruleset.lastModified;
        delete ruleset.createdOn;
        delete ruleset.status;
        delete ruleset.error;

        this._rulesList.disabled = true;

        if (ruleset.id) {
            manager.rest.api.RulesResource.updateTenantRuleset(ruleset.id!, ruleset).then((response) => {
                this._saveInProgress = false;
                this._rulesList.refresh();
                this._rulesList.disabled = false;
            });
        } else {
            manager.rest.api.RulesResource.createTenantRuleset(ruleset).then((response) => {
                ruleset.id = response.data;
                this._saveInProgress = false;
                this._rulesList.refresh();
                this._rulesList.disabled = false;
            });
        }
    }

    protected _onRequestCopy(e: OrRulesEditorRequestCopyEvent) {
        const shouldContinue = this.okToLeaveActiveRule();

        if (!shouldContinue) {
            return;
        }

        let ruleset = JSON.parse(JSON.stringify(e.detail)) as TenantRuleset;
        delete ruleset.lastModified;
        delete ruleset.createdOn;
        delete ruleset.status;
        delete ruleset.error;
        this.activeRuleset = ruleset;
    }

    protected async _onRequestDelete(event: OrRulesEditorRequestDeleteEvent) {

        let confirmed = this.okToLeaveActiveRule();

        if (!confirmed) {
            return;
        }

        confirmed = !this.activeRuleset!.id || window.confirm(i18next.t("confirmDelete"));

        if (!confirmed) {
            return;
        }

        this.activeRuleset = undefined;

        const rulesetsToDelete = event.detail;

        // We need to call the backend so disable list until done
        this._rulesList.disabled = true;
        for (let ruleset of rulesetsToDelete) {
            await manager.rest.api.RulesResource.deleteTenantRuleset(ruleset.id!);
        }
        this._rulesList.refresh();
        this._rulesList.disabled = false;
    }

    protected _onRequestSelect(event: OrRulesEditorRequestSelectEvent) {
        event.detail.allow = this.okToLeaveActiveRule();
    }

    protected _onRuleSelectionChanged(event: OrRulesEditorSelectionChangedEvent) {
        this.activeRuleset = event.detail.length == 1 ? event.detail[0] : undefined;
    }

    protected okToLeaveActiveRule() {
        return !this._activeModified || window.confirm(i18next.t("continueWithoutSaving"));
    }

    protected getTypeAndTagsFromGroup(group: LogicGroup<RuleCondition>): [string, string?][] {
        if (!group) {
            return [];
        }

        let typesAndTags: [string, string?][] = [];

        if (group.items) {
            for (const condition of group.items) {
                const type = getAssetTypeFromQuery(condition.assets);
                if (type) {
                    typesAndTags.push([type, condition.tag]);
                }
            }
        }

        if (group.groups) {
            for (const condition of group.groups) {
                typesAndTags = typesAndTags.concat(this.getTypeAndTagsFromGroup(condition));
            }
        }

        return typesAndTags;
    }

    protected getTargetTypeMap(): [string, string?][] {
        if (!this.activeRule || !this.activeRule.when) {
            return [];
        }

        return this.getTypeAndTagsFromGroup(this.activeRule.when);
    }

    protected getAssetDescriptors(useActionConfig: boolean) {
        if (useActionConfig) {
            if (this._actionAssetDescriptors) {
                return this._actionAssetDescriptors;
            }
            this._actionAssetDescriptors = getAssetDescriptors(this.config, useActionConfig);
            return this._actionAssetDescriptors;
        }

        if (this._whenAssetDescriptors) {
            return this._whenAssetDescriptors;
        }

        this._whenAssetDescriptors = getAssetDescriptors(this.config, useActionConfig);
        return this._whenAssetDescriptors;
    }
}
