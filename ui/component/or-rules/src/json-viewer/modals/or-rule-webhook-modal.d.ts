import { RuleActionWebhook } from "@openremote/model";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { LitElement, PropertyValues } from "lit";
export declare class OrRuleWebhookModal extends LitElement {
    protected action: RuleActionWebhook;
    title: string;
    constructor();
    firstUpdated(changedProperties: PropertyValues): void;
    renderDialogHTML(action: RuleActionWebhook): void;
    closeForm(event: OrInputChangedEvent): void;
    render(): import("lit-html").TemplateResult<1>;
}
