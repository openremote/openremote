import {html, LitElement, property, customElement} from 'lit-element';
import {AttributeValueType, BaseAssetQueryMatch} from "@openremote/model";

@customElement('or-select-operator')
class OrRuleWhen extends LitElement {

    @property({type: String})
    type?: AttributeValueType;

    @property({type: String})
    value?: BaseAssetQueryMatch;

    protected render() {

        return html`
             ${this.type ? html`
                <select id="or-select-operator" @change="${this.onChange}">
                    <option ?selected="${this.value === BaseAssetQueryMatch.EXACT}" value="${BaseAssetQueryMatch.EXACT}">=</option>
                    <option ?selected="${this.value === BaseAssetQueryMatch.NOT_EXACT}" value="${BaseAssetQueryMatch.NOT_EXACT}">!=</option>
                    ${this.type === AttributeValueType.NUMBER ? html`
                        <option value="LESS_THAN"><</option>
                        <option value="GREATER_THAN">></option>
                        <option value="LESS_EQUALS">=<</option>
                        <option value="GREATER_EQUALS">=></option>
                    ` :``}
                </select>
            ` :``}
        `;
    }


    onChange() {
        if(this.shadowRoot){
            const value = (<HTMLInputElement>this.shadowRoot.getElementById('or-select-operator')).value;

            let event = new CustomEvent('operator:changed', {
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

