var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { AssetModelUtil } from "@openremote/model";
import { getAssetTypeFromQuery } from "../index";
import "./or-rule-asset-query";
import "./or-rule-trigger-query";
import "@openremote/or-mwc-components/or-mwc-menu";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import "@openremote/or-icon";
import "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { Util } from "@openremote/core";
import { i18next, translate } from "@openremote/or-translate";
import { OrRulesJsonRuleChangedEvent } from "./or-rule-json-viewer";
const TIMER_COLOR = "4b87ea";
const DATE_TIME_COLOR = "6AEAA4";
export function getWhenTypesMenu(config, assetInfos) {
    let addAssetTypes = true;
    let addAgentTypes = true;
    let addTimer = true;
    if (config && config.controls && config.controls.allowedConditionTypes) {
        addAssetTypes = config.controls.allowedConditionTypes.indexOf("assetQuery" /* ConditionType.ASSET_QUERY */) >= 0;
        addAgentTypes = config.controls.allowedConditionTypes.indexOf("agentQuery" /* ConditionType.AGENT_QUERY */) >= 0;
        addTimer = config.controls.allowedConditionTypes.indexOf("time" /* ConditionType.TIME */) >= 0;
    }
    const menu = [];
    if (assetInfos) {
        if (addAssetTypes) {
            const items = assetInfos.filter((assetInfo) => assetInfo.assetDescriptor.descriptorType !== "agent").map((assetTypeInfo) => {
                const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
                const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
                const styleMap = color ? { "--or-icon-fill": "#" + color } : undefined;
                return {
                    text: Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor),
                    value: assetTypeInfo.assetDescriptor.name,
                    icon: icon ? icon : AssetModelUtil.getAssetDescriptorIcon("ThingAsset" /* WellknownAssets.THINGASSET */),
                    styleMap: styleMap
                };
            });
            menu.push(...items.sort(Util.sortByString((listItem) => listItem.text)));
        }
        if (addAssetTypes && addAgentTypes) {
            menu.push(null);
        }
        if (addAgentTypes) {
            const items = assetInfos.filter((assetInfo) => assetInfo.assetDescriptor.descriptorType === "agent").map((assetTypeInfo) => {
                const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
                const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
                const styleMap = color ? { "--or-icon-fill": "#" + color } : undefined;
                return {
                    text: Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor),
                    value: assetTypeInfo.assetDescriptor.name,
                    icon: icon ? icon : AssetModelUtil.getAssetDescriptorIcon("ThingAsset" /* WellknownAssets.THINGASSET */),
                    styleMap: styleMap
                };
            });
            menu.push(...items.sort(Util.sortByString((listItem) => listItem.text)));
        }
    }
    if (addTimer) {
        menu.push(null);
        menu.push({
            text: i18next.t("time"),
            icon: "timer",
            value: "time" /* ConditionType.TIME */,
            styleMap: { "--or-icon-fill": "#" + TIMER_COLOR }
        });
    }
    return menu;
}
export function updateRuleConditionType(ruleCondition, value, config) {
    if (!value) {
        ruleCondition.assets = undefined;
        ruleCondition.cron = undefined;
        ruleCondition.sun = undefined;
    }
    else if (value === "time" /* ConditionType.TIME */) {
        ruleCondition.assets = undefined;
        const date = new Date();
        ruleCondition.cron = Util.formatCronString(undefined, undefined, undefined, date.getUTCHours().toString(), date.getUTCMinutes().toString());
    }
    else {
        ruleCondition.cron = undefined;
        if (config && config.json && config.json.whenAssetQuery) {
            ruleCondition.assets = JSON.parse(JSON.stringify(config.json.whenAssetQuery));
        }
        else {
            ruleCondition.assets = {};
        }
        ruleCondition.assets.types = [value];
    }
}
// language=CSS
const style = css `
    :host {
        display: flex;
        flex-direction: row;
    }
    
    or-rule-asset-query {
        flex-grow: 1;
    }

    #type {
        white-space: nowrap;
        text-transform: capitalize;
        margin: 14px 3px auto 0;
    }
`;
let OrRuleCondition = class OrRuleCondition extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.readonly = false;
    }
    static get styles() {
        return style;
    }
    render() {
        const showTypeSelect = !this.config || !this.config.controls || this.config.controls.hideConditionTypeOptions !== true;
        const type = this.type;
        if (!type && !showTypeSelect) {
            return html `<span>INVALID CONFIG - NO TYPE SPECIFIED AND TYPE SELECTOR DISABLED</span>`;
        }
        let typeTemplate = ``;
        let template = ``;
        if (showTypeSelect) {
            let buttonIcon;
            let buttonColor = "inherit";
            if (type) {
                switch (type) {
                    case "time" /* ConditionType.TIME */:
                        buttonIcon = "timer";
                        buttonColor = TIMER_COLOR;
                        break;
                    default:
                        const ad = AssetModelUtil.getAssetDescriptor(type);
                        buttonIcon = AssetModelUtil.getAssetDescriptorIcon(ad) || buttonIcon;
                        buttonColor = AssetModelUtil.getAssetDescriptorColour(ad) || buttonColor;
                        break;
                }
            }
            if (this.readonly) {
                typeTemplate = html `
                <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                    <or-mwc-input readonly type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>
                </div>
                `;
            }
            else {
                typeTemplate = html `
                <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                    ${getContentWithMenuTemplate(html `<or-mwc-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>`, getWhenTypesMenu(this.config, this.assetInfos), type, (value) => this.type = value)}
                </div>
            `;
            }
        }
        if (type) {
            switch (type) {
                case "time" /* ConditionType.TIME */:
                    template = html `<or-rule-trigger-query id="asset-query" .condition="${this.ruleCondition}"></or-rule-trigger-query>`;
                    break;
                default:
                    template = html `<or-rule-asset-query id="asset-query" .config="${this.config}" .assetInfos="${this.assetInfos}" .readonly="${this.readonly}"
                                                         .condition="${this.ruleCondition}" .assetProvider="${this.assetProvider}"
                    ></or-rule-asset-query>`;
                    break;
            }
        }
        return html `
            ${typeTemplate}        
            ${template}
        `;
    }
    get type() {
        const assetType = getAssetTypeFromQuery(this.ruleCondition.assets);
        if (assetType) {
            return assetType;
        }
        if (this.ruleCondition.cron || this.ruleCondition.sun) {
            return "time" /* ConditionType.TIME */;
        }
    }
    set type(value) {
        updateRuleConditionType(this.ruleCondition, value, this.config);
        if (this._assetQuery) {
            this._assetQuery.refresh();
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleCondition.prototype, "ruleCondition", void 0);
__decorate([
    property({ type: Object })
], OrRuleCondition.prototype, "config", void 0);
__decorate([
    property({ type: Object })
], OrRuleCondition.prototype, "assetInfos", void 0);
__decorate([
    property({ type: Object })
], OrRuleCondition.prototype, "assetProvider", void 0);
__decorate([
    query("#asset-query")
], OrRuleCondition.prototype, "_assetQuery", void 0);
OrRuleCondition = __decorate([
    customElement("or-rule-condition")
], OrRuleCondition);
//# sourceMappingURL=or-rule-condition.js.map