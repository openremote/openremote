import {OrRulesRuleChangedEvent, RulesConfig, RuleView} from "./index";
import {css, customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import {RulesetLang, RulesetUnion} from "@openremote/model";
import ace, {Ace} from "ace-builds";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";

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

    @query("#ace-editor")
    protected _aceElem?: HTMLElement;
    protected _rules?: string;
    protected _aceEditor?: Ace.Editor;
    protected _changeTimer?: number;

    disconnectedCallback(): void {
        if (this._aceEditor) {
            this._aceEditor.destroy();
            this._aceEditor = undefined;
        }
        super.disconnectedCallback();
    }

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
            <div id="ace-editor"></div>
        `;
    }

    protected refresh() {
        this.destoryEditor()
        this.initEditor();
    }

    protected destoryEditor() {
        if (this._aceEditor) {
            this._aceEditor.destroy();
            this._aceEditor = undefined;
        }
    }

    protected initEditor() {
        if (this._aceElem) {
            this._aceEditor = ace.edit(this._aceElem, {
                mode: this._getMode(),
                value: this._getRulesString(),
                useSoftTabs: true,
                tabSize: 2,
                readOnly: this.readonly,
                showPrintMargin: false
            });
            this._aceEditor.on("change", () => this._onEditorChanged());
            this._aceEditor.renderer.attachToShadowRoot();
        }
    }

    protected updated(_changedProperties: PropertyValues): void {
        if(_changedProperties.has('_ruleset')){
            this.refresh();
        }
        
        if (!this._aceElem) {
            this.destoryEditor();
        } else {
            if (!this._aceEditor) {
              this.initEditor();
            }
        }
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

    protected _onEditorChanged() {
        if (this._changeTimer) {
            window.clearTimeout(this._changeTimer);
        }
        this._changeTimer = window.setTimeout(() => this._onEdit(), 1000);
    }

    protected _onEdit() {
        this.dispatchEvent(new OrRulesRuleChangedEvent(this.validate()));
        this._changeTimer = undefined;
    }

    public beforeSave() {
        if (!this._aceEditor) {
            return;
        }

        this._ruleset.rules = this._aceEditor.getValue();
    }

    public validate(): boolean {

        if (!this._aceEditor) {
            return false;
        }

        const annotations = this._aceEditor.getSession().getAnnotations();
        return !annotations || annotations.length === 0;
    }
}
