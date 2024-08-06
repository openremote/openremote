import { LitElement } from "lit";
declare const ConfirmationDialog_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class ConfirmationDialog extends ConfirmationDialog_base {
    agreeText: string;
    disagreeText: string;
    question: string;
    static get styles(): import("lit").CSSResult;
    protected render(): import("lit-html").TemplateResult<1>;
}
export {};
