import {css, html, LitElement, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import manager, {
    DefaultBoxShadow,
    DefaultColor1,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    DefaultColor6,
    Util
} from "@openremote/core";
import i18next from "i18next";
import "@openremote/or-icon";
import {
    AssetModelUtil,
    AssetDescriptor,
    AssetQuery,
    AssetTypeInfo,
    AttributeDescriptor,
    ClientRole,
    JsonRule,
    LogicGroup,
    NotificationTargetType,
    RuleActionUnion,
    RuleCondition,
    RulesetLang,
    RulesetUnion,
    WellknownAssets,
    Asset,
    AssetQueryOrderBy$Property,
    AssetQueryMatch
} from "@openremote/model";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-drawer";
import {translate} from "@openremote/or-translate";
import "./or-rule-list";
import "./or-rule-viewer";
import "./flow-viewer/flow-viewer";
import {OrRuleList} from "./or-rule-list";
import {OrRuleViewer} from "./or-rule-viewer";
import {RecurrenceOption} from "./json-viewer/or-rule-then-otherwise";
import {ValueInputProviderGenerator} from "@openremote/or-mwc-components/or-mwc-input";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";

export {buttonStyle} from "./style";

export const enum ConditionType {
    AGENT_QUERY = "agentQuery",
    ASSET_QUERY = "assetQuery",
    TIME = "time"
}

export const enum ActionType {
    WAIT = "wait",
    EMAIL = "email",
    PUSH_NOTIFICATION = "push",
    ATTRIBUTE = "attribute",
    WEBHOOK = "webhook"
}
export enum TimeTriggerType {
    TIME_OF_DAY = "TIME_OF_DAY"
}
export enum AssetQueryOperator {
    VALUE_EMPTY = "empty",
    VALUE_NOT_EMPTY = "notEmpty",
    EQUALS = "equals",
    NOT_EQUALS = "notEquals",
    GREATER_THAN = "greaterThan",
    GREATER_EQUALS = "greaterEquals",
    LESS_THAN = "lessThan",
    LESS_EQUALS = "lessEquals",
    BETWEEN = "between",
    NOT_BETWEEN = "notBetween",
    CONTAINS = "contains",
    NOT_CONTAINS = "notContains",
    STARTS_WITH = "startsWith",
    NOT_STARTS_WITH = "notStartsWith",
    ENDS_WITH = "endsWith",
    NOT_ENDS_WITH = "notEndsWith",
    CONTAINS_KEY = "containsKey",
    NOT_CONTAINS_KEY = "notContainsKey",
    INDEX_CONTAINS = "indexContains",
    NOT_INDEX_CONTAINS = "notIndexContains",
    LENGTH_EQUALS = "lengthEquals",
    NOT_LENGTH_EQUALS = "notLengthEquals",
    LENGTH_GREATER_THAN = "lengthGreaterThan",
    LENGTH_LESS_THAN = "lengthLessThan",
    IS_TRUE = "true",
    IS_FALSE = "false",
    WITHIN_RADIUS = "withinRadius",
    OUTSIDE_RADIUS = "outsideRadius",
    WITHIN_RECTANGLE = "withinRectangle",
    OUTSIDE_RECTANGLE = "outsideRectangle"
}

export interface AllowedActionTargetTypes {
    default?: NotificationTargetType[];
    actions?: {[actionType in ActionType]?: NotificationTargetType[]};
}

export interface RulesConfig {
    controls?: {
        allowedLanguages?: RulesetLang[];
        allowedConditionTypes?: ConditionType[];
        allowedActionTypes?: ActionType[];
        allowedAssetQueryOperators?: {[name: string]: AssetQueryOperator[]}; // name can be value descriptor name or value descriptor jsonType
        allowedRecurrenceOptions?: RecurrenceOption[];
        allowedActionTargetTypes?: AllowedActionTargetTypes;
        hideActionTypeOptions?: boolean;
        hideActionTargetOptions?: boolean;
        hideActionUpdateOptions?: boolean;
        hideConditionTypeOptions?: boolean;
        hideThenAddAction?: boolean;
        hideWhenAddCondition?: boolean;
        hideWhenAddAttribute?: boolean;
        hideWhenAddGroup?: boolean;
        multiSelect?: boolean;
    };
    inputProvider?: ValueInputProviderGenerator;
    descriptors?: {
        all?: RulesDescriptorSection;
        when?: RulesDescriptorSection;
        action?: RulesDescriptorSection;
    };
    rulesetTemplates?: {[key in RulesetLang]?: string};
    rulesetAddHandler?: (ruleset: RulesetUnion) => boolean;
    rulesetDeleteHandler?: (ruleset: RulesetUnion) => boolean;
    rulesetCopyHandler?: (ruleset: RulesetUnion) => boolean;
    rulesetSaveHandler?: (ruleset: RulesetUnion) => boolean;
    json?: {
        rule?: JsonRule;
        whenGroup?: LogicGroup<RuleCondition>;
        whenCondition?: RuleCondition;
        whenAssetQuery?: AssetQuery;
        then?: RuleActionUnion;
        otherwise?: RuleActionUnion;
    };
}

export interface RulesDescriptorSection {
    includeAssets?: string[];
    excludeAssets?: string[];
    attributeDescriptors?: {[attributeName: string]: RulesConfigAttribute };
    /**
     * Asset type specific config; '*' key will be used as a default fallback if no asset type specific entry exists
     */
    assets?: { [assetType: string]: RulesConfigAsset };
}

export interface RulesConfigAsset {
    includeAttributes?: string[];
    excludeAttributes?: string[];
    name?: string;
    icon?: string;
    color?: string;
    attributeDescriptors?: {[attributeName: string]: RulesConfigAttribute };
}

export interface RulesConfigAttribute extends AttributeDescriptor {
}

export interface RulesetNode {
    ruleset: RulesetUnion;
    selected: boolean;
}

export interface RequestEventDetail<T> {
    allow: boolean;
    detail: T;
}

export interface RuleView {
    validate: () => boolean;
    beforeSave: () => void;
    ruleset?: RulesetUnion;
    readonly?: boolean;
    config?: RulesConfig;
}

export interface RuleViewInfo {
    viewTemplateProvider: (ruleset:RulesetUnion, config: RulesConfig | undefined, readonly: boolean) => TemplateResult;
    viewRulesetTemplate?: string;
}

export const RuleViewInfoMap: {[key in RulesetLang]: RuleViewInfo} = {
    JSON: {
        viewTemplateProvider: (ruleset, config, readonly) => html`<or-rule-json-viewer id="rule-view" .ruleset="${ruleset}" .config="${config}" .readonly="${readonly}"></or-rule-json-viewer>`,
    },
    FLOW: {
        viewTemplateProvider: (ruleset, config, readonly) => html`<flow-editor id="rule-view" .ruleset="${ruleset}" .readonly="${readonly}"></flow-editor>`
    },
    GROOVY: {
        viewTemplateProvider: (ruleset, config, readonly) => html`
            <or-rule-text-viewer id="rule-view" .ruleset="${ruleset}" .config="${config}" .readonly="${readonly}"></or-rule-text-viewer>
        `,
        viewRulesetTemplate: "package demo.rules\n" +
            "\n" +
            "import org.openremote.manager.rules.RulesBuilder\n" +
            "import org.openremote.model.notification.*\n" +
            "import org.openremote.model.attribute.AttributeInfo\n" +
            "import org.openremote.model.asset.Asset\n" +
            "import org.openremote.model.asset.impl.*\n" +
            "import org.openremote.model.query.*\n" +
            "import org.openremote.model.query.filter.*\n" +
            "import org.openremote.model.rules.Assets\n" +
            "import org.openremote.model.rules.Notifications\n" +
            "import org.openremote.model.rules.Users\n" +
            "\n" +
            "import java.util.logging.Logger\n" +
            "import java.util.stream.Collectors\n" +
            "\n" +
            "Logger LOG = binding.LOG\n" +
            "RulesBuilder rules = binding.rules\n" +
            "Notifications notifications = binding.notifications\n" +
            "Users users = binding.users\n" +
            "Assets assets = binding.assets\n" +
            "\n" +
            "/*\n" +
            "* A groovy rule is made up of a when closure (LHS) which must return a boolean indicating whether the then closure (RHS)\n" +
            "* should be executed. The rule engine will periodically evaluate the when closure and if it evaluates to true then the\n" +
            "* rule then closure will execute.\n" +
            "*\n" +
            "* NOTE: DO NOT MODIFY THE FACTS IN THE WHEN CLOSURE THIS SHOULD BE DONE IN THE THEN CLOSURE\n" +
            "*\n" +
            "* To avoid an infinite rule loop the when closure should not continually return true for subsequent executions\n" +
            "* so either the then closure should perform an action that prevents the when closure from matching on subsequent\n" +
            "* evaluations, or custom facts should be used, some ideas:\n" +
            "*\n" +
            "* - Change the value of an attribute being matched in the when closure (which will prevent it matching on subsequent evaluations)\n" +
            "* - Insert a custom fact on first match and test this fact in the when closure to determine when the rule should match again (for\n" +
            "*   example if a rule should match whenever the asset state changes the asset state timestamp can be used)\n" +
            "*/\n" +
            "\n" +
            "rules.add()\n" +
            "        .name(\"Example rule\")\n" +
            "        .when({\n" +
            "    facts ->\n" +
            "\n" +
            "        // Find first matching asset state using an asset query\n" +
            "\n" +
            "        facts.matchFirstAssetState(\n" +
            "\n" +
            "                // Find asset state by asset type and attribute name\n" +
            "                new AssetQuery().types(ThingAsset).attributeNames(\"someAttribute\")\n" +
            "\n" +
            "                // Find asset state by asset ID and attribute name\n" +
            "                //new AssetQuery().ids(\"7CaBoyiDhtdf2kn1Xso1w5\").attributeNames(\"someAttribute\")\n" +
            "\n" +
            "                // Find asset state by asset type, attribute name and value string predicate\n" +
            "                //new AssetQuery().types(ThingAsset).attributes(\n" +
            "                //        new AttributePredicate()\n" +
            "                //                .name(\"someAttribute\")\n" +
            "                //                .value(new StringPredicate()\n" +
            "                //                            .value(\"someValue\")\n" +
            "                //                            .match(AssetQuery.Match.EXACT)\n" +
            "                //                            .caseSensitive(true)))\n" +
            "\n" +
            "                // Find asset state by asset type and location attribute predicate\n" +
            "                //new AssetQuery().types(ThingAsset).attributes(\n" +
            "                //        new AttributePredicate()\n" +
            "                //                .name(Asset.LOCATION)\n" +
            "                //                .value(new RadialGeofencePredicate()\n" +
            "                //                            .radius(100)\n" +
            "                //                            .lat(50.0)\n" +
            "                //                            .lng(0.0)))\n" +
            "\n" +
            "        ).map { assetState ->\n" +
            "\n" +
            "            // Use logging to help with debugging if needed" +
            "            //LOG.info(\"ATTRIBUTE FOUND\")\n" +
            "\n" +
            "            // Check if this rule really should fire this time\n" +
            "            Optional<Long> lastFireTimestamp = facts.getOptional(\"someAttribute\")\n" +
            "            if (lastFireTimestamp.isPresent() && assetState.getTimestamp() <= lastFireTimestamp.get()) {\n" +
            "                return false\n" +
            "            }\n" +
            "\n" +
            "            // OK to fire if we reach here\n" +
            "\n" +
            "            // Compute and bind any facts required for the then closure\n" +
            "            facts.bind(\"assetState\", assetState)\n" +
            "            true\n" +
            "        }.orElseGet {\n" +
            "            // Asset state didn't match so clear out any custom facts to allow the rule to fire next time the when closure matches\n" +
            "            facts.remove(\"someAttribute\")\n" +
            "            false\n" +
            "        }\n" +
            "\n" +
            "})\n" +
            "        .then({\n" +
            "    facts ->\n" +
            "\n" +
            "        // Extract any binded facts\n" +
            "        AttributeInfo assetState = facts.bound(\"assetState\")\n" +
            "\n" +
            "        // Insert the custom fact to prevent rule loop\n" +
            "        facts.put(\"someAttribute\", assetState.getTimestamp())\n" +
            "\n" +
            "        // Write to attributes\n" +
            "        def otherAttributeValue = null\n" +
            "        if (assetState.getValue().orElse{null} == \"Value 1\") {\n" +
            "            otherAttributeValue = \"Converted Value 1\"\n" +
            "        } else if (assetState.getValue().orElse{null} == \"Value 2\") {\n" +
            "            otherAttributeValue = \"Converted Value 2\"\n" +
            "        } else {\n" +
            "            otherAttributeValue = \"Unknown\"\n" +
            "        }\n" +
            "        assets.dispatch(assetState.id, \"otherAttribute\", otherAttributeValue)\n" +
            "\n" +
            "        // Generate notifications (useful for rules that check if an attribute is out of range)\n" +
            "        //notifications.send(new Notification()\n" +
            "        //        .setName(\"Attribute alert\")\n" +
            "        //        .setMessage(new EmailNotificationMessage()\n" +
            "        //                .setTo(\"no-reply@openremote.io\")\n" +
            "        //                .setSubject(\"Attribute out of range: Attribute=${assetState.name} Asset ID=${assetState.id}\")\n" +
            "        //                .setText(\"Some text body\")\n" +
            "        //                .setHtml(\"<p>Or some HTML body</p>\")\n" +
            "        //        )\n" +
            "        //)\n" +
            "})"
    },
    JAVASCRIPT: {
        viewTemplateProvider: (ruleset, config, readonly) => html`
            <or-rule-text-viewer id="rule-view" .ruleset="${ruleset}" .config="${config}" .readonly="${readonly}"></or-rule-text-viewer>
        `,
        viewRulesetTemplate: "rules = [ // An array of rules, add more objects to add more rules\n" +
            "    {\n" +
            "        name: \"Set bar to foo on someAttribute\",\n" +
            "        description: \"An example rule that sets 'bar' on someAttribute when it is 'foo'\",\n" +
            "        when: function(facts) {\n" +
            "            return facts.matchAssetState(\n" +
            "                new AssetQuery().types(AssetType.THING).attributeValue(\"someAttribute\", \"foo\")\n" +
            "            ).map(function(thing) {\n" +
            "                facts.bind(\"assetId\", thing.id);\n" +
            "                return true;\n" +
            "            }).orElse(false);\n" +
            "        },\n" +
            "        then: function(facts) {\n" +
            "            facts.updateAssetState(\n" +
            "                facts.bound(\"assetId\"), \"someAttribute\", \"bar\"\n" +
            "            );\n" +
            "        }\n" +
            "    }\n" +
            "]"
    }
}

function getAssetDescriptorFromSection(assetType: string, config: RulesConfig | undefined, useActionConfig: boolean): RulesConfigAsset | undefined {
    if (!config || !config.descriptors) {
        return;
    }

    const section = useActionConfig ? config.descriptors.action : config.descriptors.when;
    const allSection = config.descriptors.all;

    const descriptor = section && section.assets ? section.assets[assetType] ? section.assets[assetType] : section.assets["*"] : undefined;
    if (descriptor) {
        return descriptor;
    }

    return allSection && allSection.assets ? allSection.assets[assetType] ? allSection.assets[assetType] : allSection.assets["*"] : undefined;
}

export function getAssetTypeFromQuery(query?: AssetQuery): string | undefined {
    return query && query.types && query.types.length > 0 && query.types[0] ? query.types[0] : undefined;
}

export function getAssetIdsFromQuery(query?: AssetQuery) {
    return query && query.ids ? [...query.ids] : undefined;
}

export const getAssetTypes: () => Promise<string[]> = async () => {
    // RT: Change to just get all asset types for now as if an instance of a particular asset doesn't exist you
    // won't be able to create a rule for it (e.g. if no ConsoleAsset in a realm then cannot create a console rule)
    return AssetModelUtil.getAssetTypeInfos().map(ati => ati.assetDescriptor!.name!);
    // const response = await manager.rest.api.AssetResource.queryAssets({
    //     select: {
    //         attributes: []
    //     },
    //     realm: {
    //         realm: manager.displayRealm
    //     },
    //     recursive: true
    // });
    //
    // if (response && response.data) {
    //     return response.data.map(asset => asset.type!);
    // }
}

export function getAssetInfos(config: RulesConfig | undefined, useActionConfig: boolean): Promise<AssetTypeInfo[]> {
    const assetDescriptors: AssetDescriptor[] = AssetModelUtil.getAssetDescriptors();

    return getAssetTypes().then(availibleAssetTypes => {
        let allowedAssetTypes: string[] = availibleAssetTypes ? availibleAssetTypes : [];
        let excludedAssetTypes: string[] = [];
        if (!config || !config.descriptors) {
            return assetDescriptors.map((ad) => AssetModelUtil.getAssetTypeInfo(ad)!);
        }

        const section = useActionConfig ? config.descriptors.action : config.descriptors.when;

        if ((section && section.includeAssets) || (config.descriptors.all && config.descriptors.all.includeAssets)) {
            allowedAssetTypes = [];

            if (section && section.includeAssets) {
                allowedAssetTypes = [...section.includeAssets];
            }

            if (config.descriptors.all && config.descriptors.all.includeAssets) {
                allowedAssetTypes = [...config.descriptors.all.includeAssets];
            }
        }

        if (section && section.excludeAssets) {
            excludedAssetTypes = [...section.excludeAssets];
        }
        if (config.descriptors.all && config.descriptors.all.excludeAssets) {
            excludedAssetTypes = excludedAssetTypes.concat(config.descriptors.all.excludeAssets);
        }

        return assetDescriptors.filter((ad) => {
            if (allowedAssetTypes.length > 0 && allowedAssetTypes.indexOf(ad.name!) < 0) {
                return false;
            }
            return excludedAssetTypes.indexOf(ad.name!) < 0;

        }).map((ad) => {

            let typeInfo = AssetModelUtil.getAssetTypeInfo(ad)!;

            // Amalgamate matching descriptor from config if defined
            const configDescriptor = getAssetDescriptorFromSection(ad.name!, config, useActionConfig);
            if (!configDescriptor) {
                return typeInfo;
            }

            const modifiedTypeInfo: AssetTypeInfo = {
                assetDescriptor: typeInfo.assetDescriptor ? {...typeInfo.assetDescriptor} : {descriptorType: "asset"},
                attributeDescriptors: typeInfo.attributeDescriptors ? [...typeInfo.attributeDescriptors] : [],
                metaItemDescriptors: typeInfo.metaItemDescriptors ? [...typeInfo.metaItemDescriptors] : [],
                valueDescriptors: typeInfo.valueDescriptors ? [...typeInfo.valueDescriptors] : []
            };

            if (configDescriptor.icon) {
                modifiedTypeInfo.assetDescriptor!.icon = configDescriptor.icon;
            }
            if (configDescriptor.color) {
                modifiedTypeInfo.assetDescriptor!.colour = configDescriptor.color;
            }

            // Remove any excluded attributes
            if (modifiedTypeInfo.attributeDescriptors) {
                const includedAttributes = configDescriptor.includeAttributes !== undefined ? configDescriptor.includeAttributes : undefined;
                const excludedAttributes = configDescriptor.excludeAttributes !== undefined ? configDescriptor.excludeAttributes : undefined;

                if (includedAttributes || excludedAttributes) {
                    modifiedTypeInfo.attributeDescriptors = modifiedTypeInfo.attributeDescriptors.filter((mad) =>
                        (!includedAttributes || includedAttributes.some((inc) => Util.stringMatch(inc,  mad.name!)))
                        && (!excludedAttributes || !excludedAttributes.some((exc) => Util.stringMatch(exc,  mad.name!))));
                }

                // Override any attribute descriptors
                if (configDescriptor.attributeDescriptors) {
                    modifiedTypeInfo.attributeDescriptors.map((attributeDescriptor) => {
                        let configAttributeDescriptor: RulesConfigAttribute | undefined = configDescriptor.attributeDescriptors![attributeDescriptor.name!];
                        if (!configAttributeDescriptor) {
                            configAttributeDescriptor = section && section.attributeDescriptors ? section.attributeDescriptors[attributeDescriptor.name!] : undefined;
                        }
                        if (!configAttributeDescriptor) {
                            configAttributeDescriptor = config.descriptors!.all && config.descriptors!.all.attributeDescriptors ? config.descriptors!.all.attributeDescriptors[attributeDescriptor.name!] : undefined;
                        }
                        if (configAttributeDescriptor) {
                            if (configAttributeDescriptor.type) {
                                attributeDescriptor.type = configAttributeDescriptor.type;
                            }
                            if (configAttributeDescriptor.format) {
                                attributeDescriptor.format = configAttributeDescriptor.format;
                            }
                            if (configAttributeDescriptor.units) {
                                attributeDescriptor.units = configAttributeDescriptor.units;
                            }
                            if (configAttributeDescriptor.constraints) {
                                attributeDescriptor.constraints = attributeDescriptor.constraints ? [...configAttributeDescriptor.constraints,...attributeDescriptor.constraints] : configAttributeDescriptor.constraints;
                            }
                        }
                    });
                }
            }
            return modifiedTypeInfo;
        });

    });
}

// Function for getting assets by type
// loadedAssets is an object given as parameter that will be updated if new assets are fetched.
export async function getAssetsByType(type: string, realm?: string, loadedAssets?: Map<string, Asset[]>): Promise<{ assets?: Asset[], loadedAssets?: Map<string, Asset[]>}> {
    if(loadedAssets?.has(type)) {
        return {
            assets: loadedAssets?.get(type),
            loadedAssets: loadedAssets
        }
    } else {
        if(!loadedAssets) {
            loadedAssets = new Map<string, any[]>();
        }
        const assetQuery: AssetQuery = {
            types: [type],
            orderBy: {
                property: AssetQueryOrderBy$Property.NAME
            }
        }
        if(realm != undefined) {
            assetQuery.realm = { name: realm }
        }
        const response = await manager.rest.api.AssetResource.queryAssets(assetQuery);
        loadedAssets.set(type, response.data);
        return {
            assets: response.data,
            loadedAssets: loadedAssets
        };
    }

}

export class OrRulesRuleChangedEvent extends CustomEvent<boolean> {

    public static readonly NAME = "or-rules-rule-changed";

    constructor(valid: boolean) {
        super(OrRulesRuleChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: valid
        });
    }
}

export class OrRulesRuleUnsupportedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-rule-unsupported";

    constructor() {
        super(OrRulesRuleUnsupportedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export interface NodeSelectEventDetail {
    oldNodes: RulesetNode[];
    newNodes: RulesetNode[];
}

export class OrRulesRequestSelectionEvent extends CustomEvent<RequestEventDetail<NodeSelectEventDetail>> {

    public static readonly NAME = "or-rules-request-selection";

    constructor(request: NodeSelectEventDetail) {
        super(OrRulesRequestSelectionEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                detail: request,
                allow: true
            }
        });
    }
}

export class OrRulesSelectionEvent extends CustomEvent<NodeSelectEventDetail> {

    public static readonly NAME = "or-rules-selection";

    constructor(nodes: NodeSelectEventDetail) {
        super(OrRulesSelectionEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: nodes
        });
    }
}

export type AddEventDetail = {
    ruleset: RulesetUnion;
    sourceRuleset?: RulesetUnion;
}

export class OrRulesRequestAddEvent extends CustomEvent<RequestEventDetail<AddEventDetail>> {

    public static readonly NAME = "or-rules-request-add";

    constructor(detail: AddEventDetail) {
        super(OrRulesRequestAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                detail: detail,
                allow: true
            }
        });
    }
}

export class OrRulesAddEvent extends CustomEvent<AddEventDetail> {

