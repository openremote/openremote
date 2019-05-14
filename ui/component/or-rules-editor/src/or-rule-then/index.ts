import {html, LitElement, property, customElement, PropertyValues} from "lit-element";

import {style} from "./style";
import {Rule, RuleActionUnion,
    AttributeDescriptor} from "@openremote/model";

import "../or-rule-then-condition";
import {rulesEditorConfig, defaultThen, defaultThenCondition} from "../const/rule-config";

import cloneDeep from "lodash-es/cloneDeep";

@customElement("or-rule-then")
class OrRuleThen extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Object})
    public rule?: Rule;

    @property({type: Array})
    public attributeDescriptors?: AttributeDescriptor[];

    constructor() {
        super();
    }

    protected render() {

        return html`
               <div class="rule-content-section">
                    <div class="rule-then-container bg-white shadow">
                        <strong>Dan...</strong>

                       ${this.rule && this.rule.then ? this.rule.then.map((then: RuleActionUnion) => {
                            return html`
                                <or-rule-then-condition .condition="${then}" .attributeDescriptors="${this.attributeDescriptors}"></or-rule-then-condition>
                                
                               ${rulesEditorConfig.controls.addThenCondition ? html`
                                    <span class="rule-additional">&</span>
                               ` : ``}
                               
                        `; }) : ``}
                       
                       ${rulesEditorConfig.controls.addThenCondition ? html`
                            <a class="button-add" @click="${this.addThenCondition}">+</a>
                       ` : ``}
                    </div>
                </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if (this.rule && !this.rule.then) {
            this.rule.then = cloneDeep(defaultThen);
            this.requestUpdate();
        }
    }

    private addThenCondition() {
        if (this.rule && !this.rule.then) {
            this.rule.then = cloneDeep(defaultThen);

        }

        if (this.rule && this.rule.then) {
            this.rule.then.push(cloneDeep(defaultThenCondition));
            this.requestUpdate();
        }

    }

}
