import { JsonRule, RuleActionWebhook } from "@openremote/model";
import { LitElement } from "lit";
import "./modals/or-rule-webhook-modal";
import "./forms/or-rule-form-webhook";
export declare class OrRuleActionWebhook extends LitElement {
    static get styles(): import("lit").CSSResult;
    rule: JsonRule;
    action: RuleActionWebhook;
    render(): import("lit-html").TemplateResult<1>;
}
