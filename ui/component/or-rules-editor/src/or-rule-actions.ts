import {customElement, html, LitElement, property, TemplateResult} from "lit-element";
import {actionStyle} from "./style";
import "./or-rule-asset-query";
import {ActionTargetType, ActionType, getAssetTypeFromQuery, OrRuleChangedEvent, RulesConfig} from "./index";
import {
    AssetDescriptor,
    AttributeDescriptor,
    JsonRule,
    RuleActionUnion,
    RuleActionWriteAttribute,
    Attribute
} from "@openremote/model";
import i18next from "i18next";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {getAttributeValueTemplate} from "@openremote/or-attribute-input";

@customElement("or-rule-actions")
class OrRuleActions extends LitElement {

    static get styles() {
        return actionStyle;
    }

    @property({type: Object})
    public rule?: JsonRule;

    @property({type: Object})
    public targetTypeMap?: [string, string?][];

    public readonly?: boolean;

    public newTemplate?: RuleActionUnion;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    public allowAdd: boolean = true;

    public static DEFAULT_ALLOWED_ACTION_TARGETS = [ActionTargetType.TRIGGER_ASSETS, ActionTargetType.TAGGED_ASSETS, ActionTargetType.OTHER_ASSETS, ActionTargetType.USERS];
    public static DEFAULT_ALLOWED_ACTION_TYPES = [ActionType.WRITE_ATTRIBUTE, ActionType.UPDATE_ATTRIBUTE, ActionType.NOTIFICATION, ActionType.WAIT];

