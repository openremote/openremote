import {html, LitElement, property, customElement, PropertyValues} from 'lit-element';

import {style} from './style';
import {Rule, RuleTrigger, RuleActionUnion, NewAssetQuery, BaseAssetQueryMatch} from "@openremote/model";

import '../or-rule-then-condition';

let defaultThen:RuleActionUnion[] = [];

const defaultAssetType:NewAssetQuery = {
    "types": [{
        "predicateType": "string",
        "match": BaseAssetQueryMatch.EXACT,
        "value": "urn:openremote:asset:kmar:flight"
    }]
};

let defaultThenCondition:RuleActionUnion = {
    action: "write-attribute",
    attributeName: 'profileName',
    value: 'profiel naam',
    target: { "useAssetsFromWhen": true}
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
                        <a class="button-add" @click="${this.addThenCondition}">+</a>
                    </div>
                </div>
        `;
    }

    @property({type: Object})
    rule?: Rule;

    constructor() {
        super();
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if(this.rule && !this.rule.then) {
            this.rule.then = defaultThen;
        }
    }

    addThenCondition () {
        if(this.rule && !this.rule.then) {
            this.rule.then = defaultThen;

        }

        if(this.rule && this.rule.then) {
            this.rule.then.push(defaultThenCondition);
            this.requestUpdate();
        }

    }

}

