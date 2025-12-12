import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {ifDefined} from "lit/directives/if-defined.js";
import {until} from "lit/directives/until.js";
import {createRef, Ref, ref} from 'lit/directives/ref.js';
import {i18next, translate} from "@openremote/or-translate";
import {
    Attribute,
    AttributeDescriptor,
    AttributeEvent,
    AttributeRef,
    SharedEvent,
    ValueDescriptor,
    WellknownMetaItems,
    WellknownValueTypes,
    AssetModelUtil,
    ClientRole,
    ValueConstraintAllowedValues
} from "@openremote/model";
import manager, {subscribe, Util} from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-vaadin-components/or-vaadin-input";
import {progressCircular} from "@openremote/or-mwc-components/style";
import "@openremote/or-components/or-loading-wrapper";
import {OrLoadingWrapper} from "@openremote/or-components/or-loading-wrapper";
import {getValueHolderInputTemplateProvider} from "@openremote/or-vaadin-components/util";
import type {
    InputType,
    ValueInputProvider,
    ValueInputProviderOptions,
    ValueInputTemplateFunction
} from "@openremote/or-vaadin-components/util";
import "@openremote/or-map";
import {geoJsonPointInputTemplateProvider} from "@openremote/or-map";
import "@openremote/or-json-forms";
import {ErrorObject, OrJSONForms, StandardRenderers} from "@openremote/or-json-forms";
import {agentIdRendererRegistryEntry} from "./agent-link-json-forms-renderer";
import {OrInputChangedEvent, type ValueInputProviderGenerator} from "@openremote/or-mwc-components/or-mwc-input";

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

export function getAttributeInputWrapper(content: TemplateResult, value: any, loading: boolean, disabled: boolean, helperText: string | undefined, label: string | undefined, buttonIcon?: string, sendValue?: () => void, fullWidth?: boolean): TemplateResult {

    if (helperText) {
        content = html`
                    <div id="wrapper-helper">
                        ${label ? html`<div id="wrapper-label">${label}</div>` : ``}
                        <div id="wrapper-input">${content}</div>
                        <div id="helper-text">${helperText}</div>
                    </div>
                `;
    }

    if (buttonIcon) {
        content = html`
                ${content}
                <or-mwc-input id="send-btn" icon="${buttonIcon}" type="button" .disabled="${disabled || loading}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
            e.stopPropagation();
            if (sendValue) {
                sendValue();
            }
        }}"></or-mwc-input>
            `;
    }

    return html`
            <div id="wrapper" class="${buttonIcon || fullWidth ? "no-padding" : "right-padding"}">
                ${content}
                <div id="scrim" class="${ifDefined(loading ? undefined : "hidden")}"><progress class="pure-material-progress-circular"></progress></div>
            </div>
        `;
}

export function getHelperText(sending: boolean, error: boolean, timestamp: number | undefined): string | undefined {
    if (sending) {
        return i18next.t("sending");
    }

    if (error) {
        return i18next.t("sendFailed");
    }

    if (!timestamp) {
        return;
    }

    return i18next.t("updatedWithDate", { date: new Date(timestamp) });
}

const jsonFormsAttributeRenderers = [...StandardRenderers, agentIdRendererRegistryEntry];

let valueDescriptorSchemaHashes: Record<string, string>
const schemas = new Map<string, any>()

