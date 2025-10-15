export default {
    $schema: "http://json-schema.org/draft-07/schema#",
    definitions: {
        ArrayPredicate: {
            type: "object",
            properties: {
                index: { type: "integer" },
                lengthEquals: { type: "integer" },
                lengthGreaterThan: { type: "integer" },
                lengthLessThan: { type: "integer" },
                negated: { type: "boolean" },
                value: {
                    title: "Any Type",
                    type: ["null", "number", "integer", "boolean", "string", "array", "object"],
                    additionalProperties: true,
                },
                type: { const: "array", default: "array" },
            },
            required: ["negated", "type"],
            title: "Array Predicate",
            additionalProperties: true,
            description: "Predicate for array values; will match based on configured options.",
        },
        BluetoothMeshAgentLink: {
            type: "object",
            properties: {
                address: { type: "string", pattern: "^([0-9A-Fa-f]{4})$" },
                appKeyIndex: { type: "integer", minimum: 0, maximum: 2147483647 },
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                modelName: { type: "string", minLength: 1 },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "BluetoothMeshAgentLink", default: "BluetoothMeshAgentLink" },
            },
            required: ["appKeyIndex", "modelName", "type"],
            title: "Bluetooth Mesh Agent Link",
            additionalProperties: true,
        },
        BooleanPredicate: {
            type: "object",
            properties: { value: { type: "boolean" }, type: { const: "boolean", default: "boolean" } },
            required: ["value", "type"],
            title: "Boolean Predicate",
            additionalProperties: true,
            description:
                "Predicate for boolean values; will evaluate the value as a boolean and match against this predicates value, any value that is not a boolean will not match",
        },
        CalendarEventPredicate: {
            type: "object",
            properties: {
                timestamp: { type: "integer", format: "utc-millisec" },
                type: { const: "calendar-event", default: "calendar-event" },
            },
            title: "Calendar",
            additionalProperties: true,
            description:
                "Predicate for calendar event values; will match based on whether the calendar event is active for the specified time.",
            required: ["type"],
        },
        DateTimePredicate: {
            type: "object",
            properties: {
                negate: { type: "boolean" },
                operator: {
                    type: "string",
                    enum: ["EQUALS", "GREATER_THAN", "GREATER_EQUALS", "LESS_THAN", "LESS_EQUALS", "BETWEEN"],
                },
                rangeValue: { type: "string" },
                value: { type: "string" },
                type: { const: "datetime", default: "datetime" },
            },
            required: ["negate", "type"],
            title: "Date Time Predicate",
            additionalProperties: true,
            description:
                "Predicate for date time values; provided values should be valid ISO 8601 datetime strings (e.g. yyyy-MM-dd'T'HH:mm:ssZ or yyyy-MM-dd'T'HH:mm:ss±HH:mm), offset and time are optional, if no offset information is supplied then UTC is assumed.",
        },
        DefaultAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "DefaultAgentLink", default: "DefaultAgentLink" },
            },
            title: "Default Agent Link",
            additionalProperties: true,
        },
        HTTPAgentLink: {
            type: "object",
            properties: {
                contentType: {
                    type: "string",
                    description:
                        "The content type header value to use when making requests for this linked attribute (shortcut alternative to using headers parameter)",
                },
                headers: {
                    type: "object",
                    additionalProperties: { type: "array", items: { type: "string" } },
                    description:
                        "A JSON object of headers to be added to HTTP request; the key represents the name of the header and for each string value supplied a new header will be added with the key name and specified string value",
                },
                id: { type: "string", format: "or-agent-id" },
                messageConvertBinary: {
                    type: "boolean",
                    description:
                        "Indicates that the HTTP response is binary and should be converted to binary string representation",
                },
                messageConvertHex: {
                    type: "boolean",
                    description:
                        "Indicates that the HTTP response is binary and should be converted to hexadecimal string representation",
                },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                method: {
                    type: "string",
                    enum: ["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"],
                    description: "The HTTP method to use when making requests for this linked attribute",
                },
                pagingMode: {
                    type: "boolean",
                    description:
                        "Indicates that the HTTP server supports pagination using the standard Link header mechanism",
                },
                path: {
                    type: "string",
                    description:
                        "The URL path to append to the agents Base URL when making requests for this linked attribute",
                },
                pollingAttribute: {
                    type: "string",
                    description:
                        "Allows the polled response to be written to another attribute with the specified name on the same asset as the linked attribute",
                },
                pollingMillis: {
                    type: "integer",
                    description:
                        "Indicates that this HTTP request is used to update the linked attribute; this value indicates how frequently the HTTP request is made in order to update the linked attribute value",
                },
                queryParameters: {
                    type: "object",
                    additionalProperties: { type: "array", items: { type: "string" } },
                    description:
                        "A JSON object of query parameters to be added to HTTP request URL; the key represents the name of the query parameter and for each string value supplied a new query parameter will be added with the key name and specified string value (e.g. 'https://..../?test=1&test=2')",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "HTTPAgentLink", default: "HTTPAgentLink" },
            },
            required: ["messageConvertBinary", "messageConvertHex", "type"],
            title: "HTTP Agent Link",
            additionalProperties: true,
        },
        JsonPathFilter: {
            type: "object",
            properties: {
                path: { type: "string" },
                returnFirst: { type: "boolean" },
                returnLast: { type: "boolean" },
                type: { const: "jsonPath", default: "jsonPath" },
            },
            required: ["path", "returnFirst", "returnLast", "type"],
            title: "JSON Path",
            additionalProperties: true,
        },
        KNXAgentLink: {
            type: "object",
            properties: {
                actionGroupAddress: { type: "string", pattern: "^\\d{1,3}/\\d{1,3}/\\d{1,3}$" },
                dpt: { type: "string", pattern: "^\\d{1,3}\\.\\d{1,3}$" },
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                statusGroupAddress: { type: "string", pattern: "^\\d{1,3}/\\d{1,3}/\\d{1,3}$" },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "KNXAgentLink", default: "KNXAgentLink" },
            },
            required: ["dpt", "type"],
            title: "KNX Agent Link",
            additionalProperties: true,
        },
        MQTTAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                publishTopic: {
                    type: "string",
                    description:
                        "An MQTT topic to publish attribute events to, any received payload will be pushed into the attribute; use write value converter and/or write value to do any processing, complex processing may require a rule or a custom MQTT agent",
                },
                qos: {
                    type: "integer",
                    description: "QoS level to use for publish/subscribe (default is 0 if unset)",
                    minimum: 0,
                    maximum: 2,
                },
                subscriptionTopic: {
                    type: "string",
                    description:
                        "An MQTT topic to subscribe to, any received payload will be pushed into the attribute; use value filter(s) to extract values from string payloads and/or value converters to do simple value mapping, complex processing may require a rule or a custom MQTT agent",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "MQTTAgentLink", default: "MQTTAgentLink" },
            },
            title: "MQTT Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        MailAgentLink: {
            type: "object",
            properties: {
                fromMatchPredicate: {
                    type: "object",
                    properties: {
                        caseSensitive: { type: "boolean" },
                        match: { type: "string", enum: ["EXACT", "BEGIN", "END", "CONTAINS"] },
                        negate: { type: "boolean" },
                        value: { type: "string" },
                        type: { const: "string", default: "string" },
                    },
                    required: ["caseSensitive", "negate", "type"],
                    title: "String Predicate",
                    additionalProperties: true,
                    description:
                        "The predicate to apply to incoming mail message from address(es) to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent.",
                },
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                subjectMatchPredicate: {
                    type: "object",
                    properties: {
                        caseSensitive: { type: "boolean" },
                        match: { type: "string", enum: ["EXACT", "BEGIN", "END", "CONTAINS"] },
                        negate: { type: "boolean" },
                        value: { type: "string" },
                        type: { const: "string", default: "string" },
                    },
                    required: ["caseSensitive", "negate", "type"],
                    title: "String Predicate",
                    additionalProperties: true,
                    description:
                        "The predicate to apply to incoming mail message subjects to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                useSubject: { type: "boolean", description: "Use the subject as value instead of the body" },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "MailAgentLink", default: "MailAgentLink" },
            },
            title: "Mail Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        MathExpressionValueFilter: {
            type: "object",
            properties: {
                expression: { type: "string" },
                type: { const: "mathExpression", default: "mathExpression" },
            },
            title: "Mathematical Expression",
            additionalProperties: true,
            required: ["type"],
        },
        MockAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                requiredValue: { type: "string" },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "MockAgentLink", default: "MockAgentLink" },
            },
            title: "Mock Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        ModbusAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                pollingMillis: { type: "integer", description: "Poll interval in milliseconds" },
                readAddress: { type: "integer", description: "Zero based address from which the value is read from" },
                readMemoryArea: {
                    type: "string",
                    enum: ["COIL", "DISCRETE", "HOLDING", "INPUT"],
                    description: "Memory area to read from during read request",
                },
                readRegistersAmount: {
                    type: "integer",
                    description:
                        "Set amount of registers to read. If left empty or less than 1, will use the default size for the corresponding data-type.",
                },
                readValueType: {
                    type: "string",
                    enum: [
                        "BOOL",
                        "SINT",
                        "USINT",
                        "BYTE",
                        "INT",
                        "UINT",
                        "WORD",
                        "DINT",
                        "UDINT",
                        "DWORD",
                        "LINT",
                        "ULINT",
                        "LWORD",
                        "REAL",
                        "LREAL",
                        "CHAR",
                        "WCHAR",
                    ],
                    description: "Type to convert the returned data to. As specified by the PLC4X Modbus data types.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeAddress: {
                    type: "integer",
                    description: "Zero-based address to which the value sent is written to",
                },
                writeMemoryArea: {
                    type: "string",
                    enum: ["COIL", "HOLDING"],
                    description: 'Memory area to write to. "HOLDING" or "COIL" allowed.',
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "ModbusAgentLink", default: "ModbusAgentLink" },
            },
            required: ["pollingMillis", "readAddress", "readMemoryArea", "readValueType", "type"],
            title: "Modbus Agent Link",
            additionalProperties: true,
        },
        NumberPredicate: {
            type: "object",
            properties: {
                negate: { type: "boolean" },
                operator: {
                    type: "string",
                    enum: ["EQUALS", "GREATER_THAN", "GREATER_EQUALS", "LESS_THAN", "LESS_EQUALS", "BETWEEN"],
                },
                rangeValue: { type: "number" },
                value: { type: "number" },
                type: { const: "number", default: "number" },
            },
            required: ["negate", "type"],
            title: "Number Predicate",
            additionalProperties: true,
            description: "Predicate for number values; will match based on configured options.",
        },
        OpenWeatherMapAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                weatherProperty: {
                    type: "string",
                    enum: [
                        "TEMPERATURE",
                        "ATMOSPHERIC_PRESSURE",
                        "HUMIDITY_PERCENTAGE",
                        "CLOUD_COVERAGE",
                        "WIND_SPEED",
                        "WIND_DIRECTION_DEGREES",
                        "WIND_GUST_SPEED",
                        "PROBABILITY_OF_PRECIPITATION",
                        "RAIN_AMOUNT",
                        "ULTRAVIOLET_INDEX",
                    ],
                    description: "Select which weather property (e.g. temperature, humidity) to use as the data source",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "OpenWeatherMapAgentLink", default: "OpenWeatherMapAgentLink" },
            },
            required: ["weatherProperty", "type"],
            title: "Open Weather Map Agent Link",
            additionalProperties: true,
        },
        RadialGeofencePredicate: {
            type: "object",
            properties: {
                lat: { type: "number" },
                lng: { type: "number" },
                negated: { type: "boolean" },
                radius: { type: "integer" },
                type: { const: "radial", default: "radial" },
            },
            required: ["lat", "lng", "negated", "radius", "type"],
            title: "Radial geofence",
            additionalProperties: true,
            description:
                "Predicate for GEO JSON point values; will return true if the point is within the specified radius of the specified latitude and longitude unless negated.",
        },
        RectangularGeofencePredicate: {
            type: "object",
            properties: {
                latMax: { type: "number" },
                latMin: { type: "number" },
                lngMax: { type: "number" },
                lngMin: { type: "number" },
                negated: { type: "boolean" },
                type: { const: "rect", default: "rect" },
            },
            required: ["latMax", "latMin", "lngMax", "lngMin", "negated", "type"],
            title: "Rectangular geofence",
            additionalProperties: true,
            description:
                "Predicate for GEO JSON point values; will return true if the point is within the specified rectangle specified as latitude and longitude values of two corners unless negated.",
        },
        RegexValueFilter: {
            type: "object",
            properties: {
                matchGroup: { type: "integer" },
                matchIndex: { type: "integer" },
                pattern: { type: "string" },
                type: { const: "regex", default: "regex" },
            },
            title: "Regex",
            additionalProperties: true,
            required: ["type"],
        },
        SNMPAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "SNMPAgentLink", default: "SNMPAgentLink" },
            },
            title: "SNMP Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        SimulatorAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                replayData: {
                    type: "array",
                    items: {
                        type: "object",
                        properties: {
                            timestamp: { type: "integer" },
                            value: {
                                title: "Any Type",
                                type: ["null", "number", "integer", "boolean", "string", "array", "object"],
                                additionalProperties: true,
                            },
                        },
                        required: ["timestamp"],
                        title: "Data point",
                        additionalProperties: true,
                    },
                    description:
                        "Used to store a dataset of values that should be replayed (i.e. written to the linked attribute) in a continuous loop based on a schedule (by default replays every 24h). Predicted datapoints can be added by configuring 'Store predicted datapoints' which will insert the datapoints immediately as determined by the schedule.",
                },
                schedule: {
                    type: "object",
                    properties: {
                        end: {
                            type: "string",
                            format: "date-time",
                            description: "Not implemented, within the recurrence rule you can specify an end date.",
                        },
                        recurrence: {
                            type: "string",
                            description: "The recurrence schedule follows the RFC 5545 RRULE format.",
                        },
                        start: {
                            type: "string",
                            format: "date-time",
                            description:
                                "Set a start date, if not provided, starts immediately. When the replay datapoint timestamp is 0 it will insert it at 00:00.",
                        },
                    },
                    title: "Schedule",
                    additionalProperties: true,
                    description:
                        "When defined overwrites the possible dataset length and when it is replayed. This could be once when only a start- (and end) date are defined, or a recurring event following the RFC 5545 RRULE format. If not provided defaults to 24 hours. If the replay data contains datapoints scheduled after the default 24 hours or the recurrence rule the datapoints will be ignored.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "SimulatorAgentLink", default: "SimulatorAgentLink" },
            },
            title: "Simulator Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        StorageSimulatorAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "StorageSimulatorAgentLink", default: "StorageSimulatorAgentLink" },
            },
            title: "Storage Simulator Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        StringPredicate: {
            type: "object",
            properties: {
                caseSensitive: { type: "boolean" },
                match: { type: "string", enum: ["EXACT", "BEGIN", "END", "CONTAINS"] },
                negate: { type: "boolean" },
                value: { type: "string" },
                type: { const: "string", default: "string" },
            },
            required: ["caseSensitive", "negate", "type"],
            title: "String Predicate",
            additionalProperties: true,
            description: "Predicate for string values; will match based on configured options.",
        },
        SubStringValueFilter: {
            type: "object",
            properties: {
                beginIndex: { type: "integer" },
                endIndex: { type: "integer" },
                type: { const: "substring", default: "substring" },
            },
            required: ["beginIndex", "type"],
            title: "Substring",
            additionalProperties: true,
        },
        ValueAnyPredicate: {
            type: "object",
            title: "Any value",
            additionalProperties: true,
            description: "Predicate that matches any value including null.",
            properties: { type: { const: "value-any", default: "value-any" } },
            required: ["type"],
        },
        ValueEmptyPredicate: {
            type: "object",
            properties: { negate: { type: "boolean" }, type: { const: "value-empty", default: "value-empty" } },
            required: ["negate", "type"],
            title: "Empty value",
            additionalProperties: true,
            description: "Predicate that matches any empty/null value; unless negated.",
        },
        VelbusAgentLink: {
            type: "object",
            properties: {
                deviceAddress: { type: "integer", minimum: 1, maximum: 255 },
                deviceValueLink: { type: "string", minLength: 1 },
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "VelbusAgentLink", default: "VelbusAgentLink" },
            },
            required: ["deviceAddress", "deviceValueLink", "type"],
            title: "Velbus Agent Link",
            additionalProperties: true,
        },
        WebsocketAgentLink: {
            type: "object",
            properties: {
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                websocketSubscriptions: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/WebsocketSubscriptionImpl" },
                            { $ref: "#/definitions/WebsocketHTTPSubscription" },
                        ],
                    },
                    description:
                        "Array of WebsocketSubscriptions that should be executed when the linked attribute is linked; the subscriptions are executed in the order specified in the array.",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "WebsocketAgentLink", default: "WebsocketAgentLink" },
            },
            title: "Websocket Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
        WebsocketHTTPSubscription: {
            type: "object",
            properties: {
                body: {
                    title: "Any Type",
                    type: ["null", "number", "integer", "boolean", "string", "array", "object"],
                    additionalProperties: true,
                },
                contentType: { type: "string" },
                headers: { type: "object", additionalProperties: { type: "array", items: { type: "string" } } },
                method: { type: "string", enum: ["GET", "PUT", "POST"] },
                uri: { type: "string" },
                type: { const: "http", default: "http" },
            },
            title: "Websocket HTTP Subscription",
            additionalProperties: true,
            required: ["type"],
        },
        WebsocketSubscriptionImpl: {
            type: "object",
            properties: {
                body: {
                    title: "Any Type",
                    type: ["null", "number", "integer", "boolean", "string", "array", "object"],
                    additionalProperties: true,
                },
                type: { const: "websocket", default: "websocket" },
            },
            title: "Websocket Subscription Impl",
            additionalProperties: true,
        },
        ZWaveAgentLink: {
            type: "object",
            properties: {
                deviceEndpoint: { type: "integer" },
                deviceNodeId: { type: "integer" },
                deviceValue: { type: "string" },
                id: { type: "string", format: "or-agent-id" },
                messageMatchFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
                },
                messageMatchPredicate: {
                    oneOf: [
                        { $ref: "#/definitions/StringPredicate" },
                        { $ref: "#/definitions/BooleanPredicate" },
                        { $ref: "#/definitions/DateTimePredicate" },
                        { $ref: "#/definitions/NumberPredicate" },
                        { $ref: "#/definitions/RadialGeofencePredicate" },
                        { $ref: "#/definitions/RectangularGeofencePredicate" },
                        { $ref: "#/definitions/ArrayPredicate" },
                        { $ref: "#/definitions/ValueAnyPredicate" },
                        { $ref: "#/definitions/ValueEmptyPredicate" },
                        { $ref: "#/definitions/CalendarEventPredicate" },
                    ],
                    description:
                        "The predicate to apply to incoming messages to determine if the message is intended for the linked attribute; the value used in the predicate can be filtered using the message match filters. This must be defined to enable attributes to be updated by the linked agent.",
                },
                updateOnWrite: {
                    type: "boolean",
                    description:
                        "Don't expect a response from the protocol just update the attribute immediately on write",
                },
                valueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Defines a value converter map to allow for basic value type conversion; the incoming value will be converted to JSON and if this string matches a key in the converter then the value of that key will be pushed through to the attribute. An example use case is an API that returns 'ACTIVE'/'DISABLED' strings but you want to connect this to a Boolean attribute",
                },
                valueFilters: {
                    type: "array",
                    items: {
                        oneOf: [
                            { $ref: "#/definitions/RegexValueFilter" },
                            { $ref: "#/definitions/SubStringValueFilter" },
                            { $ref: "#/definitions/JsonPathFilter" },
                            { $ref: "#/definitions/MathExpressionValueFilter" },
                        ],
                    },
                    description:
                        "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
                },
                writeValue: {
                    type: "string",
                    format: "or-multiline",
                    description:
                        "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
                },
                writeValueConverter: {
                    type: "object",
                    patternProperties: {
                        ".+": { type: ["null", "number", "integer", "boolean", "string", "array", "object"] },
                    },
                    description:
                        "Similar to valueConverter but will be applied to outgoing values allowing for the opposite conversion",
                },
                type: { const: "ZWaveAgentLink", default: "ZWaveAgentLink" },
            },
            title: "Z Wave Agent Link",
            additionalProperties: true,
            required: ["type"],
        },
    },
    title: "Agent Link",
    oneOf: [
        { $ref: "#/definitions/KNXAgentLink" },
        { $ref: "#/definitions/MQTTAgentLink" },
        { $ref: "#/definitions/DefaultAgentLink" },
        { $ref: "#/definitions/ZWaveAgentLink" },
        { $ref: "#/definitions/HTTPAgentLink" },
        { $ref: "#/definitions/StorageSimulatorAgentLink" },
        { $ref: "#/definitions/SNMPAgentLink" },
        { $ref: "#/definitions/SimulatorAgentLink" },
        { $ref: "#/definitions/VelbusAgentLink" },
        { $ref: "#/definitions/ModbusAgentLink" },
        { $ref: "#/definitions/OpenWeatherMapAgentLink" },
        { $ref: "#/definitions/MailAgentLink" },
        { $ref: "#/definitions/WebsocketAgentLink" },
        { $ref: "#/definitions/BluetoothMeshAgentLink" },
        { $ref: "#/definitions/MockAgentLink" },
    ],
    type: "object",
    properties: { type: { const: "AgentLink" } },
    required: ["type"],
};
