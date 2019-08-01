import {html, LitElement, property} from "lit-element";
import {TenantRuleset} from "@openremote/model";
import {ruleListStyle} from "./style";

class OrRulesList extends LitElement {
    @property({type: Array})
    public rulesets: TenantRuleset[] = [];

    @property({type: Object})
    public ruleset?: TenantRuleset;

    static get styles() {
        return ruleListStyle;
    }

    public setActiveRule(ruleset: TenantRuleset) {
            const event = new CustomEvent("rules:set-active-rule", {
            detail: {ruleset: JSON.parse(JSON.stringify(ruleset))},
            bubbles: true,
            composed: true
        });
        this.dispatchEvent(event);
    }

    protected render() {

        return html`
            <div class="list-container">
                <strong class="list-title">Profielen</strong>
                ${this.rulesets && this.rulesets.map((ruleset: TenantRuleset, index: number) => {
                    return html`
                        <a ?selected="${this.ruleset && ruleset.id === this.ruleset.id}" class="d-flex list-item" @click="${() => this.setActiveRule(this.rulesets[index])}">
                            <span class="${ruleset.enabled ? "bg-green" : "bg-red"}"></span>
                            <div class="flex">
                                <span>${ruleset.name}<span>${!ruleset.id ? " [concept]" : ""}</span></span>
                            </div>
                        </a>
                    `;
                })}
            </div>
        `;
    }

}

window.customElements.define("or-rule-list", OrRulesList);
