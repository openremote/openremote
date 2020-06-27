import {css, customElement, html, LitElement, property, query, TemplateResult, unsafeCSS, PropertyValues} from "lit-element";
import manager, {
    AssetModelUtil,
    DefaultBoxShadow,
    DefaultColor1,
    DefaultColor2,
    DefaultColor3,
    DefaultColor4,
    DefaultColor5,
    DefaultColor6
} from "@openremote/core";
import i18next from "i18next";
import "@openremote/or-select";
import "@openremote/or-icon";
import {
    AssetDescriptor,
    AssetQuery,
    Attribute,
    AttributeDescriptor,
    AttributeValueDescriptor,
    JsonRule,
    LogicGroup,
    MetaItemDescriptor,
    MetaItemType,
    RuleActionUnion,
    RuleCondition,
    RulesetLang,
    RulesetUnion,
    TenantRuleset,
    ValueType,
    ClientRole,
    Asset,
    NotificationTargetType
} from "@openremote/model";
import "@openremote/or-translate";
import "@openremote/or-mwc-components/dist/or-mwc-drawer";
import {translate} from "@openremote/or-translate";
import "./or-rule-list";
import "./or-rule-viewer";
import "./flow-viewer/flow-viewer";
import {OrRuleList} from "./or-rule-list";
import {OrRuleViewer} from "./or-rule-viewer";
import {RecurrenceOption} from "./json-viewer/or-rule-then-otherwise";
import { AttributeInputCustomProvider } from "@openremote/or-attribute-input";

export const enum ConditionType {
    ASSET_QUERY = "assetQuery",
    TIMER = "timer"
}

