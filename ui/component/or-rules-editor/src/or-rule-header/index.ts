import {html, LitElement, property, customElement} from 'lit-element';
import '../selects/or-select-operator';
import '../selects/or-select-asset-attribute';

import '@openremote/or-input';
import '@openremote/or-select';
import {Rule} from '@openremote/model';

import {style} from './style';

@customElement('or-rule-header')
class OrRuleHeader extends LitElement {
    @property({type: Object})
    rule?: Rule;

    @property({type: Boolean})
    editmode: boolean = false;

    static get styles() {
        return [
            style
        ]
    }

    protected render() {

        return html`
            <div class="rule-container">
                ${this.editmode ? html`
                    <div class="layout horizontal">
                        <or-input type="text" value="${this.rule ? this.rule.name : null}"></or-input>
                        <span @click="${this.toggleEditmode}">edit</span>
                        <button @click="${this.updateRule}">opslaan</button>
                    </div>
                ` : html`
                    <div class="layout horizontal">
                        <h1>${this.rule ? this.rule.name : ''}</h1>
                        <span @click="${this.toggleEditmode}">edit</span>
                        <button @click="${this.updateRule}">opslaan</button>
                    </div>
                `}
            </div>
        `;
    }

    constructor() {
        super();
        this.addEventListener('or-input:changed', this.changeName);
    }

    changeName (e:any) {
        const value = e.detail.value;
        if(this.rule) {
            this.rule.name = value;
            console.log(this.rule);
        }
    }

    updateRule() {
        let event = new CustomEvent('rules:update-rule', {
            detail: {rule: this.rule},
            bubbles: true,
            composed: true
        });
        this.dispatchEvent(event);
    }

    toggleEditmode() {
        this.editmode = !this.editmode;
    }

}

