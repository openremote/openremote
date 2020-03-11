import {customElement, html, LitElement, property, css} from "lit-element";
import {RulesConfig} from "../index";
import {
    RuleActionNotification,
    EmailNotificationMessageRecipient,
    EmailNotificationMessage
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {OrRulesJsonRuleChangedEvent} from "./or-rule-json-viewer";

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

@customElement("or-rule-action-notification")
export class OrRuleActionNotification extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;

    public readonly?: boolean;

    public config?: RulesConfig;


    protected render() {
        let value: string = "";
        if(this.action.notification && this.action.notification.message ){
            const message:EmailNotificationMessage = this.action.notification.message;
            if(message.to) {
                value = message.to.map(t => t.address).join(';');
            }
        }

        return html`
            <or-input .type="${InputType.TEXT}" @or-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value)}" ?readonly="${this.readonly}" .value="${value}" ></or-input>
        `;


    }

    protected setActionNotificationName(emails: string | undefined) {
        this.action.notification = {
            message: {
                type: "email",
                subject: "%RULESET_NAME%",
                html: "%TRIGGER_ASSETS%",
                from: {address:"no-reply@openremote.io"},
                to: []
            }
        };

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