    protected ruleActionTemplate(action: RuleActionUnion) {

        if (!action) {
            return ``;
        }

        let targetTemplate: TemplateResult | string = ``;
        let typeTemplate: TemplateResult | string = ``;
        let currentTarget = ActionTargetType.TRIGGER_ASSETS;
        const showTargetOptions = action.action !== "wait" && (!this.config || !this.config.controls || !this.config.controls.hideActionTargetOptions);
        const showTypeOptions = !this.config || !this.config.controls || !this.config.controls.hideActionTypeOptions;
        const showUpdateOptions = !this.config || !this.config.controls || !this.config.controls.hideActionUpdateOptions;
        let allowedTargets = this.config && this.config.controls && this.config.controls.allowedActionTargets ? this.config.controls.allowedActionTargets : OrRuleActions.DEFAULT_ALLOWED_ACTION_TARGETS;
        const allowedActions = this.config && this.config.controls && this.config.controls.allowedActionTypes ? this.config.controls.allowedActionTypes : OrRuleActions.DEFAULT_ALLOWED_ACTION_TYPES;
        let targetRhsTemplate: TemplateResult | string = ``;

        if (action.target) {
            if (action.target.ruleConditionTag !== undefined && action.target.ruleConditionTag !== null) {
                currentTarget = ActionTargetType.TAGGED_ASSETS;
                if (showTargetOptions) {
                    targetRhsTemplate = html`<or-input id="actionTagSelect" type="${InputType.SELECT}" .label="${i18next.t("tag")}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionTriggerTag(action, e.detail.value)}" ?readonly="${this.readonly}" .options="${this.getTriggerTags()}" .value="${action.target.ruleConditionTag}"></or-input>`;
                }
            } else if (action.target.users) {
                currentTarget = ActionTargetType.USERS;
                if (showTargetOptions) {
                    targetRhsTemplate = html`<span>USER QUERY NOT IMPLEMENTED</span>`;
                }
            } else if (action.target.assets) {
                currentTarget = ActionTargetType.OTHER_ASSETS;
                if (showTargetOptions) {
                    targetRhsTemplate = html`<or-rule-asset-query .config="${this.config}" .assetDescriptors="${this.assetDescriptors}" .readonly="${this.readonly}" .query="${action.target.assets}"></or-rule-asset-query>`;
                }
            }
        }

        if (showTypeOptions) {
            const translatedAllowedActions = allowedActions.map((actionType) => [actionType, i18next.t(actionType)] as [string, string]);
            typeTemplate = html`
                <or-input id="actionTypeSelect" .label="${i18next.t("action")}" .type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionType(action, e.detail.value as ActionType)}" ?readonly="${this.readonly}" .options="${translatedAllowedActions}" .value="${action.action}"></or-input>
            `;
        }

        if (showTargetOptions) {

            if (action.action !== "notification") {
                allowedTargets = allowedTargets.filter((at) => at !== ActionTargetType.USERS);
            }

            const translatedAllowedTargets = allowedTargets.map((at) => [at, i18next.t(at)] as [string, string]);
            targetTemplate = html`
                <or-input id="actionTargetSelect" .label="${i18next.t("target")}" .type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionTarget(action, e.detail.value as ActionTargetType)}" ?readonly="${this.readonly}" .options="${translatedAllowedTargets}" .value="${currentTarget}"></or-input>
                ${targetRhsTemplate}
            `;
        }

        let actionTemplate: TemplateResult | string = ``;
        switch (action.action) {
            case "wait":
                actionTemplate = html`<span>WAIT NOT IMPLEMENTED</span>`;
                break;
            case "notification":
                actionTemplate = html`<span>NOTIFICATIONS NOT IMPLEMENTED</span>`;
                break;
            case "write-attribute":
            case "update-attribute":
                let assetTypes: [string, string][] = [];

                switch (currentTarget) {
                    case ActionTargetType.USERS:
                        return html`<span>INVALID RULE</span>`;
                    case ActionTargetType.TRIGGER_ASSETS:
                        // Get all asset types
                        assetTypes = this.getAssetTypes();
                        break;
                    case ActionTargetType.OTHER_ASSETS:
                        const type = getAssetTypeFromQuery(action.target!.assets);
                        if (type) {
                            if (this.assetDescriptors) {
                                const descriptor = this.assetDescriptors.find((ad) => ad.type === type);
                                if (descriptor) {
                                    assetTypes.push([descriptor.type!, descriptor.name!]);
                                }
                            }
                        }
                        break;
                    case ActionTargetType.TAGGED_ASSETS:
                        // actionTemplate = html`
                        //     <or-input type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionTriggerTag(action, e.detail.value)}" ?readonly="${this.readonly}" .options="${attributeNames}" .value="${action.attributeName}"></or-input>
                        // `;
                        assetTypes = this.getAssetTypes(action.target!.ruleConditionTag!);
                        break;
                }

                const attributeDescriptors = this.getCommonAttributes(assetTypes);
                const attributeNames = attributeDescriptors.map((ad) => [ad.attributeName, i18next.t(ad.attributeName!)]);
                const assetType = assetTypes.length > 0 ? assetTypes[0][0] : undefined;

                // A dummy attribute to allow us to re-use the following function
                let attribute: Attribute = {
                    name: action.attributeName
                };
                let inputTemplate = getAttributeValueTemplate(assetType, attribute, this.readonly || false, false, (v: any) => this.setActionAttributeValue(action, v), this.config ? this.config.inputProvider : undefined);

                actionTemplate = html`
                    <or-input id="actionAttributeSelect" .label="${i18next.t("attribute")}" .type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionAttributeName(action, e.detail.value)}" ?readonly="${this.readonly}" .options="${attributeNames}" .value="${action.attributeName}"></or-input>
                    ${action.attributeName && inputTemplate ? inputTemplate(action.value) : ``}                     
                `;

                if (action.action === "update-attribute" && showUpdateOptions) {
                    actionTemplate = html`
                        ${actionTemplate}
                        <span>UPDATE NOT IMPLEMENTED</span>
                    `;
                }
                break;
        }

        return html`
            ${typeTemplate}
            ${targetTemplate}
            ${actionTemplate}
        `;
    }

    protected render() {

        if (!this.rule) {
            return html``;
        }

        if (!this.rule.then) {
            this.rule.then = [];
        }

        return html`                   
            ${this.rule.then ? this.rule.then.map((action: RuleActionUnion) => {
                return html`
                    <div class="rule-action">
                        ${this.ruleActionTemplate(action)}
                        ${!this.readonly && (this.allowAdd || this.rule!.then!.length > 1) ? html`
                                        <button class="button-clear" @click="${() => this.removeAction(action)}"><or-icon icon="close-circle"></or-icon></input>
                                    ` : ``}                                   
                    </div>
            `; }) : ``}
            ${this.allowAdd ? html`
                <div class="add-buttons-container">
                    <button class="button-clear add-button" @click="${this.addAction}"><or-icon icon="plus-circle"></or-icon><or-translate value="rulesEditorAddAction"></or-translate></button>
                </div>
            ` : ``}
        `;
    }

