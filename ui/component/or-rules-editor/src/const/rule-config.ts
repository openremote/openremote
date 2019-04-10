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

export const rulesEditorConfig = {
    controls: {
        addWhenCondition: true,
        addThenCondition: false,
    },
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

export const ruleTemplate: Rule = {
    name: "",
    when: undefined,
    then: undefined
};

export const rulesetTemplate: TenantRuleset = {
    name: "New Rule",
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