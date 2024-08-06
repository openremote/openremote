var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { buttonStyle } from "../style";
import "./or-rule-asset-query";
import { getAssetTypeFromQuery } from "../index";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
import { AssetModelUtil } from "@openremote/model";
import i18next from "i18next";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { Util } from "@openremote/core";
import "./or-rule-action-attribute";
import "./or-rule-action-notification";
import "./or-rule-action-webhook";
import { translate } from "@openremote/or-translate";
const NOTIFICATION_COLOR = "4B87EA";
const WAIT_COLOR = "EACC54";
function getActionTypesMenu(config, assetInfos) {
    let addAssetTypes = true;
    let addWait = true;
    let addNotification = true;
    let addPushNotification = true;
    let addWebhook = true;
    if (config && config.controls && config.controls.allowedActionTypes) {
        addAssetTypes = config.controls.allowedActionTypes.indexOf("attribute" /* ActionType.ATTRIBUTE */) >= 0;
        addWait = config.controls.allowedActionTypes.indexOf("wait" /* ActionType.WAIT */) >= 0;
        addNotification = config.controls.allowedActionTypes.indexOf("email" /* ActionType.EMAIL */) >= 0;
        addPushNotification = config.controls.allowedActionTypes.indexOf("push" /* ActionType.PUSH_NOTIFICATION */) >= 0;
        addWebhook = config.controls.allowedActionTypes.indexOf("webhook" /* ActionType.WEBHOOK */) >= 0;
    }
    const menu = [];
    if (addAssetTypes && assetInfos) {
        menu.push(...assetInfos.filter((assetInfo) => assetInfo.assetDescriptor.descriptorType !== "agent").map((assetTypeInfo) => {
            const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
            const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
            const styleMap = color ? { "--or-icon-fill": "#" + color } : undefined;
            return {
                text: Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor),
                value: assetTypeInfo.assetDescriptor.name,
                icon: icon ? icon : AssetModelUtil.getAssetDescriptorIcon("ThingAsset" /* WellknownAssets.THINGASSET */),
                styleMap: styleMap
            };
        }));
    }
    menu.sort(Util.sortByString((listItem) => listItem === null || listItem === void 0 ? void 0 : listItem.value));
    menu.push(null); // divider
    if (addNotification) {
        menu.push({
            text: i18next.t("email"),
            icon: "email",
            value: "email" /* ActionType.EMAIL */,
            styleMap: { "--or-icon-fill": "#" + NOTIFICATION_COLOR }
        });
    }
    if (addPushNotification) {
        menu.push({
            text: i18next.t("push-notification"),
            icon: "cellphone-message",
            value: "push" /* ActionType.PUSH_NOTIFICATION */,
            styleMap: { "--or-icon-fill": "#" + NOTIFICATION_COLOR }
        });
    }
    if (addWait) {
        menu.push({
            text: i18next.t("wait"),
            icon: "timer",
            value: "wait" /* ActionType.WAIT */,
            styleMap: { "--or-icon-fill": "#" + WAIT_COLOR }
        });
    }
    if (addWebhook) {
        menu.push({
            text: i18next.t("webhook"),
            icon: "webhook",
            value: "webhook" /* ActionType.WEBHOOK */,
            styleMap: { "--or-icon-fill": "#" + NOTIFICATION_COLOR }
        });
    }
    return menu;
}
export var RecurrenceOption;
(function (RecurrenceOption) {
    RecurrenceOption["ALWAYS"] = "always";
    RecurrenceOption["ONCE"] = "once";
    RecurrenceOption["ONCE_PER_HOUR"] = "oncePerHour";
    RecurrenceOption["ONCE_PER_DAY"] = "oncePerDay";
    RecurrenceOption["ONCE_PER_WEEK"] = "oncePerWeek";
})(RecurrenceOption || (RecurrenceOption = {}));
function getRecurrenceMenu(config) {
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
const style = css `

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
let OrRuleThenOtherwise = class OrRuleThenOtherwise extends translate(i18next)(LitElement) {
    static get styles() {
        return style;
    }
    get thenAllowAdd() {
        return !this.config || !this.config.controls || this.config.controls.hideThenAddAction !== true;
    }
    ruleRecurrenceTemplate(reset, readonly) {
        let recurrenceTemplate = ``;
        const buttonColor = "inherit";
        let value = RecurrenceOption.ONCE;
        if (reset) {
            if (reset.mins === undefined || reset.mins === null) {
                value = RecurrenceOption.ONCE;
            }
            else if (reset.mins === 0) {
                value = RecurrenceOption.ALWAYS;
            }
            else if (reset.mins === 60) {
                value = RecurrenceOption.ONCE_PER_HOUR;
            }
            else if (reset.mins === 1440) {
                value = RecurrenceOption.ONCE_PER_DAY;
            }
            else if (reset.mins === 10080) {
                value = RecurrenceOption.ONCE_PER_WEEK;
            }
        }
        if (readonly) {
            recurrenceTemplate = html `
                <div style="--or-mwc-input-color: ${buttonColor}; margin-right: 6px;">
                    <or-mwc-input .type="${InputType.BUTTON}" label="${value}"></or-mwc-input>
                </div>
            `;
        }
        else {
            recurrenceTemplate = html `
                <div style="--or-mwc-input-color: ${buttonColor}; margin-right: 6px;">
                    ${getContentWithMenuTemplate(html `<or-mwc-input .type="${InputType.BUTTON}" label="${value}"></or-mwc-input>`, getRecurrenceMenu(this.config), value, (value) => this.setRecurrenceOption(value))}
                </div>
            `;
        }
        return html `
            <div class="rule-recurrence">
                ${recurrenceTemplate}
            </div>
        `;
    }
    ruleActionTemplate(actions, action, readonly) {
        const showTypeSelect = !this.config || !this.config.controls || this.config.controls.hideActionTypeOptions !== true;
        const type = this.getActionType(action);
        if (!type && !showTypeSelect) {
            return html `<span>INVALID CONFIG - NO TYPE SPECIFIED AND TYPE SELECTOR DISABLED</span>`;
        }
        let typeTemplate = ``;
        let template = ``;
        const thenAllowRemove = !this.readonly && (this.thenAllowAdd || this.rule.then.length > 1);
        if (showTypeSelect) {
            let buttonIcon;
            let buttonColor = "inherit";
            if (action.action) {
                switch (action.action) {
                    case "wait" /* ActionType.WAIT */:
                        buttonIcon = "timer";
                        buttonColor = WAIT_COLOR;
                        break;
                    case "webhook" /* ActionType.WEBHOOK */:
                        buttonIcon = "webhook";
                        buttonColor = NOTIFICATION_COLOR;
                        break;
                    case "notification":
                        action = action;
                        if (type === "push") {
                            buttonIcon = "cellphone-message";
                            buttonColor = NOTIFICATION_COLOR;
                        }
                        else {
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
                typeTemplate = html `
                    <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                        <or-mwc-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>
                    </div>
                `;
            }
            else {
                typeTemplate = html `
                    <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                        ${getContentWithMenuTemplate(html `<or-mwc-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>`, getActionTypesMenu(this.config, this.assetInfos), action.action, (value) => this.setActionType(actions, action, value))}
                    </div>
                `;
            }
        }
        if (type) {
            switch (type) {
                case "wait" /* ActionType.WAIT */:
                    template = html `<span>WAIT NOT IMPLEMENTED</span>`;
                    break;
                case "push" /* ActionType.PUSH_NOTIFICATION */:
                    template = html `<or-rule-action-notification id="push-notification" .rule="${this.rule}" .action="${action}" .actionType="${"push" /* ActionType.PUSH_NOTIFICATION */}" .config="${this.config}" .assetInfos="${this.assetInfos}" .readonly="${this.readonly}"></or-rule-action-notification>`;
                    break;
                case "email" /* ActionType.EMAIL */:
                    template = html `<or-rule-action-notification id="email-notification" .rule="${this.rule}" .action="${action}" .actionType="${"email" /* ActionType.EMAIL */}" .config="${this.config}" .assetInfos="${this.assetInfos}" .readonly="${this.readonly}"></or-rule-action-notification>`;
                    break;
                case "webhook" /* ActionType.WEBHOOK */:
                    template = html `<or-rule-action-webhook .rule="${this.rule}" .action="${action}" .actionType="${"webhook" /* ActionType.WEBHOOK */}"></or-rule-action-webhook>`;
                    break;
                default:
                    template = html `<or-rule-action-attribute .action="${action}" .targetTypeMap="${this.targetTypeMap}" .config="${this.config}" .assetInfos="${this.assetInfos}" .assetProvider="${this.assetProvider}" .readonly="${this.readonly}"></or-rule-action-attribute>`;
                    break;
            }
        }
        return html `
            <div class="rule-action">
                <div class="rule-action-wrapper">
                    ${typeTemplate}
                    ${template}
                </div>
                    ${thenAllowRemove ? html `
                        <button class="button-clear" @click="${() => this.removeAction(action)}"><or-icon icon="close-circle"></or-icon></input>
                    ` : ``}
            </div>
        `;
    }
    render() {
        const thenAllowAdd = !this.readonly && (this.thenAllowAdd || this.rule.then.length > 1);
        return html `
            <div>
                <or-panel .heading="${i18next.t("then")}...">
                    ${this.ruleRecurrenceTemplate(this.rule.recurrence, this.readonly)}

                    ${!this.rule.then ? `` : this.rule.then.map((action) => this.ruleActionTemplate(this.rule.then, action, this.readonly))}
                    ${thenAllowAdd ? html `
                        <span class="add-button-wrapper">
                            ${getContentWithMenuTemplate(html `<or-mwc-input class="plus-button" type="${InputType.BUTTON}" icon="plus"
                                                   .label="${i18next.t("rulesEditorAddAction")}"></or-mwc-input>`, getActionTypesMenu(this.config, this.assetInfos), undefined, (value) => this.addAction(value))}
                        </span>
                    ` : ``}
                </or-panel>
            </div>
        `;
    }
    getActionType(action) {
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
    setRecurrenceOption(value) {
        switch (value) {
            case RecurrenceOption.ALWAYS:
                this.rule.recurrence = { mins: 0 };
                break;
            case RecurrenceOption.ONCE_PER_HOUR:
                this.rule.recurrence = { mins: 60 };
                break;
            case RecurrenceOption.ONCE_PER_DAY:
                this.rule.recurrence = { mins: 1440 };
                break;
            case RecurrenceOption.ONCE_PER_WEEK:
                this.rule.recurrence = { mins: 10080 };
                break;
            case RecurrenceOption.ONCE:
            default:
                delete this.rule.recurrence;
                break;
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    setActionType(actions, action, value) {
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
                action.notification = undefined;
                break;
            case "update-attribute":
                action.value = undefined;
                action.attributeName = undefined;
                action.index = undefined;
                action.key = undefined;
                action.updateAction = undefined;
                break;
        }
        if (value === "wait" /* ActionType.WAIT */) {
            action.action = "wait";
        }
        else if (value == "webhook" /* ActionType.WEBHOOK */) {
            action = action;
            action.action = "webhook";
            action.webhook = {
                httpMethod: "POST" /* HTTPMethod.POST */,
                payload: JSON.stringify({
                    rule: "%RULESET_NAME%",
                    assets: "%TRIGGER_ASSETS%"
                }, null, 4)
            };
        }
        else if (value === "email" /* ActionType.EMAIL */) {
            action = action;
            action.action = "notification";
            action.notification = {
                message: {
                    type: "email",
                    subject: "%RULESET_NAME%",
                    html: "%TRIGGER_ASSETS%"
                }
            };
        }
        else if (value === "push" /* ActionType.PUSH_NOTIFICATION */) {
            action = action;
            action.action = "notification";
            action.notification = {
                message: {
                    type: "push",
                    title: "%RULESET_NAME%",
                    body: "%TRIGGER_ASSETS%"
                }
            };
        }
        else {
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
        actions[index] = Object.assign({}, action);
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    removeAction(action) {
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
    addAction(type, otherwise) {
        if (!this.rule) {
            return;
        }
        let actionArray;
        let newAction = {
            action: "write-attribute"
        };
        let template;
        const templateConfig = this.config && this.config.json ? this.config.json : undefined;
        if (!otherwise) {
            if (!this.rule.then) {
                this.rule.then = [];
            }
            actionArray = this.rule.then;
            template = templateConfig && templateConfig.then ? templateConfig.then : undefined;
        }
        else {
            if (!this.rule.otherwise) {
                this.rule.otherwise = [];
            }
            actionArray = this.rule.otherwise;
            template = templateConfig && templateConfig.otherwise ? templateConfig.otherwise : undefined;
        }
        if (template) {
            newAction = JSON.parse(JSON.stringify(template));
        }
        if (type) {
            this.setActionType(actionArray, newAction, type);
        }
        actionArray.push(newAction);
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
};
__decorate([
    property({ type: Array, attribute: false })
], OrRuleThenOtherwise.prototype, "targetTypeMap", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrRuleThenOtherwise.prototype, "rule", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleThenOtherwise.prototype, "readonly", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrRuleThenOtherwise.prototype, "assetInfos", void 0);
__decorate([
    property({ type: Object })
], OrRuleThenOtherwise.prototype, "assetProvider", void 0);
OrRuleThenOtherwise = __decorate([
    customElement("or-rule-then-otherwise")
], OrRuleThenOtherwise);
//# sourceMappingURL=or-rule-then-otherwise.js.map