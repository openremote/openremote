import { LitElement, PropertyValues, TemplateResult } from "lit";
import { MDCComponent } from "@material/base";
import "@openremote/or-icon";
import { AssetDescriptor, NameHolder, ValueDescriptor, ValueDescriptorHolder, ValueFormat, ValueHolder } from "@openremote/model";
export declare class OrInputChangedEvent extends CustomEvent<OrInputChangedEventDetail> {
    static readonly NAME = "or-mwc-input-changed";
    constructor(value?: any, previousValue?: any, enterPressed?: boolean);
}
export interface OrInputChangedEventDetail {
    value?: any;
    previousValue?: any;
    enterPressed?: boolean;
}
declare global {
    export interface HTMLElementEventMap {
        [OrInputChangedEvent.NAME]: OrInputChangedEvent;
    }
}
export declare enum InputType {
    BUTTON = "button",
    BUTTON_TOGGLE = "button-toggle",
    BUTTON_MOMENTARY = "button-momentary",
    CHECKBOX = "checkbox",
    CHECKBOX_LIST = "checkbox-list",
    COLOUR = "color",
    DATE = "date",
    DATETIME = "datetime-local",
    EMAIL = "email",
    JSON = "json",
    JSON_OBJECT = "json-object",
    MONTH = "month",
    NUMBER = "number",
    BIG_INT = "big-int",
    PASSWORD = "password",
    RADIO = "radio",
    SWITCH = "switch",
    RANGE = "range",
    TELEPHONE = "tel",
    TEXT = "text",
    TEXTAREA = "textarea",
    TIME = "time",
    URL = "url",
    WEEK = "week",
    SELECT = "select",
    LIST = "list",
    CRON = "cron",
    DURATION = "duration",
    DURATION_TIME = "duration-time",
    DURATION_PERIOD = "duration-period"
}
export interface ValueInputProviderOptions {
    label?: string;
    required?: boolean;
    readonly?: boolean;
    disabled?: boolean;
    compact?: boolean;
    rounded?: boolean;
    outlined?: boolean;
    comfortable?: boolean;
    resizeVertical?: boolean;
    inputType?: InputType;
}
export interface ValueInputProvider {
    templateFunction: ValueInputTemplateFunction;
    supportsHelperText: boolean;
    supportsLabel: boolean;
    supportsSendButton: boolean;
    validator?: () => boolean;
}
export type ValueInputTemplateFunction = ((value: any, focused: boolean, loading: boolean, sending: boolean, error: boolean, helperText: string | undefined) => TemplateResult | PromiseLike<TemplateResult>) | undefined;
export type ValueInputProviderGenerator = (assetDescriptor: AssetDescriptor | string, valueHolder: NameHolder & ValueHolder<any> | undefined, valueHolderDescriptor: ValueDescriptorHolder | undefined, valueDescriptor: ValueDescriptor, valueChangeNotifier: (value: OrInputChangedEventDetail | undefined) => void, options: ValueInputProviderOptions) => ValueInputProvider;
export declare const getValueHolderInputTemplateProvider: ValueInputProviderGenerator;
export declare class OrMwcInput extends LitElement {
    static get styles(): import("lit").CSSResult[];
    focused?: boolean;
    value?: any;
    type?: InputType;
    name?: String;
    readonly: boolean;
    required: boolean;
    max?: any;
    min?: any;
    step?: number;
    checked: boolean;
    indeterminate: boolean;
    maxLength?: number;
    minLength?: number;
    rows?: number;
    cols?: number;
    multiple: boolean;
    pattern?: string;
    placeHolder?: string;
    options?: any[] | any;
    autoSelect?: boolean;
    searchProvider?: (search?: string) => Promise<[any, string][]>;
    icon?: string;
    iconColor?: string;
    iconOn?: string;
    iconTrailing?: string;
    compact: boolean;
    comfortable: boolean;
    raised: boolean;
    action: boolean;
    unElevated: boolean;
    outlined: boolean;
    rounded: boolean;
    format?: ValueFormat;
    disableSliderNumberInput: boolean;
    fullWidth: boolean;
    helperText?: string;
    helperPersistent: boolean;
    validationMessage?: string;
    autoValidate: boolean;
    charCounter: boolean;
    label?: string;
    disabled: boolean;
    continuous: boolean;
    resizeVertical: boolean;
    get nativeValue(): any;
    protected _mdcComponent?: MDCComponent<any>;
    protected _mdcComponent2?: MDCComponent<any>;
    protected _selectedIndex: number;
    protected _menuObserver?: IntersectionObserver;
    protected _tempValue: any;
    protected isUiValid: boolean;
    searchableValue?: string;
    protected errorMessage?: string;
    disconnectedCallback(): void;
    protected shouldUpdate(_changedProperties: PropertyValues): boolean;
    focus(): void;
    protected render(): TemplateResult | undefined;
    protected _getFormat(): ValueFormat | undefined;
    update(_changedProperties: PropertyValues): void;
    firstUpdated(_changedProperties: PropertyValues): void;
    protected updated(_changedProperties: PropertyValues): void;
    protected renderOutlined(labelTemplate: TemplateResult | undefined): TemplateResult<1>;
    setCustomValidity(msg: string | undefined): void;
    checkValidity(): boolean;
    reportValidity(): boolean;
    protected onValueChange(elem: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement | undefined, newValue: any | undefined, enterPressed?: boolean): void;
    get valid(): boolean;
    get currentValue(): any;
    protected resolveOptions(options: any[] | undefined): [any, string][] | undefined;
    protected getSelectedTextValue(options?: [string, string][] | undefined): string;
}
