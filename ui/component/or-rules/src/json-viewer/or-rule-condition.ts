import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {AssetTypeInfo, RuleCondition, WellknownAssets, AssetModelUtil, AssetQuery} from "@openremote/model";
import {ConditionType, getAssetTypeFromQuery, RulesConfig} from "../index";
import "./or-rule-asset-query";
import "./or-rule-trigger-query";
import "@openremote/or-icon";
import "@openremote/or-translate";
import {Util} from "@openremote/core";
import {i18next, translate} from "@openremote/or-translate";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import {OrRuleAssetQuery} from "./or-rule-asset-query";
import {createMenuBarItem, MenuBarItem, SubMenuItem} from "@openremote/or-vaadin-components/or-vaadin-menu-bar";

const TIMER_COLOR = "4b87ea";
const DATE_TIME_COLOR = "6AEAA4";

export function getWhenTypesMenu(config?: RulesConfig, assetInfos?: AssetTypeInfo[], selectedType?: string): SubMenuItem[] {

    let addAssetTypes = true;
    let addAgentTypes = true;
    let addTimer = true;

    if (config && config.controls && config.controls.allowedConditionTypes) {
        addAssetTypes = config.controls.allowedConditionTypes.indexOf(ConditionType.ASSET_QUERY) >= 0;
        addAgentTypes = config.controls.allowedConditionTypes.indexOf(ConditionType.AGENT_QUERY) >= 0;
        addTimer = config.controls.allowedConditionTypes.indexOf(ConditionType.TIME) >= 0;
    }

    const getMenuItem = (assetTypeInfo: AssetTypeInfo): SubMenuItem => {
        const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
        const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
        return {
            checked: selectedType && assetTypeInfo.assetDescriptor!.name === selectedType,
            component: createMenuBarItem(html`
                <div style="display: flex; align-items: center; gap: 6px;">
                    <or-icon style=${color ? `--or-icon-fill: #${color}` : undefined}
                             icon=${icon ? icon : AssetModelUtil.getAssetDescriptorIcon(WellknownAssets.THINGASSET)}
                    ></or-icon>
                    <span>${Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor!)}</span>
                </div>
            `),
            value: assetTypeInfo.assetDescriptor!.name,
        } as SubMenuItem;
    }

    const menu: SubMenuItem[] = [];

    if (assetInfos) {

        if (addAssetTypes) {
            const items = assetInfos
                .filter((assetInfo) => assetInfo.assetDescriptor!.descriptorType !== "agent")
                .sort(Util.sortByString(assetTypeInfo => Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor!)))
                .map(getMenuItem);

            menu.push(...items);
        }

        if (addAssetTypes && addAgentTypes) {
            menu.push({ component: "hr" });
        }

        if (addAgentTypes) {
            const items = assetInfos
                .filter((assetInfo) => assetInfo.assetDescriptor!.descriptorType === "agent")
                .sort(Util.sortByString(assetTypeInfo => Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor!)))
                .map(getMenuItem);
            menu.push(...items);
        }
    }

    if (addTimer) {
        menu.push({ component: "hr" });
        menu.push({
            component: createMenuBarItem(html`
                <or-icon style="--or-icon-fill: #${TIMER_COLOR}" icon="timer"></or-icon>
                <or-translate value="time"></or-translate>
            `),
            value: ConditionType.TIME,
        } as SubMenuItem);
    }

    return menu;
}

export function updateRuleConditionType(ruleCondition: RuleCondition, value: string | ConditionType | undefined, config?: RulesConfig) {

    if (!value) {
        ruleCondition.assets = undefined;
        ruleCondition.cron = undefined;
        ruleCondition.sun = undefined;
    } else if (value === ConditionType.TIME) {
        ruleCondition.assets = undefined;
        const date = new Date();
        ruleCondition.cron = Util.formatCronString(undefined, undefined, undefined, date.getUTCHours().toString(), date.getUTCMinutes().toString());
    } else {
        ruleCondition.cron = undefined;

        if (config && config.json && config.json.whenAssetQuery) {
            ruleCondition.assets = JSON.parse(JSON.stringify(config.json.whenAssetQuery));
        } else {
            ruleCondition.assets = {};
        }
        ruleCondition.assets!.types = [value];
    }
}

// language=CSS
const style = css`
    :host {
        display: flex;
        flex-direction: row;
        align-items: baseline;
    }
    
    or-rule-asset-query {
        flex-grow: 1;
    }

    #type {
        white-space: nowrap;
        text-transform: capitalize;
    }
`;

@customElement("or-rule-condition")
class OrRuleCondition extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public ruleCondition!: RuleCondition;

    public readonly: boolean = false;

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Object})
    public assetInfos?: AssetTypeInfo[];

    @property({type: Object})
    public assetProvider?: (type: string, query?: AssetQuery) => Promise<any[] | undefined>

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
                    case ConditionType.TIME:
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
            if(this.readonly) {
                typeTemplate = html`
                    <div id="type" style="--or-icon-fill: #${buttonColor}">
                        <or-vaadin-button disabled theme="icon tertiary">
                            <or-icon icon=${buttonIcon ?? ""}></or-icon>
                        </or-vaadin-button>
                    </div>
                `;
            } else {
                const menuItems: MenuBarItem[] = [{
                    component: createMenuBarItem(html`<or-icon icon=${buttonIcon ?? ""}></or-icon>`),
                    children: getWhenTypesMenu(this.config, this.assetInfos, type)
                }]
                typeTemplate = html`
                    <div id="type" style="--or-icon-fill: #${buttonColor}">
                        <or-vaadin-menu-bar theme="icon" .items=${menuItems} 
                                            @item-selected=${(ev: CustomEvent) => this.type = ev.detail.value.value}>
                        </or-vaadin-menu-bar>
                    </div>
                `;
            }

        }

        if (type) {
            switch (type) {
                case ConditionType.TIME:
                    template = html`<or-rule-trigger-query id="asset-query" .condition="${this.ruleCondition}"></or-rule-trigger-query>`;
                    break;
                default:
                    template = html`<or-rule-asset-query id="asset-query" .config="${this.config}" .assetInfos="${this.assetInfos}" .readonly="${this.readonly}"
                                                         .condition="${this.ruleCondition}" .assetProvider="${this.assetProvider}"
                    ></or-rule-asset-query>`;
                    break;
            }
        }

        return html`
            ${typeTemplate}        
            ${template}
        `;
    }

    protected get type(): string | ConditionType | undefined {

        const assetType = getAssetTypeFromQuery(this.ruleCondition.assets);
        if (assetType) {
            return assetType;
        }

        if (this.ruleCondition.cron || this.ruleCondition.sun) {
            return ConditionType.TIME;
        }
    }

    protected set type(value: string | ConditionType | undefined) {
        updateRuleConditionType(this.ruleCondition, value, this.config);
        if (this._assetQuery?.refresh) {
            this._assetQuery.refresh();
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
