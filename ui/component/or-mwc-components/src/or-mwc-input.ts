import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {Ref, ref, createRef} from "lit/directives/ref.js";
import {classMap} from "lit/directives/class-map.js";
import {ifDefined} from "lit/directives/if-defined.js";
import {when} from 'lit/directives/when.js';
import {until} from 'lit/directives/until.js';
import {MDCTextField} from "@material/textfield";
import {MDCComponent} from "@material/base";
import {MDCRipple} from "@material/ripple";
import {MDCCheckbox} from "@material/checkbox";
import {MDCSwitch} from "@material/switch";
import {MDCSlider, MDCSliderChangeEventDetail} from "@material/slider";
import {MDCSelect, MDCSelectEvent} from "@material/select";
import {MDCList, MDCListActionEvent} from "@material/list";

import {MDCFormField, MDCFormFieldInput} from "@material/form-field";
import {MDCIconButtonToggle, MDCIconButtonToggleEventDetail} from "@material/icon-button";
import {DefaultColor4, DefaultColor5, DefaultColor8, Util} from "@openremote/core";
import "@openremote/or-icon";
import {OrIcon} from "@openremote/or-icon";
import {
    AssetDescriptor,
    Attribute,
    AttributeDescriptor,
    MetaHolder,
    NameHolder,
    NameValueHolder,
    ValueConstraint,
    ValueConstraintAllowedValues,
    ValueConstraintFuture,
    ValueConstraintFutureOrPresent,
    ValueConstraintMax,
    ValueConstraintMin,
    ValueConstraintNotBlank,
    ValueConstraintNotEmpty,
    ValueConstraintNotNull,
    ValueConstraintPast,
    ValueConstraintPastOrPresent,
    ValueConstraintPattern,
    ValueConstraintSize,
    ValueDescriptor,
    ValueDescriptorHolder,
    ValueFormat,
    ValueFormatStyleRepresentation,
    ValueHolder,
    WellknownMetaItems,
    WellknownValueTypes
} from "@openremote/model";
import {getItemTemplate, getListTemplate, ListItem, ListType} from "./or-mwc-list";
import { i18next } from "@openremote/or-translate";
import { styleMap } from "lit/directives/style-map.js";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const buttonStyle = require("@material/button/dist/mdc.button.css");
const buttonFabStyle = require("@material/fab/dist/mdc.fab.css");
const iconButtonStyle = require("@material/icon-button/dist/mdc.icon-button.css");
const textfieldStyle = require("@material/textfield/dist/mdc.textfield.css");
const rippleStyle = require("@material/ripple/dist/mdc.ripple.css");
const lineRippleStyle = require("@material/line-ripple/dist/mdc.line-ripple.css");
const floatingLabelStyle = require("@material/floating-label/dist/mdc.floating-label.css");
const formFieldStyle = require("@material/form-field/dist/mdc.form-field.css");
const checkboxStyle = require("@material/checkbox/dist/mdc.checkbox.css");
const radioStyle = require("@material/radio/dist/mdc.radio.css");
const switchStyle = require("@material/switch/dist/mdc.switch.css");
const selectStyle = require("@material/select/dist/mdc.select.css");
const listStyle = require("@material/list/dist/mdc.list.css");
const menuSurfaceStyle = require("@material/menu-surface/dist/mdc.menu-surface.css");
const menuStyle = require("@material/menu/dist/mdc.menu.css");
const sliderStyle = require("@material/slider/dist/mdc.slider.css");

export class OrInputChangedEvent extends CustomEvent<OrInputChangedEventDetail> {

    public static readonly NAME = "or-mwc-input-changed";

    constructor(value?: any, previousValue?: any, enterPressed?: boolean) {
        super(OrInputChangedEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue,
                enterPressed: enterPressed
            },
            bubbles: true,
            composed: true
        });
    }
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

