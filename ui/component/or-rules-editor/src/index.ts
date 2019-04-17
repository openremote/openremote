import {customElement, html, LitElement, property, TemplateResult, PropertyValues} from "lit-element";

import "@openremote/or-select";
import "@openremote/or-icon";
import {
    Rule,
    RuleActionWriteAttribute,
    RulesetLang,
    TenantRuleset
} from "@openremote/model";
import openremote from "@openremote/core";
import rest from "@openremote/rest";

import "./or-rule-list";
import "./or-rule-when";
import "./or-rule-then";
import "./or-rule-header";

import {style} from "./style";
import findIndex from "lodash-es/findIndex";

import {attributeDescriptors} from "./const/attribute-descriptors";
import {ruleTemplate, rulesetTemplate} from "./const/rule-config";

class InputHandlers {
    public handlers: Array<(condition: RuleActionWriteAttribute) => TemplateResult | undefined> = [];

    public addInputHandler(callback: (condition: RuleActionWriteAttribute) => TemplateResult | undefined) {
        this.handlers.push(callback);
    }

    public removeInputHandler(callback: (condition: RuleActionWriteAttribute) => TemplateResult | undefined) {
        const i = this.handlers.indexOf(callback);
        if (i <= 0) {
            this.handlers.splice(i, 1);
        }
    }
}

export default new InputHandlers();

function confirmDialog(msg: string) {
    return new Promise( (resolve, reject) => {
        const confirmed = window.confirm(msg);

        return confirmed ? resolve(true) : reject(false);
    });
}

