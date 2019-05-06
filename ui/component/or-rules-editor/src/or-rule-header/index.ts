import {html, LitElement, property, customElement, PropertyValues} from 'lit-element';
import '../selects/or-select-operator';
import '../selects/or-select-asset-attribute';

import '@openremote/or-input';
import '@openremote/or-select';
import '@openremote/or-icon';
import {TenantRuleset, Rule} from '@openremote/model';
import openremote from '@openremote/core';

import {style} from './style';

@customElement('or-rule-header')
class OrRuleHeader extends LitElement {
    @property({type: Object})
    public ruleset?: TenantRuleset;

    @property({type: Object})
    public rule?: Rule;

    @property({type: Boolean})
    public valid: boolean = false;

    static get styles() {
        return [
            style
        ];
    }

    protected render() {

        return html`
            <div class="rule-container">
              
                    <div class="layout horizontal">
                        <input ?disabled="${!openremote.hasRole("write:assets")}"  @change="${this.changeName}" type="text" .value="${this.ruleset ? this.ruleset.name : null}" />
                        
                        <div class="layout horizontal" style="margin-left: auto;">
                            <span style="margin: 9px 0;" class="toggle-label" ?data-disabled="${!this.ruleset!.id}">Actief</span>
                            <label class="switch" ?data-disabled="${!this.ruleset!.id}">
                              <input @change="${this.toggleEnabled}" ?disabled="${!this.ruleset!.id || !openremote.hasRole("write:assets")}" ?checked="${this.ruleset!.enabled}" type="checkbox">
                              <span class="slider round"></span>
                            </label>                    

                            ${openremote.hasRole("write:assets") ? html`
                                ${this.ruleset && this.ruleset.id ? html`
                                    <button ?disabled="${!this.valid}" @click="${this.updateRule}">opslaan</button>
                                ` : html`
                                    <button ?disabled="${!this.valid}" @click="${this.createRule}">opslaan</button>
                                `}
                            ` : ``}
                        </div>
                        
                    </div>
            </div>
        `;
    }

    constructor() {
        super();
    }

    changeName(e:any) {
        const value = e.target.value;
        if (this.ruleset) {
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

}

