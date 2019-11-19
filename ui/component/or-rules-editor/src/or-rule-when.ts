import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult} from "lit-element";
import {AssetDescriptor, JsonRule, LogicGroup, LogicGroupOperator, RuleCondition, AssetType} from "@openremote/model";
import {OrRulesEditorRuleChangedEvent, RulesConfig} from "./index";
import {buttonStyle} from "./style";
import "./or-rule-condition";
import i18next from "i18next";
import {InputType} from "@openremote/or-input";

// language=CSS
const style = css`
    
    :host {
        display: block;
    }
    
    ${buttonStyle}
    
    .rule-group-items {
        display: flex;
        flex-direction: column;
    }
    
    .rule-group .rule-group-items > div {
        margin: 10px 0;
    }
    
    .rule-condition {
        display: flex;
        padding-right: 5px;
    }
     
    .rule-condition > * {
        flex-grow: 0;
    }
    
    .rule-condition > or-rule-condition {
        flex-grow: 1;
    }
    
    or-rule-condition {
        margin: 10px 0;
    }

    .rule-group-item > or-icon {
        padding-left: 17px;
    }
    
    or-icon.small {
        --or-icon-width: 14px;
        --or-icon-height: 14px;
    }
    
    or-panel {
        margin: 20px;
        position: relative;
    }
    
    or-panel > .remove-button {
        position: absolute;
        right: 0;
        top: 0;
        width: 24px;
        height: 24px;
        transform: translate(50%, -50%);
    }
`;

