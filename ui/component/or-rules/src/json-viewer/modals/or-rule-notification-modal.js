var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import { translate } from "@openremote/or-translate";
import { OrMwcDialogOpenedEvent } from "@openremote/or-mwc-components/or-mwc-dialog";
const checkValidity = (form, dialog) => {
    if (form) {
        const inputs = form.querySelectorAll('or-mwc-input');
        const elements = Array.prototype.slice.call(inputs);
        const valid = elements.every((element) => {
            if (element.shadowRoot) {
                const input = element.shadowRoot.querySelector('input, textarea, select');
                if (input && input.checkValidity()) {
                    return true;
                }
                else {
                    element._mdcComponent.valid = false;
                    element._mdcComponent.helperTextContent = 'required';
                    return false;
                }
            }
            else {
                return false;
            }
        });
        if (valid) {
            dialog.close();
        }
    }
};
let OrRuleNotificationModal = class OrRuleNotificationModal extends translate(i18next)(LitElement) {
    constructor() {
        super();
        this.title = "message";
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initDialog);
    }
    initDialog() {
        const modal = this.shadowRoot.getElementById('notification-modal');
        if (!modal)
            return;
    }
    renderDialogHTML(action) {
        const dialog = this.shadowRoot.getElementById("notification-modal");
        if (!this.shadowRoot)
            return;
        const slot = this.shadowRoot.querySelector('.notification-form-slot');
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
    firstUpdated(changedProperties) {
        if (changedProperties.has("action")) {
            this.renderDialogHTML(this.action);
        }
    }
    checkForm() {
        const dialog = this.shadowRoot.host;
        if (this.shadowRoot) {
            const messageNotification = this.shadowRoot.querySelector('or-rule-form-email-message');
            const pushNotification = this.shadowRoot.querySelector('or-rule-form-push-notification');
            if (pushNotification && pushNotification.shadowRoot) {
                const form = pushNotification.shadowRoot.querySelector('form');
                return checkValidity(form, dialog);
            }
            else if (messageNotification && messageNotification.shadowRoot) {
                const form = messageNotification.shadowRoot.querySelector('form');
                return checkValidity(form, dialog);
            }
        }
    }
    render() {
        if (!this.action)
            return html ``;
        const notificationPickerModalActions = [
            {
                actionName: "cancel",
                content: html `<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                action: (dialog) => {
                }
            },
            {
                actionName: "",
                content: html `<or-mwc-input class="button" .type="${InputType.BUTTON}" label="ok" @or-mwc-input-changed="${this.checkForm}"></or-mwc-input>`
            }
        ];
        const notificationPickerModalOpen = () => {
            const dialog = this.shadowRoot.getElementById("notification-modal");
            if (dialog) {
                dialog.open();
            }
        };
        return html `
            <or-mwc-input .type="${InputType.BUTTON}" label="message" @or-mwc-input-changed="${notificationPickerModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="notification-modal" heading="${this.title}" .actions="${notificationPickerModalActions}"></or-mwc-dialog>
            <slot class="notification-form-slot"></slot>
        `;
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleNotificationModal.prototype, "action", void 0);
__decorate([
    property({ type: String })
], OrRuleNotificationModal.prototype, "title", void 0);
__decorate([
    property({ type: Object })
], OrRuleNotificationModal.prototype, "query", void 0);
OrRuleNotificationModal = __decorate([
    customElement("or-rule-notification-modal")
], OrRuleNotificationModal);
export { OrRuleNotificationModal };
//# sourceMappingURL=or-rule-notification-modal.js.map