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
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { getAssetInfos, getAssetsByType, getAssetTypeFromQuery, OrRulesRuleChangedEvent, OrRulesRuleUnsupportedEvent } from "../index";
import { Util } from "@openremote/core";
import "./or-rule-when";
import "./or-rule-then-otherwise";
import "@openremote/or-components/or-panel";
import i18next from "i18next";
import { translate } from "@openremote/or-translate";
export class OrRulesJsonRuleChangedEvent extends CustomEvent {
    constructor() {
        super(OrRulesJsonRuleChangedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrRulesJsonRuleChangedEvent.NAME = "or-rules-json-rule-changed";
function getTypeAndTagsFromGroup(group) {
    if (!group) {
        return [];
    }
    let typesAndTags = [];
    if (group.items) {
        for (const condition of group.items) {
            const type = getAssetTypeFromQuery(condition.assets);
            if (type) {
                typesAndTags.push([type, condition.tag]);
            }
        }
    }
    if (group.groups) {
        for (const condition of group.groups) {
            typesAndTags = typesAndTags.concat(getTypeAndTagsFromGroup(condition));
        }
    }
    return typesAndTags;
}
export function getTargetTypeMap(rule) {
    if (!rule.when) {
        return [];
    }
    return getTypeAndTagsFromGroup(rule.when);
}
const style = css `
    :host {
        display: flex;
        width: calc(100% - 20px);
        padding: 0px 10px;
        margin-top: -10px;
    }
    
    :host > * {
        flex: 1;
    }

    @media screen and (max-width: 1400px) {
        :host > * {
            flex-grow: 0;
        }

        :host {
            flex-direction: column;
        }
    }
`;
let OrRuleJsonViewer = class OrRuleJsonViewer extends translate(i18next)(LitElement) {
    static get styles() {
        return style;
    }
    constructor() {
        super();
        this._loadedAssets = new Map();
        this._unsupported = false;
        this._activeAssetPromises = new Map();
        this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
    }
    connectedCallback() {
        super.connectedCallback();
        if (this._unsupported) {
            this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
        }
    }
    set ruleset(ruleset) {
        if (this._ruleset === ruleset) {
            return;
        }
        this._unsupported = false;
        this._ruleset = ruleset;
        if (!ruleset.rules) {
            // New ruleset so start a new rule
            if (this.config && this.config.json && this.config.json.rule) {
                this._rule = JSON.parse(JSON.stringify(this.config.json.rule));
            }
            else {
                this._rule = {
                    recurrence: {
                        mins: 0 // always
                    }
                };
            }
        }
        else {
            try {
                const rules = JSON.parse(ruleset.rules);
                if (!rules.rules || rules.rules.length > 1) {
                    if (this.isConnected) {
                        this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
                    }
                    else {
                        this._unsupported = true;
                    }
                    return;
                }
                this._rule = rules.rules[0];
                this.requestUpdate();
            }
            catch (e) {
                console.error("Invalid JSON rules, failed to parse: " + e);
                if (this.isConnected) {
                    this.dispatchEvent(new OrRulesRuleUnsupportedEvent());
                }
                else {
                    this._unsupported = true;
                }
            }
        }
    }
    updated(changedProperties) {
        if (changedProperties.has('config')) {
            this.loadAssetDescriptors(false);
            this.loadAssetDescriptors(true);
        }
    }
    render() {
        if (!this._rule) {
            return html ``;
        }
        const targetTypeMap = getTargetTypeMap(this._rule);
        return html `
            <div class="section-container">                                    
                <or-rule-when .rule="${this._rule}" .config="${this.config}" .assetInfos="${this._whenAssetInfos}" ?readonly="${this.readonly}" 
                              .assetProvider="${(type) => __awaiter(this, void 0, void 0, function* () { return this.loadAssets(type); })}"
                ></or-rule-when>
            </div>
        
            <div class="section-container">              
                <or-rule-then-otherwise .rule="${this._rule}" .config="${this.config}" .targetTypeMap="${targetTypeMap}" .assetInfos="${this._actionAssetInfos}" ?readonly="${this.readonly}" 
                                        .assetProvider="${(type) => __awaiter(this, void 0, void 0, function* () { return this.loadAssets(type); })}"
                ></or-rule-then-otherwise>
            </div>
        `;
    }
    // loadAssets function that also tracks what promises/fetches are active.
    // If so, await for those to finish to prevent multiple API requests.
    // Also using caching with the _loadedAssets object.
    loadAssets(type) {
        return __awaiter(this, void 0, void 0, function* () {
            if (this._activeAssetPromises.has(type)) {
                const data = yield (this._activeAssetPromises.get(type)); // await for the already existing fetch
                return data.assets;
            }
            else {
                const promise = getAssetsByType(type, (this._ruleset.type == "realm" ? this._ruleset.realm : undefined), this._loadedAssets);
                this._activeAssetPromises.set(type, promise);
                const data = yield promise;
                this._activeAssetPromises.delete(type);
                return data.assets;
            }
        });
    }
    loadAssetDescriptors(useActionConfig) {
        getAssetInfos(this.config, useActionConfig).then(result => {
            if (useActionConfig) {
                this._actionAssetInfos = [...result];
                this.requestUpdate();
            }
            else {
                this._whenAssetInfos = [...result];
                this.requestUpdate();
            }
        });
    }
    beforeSave() {
        if (!this._rule) {
            return;
        }
        this._rule.name = this._ruleset.name;
        const jsonRules = {
            rules: [this._rule]
        };
        this._ruleset.rules = JSON.stringify(jsonRules);
    }
    validate() {
        const rule = this._rule;
        if (!rule) {
            return false;
        }
        if (!rule.when) {
            return false;
        }
        if (!rule.then || rule.then.length === 0) {
            return false;
        }
        if (!this._validateConditionGroup(rule.when)) {
            return false;
        }
        if (!this._validateRuleActions(rule, rule.then)) {
            return false;
        }
        // TODO: Validate other rule sections
        return true;
    }
    _onJsonRuleChanged() {
        const valid = this.validate();
        this.dispatchEvent(new OrRulesRuleChangedEvent(valid));
    }
    _validateConditionGroup(group) {
        if ((!group.items || group.items.length === 0) && (!group.groups || group.groups.length === 0)) {
            return false;
        }
        if (group.items) {
            for (const condition of group.items) {
                if (!condition.assets && !condition.cron && !condition.sun) {
                    return false;
                }
                if (condition.cron && !Util.cronStringToISOString(condition.cron, true)) {
                    return false;
                }
                if (condition.sun && (!condition.sun.position || !condition.sun.location)) {
                    return false;
                }
                if (condition.assets && !this._validateAssetQuery(condition.assets, true, false)) {
                    return false;
                }
            }
        }
        if (group.groups) {
            for (const childGroup of group.groups) {
                if (!this._validateConditionGroup(childGroup)) {
                    return false;
                }
            }
        }
        return true;
    }
    _validateRuleActions(rule, actions) {
        var _a;
        if (!actions) {
            return true;
        }
        for (const action of actions) {
            switch (action.action) {
                case "wait":
                    if (!action.millis) {
                        return false;
                    }
                    break;
                case "write-attribute":
                    if (!action.attributeName) {
                        return false;
                    }
                    if (!this._validateAssetTarget(rule, action.target)) {
                        return false;
                    }
                    break;
                case "notification":
                    // TODO: validate notification rule action
                    break;
                case "webhook":
                    if (!((_a = action.webhook) === null || _a === void 0 ? void 0 : _a.url) || !action.webhook.httpMethod) {
                        return false;
                    }
                    break;
                case "update-attribute":
                    if (!action.attributeName) {
                        return false;
                    }
                    if (!action.updateAction) {
                        return false;
                    }
                    if (!this._validateAssetTarget(rule, action.target)) {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
        }
        return true;
    }
    _validateAssetTarget(rule, target) {
        if (!target) {
            return true;
        }
        if (target.assets) {
            return this._validateAssetQuery(target.assets, false, false);
        }
        if (target.matchedAssets) {
            return this._validateAssetQuery(target.matchedAssets, false, true);
        }
        if (target.conditionAssets && getTypeAndTagsFromGroup(rule.when).findIndex((typeAndTag) => target.conditionAssets === typeAndTag[1]) < 0) {
            return false;
        }
        return true;
    }
    _validateAssetQuery(query, isWhen, isMatchedAssets) {
        if (!query.types || query.types.length === 0) {
            return false;
        }
        if (isWhen) {
            if (!query.attributes || !query.attributes.items || query.attributes.items.length === 0) {
                return false;
            }
        }
        else if (!isMatchedAssets) {
            if (!query.ids || query.ids.length === 0) {
                return false;
            }
        }
        if (query.attributes && query.attributes.items) {
            for (const attribute of query.attributes.items) {
                if (!attribute.name || !attribute.name.match || !attribute.name.value) {
                    return false;
                }
                if (!attribute.value || !this._validateValuePredicate(attribute.value)) {
                    return false;
                }
            }
        }
        return true;
    }
    _validateValuePredicate(valuePredicate) {
        switch (valuePredicate.predicateType) {
            case "string":
                return valuePredicate.match !== undefined && valuePredicate.value !== undefined;
            case "boolean":
                return valuePredicate.value !== undefined;
            case "datetime":
            case "number":
                return valuePredicate.operator !== undefined && valuePredicate.value !== undefined && (valuePredicate.operator !== "BETWEEN" /* AQO.BETWEEN */ || valuePredicate.rangeValue !== undefined);
            case "radial":
                return valuePredicate.radius !== undefined && valuePredicate.lat !== undefined && valuePredicate.lng !== undefined;
            case "rect":
                return valuePredicate.lngMax !== undefined && valuePredicate.latMax !== undefined && valuePredicate.lngMin !== undefined && valuePredicate.latMin !== undefined;
            case "array":
                return (valuePredicate.index && !valuePredicate.value) || valuePredicate.value || valuePredicate.lengthEquals || valuePredicate.lengthLessThan || valuePredicate.lengthGreaterThan;
            case "value-empty":
                return true;
            default:
                return false;
        }
    }
};
__decorate([
    property({ attribute: false })
], OrRuleJsonViewer.prototype, "readonly", void 0);
__decorate([
    property({ attribute: false })
], OrRuleJsonViewer.prototype, "config", void 0);
__decorate([
    property({ attribute: false })
], OrRuleJsonViewer.prototype, "_ruleset", void 0);
__decorate([
    state() // to be exact: Map<AssetType name, Asset[]>
], OrRuleJsonViewer.prototype, "_loadedAssets", void 0);
OrRuleJsonViewer = __decorate([
    customElement("or-rule-json-viewer")
], OrRuleJsonViewer);
export { OrRuleJsonViewer };
//# sourceMappingURL=or-rule-json-viewer.js.map