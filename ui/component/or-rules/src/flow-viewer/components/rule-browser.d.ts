import { LitElement } from "lit";
declare const RuleBrowser_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class RuleBrowser extends RuleBrowser_base {
    private status;
    private retrievedRules;
    static get styles(): import("lit").CSSResult;
    protected firstUpdated(): Promise<void>;
    protected render(): import("lit-html").TemplateResult<1>;
    private loadRule;
    private getButton;
}
export {};
