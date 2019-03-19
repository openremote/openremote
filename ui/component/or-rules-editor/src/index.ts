import {html, LitElement, property, customElement} from 'lit-element';

import '@openremote/or-select';
import {Rule, JsonRulesetDefinition, Ruleset} from '@openremote/model';
import openremote from "@openremote/core";
import rest from "@openremote/rest";

import './or-rule-list';
import './or-rule-when';
import './or-rule-then';
import './or-rule-header';

import {style} from "./style";

const ruleModel:Rule = {
    name: "",
    when: undefined,
    then: undefined
};
//TODO rules should be based on the type Rule
const rulesetModel:Ruleset = {
    name: "",
    type: "tenant",
    rules: ""
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
                    <or-rule-list .rules="${this.rulesets}"></or-rule-list>
                    <div class="bottom-toolbar">
                      <icon @click="${this.createRule}">[plus]</icon>
                    </div>
              </side-menu>
              ${this.ruleset && this.ruleset.rules ? html`
                    <or-body>
                        <or-rule-header class="bg-white shadow" .rule="${this.ruleset}"></or-rule-header>
                        <div class="content">
                            <or-rule-when .rule="${this.ruleset.rules}"></or-rule-when>
                            <or-rule-then .rule="${this.ruleset.rules}"></or-rule-then>
                        </div>
                    </or-body>
              ` : html`
              
              `}
          </div>
        `;
    }

    @property({type: Number})
    private value: number = 0;

    @property({type: Array})
    private rulesets?: Ruleset[];

    @property({type: Object})
    private ruleset?: Ruleset;

    @property({type: Array})
    private rules?: Rule[];

    @property({type: Object})
    private rule?: Rule;


    constructor() {
        super();
        this.readRules();
        this.addEventListener('rules:set-active-rule', this.setActiveRule);
        this.addEventListener('rules:update-rule', this.updateRule);
        this.addEventListener('rules:delete-rule', this.deleteRule);
    }


    readRules() {
        rest.api.RulesResource.getTenantRulesets(openremote.config.realm).then((response: any) => {

            // TODO remove this when rules are in the project
            response.data = [{
                "type": "tenant",
                "id": 1001,
                "version": 6,
                "createdOn": 1551799275058,
                "lastModified": 1552321167681,
                "name": "Geofence Test",
                "enabled": true,
                "rules": "{\n  \"rules\": [\n    {\n      \"name\": \"Rich Home\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.765106,\n                  \"lng\": -2.233860\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Rich Home\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Duurzaamheid\",\n              \"body\": \"Samenwerken aan duurzaamheid\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n  {\n      \"name\": \"Pierre Home\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.416214,\n                  \"lng\": 5.483527\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Pierre Home\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Welkom thuis\",\n              \"body\": \"Meer weten over de Vestdijk?\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n  {\n      \"name\": \"Vestdijk\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.438803,\n                  \"lng\": 5.481697\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Vestdijk\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Beleef de Vestdijk\",\n              \"body\": \"Meer weten over de Vestdijk?\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n  {\n      \"name\": \"Boeiende Binnenstad\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.439887,\n                  \"lng\": 5.478430\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Boeiende Binnenstad\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Boeiende Binnenstad\",\n              \"body\": \"Wat jullie vonden van de bestrating\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/boeiende-binnenstad/stapsgewijs-naar-een-nieuwe-inrichting\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Bekijk, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/boeiende-binnenstad/stapsgewijs-naar-een-nieuwe-inrichting\"\n                  }\n                },\n                {\n                  \"title\": \"Bekijk, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/boeiende-binnenstad/stapsgewijs-naar-een-nieuwe-inrichting\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n{\n      \"name\": \"Michael Home\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.687741,\n                  \"lng\": 5.286913\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Rich Home\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Michael\",\n              \"body\": \"Welcome to Den Bosch\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    }\n  ]\n}",
                "lang": "JSON",
                "status": "DEPLOYED",
                "realm": "eindhoven",
                "accessPublicRead": true
            }, {
                    "type": "tenant",
                    "id": 1002,
                    "version": 6,
                    "createdOn": 1551799275058,
                    "lastModified": 1552321167681,
                    "name": "Geofence Test 2",
                    "enabled": true,
                    "rules": "{\n  \"rules\": [\n    {\n      \"name\": \"Rich Home\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.765106,\n                  \"lng\": -2.233860\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Rich Home\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Duurzaamheid\",\n              \"body\": \"Samenwerken aan duurzaamheid\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n  {\n      \"name\": \"Pierre Home\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.416214,\n                  \"lng\": 5.483527\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Pierre Home\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Welkom thuis\",\n              \"body\": \"Meer weten over de Vestdijk?\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n  {\n      \"name\": \"Vestdijk\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.438803,\n                  \"lng\": 5.481697\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Vestdijk\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Beleef de Vestdijk\",\n              \"body\": \"Meer weten over de Vestdijk?\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/beleef-de-vestdijk\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n  {\n      \"name\": \"Boeiende Binnenstad\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.439887,\n                  \"lng\": 5.478430\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Boeiende Binnenstad\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Boeiende Binnenstad\",\n              \"body\": \"Wat jullie vonden van de bestrating\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/boeiende-binnenstad/stapsgewijs-naar-een-nieuwe-inrichting\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Bekijk, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/boeiende-binnenstad/stapsgewijs-naar-een-nieuwe-inrichting\"\n                  }\n                },\n                {\n                  \"title\": \"Bekijk, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/boeiende-binnenstad/stapsgewijs-naar-een-nieuwe-inrichting\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    },\n{\n      \"name\": \"Michael Home\",\n      \"when\": {\n        \"asset\": {\n          \"types\": [\n            {\n              \"predicateType\": \"string\",\n              \"match\": \"EXACT\",\n              \"value\": \"urn:openremote:asset:console\"\n            }\n          ],\n          \"attributes\": {\n            \"predicates\": [\n              {\n                \"name\": {\n                  \"predicateType\": \"string\",\n                  \"match\": \"EXACT\",\n                  \"value\": \"location\"\n                },\n                \"value\": {\n                  \"predicateType\": \"radial\",\n                  \"radius\": 100,\n                  \"lat\": 51.687741,\n                  \"lng\": 5.286913\n                }\n              }\n            ]\n          }\n        }\n      },\n      \"then\": [\n        {\n          \"action\": \"notification\",\n          \"target\": {\n            \"useAssetsFromWhen\": true\n          },\n          \"notification\": {\n            \"name\": \"Rich Home\",\n            \"message\": {\n              \"type\": \"push\",\n              \"title\": \"Michael\",\n              \"body\": \"Welcome to Den Bosch\",\n              \"action\": {\n                \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n              },\n              \"buttons\": [\n                {\n                  \"title\": \"Ja, in app\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\"\n                  }\n                },\n                {\n                  \"title\": \"Ja, in browser\",\n                  \"action\": {\n                    \"url\": \"https://www.eindhoven.nl/projecten/samen-werken-aan-duurzaamheid\",\n                    \"openInBrowser\": true\n                  }\n                }\n              ]\n            }\n          }\n        }\n      ],\n      \"reset\": {\n        \"triggerNoLongerMatches\": true\n      }\n    }\n  ]\n}",
                    "lang": "JSON",
                    "status": "DEPLOYED",
                    "realm": "eindhoven",
                    "accessPublicRead": true
                }];

            if (response && response.data) {
                response.data.forEach((rulesSet: any) => {
                    let parsedRules = JSON.parse(rulesSet.rules);
                    // Only get first element of rules for now
                    // TODO how should this work if a ruleset has multiple rules?
                    rulesSet.rules = parsedRules.rules[0];
                });
            }

            this.rulesets = response.data;
        }).catch((reason: any) => {
            console.log("Error:" + reason);

        });
    }


    createRule() {
        if(this.rulesets) {
            this.rulesets = [...this.rulesets, rulesetModel]
        }

        // rest.api.RulesResource.createTenantRuleset(this.ruleset).then((response:any)=> {
        //     console.log(response);
        // });
    }

    updateRule(e:any) {
        this.ruleset = e.detail.rule;
        console.log(this.ruleset);
        // if(this.ruleset){
        //     this.ruleset.rules = JSON.stringify(this.rules);
        //     console.log(this.ruleset);
        //     rest.api.RulesResource.updateTenantRuleset(this.ruleset.id, this.ruleset).then((response:any)=> {
        //         console.log(response);
        //     });
        // }

    }

    deleteRule() {

    }

    setActiveRule(e:any) {
        this.ruleset = e.detail.rule;
        console.log(this.ruleset);
        this.requestUpdate();
    }
}

