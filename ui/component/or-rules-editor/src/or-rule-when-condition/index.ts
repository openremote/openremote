import {customElement, html, LitElement, property, PropertyValues} from "lit-element";
import "../selects/or-select-asset-attribute";
import "../selects/or-select-operator";

import "@openremote/or-input";
import "@openremote/or-select";
import "@openremote/or-icon";
import {AssetModelUtil} from "@openremote/core";

import {style} from "./style";
import {AttributeDescriptor, AttributePredicate, AssetDescriptor} from "@openremote/model";

import {attributeDescriptors} from "../const/attribute-descriptors";
import {rulesEditorConfig} from "../const/rule-config";

@customElement("or-rule-when-condition")
class OrRuleWhenCondition extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    protected assetDescriptor?: AssetDescriptor;

    @property({type: Object})
    public predicate?: AttributePredicate;

    @property({type: Number})
    public index?: number;

    @property({type: String})
    public assetType?: string;

    @property({type: Array})
    public attributeDescriptors?: AttributeDescriptor[] = attributeDescriptors;

    @property({type: Boolean})
    public isValidRule: boolean = false;

    constructor() {
        super();

        this.addEventListener("asset-attribute:changed", this.setAssetAttribute);
        this.addEventListener("operator:changed", this.setOperator);
        this.addEventListener("or-input:changed", this.setValue);
    }

    protected shouldUpdate(_changedProperties: Map<PropertyKey, unknown>): boolean {
        if (_changedProperties.has("assetType")) {
            this.assetDescriptor = AssetModelUtil.getAssetDescriptor(this.assetType) || {attributeDescriptors: []};
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected render() {

        return html`
            <div class="rule-container">
                ${this.predicate ? html`
                    <or-select-asset-type>
                        <or-icon icon="${this.predicate.name ? "airplane" : "airplane"}">
                        </or-icon>
                    </or-select-asset-type>
                    
                    ${this.predicate.name ? html`
                        <or-select-asset-attribute value="${this.predicate.name.value}"></or-select-asset-attribute>
                    
                        ${this.predicate.value && this.predicate.value.predicateType === "string" ? html`
                            ${this.predicate.name.value ? html`
                                <or-select-operator .type="${this.getAttributeDescriptor(this.predicate.name.value)!.valueDescriptor}" .value="${this.predicate.value.match}"></or-select-operator>
                            ` : ``}
                            
                            ${this.predicate.name.value && this.predicate.value.match ? html`
                                ${this.getAttributeConfig(this.predicate.name.value)!.options  ? html`
                                    ${this.predicate.value.value}
                                    <or-select .options="${this.getAttributeConfig(this.predicate.name.value).options}" .value="${this.predicate.value.value ? this.predicate.value.value : ""}"></or-select>
                                ` : html`
                                    <or-input type="text" .value="${this.predicate.value.value ? this.predicate.value.value : null}"></or-input>
                                `}
                            ` : ``}
                            
                             ${this.predicate.value.value ? html`
                                <a style="margin-left: auto;" @click="${this.deleteCondition}">
                                 <or-icon class="small-icon" icon="close-circle"></or-icon>
                                </a>
                            ` : ``}
                            
                        ` : ``}
                        
                    ` : ``}
                    
                ` : ``}
            </div> 
        `;
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if (_changedProperties.has("predicate")) {
            this.validateRule();
        }
    }

    private validateRule() {

        if (this.predicate && this.predicate.value && this.predicate.value.predicateType === "string" && this.predicate.value.value) {
            this.isValidRule = true;
        } else {
            this.isValidRule = false;
        }
        const event = new CustomEvent("rules:validated", {
            detail: { isValidRule: this.isValidRule },
            bubbles: true,
            composed: true });

        this.dispatchEvent(event);

    }

    // setAssetType(value:string) {
    //     const assetType = {
    //         "predicateType": "string",
    //         "match": "EXACT",
    //         "value": value
    //     };
    //
    //     // TODO how does this work with multiple assetTypes?
    //     if(this.predicate && this.when.asset && this.when.asset.types) {
    //         this.when.asset.types.push(assetType);
    //     }
    // }

    private getAttributeDescriptor(attributeName: string) {
        if (!this.assetDescriptor) {
            return;
        }

        return AssetModelUtil.getAssetAttributeDescriptor(this.assetDescriptor, attributeName);
    }

    private getAttributeConfig(name: string) {
        const attributeValueDescriptors: any = rulesEditorConfig.options.attributeValueDescriptors;

        if (attributeValueDescriptors) {
            const attributeConfig = attributeValueDescriptors[name];
            return attributeConfig;
        }

    }

    private setAssetAttribute(e: any) {
        const value = e.detail.value;

        if (this.predicate && this.predicate.name) {
            this.predicate.name.value = value;

            this.setOperator(e);
            this.requestUpdate();
        }
    }

    private setOperator(e: any) {
        const value = e.detail.value;
        if (this.predicate && this.predicate.value && this.predicate.value.predicateType === "string") {
            this.predicate.value.match = value;
            this.requestUpdate();
        }
    }

    private setValue(e: any) {
        const value = e.detail.value;

        if (this.predicate && this.predicate.value && this.predicate.value.predicateType === "string") {
            this.predicate.value.value = value;
            this.validateRule();
            this.requestUpdate();
        }
    }

    private deleteCondition() {

        const event = new CustomEvent("when-condition:delete", {
            detail: { index: this.index },
            bubbles: true,
            composed: true });

        this.dispatchEvent(event);

        this.requestUpdate();
    }

}
