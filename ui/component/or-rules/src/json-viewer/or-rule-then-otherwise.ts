import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {buttonStyle} from "../style";
import "./or-rule-asset-query";
import {ActionType, getAssetTypeFromQuery, RulesConfig} from "../index";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {
    Asset,
    AssetModelUtil,
    AssetTypeInfo,
    HTTPMethod,
    JsonRule,
    RuleActionNotification,
    RuleActionUnion,
    RuleActionWebhook,
    RuleRecurrence,
    WellknownAssets
} from "@openremote/model";
import i18next from "i18next";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {Util} from "@openremote/core";
import "./or-rule-action-attribute";
import "./or-rule-action-notification";
import "./or-rule-action-webhook";
import {translate} from "@openremote/or-translate";

const NOTIFICATION_COLOR = "4B87EA";
const WAIT_COLOR = "EACC54";

function getActionTypesMenu(config?: RulesConfig, assetInfos?: AssetTypeInfo[]): (ListItem | null)[] {

    let addAssetTypes = true;
    let addWait = true;
    let addNotification = true;
    let addPushNotification = true;
    let addWebhook = true;

    if (config && config.controls && config.controls.allowedActionTypes) {
        addAssetTypes = config.controls.allowedActionTypes.indexOf(ActionType.ATTRIBUTE) >= 0;
        addWait = config.controls.allowedActionTypes.indexOf(ActionType.WAIT) >= 0;
        addNotification = config.controls.allowedActionTypes.indexOf(ActionType.EMAIL) >= 0;
        addPushNotification = config.controls.allowedActionTypes.indexOf(ActionType.PUSH_NOTIFICATION) >= 0;
        addWebhook = config.controls.allowedActionTypes.indexOf(ActionType.WEBHOOK) >= 0;
    }


    const menu: (ListItem | null)[] = [];

    if (addAssetTypes && assetInfos) {
        menu.push(...assetInfos.filter((assetInfo) => assetInfo.assetDescriptor!.descriptorType !== "agent").map((assetTypeInfo) => {

            const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
            const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
            const styleMap = color ? {"--or-icon-fill": "#" + color} : undefined;

            return {
                text: Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor!),
                value: assetTypeInfo.assetDescriptor!.name,
                icon: icon ? icon : AssetModelUtil.getAssetDescriptorIcon(WellknownAssets.THINGASSET),
                styleMap: styleMap
            } as ListItem;
        }));
    }

    menu.sort(Util.sortByString((listItem) => listItem?.value!));
    menu.push(null); // divider

    if (addNotification) {
        menu.push({
            text: i18next.t("email"),
            icon: "email",
            value: ActionType.EMAIL,
            styleMap: {"--or-icon-fill": "#" + NOTIFICATION_COLOR}
        } as ListItem);
    }
    
    if (addPushNotification) {
        menu.push({
            text: i18next.t("push-notification"),
            icon: "cellphone-message",
            value: ActionType.PUSH_NOTIFICATION,
            styleMap: {"--or-icon-fill": "#" + NOTIFICATION_COLOR}
        } as ListItem);
    }

    if (addWait) {
        menu.push({
            text: i18next.t("wait"),
            icon: "timer",
            value: ActionType.WAIT,
            styleMap: {"--or-icon-fill": "#" + WAIT_COLOR}
        } as ListItem);
    }

    if (addWebhook) {
        menu.push({
            text: i18next.t("webhook"),
            icon: "webhook",
            value: ActionType.WEBHOOK,
            styleMap: {"--or-icon-fill": "#" + NOTIFICATION_COLOR}
        })
    }

    return menu;
}

interface ResetOptions {
    [key: string]: (jsonRule: JsonRule) => void;
}

export enum RecurrenceOption {
    ALWAYS = "always",
    ONCE = "once",
    ONCE_PER_HOUR = "oncePerHour",
    ONCE_PER_DAY = "oncePerDay",
    ONCE_PER_WEEK = "oncePerWeek",
}

