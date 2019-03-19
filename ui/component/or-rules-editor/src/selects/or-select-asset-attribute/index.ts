import {html, LitElement, customElement, property} from 'lit-element';
//import "@material/mwc-icon"
import {AttributeDescriptor, AttributeValueType} from "@openremote/model"

@customElement('or-select-asset-attribute')
class OrSelectAssetAttribute extends LitElement {
    @property({type: Function})
    private changed: any;

    @property({type: String})
    icon: string = '';

    @property({type: String})
    assetType: string = '';

    @property({type: Array})
    attributeDescriptors?: AttributeDescriptor[];

    @property({type: String})
    value: any;

    protected render() {

        // TODO types should be based on rules-config
        return html`
             <select id="or-select-asset-attribute" @change="${this.onChange}">
                ${this.attributeDescriptors ? this.attributeDescriptors.map((attribute:AttributeDescriptor) => {
                    return html`
                        <option value="${attribute.name}">${attribute.name}</option>
                    `
                }) : ``}
            </select>
        `;
    }

    onChange() {
        if(this.shadowRoot){
            const value = (<HTMLInputElement>this.shadowRoot.getElementById('or-select-asset-attribute')).value;

            let event = new CustomEvent('asset-attribute:changed', {
                detail: { value: value },
                bubbles: true,
                composed: true });

            this.dispatchEvent(event);
        }
    }

    constructor() {
        super();

        // TODO Should come from rules-config based on asset type?
        // TODO what is the attribute name of profile flights?
        this.attributeDescriptors = [
            { name: 'flightProfile', valueType: AttributeValueType.STRING },
            { name: 'airportIata', valueType: AttributeValueType.STRING },
            { name: 'airlineIata', valueType: AttributeValueType.STRING },
            { name: 'originRegion', valueType: AttributeValueType.STRING },
            { name: 'languageCodes', valueType: AttributeValueType.STRING },
            { name: 'passengerCapacity',  valueType: AttributeValueType.NUMBER },
            { name: 'countryCode', valueType: AttributeValueType.STRING }
        ];
    }


}

