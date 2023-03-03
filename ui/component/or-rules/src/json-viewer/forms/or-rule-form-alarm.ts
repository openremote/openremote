import {html, LitElement, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {
    RuleActionAlarm,
    Alarm
} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";

@customElement("or-rule-form-alarm")
export class OrRuleFormAlarm extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionAlarm;

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

        return html`
            <form style="display:grid">
                <or-mwc-input value="${alarm && alarm.title ?  alarm.title : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlarmName(e.detail.value, "title")}" .label="${i18next.t("title")}" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${alarm && alarm.content ? alarm.content : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlarmName(e.detail.value, "content")}" .label="${i18next.t("content")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
            </form>
        `
    }

    protected setActionAlarmName(value: string | undefined, key?: string) {
        if(key && this.action.alarm){
            const alarm:any = this.action.alarm;
            alarm[key] = value;
            this.action.alarm = {...alarm};
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
