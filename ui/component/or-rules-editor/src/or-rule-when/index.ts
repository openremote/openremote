import {html, LitElement, property, customElement, PropertyValues} from 'lit-element';

import {style} from './style';
import {Rule, RuleTrigger, AttributePredicate, BaseAssetQueryMatch, NewAssetQuery} from "@openremote/model";

import '../or-rule-when-condition';


const ruleModel:Rule = {
    name: "",
    when: undefined,
    then: undefined
};

const defaultWhenCondition:RuleTrigger = {
    "asset": {
        "types": [{
            "predicateType": "string",
            "match": BaseAssetQueryMatch.EXACT,
            "value": "urn:openremote:asset:kmar:flight"
            }
        ],
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
        predicateType: "string"
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
                        ${this.rule.when.asset.attributes.predicates.map((predicate:AttributePredicate, index) => {
                            return html`
                                    <or-rule-when-condition index="${index}" .predicate="${predicate}"></or-rule-when-condition>
                                    <span class="rule-additional">&</span>
                            `
                        })}
                    `: ``}
                    <a class="button-add" @click="${this.addWhenCondition}">+</a>
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
        this.addEventListener('when-condition:delete', this.deleteWhenCondition);

    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if (!this.rule) {
            this.rule = ruleModel;
        }
    }

    addWhenCondition () {
        if(this.rule && !this.rule.when) {
            this.rule.when = defaultWhenCondition;
        }

        if(this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates) {
            this.rule.when.asset.attributes.predicates.push(defaultPredicate);
            this.requestUpdate();
        }

    }

    deleteWhenCondition (e:any) {

        const index = e.detail.index;

        if(this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates) {
            this.rule.when.asset.attributes.predicates.splice(index, 1);
            this.requestUpdate();
        }
    }
}

