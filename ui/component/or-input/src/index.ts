import {css, customElement, html, LitElement, property, PropertyValues, TemplateResult, unsafeCSS} from "lit-element";
import {classMap} from "lit-html/directives/class-map";
import {ifDefined} from "lit-html/directives/if-defined";
import {MDCTextField} from "@material/textfield";
import {MDCComponent} from "@material/base";
import {MDCRipple} from "@material/ripple";
import {MDCCheckbox} from "@material/checkbox";
import {MDCSwitch} from "@material/switch";
import {MDCSelect, MDCSelectEvent } from "@material/select";
import {MDCList, MDCListActionEvent} from "@material/list";

import {MDCFormField, MDCFormFieldInput} from "@material/form-field";
import {MDCIconButtonToggle, MDCIconButtonToggleEventDetail} from "@material/icon-button";
import moment from "moment";
import {DefaultColor4, DefaultColor8} from "@openremote/core";
import i18next from "i18next";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const buttonStyle = require("!!raw-loader!@material/button/dist/mdc.button.css");
const buttonFabStyle = require("!!raw-loader!@material/fab/dist/mdc.fab.css");
const iconButtonStyle = require("!!raw-loader!@material/icon-button/dist/mdc.icon-button.css");
const textfieldStyle = require("!!raw-loader!@material/textfield/dist/mdc.textfield.css");
const rippleStyle = require("!!raw-loader!@material/ripple/dist/mdc.ripple.css");
const lineRippleStyle = require("!!raw-loader!@material/line-ripple/dist/mdc.line-ripple.css");
const floatingLabelStyle = require("!!raw-loader!@material/floating-label/dist/mdc.floating-label.css");
const formFieldStyle = require("!!raw-loader!@material/form-field/dist/mdc.form-field.css");
const checkboxStyle = require("!!raw-loader!@material/checkbox/dist/mdc.checkbox.css");
const switchStyle = require("!!raw-loader!@material/switch/dist/mdc.switch.css");
const selectStyle = require("!!raw-loader!@material/select/dist/mdc.select.css");
const listStyle = require("!!raw-loader!@material/list/dist/mdc.list.css");
const menuSurfaceStyle = require("!!raw-loader!@material/menu-surface/dist/mdc.menu-surface.css");
const menuStyle = require("!!raw-loader!@material/menu/dist/mdc.menu.css");

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
    BUTTON_TOGGLE = "button-toggle",
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
    SELECT = "select",
    LIST = "list"
}

// language=CSS
const style = css`
    
    :host {
        display: inline-block;
        --internal-or-input-color: var(--or-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-input-text-color: var(--or-input-text-color, var(--or-app-color1, ${unsafeCSS(DefaultColor8)}));    
        
        --mdc-theme-primary: var(--internal-or-input-color);
        --mdc-theme-on-primary: var(--internal-or-input-text-color);
        --mdc-theme-secondary: var(--internal-or-input-color);
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
    }
    
    #wrapper > label {
        margin-left: 10px;
    }
    
    #component {
        max-width: 100%;
    }

    .mdc-text-field {
        flex: 1 1 0;
    }

    .mdc-list {
        flex: 1 1 0;
        overflow: auto;
    }

    #menu-anchor {
        max-width: 100%;
    }
       
    .or-input--rounded {
        border-radius: 50% !important;
    }

    ::-webkit-clear-button {display: none;}
    ::-webkit-inner-spin-button { display: none; }
    ::-webkit-datetime-edit { padding: 0em;}
    ::-webkit-datetime-edit-text { padding: 0; }


    /* MDC TEXT FIELD AND SELECT DON'T USE THEME VARS */
    .mdc-select #menu-anchor,
    .mdc-text-field {
        background-color: #fbfbfb!important;
    }

    .mdc-text-field--focused:not(.mdc-text-field--disabled) .mdc-floating-label {
        color: var(--mdc-theme-primary);
    }
    .mdc-text-field--focused .mdc-text-field__input:required ~ .mdc-floating-label::after,
    .mdc-text-field--focused .mdc-text-field__input:required ~ .mdc-notched-outline .mdc-floating-label::after {
        color: var(--mdc-theme-primary);
    }
    
    .mdc-select:not(.mdc-select--disabled).mdc-select--focused .mdc-floating-label {
        color: var(--mdc-theme-primary);
    }
    
    .mdc-text-field, .mdc-text-field-helper-line {
        width: inherit;
    }
    
    .mdc-text-field--dense:not(.mdc-text-field--no-label):not(.mdc-text-field--outlined) {
        height: 52px;
    }
    
    .mdc-text-field--dense.mdc-text-field--no-label {
        height: 40px;
    }
    
    .mdc-text-field--dense .mdc-text-field__input {
        white-space: nowrap;
    }
    
    .mdc-select {
        white-space: nowrap;
    }
    
    .mdc-select__dropdown-icon {
        background: none;
    }

    .mdc-select--focused .mdc-select__dropdown-icon {
        --or-icon-fill: var(--internal-or-input-color);
    }
    
    .mdc-icon-button {
        padding: 0;
    }
    
    #field {
        height: 100%;
    }
`;

