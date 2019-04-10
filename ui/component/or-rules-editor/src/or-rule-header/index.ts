import {html, LitElement, property, customElement} from 'lit-element';
import '../selects/or-select-operator';
import '../selects/or-select-asset-attribute';

import '@openremote/or-input';
import '@openremote/or-select';
import '@openremote/or-icon';
import {TenantRuleset} from '@openremote/model';

import {style} from './style';

@customElement('or-rule-header')
class OrRuleHeader extends LitElement {
    @property({type: Object})
    ruleset?: TenantRuleset;

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
              
                    <div class="layout horizontal">
                        ${this.editmode ? html`
                            <or-input type="text" value="${this.ruleset ? this.ruleset.name : null}"></or-input>
                        ` : html`
                            <h1>${this.ruleset ? this.ruleset.name : ''}</h1>
                        `}
                        
                        <span @click="${this.toggleEditmode}"><or-icon style="margin:10px;" icon="pencil-outline"></or-icon></span>
                        <span class="rule-status ${this.ruleset && this.ruleset.enabled ? 'bg-green' : 'bg-red'}"></span>
                        <button class="button-simple" @click="${this.toggleEnabled}">${this.ruleset!.enabled ? 'deactiveren' : 'publiceren'}</button>
                        
                        ${this.ruleset && this.ruleset.id ? html`
                            <button @click="${this.updateRule}">opslaan</button>
                        ` : html`
                            <button @click="${this.createRule}">toevoegen</button>
                        `}
                        
                    </div>
            </div>
        `;
    }

    constructor() {
        super();
        this.addEventListener('or-input:changed', this.changeName);
    }

    changeName (e:any) {
        const value = e.detail.value;
        if(this.ruleset) {
            this.ruleset.name = value;
        }
    }

    createRule() {
        let event = new CustomEvent('rules:write-rule', {
            detail: {ruleset: this.ruleset},
            bubbles: true,
            composed: true
        });
        this.dispatchEvent(event);
    }

    updateRule() {
        let event = new CustomEvent('rules:update-rule', {
            detail: {ruleset: this.ruleset},
            bubbles: true,
            composed: true
        });
        this.dispatchEvent(event);
    }

    toggleEnabled () {
        if(this.ruleset){
            this.ruleset.enabled = !this.ruleset.enabled;

            this.updateRule();
            this.requestUpdate();
        }
    }

    toggleEditmode() {
        this.editmode = !this.editmode;
    }

}

