import {customElement, html, LitElement, TemplateResult, property} from "lit-element";
import {
    BaseAssetQueryMatch,
    RuleCondition,
    AssetDescriptor,
} from "@openremote/model";
import {
    RulesConfig,
    ConditionType,
    getAssetTypeFromQuery, OrRuleChangedEvent
} from "./index";
import "./or-rule-asset-query";
import {OrSelectChangedEvent} from "@openremote/or-select";
import {conditionEditorStyle} from "./style";
import i18next from "i18next";

@customElement("or-rule-condition")
class OrRuleCondition extends LitElement {

    @property({type: Object})
    public ruleCondition?: RuleCondition;

    public readonly: boolean = false;

    public config?: RulesConfig;

    public assetDescriptors?: AssetDescriptor[];

    static get styles() {
        return conditionEditorStyle;
    }

    protected render() {

        if (!this.ruleCondition) {
            return;
        }

        const showTypeSelect = !this.config || !this.config.controls || this.config.controls.hideConditionTypeOptions !== true;
        const type = this.type;

        if (!type && !showTypeSelect) {
            return html`<span>INVALID CONFIG - NO TYPE SPECIFIED AND TYPE SELECTOR DISABLED</span>`;
        }

        let template: TemplateResult | string = ``;

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
            ${showTypeSelect ? html`<or-select @or-select-changed="${(e: OrSelectChangedEvent) => this.type = e.detail.value}" .options="${this.getTypes()}" .value="${type}" ?readonly="${this.readonly}"></or-select>` : ``}
        
            ${template}
        `;
    }

    protected getTypes() {

        let addAssetTypes = true;
        let addDatetime = true;
        let addTimer = true;

        if (this.config && this.config.controls && this.config.controls.allowedConditionTypes) {
            addAssetTypes = this.config.controls.allowedConditionTypes.indexOf(ConditionType.ASSET_QUERY) >= 0;
            addDatetime = this.config.controls.allowedConditionTypes.indexOf(ConditionType.DATE_TIME) >= 0;
            addTimer = this.config.controls.allowedConditionTypes.indexOf(ConditionType.TIMER) >= 0;
        }

        const types: [string, string][] = [];
        
        if (addAssetTypes && this.assetDescriptors) {
            this.assetDescriptors.forEach((ad) => types.push([ad.type!, ad.name!]));
        }

        if (types.length > 0 && (addDatetime || addTimer)) {
            types.push(["", "---"]); // Empty spacer
        }
        
        if (addDatetime) {
            types.push([ConditionType.DATE_TIME, i18next.t(ConditionType.DATE_TIME)]);
        }
        if (addTimer) {
            types.push([ConditionType.TIMER, i18next.t(ConditionType.TIMER)]);
        }

        return types;
    }

    protected get type(): string | undefined {
        if (!this.ruleCondition) {
            return;
        }

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
        if (!this.ruleCondition) {
            return;
        }

        if (!value || value === "") {
            this.ruleCondition.assets = undefined;
            this.ruleCondition.timer = undefined;
            this.ruleCondition.datetime = undefined;
            this.dispatchEvent(new OrRuleChangedEvent());
            this.requestUpdate();
            return;
        }

        if (value === ConditionType.TIMER) {
            this.ruleCondition.assets = undefined;
            this.ruleCondition.datetime = undefined;
            this.ruleCondition.timer = "1h";
            this.dispatchEvent(new OrRuleChangedEvent());
            this.requestUpdate();
            return;
        }

        if (value === ConditionType.DATE_TIME) {
            this.ruleCondition.assets = undefined;
            this.ruleCondition.timer = undefined;
            this.ruleCondition.datetime = {
                predicateType: "datetime"
            };
            this.dispatchEvent(new OrRuleChangedEvent());
            this.requestUpdate();
            return;
        }

        this.ruleCondition.timer = undefined;
        this.ruleCondition.datetime = undefined;

        if (this.config && this.config.templates && this.config.templates.whenAssetQuery) {
            this.ruleCondition.assets = JSON.parse(JSON.stringify(this.config.templates.whenAssetQuery));
        } else {
            this.ruleCondition.assets = {};
        }

        if (!this.ruleCondition!.assets!.types || this.ruleCondition!.assets!.types.length === 0) {
            this.ruleCondition!.assets!.types = [{
                predicateType: "string",
                match: BaseAssetQueryMatch.EXACT
            }];
        }

        this.ruleCondition!.assets!.types[0].value = value;
        this.dispatchEvent(new OrRuleChangedEvent());
        this.requestUpdate();
    }
}