@customElement("or-rules-editor")
class OrRulesEditor extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Number})
    private value: number = 0;

    @property({type: Array})
    private rulesets?: TenantRuleset[];

    @property({type: Object})
    private ruleset?: TenantRuleset;

    @property({type: Array})
    private rules?: Rule[];

    @property({type: Object})
    private rule?: Rule;

    @property({type: Boolean})
    private isValidRule?: boolean;

    constructor() {
        super();
        this.readRules();

        this.addEventListener("rules:validated", this.validatedRule);
        this.addEventListener("rules:set-active-rule", this.setActiveRule);
        this.addEventListener("rules:write-rule", this.writeRule);
        this.addEventListener("rules:create-rule", this.createRule);
        this.addEventListener("rules:update-rule", this.updateRule);
        this.addEventListener("rules:delete-rule", this.deleteRule);

    }

    protected render() {

        return html`
            <div class="rule-editor-container">
              <side-menu class="bg-white shadow">
                    <or-rule-list .rulesets="${this.rulesets}" .ruleset="${this.ruleset}" ></or-rule-list>
                    <div class="bottom-toolbar">
                      <icon class="small-icon" @click="${this.deleteRule}"><or-icon icon="delete"></or-icon></icon>
                      <icon class="small-icon" @click="${this.createRule}"><or-icon icon="plus"></or-icon></icon>
                    </div>
              </side-menu>
              ${this.ruleset ? html`
                    <or-body>
                    
                        ${this.rule ? html`
                            <or-rule-header class="bg-white shadow" .ruleset="${this.ruleset}" .rule="${this.rule}" ?valid="${this.isValidRule}"></or-rule-header>
                        
                            <div class="content">
                                <or-rule-when .rule="${this.rule}" .attributeDescriptors="${attributeDescriptors}"></or-rule-when>
                                <or-rule-then .rule="${this.rule}" .attributeDescriptors="${attributeDescriptors}"></or-rule-then>
                            </div>
                        ` : `
                            <div class="content layout vertical center-center">
                                <h3>Kies links een profiel of maakt een nieuw profiel aan.</h3>
                                <button @click="${this.createRule}">profiel aanmaken</button>
                            </div>
                        `}
                    
                    </or-body>
              ` : html``}
          </div>
        `;
    }

    private validatedRule(e: any) {
        this.isValidRule = e.detail.isValidRule;
    }

    private readRules() {
        rest.api.RulesResource.getTenantRulesets(openremote.config.realm, {
            language: RulesetLang.JSON,
            fullyPopulate: true
        }).then((response: any) => {
            if (response && response.data) {
                this.rulesets = response.data;
                if (this.ruleset && this.rulesets) {
                    const index = findIndex(this.rulesets, ["id", this.ruleset.id]);
                    let updatedRuleset;

                    // ID is not found when a new ruleset is added
                    if (index > 0) {
                        updatedRuleset = this.rulesets[index];
                    } else {
                        updatedRuleset = this.rulesets[this.rulesets.length - 1];
                    }

                    this.ruleset = updatedRuleset;
                    this.computeRuleset();
                }
                this.requestUpdate();
            }
        }).catch((reason: any) => {
            console.log("Error:" + reason);
        });
    }

    private createRule() {
        const shouldContinue = this.shouldShowModal();
        if (!shouldContinue) {
            return;
        }

        if (this.rulesets) {
            const newRule = {...rulesetTemplate};
            this.rulesets = [...this.rulesets, rulesetTemplate];
            this.ruleset = newRule;
            this.computeRuleset();
        }
    }

    private writeRule() {
        if (this.ruleset && this.rule) {
            this.rule.name = this.ruleset.name;
            this.ruleset.rules = JSON.stringify({rules: [this.rule]});
            rest.api.RulesResource.createTenantRuleset(this.ruleset).then((response: any) => {
                this.readRules();
            });
        }
    }

    private updateRule(e: any) {
        this.ruleset = e.detail.ruleset;
        if (this.ruleset && this.ruleset.id && this.rule) {
            delete this.ruleset.lastModified;
            delete this.ruleset.createdOn;
            delete this.ruleset.status;
            delete this.ruleset.error;

            // Parse rule to string of array of rules
            this.rule.name = this.ruleset.name;
            this.ruleset.rules = JSON.stringify({rules: [this.rule]});

            // this.ruleset.rules = JSON.stringify(this.rules);
            rest.api.RulesResource.updateTenantRuleset(this.ruleset.id, this.ruleset).then((response: any) => {
                this.readRules();
            });
        }

    }

    private deleteRule() {

        if (!this.ruleset || !this.ruleset.id) {
            return;
        }
        const id = this.ruleset.id;

        confirmDialog("Weet je zeker dat je deze regel wilt verwijderen?")
            .then(() =>  {
                    rest.api.RulesResource.deleteTenantRuleset(id).then(() => {
                        this.cleanRule();
                    });
            })
            .catch(() => {
                // do something when canceled?
            });

    }

    private cleanRule() {
        if (this.rulesets && this.ruleset) {
            const index = findIndex(this.rulesets, ["id", this.ruleset.id]);
            if (index) {
                this.rulesets.splice(index, 1);
            } else {
                this.rulesets.splice(this.rulesets.length - 1, 1);
            }

            this.rulesets = [...this.rulesets];
            this.ruleset = undefined;
            this.rule = undefined;
            this.requestUpdate();
        }
    }

    private computeRuleset() {
        if (this.ruleset && this.ruleset.rules) {
            this.rule = JSON.parse(this.ruleset.rules).rules[0];
        } else if (this.ruleset && !this.ruleset.rules) {
            this.rule = {...ruleTemplate};
            this.rule.name = this.ruleset.name;
        }
        this.requestUpdate();
    }

    private setActiveRule(e: any) {
       const shouldContinue = this.shouldShowModal();
       if (!shouldContinue) {
           return;
       }
       this.ruleset = e.detail.ruleset;
       this.computeRuleset();
    }

    private shouldShowModal() {
         if (this.ruleset && this.ruleset.rules !== JSON.stringify({rules: [this.rule]})) {
            confirmDialog("Weet je zeker dat je deze regel niet wilt opslaan?")
                .then(() =>  {
                    this.readRules();
                    return true;
                })
                .catch(() => {
                    return false;
                });
        } else {
             return true;
         }

    }


}
