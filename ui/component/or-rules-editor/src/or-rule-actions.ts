import {customElement, html, LitElement, TemplateResult, property} from "lit-element";
import {actionStyle} from "./style";
import "./or-rule-asset-query";
import {
    ActionType,
    RulesConfig,
    ActionTargetType,
    getAssetTypeFromQuery, OrRuleChangedEvent
} from "./index";
import {
    RuleActionUnion, AssetDescriptor, AttributeDescriptor, RuleActionWriteAttribute
} from "@openremote/model";
import i18next from "i18next";
import {OrInputChangedEvent, InputType} from "@openremote/or-input";

@customElement("or-rule-actions")
class OrRuleActions extends LitElement {

    static get styles() {
        return actionStyle;
    }

    @property({type: Array})
    public actions?: RuleActionUnion[];

    @property({type: Object})
    public targetTypeMap?: [string, string?][];

    public readonly?: boolean;

    public newTemplate?: RuleActionUnion;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    public allowAdd: boolean = true;

    private _ruleActionTargets = [ActionTargetType.TRIGGER_ASSETS, ActionTargetType.TAGGED_ASSETS, ActionTargetType.OTHER_ASSETS, ActionTargetType.USERS];
    private _ruleActionTypes = [ActionType.WRITE_ATTRIBUTE, ActionType.UPDATE_ATTRIBUTE, ActionType.NOTIFICATION, ActionType.WAIT];

    protected ruleActionTemplate(action: RuleActionUnion) {

        if (!action) {
            return ``;
        }

        let targetTemplate: TemplateResult | string = ``;
        let currentTarget = ActionTargetType.TRIGGER_ASSETS;
        const hideTargetOptions = !this.config || !this.config.controls || !this.config.controls.hideActionTargetOptions;
        const hideUpdateOptions = !this.config || !this.config.controls || !this.config.controls.hideActionUpdateOptions;
        const allowedTargets = this.config && this.config.controls && this.config.controls.allowedActionTargets ? this.config.controls.allowedActionTargets : this._ruleActionTargets;
        let targetRhsTemplate: TemplateResult | string = ``;

        if (action.target) {
            if (action.target.ruleConditionTag) {
                currentTarget = ActionTargetType.TAGGED_ASSETS;
                if (!hideTargetOptions) {
                    targetRhsTemplate = html`<or-input type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionTriggerTag(action, e.detail.value)}" ?readonly="${this.readonly}" .options="${this.getTriggerTags}" .value="${action.target.ruleConditionTag}"></or-input>`;
                }
            } else if (action.target.users) {
                currentTarget = ActionTargetType.USERS;
                if (!hideTargetOptions) {
                    targetRhsTemplate = html`<span>USER QUERY NOT IMPLEMENTED</span>`;
                }
            } else if (action.target.assets) {
                currentTarget = ActionTargetType.OTHER_ASSETS;
                if (!hideTargetOptions) {
                    targetRhsTemplate = html`<or-rule-asset-query .config="${this.config}" .assetDescriptors="${this.assetDescriptors}" .readonly="${this.readonly}" .query="${action.target.assets}"></or-rule-asset-query>`;
                }
            }
        }

        if (!hideTargetOptions) {
            const translatedAllowedTargets = allowedTargets.map((at) => [at, i18next.t(at)] as [string, string]);
            targetTemplate = html`
                <div class="rule-action-target">
                    <span><or-translate value="target"></or-translate>: </span><or-select id="actionTargetSelect" type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionTarget(action, e.detail.value as ActionTargetType)}" ?readonly="${this.readonly}" .options="${translatedAllowedTargets}" .value="${currentTarget}"></or-select>
                    ${targetRhsTemplate}
                </div>
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
                        assetTypes = this.getAssetTypes(action.target!.ruleConditionTag!);
                        break;
                }

                const attributeDescriptors = this.getCommonAttributes(assetTypes);
                const attributeNames = attributeDescriptors.map((ad) => [ad.attributeName, i18next.t(ad.attributeName!)]);
                const assetType = assetTypes.length > 0 ? assetTypes[0][0] : undefined;

                const attributeInput = this.config && this.config.inputProvider && action.attributeName ? this.config.inputProvider(assetType!, action.attributeName, attributeDescriptors.find((ad) => ad.attributeName === action.attributeName)!, action.value, (value: any) => this.setActionAttributeValue(action, value), this.readonly || false, false) : undefined;

                actionTemplate = html`
                    <or-input type="${InputType.SELECT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionAttributeName(action, e.detail.value)}" ?readonly="${this.readonly}" .options="${attributeNames}" .value="${action.attributeName}"></or-input>
                    ${action.attributeName ? attributeInput ? attributeInput : html`<or-input @or-input-changed="${(e: OrInputChangedEvent) => this.setActionAttributeValue(action, e.detail.value)}" ?readonly="${this.readonly}" .assetType="${assetType}" .attributeName="${action.attributeName}" .value="${action.value}"></or-input>` : ``}                     
                `;

                if (action.action === "update-attribute" && !hideUpdateOptions) {
                    actionTemplate = html`
                        ${actionTemplate}
                        <span>UPDATE NOT IMPLEMENTED</span>
                    `;
                }
                break;
        }

        return html`
            ${targetTemplate}
            <div class="rule-action-action">
                ${actionTemplate}          
            </div>
        `;
    }

    protected render() {

        const allowedActions = this.config && this.config.controls && this.config.controls.allowedActionTypes ? this.config.controls.allowedActionTypes : this._ruleActionTypes;

        return html`                   
            ${this.actions ? this.actions.map((action: RuleActionUnion) => {
                return html`
                    <div class="rule-action">
                        ${this.ruleActionTemplate(action)}
                        ${!this.readonly && (this.allowAdd || this.actions!.length > 1) ? html`
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

        return this.targetTypeMap.filter((typeAndTag) => typeAndTag[1] !== undefined).map((typeAndTag) => typeAndTag[1]!).sort();
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
        if (!this.actions || !action) {
            return;
        }

        const index = this.actions.indexOf(action);
        if (index >= 0) {
            this.actions.splice(index, 1);
            this.dispatchEvent(new OrRuleChangedEvent());
            this.requestUpdate();
        }
    }

    protected addAction() {

        if (!this.actions) {
            return;
        }

        let newAction: RuleActionUnion = {
            action: "write-attribute"
        };

        if (this.newTemplate) {
            newAction = JSON.parse(JSON.stringify(this.newTemplate)) as RuleActionUnion;
        }

        this.actions.push(newAction);
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionTarget(action: RuleActionUnion, target: ActionTargetType | undefined) {
        // TODO: Set action target
    }

    protected setActionTriggerTag(action: RuleActionUnion, tag: string | undefined) {
        action.target!.ruleConditionTag = tag;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionAttributeName(action: RuleActionUnion, name: string | undefined) {
        (action as RuleActionWriteAttribute).attributeName = name;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionAttributeValue(action: RuleActionUnion, value: any) {
        (action as RuleActionWriteAttribute).value = value;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }
}
