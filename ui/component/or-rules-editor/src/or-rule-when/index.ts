import {html, LitElement, property, customElement, PropertyValues} from "lit-element";

import {style} from "./style";
import {Rule, AttributePredicate} from "@openremote/model";

import "../or-rule-when-condition";
import {defaultPredicate, defaultWhenCondition, rulesEditorConfig} from "../const/rule-config";

@customElement("or-rule-when")
class OrRuleWhen extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Object})
    public rule?: Rule;

    @property({type: Array})
    public predicates?: AttributePredicate[] = [];


    constructor() {
        super();
        this.addEventListener("when-condition:delete", this.deleteWhenCondition);

    }

    protected render() {

        return html`
           <div class="rule-content-section">
                <div class="rule-when-container bg-white shadow">
                    <strong>Als..</strong>
                    ${this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates ? html`
                        ${this.rule.when.asset.attributes.predicates.map((predicate: AttributePredicate, index) => {
                            return html`
                                    <or-rule-when-condition index="${index}" .predicate="${predicate}"></or-rule-when-condition>
                            `; })}
                    ` : ``}
                    
                   ${rulesEditorConfig.controls.addWhenCondition ? html`
                        <a class="button-add" @click="${this.addWhenCondition}">+ voeg nog een voorwaarde toe</a>
                    ` : ``}
                </div>
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if (this.rule && !this.rule.when) {
            this.rule.when = {...defaultWhenCondition};
            if (this.rule!.when!.asset!.attributes!.predicates!.length === 0) {
                this.addPredicate();
            }
            this.requestUpdate();
        }
    }

    private addWhenCondition() {
        if (this.rule && !this.rule.when) {
            this.rule.when = {...defaultWhenCondition};
        }

        this.addPredicate();
        this.requestUpdate();

    }

    private addPredicate() {
        if (this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates) {
            this.rule.when.asset.attributes.predicates.push({...defaultPredicate});
        }
    }

    private deleteWhenCondition(e: any) {

        const index = e.detail.index;

        if (this.rule && this.rule.when && this.rule.when.asset && this.rule.when.asset.attributes && this.rule.when.asset.attributes.predicates) {
            this.rule.when.asset.attributes.predicates.splice(index, 1);
            this.requestUpdate();
        }
    }
}
