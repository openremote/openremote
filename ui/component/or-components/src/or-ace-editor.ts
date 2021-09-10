import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import ace, {Ace} from "ace-builds";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";

export class OrAceEditorChangedEvent extends CustomEvent<{ value: string, valid: boolean }> {

    public static readonly NAME = "or-ace-editor-changed";

    constructor(value: string, valid: boolean) {
        super(OrAceEditorChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                value: value,
                valid: valid
            }
        });
    }
}

export class OrAceEditorEditEvent extends CustomEvent<void> {

    public static readonly NAME = "or-ace-editor-edit";

    constructor() {
        super(OrAceEditorEditEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAceEditorChangedEvent.NAME]: OrAceEditorChangedEvent;
        [OrAceEditorEditEvent.NAME]: OrAceEditorEditEvent;
    }
}

@customElement("or-ace-editor")
export class OrAceEditor extends LitElement {

    // language=CSS
    static get styles() {
        return css`
            :host {
                display: block;
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
    }

    @property({type: Boolean, attribute: false})
    public readonly?: boolean;

    @property({attribute: false})
    public value?: any;

    @property({type: String, attribute: false})
    public mode: string = "ace/mode/json";

    @query("#ace-editor")
    protected _aceElem?: HTMLElement;
    protected _aceEditor?: Ace.Editor;
    protected _lastValue: string = "";
    protected _editing: boolean = false;
    protected _changeTimer?: number;

    disconnectedCallback(): void {
        this.destroyEditor();
        super.disconnectedCallback();
    }

    updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);

        if (_changedProperties.has("mode")) {
            this.destroyEditor();
            this.initEditor();
        }

        if (_changedProperties.has("value")) {
            if (this._aceEditor) {
                this._lastValue = this.value !== undefined ? typeof this.value === "string" ? this.value : JSON.stringify(this.value, null, 2) : "";
                this._aceEditor.setValue(this._lastValue);
            }
        }
    }

    render(): TemplateResult | void {
        return html`
            <div id="ace-editor"></div>
        `;
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
                mode: this.mode,
                value: this._lastValue,
                useSoftTabs: true,
                tabSize: 2,
                readOnly: this.readonly,
                showPrintMargin: false
            });
            this._aceEditor.renderer.attachToShadowRoot();
            // Use the changeAnnotation event instead of change event so we have up to date validation info
            // @ts-ignore
            this._aceEditor.getSession().on("changeAnnotation", () => this._onEditorChange());
            this._aceEditor.on("change", () => this._onEditorEdit());
        }
    }

    protected _onEditorEdit() {
        if (!this._editing) {
            this.dispatchEvent(new OrAceEditorEditEvent());
            this._editing = true;
        }
        if (this._changeTimer) {
            window.clearTimeout(this._changeTimer);
        }
        // Set change timer to expire after annotation web worker
        this._changeTimer = window.setTimeout(() => {
            this._changeTimer = undefined;
            if (this._editing) {
                this._onEditorChange();
            }
        }, 600);
    }

    protected _onEditorChange() {
        this._editing = false;
        const newValue = this.getValue() || "";
        if (this._lastValue !== newValue) {
            this._lastValue = newValue;
            const valid = this.validate();
            this.dispatchEvent(new OrAceEditorChangedEvent(newValue, valid));
        }
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
}
