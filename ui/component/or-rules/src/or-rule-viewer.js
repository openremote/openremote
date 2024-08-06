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
import { customElement, property, query } from "lit/decorators.js";
import { OrRulesRequestSaveEvent, OrRulesRuleChangedEvent, OrRulesRuleUnsupportedEvent, OrRulesSaveEvent, RuleViewInfoMap } from "./index";
import manager, { Util } from "@openremote/core";
import "./json-viewer/or-rule-json-viewer";
import "./or-rule-text-viewer";
import "./or-rule-validity";
import "./flow-viewer/components/flow-editor";
import "@openremote/or-mwc-components/or-mwc-input";
import { translate } from "@openremote/or-translate";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import { showErrorDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { project } from "./flow-viewer/components/flow-editor";
// language=CSS
export const style = css `

    :host {
        display: block;
        height: 100%;
        width: 100%;
        overflow-y: auto;
    }

    .wrapper {
        display: flex;
        flex-direction: column;
        align-items: center;
        height: 100%;
    }

    #main-wrapper.saving {
        opacity: 0.5;
        pointer-events: none;
    }
    
    #rule-name {
        max-width: 400px;
        flex: 1 1 0;
        display: flex;
    }
    
    #rule-header {
        display: flex;
        align-items: center;
        width: 100%;
        box-sizing: border-box;
        min-height: var(--internal-or-rules-header-height);
        height: var(--internal-or-rules-header-height);
        z-index: 7;
        padding: 15px 20px;
        --or-icon-fill: var(--internal-or-rules-panel-color);
    }
    
    #rule-header-controls {
        margin-left: auto;
        display: flex;
        align-items: center;
    }

    #rule-status {
        flex: 1;
        text-align: center;
    }
    
    #rule-id {
        min-width: 70px;
        margin-right: 10px;
    }
    
    #rule-header-controls > * {
        margin: 0 10px;
    }
    
    #active-wrapper {
        display: flex;
        align-items: center;
    }
    
    #rule-view {
        flex-grow: 1;
        background-color: var(--internal-or-rules-background-color);
    }
`;
let OrRuleViewer = class OrRuleViewer extends translate(i18next)(LitElement) {
    static get styles() {
        return [
            style
        ];
    }
    constructor() {
        super();
        this.readonly = false;
        this.disabled = false;
        this.modified = false;
        this._ruleValid = false;
        this._supported = true;
        this._focusName = false;
        this.addEventListener(OrRulesRuleChangedEvent.NAME, this._onRuleChanged);
        this.addEventListener(OrRulesRuleUnsupportedEvent.NAME, this._onRuleUnsupported);
    }
    get valid() {
        return this.ruleset && this.view && this._ruleValid && this.ruleset.name && this.ruleset.name.length >= 3 && this.ruleset.name.length < 255;
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("ruleset")) {
            this._supported = true;
            this.modified = false;
            this._ruleValid = false;
            if (this.ruleset) {
                this._focusName = true;
                this.modified = !this.ruleset.id;
            }
        }
        return super.shouldUpdate(_changedProperties);
    }
    render() {
        if (!this.ruleset) {
            return html `<div class="wrapper" style="justify-content: center"><or-translate value="noRuleSelected"></or-translate></div>`;
        }
        let viewer = RuleViewInfoMap[this.ruleset.lang].viewTemplateProvider(this.ruleset, this.config, this.readonly);
        return html `
            <div id="main-wrapper" class="wrapper">            
                <div id="rule-header">
                    <h3 id="rule-id">${this.ruleset.id ? "ID: " + this.ruleset.id : ""}</h3>
                    <or-mwc-input id="rule-name" outlined .type="${InputType.TEXT}" .label="${i18next.t("ruleName")}" ?focused="${this._focusName}" .value="${this.ruleset ? this.ruleset.name : null}" ?disabled="${this._isReadonly()}" required minlength="3" maxlength="255" @or-mwc-input-changed="${(e) => this._changeName(e.detail.value)}"></or-mwc-input>
                    <or-rule-validity id="rule-header-validity" .ruleset="${this.ruleset}"></or-rule-validity>
                    ${this.ruleset.status ? html `<span id="rule-status" title="${this.ruleset.error}">${i18next.t("status") + ": " + this.ruleset.status}</span>` : ``}
                    <div id="rule-header-controls">
                        <span id="active-wrapper">
                            <or-translate value="enabled"></or-translate>
                            <or-mwc-input .type="${InputType.SWITCH}" .value="${this.ruleset && this.ruleset.enabled}" ?disabled="${!this.ruleset.id}" @or-mwc-input-changed="${this._toggleEnabled}"></or-mwc-input>
                        </span>
           
                        <or-mwc-input .type="${InputType.BUTTON}" id="save-btn" label="save" raised ?disabled="${this._cannotSave()}" @or-mwc-input-changed="${this._onSaveClicked}"></or-mwc-input>
                    </div>                        
                </div>

                ${viewer}
            </div>
        `;
    }
    updated(_changedProperties) {
        if (_changedProperties.has("ruleset")) {
            if (this.ruleset && this.view) {
                this._ruleValid = this.view.validate();
            }
        }
    }
    _isReadonly() {
        return this.readonly || !manager.hasRole("write:rules" /* ClientRole.WRITE_RULES */);
    }
    _cannotSave() {
        return this._isReadonly() || !this.ruleset || !this.modified || !this.valid;
    }
    _changeName(name) {
        if (this.ruleset && this.ruleset.name !== name) {
            this.ruleset.name = name;
            this.modified = true;
            this.requestUpdate();
        }
    }
    _onRuleChanged(e) {
        this.modified = true;
        this._ruleValid = e.detail;
    }
    _onSaveClicked() {
        if (!this.ruleset || !this.view) {
            return;
        }
        project.emit("fitview");
        if (this.config && this.config.rulesetSaveHandler && !this.config.rulesetSaveHandler(this.ruleset)) {
            return;
        }
        Util.dispatchCancellableEvent(this, new OrRulesRequestSaveEvent(this.ruleset))
            .then((detail) => {
            if (detail.allow) {
                this._doSave();
            }
        });
    }
    _doSave() {
        return __awaiter(this, void 0, void 0, function* () {
            const ruleset = this.ruleset;
            if (!ruleset || !this.view) {
                return;
            }
            let fail = false;
            const isNew = !ruleset.id;
            this.saveBtnElem.disabled = true;
            this.wrapperElem.classList.add("saving");
            this.view.beforeSave();
            let response;
            try {
                switch (ruleset.type) {
                    case "asset":
                        if (isNew) {
                            response = yield manager.rest.api.RulesResource.createAssetRuleset(ruleset);
                        }
                        else {
                            response = yield manager.rest.api.RulesResource.updateAssetRuleset(ruleset.id, ruleset);
                        }
                        break;
                    case "realm":
                        if (isNew) {
                            response = yield manager.rest.api.RulesResource.createRealmRuleset(ruleset);
                        }
                        else {
                            response = yield manager.rest.api.RulesResource.updateRealmRuleset(ruleset.id, ruleset);
                        }
                        break;
                    case "global":
                        if (isNew) {
                            response = yield manager.rest.api.RulesResource.createGlobalRuleset(ruleset);
                        }
                        else {
                            response = yield manager.rest.api.RulesResource.updateGlobalRuleset(ruleset.id, ruleset);
                        }
                        break;
                }
                if (response.status !== (isNew ? 200 : 204)) {
                    fail = true;
                    showErrorDialog("Create ruleset returned unexpected status: " + response.status);
                    return;
                }
                else if (response.data) {
                    ruleset.id = response.data;
                }
            }
            catch (e) {
                fail = true;
                console.error("Failed to save ruleset", e);
            }
            this.wrapperElem.classList.remove("saving");
            this.saveBtnElem.disabled = false;
            if (fail) {
                showErrorDialog(i18next.t("saveRulesetFailed"));
            }
            this.dispatchEvent(new OrRulesSaveEvent({
                ruleset: ruleset,
                isNew: isNew,
                success: !fail
            }));
        });
    }
    _onRuleUnsupported() {
        this._supported = false;
    }
    _toggleEnabled() {
        if (this.ruleset) {
            this.ruleset.enabled = !this.ruleset.enabled;
            this.modified = true;
            this.requestUpdate();
        }
    }
};
__decorate([
    property({ type: Object })
], OrRuleViewer.prototype, "ruleset", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleViewer.prototype, "readonly", void 0);
__decorate([
    property({ type: Boolean })
], OrRuleViewer.prototype, "disabled", void 0);
__decorate([
    property({ type: Object })
], OrRuleViewer.prototype, "config", void 0);
__decorate([
    property({ attribute: false })
], OrRuleViewer.prototype, "modified", void 0);
__decorate([
    property({ attribute: false })
], OrRuleViewer.prototype, "_ruleValid", void 0);
__decorate([
    property({ attribute: false })
], OrRuleViewer.prototype, "_supported", void 0);
__decorate([
    query("#rule-view")
], OrRuleViewer.prototype, "view", void 0);
__decorate([
    query("#main-wrapper")
], OrRuleViewer.prototype, "wrapperElem", void 0);
__decorate([
    query("#save-btn")
], OrRuleViewer.prototype, "saveBtnElem", void 0);
OrRuleViewer = __decorate([
    customElement("or-rule-viewer")
], OrRuleViewer);
export { OrRuleViewer };
//# sourceMappingURL=or-rule-viewer.js.map