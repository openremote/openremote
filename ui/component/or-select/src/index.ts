import {html, LitElement, property, PropertyValues} from 'lit-element';
import {MDCSelect} from '@material/select';

import {selectStyle} from './style';

class OrSelect extends LitElement {
    @property({type: String})
    label: string = '';

    @property({type: String})
    name: string = '';

    @property({type: String})
    value: string = '';

    @property({type: Array})
    options: string[] = [];

    static get styles() {
        return [
            selectStyle
        ];
    }

    protected render() {

        return html`
               <div class="mdc-select">
                      <select id="or-select" @change="${this.onChange}" name="${this.name}" >
                        ${this.options.map((option: string) => {
                           return html`<option ?selected=${this.value === option} value="${option}">${option}</option>`
                        })}
                      </select>
                </div>
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
    }

    onChange() {
        if(this.shadowRoot){
            const input = (<HTMLInputElement>this.shadowRoot.getElementById('or-select'));
            const value = input.value;
            const name = input.name;

            // Launch event for all parent elements
            let event = new CustomEvent('or-input:changed', {
                detail: { value: value, name: name  },
                bubbles: true,
                composed: true });

            this.dispatchEvent(event);
        }
    }


    constructor() {
        super();
    }


}

window.customElements.define('or-select', OrSelect);
