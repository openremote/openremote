import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {AssetDescriptor, JsonRule, LogicGroup, LogicGroupOperator, RuleCondition, WellknownAssets, AssetTypeInfo} from "@openremote/model";
import {OrRulesRuleUnsupportedEvent, RulesConfig} from "../index";
import {buttonStyle} from "../style";
import "./or-rule-condition";
import i18next from "i18next";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {getWhenTypesMenu, updateRuleConditionType} from "./or-rule-condition";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import { translate } from "@openremote/or-translate";

enum ResetOption {
    NO_LONGER_MATCHES = "noLongerMatches",
    VALUE_CHANGES = "valueChanges"
}

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
        margin-bottom: 10px;
    }

    .rule-group-item > or-icon {
        padding-left: 17px;
    }
    
    or-icon.small {
        --or-icon-width: 14px;
        --or-icon-height: 14px;
    }
    
    or-panel {
        margin: 10px 10px 20px 10px;
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

    .rule-condition:hover .button-clear {
        visibility: visible;
    }

    or-panel:hover .remove-button.button-clear {
        visibility: visible;
    }
    
    .add-button-wrapper {
        display: flex;
        align-items: center;
        white-space: nowrap;
    }
    
    .add-button-wrapper > * {
        margin-right: 6px;
    }
    
    .add-button-wrapper or-mwc-menu {
        text-transform: capitalize;
    }

    strong {
        margin: var(--internal-or-panel-heading-margin);
        font-size: var(--internal-or-panel-heading-font-size);
    }
`;

@customElement("or-rule-when")
class OrRuleWhen extends translate(i18next)(LitElement) {

    static get styles() {
        return style;
    }

    @property({type: Object})
    public rule?: JsonRule;

    @property({type: Boolean})
    public readonly?: boolean;

    public config?: RulesConfig;

    @property({type: Object, attribute: false})
    public assetInfos?: AssetTypeInfo[];

    protected ruleGroupTemplate(group: LogicGroup<RuleCondition>, parentGroup?: LogicGroup<RuleCondition>): TemplateResult | undefined {

        if (!group) {
            return html``;
        }

        const isTopLevel = !parentGroup;
        const showAddCondition = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddCondition !== true);
        const showRemoveCondition = !this.readonly;
        const showRemoveGroup = !this.readonly;

        let wrapper: (content: TemplateResult, item: LogicGroup<RuleCondition> | RuleCondition, parent: LogicGroup<RuleCondition>, isGroup: boolean, isFirst: boolean) => TemplateResult;

        if (isTopLevel) {
            wrapper = (content, item, parent, isGroup, isFirst) => {
                return html`
                    <or-panel .heading="${i18next.t(isFirst ? "when" : "orWhen")}...">
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
                                <or-rule-condition .config="${this.config}" .assetInfos="${this.assetInfos}" .ruleCondition="${condition}" .readonly="${this.readonly}" ></or-rule-condition>
                                ${showRemoveGroup ? html`
                                    <button class="button-clear ${showRemoveCondition ? "" : "hidden"}" @click="${() => this.removeItem(condition, group, false)}"><or-icon icon="close-circle"></or-icon></input>
                                ` : ``}
                            </div>
                        </div>
                    `;
                    return wrapper(content, condition, group, true, isFirst && index === 0);
                })}
            `;
            isFirst = false;
        }

        if (!isTopLevel && showAddCondition) {
            addTemplate = html`
                <span class="add-button-wrapper">
                    ${getContentWithMenuTemplate(
                        html`<or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"
                                           .label="${i18next.t("rulesEditorAddCondition")}"></or-mwc-input>`,
                        getWhenTypesMenu(this.config, this.assetInfos),
                        undefined,
                        (value) => this.addCondition(group, value as string))}
                </span>
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

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
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
                        this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
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

        const showAddGroup = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddGroup !== true);

        return html`
            <div>
                ${this.ruleGroupTemplate(this.rule.when)}
            </div>
            
            ${!showAddGroup ? `` : html`
                <or-panel>
                    <strong>${i18next.t(!this.rule.when.groups || this.rule.when.groups.length === 0 ? "when" : "orWhen")}...</strong>
                    <span class="add-button-wrapper">
                        ${getContentWithMenuTemplate(
                            html`<or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"></or-mwc-input>`,
                            getWhenTypesMenu(this.config, this.assetInfos),
                            undefined,
                            (value) => this.addGroup(this.rule!.when!, value as string))}
                    </span>
                </or-panel>
            `}
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

    private addGroup(parent: LogicGroup<RuleCondition>, type: string | undefined) {
        if (!this.rule || !this.rule.when || parent !== this.rule.when) {
            return;
        }

        let newGroup: LogicGroup<RuleCondition> = {
            operator: LogicGroupOperator.AND
        };

        if (this.config && this.config.json && this.config.json.whenGroup) {
            newGroup = JSON.parse(JSON.stringify(this.config.json.whenGroup)) as LogicGroup<RuleCondition>;
        }

        if (newGroup.operator !== LogicGroupOperator.AND) {
            console.warn("JSON rules editor doesn't support top level logic group with type OR");
            this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
            return;
        }

        if (newGroup.groups && newGroup.groups.length > 0) {
            console.warn("JSON rules editor doesn't support multiple top level logic groups");
            this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
            return;
        }


        newGroup.groups = undefined;

        // Add an item if none exist
        if (!newGroup.items || newGroup.items!.length === 0) {
            this.addCondition(newGroup, type, true);
        }

        if (!parent.groups) {
            parent.groups = [];
        }
        parent.groups.push(newGroup);

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
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
                this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
                this.requestUpdate();
            }
        }
    }

    private addCondition(parent: LogicGroup<RuleCondition>, type: string | undefined, silent?: boolean) {
        if (!parent) {
            return;
        }

        if (!parent.items) {
            parent.items = [];
        }

        let newCondition: RuleCondition = {};

        if (this.config && this.config.json && this.config.json.whenCondition) {
            newCondition = JSON.parse(JSON.stringify(this.config.json.whenCondition)) as RuleCondition;
        } else {
            updateRuleConditionType(newCondition, type || WellknownAssets.THINGASSET, this.config);
        }

        parent.items.push(newCondition);
        if (!silent) {
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
}
