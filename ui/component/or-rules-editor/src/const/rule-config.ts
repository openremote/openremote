import openremote from "@openremote/core";
import {
    Rule,
    RulesetLang,
    TenantRuleset
} from "@openremote/model";

export const ruleModel: Rule = {
    name: "",
    when: undefined,
    then: undefined
};

export const rulesetModel: TenantRuleset = {
    name: "New Rule",
    type: "tenant",
    lang: RulesetLang.JSON,
    realm: openremote.getRealm(),
    accessPublicRead: true,
    rules: JSON.stringify({rules: [ruleModel]})
};

export const rulesEditorConfig = {
    options: {
        attributeValueDescriptors: {
            flightProfiles: {},
            airportIata: {},
            airlineIata: {},
            originRegion: {},
            passengerCapacity: {},
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
            }
        }
    }
};
