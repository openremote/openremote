import {html, LitElement, property} from "lit-element";
import {TenantRuleset, Rule, RuleCondition} from "@openremote/model";
import {ruleListStyle} from "./style";
import moment from "moment";

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
                            <span class="${this.checkRuleStatus(ruleset)}"></span>
                            <div class="flex">
                                <span>${ruleset.name}<span>${!ruleset.id ? " [concept]" : ""}</span></span>
                            </div>
                        </a>
                    `;
                })}
            </div>
        `;
    }


    checkRuleStatus(ruleset: TenantRuleset){
        let status = ruleset.enabled ? "bg-green" : "bg-red";

        if(ruleset.rules && ruleset.enabled) {
            const rule: Rule = JSON.parse(ruleset.rules).rules[0];

            // HACK/WIP: the status of a rule should be better thought over see issue #95
            // currently only checks the date of the first whenCondition
            if(rule && rule.when && rule.when.items && rule.when.items.length > 0) {
                const ruleCondition: RuleCondition = rule.when.items[0];
                if(ruleCondition.datetime) {
                    const today = moment();
                    const startDate = ruleCondition.datetime.value;
                    const endDate = ruleCondition.datetime.rangeValue;

                    if (today.diff(startDate) < 0) {
                        // before startDate, show blue
                        status = "bg-blue";
                    } else if (today.diff(endDate) > 0) {
                        // after endDate, show grey
                        status = "bg-grey";
                    }
                }
            }
        }

        return status;

    }

}

window.customElements.define("or-rule-list", OrRulesList);
