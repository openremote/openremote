import {html, LitElement, property, customElement} from 'lit-element';

import {style} from './style';
import {Rule, RuleTrigger, RuleActionUnion} from "@openremote/model";

import '../or-rule-then-condition';

let defaultThen:RuleActionUnion[] = [];
let defaultThenCondition:RuleActionUnion = {
    action: "write-attribute"
};

@customElement('or-rule-then')
class OrRuleThen extends LitElement {

    static get styles() {
        return [
            style
        ]
    }

    protected render() {

        return html`
               <div class="rule-content-section">
                    <h3>Dan..</h3>
                    <div class="rule-then-container bg-white shadow">
                       ${this.rule && this.rule.then ? this.rule.then.map((then:RuleActionUnion) => {
                                return html`
                                <or-rule-then-condition .condition="${then}"></or-rule-then-condition>
                                <span class="rule-additional">&</span>
                                `
                            }) : ``}
                        <a class="button-add" @click="${this.addThenRule}">+</a>
                    </div>
                </div>
            
        `;
    }

    @property({type: Object})
    rule?: Rule;

    constructor() {
        super();
    }

    addThenRule () {
        if(this.rule && !this.rule.then) {
            this.rule.then = defaultThen;
            console.log(this.rule);

        }

        if(this.rule && this.rule.then) {
            this.rule.then.push(defaultThenCondition);
            this.requestUpdate();
        }

    }

}

