var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, LitElement, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import { translate } from "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import set from "lodash-es/set";
let OrRuleFormPushNotification = class OrRuleFormPushNotification extends translate(i18next)(LitElement) {
    static get styles() {
        return css `
            or-mwc-input {
                margin-bottom: 20px;
                min-width: 420px;
                width: 100%;
            }
        `;
    }
    updated(_changedProperties) {
        if (_changedProperties.has("action")) {
            let message = this.action.notification.message;
            if (this.action.notification && message && !message.action) {
                message = { type: "push", action: { openInBrowser: true } };
                this.action.notification.message = Object.assign(Object.assign({}, this.action.notification.message), message);
            }
        }
    }
    render() {
        const message = this.action.notification.message;
        const title = message && message.title ? message.title : "";
        const body = message && message.body ? message.body : "";
        const action = message && message.action ? message.action : "";
        const actionUrl = action && action.url ? action.url : "";
        const buttons = message && message.buttons ? message.buttons : [];
        return html `
            <form style="display:grid">
                <or-mwc-input value="${title}" 
                    @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "title")}" 
                    .label="${i18next.t("subject")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-mwc-input>

                <or-mwc-input value="${body}" 
                    @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "body")}" 
                    .label="${i18next.t("message")}" 
                    type="${InputType.TEXTAREA}" 
                    required 
                    placeholder=" " ></or-mwc-input>
                <or-mwc-input value="${actionUrl}" 
                    @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "action.url")}" 
                    .label="${i18next.t("openWebsiteUrl")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-mwc-input>

                <or-mwc-input .value="${action && typeof action.openInBrowser !== "undefined" ? (action && action.openInBrowser) : true}" 
                    @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "action.openInBrowser")}" 
                    .label="${i18next.t("openInBrowser")}" 
                    type="${InputType.SWITCH}" 
                    placeholder=" "></or-mwc-input>  
                
                <or-mwc-input value="${buttons && buttons[0] && buttons[0].title ? buttons[0].title : ""}" 
                    @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "buttons.0.title")}" 
                    .label="${i18next.t("buttonTextConfirm")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-mwc-input>
                <or-mwc-input value="${buttons && buttons[1] && buttons[1].title ? buttons[1].title : ""}" 
                    @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "buttons.1.title")}" 
                    .label="${i18next.t("buttonTextDecline")}" 
                    type="${InputType.TEXT}" 
                    placeholder=" "></or-mwc-input>
            </form>
        `;
    }
    setActionNotificationName(value, key) {
        if (key && this.action.notification && this.action.notification.message) {
            let message = this.action.notification.message;
            set(message, key, value);
            if (key.includes('action')) {
                set(message, "buttons.0." + key, value);
            }
            this.action.notification.message = Object.assign({}, message);
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleFormPushNotification.prototype, "action", void 0);
OrRuleFormPushNotification = __decorate([
    customElement("or-rule-form-push-notification")
], OrRuleFormPushNotification);
export { OrRuleFormPushNotification };
//# sourceMappingURL=or-rule-form-push-notification.js.map