export const jsonFormsInputTemplateProvider: (fallback: ValueInputProvider) => ValueInputProviderGenerator = (fallback) => (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {
    if (valueDescriptor.jsonType === "object" || valueDescriptor.arrayDimensions && valueDescriptor.arrayDimensions > 0) {
        const disabled = !!(options && options.disabled);
        const readonly = !!(options && options.readonly);
        const label = options.label;

        // Apply a custom UI schema to remove the outer VerticalLayout
        const uiSchema: any = {type: "Control", scope: "#"};
        let schema: any;
        const jsonForms: Ref<OrJSONForms> = createRef();
        const loadingWrapper: Ref<OrLoadingWrapper> = createRef();

        let initialised = false;
        let prevValue: any;

        const onChanged = (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}) => {
          if (!initialised) { 
              return
          };

          if (!Util.objectsEqual(dataAndErrors.data, prevValue)) {
              prevValue = dataAndErrors.data;
              valueChangeNotifier({
                  value: dataAndErrors.data
              });
          }
        };

        const doLoad = async (data: any) => {
            if (!initialised) {
                prevValue = data;
            }
            initialised = true;

            if (!valueDescriptorSchemaHashes) {
              const response = await manager.rest.api.StatusResource.getInfo();
              valueDescriptorSchemaHashes = response.data.valueDescriptorSchemaHashes;
            }

            const descriptor = valueDescriptor.name + "[]".repeat(valueDescriptor.arrayDimensions ?? 0);
            const hash = valueDescriptorSchemaHashes[descriptor];

            if (!schema && !schemas.has(descriptor)) {
                const response = await manager.rest.api.AssetModelResource.getValueDescriptorSchema({
                    name: descriptor,
                    hash,
                }, { headers: !hash ? { "Cache-Control": "no-cache" } : {} });
                schema = response.data;
                schemas.set(descriptor, schema);
                // label ||= schema.title
            } else {
                schema = schemas.get(descriptor);
            }

            if (jsonForms.value && loadingWrapper.value) {
                const forms = jsonForms.value;
                forms.schema = schema;
                forms.data = data;
                loadingWrapper.value.loading = false;
            }
        };

        const templateFunction: ValueInputTemplateFunction = (value, _focused, _loading, _sending, _error, _helperText) => {
            // Schedule loading
            window.setTimeout(() => doLoad(value), 0);

            return html`
                <style>
                    .disabled {
                        opacity: 0.5;
                        pointer-events: none;
                    }
                    or-loading-wrapper {
                        width: 100%;
                    }
                </style>
                <or-loading-wrapper ${ref(loadingWrapper)} .loading="${true}">
                    <or-json-forms .renderers="${jsonFormsAttributeRenderers}" ${ref(jsonForms)}
                                   .disabled="${disabled}" .readonly="${readonly}" .label="${label}"
                                   .schema="${schema}" .uischema="${uiSchema}" .onChange="${onChanged}"></or-json-forms>
                </or-loading-wrapper>
            `;
        };

        return {
            templateFunction: templateFunction,
            supportsHelperText: false,
            supportsLabel: false,
            supportsSendButton: false,
            validator: () => {
                if (!jsonForms.value) {
                    return false;
                }
                return jsonForms.value.checkValidity();
            }
        };
    }

    return fallback;
};

const DEFAULT_TIMEOUT = 5000;

// TODO: Add support for attribute not found and attribute deletion/addition
@customElement("or-attribute-input")
export class OrAttributeInput extends subscribe(manager)(translate(i18next)(LitElement)) {

    // language=CSS
    static get styles() {
        return [
            progressCircular,
            css`
            :host {
                display: inline-block;
            }
            
            :host(.force-btn-padding) #wrapper.no-padding {
                /*padding-right: 52px;*/
            }   
            
            #wrapper or-mwc-input, #wrapper or-vaadin-input, #wrapper or-map {
                width: 100%;
            }
            
            #wrapper or-map {
                min-height: 250px;
            }

            #wrapper .long-press-msg {
                display: none;
            }
            
            #wrapper {
                display: flex;
                position: relative;
                align-items: center;
            }
            
            #wrapper.right-padding {
                padding-right: 52px;
            }
            
            #wrapper-helper {
                display: flex;
                flex: 1;
                flex-direction: column;
            }
            
            #wrapper-input {
                flex: 1;
                display: flex;
            }
            
            #wrapper-label, #helper-text {
                margin-left: 16px;
            }
            
            /* Copy of mdc text field helper text styles */
            #helper-text {                
                min-width: 255px;
                color: rgba(0, 0, 0, 0.6);
                font-family: Roboto, sans-serif;
                -webkit-font-smoothing: antialiased;
                font-size: 0.75rem;
                font-weight: 400;
                letter-spacing: 0.0333333em;
            }
            
            #scrim {
                position: absolute;
                left: 0;
                top: 0;
                right: 0;
                bottom: 0;
                background: white;
                opacity: 0.2;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            #scrim.hidden {
                display: none;
            }

            #send-btn { 
                flex: 0;
                margin-left: 4px;
                margin-top: 10px;
            }
        `];
    }

    @property({type: Object, reflect: false})
    public attribute?: Attribute<any>;

