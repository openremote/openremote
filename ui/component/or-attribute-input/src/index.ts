import {css, customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";
import {
    AssetAttribute,
    Attribute,
    AttributeDescriptor,
    AttributeEvent,
    AttributeRef,
    AttributeValueDescriptor,
    AttributeValueType,
    MetaItemType,
    ValueType
} from "@openremote/model";
import manager, {AssetModelUtil} from "@openremote/core";
import {subscribe} from "@openremote/core";
import "@openremote/or-input";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";

export function getAttributeLabel(attribute: Attribute | undefined, descriptor: AttributeDescriptor | undefined, fallback?: string): string {
    if (!attribute && !descriptor) {
        return fallback || "";
    }

    const labelMetaValue = AssetModelUtil.getMetaValue(MetaItemType.LABEL, attribute, descriptor);
    const name = attribute ? attribute.name : descriptor!.attributeName;
    return i18next.t([name, fallback || labelMetaValue || name]);
}

// Allows consuming applications to provide custom UI for certain attributes; if the value is changed the custom control
// must call the valueChangeNotifier.
class AttributeInputProviders {
    public _providers: Array<(assetType: string | undefined, attribute: AssetAttribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean, disabled: boolean) => TemplateResult | undefined> = [];

    public addInputProvider(callback: (assetType: string | undefined, attribute: AssetAttribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean, disabled: boolean) => TemplateResult | undefined) {
        this._providers.push(callback);
    }

    public removeInputProvider(callback: (assetType: string | undefined, attribute: AssetAttribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean, disabled: boolean) => TemplateResult | undefined) {
        const i = this._providers.indexOf(callback);
        if (i <= 0) {
            this._providers.splice(i, 1);
        }
    }
}

const inputProviders = new AttributeInputProviders();
export default inputProviders;

// TODO: Add support for attribute not found and attribute deletion/addition
@customElement("or-attribute-input")
export class OrAttributeInput extends subscribe(manager)(translate(i18next)(LitElement)) {

    static get styles() {
        return css`
            :host {
                display: inline-block;
            }
            
            or-input {
                width: 100%;
            }
        `;
    }

    @property({type: Object, reflect: false})
    public attribute?: AssetAttribute;

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: String})
    public assetType?: string;

    @property({type: String})
    public label?: string;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public preventWrite: boolean = false;

    @property({type: Boolean})
    public dense: boolean = false;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property()
    protected _loading: boolean = false;

    protected _inputType?: InputType;
    protected  _valueOutboundConverter?: (value: any) => any;
    protected _valueInboundConverter?: (value: any) => any;
    protected _min?: number;
    protected _max?: number;
    protected _step?: number;
    protected _label?: string;
    protected _readonly?: boolean;
    protected _options?: string[] | [string, string][];
    protected _template?: TemplateResult;

    @query("#or-input")
    protected _input!: OrInput;

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        const shouldUpdate = super.shouldUpdate(_changedProperties);

        if (_changedProperties.has("attribute")
            || _changedProperties.has("attributeRef")
            || _changedProperties.has("assetType")) {
            this._inputType = undefined;
            this._valueInboundConverter = undefined;
            this._valueOutboundConverter = undefined;
            this._min = undefined;
            this._max = undefined;
            this._step = undefined;
            this._label = undefined;
            this._readonly = undefined;
            this._options = undefined;

            if (!this.attribute && !this.attributeRef) {
                return shouldUpdate;
            }

            let attributeDescriptor: AttributeDescriptor | undefined;
            let valueDescriptor: AttributeValueDescriptor | undefined;
            let attributeName: string;
            let valueType: string | undefined;

            if (this.attribute) {
                attributeName = this.attribute.name!;
                // Type is actually the name of the type descriptor not the type descriptor itself
                valueType = this.attribute.type as string;
            } else {
                attributeName = this.attributeRef!.attributeName!;
            }

            attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(attributeName, this.assetType);
            valueDescriptor = AssetModelUtil.getAttributeValueDescriptorFromAsset(valueType, this.assetType, attributeName);

            for (const inputProvider of inputProviders._providers) {
                this._template = inputProvider(this.assetType, this.attribute, attributeDescriptor, valueDescriptor, (newValue: any) => this.onValueChange(newValue), this.readonly, this.disabled);
                if (this._template) {
                    break;
                }
            }

            if (!this._template) {
                if (valueDescriptor) {

                    switch (valueDescriptor.name) {
                        case AttributeValueType.GEO_JSON_POINT.name:
                            break;
                        case AttributeValueType.BOOLEAN.name:
                        case AttributeValueType.SWITCH_TOGGLE.name:
                            this._inputType = InputType.SWITCH;
                            break;
                        case AttributeValueType.NUMBER.name:
                            this._inputType = InputType.NUMBER;
                            break;
                        case AttributeValueType.STRING.name:
                            this._inputType = InputType.TEXT;
                            break;
                        case AttributeValueType.OBJECT.name:
                        case AttributeValueType.ARRAY.name:
                            this._inputType = InputType.JSON;
                            break;
                        case AttributeValueType.BRIGHTNESS_LUX.name:
                            this._inputType = InputType.NUMBER;
                            this._step = 1;
                            break;
                        default:
                            // Use value type
                            switch (valueDescriptor.valueType) {
                                case ValueType.STRING:
                                    this._inputType = InputType.TEXT;
                                    break;
                                case ValueType.NUMBER:
                                    this._inputType = InputType.NUMBER;
                                    break;
                                case ValueType.BOOLEAN:
                                    this._inputType = InputType.SWITCH;
                                    break;
                                default:
                                    this._inputType = InputType.JSON;
                                    break;
                            }
                            break;
                    }
                }

                if (!this._inputType) {
                    const value = this.attribute ? this.attribute.value : undefined;

                    if (value) {
                        if (typeof value === "number") {
                            this._inputType = InputType.NUMBER;
                        } else if (typeof value === "string") {
                            this._inputType = InputType.TEXT;
                        } else if (typeof value === "boolean") {
                            this._inputType = InputType.SWITCH;
                        } else {
                            this._inputType = InputType.JSON;
                        }
                    }
                }

                if (this._inputType) {
                    // TODO: Update to use 'effective' meta so not dependent on meta items being defined on the asset itself
                    this._min = AssetModelUtil.getMetaValue(MetaItemType.RANGE_MIN, this.attribute, attributeDescriptor);
                    this._max = AssetModelUtil.getMetaValue(MetaItemType.RANGE_MAX, this.attribute, attributeDescriptor);
                    this._label = getAttributeLabel(this.attribute, attributeDescriptor);
                    this._readonly = AssetModelUtil.getMetaValue(MetaItemType.READ_ONLY, this.attribute, attributeDescriptor);
                    this._options = AssetModelUtil.getMetaValue(MetaItemType.ALLOWED_VALUES, this.attribute, attributeDescriptor);

                    if (this._inputType === InputType.JSON) {
                        const outConverter = this._valueOutboundConverter;
                        const inConverter = this._valueInboundConverter;

                        this._valueOutboundConverter = (value) => {
                            if (outConverter) {
                                // @ts-ignore
                                value = outConverter(value);
                            }
                            if (value !== null && typeof(value) !== "string") {
                                value = JSON.stringify(value, null, 2);
                                this._valueInboundConverter = (v) => {
                                    if (inConverter) {
                                        // @ts-ignore
                                        v = inConverter(v);
                                    }
                                    if (typeof(v) === "string") {
                                        v = JSON.parse(v);
                                    }
                                    return v;
                                };
                            }

                            return value;
                        };
                    }
                }
            }
        }

        return shouldUpdate;
    }

    public updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (_changedProperties.has("attribute") || _changedProperties.has("attributeRef")) {
            if (this.attribute || this.attributeRef) {
                this._loading = true;
                super.attributeRefs = this.attribute ? [{entityId: this.attribute!.assetId!, attributeName: this.attribute!.name!}] : [this.attributeRef!];
            } else {
                super.attributeRefs = undefined;
            }
        }
    }

    public render() {

        if (this._loading) {
            return html`
                <div>LOADING</div>
            `;
        }

        if (this._template) {
            return html`${this._template}`;
        }

        if (this._inputType) {
            let value: any = this.attribute ? this.attribute.value : undefined;

            if (this._valueOutboundConverter) {
                // Convert the value before setting it on the input
                value = this._valueOutboundConverter(value);
            }

            return html`<or-input .type="${this._inputType}" .label="${this._label}" .value="${value}" .allowedValues="${this._options}" .min="${this._min}" .max="${this._max}" .readonly="${this.readonly || this._readonly}" .disabled="${this.disabled}" .dense="${this.dense}" @or-input-changed="${(e: OrInputChangedEvent) => this.onValueChange(e.detail.value)}"></or-input>`;
        }

        return html`<span>INPUT TYPE NOT IMPLEMENTED</span>`;
    }

    public onAttributeEvent(event: AttributeEvent) {
        const oldValue = this.attribute ? this.attribute.value : undefined;
        if (this.attribute) {
            this.attribute.value = event.attributeState!.value;
            super.requestUpdate();
        }
        this.dispatchEvent(new OrInputChangedEvent(event.attributeState!.value, oldValue));
    }

    protected onValueChange(newValue: any) {
        if (this.attribute || this.attributeRef) {
            if (this._valueInboundConverter) {
                newValue = this._valueInboundConverter(newValue);
            }

            if (!this.preventWrite && !this.disabled && !this.readonly) {

                let attributeRef = this.attributeRef;

                if (!this.attributeRef) {
                    attributeRef = {
                        entityId: this.attribute!.assetId,
                        attributeName: this.attribute!.name
                    };
                }

                super._sendEvent({
                    eventType: "attribute",
                    attributeState: {
                        attributeRef: attributeRef,
                        value: newValue
                    }
                } as AttributeEvent);
            }
        }
    }
}
