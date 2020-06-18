import {css, customElement, html, LitElement, property} from "lit-element";
import {i18next, translate} from "@openremote/or-translate";
import {
    AssetAttribute,
    AttributeRef,
    AttributeValueType,
    AttributeEvent,
} from "@openremote/model";
import manager, {subscribe} from "@openremote/core";
import "@openremote/or-input";

class ORAttributeTemplateProvider {
    _template: Function | null = null;

    getTemplate(object:AssetAttribute | undefined) {
        if(this._template){
            const template = this._template(object);
            if (template) {
                return template;
            }
        }
    }

    setTemplate(callback: Function) {
        this._template = callback;
    }
}

export let orAttributeTemplateProvider = new ORAttributeTemplateProvider();

orAttributeTemplateProvider.setTemplate((attribute: AssetAttribute) => {
    let template;
    switch (attribute.type) {
        default:
            template = attribute.value;
            break;
    }
    return template;
});

@customElement("or-attribute-field")
export class OrAttributeInput extends translate(i18next)(LitElement) {

    static get styles() {
        return css`
            :host {
                display: inline-block;
            }
            
        `;
    }

    @property({type: Object, reflect: false})
    public attribute?: AssetAttribute;

    constructor() {
        super();
    }

    public render() {
        return html`${orAttributeTemplateProvider.getTemplate(this.attribute)}`;
    }
}