@customElement("or-rule-when")
class OrRuleWhen extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object})
    public rule?: JsonRule;

    public readonly?: boolean;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    protected ruleGroupTemplate(group: LogicGroup<RuleCondition>, parentGroup?: LogicGroup<RuleCondition>): TemplateResult | undefined {

        if (!group) {
            return html``;
        }

        const isTopLevel = !parentGroup;
        const showAddCondition = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddCondition !== true);
        const showRemoveCondition = !this.readonly && group.items && group.items.length > 1;
        const showRemoveGroup = !this.readonly;
        const showAddGroup = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddGroup !== true);

        let wrapper: (content: TemplateResult, item: LogicGroup<RuleCondition> | RuleCondition, parent: LogicGroup<RuleCondition>, isGroup: boolean, isFirst: boolean) => TemplateResult;

        if (isTopLevel) {
            wrapper = (content, item, parent, isGroup, isFirst) => {
                return html`
                    <or-panel .heading="${i18next.t(isFirst ? "when": "orWhen")}...">
                        ${showRemoveGroup ? html`
                            <button class="button-clear remove-button" @click="${() => this.removeItem(item, parent, isGroup)}">
                                <or-icon icon="close-circle"></or-icon>  
                            </button>
                        ` : ``}
                        ${content}
                    </or-panel>`;
            };
        } else {
            wrapper = (content, item, parent, isGroup, isFirst) => {
                return html`
                    ${!isFirst ? html`
                        <or-icon class="small" icon="ampersand"></or-icon>
                    ` : ``}
                    ${content}
                `;
                return content;
            }
        }

        let groupsTemplate: TemplateResult | string = ``;
        let itemsTemplate: TemplateResult | string = ``;
        let addTemplate: TemplateResult | string = ``;
        let isFirst = true;

        if (group.groups && group.groups.length > 0) {
            groupsTemplate = html`
                ${group.groups.map((childGroup: LogicGroup<RuleCondition>, index) => {
                    const content = html`
                        <div class="rule-group-item">
                            ${this.ruleGroupTemplate(childGroup, group)}
                        </div>
                    `;
                    return wrapper(content, childGroup, group, true, index == 0);
                })}
            `;
            isFirst = false;
        }

        if (group.items && group.items.length > 0) {
            itemsTemplate = html`
                ${group.items.map((condition: RuleCondition, index) => {
                    const content = html`
                        <div class="rule-group-item">
                            <div class="rule-condition">
                                <or-rule-condition .config="${this.config}" .assetDescriptors="${this.assetDescriptors}" .ruleCondition="${condition}" .readonly="${this.readonly}" ></or-rule-condition>
                                ${showRemoveCondition ? html`
                                    <button class="button-clear" @click="${() => this.removeItem(condition, group, false)}"><or-icon icon="close-circle"></or-icon></input>
                                ` : ``}
                            </div>
                        </div>
                    `;
                    return wrapper(content, condition, group, true, isFirst && index == 0);
                })}
            `;
            isFirst = false;
        }

        if (isTopLevel && showAddGroup) {
            addTemplate = html`
                <or-panel>
                    <or-input type="${InputType.BUTTON}" icon="plus" @click="${() => this.addGroup(group)}"></or-input>
                    <strong>${i18next.t(isFirst ? "when": "orWhen")}...</strong>
                </or-panel>
            `;
        }

        if (!isTopLevel && showAddCondition) {
            addTemplate = html`
                <or-input type="${InputType.BUTTON}" .label="${i18next.t("rulesEditorAddCondition")}" @click="${() => this.addCondition(group)}"></or-input>
            `;
        }

        return html`
            ${groupsTemplate}
            ${itemsTemplate}
            ${addTemplate}
        `;
    }

    protected dateTimePredicateTemplate() {
        return html`<span>DATE TIME PREDICATE NOT IMPLEMENTED</span>`;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("rule")) {
            if (this.rule) {
                if (!this.rule.when) {
                    this.rule.when = {
                        operator: LogicGroupOperator.OR
                    };
                } else {
                    // Check this is a rule compatible with this editor
                    if (!OrRuleWhen._isRuleWhenCompatible(this.rule.when)) {
                        this.rule = undefined;
                    }
                }
            }
        }

        return super.shouldUpdate(_changedProperties);
    }

    protected render() {
        if (!this.rule || !this.rule.when) {
            return html``;
        }

        return html`
            <div>
                ${this.ruleGroupTemplate(this.rule.when!)}
            </div>
        `;
    }

    /**
     * Currently only support a top level OR group and then N child AND groups with no more descendants
     */
    protected static _isRuleWhenCompatible(when: LogicGroup<RuleCondition>): boolean {
        if (when.operator !== LogicGroupOperator.OR) {
            console.warn("Incompatible rule: when operator not set to 'OR'");
            return false;
        }

        if (when.items && when.items.length > 0) {
            console.warn("Incompatible rule: when items not supported");
            return false;
        }

        if (when.groups && when.groups.find((g) => !this._isWhenGroupCompatible(g))) {
            console.warn("Incompatible rule: when groups incompatible");
            return false;
        }

        return true;
    }

    protected static _isWhenGroupCompatible(group: LogicGroup<RuleCondition>): boolean {
        if (group.operator === LogicGroupOperator.OR) {
            console.warn("Incompatible rule: when group operator not set to 'AND'");
            return false;
        }

        if (group.groups && group.groups.length > 0) {
            console.warn("Incompatible rule: when group nested groups not supported");
            return false;
        }

        return true;
    }

    private addGroup(parent: LogicGroup<RuleCondition>) {
        if (!this.rule || !this.rule.when || parent !== this.rule.when) {
            return;
        }

        let newGroup: LogicGroup<RuleCondition> = {};

        if (this.config && this.config.templates && this.config.templates.whenGroup) {
            newGroup = JSON.parse(JSON.stringify(this.config.templates.whenGroup)) as LogicGroup<RuleCondition>;
        }

        // Force to AND as this is what the editor supports
        newGroup.operator = LogicGroupOperator.AND;

        // Clear any child groups
        newGroup.groups = undefined;

        // Add an item if none exist
        if (!newGroup.items || newGroup.items!.length === 0) {
            this.addCondition(newGroup, true);
        }

        const group = parent as LogicGroup<RuleCondition>;
        if (!group.groups) {
            group.groups = [];
        }
        group.groups.push(newGroup);

        this.dispatchEvent(new OrRulesEditorRuleChangedEvent());
        this.requestUpdate();
    }

    private removeItem(item: LogicGroup<RuleCondition> | RuleCondition, parent: LogicGroup<RuleCondition>, isGroup: boolean) {

        let removed = false;

        if (parent) {
            if (isGroup) {
                const index = parent.groups!.indexOf(item as LogicGroup<RuleCondition>);
                parent.groups!.splice(index, 1);
                removed = index >= 0;
            } else {
                const index = parent.items!.indexOf(item as RuleCondition);
                parent.items!.splice(index, 1);
                removed = index >= 0;
            }

            if (removed) {
                this.dispatchEvent(new OrRulesEditorRuleChangedEvent());
                this.requestUpdate();
            }
        }
    }

    private addCondition(parent: LogicGroup<RuleCondition>, silent?: boolean) {
        if (!parent) {
            return;
        }

        if (!parent.items) {
            parent.items = [];
        }

        let newTrigger: RuleCondition = {};

        if (this.config && this.config.templates && this.config.templates.whenCondition) {
            newTrigger = JSON.parse(JSON.stringify(this.config.templates.whenCondition)) as RuleCondition;
        }

        // Populate the condition with something to help the UI
        if (!newTrigger.timer && !newTrigger.datetime && !newTrigger.assets) {
            newTrigger.assets = {
                types: [
                    {
                        predicateType: "string",
                        value: AssetType.THING.type
                    }
                ]
            };
        }

        parent.items.push(newTrigger);
        if (!silent) {
            this.dispatchEvent(new OrRulesEditorRuleChangedEvent());
            this.requestUpdate();
        }
    }
}