export enum InputType {
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

function inputTypeSupportsButton(inputType: InputType): boolean {
    return inputType === InputType.NUMBER
        || inputType === InputType.BIG_INT
        || inputType === InputType.TELEPHONE
        || inputType === InputType.TEXT
        || inputType === InputType.PASSWORD
        || inputType === InputType.DATE
        || inputType === InputType.DATETIME
        || inputType === InputType.EMAIL
        || inputType === InputType.JSON
        || inputType === InputType.JSON_OBJECT
        || inputType === InputType.MONTH
        || inputType === InputType.TEXTAREA
        || inputType === InputType.TIME
        || inputType === InputType.URL
        || inputType === InputType.WEEK;
}

function inputTypeSupportsHelperText(inputType: InputType) {
    return inputTypeSupportsButton(inputType) || inputType === InputType.SELECT;
}

function inputTypeSupportsLabel(inputType: InputType) {
    return inputTypeSupportsHelperText(inputType) || inputType === InputType.CHECKBOX || inputType === InputType.BUTTON_MOMENTARY;
}

export const getValueHolderInputTemplateProvider: ValueInputProviderGenerator = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {

    let inputType: InputType | undefined = options.inputType;
    let step: number | undefined;
    let pattern: string | undefined;
    let min: any;
    let max: any;
    let multiple: any;
    let required: boolean | undefined;
    let selectOptions: [string, string][] | undefined;
    let valueConverter: (v: any) => any | undefined;
    const styles = {} as any;

    const assetType = typeof assetDescriptor === "string" ? assetDescriptor : assetDescriptor.name;
    const constraints: ValueConstraint[] = (valueHolder && ((valueHolder as MetaHolder).meta) || (valueDescriptor && (valueDescriptor as MetaHolder).meta) ? Util.getAttributeValueConstraints(valueHolder as Attribute<any>, valueHolderDescriptor as AttributeDescriptor, assetType) : Util.getMetaValueConstraints(valueHolder as NameValueHolder<any>, valueHolderDescriptor as AttributeDescriptor, assetType)) || [];
    const format: ValueFormat | undefined = (valueHolder && ((valueHolder as MetaHolder).meta) || (valueDescriptor && (valueDescriptor as MetaHolder).meta) ? Util.getAttributeValueFormat(valueHolder as Attribute<any>, valueHolderDescriptor as AttributeDescriptor, assetType) : Util.getMetaValueFormat(valueHolder as Attribute<any>, valueHolderDescriptor as AttributeDescriptor, assetType));

    // Determine input type
    if (!inputType) {
        switch (valueDescriptor.name) {
            case WellknownValueTypes.TEXT:
            case WellknownValueTypes.EMAIL:
            case WellknownValueTypes.UUID:
            case WellknownValueTypes.ASSETID:
            case WellknownValueTypes.HOSTORIPADDRESS:
            case WellknownValueTypes.IPADDRESS:
                inputType = Util.getMetaValue(WellknownMetaItems.MULTILINE, valueHolder, valueHolderDescriptor) === true ? InputType.TEXTAREA : InputType.TEXT;
                break;
            case WellknownValueTypes.BOOLEAN:
                if (format && format.asNumber) {
                    inputType = InputType.NUMBER;
                    step = 1;
                    min = 0;
                    max = 1;
                    valueConverter = (v) => !!v;
                    break;
                }
                if (format && (format.asOnOff || format.asOpenClosed)) {
                    inputType = InputType.SWITCH;
                } else {
                    inputType = InputType.CHECKBOX;
                }

                if (format && format.asMomentary || (Util.getMetaValue(WellknownMetaItems.MOMENTARY, valueHolder, valueHolderDescriptor) === true) ) {
                    inputType = InputType.BUTTON_MOMENTARY;
                }
                break;
            case WellknownValueTypes.BIGNUMBER:
            case WellknownValueTypes.NUMBER:
            case WellknownValueTypes.POSITIVEINTEGER:
            case WellknownValueTypes.POSITIVENUMBER:
            case WellknownValueTypes.LONG:
            case WellknownValueTypes.INTEGER:
            case WellknownValueTypes.BYTE:
            case WellknownValueTypes.INTEGERBYTE:
            case WellknownValueTypes.DIRECTION:
            case WellknownValueTypes.TCPIPPORTNUMBER:
                if (valueDescriptor.name === WellknownValueTypes.BYTE || valueDescriptor.name === WellknownValueTypes.INTEGERBYTE) {
                    min = 0;
                    max = 255;
                    step = 1;
                } else if (valueDescriptor.name === WellknownValueTypes.INTEGER || valueDescriptor.name === WellknownValueTypes.LONG) {
                    step = 1;
                }
                if (format && format.asDate) {
                    inputType = InputType.DATETIME;
                } else if (format && format.asBoolean) {
                    inputType = InputType.CHECKBOX;
                    valueConverter = (v) => v ? 1 : 0;
                } else if (format && format.asSlider) {
                    inputType = InputType.RANGE;
                } else {
                    inputType = InputType.NUMBER;
                }
                break;
            case WellknownValueTypes.BIGINTEGER:
                inputType = InputType.BIG_INT;
                step = 1;
                break;
            case WellknownValueTypes.COLOURRGB:
                inputType = InputType.COLOUR;
                break;
            case WellknownValueTypes.DATEANDTIME:
            case WellknownValueTypes.TIMESTAMP:
            case WellknownValueTypes.TIMESTAMPISO8601:
                inputType = InputType.DATETIME;
                break;
            case WellknownValueTypes.CRONEXPRESSION:
                inputType = InputType.CRON;
                break;
            case WellknownValueTypes.TIMEDURATIONISO8601:
                inputType = InputType.DURATION_TIME;
                break;
            case WellknownValueTypes.PERIODDURATIONISO8601:
                inputType = InputType.DURATION_PERIOD;
                break;
            case WellknownValueTypes.TIMEANDPERIODDURATIONISO8601:
                inputType = InputType.DURATION;
                break;
            case WellknownValueTypes.JSONOBJECT:
                inputType = InputType.JSON_OBJECT;
                break;
        }

        if (valueDescriptor.arrayDimensions && valueDescriptor.arrayDimensions > 0) {
            inputType = InputType.JSON;
        }
    }

    if (!inputType) {
        switch (valueDescriptor.jsonType) {
            case "number":
            case "bigint":
                inputType = InputType.NUMBER;
                break;
            case "boolean":
                inputType = InputType.CHECKBOX;
                break;
            case "string":
                inputType = InputType.TEXT;
                break;
            case "date":
                inputType = InputType.DATETIME;
                break;
        }
    }

    if (!inputType) {
        inputType = InputType.JSON;
    }

    // Apply any constraints
    const sizeConstraint = constraints && constraints.find(c => c.type === "size") as ValueConstraintSize;
    const patternConstraint = constraints && constraints.find(c => c.type === "pattern") as ValueConstraintPattern;
    const minConstraint = constraints && constraints.find(c => c.type === "min") as ValueConstraintMin;
    const maxConstraint = constraints && constraints.find(c => c.type === "max") as ValueConstraintMax;
    const allowedValuesConstraint = constraints && constraints.find(c => c.type === "allowedValues") as ValueConstraintAllowedValues;
    const pastConstraint = constraints && constraints.find(c => c.type === "past") as ValueConstraintPast;
    const pastOrPresentConstraint = constraints && constraints.find(c => c.type === "pastOrPresent") as ValueConstraintPastOrPresent;
    const futureConstraint = constraints && constraints.find(c => c.type === "future") as ValueConstraintFuture;
    const futureOrPresentConstraint = constraints && constraints.find(c => c.type === "futureOrPresent") as ValueConstraintFutureOrPresent;
    const notEmptyConstraint = constraints && constraints.find(c => c.type === "notEmpty") as ValueConstraintNotEmpty;
    const notBlankConstraint = constraints && constraints.find(c => c.type === "notBlank") as ValueConstraintNotBlank;
    const notNullConstraint = constraints && constraints.find(c => c.type === "notNull") as ValueConstraintNotNull;

    if (sizeConstraint) {
        min = sizeConstraint.min;
        max = sizeConstraint.max;
    }
    if (sizeConstraint) {
        min = sizeConstraint.min;
        max = sizeConstraint.max;
    }
    if (minConstraint) {
        min = minConstraint.min;
    }
    if (maxConstraint) {
        max = maxConstraint.max;
    }
    if (patternConstraint) {
        pattern = patternConstraint.regexp;
    }
    if (notNullConstraint) {
        required = true;
    }
    if (notBlankConstraint && !pattern) {
        pattern = "\\S+";
    } else if (notEmptyConstraint && !pattern) {
        pattern = ".+";
    }
    if (allowedValuesConstraint && allowedValuesConstraint.allowedValues) {
        const allowedLabels = allowedValuesConstraint.allowedValueNames && allowedValuesConstraint.allowedValueNames.length === allowedValuesConstraint.allowedValues.length ? allowedValuesConstraint.allowedValueNames : undefined;
        selectOptions = allowedValuesConstraint.allowedValues.map((v, i) => {
            let label = allowedLabels ? allowedLabels[i] : "" + v;
            label = Util.getAllowedValueLabel(label)!;
            return [v, label || "" + v];
        });
        inputType = InputType.SELECT;

        if (valueDescriptor.arrayDimensions && valueDescriptor.arrayDimensions > 0) {
            multiple = true;
        }
    }

    if (inputType === InputType.DATETIME) {
        if (pastConstraint || pastOrPresentConstraint) {
            min = undefined;
            max = new Date();
        } else if (futureConstraint || futureOrPresentConstraint) {
            min = new Date();
            max = undefined;
        }

        // Refine the input type based on formatting
        if (format) {
            if (format.timeStyle && !format.dateStyle) {
                inputType = InputType.TIME;
            } else if (format.dateStyle && !format.timeStyle) {
                inputType = InputType.DATE;
            }
        }
    }

    if (inputType === InputType.NUMBER && format && format.resolution) {
        step = format.resolution;
    }

    if (inputType === InputType.COLOUR) {
        styles.marginLeft = "24px"
    }

    const supportsHelperText = inputTypeSupportsHelperText(inputType);
    const supportsLabel = inputTypeSupportsLabel(inputType);
    const supportsSendButton = inputTypeSupportsButton(inputType);
    const readonly = options.readonly;
    required = required || options.required;
    const comfortable = options.comfortable;
    const resizeVertical = options.resizeVertical;
    const inputRef: Ref<OrMwcInput> = createRef();

    const templateFunction: ValueInputTemplateFunction = (value, focused, loading, sending, error, helperText) => {

        const disabled = options.disabled || loading || sending;
        const label = supportsLabel ? options.label : undefined;

        return html`<or-mwc-input ${ref(inputRef)} id="input" style="${styleMap(styles)}" .type="${inputType}" .label="${label}" .value="${value}" .pattern="${pattern}"
            .min="${min}" .max="${max}" .format="${format}" .focused="${focused}" .required="${required}" .multiple="${multiple}"
            .options="${selectOptions}" .comfortable="${comfortable}" .readonly="${readonly}" .disabled="${disabled}" .step="${step}"
            .helperText="${helperText}" .helperPersistent="${true}" .resizeVertical="${resizeVertical}"
            .rounded="${options.rounded}"
            .outlined="${options.outlined}"
            @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                e.stopPropagation();
                e.detail.value = valueConverter ? valueConverter(e.detail.value) : e.detail.value;
                valueChangeNotifier(e.detail);
            }}"></or-mwc-input>`
    };

    return {
        templateFunction: templateFunction,
        supportsHelperText: supportsHelperText,
        supportsSendButton: supportsSendButton,
        supportsLabel: supportsLabel,
        validator: () => {
            if (!inputRef.value) {
                return false;
            }
            return inputRef.value.checkValidity();
        }
    };
}

// language=CSS
const style = css`
    
    :host {
        display: inline-block;
        --internal-or-mwc-input-color: var(--or-mwc-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-mwc-input-text-color: var(--or-mwc-input-text-color, var(--or-app-color8, ${unsafeCSS(DefaultColor8)}));    
        
        --mdc-theme-primary: var(--internal-or-mwc-input-color);
        --mdc-theme-on-primary: var(--internal-or-mwc-input-text-color);
        --mdc-theme-secondary: var(--internal-or-mwc-input-color);
    }
    
    :host([hidden]) {
        display: none;
    }    

    :host([type=select]) {
        height: 56px;
    }
     
    #wrapper {
        display: flex;
        align-items: center;
        min-height: 48px;
        height: 100%;
    }   
    
    #wrapper > label {
        white-space: nowrap;
        margin-right: 20px;
    }
    
    #component {
        max-width: 100%;
    }

    .mdc-text-field {
        flex: 1 1 0;
    }

    .mdc-list {
        flex: 1;
        overflow: auto;
    }

    .mdc-select__anchor {
        max-width: 100%;
        width: 100%;
    }
    
    .mdc-checkbox-list input {
        display: none;
    }
    
    .mdc-checkbox-list label {
        display: block;
        border-radius: 50%;
        text-align: center;
        width: 32px;
        line-height: 32px;
        height: 32px;
        cursor: pointer;
        background-color: var(--or-app-color2);
        font-size: 13px;
    }
    
    input::-webkit-calendar-picker-indicator {
        margin: 0;
    }
    
    .mdc-checkbox-list .mdc-checkbox {
        padding: 0;
        height: 32px;
        width: 32px;
    }
    .mdc-radio-container {
        display: flex;
        flex-direction: column;
    }
    .mdc-text-field.mdc-text-field--invalid:not(.mdc-text-field--disabled) + .mdc-text-field-helper-line .mdc-text-field-helper-text {
        color: var(--mdc-theme-error, #b00020)
    }
    
    .mdc-checkbox-list input:checked + label {
        color: var(--or-app-color2);
        background-color: var(--mdc-theme-primary);
    }
    
    .mdc-button--rounded,
    .or-mwc-input--rounded {
      border-radius: 24px !important;
      --mdc-shape-small: 32px;
    }

    #select-searchable {
        background-color: transparent; 
        border: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        margin: 8px;
        width: calc(100% - 16px); 
        border-radius: 4px;
        padding: 4px 16px;
        flex: 0 0 auto;
        align-items: center;
        height: auto;
    }

    .mdc-text-field__input::-webkit-calendar-picker-indicator {
        display: block;
    }

    ::-webkit-clear-button {display: none;}
    ::-webkit-inner-spin-button { display: none; }
    ::-webkit-datetime-edit { padding: 0em; }
    ::-webkit-datetime-edit-text { padding: 0; }

    .mdc-text-field--focused:not(.mdc-text-field--disabled) .mdc-floating-label {
        color: var(--mdc-theme-primary);
    }
    .mdc-text-field--focused .mdc-text-field__input:required ~ .mdc-floating-label::after,
    .mdc-text-field--focused .mdc-text-field__input:required ~ .mdc-notched-outline .mdc-floating-label::after {
        color: var(--mdc-theme-primary);
    }

    .mdc-text-field__input.resize-vertical {
        resize: vertical;
    }

    .mdc-text-field, .mdc-text-field-helper-line {
        width: 100%;
    }
    
    .mdc-text-field.dense-comfortable, .mdc-select.dense-comfortable {
        height: 48px;
    }
    
    .mdc-text-field.dense-compact {
        height: 36px;
    }
    
    .mdc-select:not(.mdc-list) {
        white-space: nowrap;
        display: flex;
        flex-direction: column;
    }

    .mdc-select:not(.mdc-select--disabled).mdc-select--focused .mdc-floating-label {
        color: var(--mdc-theme-primary);
    }

    .mdc-select-helper-text {
        white-space: normal;
        color: rgba(0, 0, 0, 0.6);
    }

    .mdc-icon-button {
        padding: 0;
        color: var(--internal-or-mwc-input-color);
    }
    
    /* Give slider min width like select etc. */
    .mdc-slider {
        min-width: 200px;
        flex: 1;
    }
    
    .mdc-switch {
        margin: 0 24px;
    }

    .mdc-switch--full-width {
        margin-left: auto;
    }
    .mdc-button--fullwidth {
      width: 100%;
    }
    #field {
        height: 100%;
    }

    .mdc-select__menu .mdc-list .mdc-list-item.mdc-list-item--selected or-icon {
        --or-icon-fill: var(--or-app-color4);
    }

    .mdc-menu__searchable {
        overflow: hidden;
    }
    .mdc-menu__searchable.mdc-menu-surface--open {
        display: flex;
        flex-direction: column-reverse;
    }
    .mdc-menu__searchable.mdc-menu-surface--is-open-below {
        flex-direction: column;
    }
    
    /* Prevent mouse events being fired from inside the or-icon shadowDOM */
    .mdc-list-item__graphic > or-icon {
        pointer-events: none;
    }
`;

@customElement("or-mwc-input")
export class OrMwcInput extends LitElement {

