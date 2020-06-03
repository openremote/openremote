import {customElement, html, LitElement, css, property} from "lit-element";
import "@openremote/or-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {
    RuleActionNotification,
    PushNotificationMessage
} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import set from "lodash-es/set";

@customElement("or-rule-form-push-notification")
export class OrRuleFormPushNotification extends translate(i18next)(LitElement) {

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
        const message:PushNotificationMessage | undefined = this.action.notification!.message;
        const title = message && message.title ? message.title : "";
        const body = message && message.body ? message.body : "";
        const action = message && message.action ? message.action : "";
        const actionUrl = action ? action.url : "";
        const buttons = message && message.buttons ? message.buttons : [];
        
        return html`
            <div style="display:grid">
                <or-input value="${title}" 
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "title")}" 
                    .label="${i18next.t("subject")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-input>

                <or-input value="${body}" 
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "body")}" 
                    .label="${i18next.t("message")}" 
                    type="${InputType.TEXTAREA}" 
                    required 
                    placeholder=" " ></or-input>
                <or-input value="${actionUrl}" 
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "action.url")}" 
                    .label="${i18next.t("openWebsiteUrl")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-input>

                <or-input value="${buttons && buttons[0] && buttons[0].action &&  buttons[0].action.openInBrowser ? buttons[0].action.openInBrowser : ""}" 
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "buttons.0.action.openInBrowser")}" 
                    .label="${i18next.t("openInBrowser")}" 
                    type="${InputType.SWITCH}" 
                    required 
                    placeholder=" "></or-input>  
                
                <or-input value="${buttons && buttons[0] && buttons[0].title ? buttons[0].title : ""}" 
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "buttons.0.title")}" 
                    .label="${i18next.t("buttonTextConfirm")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-input>
                <or-input value="${buttons && buttons[1] && buttons[1].title ? buttons[1].title : ""}" 
                    @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "buttons.1.title")}" 
                    .label="${i18next.t("buttonTextDecline")}" 
                    type="${InputType.TEXT}" 
                    required 
                    placeholder=" "></or-input>
            </div>
        `
    }



    protected setActionNotificationName(value: string | undefined, key?: string) {
        if(key && this.action.notification && this.action.notification.message){
            let message:any = this.action.notification.message;
            message = set(message, key, value);
            this.action.notification.message = {...message};
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}