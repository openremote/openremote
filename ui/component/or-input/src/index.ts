import {customElement, html, LitElement, property, query, TemplateResult} from "lit-element";
import {orInputStyle} from "./style";
import {
    AttributeDescriptor,
    AttributeValueDescriptor,
    AttributeValueType,
    MetaItemType,
    ValueType
} from "@openremote/model";
import {AssetModelUtil} from "@openremote/core";
import "@openremote/or-select";
import {OrSelectChangedEvent} from "@openremote/or-select";

// Allows consumers of the or-input component to provide custom input controls; if the value is changed the custom control
// must call the valueChangeNotifier.
class InputProviders {
    public _providers: Array<(assetType: string, attributeName: string, attributeDescriptor: AttributeDescriptor | undefined, value: any | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean) => TemplateResult | undefined> = [];

    public addInputProvider(callback: (assetType: string, attributeName: string, attributeDescriptor: AttributeDescriptor | undefined, value: any | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean) => TemplateResult | undefined) {
        this._providers.push(callback);
    }

    public removeInputProvider(callback: (assetType: string, attributeName: string, attributeDescriptor: AttributeDescriptor | undefined, value: any | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean) => TemplateResult | undefined) {
        const i = this._providers.indexOf(callback);
        if (i <= 0) {
            this._providers.splice(i, 1);
        }
    }
}

const inputProviders = new InputProviders();
export default inputProviders;

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
    CHECKBOX = "checkbox",
    COLOUR = "color",
    DATE = "date",
    DATETIME = "datetime",
    DECIMAL = "decimal",
    EMAIL = "email",
    GEO_JSON_POINT = "geoPos",
    INTEGER = "integer",
    JSON = "json",
    NUMBER = "number",
    PASSWORD = "password",
    RADIO = "radio",
    SWITCH_MOMENTARY = "switchMomentary",
    SWITCH_TOGGLE = "checkbox",
    SELECT = "select",
    SLIDER = "range",
    TELEPHONE = "telephone",
    TEXT = "text",
    TEXTAREA = "textarea",
    TIME = "time",
    URL = "url"
}

@customElement("or-input")
export class OrInput extends LitElement {

    @property({type: String})
    public assetType?: string;

    @property({type: String})
    public attributeName?: string;

    @property({type: Object})
    public value?: any;

    @property({type: String})
    public type?: InputType;

    @property({type: Boolean})
    public allowNull: boolean = true;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public required: boolean = false;

    @property({type: Object})
    public attributeDescriptor?: AttributeDescriptor;

    @property({type: Object})
    public attributeValueDescriptor?: AttributeValueDescriptor;

    @property({type: String})
    public valueType?: ValueType;

    @query("#or-input")
    protected _input!: HTMLInputElement;

    static get styles() {
        return [
            orInputStyle
        ];
    }

