import {
    css,
    html,
    LitElement,
    PropertyValues,
    TemplateResult,
    unsafeCSS
} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {ifDefined} from "lit/directives/if-defined";
import {until} from "lit/directives/until";
import {createRef, Ref, ref} from 'lit/directives/ref.js';
import {i18next, translate} from "@openremote/or-translate";
import {
    Attribute,
    AttributeDescriptor,
    AttributeEvent,
    AttributeRef,
    WellknownMetaItems,
    SharedEvent,
    ValueDescriptor,
    WellknownValueTypes,
    AgentLink,
    Agent,
    AgentDescriptor,
    AssetDescriptor,
    NameHolder,
    ValueHolder,
    ValueDescriptorHolder
} from "@openremote/model";
import manager, {AssetModelUtil, DefaultColor4, subscribe, Util} from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrMwcInput, OrInputChangedEvent, ValueInputProviderOptions, ValueInputProviderGenerator, getValueHolderInputTemplateProvider, ValueInputProvider, OrInputChangedEventDetail, ValueInputTemplateFunction} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-map";
import { geoJsonPointInputTemplateProvider } from "@openremote/or-map";
import "@openremote/or-json-forms";
import {OrJSONForms, StandardRenderers} from "@openremote/or-json-forms";
import "./or-agent-picker";
import {OrAgentPickerChangedEvent, OrAgentPickerLoadedEvent} from "./or-agent-picker";
import { ErrorObject } from "@openremote/or-json-forms";
import {agentIdRendererRegistryEntry} from "./agent-link-json-forms-renderer";

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
            <div id="wrapper" class="${(buttonIcon === undefined || buttonIcon || fullWidth) ? "no-padding" : "right-padding"}">
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

