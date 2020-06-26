import {css, customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import {i18next, translate} from "@openremote/or-translate";
import {
    AssetAttribute,
    AttributeDescriptor,
    AttributeEvent,
    AttributeRef,
    AttributeValueDescriptor,
    AttributeValueType,
    MetaItemType,
    ValueType,
    SharedEvent
} from "@openremote/model";
import manager, {AssetModelUtil, subscribe, Util} from "@openremote/core";
import "@openremote/or-input";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";
import "@openremote/or-map";
import {Util as MapUtil, LngLat, getMarkerIconAndColorFromAssetType, OrMapClickedEvent, MapEventDetail} from "@openremote/or-map";

export class OrAttributeInputChangedEvent extends CustomEvent<OrAttributeInputChangedEventDetail> {

    public static readonly NAME = "or-attribute-input-changed";

    constructor(value?: any, previousValue?: any) {
        super(OrAttributeInputChangedEvent.NAME, {
            detail: {
                value: value,
                previousValue: previousValue
            },
            bubbles: true,
            composed: true
        });
    }
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

export type AttributeInputCustomProvider = (assetType: string | undefined, attribute: AssetAttribute | undefined, attributeDescriptor: AttributeDescriptor | undefined, valueDescriptor: AttributeValueDescriptor | undefined, valueChangeNotifier: (value: any | undefined) => void, readonly: boolean | undefined, disabled: boolean | undefined, label: string | undefined) => ((value: any) => TemplateResult) | undefined;

export const GeoJsonPointInputTemplateProvider: AttributeInputCustomProvider = (assetType, attribute, attributeDescriptor, valueDescriptor, valueChangeNotifier, readonly, disabled, label) => (value: any) => {
    let pos: LngLat | undefined;
    let center: number[] | undefined;
    if (value) {
        pos = MapUtil.getLngLat(value);
        center = pos ? pos.toArray() : undefined;
    }
    const iconAndColor = getMarkerIconAndColorFromAssetType(assetType);

    const clickHandler = (mapClickDetail: MapEventDetail) => {
        if (!readonly && !disabled) {
            const geoJsonPoint = MapUtil.getGeoJSONPoint(mapClickDetail.lngLat);
            if (valueChangeNotifier) {
                valueChangeNotifier(geoJsonPoint);
            }
        }
    };

    return html`
        <style>
            or-map {
                border: #e5e5e5 1px solid;
            }
        </style>                    
        <or-map class="or-map" @or-map-clicked="${(ev: OrMapClickedEvent) => clickHandler(ev.detail)}" .center="${center}">
            <or-map-marker active .lng="${pos ? pos.lng : undefined}" .lat="${pos ? pos.lat : undefined}" .icon="${iconAndColor ? iconAndColor.icon : undefined}" .color="${iconAndColor ? iconAndColor.color : undefined}"></or-map-marker>
        </or-map>
    `;
}

const DEFAULT_TIMEOUT = 5000;

// TODO: Add support for attribute not found and attribute deletion/addition
@customElement("or-attribute-input")
export class OrAttributeInput extends subscribe(manager)(translate(i18next)(LitElement)) {

    // language=CSS
    static get styles() {
        return css`
            :host {
                display: inline-block;
            }
            
            or-input {
                width: 100%;
            }
            
            #wrapper {
                display: flex;
                position: relative;
            }
            
            #scrim {
                position: absolute;
                left: 0;
                top: 0;
                right: 0;
                bottom: 0;
                opacity: 0.5;
            }
        `;
    }

    @property({type: Object, reflect: false})
    public attribute?: AssetAttribute;

    @property({type: Object})
    public attributeRef?: AttributeRef;

    @property({type: Object})
    public attributeDescriptor?: AttributeDescriptor;

    @property({type: Object})
    public attributeValueDescriptor?: AttributeValueDescriptor;

    @property({type: String})
    public assetType?: string;

    @property({type: String})
    public label?: string;

    @property({type: Boolean})
    public disabled?: boolean;

    @property({type: Boolean})
    public readonly?: boolean;

    @property()
    public value?: any;

    @property()
    public inputType?: InputType;

    @property({type: Boolean})
    public disableSubscribe: boolean = false;

    @query("#loading")
    protected _orInput!: OrInput;

    @property()
    protected _attributeEvent?: AttributeEvent;

    public customProvider?: AttributeInputCustomProvider;
    public writeTimeout?: number = DEFAULT_TIMEOUT;

    protected _template?: (value: any) => TemplateResult;
    protected _attributeDescriptor?: AttributeDescriptor;
    protected _attributeValueDescriptor?: AttributeValueDescriptor;
    protected _inputType?: InputType;
    protected _step?: number;
    protected _min?: any;
    protected _max?: any;
    protected _label?: string;
    protected _unit?: string;
    protected _options?: any;
    protected _readonly?: boolean;
    protected _disabled?: boolean;
    protected _valueFormatter?: (value: any) => any;
    protected _writeTimeoutHandler?: number;

    public disconnectedCallback() {
        super.disconnectedCallback();
        if (this._writeTimeoutHandler) {
            window.clearTimeout(this._writeTimeoutHandler);
            this._writeTimeoutHandler = undefined;
        }
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        const shouldUpdate = super.shouldUpdate(_changedProperties);

        let updateSubscribedRefs = false;
        let updateDescriptors = false;

        if (_changedProperties.has("disableSubscribe")) {
            updateSubscribedRefs = true;
        }

        if (_changedProperties.has("attributeDescriptor")
            || _changedProperties.has("attributeValueDescriptor")
            || _changedProperties.has("assetType")) {
            updateDescriptors = true;
        }

        if (_changedProperties.has("attribute") || _changedProperties.has("attributeRef")) {
            updateSubscribedRefs = true;
            updateDescriptors = true;
        }

        if (updateDescriptors) {
            this._updateDescriptors();
        }

        if (updateSubscribedRefs) {
            this._updateSubscribedRefs();
        }

        if (this._template
            && (_changedProperties.has("disabled")
                || _changedProperties.has("readonly")
                || _changedProperties.has("label"))) {
            this._updateTemplate();
        }

        return shouldUpdate;
    }

    protected _updateSubscribedRefs(): void {
        this._attributeEvent = undefined;

        if (this.disableSubscribe) {
            this.attributeRefs = undefined;
        } else {
            const attributeRef = this.attribute ? {entityId: this.attribute.assetId!, attributeName: this.attribute.name!} : this.attributeRef;
            this.attributeRefs = attributeRef ? [attributeRef] : undefined;
        }
    }

    protected _updateDescriptors(): void {

        this._attributeValueDescriptor = undefined;
        this._attributeDescriptor = undefined;

        if (this.attributeDescriptor) {
            this._attributeDescriptor = this.attributeDescriptor;
        } else if (this.attribute || this.attributeRef) {
            const attrName = this.attribute ? this.attribute.name! : this.attributeRef!.attributeName!;
            this._attributeDescriptor = AssetModelUtil.getAttributeDescriptorFromAsset(attrName, this.assetType);
        }

        if (this.attributeValueDescriptor) {
            this._attributeValueDescriptor = this.attributeValueDescriptor;
        } else if (this._attributeDescriptor) {
            this._attributeValueDescriptor = this._attributeDescriptor.valueDescriptor;
        } else if (this.attribute) {
            this._attributeValueDescriptor = AssetModelUtil.getAttributeValueDescriptorFromAsset(this.attribute ? this.attribute.type as string : undefined, this.assetType, this._attributeDescriptor ? this._attributeDescriptor!.attributeName : undefined);
        }

        this._updateTemplate();
    }

    protected _updateTemplate(): void {
        this._template = undefined;
        this._inputType = undefined;
        this._step = undefined;
        this._min = undefined;
        this._max = undefined;
        this._label = undefined;
        this._unit = undefined;
        this._options = undefined;
        this._disabled = undefined;
        this._readonly = undefined;
        this._inputType = undefined;
        this._valueFormatter = undefined;

        if (this.customProvider) {
            this._template = this.customProvider(this.assetType, this.attribute, this._attributeDescriptor, this._attributeValueDescriptor, (v) => this._onValueChange(v), this.readonly, this.disabled, this.label);
        }

        if (this._template) {
            return;
        }

        if (this.inputType) {
            this._inputType = this.inputType;
        } else if (this._attributeValueDescriptor) {
            switch (this._attributeValueDescriptor.name) {
                case AttributeValueType.GEO_JSON_POINT.name:
                    this._template = GeoJsonPointInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, this._attributeValueDescriptor, (v) => this._onValueChange(v), this.readonly, this.disabled, this.label);
                    return;
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
                case AttributeValueType.BRIGHTNESS.name:
                    this._inputType = InputType.NUMBER;
                    this._step = 1;
                    break;
                default:
                    // Use value type
                    switch (this._attributeValueDescriptor.valueType) {
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

        if (!this._inputType && (this.attribute || this.value)) {
            const currentValue = this.attribute ? this.attribute.value : this.value;

            if (currentValue !== undefined && currentValue !== null) {
                if (typeof currentValue === "number") {
                    this._inputType = InputType.NUMBER;
                } else if (typeof currentValue === "string") {
                    this._inputType = InputType.TEXT;
                } else if (typeof currentValue === "boolean") {
                    this._inputType = InputType.SWITCH;
                } else {
                    this._inputType = InputType.JSON;
                }
            }
        }

        if (this._inputType) {
            this._min = Util.getMetaValue(MetaItemType.RANGE_MIN, this.attribute, this._attributeDescriptor, this._attributeValueDescriptor) as number;
            this._max = Util.getMetaValue(MetaItemType.RANGE_MAX, this.attribute, this._attributeDescriptor, this._attributeValueDescriptor) as number;
            this._unit = Util.getMetaValue(MetaItemType.UNIT_TYPE, this.attribute, this._attributeDescriptor, this._attributeValueDescriptor);
            this._label = this.label !== undefined ? this.label :  Util.getAttributeLabel(this.attribute, this._attributeDescriptor) + (this._unit ? " (" + i18next.t(this._unit) + ")" : "");
            this._readonly = this.readonly !== undefined ? this.readonly : Util.getMetaValue(MetaItemType.READ_ONLY, this.attribute, this._attributeDescriptor);
            this._disabled = this.disabled;
            this._options = Util.getMetaValue(MetaItemType.ALLOWED_VALUES, this.attribute, this._attributeDescriptor);

            if (!this.inputType && this._inputType === InputType.TEXT && this._options && Array.isArray(this._options) && this._options.length > 0) {
                this._inputType = InputType.SELECT;
            }

            switch (this._inputType) {
                case InputType.NUMBER:
                case InputType.TEXT:
                case InputType.COLOUR:
                case InputType.TELEPHONE:
                    this._valueFormatter = Util.getAttributeValueFormatter(this.attribute, this._attributeDescriptor, this._attributeValueDescriptor);
            }
        }
    }

    public render() {

        // Check if attribute hasn't been loaded yet or pending write
        const loading = this.attributeRefs && !this._attributeEvent;
        let content: TemplateResult | string | undefined = "";

        if (!loading) {
            let value = this.getValue();

            if (this._template) {
                content = this._template(value);
            } else {

                if (!this._inputType) {
                    content = html`<div>INVALID</div>`;
                }

                if (this._valueFormatter) {
                    value = this._valueFormatter(value);
                }

                content = html`<or-input id="input" .type="${this._inputType}" .label="${this._label}" .value="${value}" .allowedValues="${this._options}" .min="${this._min}" .max="${this._max}" .options="${this._options}" .readonly="${this._readonly}" .disabled="${this._disabled}" @or-input-changed="${(e: OrInputChangedEvent) => {
                    this._onValueChange(e.detail.value);
                    e.stopPropagation()
                }}"></or-input>`;
            }
        }

        return html`
            <div id="wrapper">
                ${content}
                ${loading || this._writeTimeoutHandler 
                    ? html`<div id="scrim"><progress class="pure-material-progress-circular"></progress></div>` 
                    : ``}                
            </div>
        `;
    }

    protected getValue(): any {
        return this._attributeEvent ? this._attributeEvent.attributeState!.value : this.value;
    }

    public _onEvent(event: SharedEvent) {
        if (event.eventType !== "attribute") {
            return;
        }

        this._attributeEvent = event as AttributeEvent;

        const oldValue = this.attribute ? this.attribute.value : undefined;
        if (this.attribute) {
            this.attribute.value = this._attributeEvent.attributeState!.value;
            this.attribute.valueTimestamp = event.timestamp;
            super.requestUpdate();
        }
        this.dispatchEvent(new OrAttributeInputChangedEvent(this._attributeEvent.attributeState!.value, oldValue));
    }

    protected _onValueChange(newValue: any) {
        const oldValue = this.getValue();

        if (this.readonly || this._readonly) {
            return;
        }

        // Check if this control is linked to the backend via asset-mixin; if so send an update and wait for the
        // updated attribute event to come back through the system or timeout and reset the value
        if (this.attributeRefs) {
            const attributeRef = this.attributeRefs[0];

            super._sendEvent({
                eventType: "attribute",
                attributeState: {
                    attributeRef: attributeRef,
                    value: newValue
                }
            } as AttributeEvent);

            // Clear the last attribute event which will cause loading state on render
            this._writeTimeoutHandler = window.setTimeout(() => this._onWriteTimeout(), this.writeTimeout);
        } else {
            this.dispatchEvent(new OrAttributeInputChangedEvent(newValue, oldValue));
        }
    }

    protected _onWriteTimeout() {
        this._writeTimeoutHandler = undefined;
    }
}
