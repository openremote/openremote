import {html, LitElement, property} from 'lit-element';


import {orInputStyle} from './style';
class OrInput extends LitElement {

    @property({type: String})
    public type: string = "text";

    @property({type: String})
    public name: string = "";

    @property({type: Boolean})
    public required: boolean = false;

    @property({type: String})
    public value: string = "";


    static get styles() {
        return [
            orInputStyle
        ];
    }

    protected render() {

        return html`
             <input class="or-input" ?required="${this.required}" type="${this.type}" name="${this.name}" @change="${this.onChange}" .value="${this.value}" />
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


}

window.customElements.define('or-input', OrInput);
