var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import ace from "ace-builds";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";
export class OrAceEditorChangedEvent extends CustomEvent {
    constructor(value, valid) {
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
OrAceEditorChangedEvent.NAME = "or-ace-editor-changed";
export class OrAceEditorEditEvent extends CustomEvent {
    constructor() {
        super(OrAceEditorEditEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrAceEditorEditEvent.NAME = "or-ace-editor-edit";
let OrAceEditor = class OrAceEditor extends LitElement {
    constructor() {
        super(...arguments);
        this.mode = "ace/mode/json";
        this._lastValue = "";
        this._editing = false;
    }
    // language=CSS
    static get styles() {
        return css `
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
    disconnectedCallback() {
        this.destroyEditor();
        super.disconnectedCallback();
    }
    updated(_changedProperties) {
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
    render() {
        return html `
            <div id="ace-editor"></div>
        `;
    }
    destroyEditor() {
        if (this._aceEditor) {
            this._aceEditor.destroy();
            this._aceEditor = undefined;
        }
    }
    initEditor() {
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
    _onEditorEdit() {
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
    _onEditorChange() {
        this._editing = false;
        const newValue = this.getValue() || "";
        if (this._lastValue !== newValue) {
            this._lastValue = newValue;
            const valid = this.validate();
            this.dispatchEvent(new OrAceEditorChangedEvent(newValue, valid));
        }
    }
    getValue() {
        if (!this._aceEditor) {
            return undefined;
        }
        return this._aceEditor.getValue();
    }
    validate() {
        if (!this._aceEditor) {
            return false;
        }
        const annotations = this._aceEditor.getSession().getAnnotations();
        return !annotations || annotations.length === 0;
    }
};
__decorate([
    property({ type: Boolean, attribute: false })
], OrAceEditor.prototype, "readonly", void 0);
__decorate([
    property({ attribute: false })
], OrAceEditor.prototype, "value", void 0);
__decorate([
    property({ type: String, attribute: false })
], OrAceEditor.prototype, "mode", void 0);
__decorate([
    query("#ace-editor")
], OrAceEditor.prototype, "_aceElem", void 0);
OrAceEditor = __decorate([
    customElement("or-ace-editor")
], OrAceEditor);
export { OrAceEditor };
//# sourceMappingURL=or-ace-editor.js.map