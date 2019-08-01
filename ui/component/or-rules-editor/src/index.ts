import {customElement, html, LitElement, property} from "lit-element";
import i18next from "i18next";
import "@openremote/or-select";
import "@openremote/or-icon";
import {
    AssetDescriptor,
    AttributeDescriptor,
    AttributeValueDescriptor,
    BaseAssetQueryOperator,
    JsonRulesetDefinition,
    LogicGroup,
    MetaItemDescriptor,
    MetaItemType,
    NewAssetQuery,
    Rule,
    RuleActionTarget,
    RuleActionUnion,
    RuleCondition,
    RulesetLang,
    TenantRuleset,
    ValuePredicateUnion,
    ValueType
} from "@openremote/model";
import openremote, {AssetModelUtil} from "@openremote/core";
import {isTimeDuration} from "@openremote/core/dist/util";
import rest from "@openremote/rest";
import "@openremote/or-translate";
import {translate} from "@openremote/or-translate/dist/translate-mixin";
import "./or-rule-list";
import "./or-rule-when";
import "./or-rule-actions";
import "./or-rule-header";
import "./or-rule-section";
import {rulesEditorStyle} from "./style";

export const enum ConditionType {
    ASSET_QUERY = "assetQuery",
    DATE_TIME = "datetime",
    TIMER = "timer"
}

export const enum ActionType {
    WAIT = "wait",
    NOTIFICATION = "notification",
    WRITE_ATTRIBUTE = "writeAttribute",
    UPDATE_ATTRIBUTE = "updateAttribute"
}

