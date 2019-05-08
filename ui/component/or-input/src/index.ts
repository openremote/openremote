import {html, LitElement, property, PropertyValues} from 'lit-element';
import openremote from "@openremote/core";

import {orInputStyle} from './style';
class OrInput extends LitElement {

    @property({type: String})
    public type: string = "text";

    @property({type: String})
    public name: string = "";

    @property({type: Boolean})
    public required: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: String})
    public value: string = "";


    static get styles() {
        return [
            orInputStyle
        ];
    }

    protected render() {

        return html`
             <input class="or-input" ?required="${this.required}" type="${this.type}" name="${this.name}" @change="${this.onChange}" .value="${this.value}"  ?disabled="${this.disabled} />
        `;
    }

    onChange() {
        if(this.shadowRoot){
            const input = (<HTMLInputElement> this.shadowRoot.querySelector('.or-input'));
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

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        this.disabled = !openremote.hasRole("write:assets");
    }
}

window.customElements.define('or-input', OrInput);
