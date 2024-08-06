var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
let OrRuleWebhookModal = class OrRuleWebhookModal extends LitElement {
    constructor() {
        super();
        this.title = i18next.t('message');
    }
    /* ----------------------- */
    firstUpdated(changedProperties) {
        if (changedProperties.has("action")) {
            this.renderDialogHTML(this.action);
        }
    }
    renderDialogHTML(action) {
        const dialog = this.shadowRoot.getElementById("webhook-modal");
        if (!this.shadowRoot)
            return;
        const slot = this.shadowRoot.querySelector('.webhook-form-slot');
        if (dialog && slot) {
            let container = document.createElement("div");
            slot.assignedNodes({ flatten: true }).forEach((child) => {
                if (child instanceof HTMLElement) {
                    container.appendChild(child);
                }
            });
            dialog.content = html `${container}`;
            dialog.dismissAction = null;
            this.requestUpdate();
        }
    }
    closeForm(event) {
        const dialog = this.shadowRoot.host;
        dialog.close();
    }
    render() {
        if (!this.action) {
            return html `${i18next.t('errorOccurred')}`;
        }
        const webhookModalActions = [
            {
                actionName: "", content: html `
                    <or-mwc-input .type="${InputType.BUTTON}" label="ok"
                                  @or-mwc-input-changed="${this.closeForm}"></or-mwc-input>`
            }
        ];
        const webhookModalOpen = () => {
            const dialog = this.shadowRoot.getElementById("webhook-modal");
            if (dialog) {
                dialog.open();
            }
        };
        return html `
            <or-mwc-input type="${InputType.BUTTON}" label="message"
                          @or-mwc-input-changed="${webhookModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="webhook-modal" heading="${this.title}" .actions="${webhookModalActions}"></or-mwc-dialog>
            <slot class="webhook-form-slot"></slot>
        `;
    }
};
__decorate([
    property({ type: Object })
], OrRuleWebhookModal.prototype, "action", void 0);
__decorate([
    property({ type: String })
], OrRuleWebhookModal.prototype, "title", void 0);
OrRuleWebhookModal = __decorate([
    customElement("or-rule-webhook-modal")
], OrRuleWebhookModal);
export { OrRuleWebhookModal };
//# sourceMappingURL=or-rule-webhook-modal.js.map