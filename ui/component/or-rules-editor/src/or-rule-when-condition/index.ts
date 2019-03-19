import {html, LitElement, property, customElement} from 'lit-element';
import '../selects/or-select-asset-attribute';
import '../selects/or-select-operator';

import '@openremote/or-input';
import '@openremote/or-select';

import {style} from './style';
import {AttributePredicate} from "@openremote/model";

@customElement('or-rule-when-condition')
class OrRuleWhenCondition extends LitElement {

    static get styles() {
        return [
            style
        ]
    }

    protected render() {

        return html`
            <div class="rule-container">
                ${this.predicate ? html`
                    <or-select-asset-type>
                        <i class="material-icons">
                            ${this.predicate.name ? this.predicate.name.value : 'defaultIcon'}
                        </i>
                    </or-select-asset-type>
                    ${this.predicate.name ? html`
                        <or-select-asset-attribute icon="${this.predicate}" value="${this.predicate.name.value}"></or-select-asset-attribute>
                    ` : ``}
                    
                    ${this.predicate.value && this.predicate.value.predicateType === 'string' ? html`
                        <or-select-operator type="${this.predicate.value.predicateType}" value="${this.predicate.value.match}"></or-select-operator>
                    
                        <or-input type="text" value="${this.predicate.value.value}"></or-input>
                    
                    ` : ``}
                    
                ` : ``}
            </div> 
        `;
    }

    @property({type: Object})
    predicate?: AttributePredicate;

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

    setAssetAttribute(e:any) {
        const value = e.detail.value;

        if(this.predicate && this.predicate.name) {
            this.predicate.name.value = value;
            console.log(this.predicate);
        }
    }

    setOperator(e:any){
        const value = e.detail.value;
        if(this.predicate && this.predicate.value && this.predicate.value.predicateType === 'string') {
            this.predicate.value.match = value;
            console.log(this.predicate);
        }
    }

    setValue(e:any) {
        const value = e.detail.value;

        if(this.predicate && this.predicate.value && this.predicate.value.predicateType === 'string') {
            this.predicate.value.value = value;
            console.log(this.predicate);
        }
    }

    constructor() {
        super();

        this.addEventListener('asset-attribute:changed', this.setAssetAttribute);
        this.addEventListener('operator:changed', this.setOperator);
        this.addEventListener('or-input:changed', this.setValue);

    }


}

