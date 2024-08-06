var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { OrRulesRuleChangedEvent } from "./index";
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";
import "@openremote/or-components/or-ace-editor";
import { createRef, ref } from "lit/directives/ref.js";
// language=CSS
const style = css `
    :host {
        display: flex;
        width: 100%;
        height: 100%;
    }

    #ace-editor {
        position: relative;
        height: 100%;
        width: 100%;
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
let OrRuleTextViewer = class OrRuleTextViewer extends LitElement {
    constructor() {
        super(...arguments);
        this._aceEditor = createRef();
    }
    static get styles() {
        return style;
    }
    set ruleset(ruleset) {
        if (this._ruleset === ruleset) {
            return;
        }
        this._ruleset = ruleset;
        if (!ruleset.rules) {
            // New ruleset so start a new rule
            this._rules = this._createRules();
        }
        else {
            this._rules = ruleset.rules;
        }
    }
    _createRules() {
        return "";
    }
    render() {
        return html `
            <or-ace-editor ${ref(this._aceEditor)} @or-ace-editor-changed="${(ev) => this._onEditorChanged(ev)}" .mode="${this._getMode()}" .value="${this._getRulesString()}"></or-ace-editor>
        `;
    }
    _getMode() {
        switch (this._ruleset.lang) {
            case "JAVASCRIPT" /* RulesetLang.JAVASCRIPT */:
                return "ace/mode/javascript";
            case "GROOVY" /* RulesetLang.GROOVY */:
                return "ace/mode/groovy";
            case "JSON" /* RulesetLang.JSON */:
                return "ace/mode/json";
        }
    }
    _getRulesString() {
        if (!this._rules) {
            return "";
        }
        switch (this._ruleset.lang) {
            case "JAVASCRIPT" /* RulesetLang.JAVASCRIPT */:
            case "GROOVY" /* RulesetLang.GROOVY */:
                return this._rules;
            case "JSON" /* RulesetLang.JSON */:
                return JSON.stringify(JSON.parse(this._rules), null, 2);
        }
    }
    _onEditorChanged(ev) {
        const valid = ev.detail.valid;
        this.dispatchEvent(new OrRulesRuleChangedEvent(valid));
    }
    beforeSave() {
        if (!this._aceEditor.value) {
            return;
        }
        this._ruleset.rules = this._aceEditor.value.getValue();
    }
    validate() {
        return this._aceEditor.value ? this._aceEditor.value.validate() : false;
    }
};
__decorate([
    property({ attribute: false })
], OrRuleTextViewer.prototype, "readonly", void 0);
__decorate([
    property({ attribute: false })
], OrRuleTextViewer.prototype, "config", void 0);
__decorate([
    property({ attribute: false })
], OrRuleTextViewer.prototype, "_ruleset", void 0);
OrRuleTextViewer = __decorate([
    customElement("or-rule-text-viewer")
], OrRuleTextViewer);
export { OrRuleTextViewer };
//# sourceMappingURL=or-rule-text-viewer.js.map