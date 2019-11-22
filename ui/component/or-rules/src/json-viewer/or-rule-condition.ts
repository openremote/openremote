import {customElement, html, css, LitElement, property, TemplateResult} from "lit-element";
import {AssetDescriptor, AssetQueryMatch, RuleCondition} from "@openremote/model";
import {
    ConditionType,
    getAssetTypeFromQuery,
    RulesConfig
} from "../index";
import "./or-rule-asset-query";
import "@openremote/or-mwc-components/dist/or-mwc-menu";
import {MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import "@openremote/or-icon";
import "@openremote/or-translate";
import {getAssetDescriptorIconTemplate} from "@openremote/or-icon";
import {InputType} from "@openremote/or-input";
import {AssetModelUtil} from "@openremote/core";
import {i18next} from "@openremote/or-translate";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/dist/or-mwc-menu";

const TIMER_COLOR = "4b87ea";
const DATE_TIME_COLOR = "6AEAA4";

export function getWhenTypesMenu(config?: RulesConfig, assetDescriptors?: AssetDescriptor[]): MenuItem[] {

    let addAssetTypes = true;
    let addDatetime = true;
    let addTimer = true;

    if (config && config.controls && config.controls.allowedConditionTypes) {
        addAssetTypes = config.controls.allowedConditionTypes.indexOf(ConditionType.ASSET_QUERY) >= 0;
        addDatetime = config.controls.allowedConditionTypes.indexOf(ConditionType.DATE_TIME) >= 0;
        addTimer = config.controls.allowedConditionTypes.indexOf(ConditionType.TIMER) >= 0;
    }

    const menu: MenuItem[] = [];

    if (addAssetTypes && assetDescriptors) {
        menu.push(...assetDescriptors.map((ad) => {
            const content = html`
                    ${getAssetDescriptorIconTemplate(ad)}
                    &nbsp;&nbsp;<span style="text-transform: capitalize">${i18next.t(ad.name!, {defaultValue: ad.name!.replace(/_/g, " ").toLowerCase()})}</span>
                `;
            return {content: content, value: ad.type} as MenuItem
        }));
    }

    if (addDatetime) {
        const content = html`
                <or-icon style="--or-icon-fill: #${DATE_TIME_COLOR}" icon="clock"></or-icon>
                <span style="text-transform: capitalize">&nbsp;<or-translate value="datetime"></or-translate></span>
            `;
        menu.push({content: content, value: ConditionType.DATE_TIME} as MenuItem);
    }

    if (addTimer) {
        const content = html`
                <or-icon style="--or-icon-fill: #${TIMER_COLOR}" icon="timer"></or-icon>
                <span style="text-transform: capitalize">&nbsp;<or-translate value="timer"></or-translate></span>
            `;
        menu.push({content: content, value: ConditionType.TIMER} as MenuItem);
    }

    return menu;
}

export function updateRuleConditionType(ruleCondition: RuleCondition, value: string | undefined, config?: RulesConfig) {

    if (!value || value === "") {
        ruleCondition.assets = undefined;
        ruleCondition.timer = undefined;
        ruleCondition.datetime = undefined;
    } else if (value === ConditionType.TIMER) {
        ruleCondition.assets = undefined;
        ruleCondition.datetime = undefined;
        ruleCondition.timer = "1h";
    } else if (value === ConditionType.DATE_TIME) {
        ruleCondition.assets = undefined;
        ruleCondition.timer = undefined;
        ruleCondition.datetime = {
            predicateType: "datetime"
        };
    } else {
        ruleCondition.timer = undefined;
        ruleCondition.datetime = undefined;

        if (config && config.json && config.json.whenAssetQuery) {
            ruleCondition.assets = JSON.parse(JSON.stringify(config.json.whenAssetQuery));
        } else {
            ruleCondition.assets = {};
        }

        if (!ruleCondition.assets!.types || ruleCondition.assets!.types.length === 0) {
            ruleCondition.assets!.types = [{
                predicateType: "string",
                match: AssetQueryMatch.EXACT
            }];
        }
        ruleCondition.assets!.types[0].value = value;
    }
}

// language=CSS
const style = css`
    :host {
        display: flex;
        flex-direction: row;
        align-items: center;
    }
    
    or-rule-asset-query {
        flex-grow: 1;
    }

    #type-selector {
        margin-top: 10px;
    }
    
    #type {
        margin-right: 10px;
    }
`;

@customElement("or-rule-condition")
class OrRuleCondition extends LitElement {

    @property({type: Object, attribute: false})
    public ruleCondition!: RuleCondition;

    public readonly: boolean = false;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    static get styles() {
        return style;
    }

    protected render() {

        const showTypeSelect = !this.config || !this.config.controls || this.config.controls.hideConditionTypeOptions !== true;
        const type = this.type;

        if (!type && !showTypeSelect) {
            return html`<span>INVALID CONFIG - NO TYPE SPECIFIED AND TYPE SELECTOR DISABLED</span>`;
        }

        let typeTemplate: TemplateResult | string = ``;
        let template: TemplateResult | string = ``;

        if (showTypeSelect) {
            
            let buttonIcon = undefined;
            let buttonColor = "inherit";

            if (type) {
                switch (type) {
                    case ConditionType.DATE_TIME:
                        buttonIcon = "clock";
                        buttonColor = DATE_TIME_COLOR;
                        break;
                    case ConditionType.TIMER:
                        buttonIcon = "timer";
                        buttonColor = TIMER_COLOR;
                        break;
                    default:
                        const ad = AssetModelUtil.getAssetDescriptor(type);
                        buttonIcon = AssetModelUtil.getAssetDescriptorIcon(ad) || buttonIcon;
                        buttonColor = AssetModelUtil.getAssetDescriptorColor(ad) || buttonColor;
                        break;
                }
            }
            
            typeTemplate = html`
                <div id="type" style="--or-input-text-color: #${buttonColor}">
                    ${getContentWithMenuTemplate(
                        html`<or-input class="menu-button" type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-input>`,
                        getWhenTypesMenu(this.config, this.assetDescriptors),
                        type,
                        (value: string) => this.type = value)}
                </div>
            `;
        }
        
        if (type) {
            switch (type) {
                case ConditionType.DATE_TIME:
                    template = html`<span>DATE TIME NOT IMPLEMENTED</span>`;
                    break;
                case ConditionType.TIMER:
                    template = html`<span>TIMER NOT IMPLEMENTED</span>`;
                    break;
                default:
                    template = html`<or-rule-asset-query .config="${this.config}" .assetDescriptors="${this.assetDescriptors}" .readonly="${this.readonly}" .query="${this.ruleCondition.assets}"></or-rule-asset-query>`;
                    break;
            }
        }

        return html`
            ${typeTemplate}        
            ${template}
        `;
    }

    protected get type(): string | undefined {

        const assetType = getAssetTypeFromQuery(this.ruleCondition.assets);
        if (assetType) {
            return assetType;
        }

        if (this.ruleCondition.timer) {
            return ConditionType.TIMER;
        }

        if (this.ruleCondition.datetime) {
            return ConditionType.DATE_TIME;
        }
    }

    protected set type(value: string | undefined) {

        updateRuleConditionType(this.ruleCondition, value, this.config);
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
