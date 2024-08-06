var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var OrRuleWhen_1;
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { OrRulesRuleUnsupportedEvent } from "../index";
import { buttonStyle } from "../style";
import "./or-rule-condition";
import i18next from "i18next";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import { getWhenTypesMenu, updateRuleConditionType } from "./or-rule-condition";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { translate } from "@openremote/or-translate";
var ResetOption;
(function (ResetOption) {
    ResetOption["NO_LONGER_MATCHES"] = "noLongerMatches";
    ResetOption["VALUE_CHANGES"] = "valueChanges";
})(ResetOption || (ResetOption = {}));
// language=CSS
const style = css `
    
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
let OrRuleWhen = OrRuleWhen_1 = class OrRuleWhen extends translate(i18next)(LitElement) {
    static get styles() {
        return style;
    }
    ruleGroupTemplate(group, parentGroup) {
        if (!group) {
            return html ``;
        }
        const isTopLevel = !parentGroup;
        const showAddCondition = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddCondition !== true);
        const showRemoveCondition = !this.readonly;
        const showRemoveGroup = !this.readonly;
        let wrapper;
        if (isTopLevel) {
            wrapper = (content, item, parent, isGroup, isFirst) => {
                return html `
                    <or-panel .heading="${i18next.t(isFirst ? "when" : "orWhen")}...">
                        ${showRemoveGroup ? html `
                            <button class="button-clear remove-button" @click="${() => this.removeItem(item, parent, isGroup)}">
                                <or-icon icon="close-circle"></or-icon>  
                            </button>
                        ` : ``}
                        ${content}
                    </or-panel>`;
            };
        }
        else {
            wrapper = (content, item, parent, isGroup, isFirst) => {
                return html `
                    ${!isFirst ? html `
                        <or-icon class="small" icon="ampersand"></or-icon>
                    ` : ``}
                    ${content}
                `;
                return content;
            };
        }
        let groupsTemplate = ``;
        let itemsTemplate = ``;
        let addTemplate = ``;
        let isFirst = true;
        if (group.groups && group.groups.length > 0) {
            groupsTemplate = html `
                ${group.groups.map((childGroup, index) => {
                const content = html `
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
            itemsTemplate = html `
                ${group.items.map((condition, index) => {
                const content = html `
                        <div class="rule-group-item">
                            <div class="rule-condition">
                                <or-rule-condition .config="${this.config}" .assetInfos="${this.assetInfos}" .ruleCondition="${condition}" .readonly="${this.readonly}" .assetProvider="${this.assetProvider}"></or-rule-condition>
                                ${showRemoveGroup ? html `
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
            addTemplate = html `
                <span class="add-button-wrapper">
                    ${getContentWithMenuTemplate(html `<or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"
                                           .label="${i18next.t("rulesEditorAddCondition")}"></or-mwc-input>`, getWhenTypesMenu(this.config, this.assetInfos), undefined, (value) => this.addCondition(group, value))}
                </span>
            `;
        }
        return html `
            ${groupsTemplate}
            ${itemsTemplate}
            ${addTemplate}
        `;
    }
    dateTimePredicateTemplate() {
        return html `<span>DATE TIME PREDICATE NOT IMPLEMENTED</span>`;
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("rule")) {
            if (this.rule) {
                if (!this.rule.when) {
                    this.rule.when = {
                        operator: "OR" /* LogicGroupOperator.OR */
                    };
                }
                else {
                    // Check this is a rule compatible with this editor
                    if (!OrRuleWhen_1._isRuleWhenCompatible(this.rule.when)) {
                        this.rule = undefined;
                        this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
                    }
                }
            }
        }
        return super.shouldUpdate(_changedProperties);
    }
    render() {
        if (!this.rule || !this.rule.when) {
            return html ``;
        }
        const showAddGroup = !this.readonly && (!this.config || !this.config.controls || this.config.controls.hideWhenAddGroup !== true);
        return html `
            <div>
                ${this.ruleGroupTemplate(this.rule.when)}
            </div>
            
            ${!showAddGroup ? `` : html `
                <or-panel>
                    <strong>${i18next.t(!this.rule.when.groups || this.rule.when.groups.length === 0 ? "when" : "orWhen")}...</strong>
                    <span class="add-button-wrapper">
                        ${getContentWithMenuTemplate(html `<or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"></or-mwc-input>`, getWhenTypesMenu(this.config, this.assetInfos), undefined, (value) => this.addGroup(this.rule.when, value))}
                    </span>
                </or-panel>
            `}
        `;
    }
    /**
     * Currently only support a top level OR group and then N child AND groups with no more descendants
     */
    static _isRuleWhenCompatible(when) {
        if (when.operator !== "OR" /* LogicGroupOperator.OR */) {
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
    static _isWhenGroupCompatible(group) {
        if (group.operator === "OR" /* LogicGroupOperator.OR */) {
            console.warn("Incompatible rule: when group operator not set to 'AND'");
            return false;
        }
        if (group.groups && group.groups.length > 0) {
            console.warn("Incompatible rule: when group nested groups not supported");
            return false;
        }
        return true;
    }
    addGroup(parent, type) {
        if (!this.rule || !this.rule.when || parent !== this.rule.when) {
            return;
        }
        let newGroup = {
            operator: "AND" /* LogicGroupOperator.AND */
        };
        if (this.config && this.config.json && this.config.json.whenGroup) {
            newGroup = JSON.parse(JSON.stringify(this.config.json.whenGroup));
        }
        if (newGroup.operator !== "AND" /* LogicGroupOperator.AND */) {
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
        if (!newGroup.items || newGroup.items.length === 0) {
            this.addCondition(newGroup, type, true);
        }
        if (!parent.groups) {
            parent.groups = [];
        }
        parent.groups.push(newGroup);
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    removeItem(item, parent, isGroup) {
        let removed = false;
        if (parent) {
            if (isGroup) {
                const index = parent.groups.indexOf(item);
                parent.groups.splice(index, 1);
                removed = index >= 0;
            }
            else {
                const index = parent.items.indexOf(item);
                parent.items.splice(index, 1);
                removed = index >= 0;
            }
            if (removed) {
                this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
                this.requestUpdate();
            }
        }
    }
    addCondition(parent, type, silent) {
        if (!parent) {
            return;
        }
        if (!parent.items) {
            parent.items = [];
        }
        let newCondition = {};
        if (this.config && this.config.json && this.config.json.whenCondition) {
            newCondition = JSON.parse(JSON.stringify(this.config.json.whenCondition));
        }
        else {
            updateRuleConditionType(newCondition, type || "ThingAsset" /* WellknownAssets.THINGASSET */, this.config);
        }
        parent.items.push(newCondition);
        if (!silent) {
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }
};
__decorate([
    property({ type: Object })
], OrRuleWhen.prototype, "rule", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleWhen.prototype, "readonly", void 0);
__decorate([
    property({ type: Object })
], OrRuleWhen.prototype, "assetProvider", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrRuleWhen.prototype, "assetInfos", void 0);
OrRuleWhen = OrRuleWhen_1 = __decorate([
    customElement("or-rule-when")
], OrRuleWhen);
//# sourceMappingURL=or-rule-when.js.map