    @property({type: String})
    public assetId?: string;

    @property({type: Object})
    public attributeDescriptor?: AttributeDescriptor;

    @property({type: Object})
    public valueDescriptor?: ValueDescriptor;

    @property({type: String})
    public assetType?: string;

    @property({type: String})
    public label?: string;

    @property({type: Boolean})
    public disabled?: boolean;

    @property({type: Boolean})
    public readonly?: boolean;

    @property({type: Boolean, attribute: true})
    public required?: boolean;

    @property()
    public value?: any;

    @property()
    public inputType?: InputType;

    @property({type: Boolean})
    public hasHelperText?: boolean;

    @property({type: Boolean})
    public disableButton?: boolean;

    @property({type: Boolean, attribute: true})
    public disableSubscribe: boolean = false;

    @property({type: Boolean})
    public disableWrite: boolean = false;

    @property({type: Boolean})
    public compact: boolean = false;

    @property({type: Boolean})
    public comfortable: boolean = false;

    @property({type: Boolean})
    public resizeVertical: boolean = false;

    @property({type: Boolean})
    public fullWidth?: boolean;

    @property({type: Boolean})
    public rounded?: boolean;

    @property({type: Boolean})
    public outlined?: boolean;

    @property()
    protected _attributeEvent?: AttributeEvent;

    @property()
    protected _writeTimeoutHandler?: number;

    public customProvider?: ValueInputProviderGenerator;
    public writeTimeout?: number = DEFAULT_TIMEOUT;
    protected _requestFocus = false;
    protected _newValue: any;
    protected _templateProvider?: ValueInputProvider;
    protected _sendError = false;
    protected _attributeDescriptor?: AttributeDescriptor;
    protected _valueDescriptor?: ValueDescriptor;

    public disconnectedCallback() {
        super.disconnectedCallback();
        this._clearWriteTimeout();
    }

    langChangedCallback = () => {
        this._updateTemplate();
        this.requestUpdate();
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        const shouldUpdate = super.shouldUpdate(_changedProperties);

        let updateSubscribedRefs = false;
        let updateDescriptors = false;

        if (_changedProperties.has("disableSubscribe")) {
            updateSubscribedRefs = true;
        }

        if (_changedProperties.has("attributeDescriptor")
            || _changedProperties.has("valueDescriptor")
            || _changedProperties.has("assetType")) {
            updateDescriptors = true;
        }

        if (_changedProperties.has("attribute")) {
            const oldAttr = {..._changedProperties.get("attribute") as Attribute<any>};
            const attr = this.attribute;

            if (oldAttr && attr) {
                const oldValue = oldAttr.value;
                const oldTimestamp = oldAttr.timestamp;

                // Compare attributes ignoring the timestamp and value
                oldAttr.value = attr.value;
                oldAttr.timestamp = attr.timestamp;
                if (Util.objectsEqual(oldAttr, attr)) {
                    // Compare value and timestamp
                    if (!Util.objectsEqual(oldValue, attr.value) || oldTimestamp !== attr.timestamp) {
                        this._onAttributeValueChanged(oldValue, attr.value, attr.timestamp);
                    } else if (_changedProperties.size === 1) {
                        // Only the attribute has 'changed' and we've handled it so don't perform update
                        return false;
                    }
                } else {
                    updateSubscribedRefs = true;
                    updateDescriptors = true;
                }
            }
        }

        if (_changedProperties.has("assetId") && _changedProperties.get("assetId") !== this.assetId) {
            updateSubscribedRefs = true;
            updateDescriptors = true;
        }

        if (updateDescriptors) {
            this._updateDescriptors();
        }

        if (updateSubscribedRefs) {
            this._updateSubscribedRefs();
        }

        if (this._templateProvider
            && (_changedProperties.has("disabled")
                || _changedProperties.has("readonly")
                || _changedProperties.has("required")
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
            const attributeRef = this._getAttributeRef();
            this.attributeRefs = attributeRef ? [attributeRef] : undefined;
        }
    }

    protected _getAttributeRef(): AttributeRef | undefined {
        if (this.assetId && this.attribute) {
            return {name: this.attribute.name, id: this.assetId};
        }
    }

    protected _updateDescriptors(): void {

        this._valueDescriptor = undefined;
        this._attributeDescriptor = undefined;

        if (this.attributeDescriptor && this.valueDescriptor) {
            this._attributeDescriptor = this.attributeDescriptor;
            this._valueDescriptor = this.valueDescriptor;
        } else {
            const attributeOrDescriptorOrName = this.attributeDescriptor || (this.attribute ? this.attribute.name : undefined);

            if (!attributeOrDescriptorOrName) {
                this._attributeDescriptor = this.attributeDescriptor;
                this._valueDescriptor = this.valueDescriptor;
            } else {
                const attributeAndValueDescriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.assetType, attributeOrDescriptorOrName, this.attribute);
                this._attributeDescriptor = attributeAndValueDescriptors[0];
                this._valueDescriptor = this.valueDescriptor ? this._valueDescriptor : attributeAndValueDescriptors[1];
            }
        }

        // Sort asset type options in alphabetical order
        if(this._valueDescriptor && this._valueDescriptor.name == WellknownValueTypes.ASSETTYPE) {
            const allowedValuesConstraint = this._valueDescriptor.constraints?.find(
                (constraint): constraint is ValueConstraintAllowedValues =>
                    (constraint as ValueConstraintAllowedValues).type === "allowedValues"
            );

            if (allowedValuesConstraint && allowedValuesConstraint.allowedValues) {
                allowedValuesConstraint.allowedValues.sort((a, b) => a.localeCompare(b));
            }
        }

        this._updateTemplate();
    }

