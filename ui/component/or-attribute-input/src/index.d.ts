import { LitElement, PropertyValues, TemplateResult } from "lit";
import { Attribute, AttributeDescriptor, AttributeEvent, AttributeRef, SharedEvent, ValueDescriptor } from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-loading-wrapper";
import { InputType, ValueInputProvider, ValueInputProviderGenerator } from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-map";
import "@openremote/or-json-forms";
export declare class OrAttributeInputChangedEvent extends CustomEvent<OrAttributeInputChangedEventDetail> {
    static readonly NAME = "or-attribute-input-changed";
    constructor(value?: any, previousValue?: any);
}
export interface OrAttributeInputChangedEventDetail {
    value?: any;
    previousValue?: any;
}
declare global {
    export interface HTMLElementEventMap {
        [OrAttributeInputChangedEvent.NAME]: OrAttributeInputChangedEvent;
    }
}
export declare function getAttributeInputWrapper(content: TemplateResult, value: any, loading: boolean, disabled: boolean, helperText: string | undefined, label: string | undefined, buttonIcon?: string, sendValue?: () => void, fullWidth?: boolean): TemplateResult;
export declare function getHelperText(sending: boolean, error: boolean, timestamp: number | undefined): string | undefined;
export declare const jsonFormsInputTemplateProvider: (fallback: ValueInputProvider) => ValueInputProviderGenerator;
declare const OrAttributeInput_base: (new (...args: any[]) => {
    _connectRequested: boolean;
    _subscriptionIds?: string[] | undefined;
    _assetIds?: string[] | undefined;
    _attributeRefs?: AttributeRef[] | undefined;
    _status: import("@openremote/core").EventProviderStatus;
    _statusCallback: (status: import("@openremote/core").EventProviderStatus) => void;
    connectedCallback(): void;
    disconnectedCallback(): void;
    connectEvents(): void;
    disconnectEvents(): void;
    _doConnect(): Promise<void>;
    readonly eventsConnected: boolean;
    _onEventProviderStatusChanged(status: import("@openremote/core").EventProviderStatus): void;
    _onEventsConnect(): void;
    _onEventsDisconnect(): void;
    _addEventSubscriptions(): Promise<void>;
    _removeEventSubscriptions(): void;
    _refreshEventSubscriptions(): void;
    assetIds: string[] | undefined;
    attributeRefs: AttributeRef[] | undefined;
    _sendEvent(event: SharedEvent): void;
    _sendEventWithReply<U extends SharedEvent, V extends SharedEvent>(event: import("@openremote/model").EventRequestResponseWrapper<U>): Promise<V>;
    onEventsConnect(): void;
    onEventsDisconnect(): void;
    _onEvent(event: SharedEvent): void;
    requestUpdate(name?: PropertyKey | undefined, oldValue?: unknown): void;
    readonly isConnected: boolean;
}) & (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class OrAttributeInput extends OrAttributeInput_base {
    static get styles(): import("lit").CSSResult[];
    attribute?: Attribute<any>;
    assetId?: string;
    attributeDescriptor?: AttributeDescriptor;
    valueDescriptor?: ValueDescriptor;
    assetType?: string;
    label?: string;
    disabled?: boolean;
    readonly?: boolean;
    required?: boolean;
    value?: any;
    inputType?: InputType;
    hasHelperText?: boolean;
    disableButton?: boolean;
    disableSubscribe: boolean;
    disableWrite: boolean;
    compact: boolean;
    comfortable: boolean;
    resizeVertical: boolean;
    fullWidth?: boolean;
    rounded?: boolean;
    outlined?: boolean;
    protected _attributeEvent?: AttributeEvent;
    protected _writeTimeoutHandler?: number;
    customProvider?: ValueInputProviderGenerator;
    writeTimeout?: number;
    protected _requestFocus: boolean;
    protected _newValue: any;
    protected _templateProvider?: ValueInputProvider;
    protected _sendError: boolean;
    protected _attributeDescriptor?: AttributeDescriptor;
    protected _valueDescriptor?: ValueDescriptor;
    disconnectedCallback(): void;
    langChangedCallback: () => void;
    shouldUpdate(_changedProperties: PropertyValues): boolean;
    protected _updateSubscribedRefs(): void;
    protected _getAttributeRef(): AttributeRef | undefined;
    protected _updateDescriptors(): void;
    protected _updateTemplate(): void;
    getLabel(): string | undefined;
    isReadonly(): boolean;
    render(): TemplateResult;
    protected updated(_changedProperties: PropertyValues): void;
    protected get showButton(): boolean;
    protected getValue(): any;
    protected getTimestamp(): number | undefined;
    /**
     * This is called by asset-mixin
     */
    _onEvent(event: SharedEvent): void;
    checkValidity(): boolean;
    protected _onAttributeValueChanged(oldValue: any, newValue: any, timestamp?: number): void;
    protected _onInputValueChanged(value: any, updateImmediately: boolean): void;
    protected _updateValue(): void;
    protected _clearWriteTimeout(): void;
    protected _onWriteTimeout(): void;
}
export {};