function getRecurrenceMenu(config?: RulesConfig): ListItem[] {

    if (config && config.controls && config.controls.allowedRecurrenceOptions) {
        return config.controls.allowedRecurrenceOptions.map((value) => {
            return {
                text: i18next.t(value),
                value: value
            };
        });
    }

    return Object.values(RecurrenceOption).map((value) => {
        return {
            text: i18next.t(value),
            value: value
        };
    });
}
// language=CSS
const style = css`

    :host {
        display: flex;
        width: 100%;
        flex-direction: column;
    }
        
    ${buttonStyle}
    
    .rule-action {
        display: flex;
        margin: 10px 0;
    }
    
    .rule-action-wrapper {
        flex-grow: 1;
        display: flex;
        align-items: baseline;
    }
    
    .rule-action > button {
        flex-grow: 0;
    }

    .rule-action:hover .button-clear {
        visibility: visible;
    }

    or-panel:hover .remove-button.button-clear {
        visibility: visible;
    }
            
    or-panel {
        position: relative;
        margin: 10px 10px 20px 10px;
    }
    
    #type-selector {
        margin-top: 10px;
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
    
    #type {
        white-space: nowrap;
        margin: 4px 3px auto 0;
        text-transform: capitalize;
    }
    
    .rule-recurrence {
        position: absolute;
        top: 5px;
        right: 0;
    }
   
`;

@customElement("or-rule-then-otherwise")
class OrRuleThenOtherwise extends translate(i18next)(LitElement) {

    static get styles() {
        return style;
    }

    @property({type: Array, attribute: false})
    public targetTypeMap!: [string, string?][];

    @property({type: Object, attribute: false})
    public rule!: JsonRule;

    @property({type: Boolean})
    public readonly?: boolean;

    public config?: RulesConfig;

    @property({type: Object, attribute: false})
    public assetInfos?: AssetTypeInfo[];

    @property({type: Object})
    public assetProvider?: (type: string) => Promise<Asset[] | undefined>

    protected get thenAllowAdd() {
        return !this.config || !this.config.controls || this.config.controls.hideThenAddAction !== true;
    }

    protected ruleRecurrenceTemplate(reset: RuleRecurrence | undefined, readonly?: boolean) {
        let recurrenceTemplate: TemplateResult | string = ``;
        const buttonColor = "inherit";
        let value = RecurrenceOption.ONCE;

        if (reset) {
            if (reset.mins === undefined || reset.mins === null) {
                value = RecurrenceOption.ONCE;
            } else if (reset.mins === 0) {
                value = RecurrenceOption.ALWAYS;
            } else if (reset.mins === 60) {
                value = RecurrenceOption.ONCE_PER_HOUR;
            } else if (reset.mins === 1440) {
                value = RecurrenceOption.ONCE_PER_DAY;
            } else if (reset.mins === 10080) {
                value = RecurrenceOption.ONCE_PER_WEEK;
            }
        }
        if(readonly) {
            recurrenceTemplate = html`
                <div style="--or-mwc-input-color: ${buttonColor}; margin-right: 6px;">
                    <or-mwc-input .type="${InputType.BUTTON}" label="${value}"></or-mwc-input>
                </div>
            `;
        } else {
        recurrenceTemplate = html`
                <div style="--or-mwc-input-color: ${buttonColor}; margin-right: 6px;">
                    ${getContentWithMenuTemplate(
                        html`<or-mwc-input .type="${InputType.BUTTON}" label="${value}"></or-mwc-input>`,
                        getRecurrenceMenu(this.config),
                        value,
                        (value) => this.setRecurrenceOption(value as RecurrenceOption))}
                </div>
            `;
        }
        return html`
            <div class="rule-recurrence">
                ${recurrenceTemplate}
            </div>
        `;
    }