export const enum ActionTargetType {
    TRIGGER_ASSETS = "triggerAssets",
    TAGGED_ASSETS = "taggedAssets",
    USERS = "users",
    OTHER_ASSETS = "otherAssets"
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
        allowedActionTargets?: ActionTargetType[];
        allowedAssetQueryOperators?: Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]>;
        hideActionTargetOptions?: boolean;
        hideActionUpdateOptions?: boolean;
        hideConditionTypeOptions?: boolean;
        hideThenAddAction?: boolean;
        hideWhenAddCondition?: boolean;
        hideWhenAddAttribute?: boolean;
        hideWhenAddGroup?: boolean;
        hideWhenGroupOutline?: boolean;
    };
    descriptors?: {
        all?: RulesDescriptorSection;
        when?: RulesDescriptorSection;
        action?: RulesDescriptorSection;
    };
    templates?: {
        rule?: Rule;
        rulesetName?: string;
        whenGroup?: LogicGroup<RuleCondition>;
        whenCondition?: RuleCondition;
        whenAssetQuery?: NewAssetQuery;
        then?: RuleActionUnion;
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

export function getAssetTypeFromQuery(query?: NewAssetQuery) {
    return query && query.types && query.types.length > 0 && query.types[0] ? query.types[0].value : undefined;
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

function addOrReplaceMetaItemDescriptor(metaItemDescriptors: MetaItemDescriptor[], metaItemDescriptor: MetaItemDescriptor) {
    const index = metaItemDescriptors.findIndex((mid) => mid.urn === metaItemDescriptor.urn);
    if (index >= 0) {
        metaItemDescriptors.splice(index, 1);

    }
    metaItemDescriptors.push(metaItemDescriptor);
}

export class OrRuleChangedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rule-changed";

    constructor() {
        super(OrRuleChangedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrRuleChangedEvent.NAME]: OrRuleChangedEvent;
    }
}

@customElement("or-rules-editor")
class OrRulesEditor extends translate(i18next)(LitElement) {

    public static DEFAULT_RULESET_NAME = "New rule";

    static get styles() {
        return [
            rulesEditorStyle
        ];
    }

    public config?: RulesConfig;

    @property({type: Number})
    private value: number = 0;

    @property({type: Array, attribute: false})
    private _rulesets?: TenantRuleset[];

    @property({type: Boolean, attribute: false})
    private isValidRule?: boolean;

    private _activeRuleset?: TenantRuleset;
    private _activeRules?: Rule[];
    private _activeModified: boolean = false;
    private _whenAssetDescriptors?: AssetDescriptor[];
    private _actionAssetDescriptors?: AssetDescriptor[];
    private _saveInProgress = false;

    private set activeRuleset(ruleset: TenantRuleset | undefined) {
        this._activeRuleset = ruleset;
        this._activeRules = undefined;
        this._activeModified = false;

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
        this.readRules();

        this.addEventListener(OrRuleChangedEvent.NAME, this.onRuleChanged);
        this.addEventListener("rules:set-active-rule", this.setActiveRuleset);
        this.addEventListener("rules:create-rule", this.createRuleset);
        this.addEventListener("rules:save-ruleset", this.saveRuleset);
        this.addEventListener("rules:delete-rule", this.deleteRuleset);
    }

    protected ruleEditorPanelTemplate(rule: Rule) {
        const thenTemplate = this.config && this.config.templates && this.config.templates.then ? this.config.templates.then : undefined;
        const targetTypeMap = this.getTargetTypeMap();
        const whenDescriptors = this.getAssetDescriptors(false);
        const actionDescriptors = this.getAssetDescriptors(true);
        const thenAllowAdd = !this.config || !this.config.controls || this.config.controls.hideThenAddAction !== true;

        return html`
            <div class="section-container">
                <or-rule-section heading="${i18next.t("when")}...">                    
                    <or-rule-when .rule="${rule}" .config="${this.config}" .assetDescriptors="${whenDescriptors}" ?readonly="${this.isReadonly("when")}"></or-rule-when> 
                </or-rule-section>
            </div>
        
            <div class="section-container">
                <or-rule-section heading="${i18next.t("then")}...">                    
                    <or-rule-actions .actions="${rule.then}" .config="${this.config}" .newTemplate="${thenTemplate}" .allowAdd="${thenAllowAdd}" .targetTypeMap="${targetTypeMap}" .assetDescriptors="${actionDescriptors}" ?readonly="${this.isReadonly("then")}"></or-rule-actions>
                </or-rule-section>
            </div>
        `;
    }

    protected render() {

        return html`
          <div id="rule-list-container" class="shadow">
                <or-rule-list .rulesets="${this._rulesets}" .ruleset="${this.activeRuleset}" ></or-rule-list>
                <div id="bottom-toolbar">
                    ${openremote.hasRole("write:rules") ? html`
                      <button @click="${this.deleteRuleset}"><or-icon icon="delete"></or-icon></button>
                      <button style="margin-left: auto;" @click="${this.createRuleset}"><or-icon icon="plus"></or-icon></button>
                    ` : ``}
                </div>
          </div>
          ${this.activeRule ? html`
                <div class="rule-container">
                    <or-rule-header class="shadow" .readonly="${this.isReadonly("header")}" .ruleset="${this.activeRuleset}" .rule="${this.activeRule}" .saveEnabled="${!this._saveInProgress && this.isValidRule && this._activeModified}"></or-rule-header>
                    <div class="rule-editor-panel">                    
                        ${this.ruleEditorPanelTemplate(this.activeRule)}
                    </div>
                </div>
          ` : html`
            <div class="center-center">
                <h3 style="font-weight: normal;">Kies links een profiel of maak een nieuw profiel aan.</h3>
                ${openremote.hasRole("write:rules") ? html`
                    <button style="margin: auto;" @click="${this.createRuleset}">profiel aanmaken</button>
                ` : ``}
            </div>
          `}
        `;
    }

    protected isReadonly(sectionName: string) {

        if (!openremote.hasRole("write:rules")) {
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

    protected onRuleChanged() {
        this._activeModified = true;
        let isValid = false;
        const rule  = this.activeRule;

        if (rule) {
            isValid = this.validateRuleset();
        }

        this.isValidRule = isValid;
        this.requestUpdate();
    }

    protected validateRuleset(): boolean {

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

        if (!rule.then) {
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
        if (!group.items || group.items.length === 0) {
            return false;
        }

        for (const condition of group.items) {
            if (!condition.assets && !condition.datetime && !condition.timer) {
                return false;
            }

            if (condition.assets && !this.validateAssetQuery(condition.assets)) {
                return false;
            }

            if (condition.datetime && !this.validateValuePredicate(condition.datetime)) {
                return false;
            }

            if (condition.timer && !isTimeDuration(condition.timer)) {
                return false;
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

    protected validateRuleActions(rule: Rule, actions?: RuleActionUnion[]): boolean {
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

    protected validateAssetTarget(rule: Rule, target?: RuleActionTarget): boolean {
        if (!target) {
            return true;
        }

        if (!target.assets && !target.ruleConditionTag) {
            return false;
        }

        if (target.assets) {
            return this.validateAssetQuery(target.assets);
        }

        const typesAndTags = this.getTypeAndTagsFromGroup(rule.when!);
        if (typesAndTags.findIndex((typeAndTag) => target.ruleConditionTag === typeAndTag[1]) < 0) {
            return false;
        }

        return true;
    }

    protected validateAssetQuery(query?: NewAssetQuery): boolean {
        if (!query) {
            return true;
        }

        if (!query.types || query.types.length === 0) {
            return false;
        }

        if (!query.attributes || !query.attributes.items || query.attributes.items.length === 0) {
            return false;
        }

        if (query.types) {
            for (const type of query.types) {
                if (!type.match || !type.value) {
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
                return valuePredicate.operator !== undefined && valuePredicate.value !== undefined && (valuePredicate.operator !== BaseAssetQueryOperator.BETWEEN || valuePredicate.rangeValue !== undefined);
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

    protected readRules() {
        rest.api.RulesResource.getTenantRulesets(openremote.config.realm, {
            language: RulesetLang.JSON,
            fullyPopulate: true
        }).then((response: any) => {
            if (response && response.data) {
                this._rulesets = response.data;

                if (this._rulesets && this.activeRuleset) {
                    this.activeRuleset = this._rulesets.find((ruleset) => ruleset.id === this.activeRuleset!.id);
                }
            }
        }).catch((reason: any) => {
            console.error("Error: " + reason);
        });
    }

    protected async createRuleset() {
        const shouldContinue = await this.okToLeaveActiveRule();
        if (!shouldContinue) {
            return;
        }

        if (this._rulesets) {

            let rule = this.config && this.config.templates && this.config.templates.rule ? JSON.parse(JSON.stringify(this.config.templates.rule)) as Rule : undefined;

            if (!rule) {
                rule = {};
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
                realm: openremote.getRealm(),
                rules: JSON.stringify(rules)
            };

            this._rulesets = [...this._rulesets, ruleset];
            this.activeRuleset = ruleset;
        }
    }

    protected saveRuleset(e: Event) {
        e.preventDefault();

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

        if (ruleset.id) {
            rest.api.RulesResource.updateTenantRuleset(ruleset.id!, ruleset).then((response) => {
                this._saveInProgress = false;
                this.readRules();
            });
        } else {
            rest.api.RulesResource.createTenantRuleset(ruleset).then((response) => {
                ruleset.id = response.data;
                this._saveInProgress = false;
                this.readRules();
            });
        }
    }

    protected deleteRuleset() {

        if (!this.activeRuleset) {
            return;
        }

        let confirmed = !this._activeModified || window.confirm(i18next.t("continueWithoutSaving"));

        if (!confirmed) {
            return;
        }

        confirmed = !this.activeRuleset.id || window.confirm(i18next.t("confirmDelete"));

        if (!confirmed) {
            return;
        }

        const id = this.activeRuleset.id;
        const index = this._rulesets!.findIndex((ruleset) => ruleset.id === this.activeRuleset!.id);
        if (index >= 0) {
            this._rulesets!.splice(index, 1);
        }
        this.activeRuleset = undefined;

        if (id) {
            rest.api.RulesResource.deleteTenantRuleset(id).then((response) => {
                this.readRules();
            });
        }
    }

    protected async setActiveRuleset(e: any) {
        if (this.okToLeaveActiveRule()) {
            this.activeRuleset = e.detail.ruleset;
        }
    }

    protected okToLeaveActiveRule() {

        const confirmed = !this._activeModified || window.confirm(i18next.t("continueWithoutSaving"));

        if (confirmed) {
            if (this.activeRuleset && this.activeRuleset.id === 0) {
                const index = this._rulesets!.findIndex((ruleset) => ruleset.id === this.activeRuleset!.id);
                if (index >= 0) {
                    this._rulesets!.splice(index, 1);
                    this._rulesets = [...this._rulesets!];
                }
            }
        }
        return confirmed;
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
