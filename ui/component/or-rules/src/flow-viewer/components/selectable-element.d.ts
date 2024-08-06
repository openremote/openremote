import { LitElement } from "lit";
declare const SelectableElement_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class SelectableElement extends SelectableElement_base {
    get selected(): boolean;
    get handle(): HTMLElement;
    private isSelected;
    private selectableHandle;
    disconnectedCallback(): void;
    setHandle(element: HTMLElement): void;
    protected firstUpdated(): void;
    private readonly onSelected;
    private readonly onDeselected;
    private readonly handleSelection;
}
export {};
