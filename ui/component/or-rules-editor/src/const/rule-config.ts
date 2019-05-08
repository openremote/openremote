import openremote from "@openremote/core";
import {
    Rule,
    RulesetLang,
    TenantRuleset,
    NewAssetQuery,
    BaseAssetQueryMatch,
    RuleActionUnion,
    RuleActionUpdateAttributeUpdateAction,
    RuleTrigger,
    AttributePredicate
} from "@openremote/model";
import {airlines, airports, countries, languages, regions} from "./resources";

export const rulesEditorConfig = {
    controls: {
        addWhenCondition: true,
        addThenCondition: false,
    },
    options: {
        when: {
            attributeValueDescriptors: {
                airportIata: {
                    options: airports
                },
                airlineIata: {
                    options: airlines
                },
                originRegion: {
                    options: regions
                },
                passengerCapacity: {},
                languageCodes: {
                    options: languages
                },
                countryCode: {
                    options: countries
                }
            }
        },
        then: {
            attributeValueDescriptors: {
                flightProfiles: {}
            }
        }
    }
};

export const ruleTemplate: Rule = {
    name: "",
    when: undefined,
    then: undefined
};

export const rulesetTemplate: TenantRuleset = {
    name: "Nieuw Profiel",
    type: "tenant",
    lang: RulesetLang.JSON,
    realm: openremote.getRealm(),
    accessPublicRead: true,
    rules: JSON.stringify({rules: [ruleTemplate]})
};


export const defaultAssetType: NewAssetQuery = {
    types: [{
        predicateType: "string",
        match: BaseAssetQueryMatch.EXACT,
        value: "urn:openremote:asset:kmar:flight"
    }]
};

export const defaultThenCondition: RuleActionUnion = {
    action: "update-attribute",
    updateAction: RuleActionUpdateAttributeUpdateAction.ADD_OR_REPLACE,
    attributeName: "flightProfiles",
    key: "%RULESET_ID%",
    value:  {
        profileName: "%RULESET_NAME%",
        profileColor: "orange"
    },
    target: { useAssetsFromWhen: true}
};

export const defaultThen: RuleActionUnion[] = [defaultThenCondition];

export const defaultWhenCondition: RuleTrigger = {
    asset: {
        types: [{
            predicateType: "string",
            match: BaseAssetQueryMatch.EXACT,
            value: "urn:openremote:asset:kmar:flight"
        }
        ],
        attributes: {
            predicates: []
        }
    }
};

export const defaultPredicate: AttributePredicate = {
    name: {
        predicateType: "string",
        match: BaseAssetQueryMatch.EXACT,
        value: "airlineIata"
    },
    value: {
        predicateType: "string",
        match: BaseAssetQueryMatch.EXACT
    }

};