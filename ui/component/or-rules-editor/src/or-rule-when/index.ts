import {html, LitElement, property, customElement, PropertyValues} from "lit-element";

import {style} from "./style";
import openremote from "@openremote/core";
import {Rule, AttributePredicate} from "@openremote/model";

import "../or-rule-when-condition";
import {defaultPredicate, defaultWhenCondition, rulesEditorConfig} from "../const/rule-config";

import cloneDeep from "lodash-es/cloneDeep";

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

    // TODO: cleanup hacked asset type pass through
    protected render() {
        return html`
           <div class="rule-content-section">
                <div class="rule-when-container bg-white shadow">
                    <strong>Als..</strong>
                    ${this.rule && this.rule.when && this.rule.when.predicates && this.rule.when.predicates.length > 0 && this.rule.when.predicates[0].assets && this.rule.when.predicates[0].assets.types && this.rule.when.predicates[0].assets.attributes && this.rule.when.predicates[0].assets.attributes.predicates ? html`
                        ${this.rule.when.predicates[0].assets.attributes.predicates.map((predicate: AttributePredicate, index) => {
                            return html`
                                    <or-rule-when-condition index="${index}" .assetType="${this.rule!.when!.predicates![0]!.assets!.types![0].value}" .predicate="${predicate}"></or-rule-when-condition>
                            `; })}
                    ` : ``}
                    
                    ${openremote.hasRole("write:assets") ? html`
                       ${rulesEditorConfig.controls.addWhenCondition ? html`
                            <a class="button-add" @click="${this.addWhenCondition}">+ voeg nog een voorwaarde toe</a>
                        ` : ``}
                       
                    ` : ``}
                </div>
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);
        if (this.rule && !this.rule.when) {
            this.rule.when = cloneDeep(defaultWhenCondition);
            if (this.rule.when.predicates![0].assets!.attributes!.predicates!.length === 0) {
                this.addPredicate();
            }
            this.requestUpdate();
        }
    }

    private addWhenCondition() {
        if (this.rule && !this.rule.when) {
            this.rule.when = cloneDeep(defaultWhenCondition);
        }

        this.addPredicate();
        this.requestUpdate();

    }

    private addPredicate() {
        if (this.rule && this.rule.when && this.rule.when.predicates && this.rule.when.predicates.length > 0 && this.rule.when.predicates[0].assets && this.rule.when.predicates[0].assets.attributes && this.rule.when.predicates[0].assets.attributes.predicates) {
            const newPredicate = cloneDeep(defaultPredicate);

            this.rule.when.predicates[0].assets.attributes.predicates.push(newPredicate);
        }
    }

    private deleteWhenCondition(e: any) {

        const index = e.detail.index;

        if (this.rule && this.rule.when && this.rule.when.predicates && this.rule.when.predicates.length > 0 && this.rule.when.predicates[0].assets && this.rule.when.predicates[0].assets.attributes && this.rule.when.predicates[0].assets.attributes.predicates) {
            this.rule.when.predicates[0].assets.attributes.predicates.splice(index, 1);
            this.requestUpdate();
        }
    }
}
