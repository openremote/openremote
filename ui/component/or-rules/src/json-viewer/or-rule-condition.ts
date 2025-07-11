/*
 * Copyright 2025, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {AssetTypeInfo, RuleCondition, WellknownAssets, AssetModelUtil} from "@openremote/model";
import {ConditionType, getAssetTypeFromQuery, RulesConfig} from "../index";
import {OrRuleAssetQuery} from "./or-rule-asset-query";
import "./or-rule-trigger-query";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-icon";
import {i18next, translate} from "@openremote/or-translate";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {Util} from "@openremote/core";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";

const TIMER_COLOR = "4b87ea";
const DATE_TIME_COLOR = "6AEAA4";

export function getWhenTypesMenu(config?: RulesConfig, assetInfos?: AssetTypeInfo[]): (ListItem | null)[] {

    let addAssetTypes = true;
    let addAgentTypes = true;
    let addTimer = true;

    if (config && config.controls && config.controls.allowedConditionTypes) {
        addAssetTypes = config.controls.allowedConditionTypes.indexOf(ConditionType.ASSET_QUERY) >= 0;
        addAgentTypes = config.controls.allowedConditionTypes.indexOf(ConditionType.AGENT_QUERY) >= 0;
        addTimer = config.controls.allowedConditionTypes.indexOf(ConditionType.TIME) >= 0;
    }

    const menu: (ListItem | null)[] = [];

    if (assetInfos) {

        if (addAssetTypes) {
            const items = assetInfos.filter((assetInfo) => assetInfo.assetDescriptor!.descriptorType !== "agent").map((assetTypeInfo) => {

                const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
                const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
                const styleMap = color ? {"--or-icon-fill": "#" + color} : undefined;

                return {
                    text: Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor!),
                    value: assetTypeInfo.assetDescriptor!.name,
                    icon: icon || AssetModelUtil.getAssetDescriptorIcon(WellknownAssets.THINGASSET),
                    styleMap: styleMap
                } as ListItem;
            });

            menu.push(...items.sort(Util.sortByString((listItem) => listItem.text!)));
        }

        if (addAssetTypes && addAgentTypes) {
            menu.push(null);
        }

        if (addAgentTypes) {
            const items = assetInfos.filter((assetInfo) => assetInfo.assetDescriptor!.descriptorType === "agent").map((assetTypeInfo) => {

                const color = AssetModelUtil.getAssetDescriptorColour(assetTypeInfo);
                const icon = AssetModelUtil.getAssetDescriptorIcon(assetTypeInfo);
                const styleMap = color ? {"--or-icon-fill": "#" + color} : undefined;

                return {
                    text: Util.getAssetTypeLabel(assetTypeInfo.assetDescriptor!),
                    value: assetTypeInfo.assetDescriptor!.name,
                    icon: icon || AssetModelUtil.getAssetDescriptorIcon(WellknownAssets.THINGASSET),
                    styleMap: styleMap
                } as ListItem;
            });

            menu.push(...items.sort(Util.sortByString((listItem) => listItem.text!)));
        }
    }

    if (addTimer) {
        menu.push(null);
        menu.push({
            text: i18next.t("time"),
            icon: "timer",
            value: ConditionType.TIME,
            styleMap: {"--or-icon-fill": "#" + TIMER_COLOR}
        } as ListItem);
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

@customElement("or-rule-condition")
class OrRuleCondition extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public ruleCondition!: RuleCondition;

    public readonly = false;

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: Object})
    public assetInfos?: AssetTypeInfo[];

    @property({type: Object})
    public assetProvider?: (type: string) => Promise<any[] | undefined>

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
                <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                    <or-mwc-input readonly type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>
                </div>
                `;
            } else {
                typeTemplate = html`
                <div id="type" style="--or-mwc-input-color: #${buttonColor}">
                    ${getContentWithMenuTemplate(
                        html`<or-mwc-input type="${InputType.BUTTON}" .icon="${buttonIcon || ""}"></or-mwc-input>`,
                        getWhenTypesMenu(this.config, this.assetInfos),
                        type,
                        (value) => this.type = value as ConditionType)}
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
        if (this._assetQuery) {
            this._assetQuery.refresh();
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