@customElement("or-input")
export class OrInput extends LitElement {

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
            css`${unsafeCSS(switchStyle)}`,
            css`${unsafeCSS(selectStyle)}`,
            css`${unsafeCSS(listStyle)}`,
            css`${unsafeCSS(menuStyle)}`,
            css`${unsafeCSS(menuSurfaceStyle)}`,
            style
        ];
    }

    @property({type: Boolean})
    public focused?: boolean;

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
    public min?: any;

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

    @property({type: Boolean})
    public autoSelect?: boolean;

    /* STYLING PROPERTIES BELOW */

    @property({type: String})
    public icon?: string;

    @property({type: String})
    public iconOn?: string;

    @property({type: String})
    public iconTrailing?: string;

    @property({type: Boolean})
    public dense: boolean = false;

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

    /* BUTTON STYLES END */

    /* TEXT INPUT STYLES START */

    @property({type: Boolean})
    public fullWidth: boolean = false;

    @property({type: String})
    public helperText?: string;

    @property({type: Boolean})
    public helperPersistent: boolean = false;

    @property({type: String})
    public validationMessage = "";

    @property({type: Boolean})
    public charCounter: boolean = false;

    @property({type: String})
    public label?: string;

    /* TEXT INPUT STYLES END */

    protected _mdcComponent?: MDCComponent<any>;
    protected _mdcField?: MDCComponent<any>;
    protected _selectedIndex = -1;

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

            const showLabel = !this.fullWidth && this.label;
            const outlined = !this.fullWidth && this.outlined;
            const hasHelper = !!this.helperText;
            const showValidationMessage = this.validationMessage;
            const hasValue = this.value || this.value === false;
            const labelTemplate = showLabel ? html`<label class="mdc-floating-label ${hasValue ? "mdc-floating-label--float-above" : ""}" for="elem">${this.label}</label>` : ``;

            switch (this.type) {
                case InputType.RADIO:
                    return html`<span>RADIO</span>`;
                case InputType.SWITCH:
                    return html`
                        <span id="wrapper">
                            <div id="component" class="mdc-switch ${this.disabled || this.readonly ? "mdc-switch--disabled" : ""} ${this.value ? "mdc-switch--checked" : ""}">
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
                            <label for="elem">${this.label}</label>
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
                        const helperClassesList = {
                            "mdc-select-helper-text--persistent": this.helperPersistent,
                            "mdc-select-helper-text--validation-msg": showValidationMessage,
                        };
    
                        let optsList: [string, string][] | undefined;
                        if (this.options && this.options.length > 0) {
                            if (Array.isArray(this.options[0])) {
                                optsList = this.options as [string, string][];
                            } else {
                                optsList = (this.options as string[]).map((option) => [option, option]);
                            }
                        }
    
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
                        "mdc-select--disabled": this.disabled || this.readonly,
                        "mdc-select--required": this.required,
                        "mdc-select--dense": false, // this.dense,
                        "mdc-select--no-label": !this.label,
                        "mdc-select--with-leading-icon": !!this.icon
                    };
                    const helperClasses = {
                        "mdc-select-helper-text--persistent": this.helperPersistent,
                        "mdc-select-helper-text--validation-msg": showValidationMessage,
                    };

                    let opts: [string, string][] | undefined;
                    if (this.options && this.options.length > 0) {
                        if (Array.isArray(this.options[0])) {
                            opts = this.options as [string, string][];
                        } else {
                            opts = (this.options as string[]).map((option) => [option, option]);
                        }
                    }
                    const value = opts && opts.find(([optValue, optDisplay], index) => this.value === optValue);
                    const valueLabel = value ? value[1] : this.value;
                    this._selectedIndex = -1;

                    return html`
                        <div id="component" 
                            class="mdc-select ${classMap(classes)}" 
                            @MDCSelect:change="${(e: MDCSelectEvent) => this.onValueChange(undefined, e.detail.index === -1 ? undefined : Array.isArray(this.options![e.detail.index]) ? this.options![e.detail.index][0] : this.options![e.detail.index])}">
                                <div id="menu-anchor" class="mdc-select__anchor select-class">
                                    <or-icon class="mdc-select__dropdown-icon" icon="menu-down"></or-icon>
                                    <input id="elem" readonly class="mdc-select__selected-text" role="button" value="${i18next.t(valueLabel)}" aria-haspopup="listbox" aria-controls="component-helper-text" aria-describedby="component-helper-text" aria-labelledby="component-label component"/>
                                    ${outlined ? this.renderOutlined(labelTemplate) : labelTemplate}
                                    ${!outlined ? html`<div class="mdc-line-ripple"></div>` : ``}
                                </div>   
                                 
                                <div class="mdc-select__menu mdc-menu mdc-menu-surface select-class" role="listbox">
                                    <ul class="mdc-list">
                                        ${opts ? opts.map(([optValue, optDisplay], index) => {
                                            if (this.value === optValue) {
                                                this._selectedIndex = index;
                                            }
                                            return html`<li class="mdc-list-item ${this._selectedIndex === index ? "mdc-menu-item--selected" : ""}" role="option" aria-checked="${this._selectedIndex === index}"  data-value="${optValue}">
                                                                <span><or-translate value="${optDisplay}"></or-translate></span>
                                                         </li>`;
                                        }) : ``}
                                    </ul>
                                </div>                                
                                    
                                ${hasHelper ? html`
                                    <p id="component-helper-text" class="mdc-select-helper-text ${classMap(helperClasses)}" aria-hidden="true">
                                        ${showValidationMessage ? this.validationMessage : this.helperText}
                                    </p>` : ``}
                        </div>
                    `;
                case InputType.BUTTON_TOGGLE:
                    return html`
                        <button id="component" class="mdc-icon-button ${this.value ? "mdc-icon-button--on" : ""}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @MDCIconButtonToggle:change="${(evt: MDCIconButtonToggleEventDetail) => this.onValueChange(undefined, evt.isOn)}">
                            ${this.icon ? html`<or-icon class="mdc-icon-button__icon" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.iconOn ? html`<or-icon class="mdc-icon-button__icon mdc-icon-button__icon--on" aria-hidden="true" icon="${this.iconOn}"></or-icon>` : ``}
                        </button>
                    `;
                case InputType.BUTTON:
                case InputType.BUTTON_MOMENTARY: {
                    const isMomentary = this.type === InputType.BUTTON_MOMENTARY;
                    const isIconButton = !this.action && !this.label;
                    const classes = {
                        "mdc-icon-button": isIconButton,
                        "mdc-fab": !isIconButton && this.action,
                        "mdc-fab--extended": !isIconButton && this.action && !!this.label,
                        "mdc-fab--mini": !isIconButton && this.action, // && this.dense,
                        "mdc-button": !isIconButton && !this.action,
                        "mdc-button--raised": !isIconButton && !this.action && this.raised,
                        "mdc-button--unelevated": !isIconButton && !this.action && this.unElevated,
                        "mdc-button--outlined": !isIconButton && !this.action && this.outlined,
                        "mdc-button--dense": false, // !isIconButton && !this.action && this.dense,
                        "or-input--rounded": !isIconButton && !this.action && this.rounded
                    };
                    return html`
                        <button id="component" class="${classMap(classes)}"
                            ?readonly="${this.readonly}"
                            ?disabled="${this.disabled}"
                            @onmousedown="${() => {if (isMomentary) this.onValueChange(undefined, true)}}" @onmouseup="${() => isMomentary ? this.onValueChange(undefined, false) : this.onValueChange(undefined, true)}">
                            ${!isIconButton ? html`<div class="mdc-button__ripple"></div>` : ``}
                            ${this.icon ? html`<or-icon class="${isIconButton ? "" : this.action ? "mdc-fab__icon" : "mdc-button__icon"}" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.label ? html`<span class="${this.action ? "mdc-fab__label" : "mdc-button__label"}">${this.label}</span>` : ``}
                            ${!isIconButton && this.iconTrailing ? html`<or-icon class="${this.action ? "mdc-fab__icon" : "mdc-button__icon"}" aria-hidden="true" icon="${this.iconTrailing}"></or-icon>` : ``}
                        </button>
                    `;
                }
                case InputType.CHECKBOX:
                    return html`
                        <div id="field" class="mdc-form-field">
                            <div id="component" class="mdc-checkbox">
                                <input type="checkbox" 
                                    ?checked="${this.value}"
                                    ?required="${this.required}"
                                    ?disabled="${this.disabled || this.readonly}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).checked)}"
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
                    const valMinMax: [any, any, any] = [this.value, this.min, this.max];

                    if (valMinMax.find((v) => v !== undefined && v !== null && typeof(v) !== "string") !== undefined) {

                        let format: string | undefined;

                        switch (this.type) {
                            case InputType.TIME:
                                format = "HH:mm";
                               break;
                            case InputType.DATE:
                                format = "YYYY-MM-DD";
                                break;
                            case InputType.WEEK:
                                format = "YYYY-Www";
                                break;
                            case InputType.MONTH:
                                format = "YYYY-MM";
                                break;
                            case InputType.DATETIME:
                                format = "YYYY-MM-DDTHH:mm";
                                break;
                            case InputType.JSON:
                                valMinMax[0] = valMinMax[0] !== undefined && valMinMax[0] !== null ? (typeof valMinMax[0] === "string" ? valMinMax[0] : JSON.stringify(valMinMax[0], null, 2)) : "";
                                break;
                            default:
                                valMinMax[0] = valMinMax[0] !== undefined && valMinMax[0] !== null ? valMinMax[0] : "";
                                break;
                        }

                        if (format) {
                            valMinMax.forEach((val, i) => {
                                if (typeof(val) === "number") {
                                    const offset = (new Date()).getTimezoneOffset() * 60000;
                                    val = new Date(val - offset);
                                }
                                if (val instanceof Date) {
                                    val = moment(val).format(format);
                                }
                                valMinMax[i] = val;
                            });
                        }
                    }

                    const classes = {
                        "mdc-text-field--outlined": outlined,
                        "mdc-text-field--textarea": this.type === InputType.TEXTAREA || this.type === InputType.JSON,
                        "mdc-text-field--disabled": this.disabled,
                        "mdc-text-field--fullwidth": this.fullWidth,
                        "mdc-text-field--dense": false, // this.type !== InputType.TEXTAREA && this.type !== InputType.JSON && this.dense,
                        "mdc-text-field--no-label": !this.label,
                        "mdc-text-field--with-leading-icon": !!this.icon,
                        "mdc-text-field--with-trailing-icon": !!this.iconTrailing
                    };
                    const helperClasses = {
                        "mdc-text-field-helper-text--persistent": this.helperPersistent,
                        "mdc-text-field-helper-text--validation-msg": showValidationMessage,
                    };

                    return html`
                            <div id="component" class="mdc-text-field ${classMap(classes)}">
                            ${this.icon ? html`<or-icon class="mdc-text-field__icon" aria-hidden="true" icon="${this.icon}"></or-icon>` : ``}
                            ${this.type === InputType.TEXTAREA  || this.type === InputType.JSON ? html`
                                <textarea id="elem" class="mdc-text-field__input"
                                    ?required="${this.required}"
                                    ?readonly="${this.readonly}"
                                    ?disabled="${this.disabled}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLTextAreaElement), (e.target as HTMLTextAreaElement).value)}"
                                    minlength="${ifDefined(this.minLength)}"
                                    maxlength="${ifDefined(this.maxLength)}"
                                    rows="${this.rows ? this.rows : 5}" 
                                    cols="${ifDefined(this.cols)}"
                                    aria-label="${ifDefined(this.label)}">${valMinMax[0] ? valMinMax[0] : ""}</textarea>
                                ${this.renderOutlined(labelTemplate)}
                                ` :
                                html`<input type="${this.type}" id="elem" class="mdc-text-field__input"
                                    ?required="${this.required}"
                                    ?readonly="${this.readonly}"
                                    ?disabled="${this.disabled}"
                                    @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement), (e.target as HTMLInputElement).value)}"
                                    .value="${valMinMax[0] !== null && valMinMax[0] !== undefined ? valMinMax[0] : ""}"
                                    min="${ifDefined(valMinMax[1])}"
                                    max="${ifDefined(valMinMax[2])}"
                                    step="${this.step ? this.step : "any"}"
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
                switch (this.type) {
                    case InputType.LIST:
                        const mdcList = new MDCList(component);
                        this._mdcComponent = mdcList;
                        mdcList.selectedIndex = this._selectedIndex;
                        break;
                    case InputType.SELECT:
                        const mdcSelect = new MDCSelect(component);
                        this._mdcComponent = mdcSelect;
                        mdcSelect.selectedIndex = this._selectedIndex;
                        break;
                    case InputType.RADIO:
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
                            this._mdcField = mdcField;
                        }
                        break;
                    case InputType.SWITCH:
                        this._mdcComponent = new MDCSwitch(component);
                        break;
                    default:
                        const textField = new MDCTextField(component);
                        textField.useNativeValidation = false;
                        this._mdcComponent = textField;
                        break;
                }
            }
        } else if (this.type === InputType.SELECT && this._mdcComponent) {
            (this._mdcComponent as MDCSelect).selectedIndex = this._selectedIndex;
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

        if (this.focused && this._mdcComponent && typeof((this._mdcComponent as any).focus) === "function") {
            (this._mdcComponent as any).focus();
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

    protected onValueChange(elem: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement | undefined, newValue: any | undefined) {
        let valid = true;

        if (elem && this._mdcComponent) {

            // trigger validation
            valid = elem.checkValidity();
            if (this._mdcComponent instanceof MDCTextField) {
                this._mdcComponent.valid = valid;
            }
        }

        const previousValue = this.value;

        if (newValue === "null") {
            newValue = null;
        }

        if (newValue === "undefined") {
            newValue = undefined;
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
                    newValue = newValue !== "" ? JSON.parse(newValue) : null;
                    break;
                case InputType.DATETIME:
                    if (newValue === "") {
                        newValue = null;
                    } else {
                        const date = Date.parse(newValue);
                        const offset = (new Date()).getTimezoneOffset() * 60000;
                        newValue = date + offset;
                    }
                    break;
            }
        }

        this.value = newValue;

        if (newValue !== previousValue) {
            this.dispatchEvent(new OrInputChangedEvent(this.value, previousValue));
        }
    }

    public get valid(): boolean {
        const elem = this.shadowRoot!.getElementById("elem") as any;
        if (elem && elem.checkValidity) {
            return elem.checkValidity();
        }
        return true;
    }

}
