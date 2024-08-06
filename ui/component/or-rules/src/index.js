var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import manager, { DefaultBoxShadow, DefaultColor1, DefaultColor2, DefaultColor3, DefaultColor4, DefaultColor5, DefaultColor6, Util } from "@openremote/core";
import i18next from "i18next";
import "@openremote/or-icon";
import { AssetModelUtil } from "@openremote/model";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-drawer";
import { translate } from "@openremote/or-translate";
import "./or-rule-list";
import "./or-rule-viewer";
import "./flow-viewer/flow-viewer";
import { showOkCancelDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
export { buttonStyle } from "./style";
export var TimeTriggerType;
(function (TimeTriggerType) {
    TimeTriggerType["TIME_OF_DAY"] = "TIME_OF_DAY";
})(TimeTriggerType || (TimeTriggerType = {}));
export var AssetQueryOperator;
(function (AssetQueryOperator) {
    AssetQueryOperator["VALUE_EMPTY"] = "empty";
    AssetQueryOperator["VALUE_NOT_EMPTY"] = "notEmpty";
    AssetQueryOperator["EQUALS"] = "equals";
    AssetQueryOperator["NOT_EQUALS"] = "notEquals";
    AssetQueryOperator["GREATER_THAN"] = "greaterThan";
    AssetQueryOperator["GREATER_EQUALS"] = "greaterEquals";
    AssetQueryOperator["LESS_THAN"] = "lessThan";
    AssetQueryOperator["LESS_EQUALS"] = "lessEquals";
    AssetQueryOperator["BETWEEN"] = "between";
    AssetQueryOperator["NOT_BETWEEN"] = "notBetween";
    AssetQueryOperator["CONTAINS"] = "contains";
    AssetQueryOperator["NOT_CONTAINS"] = "notContains";
    AssetQueryOperator["STARTS_WITH"] = "startsWith";
    AssetQueryOperator["NOT_STARTS_WITH"] = "notStartsWith";
    AssetQueryOperator["ENDS_WITH"] = "endsWith";
    AssetQueryOperator["NOT_ENDS_WITH"] = "notEndsWith";
    AssetQueryOperator["CONTAINS_KEY"] = "containsKey";
    AssetQueryOperator["NOT_CONTAINS_KEY"] = "notContainsKey";
    AssetQueryOperator["INDEX_CONTAINS"] = "indexContains";
    AssetQueryOperator["NOT_INDEX_CONTAINS"] = "notIndexContains";
    AssetQueryOperator["LENGTH_EQUALS"] = "lengthEquals";
    AssetQueryOperator["NOT_LENGTH_EQUALS"] = "notLengthEquals";
    AssetQueryOperator["LENGTH_GREATER_THAN"] = "lengthGreaterThan";
    AssetQueryOperator["LENGTH_LESS_THAN"] = "lengthLessThan";
    AssetQueryOperator["IS_TRUE"] = "true";
    AssetQueryOperator["IS_FALSE"] = "false";
    AssetQueryOperator["WITHIN_RADIUS"] = "withinRadius";
    AssetQueryOperator["OUTSIDE_RADIUS"] = "outsideRadius";
    AssetQueryOperator["WITHIN_RECTANGLE"] = "withinRectangle";
    AssetQueryOperator["OUTSIDE_RECTANGLE"] = "outsideRectangle";
})(AssetQueryOperator || (AssetQueryOperator = {}));
export const RuleViewInfoMap = {
    JSON: {
        viewTemplateProvider: (ruleset, config, readonly) => html `<or-rule-json-viewer id="rule-view" .ruleset="${ruleset}" .config="${config}" .readonly="${readonly}"></or-rule-json-viewer>`,
    },
    FLOW: {
        viewTemplateProvider: (ruleset, config, readonly) => html `<flow-editor id="rule-view" .ruleset="${ruleset}" .readonly="${readonly}"></flow-editor>`
    },
    GROOVY: {
        viewTemplateProvider: (ruleset, config, readonly) => html `
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
        viewTemplateProvider: (ruleset, config, readonly) => html `
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
};
function getAssetDescriptorFromSection(assetType, config, useActionConfig) {
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
export function getAssetTypeFromQuery(query) {
    return query && query.types && query.types.length > 0 && query.types[0] ? query.types[0] : undefined;
}
export function getAssetIdsFromQuery(query) {
    return query && query.ids ? [...query.ids] : undefined;
}
export const getAssetTypes = () => __awaiter(void 0, void 0, void 0, function* () {
    // RT: Change to just get all asset types for now as if an instance of a particular asset doesn't exist you
    // won't be able to create a rule for it (e.g. if no ConsoleAsset in a realm then cannot create a console rule)
    return AssetModelUtil.getAssetTypeInfos().map(ati => ati.assetDescriptor.name);
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
});
export function getAssetInfos(config, useActionConfig) {
    const assetDescriptors = AssetModelUtil.getAssetDescriptors();
    return getAssetTypes().then(availibleAssetTypes => {
        let allowedAssetTypes = availibleAssetTypes ? availibleAssetTypes : [];
        let excludedAssetTypes = [];
        if (!config || !config.descriptors) {
            return assetDescriptors.map((ad) => AssetModelUtil.getAssetTypeInfo(ad));
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
            if (allowedAssetTypes.length > 0 && allowedAssetTypes.indexOf(ad.name) < 0) {
                return false;
            }
            return excludedAssetTypes.indexOf(ad.name) < 0;
        }).map((ad) => {
            let typeInfo = AssetModelUtil.getAssetTypeInfo(ad);
            // Amalgamate matching descriptor from config if defined
            const configDescriptor = getAssetDescriptorFromSection(ad.name, config, useActionConfig);
            if (!configDescriptor) {
                return typeInfo;
            }
            const modifiedTypeInfo = {
                assetDescriptor: typeInfo.assetDescriptor ? Object.assign({}, typeInfo.assetDescriptor) : { descriptorType: "asset" },
                attributeDescriptors: typeInfo.attributeDescriptors ? [...typeInfo.attributeDescriptors] : [],
                metaItemDescriptors: typeInfo.metaItemDescriptors ? [...typeInfo.metaItemDescriptors] : [],
                valueDescriptors: typeInfo.valueDescriptors ? [...typeInfo.valueDescriptors] : []
            };
            if (configDescriptor.icon) {
                modifiedTypeInfo.assetDescriptor.icon = configDescriptor.icon;
            }
            if (configDescriptor.color) {
                modifiedTypeInfo.assetDescriptor.colour = configDescriptor.color;
            }
            // Remove any excluded attributes
            if (modifiedTypeInfo.attributeDescriptors) {
                const includedAttributes = configDescriptor.includeAttributes !== undefined ? configDescriptor.includeAttributes : undefined;
                const excludedAttributes = configDescriptor.excludeAttributes !== undefined ? configDescriptor.excludeAttributes : undefined;
                if (includedAttributes || excludedAttributes) {
                    modifiedTypeInfo.attributeDescriptors = modifiedTypeInfo.attributeDescriptors.filter((mad) => (!includedAttributes || includedAttributes.some((inc) => Util.stringMatch(inc, mad.name)))
                        && (!excludedAttributes || !excludedAttributes.some((exc) => Util.stringMatch(exc, mad.name))));
                }
                // Override any attribute descriptors
                if (configDescriptor.attributeDescriptors) {
                    modifiedTypeInfo.attributeDescriptors.map((attributeDescriptor) => {
                        let configAttributeDescriptor = configDescriptor.attributeDescriptors[attributeDescriptor.name];
                        if (!configAttributeDescriptor) {
                            configAttributeDescriptor = section && section.attributeDescriptors ? section.attributeDescriptors[attributeDescriptor.name] : undefined;
                        }
                        if (!configAttributeDescriptor) {
                            configAttributeDescriptor = config.descriptors.all && config.descriptors.all.attributeDescriptors ? config.descriptors.all.attributeDescriptors[attributeDescriptor.name] : undefined;
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
                                attributeDescriptor.constraints = attributeDescriptor.constraints ? [...configAttributeDescriptor.constraints, ...attributeDescriptor.constraints] : configAttributeDescriptor.constraints;
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
export function getAssetsByType(type, realm, loadedAssets) {
    return __awaiter(this, void 0, void 0, function* () {
        if (loadedAssets === null || loadedAssets === void 0 ? void 0 : loadedAssets.has(type)) {
            return {
                assets: loadedAssets === null || loadedAssets === void 0 ? void 0 : loadedAssets.get(type),
                loadedAssets: loadedAssets
            };
        }
        else {
            if (!loadedAssets) {
                loadedAssets = new Map();
            }
            const assetQuery = {
                types: [type],
                orderBy: {
                    property: "NAME" /* AssetQueryOrderBy$Property.NAME */
                }
            };
            if (realm != undefined) {
                assetQuery.realm = { name: realm };
            }
            const response = yield manager.rest.api.AssetResource.queryAssets(assetQuery);
            loadedAssets.set(type, response.data);
            return {
                assets: response.data,
                loadedAssets: loadedAssets
            };
        }
    });
}
export class OrRulesRuleChangedEvent extends CustomEvent {
    constructor(valid) {
        super(OrRulesRuleChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: valid
        });
    }
}
OrRulesRuleChangedEvent.NAME = "or-rules-rule-changed";
export class OrRulesRuleUnsupportedEvent extends CustomEvent {
    constructor() {
        super(OrRulesRuleUnsupportedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrRulesRuleUnsupportedEvent.NAME = "or-rules-rule-unsupported";
export class OrRulesRequestSelectionEvent extends CustomEvent {
    constructor(request) {
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
OrRulesRequestSelectionEvent.NAME = "or-rules-request-selection";
export class OrRulesSelectionEvent extends CustomEvent {
    constructor(nodes) {
        super(OrRulesSelectionEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: nodes
        });
    }
}
OrRulesSelectionEvent.NAME = "or-rules-selection";
export class OrRulesRequestAddEvent extends CustomEvent {
    constructor(detail) {
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
OrRulesRequestAddEvent.NAME = "or-rules-request-add";
export class OrRulesAddEvent extends CustomEvent {
    constructor(detail) {
        super(OrRulesAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}
OrRulesAddEvent.NAME = "or-rules-add";
export class OrRulesRequestDeleteEvent extends CustomEvent {
    constructor(request) {
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
OrRulesRequestDeleteEvent.NAME = "or-rules-request-delete";
export class OrRulesRequestSaveEvent extends CustomEvent {
    constructor(ruleset) {
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
OrRulesRequestSaveEvent.NAME = "or-rules-request-save";
export class OrRulesSaveEvent extends CustomEvent {
    constructor(result) {
        super(OrRulesSaveEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: result
        });
    }
}
OrRulesSaveEvent.NAME = "or-rules-save";
export class OrRulesDeleteEvent extends CustomEvent {
    constructor(rulesets) {
        super(OrRulesDeleteEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: rulesets
        });
    }
}
OrRulesDeleteEvent.NAME = "or-rules-delete";
// language=CSS
export const style = css `

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
let OrRules = class OrRules extends translate(i18next)(LitElement) {
    static get styles() {
        return [
            style
        ];
    }
    constructor() {
        super();
        this.addEventListener(OrRulesRequestSelectionEvent.NAME, this._onRuleSelectionRequested);
        this.addEventListener(OrRulesSelectionEvent.NAME, this._onRuleSelectionChanged);
        this.addEventListener(OrRulesAddEvent.NAME, this._onRuleAdd);
        this.addEventListener(OrRulesSaveEvent.NAME, this._onRuleSave);
    }
    render() {
        return html `
            <or-rule-list id="rule-list" .config="${this.config}" .language="${this.language}" .selectedIds="${this.selectedIds}"></or-rule-list>
            <or-rule-viewer id="rule-viewer" .config="${this.config}" .readonly="${this.isReadonly()}"></or-rule-viewer>
        `;
    }
    refresh() {
        this._viewer.ruleset = undefined;
        this._rulesList.refresh();
    }
    isReadonly() {
        return this.readonly || !manager.hasRole("write:rules" /* ClientRole.WRITE_RULES */);
    }
    _confirmContinue(action) {
        if (this._viewer.modified) {
            showOkCancelDialog(i18next.t("loseChanges"), i18next.t("confirmContinueRulesetModified"), i18next.t("discard"))
                .then((ok) => {
                if (ok) {
                    action();
                }
            });
        }
        else {
            action();
        }
    }
    _onRuleSelectionRequested(event) {
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
                this._viewer.ruleset = Object.assign({}, nodes[0].ruleset);
            }
            else {
                this.selectedIds = nodes.map((node) => node.ruleset.id);
                this._viewer.ruleset = nodes.length === 1 ? nodes[0].ruleset : undefined;
            }
        });
    }
    _onRuleSelectionChanged(event) {
        const nodes = event.detail.newNodes;
        this.selectedIds = nodes.map((node) => node.ruleset.id);
        this._viewer.ruleset = nodes.length === 1 ? Object.assign({}, nodes[0].ruleset) : undefined;
    }
    _onRuleAdd(event) {
        // Load the ruleset into the viewer
        this._viewer.ruleset = event.detail.ruleset;
    }
    _onRuleSave(event) {
        return __awaiter(this, void 0, void 0, function* () {
            yield this._rulesList.refresh();
            if (event.detail.success && event.detail.isNew) {
                this.selectedIds = [event.detail.ruleset.id];
            }
        });
    }
};
OrRules.DEFAULT_RULESET_NAME = "";
__decorate([
    property({ type: Boolean })
], OrRules.prototype, "readonly", void 0);
__decorate([
    property({ type: Object })
], OrRules.prototype, "config", void 0);
__decorate([
    property({ type: String })
], OrRules.prototype, "realm", void 0);
__decorate([
    property({ type: String })
], OrRules.prototype, "language", void 0);
__decorate([
    property({ type: Array })
], OrRules.prototype, "selectedIds", void 0);
__decorate([
    property({ attribute: false })
], OrRules.prototype, "_isValidRule", void 0);
__decorate([
    query("#rule-list")
], OrRules.prototype, "_rulesList", void 0);
__decorate([
    query("#rule-viewer")
], OrRules.prototype, "_viewer", void 0);
OrRules = __decorate([
    customElement("or-rules")
], OrRules);
export { OrRules };
//# sourceMappingURL=index.js.map