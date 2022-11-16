import { JsonRule, RuleActionNotification } from "@openremote/model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import {css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "./modals/or-rule-webhook-modal";
import "./forms/or-rule-form-webhook";

// language=CSS
const style = css`
    :host {
        height: 100%;
    }

    :host > * {
        margin: 0 3px 6px;
    }

    .min-width {
        min-width: 200px;
    }
`;

@customElement("or-rule-action-webhook")
export class OrRuleActionWebhook extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public rule!: JsonRule;

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;


    /* ---------------------- */

    render() {
        return html`
            <div style="display: flex; align-items: center; height: 100%;">
                <or-rule-webhook-modal .action="${this.action}">
                    <or-rule-form-webhook></or-rule-form-webhook>
                </or-rule-webhook-modal>
            </div>
        `
    }
}