export const enum ActionType {
    WAIT = "wait",
    NOTIFICATION = "notification",
    PUSH_NOTIFICATION = "push",
    ATTRIBUTE = "attribute"
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

export interface AssetTypeAttributeName {
    assetType: string;
    attributeName: string;
}
export interface NotificationActionTargetType {
    [messageType: string]: NotificationTargetType[]
}


export interface RulesConfig {
    controls?: {
        allowedLanguages?: RulesetLang[];
        allowedConditionTypes?: ConditionType[];
        allowedActionTypes?: ActionType[];
        allowedAssetQueryOperators?: Map<AssetTypeAttributeName | AttributeDescriptor | AttributeValueDescriptor | ValueType, AssetQueryOperator[]>;
        allowedRecurrenceOptions?: RecurrenceOption[];
        hideNotificationTargetType?: NotificationActionTargetType;
        hideActionTypeOptions?: boolean;
        hideActionTargetOptions?: boolean;
        hideActionUpdateOptions?: boolean;
        hideConditionTypeOptions?: boolean;
        hideThenAddAction?: boolean;
        hideWhenAddCondition?: boolean;
        hideWhenAddAttribute?: boolean;
        hideWhenAddGroup?: boolean;
    };
    inputProvider?: AttributeInputCustomProvider;
    descriptors?: {
        all?: RulesDescriptorSection;
        when?: RulesDescriptorSection;
        action?: RulesDescriptorSection;
    };
    rulesetName?: string;
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

export interface RulesConfigAttribute {
    icon?: string;
    valueDescriptor?: RulesConfigAttribute;
    initialValue?: any;
    allowedValues?: any[];
    allowedMin?: any;
    allowedMax?: any;
}

export interface RulesConfigAttributeValue {
    name?: string;
    icon?: string;
    valueType?: ValueType;
}

export interface RulesetNode {
    ruleset: TenantRuleset;
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

function getAssetDescriptorFromSection(assetType: string, config: RulesConfig | undefined, useActionConfig: boolean) {
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

export function getDescriptorValueType(descriptor?: AttributeDescriptor) {
    if (!descriptor || !descriptor.valueDescriptor) {
        return;
    }

    return descriptor.valueDescriptor.valueType;
}

export function getAssetTypeFromQuery(query?: AssetQuery) {
    return query && query.types && query.types.length > 0 && query.types[0] ? query.types[0].value : undefined;
}

export function getAssetIdsFromQuery(query?: AssetQuery) {
    return query && query.ids ? [...query.ids] : undefined;
}

export function getAttributeNames(descriptor?: AssetDescriptor) {
    let attributes: string[] = [];

    if (descriptor && descriptor.attributeDescriptors) {
        attributes = descriptor.attributeDescriptors.map((ad) => ad.attributeName!);
    }

    return attributes;
}

export const getAssetTypes = async () => {
    const response = await manager.rest.api.AssetResource.queryAssets({
        select: {
            excludeAttributes: true,
            excludeParentInfo: true,
            excludePath: true,
            excludeAttributeMeta: true,
            excludeAttributeTimestamp: true,
            excludeAttributeType: true,
            excludeAttributeValue: true
        },
        recursive: true
    });

    if(response && response.data) {
        return response.data.map(asset => asset.type!);
    }
}


export function getAssetDescriptors(config: RulesConfig | undefined, useActionConfig: boolean) {
    const assetDescriptors: AssetDescriptor[] = AssetModelUtil.getAssetDescriptors();
    return getAssetTypes().then(availibleAssetTypes => {
        let allowedAssetTypes: string[] = availibleAssetTypes ? availibleAssetTypes : [];
        let excludedAssetTypes: string[] = [];
        if (!config || !config.descriptors) {
            return assetDescriptors;
        }

        const section = useActionConfig ? config.descriptors.action : config.descriptors.when;

        if (section && section.includeAssets) {
            allowedAssetTypes =  allowedAssetTypes.concat(section.includeAssets);
        }

        if (config.descriptors.all && config.descriptors.all.includeAssets) {
            allowedAssetTypes = allowedAssetTypes.concat(config.descriptors.all.includeAssets);
        }
        if (section && section.excludeAssets) {
            excludedAssetTypes = [...section.excludeAssets];
        }

        if (config.descriptors.all && config.descriptors.all.excludeAssets) {
            excludedAssetTypes = excludedAssetTypes.concat(config.descriptors.all.excludeAssets);
        }

        return assetDescriptors.filter((ad) => {
            if (allowedAssetTypes.length > 0 && allowedAssetTypes.indexOf(ad.type!) < 0) {
                return false;
            }
            return excludedAssetTypes.indexOf(ad.type!) < 0;

        }).map((ad) => {
            // Amalgamate matching descriptor from config if defined
            const configDescriptor = getAssetDescriptorFromSection(ad.type!, config, useActionConfig);
            if (!configDescriptor) {
                return ad;
            }

            const modifiedDescriptor: AssetDescriptor = {
                name: ad.name,
                type: ad.type,
                icon: ad.icon,
                color: ad.color,
                attributeDescriptors: ad.attributeDescriptors ? [...ad.attributeDescriptors] : undefined
            };

            if (configDescriptor.icon) {
                modifiedDescriptor.icon = configDescriptor.icon;
            }
            if (configDescriptor.name) {
                modifiedDescriptor.name = configDescriptor.name;
            }
            if (configDescriptor.color) {
                modifiedDescriptor.color = configDescriptor.color;
            }

            // Remove any excluded attributes
            if (modifiedDescriptor.attributeDescriptors) {
                const inc = configDescriptor.includeAttributes !== undefined ? configDescriptor.includeAttributes : undefined;
                const exc = configDescriptor.excludeAttributes !== undefined ? configDescriptor.excludeAttributes : undefined;

                if (inc || exc) {
                    modifiedDescriptor.attributeDescriptors = modifiedDescriptor.attributeDescriptors.filter((mad) => {
                        if (exc && exc.indexOf(mad.attributeName!) >= 0) {
                            return false;
                        }
                        if (inc && inc.indexOf(mad.attributeName!) < 0) {
                            return false;
                        }
                        return true;
                    });
                }

                // Override any attribute descriptors
                if (configDescriptor.attributeDescriptors) {
                    modifiedDescriptor.attributeDescriptors.map((attributeDescriptor) => {
                        let configAttributeDescriptor: RulesConfigAttribute | undefined = configDescriptor.attributeDescriptors![attributeDescriptor.attributeName!];
                        if (!configAttributeDescriptor) {
                            configAttributeDescriptor = section && section.attributeDescriptors ? section.attributeDescriptors[attributeDescriptor.attributeName!] : undefined;
                        }
                        if (!configAttributeDescriptor) {
                            configAttributeDescriptor = config.descriptors!.all && config.descriptors!.all.attributeDescriptors ? config.descriptors!.all.attributeDescriptors[attributeDescriptor.attributeName!] : undefined;
                        }
                        if (configAttributeDescriptor) {
                            let metaItemDescriptors = attributeDescriptor.metaItemDescriptors;
                            if (!metaItemDescriptors) {
                                attributeDescriptor.metaItemDescriptors = [];
                                metaItemDescriptors = attributeDescriptor.metaItemDescriptors;
                            }

                            if (configAttributeDescriptor.allowedValues) {
                                addOrReplaceMetaItemDescriptor(metaItemDescriptors, AssetModelUtil.getMetaItemDescriptorInitialValue(MetaItemType.ALLOWED_VALUES, configAttributeDescriptor.allowedValues));
                            }
                            if (configAttributeDescriptor.allowedMin) {
                                addOrReplaceMetaItemDescriptor(metaItemDescriptors, AssetModelUtil.getMetaItemDescriptorInitialValue(MetaItemType.RANGE_MIN, configAttributeDescriptor.allowedMin));
                            }
                            if (configAttributeDescriptor.allowedMax) {
                                addOrReplaceMetaItemDescriptor(metaItemDescriptors, AssetModelUtil.getMetaItemDescriptorInitialValue(MetaItemType.RANGE_MAX, configAttributeDescriptor.allowedMax));
                            }

                            return {
                                attributeName: attributeDescriptor.attributeName,
                                valueDescriptor: configAttributeDescriptor.valueDescriptor ? configAttributeDescriptor.valueDescriptor : attributeDescriptor.valueDescriptor,
                                initialValue: configAttributeDescriptor.initialValue ? configAttributeDescriptor.initialValue : attributeDescriptor.initialValue,
                                metaItemDescriptors: attributeDescriptor.metaItemDescriptors
                            };
                        }

                        return attributeDescriptor;
                    });
                }
            }
            return modifiedDescriptor;
        });

    });
}

function addOrReplaceMetaItemDescriptor(metaItemDescriptors: MetaItemDescriptor[], metaItemDescriptor: MetaItemDescriptor) {
    const index = metaItemDescriptors.findIndex((mid) => mid.urn === metaItemDescriptor.urn);
    if (index >= 0) {
        metaItemDescriptors.splice(index, 1);

    }
    metaItemDescriptors.push(metaItemDescriptor);
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

export class OrRulesRequestSelectEvent extends CustomEvent<RequestEventDetail<RulesetUnion[]>> {

    public static readonly NAME = "or-rules-request-select";

    constructor(request: TenantRuleset[]) {
        super(OrRulesRequestSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: request
            }
        });
    }
}

export class OrRulesRequestAddEvent extends CustomEvent<RulesetUnion> {

    public static readonly NAME = "or-rules-request-add";

    constructor(lang: RulesetLang, type:any) {
        super(OrRulesRequestAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {lang, type}
        });
    }
}

export class OrRulesRequestDeleteEvent extends CustomEvent<RulesetUnion[]> {

    public static readonly NAME = "or-rules-request-delete";

    constructor(request: TenantRuleset[]) {
        super(OrRulesRequestDeleteEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: request
        });
    }
}

export class OrRulesRequestCopyEvent extends CustomEvent<RulesetUnion> {

    public static readonly NAME = "or-rules-request-copy";

    constructor(request: TenantRuleset) {
        super(OrRulesRequestCopyEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: request
        });
    }
}