    public static readonly NAME = "or-rules-add";

    constructor(detail: AddEventDetail) {
        super(OrRulesAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}

export class OrRulesRequestDeleteEvent extends CustomEvent<RequestEventDetail<RulesetNode[]>> {

    public static readonly NAME = "or-rules-request-delete";

    constructor(request: RulesetNode[]) {
        super(OrRulesRequestDeleteEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                detail: request,
                allow: true
            }
        });
    }
}

export type SaveResult = {
    success: boolean,
    ruleset: RulesetUnion,
    isNew: boolean
};

export class OrRulesRequestSaveEvent extends CustomEvent<RequestEventDetail<RulesetUnion>> {

    public static readonly NAME = "or-rules-request-save";

    constructor(ruleset: RulesetUnion) {
        super(OrRulesRequestSaveEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: ruleset
            }
        });
    }
}

export class OrRulesSaveEvent extends CustomEvent<SaveResult> {

    public static readonly NAME = "or-rules-save";

    constructor(result: SaveResult) {
        super(OrRulesSaveEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: result
        });
    }
}

export class OrRulesDeleteEvent extends CustomEvent<RulesetUnion[]> {

    public static readonly NAME = "or-rules-delete";

    constructor(rulesets: RulesetUnion[]) {
        super(OrRulesDeleteEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: rulesets
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrRulesRuleUnsupportedEvent.NAME]: OrRulesRuleUnsupportedEvent;
        [OrRulesRuleChangedEvent.NAME]: OrRulesRuleChangedEvent;
        [OrRulesRequestSelectionEvent.NAME]: OrRulesRequestSelectionEvent;
        [OrRulesSelectionEvent.NAME]: OrRulesSelectionEvent;
        [OrRulesRequestAddEvent.NAME]: OrRulesRequestAddEvent;
        [OrRulesAddEvent.NAME]: OrRulesAddEvent;
        [OrRulesRequestDeleteEvent.NAME]: OrRulesRequestDeleteEvent;
        [OrRulesRequestSaveEvent.NAME]: OrRulesRequestSaveEvent;
        [OrRulesSaveEvent.NAME]: OrRulesSaveEvent;
        [OrRulesDeleteEvent.NAME]: OrRulesDeleteEvent;
    }
}