    protected ruleActionTemplate(actions: RuleActionUnion[], action: RuleActionUnion, readonly?: boolean) {

        const showTypeSelect = !this.config || !this.config.controls || this.config.controls.hideActionTypeOptions !== true;
        const type = this.getActionType(action);

        if (!type && !showTypeSelect) {
            return html`<span>INVALID CONFIG - NO TYPE SPECIFIED AND TYPE SELECTOR DISABLED</span>`;
        }

        let typeTemplate: TemplateResult | string = ``;
        let template: TemplateResult | string = ``;
        const thenAllowRemove = !this.readonly && (this.thenAllowAdd || this.rule.then!.length > 1);

        if (showTypeSelect) {

            let buttonIcon;
            let buttonColor = "inherit";

            if (action.action) {
                switch (action.action) {
                    case ActionType.WAIT:
                        buttonIcon = "timer";
                        buttonColor = WAIT_COLOR;
                        break;
                    case ActionType.WEBHOOK:
                        buttonIcon = "webhook";
                        buttonColor = NOTIFICATION_COLOR;
                        break;
                    case "notification":
                        action = action as RuleActionNotification
                        if (type === "push") {
                            buttonIcon = "cellphone-message";
                            buttonColor = NOTIFICATION_COLOR;
                        } else {
                            buttonIcon = "email";
                            buttonColor = NOTIFICATION_COLOR;
                        }
                        break;
                    default:
                        const ad = AssetModelUtil.getAssetDescriptor(type);
                        buttonIcon = AssetModelUtil.getAssetDescriptorIcon(ad);
                        buttonColor = AssetModelUtil.getAssetDescriptorColour(ad) || buttonColor;
                        break;
                }
            }

            if (readonly) {
                typeTemplate = html`
                    <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                        <or-mwc-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>
                    </div>
                `;
            } else {
                typeTemplate = html`
                    <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                        ${getContentWithMenuTemplate(
                            html`<or-mwc-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>`,
                            getActionTypesMenu(this.config, this.assetInfos),
                            action.action,
                            (value) => this.setActionType(actions, action, value as string))}
                    </div>
                `;
            }
        }

        if (type) {
            switch (type) {
                case ActionType.WAIT:
                    template = html`<span>WAIT NOT IMPLEMENTED</span>`;
                    break;
                case ActionType.PUSH_NOTIFICATION:
                    template = html`<or-rule-action-notification id="push-notification" .rule="${this.rule}" .action="${action}" .actionType="${ActionType.PUSH_NOTIFICATION}" .config="${this.config}" .assetInfos="${this.assetInfos}" .readonly="${this.readonly}"></or-rule-action-notification>`;
                    break;
                case ActionType.EMAIL:
                    template = html`<or-rule-action-notification id="email-notification" .rule="${this.rule}" .action="${action}" .actionType="${ActionType.EMAIL}" .config="${this.config}" .assetInfos="${this.assetInfos}" .readonly="${this.readonly}"></or-rule-action-notification>`;
                    break;
                case ActionType.WEBHOOK:
                    template = html`<or-rule-action-webhook .rule="${this.rule}" .action="${action}" .actionType="${ActionType.WEBHOOK}"></or-rule-action-webhook>`;
                    break;
                default:
                    template = html`<or-rule-action-attribute .action="${action}" .targetTypeMap="${this.targetTypeMap}" .config="${this.config}" .assetInfos="${this.assetInfos}" .assetProvider="${this.assetProvider}" .readonly="${this.readonly}"></or-rule-action-attribute>`;
                    break;
            }
        }

        return html`
            <div class="rule-action">
                <div class="rule-action-wrapper">
                    ${typeTemplate}
                    ${template}
                </div>
                    ${thenAllowRemove ? html`
                        <button class="button-clear" @click="${() => this.removeAction(action)}"><or-icon icon="close-circle"></or-icon></input>
                    ` : ``}
            </div>
        `;
    }

    protected render() {
        const thenAllowAdd = !this.readonly && (this.thenAllowAdd || this.rule.then!.length > 1);
        return html`
            <div>
                <or-panel .heading="${i18next.t("then")}...">
                    ${this.ruleRecurrenceTemplate(this.rule.recurrence, this.readonly)}

                    ${!this.rule.then ? `` : this.rule.then.map((action: RuleActionUnion) => this.ruleActionTemplate(this.rule.then!, action, this.readonly))}
                    ${thenAllowAdd ? html`
                        <span class="add-button-wrapper">
                            ${getContentWithMenuTemplate(
                                html`<or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"
                                                   .label="${i18next.t("rulesEditorAddAction")}"></or-mwc-input>`,
                                getActionTypesMenu(this.config, this.assetInfos),
                                undefined,
                                (value) => this.addAction(value as string))}
                        </span>
                    ` : ``}
                </or-panel>
            </div>
        `;
    }

