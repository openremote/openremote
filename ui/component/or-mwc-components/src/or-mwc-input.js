var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { ref, createRef } from "lit/directives/ref.js";
import { classMap } from "lit/directives/class-map.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { when } from 'lit/directives/when.js';
import { until } from 'lit/directives/until.js';
import { MDCTextField } from "@material/textfield";
import { MDCRipple } from "@material/ripple";
import { MDCCheckbox } from "@material/checkbox";
import { MDCSwitch } from "@material/switch";
import { MDCSlider } from "@material/slider";
import { MDCSelect } from "@material/select";
import { MDCList } from "@material/list";
import { MDCFormField } from "@material/form-field";
import { MDCIconButtonToggle } from "@material/icon-button";
import { DefaultColor4, DefaultColor5, DefaultColor8, Util } from "@openremote/core";
import "@openremote/or-icon";
import { getItemTemplate, getListTemplate, ListType } from "./or-mwc-list";
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
export class OrInputChangedEvent extends CustomEvent {
    constructor(value, previousValue, enterPressed) {
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
OrInputChangedEvent.NAME = "or-mwc-input-changed";
export var InputType;
(function (InputType) {
    InputType["BUTTON"] = "button";
    InputType["BUTTON_TOGGLE"] = "button-toggle";
    InputType["BUTTON_MOMENTARY"] = "button-momentary";
    InputType["CHECKBOX"] = "checkbox";
    InputType["CHECKBOX_LIST"] = "checkbox-list";
    InputType["COLOUR"] = "color";
    InputType["DATE"] = "date";
    InputType["DATETIME"] = "datetime-local";
    InputType["EMAIL"] = "email";
    InputType["JSON"] = "json";
    InputType["JSON_OBJECT"] = "json-object";
    InputType["MONTH"] = "month";
    InputType["NUMBER"] = "number";
    InputType["BIG_INT"] = "big-int";
    InputType["PASSWORD"] = "password";
    InputType["RADIO"] = "radio";
    InputType["SWITCH"] = "switch";
    InputType["RANGE"] = "range";
    InputType["TELEPHONE"] = "tel";
    InputType["TEXT"] = "text";
    InputType["TEXTAREA"] = "textarea";
    InputType["TIME"] = "time";
    InputType["URL"] = "url";
    InputType["WEEK"] = "week";
    InputType["SELECT"] = "select";
    InputType["LIST"] = "list";
    InputType["CRON"] = "cron";
    InputType["DURATION"] = "duration";
    InputType["DURATION_TIME"] = "duration-time";
    InputType["DURATION_PERIOD"] = "duration-period";
})(InputType || (InputType = {}));
function inputTypeSupportsButton(inputType) {
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
function inputTypeSupportsHelperText(inputType) {
    return inputTypeSupportsButton(inputType) || inputType === InputType.SELECT;
}
function inputTypeSupportsLabel(inputType) {
    return inputTypeSupportsHelperText(inputType) || inputType === InputType.CHECKBOX;
}
export const getValueHolderInputTemplateProvider = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {
    let inputType = options.inputType;
    let step;
    let pattern;
    let min;
    let max;
    let multiple;
    let required;
    let selectOptions;
    let valueConverter;
    const styles = {};
    const assetType = typeof assetDescriptor === "string" ? assetDescriptor : assetDescriptor.name;
    const constraints = (valueHolder && (valueHolder.meta) || (valueDescriptor && valueDescriptor.meta) ? Util.getAttributeValueConstraints(valueHolder, valueHolderDescriptor, assetType) : Util.getMetaValueConstraints(valueHolder, valueHolderDescriptor, assetType)) || [];
    const format = (valueHolder && (valueHolder.meta) || (valueDescriptor && valueDescriptor.meta) ? Util.getAttributeValueFormat(valueHolder, valueHolderDescriptor, assetType) : Util.getMetaValueFormat(valueHolder, valueHolderDescriptor, assetType));
    // Determine input type
    if (!inputType) {
        switch (valueDescriptor.name) {
            case "text" /* WellknownValueTypes.TEXT */:
            case "email" /* WellknownValueTypes.EMAIL */:
            case "UUID" /* WellknownValueTypes.UUID */:
            case "assetID" /* WellknownValueTypes.ASSETID */:
            case "hostOrIPAddress" /* WellknownValueTypes.HOSTORIPADDRESS */:
            case "IPAddress" /* WellknownValueTypes.IPADDRESS */:
                inputType = Util.getMetaValue("multiline" /* WellknownMetaItems.MULTILINE */, valueHolder, valueHolderDescriptor) === true ? InputType.TEXTAREA : InputType.TEXT;
                break;
            case "boolean" /* WellknownValueTypes.BOOLEAN */:
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
                }
                else {
                    inputType = InputType.CHECKBOX;
                }
                if (format && format.asMomentary) {
                    inputType = InputType.BUTTON_MOMENTARY;
                }
                break;
            case "bigNumber" /* WellknownValueTypes.BIGNUMBER */:
            case "number" /* WellknownValueTypes.NUMBER */:
            case "positiveInteger" /* WellknownValueTypes.POSITIVEINTEGER */:
            case "positiveNumber" /* WellknownValueTypes.POSITIVENUMBER */:
            case "long" /* WellknownValueTypes.LONG */:
            case "integer" /* WellknownValueTypes.INTEGER */:
            case "byte" /* WellknownValueTypes.BYTE */:
            case "integerByte" /* WellknownValueTypes.INTEGERBYTE */:
            case "direction" /* WellknownValueTypes.DIRECTION */:
            case "TCP_IPPortNumber" /* WellknownValueTypes.TCPIPPORTNUMBER */:
                if (valueDescriptor.name === "byte" /* WellknownValueTypes.BYTE */ || valueDescriptor.name === "integerByte" /* WellknownValueTypes.INTEGERBYTE */) {
                    min = 0;
                    max = 255;
                    step = 1;
                }
                else if (valueDescriptor.name === "integer" /* WellknownValueTypes.INTEGER */ || valueDescriptor.name === "long" /* WellknownValueTypes.LONG */) {
                    step = 1;
                }
                if (format && format.asDate) {
                    inputType = InputType.DATETIME;
                }
                else if (format && format.asBoolean) {
                    inputType = InputType.CHECKBOX;
                    valueConverter = (v) => v ? 1 : 0;
                }
                else if (format && format.asSlider) {
                    inputType = InputType.RANGE;
                }
                else {
                    inputType = InputType.NUMBER;
                }
                break;
            case "bigInteger" /* WellknownValueTypes.BIGINTEGER */:
                inputType = InputType.BIG_INT;
                step = 1;
                break;
            case "colourRGB" /* WellknownValueTypes.COLOURRGB */:
                inputType = InputType.COLOUR;
                break;
            case "dateAndTime" /* WellknownValueTypes.DATEANDTIME */:
            case "timestamp" /* WellknownValueTypes.TIMESTAMP */:
            case "timestampISO8601" /* WellknownValueTypes.TIMESTAMPISO8601 */:
                inputType = InputType.DATETIME;
                break;
            case "CRONExpression" /* WellknownValueTypes.CRONEXPRESSION */:
                inputType = InputType.CRON;
                break;
            case "timeDurationISO8601" /* WellknownValueTypes.TIMEDURATIONISO8601 */:
                inputType = InputType.DURATION_TIME;
                break;
            case "periodDurationISO8601" /* WellknownValueTypes.PERIODDURATIONISO8601 */:
                inputType = InputType.DURATION_PERIOD;
                break;
            case "timeAndPeriodDurationISO8601" /* WellknownValueTypes.TIMEANDPERIODDURATIONISO8601 */:
                inputType = InputType.DURATION;
                break;
            case "JSONObject" /* WellknownValueTypes.JSONOBJECT */:
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
    const sizeConstraint = constraints && constraints.find(c => c.type === "size");
    const patternConstraint = constraints && constraints.find(c => c.type === "pattern");
    const minConstraint = constraints && constraints.find(c => c.type === "min");
    const maxConstraint = constraints && constraints.find(c => c.type === "max");
    const allowedValuesConstraint = constraints && constraints.find(c => c.type === "allowedValues");
    const pastConstraint = constraints && constraints.find(c => c.type === "past");
    const pastOrPresentConstraint = constraints && constraints.find(c => c.type === "pastOrPresent");
    const futureConstraint = constraints && constraints.find(c => c.type === "future");
    const futureOrPresentConstraint = constraints && constraints.find(c => c.type === "futureOrPresent");
    const notEmptyConstraint = constraints && constraints.find(c => c.type === "notEmpty");
    const notBlankConstraint = constraints && constraints.find(c => c.type === "notBlank");
    const notNullConstraint = constraints && constraints.find(c => c.type === "notNull");
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
    }
    else if (notEmptyConstraint && !pattern) {
        pattern = ".+";
    }
    if (allowedValuesConstraint && allowedValuesConstraint.allowedValues) {
        const allowedLabels = allowedValuesConstraint.allowedValueNames && allowedValuesConstraint.allowedValueNames.length === allowedValuesConstraint.allowedValues.length ? allowedValuesConstraint.allowedValueNames : undefined;
        selectOptions = allowedValuesConstraint.allowedValues.map((v, i) => {
            let label = allowedLabels ? allowedLabels[i] : "" + v;
            label = Util.getAllowedValueLabel(label);
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
        }
        else if (futureConstraint || futureOrPresentConstraint) {
            min = new Date();
            max = undefined;
        }
        // Refine the input type based on formatting
        if (format) {
            if (format.timeStyle && !format.dateStyle) {
                inputType = InputType.TIME;
            }
            else if (format.dateStyle && !format.timeStyle) {
                inputType = InputType.DATE;
            }
        }
    }
    if (inputType === InputType.NUMBER && format && format.resolution) {
        step = format.resolution;
    }
    if (inputType === InputType.COLOUR) {
        styles.marginLeft = "24px";
    }
    const supportsHelperText = inputTypeSupportsHelperText(inputType);
    const supportsLabel = inputTypeSupportsLabel(inputType);
    const supportsSendButton = inputTypeSupportsButton(inputType);
    const readonly = options.readonly;
    required = required || options.required;
    const comfortable = options.comfortable;
    const resizeVertical = options.resizeVertical;
    const inputRef = createRef();
    const templateFunction = (value, focused, loading, sending, error, helperText) => {
        const disabled = options.disabled || loading || sending;
        const label = supportsLabel ? options.label : undefined;
        return html `<or-mwc-input ${ref(inputRef)} id="input" style="${styleMap(styles)}" .type="${inputType}" .label="${label}" .value="${value}" .pattern="${pattern}"
            .min="${min}" .max="${max}" .format="${format}" .focused="${focused}" .required="${required}" .multiple="${multiple}"
            .options="${selectOptions}" .comfortable="${comfortable}" .readonly="${readonly}" .disabled="${disabled}" .step="${step}"
            .helperText="${helperText}" .helperPersistent="${true}" .resizeVertical="${resizeVertical}"
            .rounded="${options.rounded}"
            .outlined="${options.outlined}"
            @or-mwc-input-changed="${(e) => {
            e.stopPropagation();
            e.detail.value = valueConverter ? valueConverter(e.detail.value) : e.detail.value;
            valueChangeNotifier(e.detail);
        }}"></or-mwc-input>`;
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
};
// language=CSS
const style = css `
    
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
let OrMwcInput = class OrMwcInput extends LitElement {
    constructor() {
        super(...arguments);
        this.readonly = false;
        this.required = false;
        this.checked = false;
        this.indeterminate = false;
        this.multiple = false;
        this.compact = false;
        this.comfortable = false;
        /* BUTTON STYLES START */
        this.raised = false;
        this.action = false;
        this.unElevated = false;
        this.outlined = false;
        this.rounded = false;
        this.disableSliderNumberInput = false;
        /* BUTTON STYLES END */
        /* TEXT INPUT STYLES START */
        this.fullWidth = false;
        this.helperPersistent = false;
        this.autoValidate = false;
        this.charCounter = false;
        this.disabled = false;
        this.continuous = false;
        this.resizeVertical = false;
        this._selectedIndex = -1;
        this.isUiValid = true;
    }
    static get styles() {
        return [
            css `${unsafeCSS(iconButtonStyle)}`,
            css `${unsafeCSS(buttonStyle)}`,
            css `${unsafeCSS(buttonFabStyle)}`,
            css `${unsafeCSS(textfieldStyle)}`,
            css `${unsafeCSS(rippleStyle)}`,
            css `${unsafeCSS(lineRippleStyle)}`,
            css `${unsafeCSS(floatingLabelStyle)}`,
            css `${unsafeCSS(formFieldStyle)}`,
            css `${unsafeCSS(checkboxStyle)}`,
            css `${unsafeCSS(radioStyle)}`,
            css `${unsafeCSS(switchStyle)}`,
            css `${unsafeCSS(selectStyle)}`,
            css `${unsafeCSS(listStyle)}`,
            css `${unsafeCSS(menuStyle)}`,
            css `${unsafeCSS(menuSurfaceStyle)}`,
            css `${unsafeCSS(sliderStyle)}`,
            style
        ];
    }
    get nativeValue() {
        if (this._mdcComponent) {
            return this._mdcComponent.value;
        }
    }
    disconnectedCallback() {
        var _a, _b;
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
            (_a = this._menuObserver) === null || _a === void 0 ? void 0 : _a.disconnect();
        }
        if (this._mdcComponent2) {
            this._mdcComponent2.destroy();
            this._mdcComponent2 = undefined;
            (_b = this._menuObserver) === null || _b === void 0 ? void 0 : _b.disconnect();
        }
    }
    shouldUpdate(_changedProperties) {
        if (_changedProperties.has("indeterminate")) {
            if (this._mdcComponent && this.type === InputType.CHECKBOX) {
                this._mdcComponent.indeterminate = this.indeterminate;
            }
        }
        if (_changedProperties.has("disabled")) {
            if (this._mdcComponent) {
                this._mdcComponent.disabled = this.disabled;
            }
            if (this.type === InputType.RANGE && this._mdcComponent2) {
                this._mdcComponent2.disabled = this.disabled;
            }
        }
        if (_changedProperties.has("readonly")) {
            if (this._mdcComponent) {
                this._mdcComponent.readonly = this.readonly;
            }
            if (this.type === InputType.RANGE && this._mdcComponent2) {
                this._mdcComponent2.readonly = this.readonly;
            }
        }
        if (!this.type && this.value) {
            if (this.value instanceof Date) {
                this.type = InputType.DATETIME;
            }
            else if (typeof (this.value) === "boolean") {
                this.type = InputType.CHECKBOX;
            }
            else if (typeof (this.value) === "number") {
                this.type = InputType.NUMBER;
            }
            else if (typeof (this.value) === "string") {
                this.type = InputType.TEXT;
            }
            else {
                this.type = InputType.JSON;
            }
        }
        return true;
    }
    focus() {
        if (this.type === InputType.RANGE && this._mdcComponent2) {
            this._mdcComponent2.focus();
        }
        else if (this._mdcComponent && typeof this._mdcComponent.focus === "function") {
            this._mdcComponent.focus();
        }
    }
    render() {
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
            let labelTemplate = showLabel ? html `<span class="mdc-floating-label ${hasValue ? "mdc-floating-label--float-above" : ""}" id="label">${this.label}</span>` : undefined;
            switch (this.type) {
                case InputType.RADIO:
                    const optsRadio = this.resolveOptions(this.options);
                    this._selectedIndex = -1;
                    return html `
                            <div class="mdc-radio-container">
                                ${optsRadio ? optsRadio.map(([optValue, optDisplay], index) => {
                        if (this.value === optValue) {
                            this._selectedIndex = index;
                        }
                        return html `
                                    <div id="field" class="mdc-form-field">
                                        <div class="mdc-radio">
                                            <input type="radio" 
                                                id="elem-${optValue}"
                                                name="${ifDefined(this.name)}"
                                                value="${optValue}"
                                                ?checked="${this.value && this.value.includes(optValue)}"
                                                ?required="${this.required}"
                                                ?disabled="${this.disabled || this.readonly}"                            
                                                @change="${(e) => this.onValueChange(e.target, optValue)}"
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
                    return html `
                        <span id="wrapper">
                            ${this.label ? html `<label for="elem" class="${this.disabled ? "mdc-switch--disabled" : ""}">${this.label}</label>` : ``}
                            <div id="component" class="mdc-switch ${classMap(classesSwitch)}">
                                <div class="mdc-switch__track"></div>
                                <div class="mdc-switch__thumb-underlay">
                                    <div class="mdc-switch__thumb">
                                        <input type="checkbox" id="elem" class="mdc-switch__native-control" 
                                        ?checked="${this.value}"
                                        ?required="${this.required}"
                                        ?disabled="${this.disabled || this.readonly}"
                                        @change="${(e) => this.onValueChange(e.target, e.target.checked)}"
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
                    return html `
                            <div id="component" class="mdc-list mdc-select ${classMap(classesList)}" @MDCList:action="${(e) => this.onValueChange(undefined, e.detail.index === -1 ? undefined : Array.isArray(this.options[e.detail.index]) ? this.options[e.detail.index][0] : this.options[e.detail.index])}">
                                <ul class="mdc-list">
                                    ${optsList ? optsList.map(([optValue, optDisplay], index) => {
                        if (this.value === optValue) {
                            this._selectedIndex = index;
                        }
                        // todo: it's not actually putting the mdc-list-item--selected class on even when this.value === optValue...
                        return html `<li class="${classMap({ "mdc-list-item": true, "mdc-list-item--selected": this.value === optValue })}" role="option" data-value="${optValue}"><or-translate value="${optDisplay}"></or-translate></li>`;
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
                    let opts;
                    if (this.searchProvider != undefined) {
                        opts = this.searchProvider(this.searchableValue);
                    }
                    else {
                        opts = this.resolveOptions(this.options);
                    }
                    const itemClickHandler = (ev, item) => {
                        var _a;
                        const value = item.value;
                        if (this.multiple) {
                            ev.stopPropagation();
                            const inputValue = (_a = this._tempValue) !== null && _a !== void 0 ? _a : (Array.isArray(this.value) ? [...this.value] : this.value !== undefined ? [this.value] : []);
                            const index = inputValue.findIndex((v) => v === value);
                            if (index >= 0) {
                                inputValue.splice(index, 1);
                            }
                            else {
                                inputValue.push(value);
                            }
                            const listItemEl = ev.composedPath()[0].closest("li"), iconEl = listItemEl.getElementsByTagName("or-icon")[0];
                            if (listItemEl) {
                                if (index >= 0) {
                                    listItemEl.classList.remove("mdc-list-item--selected");
                                }
                                else {
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
                        else if (this.searchProvider != undefined || !Array.isArray(opts)) {
                            this.onValueChange(undefined, item.value);
                        }
                    };
                    const menuCloseHandler = () => {
                        var _a;
                        const v = ((_a = this._tempValue) !== null && _a !== void 0 ? _a : this.value);
                        window.setTimeout(() => {
                            if (this._mdcComponent) {
                                // Hack to stop label moving down when there is a value set
                                this._mdcComponent.foundation.adapter.floatLabel(v && (!Array.isArray(v) || v.length > 0));
                            }
                        });
                        if (!this._tempValue) {
                            return;
                        }
                        const val = [...this._tempValue];
                        this._tempValue = undefined;
                        this.onValueChange(undefined, val);
                    };
                    const listTemplate = (options) => {
                        if (this.searchProvider != undefined && (!options || options.length == 0)) {
                            return html `<span class="mdc-text-field-helper-line" style="margin: 8px 8px 8px 0;">${i18next.t('noResults')}</span>`;
                        }
                        else {
                            return getListTemplate(this.multiple ? ListType.MULTI_TICK : ListType.SELECT, html `${options === null || options === void 0 ? void 0 : options.map(([optValue, optDisplay], index) => {
                                return getItemTemplate({
                                    text: optDisplay,
                                    value: optValue
                                }, index, Array.isArray(this.value) ? this.value : this.value ? [this.value] : [], this.multiple ? ListType.MULTI_TICK : ListType.SELECT, false, itemClickHandler);
                            })}`, false, undefined);
                        }
                    };
                    return html `
                        <div id="component"
                            class="mdc-select ${classMap(classes)}"
                            @MDCSelect:change="${(e) => __awaiter(this, void 0, void 0, function* () {
                        const options = (Array.isArray(opts) ? opts : yield opts);
                        this.onValueChange(undefined, e.detail.index === -1 ? undefined : Array.isArray(options[e.detail.index]) ? options[e.detail.index][0] : options[e.detail.index]);
                    })}">
                                <div class="mdc-select__anchor" role="button"
                                     aria-haspopup="listbox"
                                     aria-expanded="false"
                                     aria-disabled="${"" + (this.disabled || this.readonly)}"
                                     aria-labelledby="label selected-text">
                                    ${!outlined ? html `<span class="mdc-select__ripple"></span>` : undefined}
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
                                    ${!outlined ? html `<div class="mdc-line-ripple"></div>` : ``}
                                </div>
                                <div id="mdc-select-menu" class="mdc-select__menu mdc-menu mdc-menu-surface mdc-menu-surface--fixed ${this.searchProvider != undefined ? 'mdc-menu__searchable' : undefined}" @MDCMenuSurface:closed="${menuCloseHandler}">
                                    ${when(this.searchProvider != undefined, () => html `
                                        <label id="select-searchable" class="mdc-text-field mdc-text-field--filled">
                                            <span class="mdc-floating-label" style="color: rgba(0, 0, 0, 0.6); text-transform: capitalize; visibility: ${this.searchableValue ? 'hidden' : 'visible'}" id="my-label-id">${i18next.t('search')}</span>
                                            <input class="mdc-text-field__input" type="text"
                                                   @keyup="${(e) => this.searchableValue = e.target.value}"
                                            />
                                        </label>
                                    `)}
                                    ${when(Array.isArray(opts), () => {
                        return listTemplate(opts);
                    }, () => {
                        return until(new Promise((resolve) => __awaiter(this, void 0, void 0, function* () {
                            resolve(listTemplate(yield opts));
                        })), html `<span class="mdc-text-field-helper-line" style="margin: 8px 8px 8px 0;">${i18next.t('loading')}</span>`);
                    })}
                                </div>
                                ${hasHelper || showValidationMessage ? html `
                                    <p id="component-helper-text" class="mdc-select-helper-text ${classMap(helperClasses)}" aria-hidden="true">
                                        ${showValidationMessage ? this.errorMessage || this.validationMessage : this.helperText}
                                    </p>` : ``}
                        </div>
                    `;
                case InputType.BUTTON_TOGGLE:
                    return html `
                        <button id="component" class="mdc-icon-button ${this.value ? "mdc-icon-button--on" : ""}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @MDCIconButtonToggle:change="${(evt) => this.onValueChange(undefined, evt.detail.isOn)}">
                            ${this.icon ? html `<or-icon class="mdc-icon-button__icon" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.iconOn ? html `<or-icon class="mdc-icon-button__icon mdc-icon-button__icon--on" aria-hidden="true" icon="${this.iconOn}"></or-icon>` : ``}
                        </button>
                    `;
                case InputType.BUTTON:
                case InputType.BUTTON_MOMENTARY: {
                    const onMouseDown = (ev) => {
                        if (this.disabled) {
                            ev.stopPropagation();
                        }
                        if (isMomentary)
                            this.dispatchEvent(new OrInputChangedEvent(true, null));
                    };
                    const onMouseUp = (ev) => {
                        if (this.disabled) {
                            ev.stopPropagation();
                        }
                        isMomentary ? this.dispatchEvent(new OrInputChangedEvent(false, true)) : this.dispatchEvent(new OrInputChangedEvent(true, null));
                    };
                    const onClick = (ev) => {
                        if (this.disabled) {
                            ev.stopPropagation();
                        }
                    };
                    const isMomentary = this.type === InputType.BUTTON_MOMENTARY;
                    const isIconButton = !this.action && !this.label;
                    let classes = {
                        "mdc-icon-button": isIconButton,
                        "mdc-fab": !isIconButton && this.action,
                        "mdc-fab--extended": !isIconButton && this.action && !!this.label,
                        "mdc-fab--mini": !isIconButton && this.action && (this.compact || this.comfortable),
                        "mdc-button": !isIconButton && !this.action,
                        "mdc-button--raised": !isIconButton && !this.action && this.raised,
                        "mdc-button--unelevated": !isIconButton && !this.action && this.unElevated,
                        "mdc-button--outlined": !isIconButton && !this.action && this.outlined,
                        "mdc-button--rounded": !isIconButton && !this.action && this.rounded,
                        "mdc-button--fullwidth": this.fullWidth,
                    };
                    return html `
                        <button id="component" class="${classMap(classes)}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @click="${(ev) => onClick(ev)}"
                            @mousedown="${(ev) => onMouseDown(ev)}" @mouseup="${(ev) => onMouseUp(ev)}">
                            ${!isIconButton ? html `<div class="mdc-button__ripple"></div>` : ``}
                            ${this.icon ? html `<or-icon class="${isIconButton ? "" : this.action ? "mdc-fab__icon" : "mdc-button__icon"}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.label ? html `<span class="${this.action ? "mdc-fab__label" : "mdc-button__label"}"><or-translate .value="${this.label}"></or-translate></span>` : ``}
                            ${!isIconButton && this.iconTrailing ? html `<or-icon class="${this.action ? "mdc-fab__icon" : "mdc-button__icon"}" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                        </button>
                    `;
                }
                case InputType.CHECKBOX_LIST:
                    if (!Array.isArray(this.value)) {
                        if (this.value === null || this.value === undefined) {
                            this.value = [];
                        }
                        else {
                            this.value = [this.value];
                        }
                    }
                    const optsCheckboxList = this.resolveOptions(this.options);
                    this._selectedIndex = -1;
                    return html `
                            <div class="mdc-checkbox-list">
                                ${optsCheckboxList ? optsCheckboxList.map(([optValue, optDisplay], index) => {
                        if (this.value === optValue) {
                            this._selectedIndex = index;
                        }
                        return html `
                                    <div id="field" class="mdc-form-field">
                                        <div id="component" class="mdc-checkbox">
                                            <input type="checkbox"
                                                ?checked="${this.value && this.value.includes(optValue)}"
                                                ?required="${this.required}"
                                                name="${optValue}"
                                                ?disabled="${this.disabled || this.readonly}"
                                                @change="${(e) => {
                            let val = this.value;
                            if (e.target.checked) {
                                if (!val.includes(optValue)) {
                                    val = [optValue, ...val];
                                }
                            }
                            else {
                                val = val.filter((v) => v !== optValue);
                            }
                            this.onValueChange(e.target, val);
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
                    return html `
                        <div id="field" class="mdc-form-field">
                            <div id="component" class="${classMap(classList)}">
                                <input type="checkbox" 
                                    id="elem" 
                                    data-indeterminate="${this.indeterminate}"
                                    ?checked="${this.value}"
                                    ?required="${this.required}"
                                    ?disabled="${this.disabled || this.readonly}"
                                    @change="${(e) => this.onValueChange(e.target, e.target.checked)}"
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
                    return html `
                        <div id="component" style="width: 100%; display: inline-flex; align-items: center; padding: 8px 0;">
                            <input type="color" id="elem" style="border: none; height: 31px; width: 31px; padding: 1px 3px; min-height: 22px; min-width: 30px;cursor: pointer" value="${this.value}"
                                   ?disabled="${this.disabled || this.readonly}"
                                   ?required="${this.required}"
                                   @change="${(e) => this.onValueChange(e.target, e.target.value)}"
                            />
                            <label style="margin-left: 10px; cursor: pointer" for="elem">${this.label}</label>
                        </div>
                    `;
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
                    let valMinMax = [this.value === undefined || this.value === null ? undefined : this.value, this.min, this.max];
                    if (valMinMax.some((v) => typeof (v) !== "string")) {
                        if (this.type === InputType.JSON || this.type === InputType.JSON_OBJECT) {
                            if (valMinMax[0] !== undefined) {
                                if (typeof valMinMax[0] !== "string" || valMinMax[0] === null) {
                                    try {
                                        valMinMax[0] = JSON.stringify(valMinMax[0], null, 2);
                                    }
                                    catch (e) {
                                        console.warn("Failed to parse JSON expression for input control");
                                        valMinMax[0] = "";
                                    }
                                }
                            }
                            else {
                                valMinMax[0] = "";
                            }
                        }
                        else {
                            const format = this.format ? Object.assign({}, this.format) : {};
                            switch (this.type) {
                                case InputType.TIME:
                                    format.asDate = true;
                                    format.hour12 = false;
                                    format.timeStyle = this.step && this.step < 60 ? "medium" /* ValueFormatStyleRepresentation.MEDIUM */ : "short" /* ValueFormatStyleRepresentation.SHORT */;
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
                                    format.maximumFractionDigits = 20; // default according to Web documentation
                                    break;
                            }
                            // Numbers/dates must be in english locale without commas etc.
                            format.useGrouping = false;
                            valMinMax = valMinMax.map((val) => val !== undefined ? Util.getValueAsString(val, () => format, "en-GB") : undefined);
                        }
                    }
                    let inputElem;
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
                            ? html `
                                <textarea id="elem" class="mdc-text-field__input ${this.resizeVertical ? "resize-vertical" : ""}" ?required="${this.required}"
                                ?readonly="${this.readonly}" ?disabled="${this.disabled}" minlength="${ifDefined(this.minLength)}"
                                maxlength="${ifDefined(this.maxLength)}" rows="${this.rows ? this.rows : 5}"
                                cols="${ifDefined(this.cols)}" aria-label="${ifDefined(label)}"
                                aria-labelledby="${ifDefined(label ? "label" : undefined)}"
                                @change="${(e) => this.onValueChange(e.target, e.target.value)}">${valMinMax[0] ? valMinMax[0] : ""}</textarea>`
                            : html `
                            <input type="${type}" id="elem" aria-labelledby="${ifDefined(label ? "label" : undefined)}"
                            class="mdc-text-field__input" ?required="${this.required}" ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}" min="${ifDefined(valMinMax[1])}" max="${ifDefined(valMinMax[2])}"
                            step="${this.step ? this.step : "any"}" minlength="${ifDefined(this.minLength)}" pattern="${ifDefined(this.pattern)}"
                            maxlength="${ifDefined(this.maxLength)}" placeholder="${ifDefined(this.placeHolder)}"
                            .value="${valMinMax[0] !== null && valMinMax[0] !== undefined ? valMinMax[0] : ""}"
                            @keydown="${(e) => {
                                if ((e.code === "Enter" || e.code === "NumpadEnter")) {
                                    this.onValueChange(e.target, e.target.value, true);
                                }
                            }}"
                            @blur="${(e) => { if (e.target.value === "")
                                this.reportValidity(); }}"
                            @change="${(e) => this.onValueChange(e.target, e.target.value)}" />`;
                        inputElem = html `
                            <label id="${componentId}" class="${classMap(classes)}">
                                ${this.icon ? html `<or-icon class="mdc-text-field__icon mdc-text-field__icon--leading" style="color: ${this.iconColor ? "#" + this.iconColor : "unset"}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                                ${outlined ? `` : html `<span class="mdc-text-field__ripple"></span>`}
                                ${inputElem}
                                ${outlined ? this.renderOutlined(labelTemplate) : labelTemplate}
                                ${outlined ? `` : html `<span class="mdc-line-ripple"></span>`}
                                ${this.iconTrailing ? html `<or-icon class="mdc-text-field__icon mdc-text-field__icon--trailing" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                            </label>
                            ${hasHelper || showValidationMessage ? html `
                                <div class="mdc-text-field-helper-line">
                                    <div class="mdc-text-field-helper-text ${classMap(helperClasses)}">${showValidationMessage ? this.errorMessage || this.validationMessage : this.helperText}</div>
                                    ${this.charCounter && !this.readonly ? html `<div class="mdc-text-field-character-counter"></div>` : ``}
                                </div>
                            ` : ``}
                        `;
                    }
                    if (this.type === InputType.RANGE) {
                        const classes = {
                            "mdc-slider": true,
                            "mdc-slider--range": this.continuous,
                            "mdc-slider--discreet": !this.continuous,
                            "mdc-slider--disabled": this.disabled
                        };
                        inputElem = html `
                            <span id="wrapper">
                                ${this.label ? html `<label for="component" class="${this.disabled ? "mdc-switch--disabled" : ""}">${this.label}</label>` : ``}
                                <div id="component" class="${classMap(classes)}" @MDCSlider:change="${(ev) => this.onValueChange(undefined, ev.detail.value)}">
                                    <input id="elem" class="mdc-slider__input" type="range" min="${ifDefined(valMinMax[1])}" max="${ifDefined(valMinMax[2])}" value="${valMinMax[0] || valMinMax[1] || 0}" name="slider" step="${this.step || 1}" ?readonly="${this.readonly}" ?disabled="${this.disabled}" aria-label="${ifDefined(this.label)}" />
                                    <div class="mdc-slider__track">
                                        <div class="mdc-slider__track--inactive"></div>
                                        <div class="mdc-slider__track--active">
                                            <div class="mdc-slider__track--active_fill"></div>
                                        </div>
                                    </div>
                                    <div class="mdc-slider__thumb">
                                        ${!this.continuous ? html `<div class="mdc-slider__value-indicator-container" aria-hidden="true">
                                            <div class="mdc-slider__value-indicator">
                                                <span class="mdc-slider__value-indicator-text">
                                                  50
                                                </span>
                                            </div>
                                        </div>` : ``}
                                        <div class="mdc-slider__thumb-knob"></div>
                                    </div>
                                </div>
                                ${inputElem ? html `<div style="min-width: 70px; width: 70px;">${inputElem}</div>` : ``}
                            </span>
                        `;
                    }
                    return inputElem;
                }
            }
        }
        return html `<span>INPUT TYPE NOT IMPLEMENTED</span>`;
    }
    _getFormat() {
        if (this.format) {
            return this.format;
        }
    }
    update(_changedProperties) {
        if (_changedProperties.has('autoValidate') && this._mdcComponent) {
            const comp = this._mdcComponent;
            if (comp.foundation && comp.foundation.setValidateOnValueChange) {
                comp.foundation.setValidateOnValueChange(this.autoValidate);
            }
        }
        super.update(_changedProperties);
    }
    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        if (this.autoValidate) {
            this.reportValidity();
        }
    }
    updated(_changedProperties) {
        var _a, _b;
        super.updated(_changedProperties);
        if (_changedProperties.has("type")) {
            const component = this.shadowRoot.getElementById("component");
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
                            this._mdcComponent.foundation.adapter.getSelectedIndex = () => {
                                // Return first item index
                                if (!Array.isArray(this.value) || this.value.length === 0) {
                                    return -1;
                                }
                                const firstSelected = this.value[0];
                                const items = this._mdcComponent.foundation.adapter.getMenuItemValues();
                                return items.indexOf(firstSelected);
                            };
                        }
                        mdcSelect.useDefaultValidation = !this.multiple;
                        mdcSelect.valid = !this.required || (!this.multiple && mdcSelect.valid) || (this.multiple && Array.isArray(this.value) && this.value.length > 0);
                        const selectedText = this.getSelectedTextValue();
                        this._mdcComponent.foundation.adapter.setSelectedText(selectedText);
                        this._mdcComponent.foundation.adapter.floatLabel(!!selectedText);
                        // Set width of fixed select menu to match the component width
                        // Using an observer to prevent forced reflow / DOM measurements; prevents blocking the thread
                        if (!this._menuObserver) {
                            this._menuObserver = new IntersectionObserver((entries, observer) => {
                                var _a, _b;
                                if (entries[0].target.style.minWidth != (((_a = entries[0].target.parentElement) === null || _a === void 0 ? void 0 : _a.clientWidth) + "px")) {
                                    entries[0].target.style.minWidth = ((_b = entries[0].target.parentElement) === null || _b === void 0 ? void 0 : _b.clientWidth) + "px";
                                }
                            });
                            this._menuObserver.observe(this.shadowRoot.getElementById("mdc-select-menu"));
                        }
                        // This overrides the standard mdc menu body click capture handler as it doesn't work with webcomponents
                        const searchable = (this.searchProvider !== undefined);
                        const multi = this.multiple;
                        mdcSelect.menu.menuSurface_.foundation.handleBodyClick = function (evt) {
                            const el = evt.composedPath()[0]; // Use composed path not evt target to work with webcomponents
                            if (this.adapter.isElementInContainer(el)) {
                                if (!searchable) {
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
                            mdcSelect.menu.menuSurface_.close();
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
                        const field = this.shadowRoot.getElementById("field");
                        if (field) {
                            const mdcField = new MDCFormField(field);
                            mdcField.input = this._mdcComponent;
                            this._mdcComponent2 = mdcField;
                        }
                        break;
                    case InputType.SWITCH:
                        this._mdcComponent = new MDCSwitch(component);
                        break;
                    case InputType.RANGE:
                        this._mdcComponent = new MDCSlider(component);
                        const numberComponent = this.shadowRoot.getElementById("number");
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
                if (this._mdcComponent && this.focused && typeof (this._mdcComponent.focus) === "function") {
                    this._mdcComponent.focus();
                }
            }
        }
        else {
            // some components need to be kept in sync with the DOM
            if (this.type === InputType.SELECT && this._mdcComponent) {
                if (_changedProperties.has("options")) {
                    this._mdcComponent.layoutOptions(); // has big impact on performance when the MDCSelect list is large.
                }
                this._mdcComponent.disabled = !!(this.disabled || this.readonly);
                this._mdcComponent.useDefaultValidation = !this.multiple;
                this._mdcComponent.valid = !this.required || (!this.multiple && this._mdcComponent.valid) || (this.multiple && Array.isArray(this.value) && this.value.length > 0);
                const selectedText = this.getSelectedTextValue();
                this._mdcComponent.foundation.adapter.setSelectedText(selectedText);
                this._mdcComponent.foundation.adapter.floatLabel(!!selectedText);
            }
            else if (this.type === InputType.RANGE && this._mdcComponent) {
                const slider = this._mdcComponent;
                slider.setDisabled(this.disabled || this.readonly);
                // slider.getDefaultFoundation(). getDefaultFoundation()..getMax() = this.min;
                // slider.max = this.max;
                slider.setValue(this.value);
            }
            else if (this.type === InputType.SWITCH && this._mdcComponent) {
                const swtch = this._mdcComponent;
                swtch.checked = this.value;
            }
            else if (this.type === InputType.CHECKBOX && this._mdcComponent) {
                const checkbox = this._mdcComponent;
                checkbox.checked = !!this.value;
                checkbox.disabled = !!(this.disabled || this.readonly);
            }
            if (this._mdcComponent) {
                this._mdcComponent.required = !!this.required;
            }
        }
        if (_changedProperties.has("label")) {
            (_b = (_a = this._mdcComponent) === null || _a === void 0 ? void 0 : _a.layout) === null || _b === void 0 ? void 0 : _b.call(_a); // Adjusts the dimensions and positions for all sub-elements.
        }
        if (this.autoValidate) {
            this.reportValidity();
        }
    }
    renderOutlined(labelTemplate) {
        return html `
            <span class="mdc-notched-outline">
                <span class="mdc-notched-outline__leading"></span>
                ${labelTemplate ? html `
                <span class="mdc-notched-outline__notch">
                    ${labelTemplate}
                </span>
                ` : ``}
                <span class="mdc-notched-outline__trailing"></span>
            </span>
        `;
    }
    setCustomValidity(msg) {
        this.errorMessage = msg;
        const elem = this.shadowRoot.getElementById("elem");
        if (elem && elem.setCustomValidity) {
            elem.setCustomValidity(msg !== null && msg !== void 0 ? msg : "");
        }
        this.reportValidity();
    }
    checkValidity() {
        const elem = this.shadowRoot.getElementById("elem");
        let valid = true;
        if (elem && elem.validity) {
            const nativeValidity = elem.validity;
            valid = nativeValidity.valid;
        }
        if (valid && (this.type === InputType.JSON || this.type === InputType.JSON_OBJECT)) {
            // JSON needs special validation - if no text value but this.value then parsing failed
            if (this.value !== undefined && this.value !== null && this._mdcComponent.value === "") {
                valid = false;
            }
        }
        return valid;
    }
    reportValidity() {
        const isValid = this.checkValidity();
        this.isUiValid = isValid;
        if (this._mdcComponent) {
            this._mdcComponent.valid = isValid;
        }
        return isValid;
    }
    onValueChange(elem, newValue, enterPressed) {
        var _a, _b;
        let previousValue = this.value;
        let errorMsg;
        if (newValue === "undefined") {
            previousValue = null;
            newValue = undefined;
        }
        if (newValue === "null") {
            previousValue = undefined;
            newValue = null;
        }
        if (typeof (newValue) === "string") {
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
                    }
                    else {
                        try {
                            newValue = JSON.parse(newValue);
                            if (this.type === InputType.JSON_OBJECT && (typeof newValue !== 'object' || Array.isArray(newValue))) {
                                newValue = this.value;
                                errorMsg = i18next.t("validation.invalidJSON");
                            }
                        }
                        catch (e) {
                            newValue = this.value;
                            errorMsg = this.type === InputType.JSON || this.type == InputType.JSON_OBJECT ? i18next.t("validation.invalidJSON") : i18next.t("validation.invalidNumber");
                        }
                    }
                    break;
                case InputType.DATETIME:
                    if (newValue === "") {
                        newValue = null;
                    }
                    else {
                        try {
                            newValue = Date.parse(newValue);
                        }
                        catch (e) {
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
                this._mdcComponent.setValue(newValue);
                if (this._mdcComponent2) {
                    this._mdcComponent2.value = newValue;
                }
            }
            this.dispatchEvent(new OrInputChangedEvent(this.value, previousValue, enterPressed));
        }
        // Reset search if value has been selected
        if (this.searchProvider != undefined && this.type === InputType.SELECT) {
            const searchableElement = (_b = (_a = this.shadowRoot) === null || _a === void 0 ? void 0 : _a.getElementById('select-searchable')) === null || _b === void 0 ? void 0 : _b.children[1];
            if (searchableElement) {
                this.searchableValue = undefined;
                searchableElement.value = "";
            }
        }
        if (this.type === InputType.CHECKBOX_LIST && !Util.objectsEqual(newValue, previousValue, true)) {
            this.dispatchEvent(new OrInputChangedEvent(newValue, previousValue, enterPressed));
        }
    }
    get valid() {
        const elem = this.shadowRoot.getElementById("elem");
        if (elem && elem.checkValidity) {
            return elem.checkValidity();
        }
        return true;
    }
    get currentValue() {
        const elem = this.shadowRoot.getElementById("elem");
        if (elem && elem.value) {
            return elem.value;
        }
    }
    resolveOptions(options) {
        let resolved;
        if (options && options.length > 0) {
            resolved = options.map(opt => {
                if (Array.isArray(opt)) {
                    return opt;
                }
                else {
                    const optStr = "" + opt;
                    return [opt, i18next.t(optStr, { defaultValue: Util.camelCaseToSentenceCase(optStr) })];
                }
            });
        }
        return resolved;
    }
    getSelectedTextValue(options) {
        const value = this.value;
        const values = Array.isArray(value) ? value : value != null ? [value] : undefined;
        if (!values) {
            return "";
        }
        const opts = options || this.resolveOptions(this.options);
        return !opts || !values ? "" : values.map(v => opts.find(([optValue, optDisplay], index) => v === optValue)).map((opt) => opt ? opt[1] : "").join(", ");
    }
};
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "focused", void 0);
__decorate([
    property()
], OrMwcInput.prototype, "value", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "type", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "name", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "readonly", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "required", void 0);
__decorate([
    property()
], OrMwcInput.prototype, "max", void 0);
__decorate([
    property()
], OrMwcInput.prototype, "min", void 0);
__decorate([
    property({ type: Number })
], OrMwcInput.prototype, "step", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "checked", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "indeterminate", void 0);
__decorate([
    property({ type: Number })
], OrMwcInput.prototype, "maxLength", void 0);
__decorate([
    property({ type: Number })
], OrMwcInput.prototype, "minLength", void 0);
__decorate([
    property({ type: Number })
], OrMwcInput.prototype, "rows", void 0);
__decorate([
    property({ type: Number })
], OrMwcInput.prototype, "cols", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "multiple", void 0);
__decorate([
    property({ type: String, attribute: true, reflect: false })
], OrMwcInput.prototype, "pattern", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "placeHolder", void 0);
__decorate([
    property({ type: Array })
], OrMwcInput.prototype, "options", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "autoSelect", void 0);
__decorate([
    property({ type: Object })
], OrMwcInput.prototype, "searchProvider", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "icon", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "iconColor", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "iconOn", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "iconTrailing", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "compact", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "comfortable", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "raised", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "action", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "unElevated", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "outlined", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "rounded", void 0);
__decorate([
    property({ type: Object })
], OrMwcInput.prototype, "format", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "disableSliderNumberInput", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "fullWidth", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "helperText", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "helperPersistent", void 0);
__decorate([
    property({ type: String, attribute: true })
], OrMwcInput.prototype, "validationMessage", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "autoValidate", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "charCounter", void 0);
__decorate([
    property({ type: String })
], OrMwcInput.prototype, "label", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "disabled", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "continuous", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcInput.prototype, "resizeVertical", void 0);
__decorate([
    state()
], OrMwcInput.prototype, "isUiValid", void 0);
__decorate([
    state()
], OrMwcInput.prototype, "searchableValue", void 0);
__decorate([
    state()
], OrMwcInput.prototype, "errorMessage", void 0);
OrMwcInput = __decorate([
    customElement("or-mwc-input")
], OrMwcInput);
export { OrMwcInput };
//# sourceMappingURL=or-mwc-input.js.map