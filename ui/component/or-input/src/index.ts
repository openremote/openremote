import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult, unsafeCSS} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import {ifDefined} from "lit-html/directives/if-defined";
import {orInputStyle} from "./style";
import "@openremote/or-select";
import {MDCTextField} from "@material/textfield";
import {MDCComponent} from "@material/base";
import {MDCRipple} from "@material/ripple";
import {MDCCheckbox} from "@material/checkbox";
import {MDCSwitch} from "@material/switch";
import {MDCFormField, MDCFormFieldInput} from "@material/form-field";
import {OrSelectChangedEvent} from "@openremote/or-select";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const buttonStyle = require("!!raw-loader!@material/button/dist/mdc.button.css");
const iconButtonStyle = require("!!raw-loader!@material/icon-button/dist/mdc.icon-button.css");
const textfieldStyle = require("!!raw-loader!@material/textfield/dist/mdc.textfield.css");
const rippleStyle = require("!!raw-loader!@material/ripple/dist/mdc.ripple.css");
const lineRippleStyle = require("!!raw-loader!@material/line-ripple/dist/mdc.line-ripple.css");
const floatingLabelStyle = require("!!raw-loader!@material/floating-label/dist/mdc.floating-label.css");
const formFieldStyle = require("!!raw-loader!@material/form-field/dist/mdc.form-field.css");
const checkboxStyle = require("!!raw-loader!@material/checkbox/dist/mdc.checkbox.css");
const switchStyle = require("!!raw-loader!@material/switch/dist/mdc.switch.css");

export class OrInputChangedEvent extends CustomEvent<OrInputChangedEventDetail> {

    public static readonly NAME = "or-input-changed";

