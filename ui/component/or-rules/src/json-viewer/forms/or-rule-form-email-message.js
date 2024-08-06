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
let OrRuleFormEmailMessage = class OrRuleFormEmailMessage extends translate(i18next)(LitElement) {
    static get styles() {
        return css `
            or-mwc-input {
                margin-bottom: 20px;
                min-width: 420px;
                width: 100%;
            }
        `;
    }
    render() {
        const message = this.action.notification.message;
        return html `
            <form style="display:grid">
                <or-mwc-input value="${message && message.subject ? message.subject : ""}" @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "subject")}" .label="${i18next.t("subject")}" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${message && message.html ? message.html : ""}" @or-mwc-input-changed="${(e) => this.setActionNotificationName(e.detail.value, "html")}" .label="${i18next.t("message")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
            </form>
        `;
    }
    setActionNotificationName(value, key) {
        if (key && this.action.notification && this.action.notification.message) {
            const message = this.action.notification.message;
            message[key] = value;
            this.action.notification.message = Object.assign({}, message);
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
};
__decorate([
    property({ type: Object, attribute: false })
], OrRuleFormEmailMessage.prototype, "action", void 0);
OrRuleFormEmailMessage = __decorate([
    customElement("or-rule-form-email-message")
], OrRuleFormEmailMessage);
export { OrRuleFormEmailMessage };
//# sourceMappingURL=or-rule-form-email-message.js.map