    protected getCommonAttributes(assetTypeAndNames: [string, string][]) {

        let isFirst = true;
        let attributes: AttributeDescriptor[] = [];
        for (const assetTypeAndName of assetTypeAndNames) {
            const assetDescriptor = this.assetDescriptors ? this.assetDescriptors.find((ad) => ad.type === assetTypeAndName[0]) : undefined;
            if (assetDescriptor && assetDescriptor.attributeDescriptors) {
                if (isFirst) {
                    attributes = [...assetDescriptor.attributeDescriptors];
                } else {
                    attributes = attributes.filter((attribute) => assetDescriptor.attributeDescriptors!.find((ad) => ad.attributeName === attribute.attributeName));
                }
            }
            isFirst = false;
            if (attributes.length === 0) {
                break;
            }
        }

        return attributes;
    }

    protected getTriggerTags(): string[] {
        if (!this.targetTypeMap) {
            return [];
        }

        return this.targetTypeMap.map((typeAndTag, index) => typeAndTag[1] !== undefined ? typeAndTag[1] : index.toString()).sort();
    }

    protected getAssetTypes(tagName?: string) {
        if (!this.targetTypeMap || !this.assetDescriptors) {
            return [];
        }

        return this.targetTypeMap.filter((typeAndTag) => !tagName || typeAndTag[1] === tagName).map((typeAndTag) => {
            const descriptor = this.assetDescriptors!.find((ad) => ad.type === typeAndTag[0]);
            if (descriptor) {
                return [descriptor.type, descriptor.name] as [string, string];
            }
            return null;
        }).filter((type) => type !== null) as [string, string][];
    }

    protected removeAction(action?: RuleActionUnion) {
        if (!this.rule || !this.rule.then || !action) {
            return;
        }

        const index = this.rule.then.indexOf(action);
        if (index >= 0) {
            this.rule.then.splice(index, 1);
            this.dispatchEvent(new OrRuleChangedEvent());
            this.requestUpdate();
        }
    }

    protected addAction() {

        if (!this.rule || !this.rule.then) {
            return;
        }

        let newAction: RuleActionUnion = {
            action: "write-attribute"
        };

        if (this.newTemplate) {
            newAction = JSON.parse(JSON.stringify(this.newTemplate)) as RuleActionUnion;
        }

        this.rule.then.push(newAction);
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionTarget(action: RuleActionUnion, target: ActionTargetType | undefined) {
        switch (target) {
            case ActionTargetType.TRIGGER_ASSETS:
                action.target = undefined;
                break;
            case ActionTargetType.TAGGED_ASSETS:
                action.target = {
                    ruleConditionTag: ""
                };
                break;
            case ActionTargetType.USERS:
                action.target = {
                    users: {

                    }
                };
                break;
            case ActionTargetType.OTHER_ASSETS:
                action.target = {
                    assets: {

                    }
                };
                break;
        }

        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionType(action: RuleActionUnion, type: ActionType | undefined) {

        const actions = this.rule!.then!;
        const index = actions.indexOf(action);
        if (index < 0) {
            return;
        }

        switch (type) {
            case ActionType.WAIT:
                action = {
                    action: "wait"
                };
                break;
            case ActionType.NOTIFICATION:
                action = {
                    action: "notification"
                };
                break;
            case ActionType.WRITE_ATTRIBUTE:
                action = {
                    action: "write-attribute"
                };
                break;
            case ActionType.UPDATE_ATTRIBUTE:
                action = {
                    action: "update-attribute"
                };
                break;
        }

        actions[index] = action;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionTriggerTag(action: RuleActionUnion, tag: string | undefined) {
        action.target!.ruleConditionTag = tag;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionAttributeName(action: RuleActionUnion, name: string | undefined) {
        (action as RuleActionWriteAttribute).attributeName = name;
        (action as RuleActionWriteAttribute).value = undefined;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionAttributeValue(action: RuleActionUnion, value: any) {
        (action as RuleActionWriteAttribute).value = value;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }
}