    static get styles() {
        return [
            css`${unsafeCSS(iconButtonStyle)}`,
            css`${unsafeCSS(buttonStyle)}`,
            css`${unsafeCSS(buttonFabStyle)}`,
            css`${unsafeCSS(textfieldStyle)}`,
            css`${unsafeCSS(rippleStyle)}`,
            css`${unsafeCSS(lineRippleStyle)}`,
            css`${unsafeCSS(floatingLabelStyle)}`,
            css`${unsafeCSS(formFieldStyle)}`,
            css`${unsafeCSS(checkboxStyle)}`,
            css`${unsafeCSS(radioStyle)}`,
            css`${unsafeCSS(switchStyle)}`,
            css`${unsafeCSS(selectStyle)}`,
            css`${unsafeCSS(listStyle)}`,
            css`${unsafeCSS(menuStyle)}`,
            css`${unsafeCSS(menuSurfaceStyle)}`,
            css`${unsafeCSS(sliderStyle)}`,
            style
        ];
    }

    @property({type: Boolean})
    public focused?: boolean;

    @property()
    public value?: any;

    @property({type: String})
    public type?: InputType;

    @property({type: String})
    public name?: String;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public required: boolean = false;

    @property()
    public max?: any;

    @property()
    public min?: any;

    @property({type: Number})
    public step?: number;

    @property({type: Boolean})
    public checked: boolean = false;

    @property({type: Boolean})
    public indeterminate: boolean = false;

    @property({type: Number})
    public maxLength?: number;

    @property({type: Number})
    public minLength?: number;

    @property({type: Number})
    public rows?: number;

    @property({type: Number})
    public cols?: number;

    @property({type: Boolean})
    public multiple: boolean = false;

    @property({type: String, attribute: true, reflect: false})
    public pattern?: string;

    @property({type: String})
    public placeHolder?: string;

    @property({type: Array})
    public options?: any[] | any;

    @property({type: Boolean})
    public autoSelect?: boolean;

    @property({type: Object})
    public searchProvider?: (search?: string) => Promise<[any, string][]>

    @property({type: String})
    public searchLabel = "search"

    /* STYLING PROPERTIES BELOW */

    @property({type: String})
    public icon?: string;

    @property({type: String})
    public iconColor?: string;

    @property({type: String})
    public iconOn?: string;

    @property({type: String})
    public iconTrailing?: string;

    @property({type: Boolean})
    public compact: boolean = false;

    @property({type: Boolean})
    public comfortable: boolean = false;

    /* BUTTON STYLES START */

    @property({type: Boolean})
    public raised: boolean = false;

    @property({type: Boolean})
    public action: boolean = false;

    @property({type: Boolean})
    public unElevated: boolean = false;

    @property({type: Boolean})
    public outlined: boolean = false;

    @property({type: Boolean})
    public rounded: boolean = false;

    @property({type: Object})
    public format?: ValueFormat;

    @property({type: Boolean})
    public disableSliderNumberInput: boolean = false;

    /* BUTTON STYLES END */

    /* TEXT INPUT STYLES START */

    @property({type: Boolean})
    public fullWidth: boolean = false;

    @property({type: String})
    public helperText?: string;

    @property({type: Boolean})
    public helperPersistent: boolean = false;

    @property({type: String, attribute: true})
    public validationMessage?: string;

    @property({type: Boolean})
    public autoValidate = false;

    @property({type: Boolean})
    public charCounter: boolean = false;

    @property({type: String})
    public label?: string;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public continuous: boolean = false;

    @property({type: Boolean})
    public resizeVertical: boolean = false;

    /**
     * Always censure text fields (like a password), and do not allow toggling
     */
    @property({type: Boolean})
    public censored: boolean = false;

    /**
     * Toggles visibility state of the password InputType (true = shown, false = hidden)
     */
    @property({type: Boolean, reflect: true})
    public advertised: boolean = false;

    public get nativeValue(): any {
        if (this._mdcComponent) {
            return (this._mdcComponent as any).value;
        }
    }

    /* TEXT INPUT STYLES END */

    protected _mdcComponent?: MDCComponent<any>;
    protected _mdcComponent2?: MDCComponent<any>;
    protected _selectedIndex = -1;
    protected _menuObserver?: IntersectionObserver;
    protected _tempValue: any;
    @state()
    protected isUiValid = true;
    @state()
    public searchableValue?: string;
    @state()
    protected errorMessage?: string;

    disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
            this._menuObserver?.disconnect()
        }
        if (this._mdcComponent2) {
            this._mdcComponent2.destroy();
            this._mdcComponent2 = undefined;
            this._menuObserver?.disconnect();
        }
    }

    protected shouldUpdate(_changedProperties: PropertyValues) {
        if(_changedProperties.has("indeterminate")) {
            if(this._mdcComponent && this.type === InputType.CHECKBOX){
                (this._mdcComponent as any).indeterminate = this.indeterminate;
            }
        }

        if (_changedProperties.has("disabled")) {
            if (this._mdcComponent) {
                (this._mdcComponent as any).disabled = this.disabled;
            }
            if (this.type === InputType.RANGE && this._mdcComponent2) {
                (this._mdcComponent2 as any).disabled = this.disabled;
            }
        }

        if (_changedProperties.has("readonly")) {
            if (this._mdcComponent) {
                (this._mdcComponent as any).readonly = this.readonly;
            }
            if (this.type === InputType.RANGE && this._mdcComponent2) {
                (this._mdcComponent2 as any).readonly = this.readonly;
            }
        }

        if (!this.type && this.value) {
            if (this.value instanceof Date) {
                this.type = InputType.DATETIME;
            } else if (typeof(this.value) === "boolean") {
                this.type = InputType.CHECKBOX;
            } else if (typeof(this.value) === "number") {
                this.type = InputType.NUMBER;
            } else if (typeof(this.value) === "string") {
                this.type = InputType.TEXT;
            } else {
                this.type = InputType.JSON;
            }
        }
        return true;
    }

    public focus() {
        if (this.type === InputType.RANGE && this._mdcComponent2) {
            (this._mdcComponent2 as any).focus();
        } else if (this._mdcComponent && typeof (this._mdcComponent as any).focus === "function") {
            (this._mdcComponent as any).focus();
        }
    }


    protected render() {

        if (this.type) {

            const showLabel = !this.fullWidth && this.label;
            let outlined = !this.fullWidth && this.outlined;
            let hasHelper = !!this.helperText;
            const showValidationMessage = !this.isUiValid && (!!this.errorMessage || !!this.validationMessage);
            const helperClasses = {
                "mdc-text-field-helper-text--persistent": !showValidationMessage && this.helperPersistent,
                "mdc-text-field-helper-text--validation-msg": showValidationMessage,
            };
            const hasValue = (this.value !== null && this.value !== undefined) || this.value === false;
            let labelTemplate = showLabel ? html`<span class="mdc-floating-label ${hasValue ? "mdc-floating-label--float-above" : ""}" id="label">${this.label}</span>` : undefined;

            switch (this.type) {
                case InputType.RADIO:
                    const optsRadio = this.resolveOptions(this.options);
                    this._selectedIndex = -1;
                    return html`
                            <div class="mdc-radio-container">
                                ${optsRadio ? optsRadio.map(([optValue, optDisplay], index) => {
                                    if (this.value === optValue) {
                                        this._selectedIndex = index;
                                    }
                                    return html`
                                    <div id="field" class="mdc-form-field">
                                        <div class="mdc-radio">
                                            <input type="radio" 
                                                id="elem-${optValue}"
                                                name="${ifDefined(this.name)}"
                                                value="${optValue}"
                                                ?checked="${this.value && this.value.includes(optValue)}"
                                                ?required="${this.required}"
                                                ?disabled="${this.disabled || this.readonly}"                            
                                                @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement), optValue)}"
                                                class="mdc-radio__native-control"/>
                                            <div class="mdc-radio__background">
                                            <div class="mdc-radio__outer-circle"></div>
                                            <div class="mdc-radio__inner-circle"></div>
                                            </div>
                                            <div class="mdc-radio__ripple"></div>
                                        </div>
                                        <label for="elem-${optValue}"><or-translate value="${optDisplay}"></or-translate></label>
                                    </div>

                                    `;
                                }) : ``}
                            </div>
                    `;
                case InputType.SWITCH:
                    const classesSwitch = {
                        "mdc-switch--disabled": this.disabled || this.readonly,
                        "mdc-switch--full-width": this.fullWidth,
                        "mdc-switch--checked": this.value,
                    };

                    return html`
                        <span id="wrapper">
                            ${this.label ? html`<label for="elem" class="${this.disabled ? "mdc-switch--disabled" : ""}">${this.label}</label>` : ``}
                            <div id="component" class="mdc-switch ${classMap(classesSwitch)}">
                                <div class="mdc-switch__track"></div>
                                <div class="mdc-switch__thumb-underlay">
                                    <div class="mdc-switch__thumb">
                                        <input type="checkbox" id="elem" class="mdc-switch__native-control" 
                                        ?checked="${this.value}"
                                        ?required="${this.required}"
                                        ?disabled="${this.disabled || this.readonly}"
                                        @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).checked)}"
                                        role="switch">
                                    </div>
                                </div>
                            </div>
                        </span>
                    `;
                case InputType.LIST:
                        const classesList = {
                            "mdc-select--outlined": outlined,
                            "mdc-select--disabled": this.disabled,
                            "mdc-select--required": this.required,
                            "mdc-select--dense": false, // this.dense,
                            "mdc-select--no-label": !this.label,
                            "mdc-select--with-leading-icon": !!this.icon
                        };

                        const optsList = this.resolveOptions(this.options);
                        this._selectedIndex = -1;
                        return html`
                            <div id="component" class="mdc-list mdc-select ${classMap(classesList)}" @MDCList:action="${(e: MDCListActionEvent) => this.onValueChange(undefined, e.detail.index === -1 ? undefined : Array.isArray(this.options![e.detail.index]) ? this.options![e.detail.index][0] : this.options![e.detail.index])}">
                                <ul class="mdc-list">
                                    ${optsList ? optsList.map(([optValue, optDisplay], index) => {
                                        if (this.value === optValue) {
                                            this._selectedIndex = index;
                                        }
                                        // todo: it's not actually putting the mdc-list-item--selected class on even when this.value === optValue...
                                        return html`<li class="${classMap({"mdc-list-item": true, "mdc-list-item--selected": this.value === optValue})}" role="option" data-value="${optValue}"><or-translate value="${optDisplay}"></or-translate></li>`;
                                    }) : ``}
                                </ul>
                            </div>
                        `;
                case InputType.SELECT:
                    const classes = {
                        "mdc-select--outlined": outlined,
                        "mdc-select--filled": !outlined,
                        "mdc-select--disabled": this.disabled || this.readonly,
                        "mdc-select--required": this.required,
                        "mdc-select--dense": false, // this.dense,
                        "dense-comfortable": this.comfortable,
                        "mdc-select--no-label": !this.label,
                        "mdc-select--with-leading-icon": !!this.icon,
                        "or-mwc-input--rounded": this.rounded

                    };

                    let opts: [any, string][] | Promise<[any, string][]>;
                    if(this.searchProvider != undefined) {
                        opts = this.searchProvider(this.searchableValue);
                    } else {
                        opts = this.resolveOptions(this.options)!;
                    }
                    const itemClickHandler: (ev: MouseEvent, item: ListItem) => void = (ev, item) => {
                        const value = item.value;

                        if (this.multiple) {
                            ev.stopPropagation();
                            const inputValue = this._tempValue ?? (Array.isArray(this.value) ? [...this.value] : this.value !== undefined ? [this.value] : []);

                            const index = inputValue.findIndex((v: any) => v === value);
                            if (index >= 0) {
                                inputValue.splice(index, 1);
                            } else {
                                inputValue.push(value);
                            }
                            const listItemEl = (ev.composedPath()[0] as HTMLElement).closest("li") as HTMLElement,
                                iconEl = listItemEl.getElementsByTagName("or-icon")[0] as OrIcon;
                            if (listItemEl) {
                                if (index >= 0) {
                                    listItemEl.classList.remove("mdc-list-item--selected");
                                } else {
                                    listItemEl.classList.add("mdc-list-item--selected");
                                }
                            }
                            if (iconEl) {
                                iconEl.icon = index >= 0 ? "checkbox-blank-outline" : "checkbox-marked";
                            }
                            this._tempValue = inputValue;
                        }

                        // A narrowed down list with search, or a different asynchronous approach does not always trigger @MDCSelect:change,
                        // so using itemClickHandler instead to let it trigger anyway.
                        else if(this.searchProvider != undefined || !Array.isArray(opts)) {
                            this.onValueChange(undefined, item.value);
                        }

                    };

                    const menuCloseHandler = () => {
                        const v = (this._tempValue ?? this.value);
                        window.setTimeout(() => {
                            if (this._mdcComponent) {
                                // Hack to stop label moving down when there is a value set
                                (this._mdcComponent as any).foundation.adapter.floatLabel(v && (!Array.isArray(v) || v.length > 0));
                            }
                        });

                        if (!this._tempValue) {
                            return;
                        }
                        const val = [...this._tempValue];
                        this._tempValue = undefined;
                        this.onValueChange(undefined, val);
                    };

                    const listTemplate = (options: [any, string][]) => {
                        if(this.searchProvider != undefined && (!options || options.length == 0)) {
                            return html`<span class="mdc-text-field-helper-line" style="margin: 8px 8px 8px 0;">${i18next.t('noResults')}</span>`
                        } else {
                            return getListTemplate(
                                this.multiple ? ListType.MULTI_TICK : ListType.SELECT,
                                html`${options?.map(([optValue, optDisplay], index) => {
                                    return getItemTemplate(
                                        {
                                            text: optDisplay,
                                            value: optValue
                                        },
                                        index,
                                        Array.isArray(this.value) ? this.value as any[] : this.value ? [this.value as any] : [],
                                        this.multiple ? ListType.MULTI_TICK : ListType.SELECT,
                                        false,
                                        itemClickHandler
                                    );
                                })}`,
                                false,
                                undefined
                            );
                        }
                    }

                    return html`
                        <div id="component"
                            class="mdc-select ${classMap(classes)}"
                            @MDCSelect:change="${async (e: MDCSelectEvent) => {
                                const options: [any, string][] = (Array.isArray(opts) ? opts : await opts);
                                this.onValueChange(undefined, e.detail.index === -1 ? undefined : Array.isArray(options[e.detail.index]) ? options[e.detail.index][0] : options[e.detail.index]);
                            }}">
                                <div class="mdc-select__anchor" role="button"
                                     aria-haspopup="listbox"
                                     aria-expanded="false"
                                     aria-disabled="${""+(this.disabled || this.readonly)}"
                                     aria-labelledby="label selected-text">
                                    ${!outlined ? html`<span class="mdc-select__ripple"></span>` : undefined}
                                    ${outlined ? this.renderOutlined(labelTemplate) : labelTemplate}
                                    <span class="mdc-select__selected-text-container">
                                      <span id="selected-text" class="mdc-select__selected-text"></span>
                                    </span>
                                    <span class="mdc-select__dropdown-icon">
                                        <svg
                                          class="mdc-select__dropdown-icon-graphic"
                                          viewBox="7 10 10 5">
                                        <polygon
                                            class="mdc-select__dropdown-icon-inactive"
                                            stroke="none"
                                            fill-rule="evenodd"
                                            points="7 10 12 15 17 10">
                                        </polygon>
                                        <polygon
                                            class="mdc-select__dropdown-icon-active"
                                            stroke="none"
                                            fill-rule="evenodd"
                                            points="7 15 12 10 17 15">
                                        </polygon>
                                      </svg>
                                    </span>
                                    ${!outlined ? html`<div class="mdc-line-ripple"></div>` : ``}
                                </div>
                                <div id="mdc-select-menu" class="mdc-select__menu mdc-menu mdc-menu-surface mdc-menu-surface--fixed ${this.searchProvider != undefined ? 'mdc-menu__searchable' : undefined}" @MDCMenuSurface:closed="${menuCloseHandler}">
                                    ${when(this.searchProvider != undefined, () => html`
                                        <label id="select-searchable" class="mdc-text-field mdc-text-field--filled">
                                            <span class="mdc-floating-label" style="color: rgba(0, 0, 0, 0.6); text-transform: capitalize; visibility: ${this.searchableValue ? 'hidden' : 'visible'}" id="my-label-id">
                                                <or-translate .value="${this.searchLabel}"></or-translate>
                                            </span>
                                            <input class="mdc-text-field__input" type="text"
                                                   @keyup="${(e: KeyboardEvent) => this.searchableValue = (e.target as HTMLInputElement).value}"
                                            />
                                        </label>
                                    `)}
                                    ${when(Array.isArray(opts), () => {
                                        return listTemplate(opts as [any, string][]);
                                    }, () => {
                                        return until(new Promise(async (resolve) => {
                                            resolve(listTemplate(await opts));
                                        }), html`<span class="mdc-text-field-helper-line" style="margin: 8px 8px 8px 0;">${i18next.t('loading')}</span>`)
                                    })}
                                </div>
                                ${hasHelper || showValidationMessage ? html`
                                    <p id="component-helper-text" class="mdc-select-helper-text ${classMap(helperClasses)}" aria-hidden="true">
                                        ${showValidationMessage ? this.errorMessage || this.validationMessage : this.helperText}
                                    </p>` : ``}
                        </div>
                    `;
                case InputType.BUTTON_TOGGLE:
                    return html`
                        <button id="component" class="mdc-icon-button ${this.value ? "mdc-icon-button--on" : ""}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @MDCIconButtonToggle:change="${(evt: CustomEvent<MDCIconButtonToggleEventDetail>) => this.onValueChange(undefined, evt.detail.isOn)}">
                            ${this.icon ? html`<or-icon class="mdc-icon-button__icon" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.iconOn ? html`<or-icon class="mdc-icon-button__icon mdc-icon-button__icon--on" aria-hidden="true" icon="${this.iconOn}"></or-icon>` : ``}
                        </button>
                    `;
                case InputType.BUTTON:
                case InputType.BUTTON_MOMENTARY: {

                    const onMouseDown = (ev: MouseEvent) => {
                        if (this.disabled) {
                            ev.stopPropagation();
                        }

                        if (isMomentary) this.dispatchEvent(new OrInputChangedEvent(true, null))
                    };
                    const onMouseUp = (ev: MouseEvent) => {
                        if (this.disabled) {
                            ev.stopPropagation();
                        }

                        if (isMomentary) this.dispatchEvent(new OrInputChangedEvent(false, true))
                    };
                    const onClick = (ev: MouseEvent) => {
                        if (this.disabled) {
                            ev.stopPropagation();
                        }

                        if (!isMomentary) this.dispatchEvent(new OrInputChangedEvent(true, null))
                    };

                    const isMomentary = this.type === InputType.BUTTON_MOMENTARY;
                    const isIconButton = !this.action && !this.label;
                    // If no action, label or icons are given, show as a circle.
                    if (isIconButton && !this.iconTrailing && !this.icon ) {
                        this.icon = "circle"
                        }

                    const classes = {
                        "mdc-icon-button": isIconButton,
                        "mdc-fab": !isIconButton && this.action,
                        "mdc-fab--extended": !isIconButton && this.action && !!this.label,
                        "mdc-fab--mini": !isIconButton && this.action && (this.compact || this.comfortable),
                        "mdc-button": !isIconButton && !this.action,
                        "mdc-button--raised": !isIconButton && !this.action && this.raised,
                        "mdc-button--unelevated": !isIconButton && !this.action && this.unElevated,
                        "mdc-button--outlined": !isIconButton && !this.action && (this.outlined || isMomentary),
                        "mdc-button--rounded": !isIconButton && !this.action && this.rounded,                        
                        "mdc-button--fullwidth": this.fullWidth,
                    };
                    return html`
                        <button id="component" class="${classMap(classes)}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @click="${(ev: MouseEvent) => onClick(ev)}"
                            @mousedown="${(ev: MouseEvent) => onMouseDown(ev)}" @mouseup="${(ev: MouseEvent) => onMouseUp(ev)}">
                            ${!isIconButton ? html`<div class="mdc-button__ripple"></div>` : ``}
                            ${this.icon ? html`<or-icon class="${isIconButton ? "" : this.action ? "mdc-fab__icon" : "mdc-button__icon"}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.label ? html`<span class="${this.action ? "mdc-fab__label" : "mdc-button__label"}"><or-translate .value="${this.label}"></or-translate></span>` : ``}
                            ${!isIconButton && this.iconTrailing ? html`<or-icon class="${this.action ? "mdc-fab__icon" : "mdc-button__icon"}" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                        </button>
                    `;
                }
                case InputType.CHECKBOX_LIST:
                    if (!Array.isArray(this.value)) {
                        if (this.value === null || this.value === undefined) {
                            this.value = [];
                        } else {
                            this.value = [this.value];
                        }
                    }
                    const optsCheckboxList = this.resolveOptions(this.options);
                    this._selectedIndex = -1;
                    return html`
                            <div class="mdc-checkbox-list">
                                ${optsCheckboxList ? optsCheckboxList.map(([optValue, optDisplay], index) => {
                                    if (this.value === optValue) {
                                        this._selectedIndex = index;
                                    }
                                    return html`
                                    <div id="field" class="mdc-form-field">
                                        <div id="component" class="mdc-checkbox">
                                            <input type="checkbox"
                                                ?checked="${this.value && this.value.includes(optValue)}"
                                                ?required="${this.required}"
                                                name="${optValue}"
                                                ?disabled="${this.disabled || this.readonly}"
                                                @change="${(e: Event) => {
                                                    let val: any[] = this.value;
                                                    if ((e.target as HTMLInputElement).checked) {
                                                        if (!val.includes(optValue)) {
                                                            val = [optValue,...val];
                                                        }
                                                    } else {
                                                        val = val.filter((v: any) => v !== optValue);
                                                    }
                                                    this.onValueChange((e.target as HTMLInputElement), val);
                                                }}"
                                                class="mdc-checkbox__native-control" id="elem-${optValue}"/>

                                            <label for="elem-${optValue}"><or-translate value="${optDisplay}"></or-translate></label>
                                              
                                        </div>
                                    </div>

                                    `;
                                }) : ``}
                            </div>
                    `;
                case InputType.CHECKBOX:
                    let classList = {
                        "mdc-checkbox": true,
                        "mdc-checkbox--disabled": this.disabled || this.readonly
                    };
                    return html`
                        <div id="field" class="mdc-form-field">
                            <div id="component" class="${classMap(classList)}">
                                <input type="checkbox" 
                                    id="elem" 
                                    data-indeterminate="${this.indeterminate}"
                                    ?checked="${this.value}"
                                    ?required="${this.required}"
                                    ?disabled="${this.disabled || this.readonly}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).checked)}"
                                    class="mdc-checkbox__native-control" />
                                <div class="mdc-checkbox__background">
                                    <svg class="mdc-checkbox__checkmark" viewBox="0 0 24 24">
                                        <path class="mdc-checkbox__checkmark-path" fill="none" d="M1.73,12.91 8.1,19.28 22.79,4.59"></path>
                                    </svg>
                                    <div class="mdc-checkbox__mixedmark"></div>
                                </div>
                                <div class="mdc-checkbox__ripple"></div>
                            </div>
                            <label class="mdc-checkbox-circle" for="elem">${this.label}</label>
                        </div>
                    `;
                case InputType.COLOUR:
                    return html`
                        <div id="component" style="width: 100%; display: inline-flex; align-items: center; padding: 8px 0;">
                            <input type="color" id="elem" style="border: none; height: 31px; width: 31px; padding: 1px 3px; min-height: 22px; min-width: 30px;cursor: pointer" value="${this.value}"
                                   ?disabled="${this.disabled || this.readonly}"
                                   ?required="${this.required}"
                                   @change="${(e: any) => this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).value)}"
                            />
                            <label style="margin-left: 10px; cursor: pointer" for="elem">${this.label}</label>
                        </div>
                    `
                case InputType.NUMBER:
                case InputType.RANGE:
                case InputType.DATE:
                case InputType.DATETIME:
                case InputType.TIME:
                case InputType.MONTH:
                case InputType.WEEK:
                case InputType.EMAIL:
                case InputType.PASSWORD:
                case InputType.TELEPHONE:
                case InputType.URL:
                case InputType.TEXT:
                case InputType.TEXTAREA:
                case InputType.JSON:
                case InputType.JSON_OBJECT: {
                    // The following HTML input types require the values as specially formatted strings
                    let valMinMax: [any, any, any] = [this.value === undefined || this.value === null ? undefined : this.value, this.min, this.max];

                    if (valMinMax.some((v) => typeof (v) !== "string")) {

                        if (this.type === InputType.JSON || this.type === InputType.JSON_OBJECT) {
                            if (valMinMax[0] !== undefined) {
                                if (typeof valMinMax[0] !== "string" || valMinMax[0] === null) {
                                    try {
                                        valMinMax[0] = JSON.stringify(valMinMax[0], null, 2);
                                    } catch (e) {
                                        console.warn("Failed to parse JSON expression for input control");
                                        valMinMax[0] = "";
                                    }
                                }
                            } else {
                                valMinMax[0] = "";
                            }
                        } else {

                            const format = this.format ? {...this.format} : {};

                            switch (this.type) {
                                case InputType.TIME:
                                    format.asDate = true;
                                    format.hour12 = false;
                                    format.timeStyle = this.step && this.step < 60 ? ValueFormatStyleRepresentation.MEDIUM : ValueFormatStyleRepresentation.SHORT;
                                    break;
                                case InputType.DATE:
                                    format.asDate = true;
                                    format.momentJsFormat = "YYYY-MM-DD";
                                    break;
                                case InputType.WEEK:
                                    format.asDate = true;
                                    format.momentJsFormat = "YYYY-[W]WW";
                                    break;
                                case InputType.MONTH:
                                    format.asDate = true;
                                    format.momentJsFormat = "YYYY-MM";
                                    break;
                                case InputType.DATETIME:
                                    format.asDate = true;
                                    format.momentJsFormat = "YYYY-MM-DDTHH:mm";
                                    break;
                                case InputType.NUMBER:
                                    format.maximumFractionDigits ??= 20; // default according to Web documentation
                                    break;
                            }

                            // Numbers/dates must be in english locale without commas etc.
                            format.useGrouping = false;
                            valMinMax = valMinMax.map((val) => val !== undefined ? Util.getValueAsString(val, () => format, "en-GB") : undefined) as [any,any,any];
                        }
                    }

                    let inputElem: TemplateResult | undefined;
                    let label = this.label;
                    let type = this.type;
                    let componentId = "component";

                    if (this.type === InputType.RANGE) {
                        // Change vars so number input can be included alongside the slider
                        label = undefined;
                        outlined = false;
                        hasHelper = false;
                        type = InputType.NUMBER;
                        componentId = "number";
                    }

                    if (!(this.type === InputType.RANGE && this.disableSliderNumberInput)) {

                        // Handle password toggling logic
                        if(this.censored) type = InputType.PASSWORD;
                        if(this.type === InputType.PASSWORD && this.advertised) type = InputType.TEXT;

                        const classes = {
                            "mdc-text-field": true,
                            "mdc-text-field--invalid": !this.valid,
                            "mdc-text-field--filled": !outlined,
                            "mdc-text-field--outlined": outlined,
                            "mdc-text-field--textarea": type === InputType.TEXTAREA || type === InputType.JSON || type === InputType.JSON_OBJECT,
                            "mdc-text-field--disabled": this.disabled,
                            "mdc-text-field--fullwidth": this.fullWidth && !outlined,
                            "dense-comfortable": this.comfortable && !(type === InputType.TEXTAREA || type === InputType.JSON || type === InputType.JSON_OBJECT),
                            "dense-compact": !this.comfortable && this.compact,
                            "mdc-text-field--label-floating": hasValue,
                            "mdc-text-field--no-label": !this.label,
                            "mdc-text-field--with-leading-icon": !!this.icon,
                            "mdc-text-field--with-trailing-icon": !!this.iconTrailing,
                            "or-mwc-input--rounded": this.rounded
                        };

                        inputElem = type === InputType.TEXTAREA || type === InputType.JSON || type === InputType.JSON_OBJECT
                            ? html`
                                <textarea id="elem" class="mdc-text-field__input ${this.resizeVertical ? "resize-vertical" : ""}" ?required="${this.required}"
                                ?readonly="${this.readonly}" ?disabled="${this.disabled}" minlength="${ifDefined(this.minLength)}"
                                maxlength="${ifDefined(this.maxLength)}" rows="${this.rows ? this.rows : 5}"
                                cols="${ifDefined(this.cols)}" aria-label="${ifDefined(label)}"
                                aria-labelledby="${ifDefined(label ? "label" : undefined)}"
                                @change="${(e: Event) => this.onValueChange((e.target as HTMLTextAreaElement), (e.target as HTMLTextAreaElement).value)}">${valMinMax[0] ? valMinMax[0] : ""}</textarea>`
                            : html`
                            <input type="${type}" id="elem" aria-labelledby="${ifDefined(label ? "label" : undefined)}"
                            class="mdc-text-field__input" ?required="${this.required}" ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}" min="${ifDefined(valMinMax[1])}" max="${ifDefined(valMinMax[2])}"
                            step="${this.step ? this.step : "any"}" minlength="${ifDefined(this.minLength)}" pattern="${ifDefined(this.pattern)}"
                            maxlength="${ifDefined(this.maxLength)}" placeholder="${ifDefined(this.placeHolder)}"
                            .value="${valMinMax[0] !== null && valMinMax[0] !== undefined ? valMinMax[0] : ""}"
                            @keydown="${(e: KeyboardEvent) => {
                                if ((e.code === "Enter" || e.code === "NumpadEnter")) {
                                    this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).value, true);
                                }}}"
                            @blur="${(e: Event) => {if ((e.target as HTMLInputElement).value === "") this.reportValidity()}}"
                            @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).value)}" />`;

                        inputElem = html`
                            <label id="${componentId}" class="${classMap(classes)}">
                                ${this.icon ? html`<or-icon class="mdc-text-field__icon mdc-text-field__icon--leading" style="color: ${this.iconColor ? "#" + this.iconColor : "unset"}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                                ${outlined ? `` : html`<span class="mdc-text-field__ripple"></span>`}
                                ${inputElem}
                                ${outlined ? this.renderOutlined(labelTemplate) : labelTemplate}
                                ${outlined ? `` : html`<span class="mdc-line-ripple"></span>`}
                                ${this.type === InputType.PASSWORD && !this.censored ? html`<or-icon class="mdc-text-field__icon mdc-text-field__icon--trailing" aria-hidden="true" icon=${this.advertised ? 'eye' : 'eye-off'} style="pointer-events: auto;" @click=${() => this.advertised = !this.advertised}></or-icon>` : ``}
                                ${this.iconTrailing ? html`<or-icon class="mdc-text-field__icon mdc-text-field__icon--trailing" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                            </label>
                            ${hasHelper || showValidationMessage ? html`
                                <div class="mdc-text-field-helper-line">
                                    <div class="mdc-text-field-helper-text ${classMap(helperClasses)}">${showValidationMessage ? this.errorMessage || this.validationMessage : this.helperText}</div>
                                    ${this.charCounter && !this.readonly ? html`<div class="mdc-text-field-character-counter"></div>` : ``}
                                </div>
                            ` : ``}
                        `;
                    }

                    if (this.type === InputType.RANGE) {

                        const classes = {
                            "mdc-slider": true,
                            "mdc-slider--range": this.continuous,
                            "mdc-slider--discreet": !this.continuous,
                            "mdc-slider--disabled": this.disabled || this.readonly
                        };

                        inputElem = html`
                            <span id="wrapper">
                                ${this.label ? html`<label for="component" class="${this.disabled ? "mdc-switch--disabled" : ""}">${this.label}</label>` : ``}
                                <div id="component" class="${classMap(classes)}" @MDCSlider:change="${(ev:CustomEvent<MDCSliderChangeEventDetail>) => this.onValueChange(undefined, ev.detail.value)}">
                                    <input id="elem" class="mdc-slider__input" type="range" min="${ifDefined(valMinMax[1])}" max="${ifDefined(valMinMax[2])}" value="${valMinMax[0] || valMinMax[1] || 0}" name="slider" step="${this.step || 1}" ?readonly="${this.readonly}" ?disabled="${this.disabled}" aria-label="${ifDefined(this.label)}" />
                                    <div class="mdc-slider__track">
                                        <div class="mdc-slider__track--inactive"></div>
                                        <div class="mdc-slider__track--active">
                                            <div class="mdc-slider__track--active_fill"></div>
                                        </div>
                                    </div>
                                    <div class="mdc-slider__thumb">
                                        ${!this.continuous ? html`<div class="mdc-slider__value-indicator-container" aria-hidden="true">
                                            <div class="mdc-slider__value-indicator">
                                                <span class="mdc-slider__value-indicator-text">
                                                  50
                                                </span>
                                            </div>
                                        </div>` : ``}
                                        <div class="mdc-slider__thumb-knob"></div>
                                    </div>
                                </div>
                                ${inputElem ? html`<div style="min-width: 70px; width: 70px;">${inputElem}</div>` : ``}
                            </span>
                        `;
                    }

                    return inputElem;
                }
            }
        }

        return html`<span>INPUT TYPE NOT IMPLEMENTED</span>`;
    }

    protected _getFormat(): ValueFormat | undefined {
        if (this.format) {
            return this.format;
        }
    }

    update(_changedProperties: PropertyValues) {
        if (_changedProperties.has('autoValidate') && this._mdcComponent) {
            const comp = this._mdcComponent as any;
            if (comp.foundation && comp.foundation.setValidateOnValueChange) {
                comp.foundation.setValidateOnValueChange(this.autoValidate);
            }
        }

        super.update(_changedProperties);
    }

    firstUpdated(_changedProperties: PropertyValues) {
        super.firstUpdated(_changedProperties);

        if (this.autoValidate) {
            this.reportValidity();
        }
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (_changedProperties.has("type")) {
            const component = this.shadowRoot!.getElementById("component");
            if (this._mdcComponent) {
                this._mdcComponent.destroy();
                this._mdcComponent = undefined;
            }
            if (this._mdcComponent2) {
                this._mdcComponent2.destroy();
                this._mdcComponent2 = undefined;
            }

            if (component && this.type) {
                switch (this.type) {
                    case InputType.LIST:
                        const mdcList = new MDCList(component);
                        this._mdcComponent = mdcList;
                        mdcList.selectedIndex = this._selectedIndex;
                        break;
                    case InputType.SELECT:
                        const mdcSelect = new MDCSelect(component);
                        this._mdcComponent = mdcSelect;

                        const hasValue = (this.value !== null && this.value !== undefined);
                        if (!hasValue) {
                            mdcSelect.selectedIndex = -1; // Without this first option will be shown as selected
                        }

                        if (this.multiple) {
                            // To make multiple select work then override the adapter getSelectedIndex
                            (this._mdcComponent as any).foundation.adapter.getSelectedIndex = () => {
                                // Return first item index
                                if (!Array.isArray(this.value) || (this.value as []).length === 0) {
                                    return -1;
                                }
                                const firstSelected = (this.value as any[])[0];
                                const items = (this._mdcComponent as any).foundation.adapter.getMenuItemValues();
                                return items.indexOf(firstSelected);
                            };
                        }

                        mdcSelect.useDefaultValidation = !this.multiple;
                        mdcSelect.valid = !this.required || (!this.multiple && mdcSelect.valid) || (this.multiple && Array.isArray(this.value) && (this.value as []).length > 0);

                        const selectedText = this.getSelectedTextValue();
                        (this._mdcComponent as any).foundation.adapter.setSelectedText(selectedText);
                        (this._mdcComponent as any).foundation.adapter.floatLabel(!!selectedText);

                        // Set width of fixed select menu to match the component width
                        // Using an observer to prevent forced reflow / DOM measurements; prevents blocking the thread
                        if(!this._menuObserver) {
                            this._menuObserver = new IntersectionObserver((entries, observer) => {
                                if((entries[0].target as HTMLElement).style.minWidth != (entries[0].target.parentElement?.clientWidth + "px")) {
                                    (entries[0].target as HTMLElement).style.minWidth = entries[0].target.parentElement?.clientWidth + "px";
                                }
                            })
                            this._menuObserver.observe(this.shadowRoot!.getElementById("mdc-select-menu")!);
                        }

                        // This overrides the standard mdc menu body click capture handler as it doesn't work with webcomponents
                        const searchable: boolean = (this.searchProvider !== undefined);
                        const multi: boolean = this.multiple;
                        (mdcSelect as any).menu.menuSurface_.foundation.handleBodyClick = function (evt: MouseEvent) {
                            const el = evt.composedPath()[0]; // Use composed path not evt target to work with webcomponents
                            if (this.adapter.isElementInContainer(el)) {
                                if(!searchable) {
                                    return; // Normal select menu closes automatically, so abort
                                }
                                // if searchable, we manually close the menu when clicking a list item.
                                // However, if something else than a list item (for example the search field) is clicked, it should not close, so abort.
                                else if (el instanceof Element && !el.className.includes('mdc-list-item')) {
                                    return;
                                }
                                else if (multi) {
                                    return;
                                }
                            }
                            (mdcSelect as any).menu.menuSurface_.close();
                        };

                        break;
                    case InputType.RADIO:
                    case InputType.CHECKBOX_LIST:
                    case InputType.COLOUR:
                        break;
                    case InputType.BUTTON:
                    case InputType.BUTTON_MOMENTARY:
                        const isIconButton = !this.action && !this.label;
                        const ripple = new MDCRipple(component);
                        if (isIconButton) {
                            ripple.unbounded = true;
                        }
                        this._mdcComponent = ripple;
                        break;
                    case InputType.BUTTON_TOGGLE:
                        this._mdcComponent = new MDCIconButtonToggle(component);
                        break;
                    case InputType.CHECKBOX:
                        this._mdcComponent = new MDCCheckbox(component);
                        const field = this.shadowRoot!.getElementById("field");
                        if (field) {
                            const mdcField = new MDCFormField(field);
                            mdcField.input = this._mdcComponent as unknown as MDCFormFieldInput;
                            this._mdcComponent2 = mdcField;
                        }
                        break;
                    case InputType.SWITCH:
                        this._mdcComponent = new MDCSwitch(component);
                        break;
                    case InputType.RANGE:
                        this._mdcComponent = new MDCSlider(component);
                        const numberComponent = this.shadowRoot!.getElementById("number");
                        if (numberComponent) {
                            const numberField = new MDCTextField(numberComponent);
                            numberField.useNativeValidation = false;
                            this._mdcComponent2 = numberField;
                        }
                        break;
                    default:
                        const textField = new MDCTextField(component);
                        textField.useNativeValidation = false;
                        this._mdcComponent = textField;
                        break;
                }

                if (this._mdcComponent && this.focused && typeof((this._mdcComponent as any).focus) === "function") {
                    (this._mdcComponent as any).focus();
                }
            }
        } else {
            // some components need to be kept in sync with the DOM
            if (this.type === InputType.SELECT && this._mdcComponent) {
                if (_changedProperties.has("options")) {
                    (this._mdcComponent as MDCSelect).layoutOptions(); // has big impact on performance when the MDCSelect list is large.
                }
                (this._mdcComponent as MDCSelect).disabled = !!(this.disabled || this.readonly);
                (this._mdcComponent as MDCSelect).useDefaultValidation = !this.multiple;
                (this._mdcComponent as MDCSelect).valid = !this.required || (!this.multiple && (this._mdcComponent as MDCSelect).valid) || (this.multiple && Array.isArray(this.value) && (this.value as []).length > 0);
                const selectedText = this.getSelectedTextValue();
                (this._mdcComponent as any).foundation.adapter.setSelectedText(selectedText);
                (this._mdcComponent as any).foundation.adapter.floatLabel(!!selectedText);
            } else if (this.type === InputType.RANGE && this._mdcComponent) {
                const slider = this._mdcComponent as MDCSlider;
                slider.setDisabled(this.disabled || this.readonly);
                // slider.getDefaultFoundation(). getDefaultFoundation()..getMax() = this.min;
                // slider.max = this.max;
                slider.setValue(this.value);
            } else if (this.type === InputType.SWITCH && this._mdcComponent) {
                const swtch = this._mdcComponent as MDCSwitch;
                swtch.checked = this.value;
            } else if (this.type === InputType.CHECKBOX && this._mdcComponent) {
                const checkbox = this._mdcComponent as MDCCheckbox;
                checkbox.checked = !!this.value;
                checkbox.disabled = !!(this.disabled || this.readonly);
            }

            if (this._mdcComponent) {
                (this._mdcComponent as any).required = !!this.required;
            }
        }

        if(_changedProperties.has("label")) {
            (this._mdcComponent as any)?.layout?.(); // Adjusts the dimensions and positions for all sub-elements.
        }

        if (this.autoValidate) {
            this.reportValidity();
        }
    }

    protected renderOutlined(labelTemplate: TemplateResult | undefined) {
        return html`
            <span class="mdc-notched-outline">
                <span class="mdc-notched-outline__leading"></span>
                ${labelTemplate ? html`
                <span class="mdc-notched-outline__notch">
                    ${labelTemplate}
                </span>
                ` : ``}
                <span class="mdc-notched-outline__trailing"></span>
            </span>
        `;
    }

    public setCustomValidity(msg: string | undefined) {
        this.errorMessage = msg;
        const elem = this.shadowRoot!.getElementById("elem") as HTMLElement;
        if (elem && (elem as any).setCustomValidity) {
            (elem as any).setCustomValidity(msg ?? "");
        }
        this.reportValidity();
    }

    public checkValidity(): boolean {
        const elem = this.shadowRoot!.getElementById("elem") as any;
        let valid = true;
        if (elem && elem.validity) {
            const nativeValidity = elem.validity as ValidityState;
            valid = nativeValidity.valid;
        }

        if (valid && (this.type === InputType.JSON || this.type === InputType.JSON_OBJECT)) {
            // JSON needs special validation - if no text value but this.value then parsing failed
            if (this.value !== undefined && this.value !== null && (this._mdcComponent as MDCTextField).value === "") {
                valid = false;
            }
        }

        return valid;
    }

    public reportValidity(): boolean {
        const isValid = this.checkValidity();
        this.isUiValid = isValid;

        if (this._mdcComponent) {
            (this._mdcComponent as any).valid = isValid;
        }

        return isValid;
    }

    protected onValueChange(elem: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement | undefined, newValue: any | undefined, enterPressed?: boolean) {
        let previousValue = this.value;
        let errorMsg: string | undefined;

        if (newValue === "undefined") {
            previousValue = null;
            newValue = undefined;
        }

        if (newValue === "null") {
            previousValue = undefined;
            newValue = null;
        }

        if (typeof(newValue) === "string") {
            switch (this.type) {
                case InputType.CHECKBOX:
                case InputType.SWITCH:
                    newValue = newValue === "on";
                    break;
                case InputType.JSON:
                case InputType.JSON_OBJECT:
                case InputType.NUMBER:
                case InputType.RANGE:
                    if (newValue === "") {
                        newValue = null;
                    } else {
                        try {
                            newValue = JSON.parse(newValue);
                            if (this.type === InputType.JSON_OBJECT && (typeof newValue !== 'object' || Array.isArray(newValue))) {
                                newValue = this.value;
                                errorMsg = i18next.t("validation.invalidJSON");
                            }
                        } catch (e) {
                            newValue = this.value;
                            errorMsg = this.type === InputType.JSON || this.type == InputType.JSON_OBJECT ? i18next.t("validation.invalidJSON") : i18next.t("validation.invalidNumber");
                        }
                    }
                    break;
                case InputType.DATETIME:
                    if (newValue === "") {
                        newValue = null;
                    } else {
                        try {
                            newValue = Date.parse(newValue);
                        } catch (e) {
                            newValue = this.value;
                            errorMsg = i18next.t("validation.invalidDate");
                        }
                    }
                    break;
            }
        }
        this.value = newValue;
        this.setCustomValidity(errorMsg);
        this.reportValidity();

        if (this.type !== InputType.CHECKBOX_LIST && newValue !== previousValue) {
            if (this.type === InputType.RANGE) {
                (this._mdcComponent as MDCSlider).setValue(newValue);
                if (this._mdcComponent2) {
                    (this._mdcComponent2 as MDCTextField).value = newValue;
                }
            }
            this.dispatchEvent(new OrInputChangedEvent(this.value, previousValue, enterPressed));
        }

        // Reset search if value has been selected
        if(this.searchProvider != undefined && this.type === InputType.SELECT) {
            const searchableElement = this.shadowRoot?.getElementById('select-searchable')?.children[1];
            if(searchableElement) {
                this.searchableValue = undefined;
                (searchableElement as HTMLInputElement).value = "";
            }
        }

        if (this.type === InputType.CHECKBOX_LIST && !Util.objectsEqual(newValue, previousValue, true)) {
            this.dispatchEvent(new OrInputChangedEvent(newValue, previousValue, enterPressed));
        }
    }

    public get valid(): boolean {
        const elem = this.shadowRoot!.getElementById("elem") as any;
        if (elem && elem.checkValidity) {
            return elem.checkValidity();
        }
        return true;
    }

    public get currentValue(): any {
        const elem = this.shadowRoot!.getElementById("elem") as any;
        if (elem && elem.value) {
            return elem.value;
        }
    }

    protected resolveOptions(options: any[] | undefined): [any, string][] | undefined {
        let resolved: [any, string][] | undefined;

        if (options && options.length > 0) {
            resolved = options.map(opt => {
                if (Array.isArray(opt)) {
                    return opt as [any, string];
                } else {
                    const optStr = "" + opt;
                    return [opt, i18next.t(optStr, {defaultValue: Util.camelCaseToSentenceCase(optStr)})]
                }
            });
        }

        return resolved;
    }

    protected getSelectedTextValue(options?: [string, string][] | undefined): string {
        const value = this.value;
        const values = Array.isArray(value) ? value as string[] : value != null ? [value as string] : undefined;
        if (!values) {
            return "";
        }
        const opts = options || this.resolveOptions(this.options);
        return !opts || !values ? "" : values.map(v => opts.find(([optValue, optDisplay], index) => v === optValue)).map((opt) => opt ? opt[1] : "").join(", ");
    }
}
