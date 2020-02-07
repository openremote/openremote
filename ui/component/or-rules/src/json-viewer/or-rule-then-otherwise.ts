import {css, customElement, html, LitElement, property, TemplateResult} from "lit-element";
import {buttonStyle} from "../style";
import "./or-rule-asset-query";
import {ActionType, getAssetTypeFromQuery, RulesConfig} from "../index";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {
    AssetDescriptor,
    AssetType,
    JsonRule,
    RuleActionUnion,
    RuleRecurrence,
    RuleRecurrenceScope
} from "@openremote/model";
import i18next from "i18next";
import {InputType} from "@openremote/or-input";
import {getContentWithMenuTemplate, MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {AssetModelUtil} from "@openremote/core";
import "./or-rule-action-attribute";
import "./or-rule-action-notification";
import {translate} from "@openremote/or-translate";

const NOTIFICATION_COLOR = "4B87EA";
const WAIT_COLOR = "EACC54";

function getActionTypesMenu(config?: RulesConfig, assetDescriptors?: AssetDescriptor[]): MenuItem[] {

    let addAssetTypes = true;
    let addWait = true;
    let addNotification = true;

    if (config && config.controls && config.controls.allowedActionTypes) {
        addAssetTypes = config.controls.allowedActionTypes.indexOf(ActionType.ATTRIBUTE) >= 0;
        addWait = config.controls.allowedActionTypes.indexOf(ActionType.WAIT) >= 0;
        addNotification = config.controls.allowedActionTypes.indexOf(ActionType.NOTIFICATION) >= 0;
    }

    const menu: MenuItem[] = [];

    if (addAssetTypes && assetDescriptors) {
        menu.push(...assetDescriptors.map((ad) => {

            const color = AssetModelUtil.getAssetDescriptorColor(ad);
            const icon = AssetModelUtil.getAssetDescriptorIcon(ad);
            const styleMap = color ? {"--or-icon-fill": "#" + color} : undefined;

            return {
                text: i18next.t(ad.name!, {defaultValue: ad.name!.replace(/_/g, " ").toLowerCase()}),
                value: ad.type,
                icon: icon ? icon : AssetType.THING.icon,
                styleMap: styleMap
            } as MenuItem;
        }));
    }

    if (addNotification) {
        menu.push({
            text: i18next.t("notification"),
            icon: "email",
            value: ActionType.NOTIFICATION,
            styleMap: {"--or-icon-fill": "#" + NOTIFICATION_COLOR}
        } as MenuItem);
    }

    if (addWait) {
        menu.push({
            text: i18next.t("wait"),
            icon: "timer",
            value: ActionType.WAIT,
            styleMap: {"--or-icon-fill": "#" + WAIT_COLOR}
        } as MenuItem);
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

function getRecurrenceMenu(config?: RulesConfig): MenuItem[] {

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

    @property({type: Object, attribute: false})
    public rule!: JsonRule;

    public readonly?: boolean;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    protected get thenAllowAdd() {
        return !this.config || !this.config.controls || this.config.controls.hideThenAddAction !== true;
    }

    protected ruleRecurrenceTemplate(reset: RuleRecurrence | undefined) {
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

        recurrenceTemplate = html`
                <div style="color: #${buttonColor}; margin-right: 6px;">
                    ${getContentWithMenuTemplate(
                        html`<or-input .type="${InputType.BUTTON}" .label="${i18next.t(value)}"></or-input>`,
                        getRecurrenceMenu(this.config),
                        value,
                        (value) => this.setRecurrenceOption(value as RecurrenceOption))}
                </div>
            `;

        return html`
            <div class="rule-recurrence">
                ${recurrenceTemplate}
            </div>
        `;
    }

    protected ruleActionTemplate(actions: RuleActionUnion[], action: RuleActionUnion) {

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
                switch (action.action as ActionType) {
                    case ActionType.WAIT:
                        buttonIcon = "timer";
                        buttonColor = WAIT_COLOR;
                        break;
                    case ActionType.NOTIFICATION:
                        buttonIcon = "email";
                        buttonColor = NOTIFICATION_COLOR;
                        break;
                    default:
                        const ad = AssetModelUtil.getAssetDescriptor(type);
                        buttonIcon = AssetModelUtil.getAssetDescriptorIcon(ad);
                        buttonColor = AssetModelUtil.getAssetDescriptorColor(ad) || buttonColor;
                        break;
                }
            }

            typeTemplate = html`
                <div id="type" style="color: #${buttonColor}">
                    ${getContentWithMenuTemplate(
                        html`<or-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-input>`,
                        getActionTypesMenu(this.config, this.assetDescriptors),
                        action.action,
                        (values: string[] | string) => this.setActionType(actions, action, values as string))}
                </div>
            `;
        }

        if (type) {
            switch (type) {
                case ActionType.WAIT:
                    template = html`<span>WAIT NOT IMPLEMENTED</span>`;
                    break;
                case ActionType.NOTIFICATION:
                    template = html`<or-rule-action-notification id="rule-notification" .action="${action}" .config="${this.config}" .readonly="${this.readonly}"></or-rule-action-notification>`;
                    break;
                default:
                    template = html`<or-rule-action-attribute .action="${action}" .config="${this.config}" .assetDescriptors="${this.assetDescriptors}" .readonly="${this.readonly}"></or-rule-action-attribute>`;
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

        return html`
            <div>
                <or-panel .heading="${i18next.t("then")}...">
                    ${this.ruleRecurrenceTemplate(this.rule.recurrence)}

                    ${!this.rule.then ? `` : this.rule.then.map((action: RuleActionUnion) => this.ruleActionTemplate(this.rule.then!, action))}
                    ${this.thenAllowAdd ? html`
                        <span class="add-button-wrapper">
                            ${getContentWithMenuTemplate(
                                html`<or-input class="plus-button" type="${InputType.BUTTON}" icon="plus"></or-input>`,
                                getActionTypesMenu(this.config, this.assetDescriptors),
                                undefined,
                                (values: string[] | string) => this.addAction(values as string))}
                            <span>${i18next.t("rulesEditorAddAction")}</span>
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
            case "notification":
                return action.action;
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
        } else if (value === ActionType.NOTIFICATION) {
            action.action = "notification";
        } else {
            action.action = "write-attribute";
            action.target = {
                matchedAssets: {
                    types: [
                        {
                            predicateType: "string",
                            value: value
                        }
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
