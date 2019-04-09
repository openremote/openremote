import {html, LitElement, property, customElement, PropertyValues} from 'lit-element';

import {style} from './style';
import {Rule, RuleTrigger, AttributePredicate, BaseAssetQueryMatch, NewAssetQuery, AttributeDescriptor,
    BaseAssetQueryOperator} from "@openremote/model";

import '../or-rule-when-condition';


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
        match: BaseAssetQueryMatch.EXACT,
        value: "airlineIata"
    },
    value: {
        predicateType: "string",
        match: BaseAssetQueryMatch.EXACT
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
                                    <or-rule-when-condition index="${index}" .predicate="${predicate}" .attributeDescriptors="${this.attributeDescriptors}"></or-rule-when-condition>
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

    @property({type: Array})
    public attributeDescriptors?: AttributeDescriptor[];


    constructor() {
        super();
        this.addEventListener('when-condition:delete', this.deleteWhenCondition);

    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if(this.rule && !this.rule.when) {
            this.rule.when = defaultWhenCondition;
            if(this.rule!.when!.asset!.attributes!.predicates!.length === 0) {
                this.addPredicate();
            }
            this.requestUpdate();
        }
    }

    addWhenCondition () {
        if(this.rule && !this.rule.when) {
            this.rule.when = defaultWhenCondition;
        }

        this.addPredicate();
        this.requestUpdate();

    }

    addPredicate() {
        if(this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates) {
            this.rule.when.asset.attributes.predicates.push(defaultPredicate);
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

