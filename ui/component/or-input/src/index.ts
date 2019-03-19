import {html, LitElement, property} from 'lit-element';

class OrInput extends LitElement {

    @property({type: String})
    type: string = 'text';

    @property({type: String})
    name: string = '';

    @property({type: String})
    value: string = '';

    protected render() {

        return html`
             <input id="or-input" type="${this.type}" name="${this.name}" @change="${this.onChange}" value="${this.value}" />
        `;
    }


    onChange() {
        if(this.shadowRoot){
            const value = (<HTMLInputElement>this.shadowRoot.getElementById('or-input')).value;

            // Launch event for all parent elements
            let event = new CustomEvent('or-input:changed', {
                detail: { value: value },
                bubbles: true,
                composed: true });

            this.dispatchEvent(event);

        }
    }


    constructor() {
        super();
    }


}

window.customElements.define('or-input', OrInput);
