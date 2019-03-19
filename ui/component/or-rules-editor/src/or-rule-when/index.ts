import {html, LitElement, property, customElement} from 'lit-element';

import {style} from './style';
import {Rule, RuleTrigger, AttributePredicate, BaseAssetQueryMatch} from "@openremote/model";

import '../or-rule-when-condition';

const defaultWhenCondition:RuleTrigger = {
    "asset": {
        "types": [],
        "attributes": {
            "predicates": []
        }
    }
};

const defaultPredicate:AttributePredicate = {
    name: {
        predicateType: "string",
        match: BaseAssetQueryMatch.EXACT
    },
    value: {
        predicateType: "string",
        match: BaseAssetQueryMatch.EXACT,
        value: ""
    }

};

@customElement('or-rule-when')
class OrRuleWhen extends LitElement {

    static get styles() {
        return [
            style
        ]
    }

    protected render() {

        return html`
           <div class="rule-content-section">
                <h3>Als..</h3>
                <div class="rule-when-container bg-white shadow">
                    ${this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates ? html`
                        ${this.rule.when.asset.attributes.predicates.map((predicate:AttributePredicate) => {
                            return html`
                                    <or-rule-when-condition .predicate="${predicate}"></or-rule-when-condition>
                                    <span class="rule-additional">&</span>
                            `
                        })}
                    `: ``}
                    <a class="button-add" @click="${this.addWhenRule}">+</a>
                </div>
            </div>
        `;
    }

    @property({type: Object})
    rule?: Rule;

    @property({type: Array})
    predicates?: AttributePredicate[] = [];

    constructor() {
        super();
    }

    addWhenRule () {
        if(this.rule && !this.rule.when) {
            this.rule.when = defaultWhenCondition;
        }

        if(this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates) {
            this.rule.when.asset.attributes.predicates.push(defaultPredicate);
            this.requestUpdate();
        }

    }

}

