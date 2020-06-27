import {customElement, html, LitElement, css, property} from "lit-element";
import "@openremote/or-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {
    RuleActionNotification,
    EmailNotificationMessage
} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";

@customElement("or-rule-form-message")
export class OrRuleFormMessage extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;


    static get styles() {
        return css`
            or-input {
                margin-bottom: 20px;
                min-width: 420px;
                width: 100%;
            }
        `
    }

    protected render() {
        const message:EmailNotificationMessage | undefined = this.action.notification!.message;
        
        return html`
            <div style="display:grid">
                <or-input value="${message && message.subject ?  message.subject : ""}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "subject")}" .label="${i18next.t("subject")}" type="${InputType.TEXT}" required placeholder=" "></or-input>
                <or-input value="${message && message.html ? message.html : ""}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "html")}" .label="${i18next.t("message")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-input>
            </div>
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
