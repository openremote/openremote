import {customElement, html, LitElement, property, TemplateResult} from "lit-element";

import "@openremote/or-select";
import "@openremote/or-icon";
import {
    AttributeDescriptor,
    Rule,
    RuleActionWriteAttribute,
    RulesetLang,
    TenantRuleset,
    ValueType
} from "@openremote/model";
import openremote from "@openremote/core";
import rest from "@openremote/rest";

import "./or-rule-list";
import "./or-rule-when";
import "./or-rule-then";
import "./or-rule-header";

import {style} from "./style";
import findIndex from "lodash-es/findIndex";

const ruleModel: Rule = {
    name: "",
    when: undefined,
    then: undefined
};

const rulesetModel: TenantRuleset = {
    name: "New Rule",
    type: "tenant",
    lang: RulesetLang.JSON,
    realm: openremote.getRealm(),
    accessPublicRead: true,
    rules: JSON.stringify({rules: [ruleModel]})
};


const attributeDescriptors: AttributeDescriptor[] = [
    {name: "profiles", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "airportIata", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "airlineIata", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "originRegion", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "languageCodes", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "passengerCapacity", valueDescriptor: {name: "NUMBER", valueType: ValueType.NUMBER}},
    {name: "countryCode", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}}
];

const rulesEditorConfig = {
    languageCodes: {
        options: [
            "Dutch",
            "English",
        ]
    },
    countryCode: {
        options: [
            "NL",
            "GB",
        ]
    },
};

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

    constructor() {
        super();
        this.readRules();
        this.addEventListener("rules:set-active-rule", this.setActiveRule);
        this.addEventListener("rules:write-rule", this.writeRule);
        this.addEventListener("rules:create-rule", this.createRule);
        this.addEventListener("rules:update-rule", this.updateRule);
        this.addEventListener("rules:delete-rule", this.deleteRule);
    }

    public readRules() {
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

    public createRule() {
        if (this.rulesets) {

            const newRule = rulesetModel;
            this.rulesets = [...this.rulesets, rulesetModel];
            this.ruleset = newRule;
            this.computeRuleset();


        }
    }

    public writeRule() {
        if (this.ruleset && this.rule) {
            this.rule.name = this.ruleset.name;
            this.ruleset.rules = JSON.stringify({rules: [this.rule]});
            rest.api.RulesResource.createTenantRuleset(this.ruleset).then((response: any) => {
                this.readRules();
            });
        }
    }

    public updateRule(e: any) {
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

    public deleteRule() {
        if (this.ruleset && this.ruleset.id) {
            rest.api.RulesResource.deleteTenantRuleset(this.ruleset.id).then((response: any) => {

                if (this.rulesets && this.ruleset) {
                    const index = findIndex(this.rulesets, ["id", this.ruleset.id]);
                    this.rulesets.splice(index, 1);
                    this.rulesets = [...this.rulesets];
                    this.ruleset = undefined;
                    this.rule = undefined;
                    this.requestUpdate();
                }

            });
        }
    }

    public computeRuleset() {
        if (this.ruleset && this.ruleset.rules) {
            this.rule = JSON.parse(this.ruleset.rules).rules[0];
        } else if (this.ruleset && !this.ruleset.rules) {
            this.rule = ruleModel;
            this.rule.name = this.ruleset.name;
        }
        this.requestUpdate();
    }

    public setActiveRule(e: any) {
        this.ruleset = e.detail.ruleset;

        this.computeRuleset();
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
                        <or-rule-header class="bg-white shadow" .ruleset="${this.ruleset}" .rule="${this.rule}"></or-rule-header>
                        
                        ${this.rule ? html`
                            <div class="content">
                                <or-rule-when .rule="${this.rule}" .attributeDescriptors="${attributeDescriptors}"></or-rule-when>
                                <or-rule-then .rule="${this.rule}" .attributeDescriptors="${attributeDescriptors}"></or-rule-then>
                            </div>
                        ` : ``}
                    
                    </or-body>
              ` : html``}
          </div>
        `;
    }
}