    protected getActionType(action: RuleActionUnion): string | undefined {

        switch (action.action) {
            case "wait":
                break;
            case "webhook":
                return action.action;
                break;
            case "notification":
                const type = action.notification && action.notification.message && action.notification.message.type ? action.notification.message.type : action.action;
                return type;
                break;
            case "write-attribute":
            case "update-attribute":
                if (!action.target) {
                    return;
                }
                if (action.target.matchedAssets) {
                    return getAssetTypeFromQuery(action.target.matchedAssets);
                }
                if (action.target.assets) {
                    return getAssetTypeFromQuery(action.target.assets);
                }
                break;
        }
    }

    protected setRecurrenceOption(value: RecurrenceOption) {

        switch (value) {
            case RecurrenceOption.ALWAYS:
                this.rule.recurrence = {mins: 0};
                break;
            case RecurrenceOption.ONCE_PER_HOUR:
                this.rule.recurrence = {mins: 60};
                break;
            case RecurrenceOption.ONCE_PER_DAY:
                this.rule.recurrence = {mins: 1440};
                break;
            case RecurrenceOption.ONCE_PER_WEEK:
                this.rule.recurrence = {mins: 10080};
                break;
            case RecurrenceOption.ONCE:
            default:
                delete this.rule.recurrence;
                break;
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected setActionType(actions: RuleActionUnion[], action: RuleActionUnion, value: string) {

        action.target = undefined;
        switch (action.action) {
            case "wait":
                action.millis = undefined;
                break;
            case "webhook":
                break;
            case "write-attribute":
                action.value = undefined;
                action.attributeName = undefined;
                break;
            case "notification":
                action.notification =  undefined;
                break;
            case "update-attribute":
                action.value = undefined;
                action.attributeName = undefined;
                action.index = undefined;
                action.key = undefined;
                action.updateAction = undefined;
                break;
        }

        if (value === ActionType.WAIT) {
            action.action = "wait";
        } else if (value == ActionType.WEBHOOK) {
            action = action as RuleActionWebhook;
            action.action = "webhook";
            action.webhook = {
                httpMethod: HTTPMethod.POST,
                payload: JSON.stringify({
                    rule: "%RULESET_NAME%",
                    assets: "%TRIGGER_ASSETS%"
                }, null, 4)
            };
        } else if (value === ActionType.EMAIL) {
            action = action as RuleActionNotification;
            action.action = "notification";
            action.notification = {
                message: {
                    type: "email",
                    subject: "%RULESET_NAME%",
                    html: "%TRIGGER_ASSETS%"
                }
            };
        }  else if (value === ActionType.PUSH_NOTIFICATION) {
            action = action as RuleActionNotification;
            action.action = "notification";
            action.notification = {
                message: {
                    type: "push",
                    title: "%RULESET_NAME%",
                    body: "%TRIGGER_ASSETS%"
                }
            };
        } else {
            action.action = "write-attribute";
            action.target = {
                matchedAssets: {
                    types: [
                        value
                    ]
                }
            };
        }

        const index = actions.indexOf(action);
        actions[index] = {...action};

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected removeAction(action?: RuleActionUnion) {
        if (!this.rule || !this.rule.then || !action) {
            return;
        }

        const index = this.rule.then.indexOf(action);
        if (index >= 0) {
            this.rule.then.splice(index, 1);
            this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
            this.requestUpdate();
        }
    }

    protected addAction(type: string | undefined, otherwise?: boolean) {

        if (!this.rule) {
            return;
        }

        let actionArray: RuleActionUnion[];
        let newAction: RuleActionUnion = {
            action: "write-attribute"
        };
        let template: RuleActionUnion | undefined;
        const templateConfig = this.config && this.config.json ? this.config.json : undefined;

        if (!otherwise) {
            if (!this.rule.then) {
                this.rule.then = [];
            }
            actionArray = this.rule.then;
            template = templateConfig && templateConfig.then ? templateConfig.then : undefined;
        } else {
            if (!this.rule.otherwise) {
                this.rule.otherwise = [];
            }
            actionArray = this.rule.otherwise;
            template = templateConfig && templateConfig.otherwise ? templateConfig.otherwise : undefined;
        }

        if (template) {
            newAction = JSON.parse(JSON.stringify(template)) as RuleActionUnion;
        }

        if (type) {
            this.setActionType(actionArray, newAction, type);
        }

        actionArray.push(newAction);
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
