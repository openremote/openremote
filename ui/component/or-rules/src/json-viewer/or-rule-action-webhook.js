var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "./modals/or-rule-webhook-modal";
import "./forms/or-rule-form-webhook";
// language=CSS
const style = css `
    :host {
        height: 100%;
        margin: 2px 3px auto 0;
    }

    :host > * {
        margin: 0 3px 6px;
    }

    .min-width {
        min-width: 200px;
    }
`;
let OrRuleActionWebhook = class OrRuleActionWebhook extends LitElement {
    static get styles() {
        return style;
    }
    /* ---------------------- */
    render() {
        return html `
            <div style="display: flex; align-items: center; height: 100%;">
                <or-rule-webhook-modal .action="${this.action}">
                    <or-rule-form-webhook .webhook="${this.action.webhook}"></or-rule-form-webhook>
                </or-rule-webhook-modal>
            </div>
        `;
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleActionWebhook.prototype, "rule", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrRuleActionWebhook.prototype, "action", void 0);
OrRuleActionWebhook = __decorate([
    customElement("or-rule-action-webhook")
], OrRuleActionWebhook);
export { OrRuleActionWebhook };
//# sourceMappingURL=or-rule-action-webhook.js.map