import {html, LitElement, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate"
import {translate} from "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {EmailNotificationMessage} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";

@customElement("or-rule-form-email-message")
export class OrRuleFormEmailMessage extends translate(i18next)(LitElement) {

    @property({type: Object})
    public message?: EmailNotificationMessage;

    static get styles() {
        return css`
            or-mwc-input {
                margin-bottom: 20px;
                min-width: 420px;
                width: 100%;
            }
        `;
    }

    protected render() {
        if(!this.message) {
            return html`<or-translate .value="${"errorOccurred"}"></or-translate>`;
        }
        
        return html`
            <form style="display:grid">
                <or-mwc-input value="${this.message.subject || ''}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "subject")}" .label="${i18next.t("subject")}" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${this.message.html || ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionNotificationName(e.detail.value, "html")}" .label="${i18next.t("message")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
            </form>
        `
    }

    protected setActionNotificationName(value: string | undefined, key?: string) {
        if(key && this.message){
            (this.message as any)[key] = value;
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
