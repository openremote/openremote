import {customElement, html, css, LitElement, property, TemplateResult, query} from "lit-element";
import {AssetDescriptor, AssetQueryMatch, RuleCondition, AssetType} from "@openremote/model";
import {
    ActionType,
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
import {i18next, translate} from "@openremote/or-translate";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import { OrRuleAssetQuery } from "./or-rule-asset-query";

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

    if (addDatetime) {
        menu.push({
            text: i18next.t("datetime"),
            icon: "clock",
            value: ConditionType.DATE_TIME,
            styleMap: {"--or-icon-fill": "#" + DATE_TIME_COLOR}
        } as MenuItem);
    }

    if (addTimer) {
        menu.push({
            text: i18next.t("timer"),
            icon: "timer",
            value: ConditionType.TIMER,
            styleMap: {"--or-icon-fill": "#" + TIMER_COLOR}
        } as MenuItem);
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

    #type {
        white-space: nowrap;
        text-transform: capitalize;
        margin-right: 6px;
    }
`;

@customElement("or-rule-condition")
class OrRuleCondition extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public ruleCondition!: RuleCondition;

    public readonly: boolean = false;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    @query("#asset-query")
    protected _assetQuery?: OrRuleAssetQuery;

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
            
            let buttonIcon;
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
                <div id="type" style="color: #${buttonColor}">
                    ${getContentWithMenuTemplate(
                        html`<or-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-input>`,
                        getWhenTypesMenu(this.config, this.assetDescriptors),
                        type,
                        (values: string[] | string) => this.type = values as string)}
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
                    template = html`<or-rule-asset-query id="asset-query" .config="${this.config}" .assetDescriptors="${this.assetDescriptors}" .readonly="${this.readonly}" .condition="${this.ruleCondition}"></or-rule-asset-query>`;
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
        if (this._assetQuery) {
            this._assetQuery.refresh();
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
