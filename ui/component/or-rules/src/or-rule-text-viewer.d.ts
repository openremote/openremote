/// <reference types="ace-builds/ace-modules" />
import { RulesConfig, RuleView } from "./index";
import { LitElement, TemplateResult } from "lit";
import { RulesetUnion } from "@openremote/model";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import "ace-builds/webpack-resolver";
import "@openremote/or-components/or-ace-editor";
import { OrAceEditor, OrAceEditorChangedEvent } from "@openremote/or-components/or-ace-editor";
import { Ref } from "lit/directives/ref.js";
export declare class OrRuleTextViewer extends LitElement implements RuleView {
    static get styles(): import("lit").CSSResult;
    readonly?: boolean;
    config?: RulesConfig;
    protected _ruleset: RulesetUnion;
    protected _rules?: string;
    protected _aceEditor: Ref<OrAceEditor>;
    set ruleset(ruleset: RulesetUnion);
    protected _createRules(): string;
    protected render(): TemplateResult | void;
    protected _getMode(): "ace/mode/javascript" | "ace/mode/groovy" | "ace/mode/json" | undefined;
    protected _getRulesString(): string | undefined;
    protected _onEditorChanged(ev: OrAceEditorChangedEvent): void;
    beforeSave(): void;
    validate(): boolean;
}