    protected _updateTemplate(): void {
        this._templateProvider = undefined;

        if (!this.assetType) {
            return;
        }

        const valueDescriptor = AssetModelUtil.resolveValueDescriptor(this.attribute, this._valueDescriptor || this._attributeDescriptor);

        if (!valueDescriptor) {
            return;
        }

        const options: ValueInputProviderOptions = {
            readonly: this.isReadonly(),
            required: this.required,
            disabled: this.disabled,
            compact: this.compact,
            rounded: this.rounded,
            outlined: this.outlined,
            label: this.getLabel(),
            comfortable: this.comfortable,
            resizeVertical: this.resizeVertical,
            inputType: this.inputType
        };


        // Use json forms with fallback to simple input provider
        const valueChangeHandler = (value: any, updateImmediately?: boolean) => {
            updateImmediately ??= !this._templateProvider || !this.showButton || !this._templateProvider.supportsSendButton;
            this._onInputValueChanged(value, updateImmediately);
        };

        if (this.customProvider) {
            this._templateProvider = this.customProvider ? this.customProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail?.value), options) : undefined;
            return;
        }

        // Handle special value types
        if (valueDescriptor.name === WellknownValueTypes.GEOJSONPOINT) {
            this._templateProvider = geoJsonPointInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail?.value), options);
            return;
        }

        const standardInputProvider = getValueHolderInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (value) => valueChangeHandler(value, true), options);
        this._templateProvider = jsonFormsInputTemplateProvider(standardInputProvider)(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail?.value), options);

        if (!this._templateProvider) {
            this._templateProvider = standardInputProvider;
        }
    }

    public getLabel(): string | undefined {
        let label;

        if (this.label) {
            label = this.label;
        } else if (this.label !== "" && this.label !== null) {
            const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(this.assetType, this.attribute ? this.attribute!.name : undefined, this._attributeDescriptor);
            label = Util.getAttributeLabel(this.attribute, descriptors[0], this.assetType, true);
        }

        return label;
    }

    public isReadonly(): boolean {
        if(!manager.hasRole(ClientRole.WRITE_ATTRIBUTES)) {
            this.readonly = !manager.hasRole(ClientRole.WRITE_ATTRIBUTES);
            return this.readonly;
        }

        return this.readonly !== undefined ? this.readonly : Util.getMetaValue(WellknownMetaItems.READONLY, this.attribute, this._attributeDescriptor);
    }

    public render() {

        if (!this.assetType || !this._templateProvider) {
            return html``;
        }
        
        // Check if attribute hasn't been loaded yet or pending write
        const loading = (this.attributeRefs && !this._attributeEvent) || !!this._writeTimeoutHandler;
        let content: TemplateResult;

        const value = this.getValue();
        const focus = this._requestFocus;
        this._requestFocus = false;
        const helperText = this.hasHelperText ? getHelperText(!!this._writeTimeoutHandler, this._sendError, this.getTimestamp()) : undefined;
        const buttonIcon = !this.showButton ? (this.disableButton ? undefined : "") : this._writeTimeoutHandler ? "send-clock" : "send";

        if (this._templateProvider && this._templateProvider.templateFunction) {
            content = html`${until(this._templateProvider.templateFunction(value, focus, loading, !!this._writeTimeoutHandler, this._sendError, this._templateProvider.supportsHelperText ? helperText : undefined), ``)}`;
        } else {
            content = html`<or-translate .value="attributeUnsupported"></or-translate>`;
        }

        content = getAttributeInputWrapper(content, value, loading, !!this.disabled, this._templateProvider.supportsHelperText ? undefined : helperText, this._templateProvider.supportsLabel ? undefined : this.getLabel(), this._templateProvider.supportsSendButton ? buttonIcon : undefined, () => this._updateValue(), this.fullWidth);
        return content;
    }

    protected updated(_changedProperties: PropertyValues): void {
        if (_changedProperties.has("_writeTimeoutHandler") && !this._writeTimeoutHandler) {
            this._requestFocus = true;
            this.requestUpdate();
        }
    }

    protected get showButton(): boolean {
        if (this.isReadonly() || this.disabled || this.disableButton || !this._getAttributeRef()) {
            return false;
        }

        return this._templateProvider ? this._templateProvider.supportsSendButton : false;
    }

    protected getValue(): any {
        return this._attributeEvent ? this._attributeEvent.value : this.attribute ? this.attribute.value : this.value;
    }

    protected getTimestamp(): number | undefined {
        return this._attributeEvent ? this._attributeEvent.timestamp : this.attribute ? this.attribute.timestamp : undefined;
    }

    /**
     * This is called by asset-mixin
     */
    public _onEvent(event: SharedEvent) {
        if (event.eventType !== "attribute") {
            return;
        }

        const oldValue = this.getValue();
        this._attributeEvent = event as AttributeEvent;
        this._onAttributeValueChanged(oldValue, this._attributeEvent.value, event.timestamp);
    }

    public checkValidity(): boolean {
        if (!this._templateProvider) {
            return false;
        }
        if (!this._templateProvider.validator) {
            return true;
        }
        return this._templateProvider.validator();
    }

    protected _onAttributeValueChanged(oldValue: any, newValue: any, timestamp?: number) {
        if (this.attribute) {
            this.attribute.value = newValue;
            this.attribute.timestamp = timestamp;
        }

        this._clearWriteTimeout();
        this.value = newValue;
        this._sendError = false;
        this.dispatchEvent(new OrAttributeInputChangedEvent(newValue, oldValue));
    }

    protected _onInputValueChanged(value: any, updateImmediately: boolean) {
        this._newValue = value;

        if (updateImmediately) {
            this._updateValue();
        }
    }

    protected _updateValue() {
        if (this.readonly || this.isReadonly()) {
            return;
        }

        if (this._writeTimeoutHandler) {
            return;
        }

        if (this._newValue === undefined) {
            this._newValue = this.getValue();
        }

        const oldValue = this.getValue();
        const newValue = this._newValue;
        this._newValue = undefined;


        // If we have an attributeRef then send an update and wait for the updated attribute event to come back through
        // the system or for the attribute property to be updated by a parent control or timeout and reset the value
        const attributeRef = this._getAttributeRef();

        if (attributeRef && !this.disableWrite) {

            super._sendEvent({
                eventType: "attribute",
                ref: attributeRef,
                value: newValue
            } as AttributeEvent);

            this._writeTimeoutHandler = window.setTimeout(() => this._onWriteTimeout(), this.writeTimeout);
        } else {
            this.value = newValue;
            this.dispatchEvent(new OrAttributeInputChangedEvent(newValue, oldValue));
        }
    }

    protected _clearWriteTimeout() {
        if (this._writeTimeoutHandler) {
            window.clearTimeout(this._writeTimeoutHandler);
        }
        this._writeTimeoutHandler = undefined;
    }

    protected _onWriteTimeout() {
        this._sendError = true;
        if (!this.showButton || this.hasHelperText) {
            this.requestUpdate("value");
        }
        this._clearWriteTimeout();
    }
}
