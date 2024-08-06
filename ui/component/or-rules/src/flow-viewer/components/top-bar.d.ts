import { LitElement } from "lit";
import { FlowEditor } from "./flow-editor";
declare const TopBar_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class TopBar extends TopBar_base {
    static get styles(): import("lit").CSSResult;
    application: FlowEditor;
    protected firstUpdated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private save;
    private showRuleBrowser;
    private showSaveAsDialog;
}
export {};
