import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {ifDefined} from "lit/directives/if-defined.js";
import {until} from "lit/directives/until.js";
import {createRef, Ref, ref} from 'lit/directives/ref.js';
import {i18next, translate} from "@openremote/or-translate";
import {
    Agent,
    AgentDescriptor,
    AgentLink,
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
import {progressCircular} from "@openremote/or-mwc-components/style";
import "@openremote/or-components/or-loading-wrapper";
import {OrLoadingWrapper} from "@openremote/or-components/or-loading-wrapper";
import {
    getValueHolderInputTemplateProvider,
    InputType,
    OrInputChangedEvent,
    OrInputChangedEventDetail,
    OrMwcInput,
    ValueInputProvider,
    ValueInputProviderGenerator,
    ValueInputProviderOptions,
    ValueInputTemplateFunction
} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-map";
import {geoJsonPointInputTemplateProvider} from "@openremote/or-map";
import "@openremote/or-json-forms";
import {ErrorObject, OrJSONForms, StandardRenderers} from "@openremote/or-json-forms";
import {agentIdRendererRegistryEntry, loadAgents} from "./agent-link-json-forms-renderer";

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
type ErrorMessage = "agentNotFound" | "agentTypeMismatch";

export const jsonFormsInputTemplateProvider: (fallback: ValueInputProvider) => ValueInputProviderGenerator = (fallback) => (assetDescriptor, valueHolder, valueHolderDescriptor, valueDescriptor, valueChangeNotifier, options) => {

    const disabled = !!(options && options.disabled);
    const readonly = !!(options && options.readonly);
    const label = options.label;

    // Agent link needs some special handling as we need an agent picker no matter what
    if (valueDescriptor.name === WellknownValueTypes.AGENTLINK) {

        // Apply a custom UI schema to remove the outer VerticalLayout
        const uiSchema: any = {type: "Control", scope: "#"};
        let schema: any;
        const jsonForms: Ref<OrJSONForms> = createRef();
        const loadingWrapper: Ref<OrLoadingWrapper> = createRef();
        let loadedAgents: Agent[] = [];
        let initialised = false;
        let agentLink: AgentLink | undefined;

        const onAgentLinkChanged = (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}) => {
            if (!initialised) {
                return;
            }

            const newAgentLink: AgentLink | undefined = dataAndErrors.data;

            if (newAgentLink) {
                const agent = loadedAgents.find((agnt) => agnt.id === newAgentLink!.id);
                if (agent) {
                    const newAgentDescriptor = AssetModelUtil.getAssetDescriptor(agent.type) as AgentDescriptor;
                    if (newAgentDescriptor) {
                        newAgentLink.type = newAgentDescriptor.agentLinkType!;
                    }
                }
            }

            if (!Util.objectsEqual(newAgentLink, agentLink)) {
                agentLink = newAgentLink;
                valueChangeNotifier({
                    value: newAgentLink
                });
            }
        };


        const doLoad = async (link: AgentLink) => {

            if (!initialised) {
                agentLink = link;
            }
            initialised = true;

            if (!schema) {
                // TODO: Load agent link schema from AssetModelUtil - For now run AssetModelTest in debug and copy paste the result of def schema = ValueUtil.getSchema(AgentLink.class)
                schema = JSON.parse('{"$schema":"http://json-schema.org/draft-07/schema#","definitions":{"ArrayPredicate":{"type":"object","properties":{"index":{"type":"integer"},"lengthEquals":{"type":"integer"},"lengthGreaterThan":{"type":"integer"},"lengthLessThan":{"type":"integer"},"negated":{"type":"boolean"},"value":{"title":"Any Type","type":["null","number","integer","boolean","string","array","object"],"additionalProperties":true},"type":{"const":"array","default":"array"}},"required":["negated","type"],"title":"Array Predicate","additionalProperties":true,"description":"Predicate for array values; will match based on configured options.","i18n":"org.openremote.model.query.filter.ArrayPredicate"},"BluetoothMeshAgentLink":{"type":"object","properties":{"address":{"type":"string","pattern":"^([0-9A-Fa-f]{4})$"},"appKeyIndex":{"type":"integer","minimum":0,"maximum":2147483647},"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"modelName":{"type":"string","minLength":1},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"BluetoothMeshAgentLink","default":"BluetoothMeshAgentLink"}},"required":["appKeyIndex","modelName","type"],"title":"Bluetooth Mesh Agent Link","additionalProperties":true},"BooleanPredicate":{"type":"object","properties":{"value":{"type":"boolean"},"type":{"const":"boolean","default":"boolean"}},"required":["value","type"],"title":"Boolean Predicate","additionalProperties":true,"description":"Predicate for boolean values; will evaluate the value as a boolean and match against this predicates value, any value that is not a boolean will not match","i18n":"org.openremote.model.query.filter.BooleanPredicate"},"CalendarEventPredicate":{"type":"object","properties":{"timestamp":{"type":"integer","format":"utc-millisec"},"type":{"const":"calendar-event","default":"calendar-event"}},"title":"Calendar Event Predicate","additionalProperties":true,"description":"Predicate for calendar event values; will match based on whether the calendar event is active for the specified time.","required":["type"],"label":"Calendar","i18n":"org.openremote.model.query.filter.CalendarEventPredicate"},"DateTimePredicate":{"type":"object","properties":{"negate":{"type":"boolean"},"operator":{"type":"string","enum":["EQUALS","GREATER_THAN","GREATER_EQUALS","LESS_THAN","LESS_EQUALS","BETWEEN"]},"rangeValue":{"type":"string"},"value":{"type":"string"},"type":{"const":"datetime","default":"datetime"}},"required":["negate","type"],"title":"Date Time Predicate","additionalProperties":true,"description":"Predicate for date time values; provided values should be valid ISO 8601 datetime strings (e.g. yyyy-MM-dd\'T\'HH:mm:ssZ or yyyy-MM-dd\'T\'HH:mm:ss±HH:mm), offset and time are optional, if no offset information is supplied then UTC is assumed.","i18n":"org.openremote.model.query.filter.DateTimePredicate"},"DefaultAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"DefaultAgentLink","default":"DefaultAgentLink"}},"title":"Default Agent Link","additionalProperties":true},"HTTPAgentLink":{"type":"object","properties":{"contentType":{"type":"string","description":"The content type header value to use when making requests for this linked attribute (shortcut alternative to using headers parameter)"},"headers":{"type":"object","additionalProperties":{"type":"array","items":{"type":"string"}},"description":"A JSON object of headers to be added to HTTP request; the key represents the name of the header and for each string value supplied a new header will be added with the key name and specified string value"},"id":{"type":"string","format":"or-agent-id"},"messageConvertBinary":{"type":"boolean","description":"Indicates that the HTTP response is binary and should be converted to binary string representation"},"messageConvertHex":{"type":"boolean","description":"Indicates that the HTTP response is binary and should be converted to hexadecimal string representation"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"method":{"type":"string","enum":["GET","POST","PUT","DELETE","OPTIONS","PATCH"],"description":"The HTTP method to use when making requests for this linked attribute"},"pagingMode":{"type":"boolean","description":"Indicates that the HTTP server supports pagination using the standard Link header mechanism"},"path":{"type":"string","description":"The URL path to append to the agents Base URL when making requests for this linked attribute"},"pollingAttribute":{"type":"string","description":"Allows the polled response to be written to another attribute with the specified name on the same asset as the linked attribute"},"pollingMillis":{"type":"integer","description":"Indicates that this HTTP request is used to update the linked attribute; this value indicates how frequently the HTTP request is made in order to update the linked attribute value"},"queryParameters":{"type":"object","additionalProperties":{"type":"array","items":{"type":"string"}},"description":"A JSON object of query parameters to be added to HTTP request URL; the key represents the name of the query parameter and for each string value supplied a new query parameter will be added with the key name and specified string value (e.g. \'https://..../?test=1&test=2\')"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"HTTPAgentLink","default":"HTTPAgentLink"}},"required":["messageConvertBinary","messageConvertHex","type"],"title":"HTTP Agent Link","additionalProperties":true},"JsonPathFilter":{"type":"object","properties":{"path":{"type":"string"},"returnFirst":{"type":"boolean"},"returnLast":{"type":"boolean"},"type":{"const":"jsonPath","default":"jsonPath"}},"required":["path","returnFirst","returnLast","type"],"title":"Json Path Filter","additionalProperties":true,"label":"JSON Path","i18n":"org.openremote.model.value.JsonPathFilter"},"KNXAgentLink":{"type":"object","properties":{"actionGroupAddress":{"type":"string","pattern":"^\\\\d{1,3}/\\\\d{1,3}/\\\\d{1,3}$"},"dpt":{"type":"string","pattern":"^\\\\d{1,3}\\\\.\\\\d{1,3}$"},"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"statusGroupAddress":{"type":"string","pattern":"^\\\\d{1,3}/\\\\d{1,3}/\\\\d{1,3}$"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"KNXAgentLink","default":"KNXAgentLink"}},"required":["dpt","type"],"title":"KNX Agent Link","additionalProperties":true},"MQTTAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"publishTopic":{"type":"string","description":"An MQTT topic to publish attribute events to, any received payload will be pushed into the attribute; use write value converter and/or write value to do any processing, complex processing may require a rule or a custom MQTT agent"},"qos":{"type":"integer","description":"QoS level to use for publish/subscribe (default is 0 if unset)","minimum":0,"maximum":2},"subscriptionTopic":{"type":"string","description":"An MQTT topic to subscribe to, any received payload will be pushed into the attribute; use value filter(s) to extract values from string payloads and/or value converters to do simple value mapping, complex processing may require a rule or a custom MQTT agent"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"MQTTAgentLink","default":"MQTTAgentLink"}},"title":"MQTT Agent Link","additionalProperties":true,"required":["type"]},"MailAgentLink":{"type":"object","properties":{"fromMatchPredicate":{"type":"object","properties":{"caseSensitive":{"type":"boolean"},"match":{"type":"string","enum":["EXACT","BEGIN","END","CONTAINS"]},"negate":{"type":"boolean"},"value":{"type":"string"},"type":{"const":"string","default":"string"}},"required":["caseSensitive","negate","type"],"title":"String Predicate","additionalProperties":true,"description":"The predicate to apply to incoming mail message from address(es) to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.query.filter.StringPredicate"},"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"subjectMatchPredicate":{"type":"object","properties":{"caseSensitive":{"type":"boolean"},"match":{"type":"string","enum":["EXACT","BEGIN","END","CONTAINS"]},"negate":{"type":"boolean"},"value":{"type":"string"},"type":{"const":"string","default":"string"}},"required":["caseSensitive","negate","type"],"title":"String Predicate","additionalProperties":true,"description":"The predicate to apply to incoming mail message subjects to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.query.filter.StringPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"useSubject":{"type":"boolean","description":"Use the subject as value instead of the body"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"MailAgentLink","default":"MailAgentLink"}},"title":"Mail Agent Link","additionalProperties":true,"required":["type"]},"MathExpressionValueFilter":{"type":"object","properties":{"expression":{"type":"string"},"type":{"const":"mathExpression","default":"mathExpression"}},"title":"Math Expression Value Filter","additionalProperties":true,"required":["type"],"label":"Mathematical Expression","i18n":"org.openremote.model.value.MathExpressionValueFilter"},"ModbusAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"pollingMillis":{"type":"integer","description":"Poll interval in milliseconds"},"readAddress":{"type":"integer","description":"Zero based address from which the value is read from"},"readMemoryArea":{"type":"string","enum":["COIL","DISCRETE","HOLDING","INPUT"],"description":"Memory area to read from during read request"},"readRegistersAmount":{"type":"integer","description":"Set amount of registers to read. If left empty or less than 1, will use the default size for the corresponding data-type."},"readValueType":{"type":"string","enum":["BOOL","SINT","USINT","BYTE","INT","UINT","WORD","DINT","UDINT","DWORD","LINT","ULINT","LWORD","REAL","LREAL","CHAR","WCHAR"],"description":"Type to convert the returned data to. As specified by the PLC4X Modbus data types."},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeAddress":{"type":"integer","description":"Zero-based address to which the value sent is written to"},"writeMemoryArea":{"type":"string","enum":["COIL","HOLDING"],"description":"Memory area to write to. \\"HOLDING\\" or \\"COIL\\" allowed."},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"ModbusAgentLink","default":"ModbusAgentLink"}},"required":["pollingMillis","readAddress","readMemoryArea","readValueType","type"],"title":"Modbus Agent Link","additionalProperties":true},"NumberPredicate":{"type":"object","properties":{"negate":{"type":"boolean"},"operator":{"type":"string","enum":["EQUALS","GREATER_THAN","GREATER_EQUALS","LESS_THAN","LESS_EQUALS","BETWEEN"]},"rangeValue":{"type":"number"},"value":{"type":"number"},"type":{"const":"number","default":"number"}},"required":["negate","type"],"title":"Number Predicate","additionalProperties":true,"description":"Predicate for number values; will match based on configured options.","i18n":"org.openremote.model.query.filter.NumberPredicate"},"RadialGeofencePredicate":{"type":"object","properties":{"lat":{"type":"number"},"lng":{"type":"number"},"negated":{"type":"boolean"},"radius":{"type":"integer"},"type":{"const":"radial","default":"radial"}},"required":["lat","lng","negated","radius","type"],"title":"Radial Geofence Predicate","additionalProperties":true,"description":"Predicate for GEO JSON point values; will return true if the point is within the specified radius of the specified latitude and longitude unless negated.","label":"Radial geofence","i18n":"org.openremote.model.query.filter.RadialGeofencePredicate"},"RectangularGeofencePredicate":{"type":"object","properties":{"latMax":{"type":"number"},"latMin":{"type":"number"},"lngMax":{"type":"number"},"lngMin":{"type":"number"},"negated":{"type":"boolean"},"type":{"const":"rect","default":"rect"}},"required":["latMax","latMin","lngMax","lngMin","negated","type"],"title":"Rectangular Geofence Predicate","additionalProperties":true,"description":"Predicate for GEO JSON point values; will return true if the point is within the specified rectangle specified as latitude and longitude values of two corners unless negated.","label":"Rectangular geofence","i18n":"org.openremote.model.query.filter.RectangularGeofencePredicate"},"RegexValueFilter":{"type":"object","properties":{"matchGroup":{"type":"integer"},"matchIndex":{"type":"integer"},"pattern":{"type":"string"},"type":{"const":"regex","default":"regex"}},"title":"Regex Value Filter","additionalProperties":true,"required":["type"],"label":"Regex","i18n":"org.openremote.model.value.RegexValueFilter"},"SNMPAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"SNMPAgentLink","default":"SNMPAgentLink"}},"title":"SNMP Agent Link","additionalProperties":true,"required":["type"]},"SimulatorAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"replayData":{"type":"array","items":{"type":"object","properties":{"timestamp":{"type":"integer"},"value":{"title":"Any Type","type":["null","number","integer","boolean","string","array","object"],"additionalProperties":true}},"required":["timestamp"],"title":"Simulator Replay Datapoint","additionalProperties":true,"label":"Data point","i18n":"org.openremote.model.simulator.SimulatorReplayDatapoint"},"description":"Used to store a dataset of values that should be replayed (i.e. written to the linked attribute) in a continuous loop based on a schedule (by default replays every 24h). Predicted datapoints can be added by configuring \'Store predicted datapoints\' which will insert the datapoints immediately as determined by the schedule."},"schedule":{"type":"object","properties":{"end":{"type":"string","format":"date-time","description":"Not implemented, within the recurrence rule you can specify an end date."},"recurrence":{"type":"string","description":"The recurrence schedule follows the RFC 5545 RRULE format."},"start":{"type":"string","format":"date-time","description":"Set a start date, if not provided, starts immediately. When the replay datapoint timestamp is 0 it will insert it at 00:00."}},"title":"Schedule","additionalProperties":true,"description":"When defined overwrites the possible dataset length and when it is replayed. This could be once when only a start- (and end) date are defined, or a recurring event following the RFC 5545 RRULE format. If not provided defaults to 24 hours. If the replay data contains datapoints scheduled after the default 24 hours or the recurrence rule the datapoints will be ignored."},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"SimulatorAgentLink","default":"SimulatorAgentLink"}},"title":"Simulator Agent Link","additionalProperties":true,"required":["type"]},"StorageSimulatorAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"StorageSimulatorAgentLink","default":"StorageSimulatorAgentLink"}},"title":"Storage Simulator Agent Link","additionalProperties":true,"required":["type"]},"StringPredicate":{"type":"object","properties":{"caseSensitive":{"type":"boolean"},"match":{"type":"string","enum":["EXACT","BEGIN","END","CONTAINS"]},"negate":{"type":"boolean"},"value":{"type":"string"},"type":{"const":"string","default":"string"}},"required":["caseSensitive","negate","type"],"title":"String Predicate","additionalProperties":true,"description":"Predicate for string values; will match based on configured options.","i18n":"org.openremote.model.query.filter.StringPredicate"},"SubStringValueFilter":{"type":"object","properties":{"beginIndex":{"type":"integer"},"endIndex":{"type":"integer"},"type":{"const":"substring","default":"substring"}},"required":["beginIndex","type"],"title":"Sub String Value Filter","additionalProperties":true,"label":"Substring","i18n":"org.openremote.model.value.SubStringValueFilter"},"ValueAnyPredicate":{"type":"object","title":"Value Any Predicate","additionalProperties":true,"description":"Predicate that matches any value including null.","properties":{"type":{"const":"value-any","default":"value-any"}},"required":["type"],"label":"Any value","i18n":"org.openremote.model.query.filter.ValueAnyPredicate"},"ValueEmptyPredicate":{"type":"object","properties":{"negate":{"type":"boolean"},"type":{"const":"value-empty","default":"value-empty"}},"required":["negate","type"],"title":"Value Empty Predicate","additionalProperties":true,"description":"Predicate that matches any empty/null value; unless negated.","label":"Empty value","i18n":"org.openremote.model.query.filter.ValueEmptyPredicate"},"VelbusAgentLink":{"type":"object","properties":{"deviceAddress":{"type":"integer","minimum":1,"maximum":255},"deviceValueLink":{"type":"string","minLength":1},"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"VelbusAgentLink","default":"VelbusAgentLink"}},"required":["deviceAddress","deviceValueLink","type"],"title":"Velbus Agent Link","additionalProperties":true},"WebsocketAgentLink":{"type":"object","properties":{"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"websocketSubscriptions":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/WebsocketSubscriptionImpl"},{"$ref":"#/definitions/WebsocketHTTPSubscription"}]},"description":"Array of WebsocketSubscriptions that should be executed when the linked attribute is linked; the subscriptions are executed in the order specified in the array."},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"WebsocketAgentLink","default":"WebsocketAgentLink"}},"title":"Websocket Agent Link","additionalProperties":true,"required":["type"]},"WebsocketHTTPSubscription":{"type":"object","properties":{"body":{"title":"Any Type","type":["null","number","integer","boolean","string","array","object"],"additionalProperties":true},"contentType":{"type":"string"},"headers":{"type":"object","additionalProperties":{"type":"array","items":{"type":"string"}}},"method":{"type":"string","enum":["GET","PUT","POST"]},"uri":{"type":"string"},"type":{"const":"http","default":"http"}},"title":"Websocket HTTP Subscription","additionalProperties":true,"required":["type"]},"WebsocketSubscriptionImpl":{"type":"object","properties":{"body":{"title":"Any Type","type":["null","number","integer","boolean","string","array","object"],"additionalProperties":true},"type":{"const":"websocket","default":"websocket"}},"title":"Websocket Subscription Impl","additionalProperties":true},"ZWaveAgentLink":{"type":"object","properties":{"deviceEndpoint":{"type":"integer"},"deviceNodeId":{"type":"integer"},"deviceValue":{"type":"string"},"id":{"type":"string","format":"or-agent-id"},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchFilters"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.","i18n":"org.openremote.model.asset.agent.AgentLink.messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write","i18n":"org.openremote.model.asset.agent.AgentLink.updateOnWrite"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns \'ACTIVE\'/\'DISABLED\' strings but you want to connect this to a Boolean attribute","i18n":"org.openremote.model.asset.agent.AgentLink.valueConverter"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/RegexValueFilter"},{"$ref":"#/definitions/SubStringValueFilter"},{"$ref":"#/definitions/JsonPathFilter"},{"$ref":"#/definitions/MathExpressionValueFilter"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order","i18n":"org.openremote.model.asset.agent.AgentLink.valueFilters"},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dynamic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)","i18n":"org.openremote.model.asset.agent.AgentLink.writeValue"},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}},"description":"Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion","i18n":"org.openremote.model.asset.agent.AgentLink.writeValueConverter"},"type":{"const":"ZWaveAgentLink","default":"ZWaveAgentLink"}},"title":"Z Wave Agent Link","additionalProperties":true,"required":["type"]}},"title":"Agent Link","oneOf":[{"$ref":"#/definitions/VelbusAgentLink"},{"$ref":"#/definitions/WebsocketAgentLink"},{"$ref":"#/definitions/DefaultAgentLink"},{"$ref":"#/definitions/MailAgentLink"},{"$ref":"#/definitions/HTTPAgentLink"},{"$ref":"#/definitions/StorageSimulatorAgentLink"},{"$ref":"#/definitions/BluetoothMeshAgentLink"},{"$ref":"#/definitions/ModbusAgentLink"},{"$ref":"#/definitions/KNXAgentLink"},{"$ref":"#/definitions/MQTTAgentLink"},{"$ref":"#/definitions/SNMPAgentLink"},{"$ref":"#/definitions/ZWaveAgentLink"},{"$ref":"#/definitions/SimulatorAgentLink"}],"type":"object","properties":{"type":{"const":"AgentLink"}},"required":["type"]}');
            }

            loadedAgents = await loadAgents();

            if (!loadedAgents) {
                console.warn("Failed to load agents for agent link");
                return;
            }

            if (link) {
                if (link.id) {
                    const matchedAgent = loadedAgents.find(agent => agent.id === link.id);
                    if (!matchedAgent) {
                        console.warn("Agent link: linked agent could not be found");
                        link.id = "";
                    }
                } else {
                    link.id = "";
                }
            }

            // let error: ErrorMessage | undefined;
            //
            // if (!matchedAgent) {
            //     console.warn("Linked agent cannot be found: " + agentLink);
            //     error = "agentNotFound";
            // } else {
            //     // Check agent link type
            //     const agentDescriptor = AssetModelUtil.getAssetDescriptor(matchedAgent.type) as AgentDescriptor;
            //     if (!agentDescriptor) {
            //         console.warn("Failed to load agent descriptor for agent link: " + agentLink);
            //         error = "agentNotFound";
            //     } else if (agentDescriptor.agentLinkType !== agentLink.type) {
            //         console.warn("Agent link type does not match agent descriptor agent link type: " + agentLink);
            //         error = "agentTypeMismatch";
            //     }
            // }

            if (jsonForms.value && loadingWrapper.value) {
                const forms = jsonForms.value;
                forms.schema = schema;
                forms.data = link;
                loadingWrapper.value.loading = false;
            }
        };

        const templateFunction: ValueInputTemplateFunction = (value, focused, loading, sending, error, helperText) => {

            // Need to have a value so that the agent ID picker is shown
            if (!value) {
                value = {
                    id: "",
                    type: "DefaultAgentLink"
                };
            }

            // Schedule loading
            window.setTimeout(() => doLoad(value as AgentLink), 0);

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
                                   .schema="${schema}" label="Agent link" .uischema="${uiSchema}" .onChange="${onAgentLinkChanged}"></or-json-forms>
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
            
            #wrapper or-mwc-input, #wrapper or-map {
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
        const valueChangeHandler = (detail: OrInputChangedEventDetail | undefined) => {
            const value = detail ? detail.value : undefined;
            const updateImmediately = (detail && detail.enterPressed) || !this._templateProvider || !this.showButton || !this._templateProvider.supportsSendButton;
            this._onInputValueChanged(value, updateImmediately);
        };

        if (this.customProvider) {
            this._templateProvider = this.customProvider ? this.customProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail), options) : undefined;
            return;
        }

        // Handle special value types
        if (valueDescriptor.name === WellknownValueTypes.GEOJSONPOINT) {
            this._templateProvider = geoJsonPointInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail), options);
            return;
        }

        const standardInputProvider = getValueHolderInputTemplateProvider(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail), options);
        this._templateProvider = jsonFormsInputTemplateProvider(standardInputProvider)(this.assetType, this.attribute, this._attributeDescriptor, valueDescriptor, (detail) => valueChangeHandler(detail), options);

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