// language=CSS
export const style = css`

    :host {
        display: flex;
        height: 100%;
        width: 100%;
        
        --internal-or-rules-background-color: var(--or-rules-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-rules-text-color: var(--or-rules-text-color, inherit);
        --internal-or-rules-button-color: var(--or-rules-button-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-rules-invalid-color: var(--or-rules-invalid-color, var(--or-app-color6, ${unsafeCSS(DefaultColor6)}));        
        --internal-or-rules-panel-color: var(--or-rules-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-rules-line-color: var(--or-rules-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        
        --internal-or-rules-list-selected-color: var(--or-rules-list-selected-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-rules-list-text-color: var(--or-rules-list-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-rules-list-text-size: var(--or-rules-list-text-size, 15px);
        --internal-or-rules-list-header-height: var(--or-rules-list-header-height, 48px);

        --internal-or-rules-list-icon-color-error: var(--or-rules-list-icon-color-error, var(--or-app-color6, ${unsafeCSS(DefaultColor6)}));
        --internal-or-rules-list-icon-color-ok: var(--or-rules-list-icon-color-ok, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));

        --internal-or-rules-list-button-size: var(--or-rules-list-button-size, 24px);
        
        --internal-or-rules-header-background-color: var(--or-rules-header-background-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-rules-header-height: var(--or-rules-header-height, unset);
        
        --or-panel-background-color: var(--internal-or-rules-panel-color);
    }

    or-rule-list {
        min-width: 300px;
        width: 300px;
        z-index: 2;
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-rules-panel-color);
        color: var(--internal-or-rules-list-text-color);
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
    }
    
    or-rule-viewer {
        z-index: 1;    
    }
`;

