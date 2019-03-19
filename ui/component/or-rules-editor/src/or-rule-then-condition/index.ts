import {html, LitElement, property, customElement} from 'lit-element';
import '../selects/or-select-operator';
import '../selects/or-select-asset-attribute';

import '@openremote/or-input';
import '@openremote/or-select';

import {style} from './style';

import {RuleActionUnion} from "@openremote/model";

@customElement('or-rule-then-condition')
class OrRuleThenCondition extends LitElement {

    static get styles() {
        return [
            style
        ]
    }

    protected render() {

        return html`
            <div class="rule-container">
                <or-select-asset-type></or-select-asset-type>
                <or-select-asset-attribute type="${this.condition}"></or-select-asset-attribute>
                <or-select-operator type="${this.condition}"></or-select-operator>
                <or-input type="text"></or-input>
            </div>
        `;
    }

    @property({type: Object})
    condition?: RuleActionUnion;

    @property({type: Number})
    private value: number = 0;


    constructor() {
        super();
    }


}

