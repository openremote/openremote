import {html, LitElement, customElement, property} from "lit-element";
import {AttributeDescriptor, AttributeValueType, AttributeValueDescriptor, ValueType} from "@openremote/model";

const Test = {
    STRING: {name: "flightProfile", icon: "file-text-o", valueType: ValueType.STRING},
    NUMBER: {name: ""}
};
@customElement("or-select-asset-attribute")
class OrSelectAssetAttribute extends LitElement {

    @property({type: String})
    public icon: string = "";

    @property({type: String})
    public assetType: string = "";

    @property({type: Array})
    public attributeDescriptors?: AttributeDescriptor[];

    @property({type: String})
    public value: any;

    @property({type: Boolean})
    public disabled: boolean = false;
    @property({type: Function})
    private changed: any;

    constructor() {
        super();

        // TODO Should come from rules-config based on asset type?
        this.attributeDescriptors = [
            { name: "flightProfile", valueDescriptor: { name: "NUMBER", valueType: ValueType.NUMBER } },
            { name: "profileName", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } },
            { name: "profileColor", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } },
            { name: "airportIata", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } },
            { name: "airlineIata", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } },
            { name: "originRegion", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } },
            { name: "languageCodes", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } },
            { name: "passengerCapacity",  valueDescriptor: { name: "NUMBER", valueType: ValueType.NUMBER } },
            { name: "countryCode", valueDescriptor: { name: "STRING", valueType: ValueType.STRING } }
        ];
    }

    public onChange() {
        if (this.shadowRoot) {
            const value = (this.shadowRoot.getElementById("or-select-asset-attribute") as HTMLInputElement).value;

            const event = new CustomEvent("asset-attribute:changed", {
                detail: { value: value },
                bubbles: true,
                composed: true });

            this.dispatchEvent(event);
        }
    }

    protected render() {

        // TODO types should be based on rules-config
        return html`
             <select ?disabled="${this.disabled}" id="or-select-asset-attribute" @change="${this.onChange}">
                ${this.attributeDescriptors ? this.attributeDescriptors.map((attribute: AttributeDescriptor) => {
                    return html`
                        <option ?selected="${attribute.name === this.value}" value="${attribute.name}">${attribute.name}</option>
                    `;
                }) : ``}
            </select>
        `;
    }
}