    constructor(value?: any, previousValue?: any) {
        super(OrInputChangedEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
}

export interface OrInputChangedEventDetail {
    value?: any;
    previousValue?: any;
}

declare global {
    export interface HTMLElementEventMap {
        [OrInputChangedEvent.NAME]: OrInputChangedEvent;
    }
}

export enum InputType {
    BUTTON = "button",
    BUTTON_MOMENTARY = "button-momentary",
    CHECKBOX = "checkbox",
    COLOUR = "color",
    DATE = "date",
    DATETIME = "datetime-local",
    EMAIL = "email",
    JSON = "json",
    MONTH = "month",
    NUMBER = "number",
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
    SELECT = "select"
}

@customElement("or-input")
export class OrInput extends LitElement {

    static get styles() {
        return [
            css`${unsafeCSS(iconButtonStyle)}`,
            css`${unsafeCSS(buttonStyle)}`,
            css`${unsafeCSS(textfieldStyle)}`,
            css`${unsafeCSS(rippleStyle)}`,
            css`${unsafeCSS(lineRippleStyle)}`,
            css`${unsafeCSS(floatingLabelStyle)}`,
            css`${unsafeCSS(formFieldStyle)}`,
            css`${unsafeCSS(checkboxStyle)}`,
            css`${unsafeCSS(switchStyle)}`,
            orInputStyle
        ];
    }

    @property()
    public value?: any;

    @property({type: String})
    public type?: InputType;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public required: boolean = false;

    @property()
    public max?: any;

    @property()
    public min?: HTMLInputElement;

    @property({type: Number})
    public step?: number;

    @property({type: Boolean})
    public checked: boolean = false;

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

    @property({type: String})
    public pattern?: string;

    @property({type: String})
    public placeHolder?: string;

    @property({type: Array})
    public options?: string[] | [string, string][];

    /* STYLING PROPERTIES BELOW */

    @property({type: String})
    public icon?: string;

    @property({type: Boolean})
    public dense: boolean = false;

    /* BUTTON STYLES START */

    @property({type: Boolean})
    public iconTrailing: boolean = false;

    @property({type: Boolean})
    public raised: boolean = false;

    @property({type: Boolean})
    public unElevated: boolean = false;

    @property({type: Boolean})
    public outlined: boolean = false;

    @property({type: Boolean})
    public rounded: boolean = false;

    /* BUTTON STYLES END */

    /* TEXT INPUT STYLES START */

    @property({type: Boolean})
    public fullWidth: boolean = false;

    @property({type: String})
    public helperText?: string;

    @property({type: Boolean})
    public helperPersistent: boolean = false;

    @property({type: String})
    public validationMessage = '';

    @property({type: Boolean})
    public charCounter: boolean = false;

    @property({type: String})
    public label?: string;

    /* TEXT INPUT STYLES END */

    protected _mdcComponent?: MDCComponent<any>;
    protected _mdcField?: MDCComponent<any>;
    protected _processedValue?: any;

    disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
        if (this._mdcField) {
            this._mdcField.destroy();
            this._mdcField = undefined;
        }
    }

    protected render() {

        if (this.type) {
            switch (this.type) {
                case InputType.RADIO:
                    return html`<span>RADIO</span>`;
                case InputType.SWITCH:
                    return html`
                        <div id="field" class="mdc-form-field ${this.dense ? "mdc-form-field--dense" : ""}">
                            <div id="component" class="mdc-switch ${this.disabled || this.readonly ? "mdc-switch--disabled" : ""} ${this.value ? "mdc-switch--checked" : ""}">
                                <div class="mdc-switch__track"></div>
                                <div class="mdc-switch__thumb-underlay">
                                    <div class="mdc-switch__thumb">
                                        <input type="checkbox" id="elem" class="mdc-switch__native-control" 
                                        ?checked="${this._processedValue}"
                                        ?required="${this.required}"
                                        ?disabled="${this.disabled || this.readonly}"
                                        @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement).checked)}"
                                        role="switch">
                                    </div>
                                </div>
                            </div>
                            <label for="elem">${this.label}</label>
                        </div>
                    `;
                case InputType.SELECT:
                    return html`
                        <or-select .required="${this.required}" .options="${this.options}" .value="${this._processedValue}" .readonly="${this.readonly}" @or-select-changed="${(e: OrSelectChangedEvent) => this.onValueChange(e.detail.value)}"></or-select>
                    `;
                case InputType.BUTTON:
                case InputType.BUTTON_MOMENTARY: {
                    const rounded = !!(!this._processedValue && this.rounded && this.icon);
                    const classes = {
                        "mdc-button--raised": this.raised,
                        "mdc-button--unelevated": this.unElevated,
                        "mdc-button--outlined": !rounded && this.outlined,
                        "mdc-button--dense": !rounded && this.dense,
                        "or-input--rounded": this.rounded,
                        "mdc-button": !rounded,
                        "mdc-icon-button": rounded
                    };
                    const iconClasses = {
                        "mdc-icon-button__icon": rounded,
                        "mdc-button__icon": !rounded,
                    };
                    if (this.type === InputType.BUTTON_MOMENTARY) {
                        return html`
                            <button id="component" class="${classMap(classes)}"
                                ?required="${this.required}"
                                ?readonly="${this.readonly}"
                                ?disabled="${this.disabled}"
                                @onmousedown="${() => this.onValueChange(true)}" @onmouseup="${() => this.onValueChange(false)}">
                                ${this.icon ? html`<or-icon class="${classMap(iconClasses)}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                                ${this.label ? html`<span class="mdc-button__label">${this.label}</span>` : ``}
                                ${this.iconTrailing ? html`<or-icon class="${classMap(iconClasses)}" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                            </button>
                        `;
                    }
                    return html`
                        <button id="component" class="${classMap(classes)}"
                            ?required="${this.required}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @onmouseup="${() => this.onValueChange(true)}">
                            ${this.icon ? html`<or-icon class="${classMap(iconClasses)}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.label ? html`<span class="mdc-button__label">${this.label}</span>` : ``}
                            ${this.iconTrailing ? html`<or-icon class="${classMap(iconClasses)}" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                        </button>
                    `;
                }
                case InputType.CHECKBOX:
                    return html`
                        <div id="field" class="mdc-form-field ${this.dense ? "mdc-form-field--dense" : ""}">
                            <div id="component" class="mdc-checkbox ${this.dense ? "mdc-checkbox--dense" : ""}">
                                <input type="checkbox" 
                                    ?checked="${this._processedValue}"
                                    ?required="${this.required}"
                                    ?disabled="${this.disabled || this.readonly}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement).checked)}"
                                    class="mdc-checkbox__native-control" id="elem"/>
                                <div class="mdc-checkbox__background">
                                    <svg class="mdc-checkbox__checkmark" viewBox="0 0 24 24">
                                        <path class="mdc-checkbox__checkmark-path" fill="none" d="M1.73,12.91 8.1,19.28 22.79,4.59"></path>
                                    </svg>
                                    <div class="mdc-checkbox__mixedmark"></div>
                                </div>
                            </div>
                            <label for="elem">${this.label}</label>
                        </div>
                    `;
                case InputType.NUMBER:
                case InputType.RANGE:
                case InputType.DATE:
                case InputType.DATETIME:
                case InputType.TIME:
                case InputType.MONTH:
                case InputType.WEEK:
                case InputType.COLOUR:
                case InputType.EMAIL:
                case InputType.PASSWORD:
                case InputType.TELEPHONE:
                case InputType.URL:
                case InputType.TEXT:
                case InputType.TEXTAREA:
                case InputType.JSON: {
                    const showLabel = !this.fullWidth && this.label;
                    const outlined = !this.fullWidth && this.outlined;
                    const hasHelper = !!this.helperText;
                    const showValidationMessage = this.validationMessage;
                    const classes = {
                        "mdc-text-field--outlined": outlined,
                        "mdc-text-field--textarea": this.type === InputType.TEXTAREA || this.type === InputType.JSON,
                        "mdc-text-field--disabled": this.disabled,
                        "mdc-text-field--fullwidth": this.fullWidth,
                        "mdc-text-field--dense": this.type !== InputType.TEXTAREA && this.type !== InputType.JSON && this.dense,
                        "mdc-text-field--no-label": !this.label,
                        "mdc-text-field--with-leading-icon": !!this.icon,
                        "mdc-text-field--with-trailing-icon": this.iconTrailing
                    };
                    const helperClasses = {
                        "mdc-text-field-helper-text--persistent": this.helperPersistent,
                        "mdc-text-field-helper-text--validation-msg": showValidationMessage,
                    };

                    const labelTemplate = showLabel ? html`<label class="mdc-floating-label ${this._processedValue || this._processedValue === false ? "mdc-floating-label--float-above" : ""}" for="elem">${this.label}</label>` : ``;

                    return html`
                            <div id="component" class="mdc-text-field ${classMap(classes)}">
                            ${this.icon ? html`<or-icon class="mdc-text-field__icon" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.type === InputType.TEXTAREA  || this.type === InputType.JSON ? html`
                                <textarea id="elem" class="mdc-text-field__input"
                                    ?required="${this.required}"
                                    ?readonly="${this.readonly}"
                                    ?disabled="${this.disabled}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLTextAreaElement).value)}"
                                    minlength="${ifDefined(this.minLength)}"
                                    maxlength="${ifDefined(this.maxLength)}"
                                    rows="${this.rows ? this.rows : 5}" 
                                    cols="${ifDefined(this.cols)}"
                                    aria-label="${ifDefined(this.label)}">${this._processedValue}</textarea>
                                ${this.renderOutlined(labelTemplate)}
                                ` :
                                html`<input type="${this.type}" id="elem" class="mdc-text-field__input"
                                    ?required="${this.required}"
                                    ?readonly="${this.readonly}"
                                    ?disabled="${this.disabled}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement).value)}"
                                    value="${this._processedValue}"
                                    min="${ifDefined(this.min)}"
                                    max="${ifDefined(this.max)}"
                                    step="${ifDefined(this.step)}"
                                    aria-label="${ifDefined(this.label)}"
                                    minlength="${ifDefined(this.minLength)}"
                                    maxlength="${ifDefined(this.maxLength)}"
                                    placeholder="${ifDefined(this.placeHolder)}" />
                                    ${outlined ? this.renderOutlined(labelTemplate) : labelTemplate}
                                    ${!outlined ? html`<div class="mdc-line-ripple"></div>` : ``}
                                    ${this.iconTrailing ? html`<or-icon class="mdc-text-field__icon" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                                `}
                            </div>
                            ${hasHelper ? html`
                                <div class="mdc-text-field-helper-line">
                                    <div class="mdc-text-field-helper-text ${classMap(helperClasses)}">${showValidationMessage ? this.validationMessage : this.helperText}</div>
                                    ${this.charCounter && !this.readonly ? html`<div class="mdc-text-field-character-counter"></div>` : ``}
                                </div>
                        ` : ``}
                    `;
                }
            }
        }

        return html`<span>INPUT TYPE NOT IMPLEMENTED</span>`;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("value") || _changedProperties.has("type")) {
            let val = this.value;

            if (typeof(val) !== "string") {
                switch (this.type) {
                    case InputType.DATETIME:
                        // Date time conversion for UNIX timestamps in millis
                        if (typeof(val) === "number") {
                            const offset = (new Date()).getTimezoneOffset() * 60000;
                            const str = (new Date(val - offset)).toISOString();
                            val = str.slice(0, str.lastIndexOf(":"));
                        }
                        break;
                    case InputType.JSON:
                        val = val !== undefined && val !== null ? JSON.stringify(val, null, 2) : "";
                        break;
                }
            }
            this._processedValue = val;
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (_changedProperties.has("type")) {
            const component = this.shadowRoot!.getElementById("component");
            if (this._mdcComponent) {
                this._mdcComponent.destroy();
                this._mdcComponent = undefined;
            }
            if (this._mdcField) {
                this._mdcField.destroy();
                this._mdcField = undefined;
            }

            if (component && this.type) {
                switch(this.type) {
                    case InputType.SELECT:
                        break;
                    case InputType.RADIO:
                        break;
                    case InputType.BUTTON:
                        this._mdcComponent = new MDCRipple(component);
                        break;
                    case InputType.CHECKBOX:
                        this._mdcComponent = new MDCCheckbox(component);
                        const field = this.shadowRoot!.getElementById("field");
                        if (field) {
                            const mdcField = new MDCFormField(field);
                            mdcField.input = this._mdcComponent as unknown as MDCFormFieldInput;
                            this._mdcField = mdcField;
                        }
                        break;
                    case InputType.SWITCH:
                        this._mdcComponent = new MDCSwitch(component);
                        break;
                    default:
                        this._mdcComponent = new MDCTextField(component);
                        break;
                }
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
    }

    protected renderOutlined(labelTemplate: TemplateResult | "") {
        return html`
            <div class="mdc-notched-outline">
                <div class="mdc-notched-outline__leading"></div>
                <div class="mdc-notched-outline__notch">
                    ${labelTemplate}
                </div>
                <div class="mdc-notched-outline__trailing"></div>
            </div>
        `;
    }

    protected onValueChange(newValue: any | undefined) {
        const previousValue = this.value;

        if (newValue === "null") {
            newValue = null;
        }

        if (typeof(newValue) === "string" && typeof(previousValue) !== "string") {
            switch (this.type) {
                case InputType.CHECKBOX:
                case InputType.SWITCH:
                    newValue = newValue === "on";
                    break;
                case InputType.JSON:
                case InputType.NUMBER:
                case InputType.RANGE:
                    newValue = JSON.parse(newValue);
                    break;
                case InputType.DATETIME:
                    const date = Date.parse(newValue);
                    const offset = (new Date()).getTimezoneOffset() * 60000;
                    newValue = date + offset;
                    break;
            }
        }

        this.value = newValue;
        this.dispatchEvent(new OrInputChangedEvent(this.value, previousValue));
    }
}
