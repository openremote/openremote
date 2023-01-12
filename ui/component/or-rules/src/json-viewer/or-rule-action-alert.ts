import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {until} from "lit/directives/until.js";
import {ActionType, RulesConfig} from "../index";
import {
    JsonRule,
    RuleActionAlert,
} from "@openremote/model";
import "./modals/or-rule-alert-modal";
import "./forms/or-rule-form-alert";
import "./or-rule-action-attribute";

// language=CSS
const style = css`
    :host {
        display: flex;
        align-items: center;
    }

    :host > * {
        margin: 0 3px 6px;
    }

    .min-width {
        min-width: 200px;
    }
`;

@customElement("or-rule-action-alert")
export class OrRuleActionAlert extends LitElement {

    static get styles() {
        return style;
    }

    @property({type: Object, attribute: false})
    public rule!: JsonRule;

    @property({type: Object, attribute: false})
    public action!: RuleActionAlert;

    @property({type: String, attribute: false})
    public actionType!: ActionType;

    public readonly?: boolean;

    @property({type: Object})
    public config?: RulesConfig;


    protected render() {

        if (!this.action.alert || !this.action.alert.title) {
            return html``;
        }

        const alert = this.action.alert;

        let modalTemplate: TemplateResult | string = ``;

        if (alert) {
            modalTemplate = html`
                <or-rule-alert-modal title="alert" .action="${this.action}">
                    <or-rule-form-alert .action="${this.action}"></or-rule-form-alert>
                </or-rule-alert-modal>
            `;
        }

        return html`${until(modalTemplate,html``)}`;
    }
}
