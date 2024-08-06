import { LitElement, TemplateResult } from "lit";
import { ActionType, RulesConfig } from "../index";
import { AssetQuery, AssetTypeInfo, JsonRule, RuleAction, RuleActionNotification, NotificationTargetType } from "@openremote/model";
import "./modals/or-rule-notification-modal";
import "./forms/or-rule-form-email-message";
import "./forms/or-rule-form-push-notification";
import "./or-rule-action-attribute";
export declare class OrRuleActionNotification extends LitElement {
    static get styles(): import("lit").CSSResult;
    rule: JsonRule;
    action: RuleActionNotification;
    actionType: ActionType;
    readonly?: boolean;
    assetInfos?: AssetTypeInfo[];
    config?: RulesConfig;
    protected static getActionTargetTemplate(targetTypeMap: [string, string?][], action: RuleAction, actionType: ActionType, readonly: boolean, config: RulesConfig | undefined, baseAssetQuery: AssetQuery | undefined, onTargetTypeChangedCallback: (type: NotificationTargetType) => void, onTargetChangedCallback: (type: NotificationTargetType, value: string | undefined) => void): PromiseLike<TemplateResult> | undefined;
    protected render(): TemplateResult<1> | "";
    protected _onTargetTypeChanged(targetType: NotificationTargetType): void;
    protected _onTargetChanged(targetType: NotificationTargetType, value: string | undefined): void;
}