export const jsonFormsInputTemplateProvider: (assetDescriptor: AssetDescriptor | string, valueHolder: NameHolder & ValueHolder<any> | undefined, valueHolderDescriptor: ValueDescriptorHolder | undefined, valueDescriptor: ValueDescriptor, valueChangeNotifier: (value: OrInputChangedEventDetail | undefined) => void, options: ValueInputProviderOptions, fallback: ValueInputProvider) => ValueInputProvider = (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options, fallback) => {

    const disabled = !!(options && options.disabled);
    const readonly = !!(options && options.readonly);
    const label = options.label;

    // Agent link needs some special handling as we need an agent picker no matter what
    if (valueDescriptor.name === WellknownValueTypes.AGENTLINK) {

        const templateFunction: ValueInputTemplateFunction = (value, focused, loading, sending, error, helperText) => {
            const jsonForms: Ref<OrJSONForms> = createRef();
            const editorDiv: Ref<HTMLDivElement> = createRef();
            const errorDiv: Ref<HTMLDivElement> = createRef();
            const agentLink = value as AgentLink;
            const agentPickerLoadedDeferred: Util.Deferred<{agent: Agent, error: boolean}> = new Util.Deferred();
            const schemaEditorPromise: PromiseLike<TemplateResult> = agentPickerLoadedDeferred.promise.then((agentAndError) => {

                const onAgentLinkChanged = (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}) => {
                    valueChangeNotifier({
                        value: dataAndErrors.data
                    });
                };

                const schema = JSON.parse("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"definitions\":{\"ArrayPredicate-1\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\"},\"lengthEquals\":{\"type\":\"integer\"},\"lengthGreaterThan\":{\"type\":\"integer\"},\"lengthLessThan\":{\"type\":\"integer\"},\"negated\":{\"type\":\"boolean\"},\"value\":{}}},\"ArrayPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/ArrayPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"ArrayPredicate\"}}}]},\"BooleanPredicate-1\":{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"boolean\"}}},\"BooleanPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/BooleanPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"BooleanPredicate\"}}}]},\"CalendarEventPredicate-1\":{\"type\":\"object\",\"properties\":{\"timestamp\":{\"type\":\"string\"}}},\"CalendarEventPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/CalendarEventPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"CalendarEventPredicate\"}}}]},\"DateTimePredicate-1\":{\"type\":\"object\",\"properties\":{\"negate\":{\"type\":\"boolean\"},\"operator\":{\"$ref\":\"#/definitions/Operator\"},\"rangeValue\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}},\"DateTimePredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/DateTimePredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"DateTimePredicate\"}}}]},\"JsonPathFilter-1\":{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"returnFirst\":{\"type\":\"boolean\"},\"returnLast\":{\"type\":\"boolean\"}}},\"JsonPathFilter-2\":{\"allOf\":[{\"$ref\":\"#/definitions/JsonPathFilter-1\"},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"jsonPath\"}}}]},\"Map(String,List(String))\":{\"type\":\"object\"},\"NumberPredicate-1\":{\"type\":\"object\",\"properties\":{\"negate\":{\"type\":\"boolean\"},\"operator\":{\"$ref\":\"#/definitions/Operator\"},\"rangeValue\":{\"type\":\"number\"},\"value\":{\"type\":\"number\"}}},\"NumberPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/NumberPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"NumberPredicate\"}}}]},\"ObjectNode\":{\"type\":\"object\"},\"Operator\":{\"type\":\"string\",\"enum\":[\"EQUALS\",\"GREATER_THAN\",\"GREATER_EQUALS\",\"LESS_THAN\",\"LESS_EQUALS\",\"BETWEEN\"]},\"RadialGeofencePredicate-1\":{\"type\":\"object\",\"properties\":{\"lat\":{\"type\":\"number\"},\"lng\":{\"type\":\"number\"},\"negated\":{\"type\":\"boolean\"},\"radius\":{\"type\":\"integer\"}}},\"RadialGeofencePredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/RadialGeofencePredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"RadialGeofencePredicate\"}}}]},\"RectangularGeofencePredicate-1\":{\"type\":\"object\",\"properties\":{\"latMax\":{\"type\":\"number\"},\"latMin\":{\"type\":\"number\"},\"lngMax\":{\"type\":\"number\"},\"lngMin\":{\"type\":\"number\"},\"negated\":{\"type\":\"boolean\"}}},\"RectangularGeofencePredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/RectangularGeofencePredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"RectangularGeofencePredicate\"}}}]},\"RegexValueFilter-1\":{\"type\":\"object\",\"properties\":{\"matchGroup\":{\"type\":\"integer\"},\"matchIndex\":{\"type\":\"integer\"},\"pattern\":{\"type\":\"object\",\"properties\":{\"flags\":{\"type\":\"integer\"},\"pattern\":{\"type\":\"string\"}}}}},\"RegexValueFilter-2\":{\"allOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-1\"},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"regex\"}}}]},\"StringPredicate-1\":{\"type\":\"object\",\"properties\":{\"caseSensitive\":{\"type\":\"boolean\"},\"match\":{\"type\":\"string\",\"enum\":[\"EXACT\",\"BEGIN\",\"END\",\"CONTAINS\"]},\"negate\":{\"type\":\"boolean\"},\"value\":{\"type\":\"string\"}}},\"StringPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/StringPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"StringPredicate\"}}}]},\"SubStringValueFilter-1\":{\"type\":\"object\",\"properties\":{\"beginIndex\":{\"type\":\"integer\"},\"endIndex\":{\"type\":\"integer\"}}},\"SubStringValueFilter-2\":{\"allOf\":[{\"$ref\":\"#/definitions/SubStringValueFilter-1\"},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"substring\"}}}]},\"ValueEmptyPredicate-1\":{\"type\":\"object\"},\"ValueEmptyPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/ValueEmptyPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"ValueEmptyPredicate\"}}}]},\"ValueNotEmptyPredicate-1\":{\"type\":\"object\"},\"ValueNotEmptyPredicate-2\":{\"allOf\":[{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-1\"},{\"type\":\"object\",\"properties\":{\"predicateType\":{\"const\":\"ValueNotEmptyPredicate\"}}}]},\"WebsocketHTTPSubscription-1\":{\"type\":\"object\",\"properties\":{\"body\":{},\"contentType\":{\"type\":\"string\"},\"headers\":{\"$ref\":\"#/definitions/Map(String,List(String))\"},\"method\":{\"type\":\"string\",\"enum\":[\"GET\",\"PUT\",\"POST\"]},\"type\":{\"type\":\"string\"},\"uri\":{\"type\":\"string\"}}},\"WebsocketHTTPSubscription-2\":{\"allOf\":[{\"$ref\":\"#/definitions/WebsocketHTTPSubscription-1\"},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"WebsocketHTTPSubscription\"}}}]},\"WebsocketSubscription\":{\"anyOf\":[{\"$ref\":\"#/definitions/WebsocketSubscription\"},{\"$ref\":\"#/definitions/WebsocketHTTPSubscription-2\"}]}},\"anyOf\":[{\"allOf\":[{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"oid\":{\"type\":\"string\"},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"SNMPAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"deviceEndpoint\":{\"type\":\"integer\"},\"deviceNodeId\":{\"type\":\"integer\"},\"deviceValue\":{\"type\":\"string\"},\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"ZWaveAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"deviceAddress\":{\"type\":\"integer\"},\"deviceValueLink\":{\"type\":\"string\"},\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"VelbusAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"DefaultAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"replayData\":{\"description\":\"Used to store 24h dataset of values that should be replayed (i.e. written to the linked attribute) in a continuous loop.\",\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"timestamp\":{\"type\":\"integer\"},\"value\":{}}}},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"SimulatorAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"websocketSubscriptions\":{\"description\":\"Array of WebsocketSubscriptions that should be executed when the linked attribute is linked; the subscriptions are executed in the order specified in the array.\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/WebsocketSubscription\"},{\"$ref\":\"#/definitions/WebsocketHTTPSubscription-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"WebsocketAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"contentType\":{\"type\":\"string\"},\"headers\":{\"$ref\":\"#/definitions/Map(String,List(String))\"},\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"method\":{\"type\":\"string\",\"enum\":[\"GET\",\"POST\",\"PUT\",\"DELETE\",\"OPTIONS\",\"PATCH\"]},\"pagingMode\":{\"type\":\"boolean\"},\"path\":{\"type\":\"string\"},\"pollingAttribute\":{\"type\":\"string\"},\"pollingMillis\":{\"type\":\"integer\"},\"queryParameters\":{\"$ref\":\"#/definitions/Map(String,List(String))\"},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"HTTPAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"requiredValue\":{\"type\":\"string\"},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"MockAgentLink\"}}}]},{\"allOf\":[{\"type\":\"object\",\"properties\":{\"actionGroupAddress\":{\"type\":\"string\"},\"dpt\":{\"type\":\"string\"},\"id\":{\"type\":\"string\",\"format\":\"or-agent-id\"},\"messageMatchFilters\":{\"description\":\"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"messageMatchPredicate\":{\"anyOf\":[{\"$ref\":\"#/definitions/StringPredicate-2\"},{\"$ref\":\"#/definitions/BooleanPredicate-2\"},{\"$ref\":\"#/definitions/DateTimePredicate-2\"},{\"$ref\":\"#/definitions/NumberPredicate-2\"},{\"$ref\":\"#/definitions/RadialGeofencePredicate-2\"},{\"$ref\":\"#/definitions/RectangularGeofencePredicate-2\"},{\"$ref\":\"#/definitions/ArrayPredicate-2\"},{\"$ref\":\"#/definitions/ValueEmptyPredicate-2\"},{\"$ref\":\"#/definitions/ValueNotEmptyPredicate-2\"},{\"$ref\":\"#/definitions/CalendarEventPredicate-2\"}]},\"statusGroupAddress\":{\"type\":\"string\"},\"valueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute\"}]},\"valueFilters\":{\"description\":\"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order\",\"type\":\"array\",\"items\":{\"anyOf\":[{\"$ref\":\"#/definitions/RegexValueFilter-2\"},{\"$ref\":\"#/definitions/SubStringValueFilter-2\"},{\"$ref\":\"#/definitions/JsonPathFilter-2\"}]}},\"writeValue\":{\"type\":\"string\",\"description\":\"String to be used for attribute writes and can contain '{$value}' placeholders to allow the written value to be injected into the string or to even hardcode the value written to the protocol\"},\"writeValueConverter\":{\"allOf\":[{\"$ref\":\"#/definitions/ObjectNode\"},{\"description\":\"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion\"}]}}},{\"type\":\"object\",\"properties\":{\"type\":{\"const\":\"KNXAgentLink\"}}}]}]}");

                return html`
                    ${agentAndError.error
                        ? html``
                        : html`
                                <div>
                                    <or-json-forms .renderers="${jsonFormsAttributeRenderers}" ${ref(jsonForms)}
                                       .schema="${schema}" .onChange="${onAgentLinkChanged}"
                                       .data="${agentLink}"></or-json-forms>
                                </div>
                        `
                    }
                `
            });

            const onAgentPickerLoaded = (agents: Agent[]) => {
                console.log("Picker loaded");
                if (!agents) {
                    console.warn("Failed to load agent assets for agent picker");
                    return;
                }

                if (!agentLink) {
                    // Just remove disabled class
                    editorDiv.value!.classList.remove("disabled");
                    return;
                }

                const matchedAgent = agents.find(agent => agent.id === agentLink.id);
                let error = false;

                if (!matchedAgent) {
                    console.warn("Linked agent cannot be found: " + agentLink);
                    error = true;
                } else {
                    // Check agent link type
                    const agentDescriptor = AssetModelUtil.getAssetDescriptor(matchedAgent.type) as AgentDescriptor;
                    if (!agentDescriptor) {
                        console.warn("Failed to load agent descriptor for agent link: " + agentLink);
                        error = true;
                    } else if (agentDescriptor.agentLinkType !== agentLink.type) {
                        console.warn("Agent link type does not match agent descriptor agent link type: " + agentLink);
                        error = true;
                    }
                }

                if (matchedAgent) {
                    if (!error) {
                        editorDiv.value!.classList.remove("disabled");
                    }
                    agentPickerLoadedDeferred.resolve({
                        agent: matchedAgent,
                        error: error
                    });
                }
            };

            const onAgentChanged = (agent: Agent) => {
                console.log("AGENT CHANGED! " + (agent ? agent.id : ""));
                let agentLink: AgentLink | undefined;

                if (agent) {
                    const agentDescriptor = AssetModelUtil.getAssetDescriptor(agent.type) as AgentDescriptor;
                    agentLink = {
                        id: agent.id,
                        type: agentDescriptor.agentLinkType!
                    };
                }

                valueChangeNotifier({
                    value: agentLink
                });
            };

            return html`
                <style>
                    .disabled {
                        opacity: 0.5;
                        pointer-events: none;
                    }
                </style>
                <div ${ref(editorDiv)} class="disabled">
                    <div>
                        <or-agent-picker .agentId="${agentLink ? agentLink.id : undefined}" @or-agent-picker-loaded="${(ev: OrAgentPickerLoadedEvent) => onAgentPickerLoaded(ev.detail)}" @or-agent-picker-changed="${(ev: OrAgentPickerChangedEvent) => onAgentChanged(ev.detail)}"></or-agent-picker>
                    </div>
                    ${until(schemaEditorPromise, html``)}
                </div>
            `;
        };

        return {
            templateFunction: templateFunction,
            supportsHelperText: false,
            supportsLabel: false,
            supportsSendButton: false
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
        return css`
            :host {
                display: inline-block;
            }
            
            #wrapper or-mwc-input, #wrapper or-map {
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
                margin-top: 4px;
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

    @property({type: Boolean})
    public comfortable: boolean = false;

    @property({type: Boolean})
    public resizeVertical: boolean = false;

    @property({type: Boolean})
    public fullWidth?: boolean;

    @property()
    protected _attributeEvent?: AttributeEvent;

    @property()
    protected _writeTimeoutHandler?: number;

    @query("#send-btn")
    protected _sendButton!: OrMwcInput;
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
            comfortable: this.comfortable,
            resizeVertical: this.resizeVertical,
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

        // Use json forms with fallback to simple input provider
        const valueChangeHandler = (detail: OrInputChangedEventDetail | undefined) => {
            if (detail && (detail.enterPressed || !this._templateProvider || !this.showButton || !this._templateProvider.supportsSendButton)) {
                this._onInputValueChanged(detail.value);
            } else {
                this._newValue = detail ? detail.value : undefined;
            }
        };

        const standardInputProvider = getValueHolderInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (e) => valueChangeHandler(e), options);
        this._templateProvider = jsonFormsInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor,  (v: any) => this._onInputValueChanged(v), options, standardInputProvider);

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

        if (this._newValue === undefined) {
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
