import {html, LitElement, css} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {
    RuleActionAlert,
    Alert
} from "@openremote/model";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";

@customElement("or-rule-form-alert")
export class OrRuleFormAlert extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionAlert;


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
        const alert: Alert | undefined = this.action.alert as Alert;
        
        return html`
            <form style="display:grid">
                <or-mwc-input value="${alert && alert.title ?  alert.title : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlertName(e.detail.value, "title")}" .label="${i18next.t("title")}" type="${InputType.TEXT}" required placeholder=" "></or-mwc-input>
                <or-mwc-input value="${alert && alert.content ? alert.content : ""}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlertName(e.detail.value, "content")}" .label="${i18next.t("content")}" type="${InputType.TEXTAREA}" required placeholder=" " ></or-mwc-input>
            </form>
        `
    }

    protected setActionAlertName(value: string | undefined, key?: string) {
        if(key && this.action.alert){
            const alert:any = this.action.alert;
            alert[key] = value;
            this.action.alert = {...alert};
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
