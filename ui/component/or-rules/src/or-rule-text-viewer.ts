import {OrRulesRuleChangedEvent, RulesConfig, RuleView} from "./index";
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {RulesetLang, RulesetUnion} from "@openremote/model";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";
import "@openremote/or-components/or-ace-editor";
import {OrAceEditor, OrAceEditorChangedEvent} from "@openremote/or-components/or-ace-editor";
import {createRef, ref, Ref} from "lit/directives/ref.js";

// language=CSS
const style = css`
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

@customElement("or-rule-text-viewer")
export class OrRuleTextViewer extends LitElement implements RuleView {

    static get styles() {
        return style;
    }

    @property({attribute: false})
    public readonly?: boolean;

    @property({attribute: false})
    public config?: RulesConfig;

    @property({attribute: false})
    protected _ruleset!: RulesetUnion;

    protected _rules?: string;
    protected _aceEditor: Ref<OrAceEditor> = createRef();

    public set ruleset(ruleset: RulesetUnion) {
        if (this._ruleset === ruleset) {
            return;
        }

        this._ruleset = ruleset;

        if (!ruleset.rules) {
            // New ruleset so start a new rule
            this._rules = this._createRules();
        } else {
            this._rules = ruleset.rules;
        }
    }

    protected _createRules(): string {
        return "";
    }

    protected render(): TemplateResult | void {
        return html`
            <or-ace-editor ${ref(this._aceEditor)} @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => this._onEditorChanged(ev)}" .mode="${this._getMode()}" .value="${this._getRulesString()}"></or-ace-editor>
        `;
    }

    protected _getMode() {
        switch (this._ruleset.lang) {
            case RulesetLang.JAVASCRIPT:
                return "ace/mode/javascript";
            case RulesetLang.GROOVY:
                return "ace/mode/groovy";
            case RulesetLang.JSON:
                return "ace/mode/json";
        }
    }

    protected _getRulesString() {
        if (!this._rules) {
            return "";
        }

        switch (this._ruleset.lang) {
            case RulesetLang.JAVASCRIPT:
            case RulesetLang.GROOVY:
                return this._rules;
            case RulesetLang.JSON:
                return JSON.stringify(JSON.parse(this._rules), null, 2);
        }
    }

    protected _onEditorChanged(ev: OrAceEditorChangedEvent) {
        const valid = ev.detail.valid;
        this.dispatchEvent(new OrRulesRuleChangedEvent(valid));
    }

    public beforeSave() {
        if (!this._aceEditor.value) {
            return;
        }

        this._ruleset.rules = this._aceEditor.value.getValue();
    }

    public validate(): boolean {
        return this._aceEditor.value ? this._aceEditor.value.validate() : false;
    }
}
