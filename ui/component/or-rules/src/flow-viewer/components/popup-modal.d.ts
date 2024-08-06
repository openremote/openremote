import { LitElement, TemplateResult } from "lit";
declare const PopupModal_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class PopupModal extends PopupModal_base {
    closeButton: boolean;
    header?: string;
    content?: TemplateResult;
    private isOpen;
    constructor();
    close(): void;
    open(): void;
    static get styles(): import("lit").CSSResult;
    protected firstUpdated(): void;
    protected render(): TemplateResult<1>;
}
export {};
