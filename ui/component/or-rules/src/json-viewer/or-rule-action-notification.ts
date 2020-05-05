import {customElement, html, LitElement, property, css, TemplateResult} from "lit-element";
import {RulesConfig} from "../index";
import {
    RuleActionNotification,
    EmailNotificationMessageRecipient,
    EmailNotificationMessage,
    PushNotificationMessageTargetType,
    PushNotificationMessage,
    AbstractNotificationMessageUnion
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";
import "./modals/or-rule-notification-modal";
import "./forms/or-rule-form-message";
import "./forms/or-rule-form-push-notification";
import { i18next } from "@openremote/or-translate";

// language=CSS
const style = css`
    :host {
        display: flex;
        align-items: center;
    }

    :host > * {
        margin-right: 10px;
    }
`;
const NOTIFICATION_OPTIONS = [["email", "Email"], ["push-notification", "Push notification"]];
const PUSH_NOTIFICATION_OPTIONS = [[PushNotificationMessageTargetType.DEVICE, "Device"], [PushNotificationMessageTargetType.TOPIC, "Topic"], [PushNotificationMessageTargetType.CONDITION, "Condition"]];
@customElement("or-rule-action-notification")
export class OrRuleActionNotification extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;

    public readonly?: boolean;

    @property({type: Object})
    public config?: RulesConfig;


    protected render() {
        let value: string = "";
        const message = this.action.notification && this.action.notification.message ? this.action.notification.message : undefined;
        const type = message && message.type ? message.type : undefined;

        const typeSelector = html`
                <or-input   .type="${InputType.SELECT}" 
                            .options="${NOTIFICATION_OPTIONS}"
                            label="${i18next.t("type")}"
                            value="${type}"
                            @or-input-changed="${(e: OrInputChangedEvent) => this.setNotificationType(e.detail.value)}" 
                            ?readonly="${this.readonly}"></or-input>
        `;
        let typeTemplate;

        if(message) {
            if(type === "push-notification") {
                typeTemplate = html`
                    <or-input type="${InputType.SELECT}" 
                        .options="${PUSH_NOTIFICATION_OPTIONS}"
                        @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value)}" 
                        ?readonly="${this.readonly}"></or-input>
                
                    <or-rule-notification-modal .action="${this.action}">
                        <or-rule-form-push-notification .action="${this.action}"></or-rule-form-push-notification>
                    </or-rule-notification-modal>
                `;
            }
            
            if(type === "email") {
                const emailMessage:EmailNotificationMessage = message;
                value = message && emailMessage.to ? emailMessage.to.map(t => t.address).join(';') : "";
                typeTemplate = html`
                    <or-input .type="${InputType.TEXT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value)}" ?readonly="${this.readonly}" .value="${value}" ></or-input>
                    
                    <or-rule-notification-modal .action="${this.action}">
                        <or-rule-form-message .action="${this.action}"></or-rule-form-message>
                    </or-rule-notification-modal>
                `;
            }
        }

        return html`
            ${typeSelector}
            ${typeTemplate}
        `;
    }

    protected setNotificationType(type: string | undefined) {
        switch (type) {
            case "email":
                this.action.notification = {
                    message: {
                        type: "email",
                        subject: "%RULESET_NAME%",
                        html: "%TRIGGER_ASSETS%",
                        from: {address:"no-reply@openremote.io"},
                        to: []
                    }
                };
                break;
            case "push-notification":
                this.action.notification = {
                    message: {
                        type: "push-notification"
                    }
                };
                break;
        }
        this.requestUpdate();
    } 

    protected setActionNotificationName(emails: string | undefined) {
      

        if(emails && this.action.notification && this.action.notification.message){

            const arrayOfEmails = emails.split(';');
            const message:EmailNotificationMessage = this.action.notification.message;

            arrayOfEmails.forEach(email => {
                const messageRecipient:EmailNotificationMessageRecipient = {
                        address: email,
                        name: email
                };
                if(message && message.to){
                    message.to.push(messageRecipient);
                }
            });

            this.action.notification.message = message;
        }
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

}