import {css, html, LitElement, PropertyValues, TemplateResult } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import ace, {Ace} from "ace-builds";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";

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

@customElement("or-json-forms-json-editor")
export class JsonEditor extends LitElement {

    static get styles() {
        return style;
    }

    @property({attribute: false})
    public readonly?: boolean;

    @property({type: String, attribute: false})
    public json?: string;

    @query("#ace-editor")
    protected _aceElem?: HTMLElement;
    protected _aceEditor?: Ace.Editor;
    protected _changeTimer?: number;

    disconnectedCallback(): void {
        this.destroyEditor();
        super.disconnectedCallback();
    }

    firstUpdated(_changedProperties: PropertyValues) {
        super.firstUpdated(_changedProperties);
        this.initEditor();
    }

    render(): TemplateResult | void {
        return html`
            <div id="ace-editor"></div>
        `;
    }

    protected _onEditorChanged() {
        if (this._changeTimer) {
            window.clearTimeout(this._changeTimer);
        }
        this._changeTimer = window.setTimeout(() => this._onEdit(), 1000);
    }

    protected _onEdit() {
        //this.dispatchEvent(new OrRulesRuleChangedEvent(this.validate()));
        this._changeTimer = undefined;
    }

    public getValue(): string | undefined {
        if (!this._aceEditor) {
            return undefined;
        }

        return this._aceEditor.getValue();
    }

    public validate(): boolean {

        if (!this._aceEditor) {
            return false;
        }

        const annotations = this._aceEditor.getSession().getAnnotations();
        return !annotations || annotations.length === 0;
    }


    protected destroyEditor() {
        if (this._aceEditor) {
            this._aceEditor.destroy();
            this._aceEditor = undefined;
        }
    }

    protected initEditor() {
        if (this._aceElem) {
            this._aceEditor = ace.edit(this._aceElem, {
                mode: "ace/mode/json",
                value: this.json,
                useSoftTabs: true,
                tabSize: 2,
                readOnly: this.readonly,
                showPrintMargin: false
            });
            this._aceEditor.on("change", () => this._onEditorChanged());
            this._aceEditor.renderer.attachToShadowRoot();
        }
    }
}
