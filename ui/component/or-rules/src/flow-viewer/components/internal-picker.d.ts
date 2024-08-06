import { LitElement, TemplateResult } from "lit";
import { Node } from "@openremote/model";
import "@openremote/or-asset-tree";
declare const InternalPicker_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class InternalPicker extends InternalPicker_base {
    node: Node;
    internalIndex: number;
    private selectedAsset;
    private resizeObserver;
    constructor();
    get internal(): import("@openremote/model").NodeInternal;
    static get styles(): import("lit").CSSResult[];
    protected firstUpdated(): void;
    protected render(): TemplateResult;
    private setSocketTypeDynamically;
    private setSelectedAssetFromInternalValue;
    private get assetAttributeInput();
    private get colorInput();
    private get doubleDropdownInput();
    private get dropdownInput();
    private get checkBoxInput();
    private get multilineInput();
    private get numberInput();
    private get textInput();
    private setValue;
    private onPicked;
}
export {};