    protected render() {

        if (!this.assetType || !this.attributeName) {
            return;
        }

        if (!this.attributeDescriptor) {
            this.attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(this.assetType, this.attributeName);
        }

        for (const inputProvider of inputProviders._providers) {
            const template = inputProvider(this.assetType, this.attributeName, this.attributeDescriptor, this.value, (newValue: any) => this.onValueChange(newValue), this.readonly);
            if (template) {
                return template;
            }
        }

        const valueType = this.getValueType();

        if (!this.type) {
            const valueDescriptor = this.getValueDescriptor();
            if (valueDescriptor && valueDescriptor.name) {
                // @ts-ignore
                this.type = InputType[valueDescriptor.name];

                if (!this.type) {
                    switch (valueDescriptor.name) {
                        case AttributeValueType.BOOLEAN.name:
                            this.type = InputType.CHECKBOX;
                            break;
                        case AttributeValueType.STRING.name:
                            this.type = InputType.TEXT;
                            break;
                        case AttributeValueType.ARRAY.name:
                        case AttributeValueType.OBJECT.name:
                            this.type = InputType.JSON;
                            break;
                    }
                }
            }
        }

        if (this.type) {
            switch (this.type) {
                case InputType.BUTTON:
                    const isExecutable = this.attributeDescriptor && this.attributeDescriptor.metaItemDescriptors && this.attributeDescriptor.metaItemDescriptors.find((mid) => mid.urn === MetaItemType.EXECUTABLE.urn);
                    if (isExecutable) {
                        return html`<span>EXECUTABLE BUTTON NOT IMPLEMENTED</span>`;
                    }
                    break;
                case InputType.SWITCH_MOMENTARY:
                    if (!valueType || valueType === ValueType.BOOLEAN) {
                        return html`<input type="button" @onmousedown="${() => this.onValueChange(true)}" @onmouseup="${() => this.onValueChange(false)}" value="" ?readonly="${this.readonly}" />`;
                    }
                    break;
                case InputType.DECIMAL:
                case InputType.INTEGER:
                case InputType.NUMBER:
                case InputType.SLIDER:
                    if (valueType && valueType !== ValueType.NUMBER) {
                        break;
                    }
                    const step = this.type === InputType.INTEGER ? "1" : undefined;
                    const min = this.getMinValue();
                    const max = this.getMaxValue();

                    return html`<input ?required="${this.required}" type="${this.type}" step="${step}" min="${min}" max="${max}" value="${this.value}" @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement).value)}" />`;
                    break;
                case InputType.DATE:
                case InputType.DATETIME:
                case InputType.TIME:
                    if (valueType && valueType !== ValueType.NUMBER && valueType !== ValueType.STRING) {
                        break;
                    }
                    break;
                case InputType.SWITCH_TOGGLE:
                case InputType.CHECKBOX:
                case InputType.COLOUR:
                case InputType.EMAIL:
                case InputType.PASSWORD:
                case InputType.TELEPHONE:
                case InputType.TEXT:
                case InputType.URL:
                case InputType.SELECT:
                    if (this.type === InputType.TEXT || this.type === InputType.SELECT) {
                        const options = this.getAllowedValues();
                        if (options || this.type === InputType.SELECT) {
                            return html`
                                <or-select ?required="${this.required}" .options="${options}" .value="${this.value}" ?readonly="${this.readonly}" @or-select-changed="${(e: OrSelectChangedEvent) => this.onValueChange(e.detail.value)}"></or-select>
                            `;
                        }
                    }

                    return html`<input ?required="${this.required}" type="${this.type}" value="${this.value}" @change="${(e: Event) => this.onValueChange((e.target as HTMLInputElement).value)}" />`;
                case InputType.GEO_JSON_POINT:
                    break;
            }
        }

        return html`<span>INPUT TYPE NOT IMPLEMENTED</span>`;
    }

    protected onValueChange(newValue: any | undefined) {
        const previousValue = this.value;
        this.value = newValue;
        this.dispatchEvent(new OrInputChangedEvent(this.value, previousValue));
    }

    protected getValueDescriptor() {
        if (this.attributeValueDescriptor) {
            return this.attributeValueDescriptor;
        }

        if (this.attributeDescriptor) {
            return this.attributeDescriptor.valueDescriptor;
        }
    }

    protected getValueType() {
        if (this.valueType) {
            return this.valueType;
        }

        const descriptor = this.getValueDescriptor();
        if (descriptor) {
            return descriptor.valueType;
        }

        if (this.value) {
            if (typeof this.value === "number") {
                return ValueType.NUMBER;
            }
            if (typeof this.value === "string") {
                return ValueType.STRING;
            }
            if (typeof this.value === "boolean") {
                return ValueType.BOOLEAN;
            }
            if (Array.isArray(this.value)) {
                return ValueType.ARRAY;
            }
            return ValueType.OBJECT;
        }
    }

    protected getMinValue() {
        if (!this.attributeDescriptor) {
            return;
        }

        const descriptor = AssetModelUtil.getMetaInitialValueFromMetaDescriptors(MetaItemType.RANGE_MIN, this.attributeDescriptor.metaItemDescriptors);
        if (descriptor) {
            return descriptor.initialValue;
        }
    }

    protected getMaxValue() {
        if (!this.attributeDescriptor) {
            return;
        }

        const descriptor = AssetModelUtil.getMetaInitialValueFromMetaDescriptors(MetaItemType.RANGE_MAX, this.attributeDescriptor.metaItemDescriptors);
        if (descriptor) {
            return descriptor.initialValue;
        }
    }

    protected getAllowedValues() {
        if (!this.attributeDescriptor) {
            return;
        }

        const allowedValues = AssetModelUtil.getMetaInitialValueFromMetaDescriptors(MetaItemType.ALLOWED_VALUES, this.attributeDescriptor.metaItemDescriptors);
        if (allowedValues && Array.isArray(allowedValues) && allowedValues.length > 0) {
            return [...allowedValues];
        }
    }
}
