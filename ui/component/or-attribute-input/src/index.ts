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
                schema = JSON.parse('{"$schema":"http://json-schema.org/draft-07/schema#","title":"Agent Link","oneOf":[{"$ref":"#/definitions/SNMPAgentLink"},{"$ref":"#/definitions/DefaultAgentLink"},{"$ref":"#/definitions/HTTPAgentLink"},{"$ref":"#/definitions/MockAgentLink"},{"$ref":"#/definitions/WebsocketAgentLink"},{"$ref":"#/definitions/ZWaveAgentLink"},{"$ref":"#/definitions/ModbusAgentLink"},{"$ref":"#/definitions/StorageSimulatorAgentLink"},{"$ref":"#/definitions/BluetoothMeshAgentLink"},{"$ref":"#/definitions/MQTTAgentLink"},{"$ref":"#/definitions/KNXAgentLink"},{"$ref":"#/definitions/SimulatorAgentLink"},{"$ref":"#/definitions/MailAgentLink"},{"$ref":"#/definitions/VelbusAgentLink"}],"definitions":{"SNMPAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["SNMPAgentLink"],"default":"SNMPAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"oid":{"type":"string"}},"title":"SNMPAgentLink","required":["type","oid"]},"regex":{"type":"object","additionalProperties":true,"title":"Regex","properties":{"type":{"type":"string","enum":["regex"],"default":"regex"},"pattern":{"type":"string"},"matchGroup":{"type":"integer"},"matchIndex":{"type":"integer"}},"required":["type"]},"substring":{"type":"object","additionalProperties":true,"title":"Substring","properties":{"type":{"type":"string","enum":["substring"],"default":"substring"},"beginIndex":{"type":"integer"},"endIndex":{"type":"integer"}},"required":["type","beginIndex"]},"jsonPath":{"type":"object","additionalProperties":true,"title":"JSON Path","properties":{"type":{"type":"string","enum":["jsonPath"],"default":"jsonPath"},"path":{"type":"string"},"returnFirst":{"type":"boolean"},"returnLast":{"type":"boolean"}},"required":["type","path","returnFirst","returnLast"]},"mathExpression":{"type":"object","additionalProperties":true,"title":"Mathematical Expression","properties":{"type":{"type":"string","enum":["mathExpression"],"default":"mathExpression"},"expression":{"type":"string"}},"required":["type"]},"AnyType":{"type":["null","number","integer","boolean","string","array","object"],"additionalProperties":true,"properties":{}},"StringPredicate":{"type":"object","additionalProperties":true,"description":"Predicate for string values; will match based on configured options.","properties":{"predicateType":{"type":"string","enum":["string"],"default":"string"},"match":{"type":"string","enum":["EXACT","BEGIN","END","CONTAINS"]},"caseSensitive":{"type":"boolean"},"value":{"type":"string"},"negate":{"type":"boolean"}},"title":"string","required":["predicateType","caseSensitive","negate"]},"BooleanPredicate":{"type":"object","additionalProperties":true,"description":"Predicate for boolean values; will evaluate the value as a boolean and match against this predicates value, any value that is not a boolean will not match","properties":{"predicateType":{"type":"string","enum":["boolean"],"default":"boolean"},"value":{"type":"boolean"}},"title":"boolean","required":["predicateType","value"]},"DateTimePredicate":{"type":"object","additionalProperties":true,"description":"Predicate for date time values; provided values should be valid ISO 8601 datetime strings (e.g. yyyy-MM-dd\'T\'HH:mm:ssZ or yyyy-MM-dd\'T\'HH:mm:ss±HH:mm), offset and time are optional, if no offset information is supplied then UTC is assumed.","properties":{"predicateType":{"type":"string","enum":["datetime"],"default":"datetime"},"value":{"type":"string"},"rangeValue":{"type":"string"},"operator":{"type":"string","enum":["EQUALS","GREATER_THAN","GREATER_EQUALS","LESS_THAN","LESS_EQUALS","BETWEEN"]},"negate":{"type":"boolean"}},"title":"datetime","required":["predicateType","negate"]},"NumberPredicate":{"type":"object","additionalProperties":true,"description":"Predicate for number values; will match based on configured options.","properties":{"predicateType":{"type":"string","enum":["number"],"default":"number"},"value":{"type":"number"},"rangeValue":{"type":"number"},"operator":{"type":"string","enum":["EQUALS","GREATER_THAN","GREATER_EQUALS","LESS_THAN","LESS_EQUALS","BETWEEN"]},"negate":{"type":"boolean"}},"title":"number","required":["predicateType","negate"]},"RadialGeofencePredicate":{"type":"object","additionalProperties":true,"description":"Predicate for GEO JSON point values; will return true if the point is within the specified radius of the specified latitude and longitude unless negated.","title":"Radial geofence","properties":{"predicateType":{"type":"string","enum":["radial"],"default":"radial"},"radius":{"type":"integer"},"lat":{"type":"number"},"lng":{"type":"number"},"negated":{"type":"boolean"}},"required":["predicateType","radius","lat","lng","negated"]},"RectangularGeofencePredicate":{"type":"object","additionalProperties":true,"description":"Predicate for GEO JSON point values; will return true if the point is within the specified rectangle specified as latitude and longitude values of two corners unless negated.","title":"Rectangular geofence","properties":{"predicateType":{"type":"string","enum":["rect"],"default":"rect"},"latMin":{"type":"number"},"lngMin":{"type":"number"},"latMax":{"type":"number"},"lngMax":{"type":"number"},"negated":{"type":"boolean"}},"required":["predicateType","latMin","lngMin","latMax","lngMax","negated"]},"ArrayPredicate":{"type":"object","additionalProperties":true,"description":"Predicate for array values; will match based on configured options.","properties":{"predicateType":{"type":"string","enum":["array"],"default":"array"},"value":{"$ref":"#/definitions/AnyType"},"index":{"type":"integer"},"lengthEquals":{"type":"integer"},"lengthGreaterThan":{"type":"integer"},"lengthLessThan":{"type":"integer"},"negated":{"type":"boolean"}},"title":"array","required":["predicateType","negated"]},"ValueAnyPredicate":{"type":"object","additionalProperties":true,"description":"Predicate that matches any value including null.","title":"Any value","properties":{"predicateType":{"type":"string","enum":["value-any"],"default":"value-any"}},"required":["predicateType"]},"ValueEmptyPredicate":{"type":"object","additionalProperties":true,"description":"Predicate that matches any empty/null value; unless negated.","title":"Empty value","properties":{"predicateType":{"type":"string","enum":["value-empty"],"default":"value-empty"},"negate":{"type":"boolean"}},"required":["predicateType","negate"]},"CalendarEventPredicate":{"type":"object","additionalProperties":true,"description":"Predicate for calendar event values; will match based on whether the calendar event is active for the specified time.","title":"Calendar","properties":{"predicateType":{"type":"string","enum":["calendar-event"],"default":"calendar-event"},"timestamp":{"type":"integer","format":"utc-millisec"}},"required":["predicateType"]},"DefaultAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["DefaultAgentLink"],"default":"DefaultAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"}},"title":"DefaultAgentLink","required":["type"]},"HTTPAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["HTTPAgentLink"],"default":"HTTPAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"headers":{"type":"object","additionalProperties":{"type":"array","items":{"type":"string"}},"description":"A JSON object of headers to be added to HTTP request; the key represents the name of the header and for each string value supplied a new header will be added with the key name and specified string value"},"queryParameters":{"type":"object","additionalProperties":{"type":"array","items":{"type":"string"}},"description":"A JSON object of query parameters to be added to HTTP request URL; the key represents the name of the query parameter and for each string value supplied a new query parameter will be added with the key name and specified string value (e.g. \'https://..../?test=1&test=2\')"},"pollingMillis":{"type":"integer","description":"Indicates that this HTTP request is used to update the linked attribute; this value indicates how frequently the HTTP request is made in order to update the linked attribute value"},"pagingMode":{"type":"boolean","description":"Indicates that the HTTP server supports pagination using the standard Link header mechanism"},"path":{"type":"string","description":"The URL path to append to the agents Base URL when making requests for this linked attribute"},"method":{"type":"string","enum":["GET","POST","PUT","DELETE","OPTIONS","PATCH"],"description":"The HTTP method to use when making requests for this linked attribute"},"contentType":{"type":"string","description":"The content type header value to use when making requests for this linked attribute (shortcut alternative to using headers parameter)"},"pollingAttribute":{"type":"string","description":"Allows the polled response to be written to another attribute with the specified name on the same asset as the linked attribute"},"messageConvertBinary":{"type":"boolean","description":"Indicates that the HTTP response is binary and should be converted to binary string representation"},"messageConvertHex":{"type":"boolean","description":"Indicates that the HTTP response is binary and should be converted to hexadecimal string representation"}},"title":"HTTPAgentLink","required":["type","messageConvertBinary","messageConvertHex"]},"MockAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["MockAgentLink"],"default":"MockAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"requiredValue":{"type":"string"}},"title":"MockAgentLink","required":["type"]},"WebsocketAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["WebsocketAgentLink"],"default":"WebsocketAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"websocketSubscriptions":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/websocket"},{"$ref":"#/definitions/http"}]}}},"title":"WebsocketAgentLink","required":["type"]},"websocket":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["websocket"],"default":"websocket"},"body":{"$ref":"#/definitions/AnyType"}},"title":"websocket","required":["type"]},"http":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["http"],"default":"http"},"body":{"$ref":"#/definitions/AnyType"},"method":{"type":"string","enum":["GET","PUT","POST"]},"contentType":{"type":"string"},"headers":{"type":"object","additionalProperties":{"type":"array","items":{"type":"string"}}},"uri":{"type":"string"}},"title":"http","required":["type"]},"ZWaveAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["ZWaveAgentLink"],"default":"ZWaveAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"deviceNodeId":{"type":"integer"},"deviceEndpoint":{"type":"integer"},"deviceValue":{"type":"string"}},"title":"ZWaveAgentLink","required":["type"]},"ModbusAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["ModbusAgentLink"],"default":"ModbusAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"pollingMillis":{"type":"integer","description":"Poll interval in milliseconds"},"readMemoryArea":{"type":"string","enum":["COIL","DISCRETE","HOLDING","INPUT"],"description":"Memory area to read from during read request"},"readValueType":{"type":"string","enum":["BOOL","SINT","USINT","BYTE","INT","UINT","WORD","DINT","UDINT","DWORD","LINT","ULINT","LWORD","REAL","LREAL","CHAR","WCHAR"],"description":"Type to convert the returned data to. As specified by the PLC4X Modbus data types."},"readAddress":{"type":"integer","description":"Zero based address from which the value is read from"},"writeMemoryArea":{"type":"string","enum":["COIL","HOLDING"],"description":"Memory area to write to. \\"HOLDING\\" or \\"COIL\\" allowed."},"writeAddress":{"type":"integer","description":"Zero-based address to which the value sent is written to"},"readRegistersAmount":{"type":"integer","description":"Set amount of registers to read. If left empty or less than 1, will use the default size for the corresponding data-type."}},"title":"ModbusAgentLink","required":["type","pollingMillis","readMemoryArea","readValueType","readAddress"]},"StorageSimulatorAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["StorageSimulatorAgentLink"],"default":"StorageSimulatorAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"}},"title":"StorageSimulatorAgentLink","required":["type"]},"BluetoothMeshAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["BluetoothMeshAgentLink"],"default":"BluetoothMeshAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"appKeyIndex":{"type":"integer","minimum":0,"maximum":2147483647},"address":{"type":"string","pattern":"^([0-9A-Fa-f]{4})$"},"modelName":{"type":"string","pattern":"^.*\\\\S+.*$","minLength":1}},"title":"BluetoothMeshAgentLink","required":["type","appKeyIndex","modelName"]},"MQTTAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["MQTTAgentLink"],"default":"MQTTAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"subscriptionTopic":{"type":"string","description":"An MQTT topic to subscribe to, any received payload will be pushed into the attribute; use value filter(s) to extract values from string payloads and/or value converters to do simple value mapping, complex processing may require a rule or a custom MQTT agent"},"publishTopic":{"type":"string","description":"An MQTT topic to publish attribute events to, any received payload will be pushed into the attribute; use write value converter and/or write value to do any processing, complex processing may require a rule or a custom MQTT agent"},"qos":{"type":"integer","minimum":0,"maximum":2,"description":"QoS level to use for publish/subscribe (default is 0 if unset)"}},"title":"MQTTAgentLink","required":["type"]},"KNXAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["KNXAgentLink"],"default":"KNXAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"dpt":{"type":"string","pattern":"^\\\\d{1,3}\\\\.\\\\d{1,3}$"},"actionGroupAddress":{"type":"string","pattern":"^\\\\d{1,3}/\\\\d{1,3}/\\\\d{1,3}$"},"statusGroupAddress":{"type":"string","pattern":"^\\\\d{1,3}/\\\\d{1,3}/\\\\d{1,3}$"}},"title":"KNXAgentLink","required":["type","dpt"]},"SimulatorAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["SimulatorAgentLink"],"default":"SimulatorAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"replayData":{"type":"array","items":{"$ref":"#/definitions/SimulatorReplayDatapoint"},"description":"Used to store 24h dataset of values that should be replayed (i.e. written to the linked attribute) in a continuous loop."}},"title":"SimulatorAgentLink","required":["type"]},"SimulatorReplayDatapoint":{"type":"object","additionalProperties":true,"title":"Data point","properties":{"timestamp":{"type":"integer"},"value":{"$ref":"#/definitions/AnyType"}},"required":["timestamp"]},"MailAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["MailAgentLink"],"default":"MailAgentLink"},"id":{"type":"string","format":"or-agent-id"},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"},"subjectMatchPredicate":{"$ref":"#/definitions/StringPredicate","description":"The predicate to apply to incoming mail message subjects to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent."},"fromMatchPredicate":{"$ref":"#/definitions/StringPredicate","description":"The predicate to apply to incoming mail message from address(es) to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent."},"useSubject":{"type":"boolean","description":"Use the subject as value instead of the body"}},"title":"MailAgentLink","required":["type"]},"VelbusAgentLink":{"type":"object","additionalProperties":true,"properties":{"type":{"type":"string","enum":["VelbusAgentLink"],"default":"VelbusAgentLink"},"id":{"type":"string","format":"or-agent-id"},"deviceAddress":{"type":"integer","minimum":1,"maximum":255},"deviceValueLink":{"type":"string","pattern":"^.*\\\\S+.*$","minLength":1},"valueFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order"},"valueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValueConverter":{"type":"object","patternProperties":{".+":{"type":["null","number","integer","boolean","string","array","object"]}}},"writeValue":{"type":"string","format":"or-multiline","description":"String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)"},"messageMatchPredicate":{"oneOf":[{"$ref":"#/definitions/StringPredicate"},{"$ref":"#/definitions/BooleanPredicate"},{"$ref":"#/definitions/DateTimePredicate"},{"$ref":"#/definitions/NumberPredicate"},{"$ref":"#/definitions/RadialGeofencePredicate"},{"$ref":"#/definitions/RectangularGeofencePredicate"},{"$ref":"#/definitions/ArrayPredicate"},{"$ref":"#/definitions/ValueAnyPredicate"},{"$ref":"#/definitions/ValueEmptyPredicate"},{"$ref":"#/definitions/CalendarEventPredicate"}],"description":"The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent."},"messageMatchFilters":{"type":"array","items":{"oneOf":[{"$ref":"#/definitions/regex"},{"$ref":"#/definitions/substring"},{"$ref":"#/definitions/jsonPath"},{"$ref":"#/definitions/mathExpression"}]},"description":"ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate"},"updateOnWrite":{"type":"boolean","description":"Don\'t expect a response from the protocol just update the attribute immediately on write"}},"title":"VelbusAgentLink","required":["type","deviceAddress","deviceValueLink"]}}}');
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
