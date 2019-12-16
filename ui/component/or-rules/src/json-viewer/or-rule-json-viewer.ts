import {customElement, html, LitElement, property, TemplateResult, css} from "lit-element";
import {
    getAssetDescriptors,
    getAssetTypeFromQuery,
    OrRulesRuleChangedEvent,
    OrRulesRuleUnsupportedEvent,
    RulesConfig,
    RuleView
} from "../index";
import {
    AssetDescriptor,
    AssetQuery,
    AssetQueryOperator as AQO,
    JsonRule,
    JsonRulesetDefinition,
    LogicGroup,
    RuleActionTarget,
    RuleActionUnion,
    RuleCondition,
    RulesetUnion,
    ValuePredicateUnion
} from "@openremote/model";
import {Util} from "@openremote/core";
import "./or-rule-when";
import "./or-rule-then-otherwise";
import "@openremote/or-panel";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";

export class OrRulesJsonRuleChangedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-json-rule-changed";

    constructor() {
        super(OrRulesJsonRuleChangedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrRulesJsonRuleChangedEvent.NAME]: OrRulesJsonRuleChangedEvent;
    }
}

const style = css`
    :host {
        display: flex;
        width: calc(100% - 20px);
        padding: 0px 10px;
        margin-top: -10px;
    }
    
    :host > * {
        flex: 1;
    }

    @media screen and (max-width: 1400px) {
        :host > * {
            flex-grow: 0;
        }

        :host {
            flex-direction: column;
        }
    }
`;

@customElement("or-rule-json-viewer")
export class OrRuleJsonViewer extends translate(i18next)(LitElement) implements RuleView {

    static get styles() {
        return style;
    }

    @property({attribute: false})
    public readonly?: boolean;

    @property({attribute: false})
    public config?: RulesConfig;

    @property({attribute: false})
    protected _ruleset!: RulesetUnion;

    protected _rule!: JsonRule;
    protected _unsupported = false;
    protected _assetDescriptors?: AssetDescriptor[];
    protected _whenAssetDescriptors?: AssetDescriptor[];
    protected _actionAssetDescriptors?: AssetDescriptor[];

    constructor() {
        super();
        this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
    }

    connectedCallback(): void {
        super.connectedCallback();

        if (this._unsupported) {
            this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
        }
    }

    public set ruleset(ruleset: RulesetUnion) {
        if (this._ruleset === ruleset) {
            return;
        }

        this._unsupported = false;
        this._ruleset = ruleset;

        if (!ruleset.rules) {
            // New ruleset so start a new rule
            if (this.config && this.config.json && this.config.json.rule) {
                this._rule = JSON.parse(JSON.stringify(this.config.json.rule)) as JsonRule;
            } else {
                this._rule = {};
            }
        } else {
            try {
                const rules = JSON.parse(ruleset.rules) as JsonRulesetDefinition;
                if (!rules.rules || rules.rules.length > 1) {
                    if (this.isConnected) {
                        this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
                    } else {
                        this._unsupported = true;
                    }
                    return;
                }
                this._rule = rules.rules[0];
                this.requestUpdate();
            } catch (e) {
                console.error("Invalid JSON rules, failed to parse: " + e);
                if (this.isConnected) {
                    this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
                } else {
                    this._unsupported = true;
                }
            }
        }
    }

    protected render(): TemplateResult | void {

        if (!this._rule) {
            return html``;
        }

        const targetTypeMap = this.getTargetTypeMap();
        const whenDescriptors = this.getAssetDescriptors(false);
        const actionDescriptors = this.getAssetDescriptors(true);

        return html`
            <div class="section-container">                                    
                <or-rule-when .rule="${this._rule}" .config="${this.config}" .assetDescriptors="${whenDescriptors}" ?readonly="${this.readonly}"></or-rule-when>
            </div>
        
            <div class="section-container">              
                <or-rule-then-otherwise .rule="${this._rule}" .config="${this.config}" .targetTypeMap="${targetTypeMap}" .assetDescriptors="${actionDescriptors}" ?readonly="${this.readonly}"></or-rule-then-otherwise>
            </div>
        `;
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

    protected getTargetTypeMap(): [string, string?][] {
        if (!this._rule.when) {
            return [];
        }

        return this.getTypeAndTagsFromGroup(this._rule.when);
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

    public beforeSave() {
        if (!this._rule) {
            return;
        }

        this._rule.name = this._ruleset.name;

        const jsonRules: JsonRulesetDefinition = {
            rules: [this._rule]
        };

        this._ruleset.rules = JSON.stringify(jsonRules);
    }

    public validate(): boolean {

        const rule = this._rule;

        if (!rule) {
            return false;
        }

        if (!rule.when) {
            return false;
        }

        if (!rule.then || rule.then.length === 0) {
            return false;
        }

        if (!this._validateConditionGroup(rule.when)) {
            return false;
        }

        if (!this._validateRuleActions(rule, rule.then)) {
            return false;
        }

        // TODO: Validate other rule sections
        return true;
    }

    protected _onJsonRuleChanged() {
        let valid = this.validate();
        this.dispatchEvent(new OrRulesRuleChangedEvent(valid));
    }

    protected _validateConditionGroup(group: LogicGroup<RuleCondition>): boolean {
        if ((!group.items || group.items.length === 0) && (!group.groups || group.groups.length === 0)) {
            return false;
        }

        if (group.items) {
            for (const condition of group.items) {
                if (!condition.assets && !condition.datetime && !condition.timer) {
                    return false;
                }

                if (condition.assets && !this._validateAssetQuery(condition.assets, true, false)) {
                    return false;
                }

                if (condition.datetime && !this._validateValuePredicate(condition.datetime)) {
                    return false;
                }

                if (condition.timer && !Util.isTimeDuration(condition.timer)) {
                    return false;
                }
            }
        }

        if (group.groups) {
            for (const childGroup of group.groups) {
                if (!this._validateConditionGroup(childGroup)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected _validateRuleActions(rule: JsonRule, actions?: RuleActionUnion[]): boolean {
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
                    if (!this._validateAssetTarget(rule, action.target)) {
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
                    if (!this._validateAssetTarget(rule, action.target)) {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
        }

        return true;
    }

    protected _validateAssetTarget(rule: JsonRule, target?: RuleActionTarget): boolean {
        if (!target) {
            return true;
        }

        if (!target.assets && !target.ruleConditionTag) {
            return false;
        }

        if (target.assets) {
            return this._validateAssetQuery(target.assets, false, false);
        }

        if (target.matchedAssets) {
            return this._validateAssetQuery(target.matchedAssets, false, true);
        }

        const typesAndTags = this.getTypeAndTagsFromGroup(rule.when!);
        if (typesAndTags.findIndex((typeAndTag) => target.ruleConditionTag === typeAndTag[1]) < 0) {
            return false;
        }

        return true;
    }

    protected _validateAssetQuery(query: AssetQuery, isWhen: boolean, isMatchedAssets: boolean): boolean {

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
                if (!attribute.value || !this._validateValuePredicate(attribute.value)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected _validateValuePredicate(valuePredicate: ValuePredicateUnion): boolean {
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
}