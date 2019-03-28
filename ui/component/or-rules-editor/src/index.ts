import {html, LitElement, property, customElement} from 'lit-element';

import '@openremote/or-select';
import '@openremote/or-icon';
import {Rule, JsonRulesetDefinition, Ruleset, RulesetLang, TenantRuleset} from '@openremote/model';
import openremote from "@openremote/core";
import rest from "@openremote/rest";

import './or-rule-list';
import './or-rule-when';
import './or-rule-then';
import './or-rule-header';

import {style} from "./style";
import findIndex from 'lodash-es/findIndex';

const ruleModel:Rule = {
    name: "",
    when: undefined,
    then: undefined
};

const rulesetModel:TenantRuleset = {
    name: "New Rule",
    type: "tenant",
    lang: RulesetLang.JSON,
    realm: openremote.getRealm(),
    accessPublicRead: true,
    rules: JSON.stringify({rules: [ruleModel]})
};

@customElement('or-rules-editor')
class OrRulesEditor extends LitElement {

    static get styles() {
        return [
            style
        ]
    }

    protected render() {

        return html`
            <div class="rule-editor-container">
              <side-menu class="bg-white shadow">
                    <or-rule-list .rulesets="${this.rulesets}" .ruleset="${this.ruleset}"></or-rule-list>
                    <div class="bottom-toolbar">
                      <icon class="small-icon" @click="${this.deleteRule}"><or-icon icon="delete"></or-icon></icon>
                      <icon class="small-icon" @click="${this.createRule}"><or-icon icon="plus"></or-icon></icon>
                    </div>
              </side-menu>
              ${this.ruleset ? html`
                    <or-body>
                        <or-rule-header class="bg-white shadow" .ruleset="${this.ruleset}"></or-rule-header>
                        
                        ${this.rule ? html`
                            <div class="content">
                                <or-rule-when .rule="${this.rule}"></or-rule-when>
                                <or-rule-then .rule="${this.rule}"></or-rule-then>
                            </div>
                        ` : ``}
                    
                    </or-body>
              ` : html``}
          </div>
        `;
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


    constructor() {
        super();
        this.readRules();
        this.addEventListener('rules:set-active-rule', this.setActiveRule);
        this.addEventListener('rules:create-rule', this.createRule);
        this.addEventListener('rules:update-rule', this.updateRule);
        this.addEventListener('rules:delete-rule', this.deleteRule);
    }


    readRules() {
        rest.api.RulesResource.getTenantRulesets(openremote.config.realm,  { fullyPopulate: true }).then((response: any) => {
            if (response && response.data) {
                this.rulesets = response.data;
                if(this.ruleset && this.rulesets) {
                    const index = findIndex(this.rulesets, ['id', this.ruleset.id]);
                    let updatedRuleset;

                    // ID is not found when a new ruleset is added
                    if(index > 0) {
                        updatedRuleset = this.rulesets[index];
                    } else {
                        updatedRuleset = this.rulesets[this.rulesets.length-1];
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


    createRule() {
        if(this.rulesets) {

            const newRule = rulesetModel;
            this.rulesets = [...this.rulesets, rulesetModel];
            this.ruleset = newRule;
            this.computeRuleset();

            rest.api.RulesResource.createTenantRuleset(newRule).then((response:any)=> {
                this.readRules();
            });
        }
    }

    updateRule(e:any) {
        this.ruleset = e.detail.ruleset;
        if(this.ruleset && this.ruleset.id && this.rule){
            delete this.ruleset.lastModified;
            delete this.ruleset.createdOn;
            delete this.ruleset.status;
            delete this.ruleset.error;

            // Parse rule to string of array of rules
            this.rule.name = this.ruleset.name;
            this.ruleset.rules = JSON.stringify({"rules": [this.rule] });

            //this.ruleset.rules = JSON.stringify(this.rules);
            rest.api.RulesResource.updateTenantRuleset(this.ruleset.id, this.ruleset).then((response:any)=> {
               this.readRules();
            });
        }

    }

    deleteRule() {
        if(this.ruleset && this.ruleset.id) {
            rest.api.RulesResource.deleteTenantRuleset(this.ruleset.id).then((response: any) => {

                if(this.rulesets && this.ruleset) {
                    const index = findIndex(this.rulesets, ['id', this.ruleset.id]);
                    this.rulesets.splice(index, 1);
                    this.rulesets = [...this.rulesets];
                    this.ruleset = undefined;
                    this.rule = undefined;
                    this.requestUpdate();
                }

            });
        }
    }

    computeRuleset () {
        if(this.ruleset && this.ruleset.rules){
            this.rule = JSON.parse(this.ruleset.rules).rules[0];
        }
        else if(this.ruleset && !this.ruleset.rules){
            this.rule = ruleModel;
            this.rule.name = this.ruleset.name;
        }

        this.requestUpdate();
    }

    setActiveRule(e:any) {
        this.ruleset = e.detail.ruleset;

        this.computeRuleset();
    }
}

