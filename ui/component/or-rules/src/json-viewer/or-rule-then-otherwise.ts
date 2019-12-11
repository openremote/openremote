import {css, customElement, html, LitElement, property, TemplateResult} from "lit-element";
import {buttonStyle} from "../style";
import "./or-rule-asset-query";
import {
    ActionType,
    getAssetTypeFromQuery,
    RulesConfig
} from "../index";
import {
    OrRulesJsonRuleChangedEvent
} from "./or-rule-json-viewer";
import {AssetDescriptor, JsonRule, RuleActionUnion, RuleConditionReset, RuleActionNotification, AssetType} from "@openremote/model";
import i18next from "i18next";
import {InputType} from "@openremote/or-input";
import {MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {AssetModelUtil} from "@openremote/core";
import "./or-rule-action-attribute";
import "./or-rule-action-notification";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import { translate } from "@openremote/or-translate";

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

type resetOptions = {
    [key: string]: string
}

const resetOptions:resetOptions = {
    "everyTime": "everyTime",
    "onlyOnce": "onlyOnce",
    "1h": "oncePerHour",
    "1d": "oncePerDay",
    "1w": "oncePerWeek",
    "1mn": "oncePerMonth"
};

function getResetMenu(config?: RulesConfig): MenuItem[] {

    const menu: MenuItem[] = [];

    menu.push(...Object.entries(resetOptions).map(([key, value]) => {
        const content = html`
                <span style="white-space: nowrap;">${i18next.t(value)}</span>
            `;
        return {content: content, value: key} as MenuItem
    }));

    return menu;
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
        align-items: center;
    }
    
    .rule-action-wrapper {
        flex-grow: 1;
        display: flex;
        align-items: center;
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
    }
    
    .add-button-wrapper > * {
        margin-right: 6px;
    }
    
    #type {
        white-space: nowrap;
        text-transform: capitalize;
        margin-right: 6px;
    }
    
    .rule-reset {
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

    protected ruleResetTemplate(reset: RuleConditionReset) {
        let resetTemplate: TemplateResult | string = ``;

        let buttonIcon = undefined;
        let buttonColor = "inherit";

        let value;

        if (!reset) value = "onlyOnce";
            else if (reset.valueChanges && reset.timestampChanges) value = "everyTime";
            else if (reset.timer) value = reset.timer;
            else if (this.config && this.config.json && this.config.json.rule && this.config.json.rule.reset) value = this.config.json.rule.reset.timer;


        resetTemplate = html`
                <div style="color: #${buttonColor}; margin-right: 6px;">
                    ${getContentWithMenuTemplate(
            html`<or-input type="${InputType.BUTTON}"  label="${value ? i18next.t(resetOptions[value]) : i18next.t('frequency') }"></or-input>`,
            getResetMenu(this.config),
            value,
            (value: string) => this.setResetOption(value))}
                </div>
            `;

        return html`
            <div class="rule-reset">
                ${resetTemplate}
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
                    ${this.ruleResetTemplate(this.rule.reset!)}

                    ${!this.rule.then ? `` : this.rule.then.map((action: RuleActionUnion) => this.ruleActionTemplate(this.rule.then!, action))}
                    ${this.thenAllowAdd ? html`
                        <span class="add-button-wrapper" style="white-space: nowrap; text-transform: capitalize">
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

    protected setResetOption(value: string) {

        switch (value) {
            case "onlyOnce":
                delete this.rule.reset;
                break;
            case "everyTime":
                if (this.rule.reset) delete this.rule.reset.timer;
                this.rule.reset = {
                    valueChanges: true,
                    timestampChanges: true
                };
                break;
            default:
                if (this.rule.reset) {
                    if (this.rule.reset.valueChanges) delete this.rule.reset.valueChanges;
                    if (this.rule.reset.timestampChanges) delete this.rule.reset.timestampChanges;
                    this.rule.reset.timer = value;
                } else {
                    this.rule.reset = {timer: value};
                }
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
            }
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
