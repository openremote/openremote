import {html, LitElement, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {
    RuleActionAlarm,
    Alarm,
    User
} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import {i18next} from "@openremote/or-translate";

@customElement("or-rule-form-alarm")
export class OrRuleFormAlarm extends LitElement {

    @property({type: Object, attribute: false})
    public action!: RuleActionAlarm;

    @property()
    public users: User[] = [];

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
        const alarm: Alarm | undefined = this.action.alarm as Alarm;
        const options: {value: string | undefined, label: string | undefined}[] = this.users.filter((u) => u.username != 'manager-keycloak').map((u) => {
            return { value: u.id, label: u.username };
        });
        
        return html`
            <form style="display:grid">
                <or-mwc-input value="${alarm && alarm.title ?  alarm.title : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlarmName(e.detail.value, "title")}" .label="title" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${alarm && alarm.content ? alarm.content : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlarmName(e.detail.value, "content")}" .label=content" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
                <or-mwc-input .label="${i18next.t("alarm.assignee")}"
                              .type="${InputType.SELECT}"
                              .options="${options.map((obj) => obj.label)}"
                              .value="${this.action.assigneeId ? options.filter((obj) => obj.value === this.action.assigneeId).map((obj) => obj.label)[0] : ""}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    this.action.assigneeId = options.filter((obj) => obj.label === e.detail.value).map((obj) => obj.value)[0];
                                    this.setActionAlarmName(e.detail.value, undefined);
                                }}"
                ></or-mwc-input> 
            </form>
        `
    }

    protected setActionAlarmName(value: string | undefined, key?: string) {
        if(key && this.action.alarm){
            const alarm:any = this.action.alarm;
            alarm[key] = value;
            this.action.alarm = {...alarm};
        }
        if(!key) {
            this.action.assigneeId = this.users.filter((obj) => obj.username === value).map((obj) => obj.id)[0];
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