export class OrRulesSaveStartEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-save-start";

    constructor() {
        super(OrRulesSaveStartEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrRulesSaveEndEvent extends CustomEvent<boolean> {

    public static readonly NAME = "or-rules-save-end";

    constructor(success: boolean) {
        super(OrRulesSaveEndEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: success
        });
    }
}

export class OrRulesSelectionChangedEvent extends CustomEvent<RulesetUnion[]> {

    public static readonly NAME = "or-rules-selection-changed";

    constructor(nodes: TenantRuleset[]) {
        super(OrRulesSelectionChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: nodes
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrRulesRuleUnsupportedEvent.NAME]: OrRulesRuleUnsupportedEvent;
        [OrRulesRequestSelectEvent.NAME]: OrRulesRequestSelectEvent;
        [OrRulesRequestAddEvent.NAME]: OrRulesRequestAddEvent;
        [OrRulesRequestDeleteEvent.NAME]: OrRulesRequestDeleteEvent;
        [OrRulesRequestCopyEvent.NAME]: OrRulesRequestCopyEvent;
        [OrRulesSaveStartEvent.NAME]: OrRulesSaveStartEvent;
        [OrRulesSaveEndEvent.NAME]: OrRulesSaveEndEvent;
        [OrRulesRuleChangedEvent.NAME]: OrRulesRuleChangedEvent;
        [OrRulesSelectionChangedEvent.NAME]: OrRulesSelectionChangedEvent;
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

        --internal-or-rules-list-button-size: var(--or-rules-list-button-size, 24px);
        
        --internal-or-rules-header-background-color: var(--or-rules-header-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
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
    private _ruleViewer!: OrRuleViewer;

    @property({attribute: false})
    private _activeRuleset?: RulesetUnion;

    constructor() {
        super();

        this.addEventListener(OrRulesRequestSelectEvent.NAME, this._onRequestSelect);
        this.addEventListener(OrRulesRequestAddEvent.NAME,this._onRequestAdd);
        this.addEventListener(OrRulesRequestDeleteEvent.NAME, this._onRequestDelete);
        this.addEventListener(OrRulesRequestCopyEvent.NAME, this._onRequestCopy);
        this.addEventListener(OrRulesSelectionChangedEvent.NAME, this._onRuleSelectionChanged);
        this.addEventListener(OrRulesSaveEndEvent.NAME, this._onRuleSaveEnd);
        this.addEventListener(OrRulesSaveStartEvent.NAME, this._onRuleSaveStart);
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (this._rulesList && _changedProperties.has("selectedIds")) {
            this._rulesList.selectedIds = this.selectedIds;
            if (_changedProperties.size === 1) {
                return false;
            }
        }

        return super.shouldUpdate(_changedProperties);
    }

    protected render() {

        return html`
            <or-rule-list id="rule-list" .config="${this.config}" .language="${this.language}" .selectedIds="${this.selectedIds}"></or-rule-list>
            <or-rule-viewer id="rule-viewer" .ruleset="${this._activeRuleset}" .config="${this.config}" .readonly="${this.isReadonly()}"></or-rule-viewer>
        `;
    }

    protected isReadonly(): boolean {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_RULES);
    }

    protected _onRequestAdd(e: OrRulesRequestAddEvent) {
        const lang = e.detail.lang;
        const type = e.detail.type;
        const shouldContinue = this._okToLeaveActiveRule();

        if (!shouldContinue) {
            return;
        }

        const name = this.config && this.config.rulesetName ? this.config.rulesetName : OrRules.DEFAULT_RULESET_NAME;
        const realm = manager.isSuperUser() ? manager.displayRealm : manager.config.realm;
        const ruleset: RulesetUnion = {
            id: 0,
            type: type,
            name: name,
            lang: lang,
            realm: realm,
            rules: undefined // View needs to populate this on load
        };
        console.log(ruleset)
        this._activeRuleset = ruleset;
    }

    protected _onRequestCopy(e: OrRulesRequestCopyEvent) {
        const shouldContinue = this._okToLeaveActiveRule();

        if (!shouldContinue) {
            return;
        }

        let ruleset = JSON.parse(JSON.stringify(e.detail)) as RulesetUnion;
        delete ruleset.lastModified;
        delete ruleset.createdOn;
        delete ruleset.status;
        delete ruleset.error;
        delete ruleset.id;
        this._activeRuleset = ruleset;
    }

    protected async _onRequestDelete(event: OrRulesRequestDeleteEvent) {

        let confirmed = this._okToLeaveActiveRule();

        if (!confirmed) {
            return;
        }

        confirmed = !this._activeRuleset!.id || window.confirm(i18next.t("confirmDelete"));

        if (!confirmed) {
            return;
        }

        this._activeRuleset = undefined;

        const rulesetsToDelete = event.detail;

        // We need to call the backend so disable list until done
        this._rulesList.disabled = true;
        for (let ruleset of rulesetsToDelete) {
            try {
                await manager.rest.api.RulesResource.deleteTenantRuleset(ruleset.id!);
            } catch (e) {
                console.error("Failed to delete ruleset '" + ruleset.id + "': " + e);
            }
        }
        this._rulesList.refresh();
        this._rulesList.disabled = false;
    }

    protected _onRequestSelect(event: OrRulesRequestSelectEvent) {
        event.detail.allow = this._okToLeaveActiveRule();
    }

    protected _onRuleSelectionChanged(event: OrRulesSelectionChangedEvent) {
        this._activeRuleset = event.detail.length === 1 ? {...event.detail[0]} : undefined;
        this.selectedIds = event.detail.length === 1 ? [event.detail[0].id!] : undefined;
    }

    protected _onRuleSaveStart(event: OrRulesSaveStartEvent) {
        this._rulesList.disabled = true;
        this._ruleViewer.disabled = true;
    }

    protected _onRuleSaveEnd(event: OrRulesSaveEndEvent) {
        this._rulesList.disabled = false;
        this._ruleViewer.disabled = false;
        this.selectedIds = [
            this._ruleViewer.ruleset!.id!
        ];
        if (event.detail) {
            this._rulesList.refresh();
        }
    }

    protected _okToLeaveActiveRule() {
        return !this._ruleViewer.modified || window.confirm(i18next.t("continueWithoutSaving"));
    }
}