@customElement("or-rules")
export class OrRules extends translate(i18next)(LitElement) {

    public static DEFAULT_RULESET_NAME = "";

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Boolean})
    public readonly?: boolean;

    @property({type: Object})
    public config?: RulesConfig;

    @property({type: String})
    public realm?: string;

    @property({type: String})
    public language?: RulesetLang;

    @property({type: Array})
    public selectedIds?: number[];

    @property({attribute: false})
    private _isValidRule?: boolean;

    @query("#rule-list")
    private _rulesList!: OrRuleList;

    @query("#rule-viewer")
    private _viewer!: OrRuleViewer;

    constructor() {
        super();

        this.addEventListener(OrRulesRequestSelectionEvent.NAME, this._onRuleSelectionRequested);
        this.addEventListener(OrRulesSelectionEvent.NAME, this._onRuleSelectionChanged);
        this.addEventListener(OrRulesAddEvent.NAME, this._onRuleAdd);
        this.addEventListener(OrRulesSaveEvent.NAME, this._onRuleSave);
    }

    protected render() {

        return html`
            <or-rule-list id="rule-list" .config="${this.config}" .language="${this.language}" .selectedIds="${this.selectedIds}"></or-rule-list>
            <or-rule-viewer id="rule-viewer" .config="${this.config}" .readonly="${this.isReadonly()}"></or-rule-viewer>
        `;
    }

    public refresh() {
        this._viewer.ruleset = undefined;
        this._rulesList.refresh();
    }

    protected isReadonly(): boolean {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_RULES);
    }

    protected _confirmContinue(action: () => void) {
        if (this._viewer.modified) {
            showOkCancelDialog(i18next.t("loseChanges"), i18next.t("confirmContinueRulesetModified"), i18next.t("discard"))
                .then((ok) => {
                    if (ok) {
                        action();
                    }
                });
        } else {
            action();
        }
    }

    protected _onRuleSelectionRequested(event: OrRulesRequestSelectionEvent) {
        const isModified = this._viewer.modified;

        if (!isModified) {
            return;
        }

        // Prevent the request and check if user wants to lose changes
        event.detail.allow = false;

        this._confirmContinue(() => {
            const nodes = event.detail.detail.newNodes;
            if (Util.objectsEqual(nodes, event.detail.detail.oldNodes)) {
                // User has clicked the same node so let's force reload it
                this._viewer.ruleset =  {...nodes[0].ruleset};
            } else {
                this.selectedIds = nodes.map((node) => node.ruleset.id!);
                this._viewer.ruleset = nodes.length === 1 ? nodes[0].ruleset : undefined;
            }
        });
    }

    protected _onRuleSelectionChanged(event: OrRulesSelectionEvent) {
        const nodes = event.detail.newNodes;
        this.selectedIds = nodes.map((node) => node.ruleset.id!);
        this._viewer.ruleset = nodes.length === 1 ? {...nodes[0].ruleset} : undefined;
    }

    protected _onRuleAdd(event: OrRulesAddEvent) {
        // Load the ruleset into the viewer
        this._viewer.ruleset = event.detail.ruleset;
    }

    protected async _onRuleSave(event: OrRulesSaveEvent) {
        await this._rulesList.refresh();
        if (event.detail.success && event.detail.isNew) {
            this.selectedIds = [event.detail.ruleset.id!];
        }
    }
}
