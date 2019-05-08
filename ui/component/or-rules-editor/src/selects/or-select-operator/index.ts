import {html, LitElement, property, customElement, PropertyValues} from "lit-element";
import openremote from "@openremote/core";
import {AttributeValueType, BaseAssetQueryMatch, BaseAssetQueryOperator, AttributeValueDescriptor} from "@openremote/model";

import {selectStyle} from "@openremote/or-select/dist/style";
import {AssetModelUtil} from "@openremote/core/dist";

@customElement('or-select-operator')
class OrRuleWhen extends LitElement {

    @property({type: Object})
    type?: AttributeValueDescriptor;

    @property({type: String})
    value?: BaseAssetQueryOperator;

    @property({type: Boolean})
    disabled: boolean = false;

    static get styles() {
        return [
            selectStyle
        ];
    }

    protected render() {

        return html`
             ${this.type ? html`
                <select ?disabled="${this.disabled}" id="or-select-operator" @change="${this.onChange}">
                    <option ?selected="${this.value === BaseAssetQueryOperator.EQUALS}" value="${BaseAssetQueryOperator.EQUALS}">=</option>
                    <option ?selected="${this.value === BaseAssetQueryOperator.NOT_EQUALS}" value="${BaseAssetQueryOperator.NOT_EQUALS}">!=</option>
                    ${AssetModelUtil.attributeValueDescriptorsMatch(this.type, AttributeValueType.NUMBER) ? html`
                        <option ?selected="${this.value === BaseAssetQueryOperator.LESS_THAN}" value="${BaseAssetQueryOperator.LESS_THAN}" value="LESS_THAN"><</option>
                        <option ?selected="${this.value === BaseAssetQueryOperator.GREATER_THAN}" value="${BaseAssetQueryOperator.GREATER_THAN}" value="GREATER_THAN">></option>
                        <option ?selected="${this.value === BaseAssetQueryOperator.LESS_EQUALS}" value="${BaseAssetQueryOperator.LESS_EQUALS}" value="LESS_EQUALS">=<</option>
                        <option ?selected="${this.value === BaseAssetQueryOperator.GREATER_EQUALS}" value="${BaseAssetQueryOperator.GREATER_EQUALS}" value="GREATER_EQUALS">=></option>
                    ` : ``}
                </select>
            ` : ``}
        `;
    }

    onChange() {
        if (this.shadowRoot) {
            const value = (<HTMLInputElement>this.shadowRoot.getElementById('or-select-operator')).value;

            const event = new CustomEvent('operator:changed', {
                detail: { value: value },
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
