import {html, LitElement, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {
    RuleActionNotification,
    EmailNotificationMessage
} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";

@customElement("or-rule-form-email-message")
export class OrRuleFormEmailMessage extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;


    static get styles() {
        return css`
            or-mwc-input {
                margin-bottom: 20px;
                min-width: 420px;
                width: 100%;
            }
        `
    }

    protected render() {
        const message: EmailNotificationMessage | undefined = this.action.notification!.message as EmailNotificationMessage;
        
        return html`
            <form style="display:grid">
                <or-mwc-input value="${message && message.subject ?  message.subject : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "subject")}" .label="${i18next.t("subject")}" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${message && message.html ? message.html : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "html")}" .label="${i18next.t("message")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
            </form>
        `
    }

    protected setActionNotificationName(value: string | undefined, key?: string) {
        if(key && this.action.notification && this.action.notification.message){
            const message:any = this.action.notification.message;
            message[key] = value;
            this.action.notification.message = {...message};
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
