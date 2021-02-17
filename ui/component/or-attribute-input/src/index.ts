import {
    css,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    query,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import {ifDefined} from "lit-html/directives/if-defined";
import {i18next, translate} from "@openremote/or-translate";
import {
    Attribute,
    AttributeDescriptor,
    AttributeEvent,
    AttributeRef,
    WellknownMetaItems,
    SharedEvent,
    ValueDescriptor,
    WellknownValueTypes
} from "@openremote/model";
import manager, {AssetModelUtil, DefaultColor4, subscribe, Util} from "@openremote/core";
import "@openremote/or-input";
import {InputType, OrInput, OrInputChangedEvent, ValueInputProviderOptions, ValueInputProviderGenerator, getValueHolderInputTemplateProvider, ValueInputProvider, OrInputChangedEventDetail} from "@openremote/or-input";
import "@openremote/or-map";
import { geoJsonPointInputTemplateProvider } from "@openremote/or-map";

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

export function getAttributeInputWrapper(content: TemplateResult, value: any, loading: boolean, disabled: boolean, helperText: string | undefined, label: string | undefined, buttonIcon?: string, sendValue?: () => void): TemplateResult {

    if (helperText) {
        content = html`
                    <div id="wrapper-helper">
                        ${label ? html`<div style="margin-left: 16px">${label}</div>` : ``}
                        <div id="wrapper-input">${content}</div>
                        <div id="helper-text">${helperText}</div>
                    </div>
                `;
    }

    if (buttonIcon) {
        content = html`
                ${content}
                <or-input id="send-btn" icon="${buttonIcon}" type="button" .disabled="${disabled || loading}" @or-input-changed="${(e: OrInputChangedEvent) => {
            e.stopPropagation();
            if (sendValue) {
                sendValue();
            }
        }}"></or-input>
            `;
    }

    return html`
            <div id="wrapper" class="${buttonIcon === undefined || buttonIcon ? "no-padding" : "right-padding"}">
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
            
            #wrapper or-input, #wrapper or-map {
                width: 100%;
            }
            
            #wrapper or-map {
                min-height: 250px;
            }
            
            #wrapper {
                display: flex;
                position: relative;
            }
            
            #wrapper.right-padding {
                padding-right: 48px;
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
            
            #wrapper-input > or-input {
                margin-left: 16px;
            }
            
            /* Copy of mdc text field helper text styles */
            #helper-text {
                margin-left: 16px;
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
            }
            
            /*  https://codepen.io/finnhvman/pen/bmNdNr  */
            .pure-material-progress-circular {
                -webkit-appearance: none;
                -moz-appearance: none;
                appearance: none;
                box-sizing: border-box;
                border: none;
                border-radius: 50%;
                padding: 0.25em;
                width: 3em;
                height: 3em;
                color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
                background-color: transparent;
                font-size: 16px;
                overflow: hidden;
            }

            .pure-material-progress-circular::-webkit-progress-bar {
                background-color: transparent;
            }

            /* Indeterminate */
            .pure-material-progress-circular:indeterminate {
                -webkit-mask-image: linear-gradient(transparent 50%, black 50%), linear-gradient(to right, transparent 50%, black 50%);
                mask-image: linear-gradient(transparent 50%, black 50%), linear-gradient(to right, transparent 50%, black 50%);
                animation: pure-material-progress-circular 6s infinite cubic-bezier(0.3, 0.6, 1, 1);
            }

            :-ms-lang(x), .pure-material-progress-circular:indeterminate {
                animation: none;
            }

            .pure-material-progress-circular:indeterminate::before,
            .pure-material-progress-circular:indeterminate::-webkit-progress-value {
                content: "";
                display: block;
                box-sizing: border-box;
                margin-bottom: 0.25em;
                border: solid 0.25em transparent;
                border-top-color: currentColor;
                border-radius: 50%;
                width: 100% !important;
                height: 100%;
                background-color: transparent;
                animation: pure-material-progress-circular-pseudo 0.75s infinite linear alternate;
            }

            .pure-material-progress-circular:indeterminate::-moz-progress-bar {
                box-sizing: border-box;
                border: solid 0.25em transparent;
                border-top-color: currentColor;
                border-radius: 50%;
                width: 100%;
                height: 100%;
                background-color: transparent;
                animation: pure-material-progress-circular-pseudo 0.75s infinite linear alternate;
            }

            .pure-material-progress-circular:indeterminate::-ms-fill {
                animation-name: -ms-ring;
            }

            @keyframes pure-material-progress-circular {
                0% {
                    transform: rotate(0deg);
                }
                12.5% {
                    transform: rotate(180deg);
                    animation-timing-function: linear;
                }
                25% {
                    transform: rotate(630deg);
                }
                37.5% {
                    transform: rotate(810deg);
                    animation-timing-function: linear;
                }
                50% {
                    transform: rotate(1260deg);
                }
                62.5% {
                    transform: rotate(1440deg);
                    animation-timing-function: linear;
                }
                75% {
                    transform: rotate(1890deg);
                }
                87.5% {
                    transform: rotate(2070deg);
                    animation-timing-function: linear;
                }
                100% {
                    transform: rotate(2520deg);
                }
            }

            @keyframes pure-material-progress-circular-pseudo {
                0% {
                    transform: rotate(-30deg);
                }
                29.4% {
                    border-left-color: transparent;
                }
                29.41% {
                    border-left-color: currentColor;
                }
                64.7% {
                    border-bottom-color: transparent;
                }
                64.71% {
                    border-bottom-color: currentColor;
                }
                100% {
                    border-left-color: currentColor;
                    border-bottom-color: currentColor;
                    transform: rotate(225deg);
                }
            }
        `;
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

    @property()
    public value?: any;

    @property()
    public inputType?: InputType;

    @property({type: Boolean})
    public hasHelperText?: boolean;

    @property({type: Boolean})
    public disableButton?: boolean;

    @property({type: Boolean})
    public disableSubscribe: boolean = false;

    @property({type: Boolean})
    public disableWrite: boolean = false;

    @property({type: Boolean})
    public compact: boolean = false;

    @property()
    protected _attributeEvent?: AttributeEvent;

    @property()
    protected _writeTimeoutHandler?: number;

    @query("#send-btn")
    protected _sendButton!: OrInput;
    @query("#scrim")
    protected _scrimElem!: HTMLDivElement;

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
            disabled: this.disabled,
            compact: this.compact,
            label: this.getLabel(),
            inputType: this.inputType
        };

        if (this.customProvider) {
            this._templateProvider = this.customProvider ? this.customProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (v) => this._onInputValueChanged(v), options) : undefined;
            return;
        }

        // Handle special value types
        if (valueDescriptor.name === WellknownValueTypes.GEOJSONPOINT) {
            this._templateProvider = geoJsonPointInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (v: any) => this._onInputValueChanged(v), options);
            return;
        }

        // Fallback to simple input provider
        const valueChangeHandler = (detail: OrInputChangedEventDetail) => {
            if (detail.enterPressed || !this._templateProvider || !this.showButton || !this._templateProvider.supportsSendButton) {
                this._onInputValueChanged(detail.value);
            } else {
                this._newValue = detail.value;
            }
        };
        this._templateProvider = getValueHolderInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (e: OrInputChangedEventDetail) => valueChangeHandler(e), options);
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
        if(!manager.hasRole("write:attributes")) {
            this.readonly = !manager.hasRole("write:attributes");
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
            content = this._templateProvider.templateFunction(value, focus, loading, !!this._writeTimeoutHandler, this._sendError, this._templateProvider.supportsHelperText ? helperText : undefined);
        } else {
            content = html`<or-translate .value="attributeUnsupported"></or-translate>`;
        }

        content = getAttributeInputWrapper(content, value, loading, !!this.disabled, this._templateProvider.supportsHelperText ? undefined : helperText, this._templateProvider.supportsLabel ? undefined : this.getLabel(), this._templateProvider.supportsSendButton ? buttonIcon : undefined, () => this._updateValue());
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
        return this._attributeEvent ? this._attributeEvent.attributeState!.value : this.attribute ? this.attribute.value : this.value;
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
        this._onAttributeValueChanged(oldValue, this._attributeEvent.attributeState!.value, event.timestamp);
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

    protected _onInputValueChanged(value: any) {
        this._newValue = value;
        this._updateValue();
    }

    protected _updateValue() {
        if (this.readonly || this.isReadonly()) {
            return;
        }

        if (this._writeTimeoutHandler) {
            return;
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
                attributeState: {
                    ref: attributeRef,
                    value: newValue
                }
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
