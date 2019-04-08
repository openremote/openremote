import {html, LitElement, property, customElement, TemplateResult} from "lit-element";
import "../selects/or-select-operator";
import "../selects/or-select-asset-attribute";

import "@openremote/or-input";
import "@openremote/or-select";
import "@openremote/or-icon";
import handler from "../index";
import {style} from "./style";

import {RuleActionUnion, RuleActionWriteAttribute} from "@openremote/model";

// TODO use create select option to
@customElement("or-rule-then-condition")
class OrRuleThenCondition extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Object})
    public condition?: RuleActionWriteAttribute;

    @property({type: Number})
    private value: number = 0;

    constructor() {
        super();

        this.addEventListener("or-input:changed", this.setValue);
    }

    public setValue(e: any) {
        const value = e.detail.value;

        if (this.condition && this.condition.value) {
            this.condition.value = value;
            this.requestUpdate();
        }
    }

    protected render() {

        return html`
            <div class="rule-container">
                ${this.condition ? html`
                   <or-select-asset-attribute disabled type="${this.condition}" value="${this.condition.attributeName}"></or-select-asset-attribute>
                   <or-select-operator disabled type="${this.condition}" value="EQUAL"></or-select-operator>
                   ${this.createInputControl(this.condition!)}
                ` : ``}
            </div>
        `;
    }

    protected createInputControl(condition: RuleActionWriteAttribute): TemplateResult {
        for (const h of handler.handlers) {

            const result = h(this.condition!);
            if (result) {
               return html`${result}`;
            }
        }

        return html`<or-input type="text" value="${this.condition!.value}"></or-input>`;
    }

}

