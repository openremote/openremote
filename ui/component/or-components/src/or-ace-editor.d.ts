/// <reference types="ace-builds/ace-modules" />
import { LitElement, PropertyValues, TemplateResult } from "lit";
import { Ace } from "ace-builds";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";
export declare class OrAceEditorChangedEvent extends CustomEvent<{
    value: string;
    valid: boolean;
}> {
    static readonly NAME = "or-ace-editor-changed";
    constructor(value: string, valid: boolean);
}
export declare class OrAceEditorEditEvent extends CustomEvent<void> {
    static readonly NAME = "or-ace-editor-edit";
    constructor();
}
declare global {
    export interface HTMLElementEventMap {
        [OrAceEditorChangedEvent.NAME]: OrAceEditorChangedEvent;
        [OrAceEditorEditEvent.NAME]: OrAceEditorEditEvent;
    }
}
export declare class OrAceEditor extends LitElement {
    static get styles(): import("lit").CSSResult;
    readonly?: boolean;
    value?: any;
    mode: string;
    protected _aceElem?: HTMLElement;
    protected _aceEditor?: Ace.Editor;
    protected _lastValue: string;
    protected _editing: boolean;
    protected _changeTimer?: number;
    disconnectedCallback(): void;
    updated(_changedProperties: PropertyValues): void;
    render(): TemplateResult | void;
    protected destroyEditor(): void;
    protected initEditor(): void;
    protected _onEditorEdit(): void;
    protected _onEditorChange(): void;
    getValue(): string | undefined;
    validate(): boolean;
}
