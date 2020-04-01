import {css, customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import {i18next, translate} from "@openremote/or-translate";
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
import manager, {AssetModelUtil, subscribe, Util} from "@openremote/core";
import "@openremote/or-input";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";

export function getAttributeValueTemplate(
    assetType: string | undefined,
    attribute: Attribute,
    readonly: boolean | undefined,
    disabled: boolean | undefined,
    valueChangedCallback: (value: any) => void,
    customProvider?: (assetType: string | undefined, attribute: Attribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean | undefined, disabled: boolean | undefined) => ((value: any) => TemplateResult) | undefined,
    forceInputType?: InputType,
    labelOverride?: string) {

    let template: ((value: any) => TemplateResult) | undefined;

    if (!attribute) {
        return () => html``;
    }

    let attributeDescriptor: AttributeDescriptor | undefined;
    let valueDescriptor: AttributeValueDescriptor | undefined;

    attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(attribute.name!, assetType);
    valueDescriptor = AssetModelUtil.getAttributeValueDescriptorFromAsset(attributeDescriptor && attributeDescriptor.valueDescriptor ? attributeDescriptor.valueDescriptor.name : attribute ? attribute.type as string : undefined, assetType, attribute.name);

    if (customProvider) {
        template = customProvider(assetType, attribute, attributeDescriptor, valueDescriptor, (newValue: any) => valueChangedCallback(newValue), readonly, disabled);
    }

    if (!template) {
        let inputType: InputType | undefined;
        let step: number | undefined;
        let min: number | undefined;
        let max: number | undefined;
        let ro: boolean | undefined;
        let label: string | undefined;
        let unit: string | undefined;
        let options: any | undefined;

        if (valueDescriptor) {

            switch (valueDescriptor.name) {
                case AttributeValueType.GEO_JSON_POINT.name:
                    break;
                case AttributeValueType.BOOLEAN.name:
                case AttributeValueType.SWITCH_TOGGLE.name:
                    inputType = InputType.SWITCH;
                    break;
                case AttributeValueType.NUMBER.name:
                    inputType = InputType.NUMBER;
                    break;
                case AttributeValueType.STRING.name:
                    inputType = InputType.TEXT;
                    break;
                case AttributeValueType.OBJECT.name:
                case AttributeValueType.ARRAY.name:
                    inputType = InputType.JSON;
                    break;
                case AttributeValueType.BRIGHTNESS.name:
                    inputType = InputType.NUMBER;
                    step = 1;
                    break;
                default:
                    // Use value type
                    switch (valueDescriptor.valueType) {
                        case ValueType.STRING:
                            inputType = InputType.TEXT;
                            break;
                        case ValueType.NUMBER:
                            inputType = InputType.NUMBER;
                            break;
                        case ValueType.BOOLEAN:
                            inputType = InputType.SWITCH;
                            break;
                        default:
                            inputType = InputType.JSON;
                            break;
                    }
                    break;
            }
        }

        if (forceInputType) {
            inputType = forceInputType;
        }

        if (!inputType) {
            const currentValue = attribute.value;

            if (currentValue) {
                if (typeof currentValue === "number") {
                    inputType = InputType.NUMBER;
                } else if (typeof currentValue === "string") {
                    inputType = InputType.TEXT;
                } else if (typeof currentValue === "boolean") {
                    inputType = InputType.SWITCH;
                } else {
                    inputType = InputType.JSON;
                }
            }
        }

        if (inputType) {
            // TODO: Update to use 'effective' meta so not dependent on meta items being defined on the asset itself
            min = Util.getMetaValue(MetaItemType.RANGE_MIN, attribute, attributeDescriptor) as number;
            max = Util.getMetaValue(MetaItemType.RANGE_MAX, attribute, attributeDescriptor) as number;
            label = labelOverride !== undefined ? labelOverride : Util.getAttributeLabel(attribute, attributeDescriptor);
            ro = readonly === undefined ? Util.getMetaValue(MetaItemType.READ_ONLY, attribute, attributeDescriptor) : readonly;
            unit = Util.getMetaValue(MetaItemType.UNIT_TYPE, attribute, attributeDescriptor);
            options = Util.getMetaValue(MetaItemType.ALLOWED_VALUES, attribute, attributeDescriptor);
            if(unit) {
                label = label + " ("+i18next.t(unit)+")";
            }
            if (inputType === InputType.TEXT && options && Array.isArray(options) && options.length > 0) {
                inputType = InputType.SELECT;
            }

            const getValue = (value: any) => {
                if (inputType === InputType.JSON && value !== null && typeof(value) !== "string") {
                    value = JSON.stringify(value, null, 2);
                }

                return value;
            };

            const setValue = (value: any) => {
                if (inputType === InputType.JSON && value !== null && typeof(value) !== "string") {
                    if (typeof(value) === "string") {
                        value = JSON.parse(value);
                    }
                }
                valueChangedCallback(value);
            };

            template = (value) => html`<or-input .type="${inputType}" .label="${label}" .value="${getValue(value)}" .allowedValues="${options}" .min="${min}" .max="${max}" .options="${options}" .readonly="${readonly || ro}" .disabled="${disabled}" @or-input-changed="${(e: OrInputChangedEvent) => setValue(e.detail.value)}"></or-input>`;
        } else {
            template = () => html``;
        }
    }

    return template;
}

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
    public disabled?: boolean;

    @property({type: Boolean})
    public disableSubscribe: boolean = false;

    @property({type: Boolean})
    public disableWrite: boolean = false;

    @property({type: Boolean})
    public readonly?: boolean;

    public customProvider?: (assetType: string | undefined, attribute: Attribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean | undefined, disabled: boolean | undefined) => ((value: any) => TemplateResult) | undefined;

    protected _loading: boolean = false;
    protected _invalid: boolean = false;
    protected _template?: (value: any) => TemplateResult;

    @query("#or-input")
    protected _input!: OrInput;

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        const shouldUpdate = super.shouldUpdate(_changedProperties);

        if (_changedProperties.has("attribute")
            || _changedProperties.has("attributeRef")
            || _changedProperties.has("assetType")) {

            if (!this.attribute) {
                this._loading = true;
                if (this.attributeRef) {
                    manager.rest.api.AssetResource.queryAssets({
                        ids: [this.attributeRef.entityId!],
                        select: {
                            attributes: [this.attributeRef.attributeName!],
                            excludePath: true,
                            excludeParentInfo: true
                        }
                    }).then((response) => {
                        if (response.data) {
                            this.assetType = response.data[0].type;
                            this.attribute = Util.getAssetAttribute(response.data[0], this.attributeRef!.attributeName!);
                        }
                    });
                } else {
                    this._invalid = true;
                }
            } else {
                this._invalid = false;
                this._template = getAttributeValueTemplate(this.assetType, this.attribute, this.readonly, this.disabled, (value) => this.onValueChange(value), this.customProvider);
            }
        }

        return shouldUpdate;
    }

    public updated(_changedProperties: PropertyValues): void {
        super.updated(_changedProperties);

        if (_changedProperties.has("attribute") || _changedProperties.has("disabledSubscribe")) {
            if (this.attribute && !this.disableSubscribe) {
                this._loading = true;
                super.attributeRefs = [{entityId: this.attribute!.assetId!, attributeName: this.attribute!.name!}];
            } else {
                super.attributeRefs = undefined;
            }
        }
    }

    public render() {

        if (this._invalid) {
            return html`
                <div>INVALID</div>
            `;
        }

        if (this._loading) {
            return html`
                <div>LOADING</div>
            `;
        }

        if (this._template) {
            return html`${this._template(this.attribute!.value)}`;
        }
    }

    public onAttributeEvent(event: AttributeEvent) {
        const oldValue = this.attribute ? this.attribute.value : undefined;
        if (this.attribute) {
            this.attribute.value = event.attributeState!.value;
            this.attribute.valueTimestamp = event.timestamp;
            this._loading = false;
            super.requestUpdate();
        }
        this.dispatchEvent(new OrInputChangedEvent(event.attributeState!.value, oldValue));
    }

    protected onValueChange(newValue: any) {
        if (this.attribute || this.attributeRef) {

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
