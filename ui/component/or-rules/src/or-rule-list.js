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
var OrRuleList_1;
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-translate";
import manager, { Util } from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { style as OrAssetTreeStyle } from "@openremote/or-asset-tree";
import { OrRules, OrRulesAddEvent, OrRulesRequestAddEvent, OrRulesRequestDeleteEvent, OrRulesRequestSelectionEvent, OrRulesSelectionEvent, RuleViewInfoMap } from "./index";
import "@openremote/or-mwc-components/or-mwc-menu";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { translate } from "@openremote/or-translate";
import i18next from "i18next";
import { showErrorDialog, showOkCancelDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
// language=CSS
const style = css `
    :host {
        flex-grow: 1;
        font-size: var(--internal-or-rules-list-text-size);
        
        /*Override asset tree styles*/
        --internal-or-asset-tree-header-height: var(--internal-or-rules-list-header-height);
    }
    
    #wrapper {
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;
    }

    #wrapper[data-disabled] {
        opacity: 0.3;
        pointer-events: none;
    }

    .node-container {
        align-items: center;
        padding-left: 10px;
    }
    
    .header-ruleset-type {
        display: flex;
        align-items: center;
        height: var(--internal-or-asset-tree-header-height);
        line-height: var(--internal-or-asset-tree-header-height);
        color: var(--internal-or-rules-panel-color);
        background-color: var(--internal-or-rules-header-background-color);
    }
    
    #header-btns {
        --or-mwc-input-color: var(--internal-or-rules-text-color);
    }
    
    .header-ruleset-type p {
        margin: 0 15px;
    }

    .node-status {
        --or-icon-width: 18px; 
        margin-right: 8px;
    }

    .node-language{
        padding-left: 10px;
        opacity: 50%;
    }

    .iconfill-gray {
        --or-icon-fill: var(--internal-or-rules-list-icon-color-ok);
    }

    .iconfill-red {
        --or-icon-fill: var(--internal-or-rules-list-icon-color-error);
    }
`;
let OrRuleList = OrRuleList_1 = class OrRuleList extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.readonly = false;
        this.disabled = false;
        this.multiSelect = true;
        this._showLoading = true;
        this._globalRulesets = false;
        this._selectedNodes = [];
        this._rulesetPromises = new Map();
        this._ready = false;
    }
    static get styles() {
        return [
            OrAssetTreeStyle,
            style
        ];
    }
    refresh() {
        return __awaiter(this, void 0, void 0, function* () {
            this._nodes = undefined;
            // reloading rulesets is automatically done in shouldUpdate()
        });
    }
    get _allowedLanguages() {
        const languages = this.config && this.config.controls && this.config.controls.allowedLanguages ? [...this.config.controls.allowedLanguages] : OrRuleList_1.DEFAULT_ALLOWED_LANGUAGES;
        const groovyIndex = languages.indexOf("GROOVY" /* RulesetLang.GROOVY */);
        const flowIndex = languages.indexOf("FLOW" /* RulesetLang.FLOW */);
        if (!manager.isSuperUser()) {
            if (groovyIndex > 0)
                languages.splice(groovyIndex, 1);
        }
        else if (groovyIndex < 0) {
            languages.push("GROOVY" /* RulesetLang.GROOVY */);
        }
        if (this._globalRulesets) {
            if (flowIndex > 0)
                languages.splice(flowIndex, 1);
        }
        return languages;
    }
    _updateLanguage() {
        const rulesetLangs = this._allowedLanguages;
        if (!rulesetLangs || rulesetLangs.length === 0) {
            this.language = undefined;
        }
        else if (!this.language || rulesetLangs.indexOf(this.language) < 0) {
            this.language = rulesetLangs[0];
        }
    }
    _onReady() {
        this._ready = true;
        this._loadRulesets();
    }
    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        if (manager.ready) {
            this._onReady();
        }
    }
    shouldUpdate(_changedProperties) {
        const result = super.shouldUpdate(_changedProperties);
        if (!this.sortBy) {
            this.sortBy = "name";
        }
        if (_changedProperties.has("language")) {
            this._updateLanguage();
        }
        if (_changedProperties.has("_globalRulesets")) {
            this._updateLanguage();
            this.refresh();
        }
        if (manager.ready) {
            if (!this._nodes) {
                this._loadRulesets();
                return true;
            }
            if (_changedProperties.has("selectedIds")) {
                let changed = true;
                const oldValue = _changedProperties.get("selectedIds");
                if (this.selectedIds && oldValue && this.selectedIds.length === oldValue.length) {
                    changed = !!this.selectedIds.find((oldSelected) => oldValue.indexOf(oldSelected) < 0);
                }
                if (changed) {
                    this._updateSelectedNodes();
                }
            }
            if (_changedProperties.has("sortBy")) {
                OrRuleList_1._updateSort(this._nodes, this._getSortFunction());
            }
        }
        return result;
    }
    render() {
        if (!this.language) {
            this._updateLanguage();
        }
        const allowedLanguages = this._allowedLanguages;
        const sortOptions = ["name", "createdOn", "status"];
        if (allowedLanguages && allowedLanguages.length > 1)
            sortOptions.push("lang");
        let addTemplate = ``;
        if (!this._isReadonly()) {
            if (allowedLanguages && allowedLanguages.length > 1) {
                addTemplate = getContentWithMenuTemplate(html `<or-mwc-input type="${InputType.BUTTON}" icon="plus"></or-mwc-input>`, allowedLanguages.map((l) => {
                    return { value: l, text: i18next.t("rulesLanguages." + l) };
                }), this.language, (v) => this._onAddClicked(v));
            }
            else {
                addTemplate = html `<or-mwc-input type="${InputType.BUTTON}" icon="plus" @or-mwc-input-changed="${() => this._onAddClicked(this.language)}"></or-mwc-input>`;
            }
        }
        return html `
            <div id="wrapper" ?data-disabled="${this.disabled}">
                ${manager.isSuperUser() ? html `
                    <div class="header-ruleset-type">
                        <p>${i18next.t("realmRules")}</p>
                        
                        <div style="flex: 1 1 0; display: flex;">
                            <or-mwc-input style="margin: auto;" type="${InputType.SWITCH}" @or-mwc-input-changed="${(evt) => this._globalRulesets = evt.detail.value}"></or-mwc-input>
                        </div>

                        <p>${i18next.t("globalRules")}</p>
                    </div>
                ` : ``}

                <div id="header">
                    <div id="title-container">
                        <or-translate id="title" value="rule_plural"></or-translate>
                    </div>
        
                    <div id="header-btns">
                        <or-mwc-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="content-copy" @or-mwc-input-changed="${() => this._onCopyClicked()}"></or-mwc-input>
                        <or-mwc-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="delete" @or-mwc-input-changed="${() => this._onDeleteClicked()}"></or-mwc-input>
                        ${addTemplate}
                        <or-mwc-input hidden type="${InputType.BUTTON}" icon="magnify" @or-mwc-input-changed="${() => this._onSearchClicked()}"></or-mwc-input>
                        
                        ${getContentWithMenuTemplate(html `<or-mwc-input type="${InputType.BUTTON}" icon="sort-variant" ></or-mwc-input>`, sortOptions.map((sort) => { return { value: sort, text: i18next.t(sort) }; }), this.sortBy, (v) => this._onSortClicked(v))}
                    </div>
                </div>
        
                ${!this._nodes || this._showLoading
            ? html `
                        <span id="loading"><or-translate value="loading"></or-translate></span>`
            : html `
                        <div id="list-container">
                            <ol id="list">
                                ${this._nodes.map((treeNode) => this._nodeTemplate(treeNode))}
                            </ol>
                        </div>
                    `}
        
                <div id="footer">
                
                </div>
            </div>
        `;
    }
    _isReadonly() {
        return this.readonly || !manager.hasRole("write:rules" /* ClientRole.WRITE_RULES */);
    }
    _nodeTemplate(node) {
        let statusIcon = "help";
        let statusClass = "iconfill-gray";
        let nodeIcon = "mdi-state-machine";
        let nodeTitle = "Unknown language";
        switch (node.ruleset.status) {
            case "DEPLOYED":
                statusIcon = "";
                statusClass = "iconfill-gray";
                break;
            case "READY":
                statusIcon = "check";
                statusClass = "iconfill-gray";
                break;
            case "COMPILATION_ERROR":
            case "LOOP_ERROR":
            case "EXECUTION_ERROR":
                statusIcon = "alert-octagon";
                statusClass = "iconfill-red";
                break;
            case "DISABLED":
                statusIcon = "minus-circle";
                statusClass = "iconfill-gray";
                break;
            case "PAUSED":
                statusIcon = "calendar-arrow-right";
                statusClass = "iconfill-gray";
                break;
            case "EXPIRED":
                statusIcon = "calendar-remove";
                statusClass = "iconfill-gray";
                break;
            case "REMOVED":
                statusIcon = "close";
                statusClass = "iconfill-gray";
                break;
            default:
                statusIcon = "stop";
                statusClass = "iconfill-gray";
        }
        switch (node.ruleset.lang) {
            case ("JSON" /* RulesetLang.JSON */):
                nodeIcon = "ray-start-arrow";
                nodeTitle = "rulesLanguages.JSON";
                break;
            case ("FLOW" /* RulesetLang.FLOW */):
                nodeIcon = "transit-connection-variant";
                nodeTitle = "rulesLanguages.FLOW";
                break;
            case ("GROOVY" /* RulesetLang.GROOVY */):
                nodeIcon = "alpha-g-box-outline";
                nodeTitle = "rulesLanguages.GROOVY";
                break;
            case ("JAVASCRIPT" /* RulesetLang.JAVASCRIPT */):
                nodeIcon = "language-javascript";
                nodeTitle = "rulesLanguages.JAVASCRIPT";
                break;
            default:
                nodeIcon = "mdi-state-machine";
                nodeTitle = "Unknown language";
        }
        return html `
            <li ?data-selected="${node.selected}" @click="${(evt) => this._onNodeClicked(evt, node)}">
                <div class="node-container">
                    <or-icon style="--or-icon-width: 18px; margin-right: 8px;" icon="${nodeIcon}" title="${i18next.t(nodeTitle)}"></or-icon>
                    <span class="node-name">${node.ruleset.name}</span>
                    <or-icon class="node-status ${statusClass}" title="${i18next.t("rulesetStatus." + (node.ruleset.status ? node.ruleset.status : "NOSTATUS"))}" icon="${statusIcon}"></or-icon>
                </div>
            </li>
        `;
    }
    static _getNodeStatusClasses(ruleset) {
        let status = ruleset.enabled ? "bg-green" : "bg-red";
        if (ruleset.enabled) {
            // Look at validity meta
            if (ruleset.meta && ruleset.meta["validity"]) {
                const calendarEvent = ruleset.meta["validity"];
                const now = new Date().getTime();
                if (calendarEvent.start) {
                    if (now < calendarEvent.start) {
                        // before startDate, show blue
                        status = "bg-blue";
                    }
                    else if (calendarEvent.end && now > calendarEvent.end) {
                        // after endDate, show grey
                        status = "bg-grey";
                    }
                    else if (calendarEvent.recurrence) {
                        // TODO: Implement RRule support
                    }
                }
            }
        }
        return status;
    }
    _updateSelectedNodes() {
        const actuallySelectedIds = [];
        const selectedNodes = [];
        if (this._nodes) {
            this._nodes.forEach((node) => {
                if (this.selectedIds && this.selectedIds.indexOf(node.ruleset.id) >= 0) {
                    actuallySelectedIds.push(node.ruleset.id);
                    selectedNodes.push(node);
                    node.selected = true;
                }
                else {
                    node.selected = false;
                }
            });
        }
        this.selectedIds = actuallySelectedIds;
        const oldSelection = this._selectedNodes;
        this._selectedNodes = selectedNodes;
        this.dispatchEvent(new OrRulesSelectionEvent({
            oldNodes: oldSelection,
            newNodes: selectedNodes
        }));
    }
    static _updateSort(nodes, sortFunction) {
        if (!nodes) {
            return;
        }
        nodes.sort(sortFunction);
    }
    _onNodeClicked(evt, node) {
        if (evt.defaultPrevented) {
            return;
        }
        evt.preventDefault();
        let selectedNodes = [];
        const index = this._selectedNodes.indexOf(node);
        let select = true;
        let deselectOthers = true;
        const multiSelect = !this._isReadonly() && this.multiSelect && (!this.config || !this.config.controls || !this.config.controls.multiSelect);
        if (multiSelect && (evt.ctrlKey || evt.metaKey)) {
            deselectOthers = false;
            if (index >= 0 && this._selectedNodes && this._selectedNodes.length > 1) {
                select = false;
            }
        }
        if (deselectOthers) {
            selectedNodes = [node];
        }
        else if (select) {
            if (index < 0) {
                selectedNodes = [...this._selectedNodes];
                selectedNodes.push(node);
            }
        }
        else if (index >= 0) {
            selectedNodes = [...this._selectedNodes];
            selectedNodes.splice(index, 1);
        }
        Util.dispatchCancellableEvent(this, new OrRulesRequestSelectionEvent({
            oldNodes: this._selectedNodes,
            newNodes: selectedNodes
        })).then((detail) => {
            if (detail.allow) {
                this.selectedIds = detail.detail.newNodes.map((node) => node.ruleset.id);
            }
        });
    }
    _onCopyClicked() {
        if (this._selectedNodes.length !== 1) {
            return;
        }
        const node = this._selectedNodes[0];
        const ruleset = JSON.parse(JSON.stringify(node.ruleset));
        delete ruleset.lastModified;
        delete ruleset.createdOn;
        delete ruleset.status;
        delete ruleset.error;
        delete ruleset.id;
        ruleset.name = ruleset.name + " copy";
        if (this.config && this.config.rulesetCopyHandler && !this.config.rulesetCopyHandler(ruleset)) {
            return;
        }
        Util.dispatchCancellableEvent(this, new OrRulesRequestAddEvent({
            ruleset: ruleset,
            sourceRuleset: node.ruleset
        })).then((detail) => {
            if (detail.allow) {
                this.dispatchEvent(new OrRulesAddEvent(detail.detail));
            }
        });
    }
    _onAddClicked(lang) {
        const type = this._globalRulesets ? "global" : "realm";
        const realm = manager.isSuperUser() ? manager.displayRealm : manager.config.realm;
        const ruleset = {
            id: 0,
            type: type,
            name: OrRules.DEFAULT_RULESET_NAME,
            lang: lang,
            realm: realm,
            rules: undefined
        };
        if (this.config && this.config.rulesetAddHandler && !this.config.rulesetAddHandler(ruleset)) {
            return;
        }
        if (this.config && this.config.rulesetTemplates && this.config.rulesetTemplates[lang]) {
            ruleset.rules = this.config.rulesetTemplates[lang];
        }
        else {
            ruleset.rules = RuleViewInfoMap[lang].viewRulesetTemplate;
        }
        // Ensure config hasn't messed with certain values
        if (type === "realm") {
            ruleset.realm = realm;
        }
        const detail = {
            ruleset: ruleset,
            isCopy: false
        };
        Util.dispatchCancellableEvent(this, new OrRulesRequestAddEvent(detail))
            .then((detail) => {
            if (detail.allow) {
                this.dispatchEvent(new OrRulesAddEvent(detail.detail));
            }
        });
    }
    _onDeleteClicked() {
        if (this._selectedNodes.length > 0) {
            Util.dispatchCancellableEvent(this, new OrRulesRequestDeleteEvent(this._selectedNodes))
                .then((detail) => {
                if (detail.allow) {
                    this._doDelete();
                }
            });
        }
    }
    _doDelete() {
        if (!this._selectedNodes || this._selectedNodes.length === 0) {
            return;
        }
        const rulesetsToDelete = this._selectedNodes.map((rulesetNode) => rulesetNode.ruleset);
        const rulesetNamesToDelete = rulesetsToDelete.map(ruleset => "\n- " + ruleset.name);
        const doDelete = () => __awaiter(this, void 0, void 0, function* () {
            this.disabled = true;
            let fail = false;
            for (const ruleset of rulesetsToDelete) {
                if (this.config && this.config.rulesetDeleteHandler && !this.config.rulesetDeleteHandler(ruleset)) {
                    continue;
                }
                try {
                    let response;
                    switch (ruleset.type) {
                        case "asset":
                            response = yield manager.rest.api.RulesResource.deleteAssetRuleset(ruleset.id);
                            break;
                        case "realm":
                            response = yield manager.rest.api.RulesResource.deleteRealmRuleset(ruleset.id);
                            break;
                        case "global":
                            response = yield manager.rest.api.RulesResource.deleteGlobalRuleset(ruleset.id);
                            break;
                    }
                    if (response.status !== 204) {
                        console.error("Delete ruleset returned unexpected status '" + response.status + "': " + JSON.stringify(ruleset, null, 2));
                        fail = true;
                    }
                }
                catch (e) {
                    console.error("Failed to delete ruleset: " + JSON.stringify(ruleset, null, 2), e);
                    fail = true;
                }
            }
            if (fail) {
                showErrorDialog(i18next.t("deleteAssetsFailed"));
            }
            this.disabled = false;
            this.refresh();
        });
        // Confirm deletion request
        showOkCancelDialog(i18next.t("deleteRulesets"), i18next.t("deleteRulesetsConfirm", { ruleNames: rulesetNamesToDelete }), i18next.t("delete"))
            .then((ok) => {
            if (ok) {
                doDelete();
            }
        });
    }
    _onSearchClicked() {
    }
    _onSortClicked(sortBy) {
        this.sortBy = sortBy;
    }
    _getSortFunction() {
        switch (this.sortBy) {
            case "createdOn":
                return Util.sortByNumber((node) => node.ruleset[this.sortBy]);
            case "status":
                return Util.sortByString((node) => node.ruleset[this.sortBy]);
            default:
                return Util.sortByString((node) => node.ruleset[this.sortBy]);
        }
    }
    _getRealm() {
        if (manager.isSuperUser()) {
            return manager.displayRealm;
        }
        return manager.getRealm();
    }
    _loadRulesets() {
        return __awaiter(this, void 0, void 0, function* () {
            const sortFunction = this._getSortFunction();
            let data;
            // Global rulesets
            if (this._globalRulesets) {
                if (this._rulesetPromises.size == 0 && !this._rulesetPromises.has("global")) {
                    this._rulesetPromises.set("global", new Promise((resolve) => __awaiter(this, void 0, void 0, function* () {
                        const response = yield manager.rest.api.RulesResource.getGlobalRulesets({ fullyPopulate: true });
                        resolve(response.data);
                    })));
                }
                data = yield this._rulesetPromises.get("global");
                this._rulesetPromises.delete("global");
            }
            // Realm rulesets
            else {
                const ruleRealm = this._getRealm() || manager.displayRealm;
                if (this._rulesetPromises.size == 0 && !this._rulesetPromises.has(ruleRealm)) {
                    this._rulesetPromises.set(ruleRealm, new Promise((resolve) => __awaiter(this, void 0, void 0, function* () {
                        const params = { fullyPopulate: true, language: this._allowedLanguages };
                        const response = yield manager.rest.api.RulesResource.getRealmRulesets(ruleRealm, params);
                        resolve(response.data);
                    })));
                }
                data = yield this._rulesetPromises.get(ruleRealm);
                this._rulesetPromises.delete(ruleRealm);
            }
            // ... building the nodes
            if (data) {
                this._buildTreeNodes(data, sortFunction);
            }
        });
    }
    _buildTreeNodes(rulesets, sortFunction) {
        if (!rulesets || rulesets.length === 0) {
            this._nodes = [];
        }
        else {
            const nodes = rulesets.map((ruleset) => {
                return {
                    ruleset: ruleset
                };
            });
            nodes.sort(sortFunction);
            this._nodes = nodes;
        }
        if (this.selectedIds && this.selectedIds.length > 0) {
            this._updateSelectedNodes();
        }
        this._showLoading = false;
    }
};
OrRuleList.DEFAULT_ALLOWED_LANGUAGES = ["JSON" /* RulesetLang.JSON */, "GROOVY" /* RulesetLang.GROOVY */, "JAVASCRIPT" /* RulesetLang.JAVASCRIPT */, "FLOW" /* RulesetLang.FLOW */];
__decorate([
    property({ type: Object })
], OrRuleList.prototype, "config", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleList.prototype, "readonly", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleList.prototype, "disabled", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleList.prototype, "multiSelect", void 0);
__decorate([
    property({ type: Array })
], OrRuleList.prototype, "selectedIds", void 0);
__decorate([
    property({ type: String })
], OrRuleList.prototype, "sortBy", void 0);
__decorate([
    property({ type: String })
], OrRuleList.prototype, "language", void 0);
__decorate([
    property({ attribute: false })
], OrRuleList.prototype, "_nodes", void 0);
__decorate([
    property({ attribute: false })
], OrRuleList.prototype, "_showLoading", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleList.prototype, "_globalRulesets", void 0);
OrRuleList = OrRuleList_1 = __decorate([
    customElement("or-rule-list")
], OrRuleList);
export { OrRuleList };
//# sourceMappingURL=or-rule-list.js.map