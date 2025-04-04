const constraints = {
  $schema: "http://json-schema.org/draft-07/schema#",
  title: "Value Constraint",
  oneOf: [
    { $ref: "#/definitions/size" },
    { $ref: "#/definitions/pattern" },
    { $ref: "#/definitions/min" },
    { $ref: "#/definitions/max" },
    { $ref: "#/definitions/allowedValues" },
    { $ref: "#/definitions/past" },
    { $ref: "#/definitions/pastOrPresent" },
    { $ref: "#/definitions/future" },
    { $ref: "#/definitions/futureOrPresent" },
    { $ref: "#/definitions/notEmpty" },
    { $ref: "#/definitions/notBlank" },
    { $ref: "#/definitions/notNull" },
  ],
  definitions: {
    size: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be between the specified boundaries based on the `min` and `max` properties. Supported types are JSON compatible strings, arrays and objects. Null values are considered valid.",
      properties: {
        type: { type: "string", enum: ["size"], default: "size" },
        min: { type: "integer" },
        max: { type: "integer" },
        message: { type: "string" },
      },
      title: "size",
      required: ["type"],
    },
    pattern: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must match the regular expression pattern described in the `regexp` property. The regular expression follows the Java regular expression conventions. Flags can be specified using the `flags` property with values: CASE_INSENSITIVE, MULTILINE, DOTALL, UNIX_LINES, UNICODE_CASE, CANON_EQ and COMMENTS. Null values are considered valid.",
      properties: {
        type: { type: "string", enum: ["pattern"], default: "pattern" },
        regexp: { type: "string" },
        flags: {
          type: "array",
          items: {
            type: "string",
            enum: [
              "UNIX_LINES",
              "CASE_INSENSITIVE",
              "COMMENTS",
              "MULTILINE",
              "DOTALL",
              "UNICODE_CASE",
              "CANON_EQ",
            ],
          },
        },
        message: { type: "string" },
      },
      title: "pattern",
      required: ["type"],
    },
    min: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be a number higher or equal to the specified value on the `min` property. Null values are considered valid.",
      properties: {
        type: { type: "string", enum: ["min"], default: "min" },
        min: { type: "number" },
        message: { type: "string" },
      },
      title: "min",
      required: ["type"],
    },
    max: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be a number lower or equal to the specified value on the `max` property. Null values are considered valid.",
      properties: {
        type: { type: "string", enum: ["max"], default: "max" },
        max: { type: "number" },
        message: { type: "string" },
      },
      title: "max",
      required: ["type"],
    },
    allowedValues: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must match any of the specified values in the `allowedValues` property. The associated input in the UI will change to a select input with the options specified in the `allowedValues` property. The `allowedValueNames` property accepts a list of names that replace the labels in the select input, if the `allowedValueNames` list matches the length of the `allowedValues` list otherwise it falls back to the `allowedValues` as labels. Null values are considered valid. If `allowedValues` is not specified or empty the constraint only accepts null values.",
      properties: {
        type: {
          type: "string",
          enum: ["allowedValues"],
          default: "allowedValues",
        },
        allowedValueNames: { type: "array", items: { type: "string" } },
        allowedValues: {
          type: "array",
          items: { $ref: "#/definitions/AnyType" },
        },
        message: { type: "string" },
      },
      title: "allowedValues",
      required: ["type"],
    },
    AnyType: {
      type: [
        "null",
        "number",
        "integer",
        "boolean",
        "string",
        "array",
        "object",
      ],
      additionalProperties: true,
      properties: {},
    },
    past: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be a java time object, java date object, a string in ISO8601 format or a number representing epoch milliseconds; the value must represent a time in the past. Null values are considered valid.",
      properties: {
        type: { type: "string", enum: ["past"], default: "past" },
        message: { type: "string" },
      },
      title: "past",
      required: ["type"],
    },
    pastOrPresent: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be a java time object, java date object, a string in ISO8601 format or a number representing epoch milliseconds; the value must represent a time in the past or present. Null values are considered valid.",
      properties: {
        type: {
          type: "string",
          enum: ["pastOrPresent"],
          default: "pastOrPresent",
        },
        message: { type: "string" },
      },
      title: "pastOrPresent",
      required: ["type"],
    },
    future: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be a java time object, java date object, a string in ISO8601 format or a number representing epoch milliseconds; the value must represent a time in the future. Null values are considered valid.",
      properties: {
        type: { type: "string", enum: ["future"], default: "future" },
        message: { type: "string" },
      },
      title: "future",
      required: ["type"],
    },
    futureOrPresent: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must be a java time object, java date object, a string in ISO8601 format or a number representing epoch milliseconds; the value must represent a time in the future or present. Null values are considered valid.",
      properties: {
        type: {
          type: "string",
          enum: ["futureOrPresent"],
          default: "futureOrPresent",
        },
        message: { type: "string" },
      },
      title: "futureOrPresent",
      required: ["type"],
    },
    notEmpty: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must not be null nor empty. Supported types are JSON compatible strings, arrays and objects which contain at least 1 character, item or property.",
      properties: {
        type: { type: "string", enum: ["notEmpty"], default: "notEmpty" },
        message: { type: "string" },
      },
      title: "notEmpty",
      required: ["type"],
    },
    notBlank: {
      type: "object",
      additionalProperties: true,
      description:
        "The attribute value must not be null and must contain at least 1 non-whitespace character. Accepts strings.",
      properties: {
        type: { type: "string", enum: ["notBlank"], default: "notBlank" },
        message: { type: "string" },
      },
      title: "notBlank",
      required: ["type"],
    },
    notNull: {
      type: "object",
      additionalProperties: true,
      description: "The attribute value must not be null. Accepts any type.",
      properties: {
        type: { type: "string", enum: ["notNull"], default: "notNull" },
        message: { type: "string" },
      },
      title: "notNull",
      required: ["type"],
    },
  },
};

const agentLink = {
  $schema: "http://json-schema.org/draft-07/schema#",
  title: "Agent Link",
  oneOf: [
    { $ref: "#/definitions/SNMPAgentLink" },
    { $ref: "#/definitions/DefaultAgentLink" },
    { $ref: "#/definitions/HTTPAgentLink" },
    { $ref: "#/definitions/MockAgentLink" },
    { $ref: "#/definitions/WebsocketAgentLink" },
    { $ref: "#/definitions/ZWaveAgentLink" },
    { $ref: "#/definitions/StorageSimulatorAgentLink" },
    { $ref: "#/definitions/BluetoothMeshAgentLink" },
    { $ref: "#/definitions/MQTTAgentLink" },
    { $ref: "#/definitions/KNXAgentLink" },
    { $ref: "#/definitions/SimulatorAgentLink" },
    { $ref: "#/definitions/MailAgentLink" },
    { $ref: "#/definitions/VelbusAgentLink" },
  ],
  definitions: {
    SNMPAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["SNMPAgentLink"],
          default: "SNMPAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        oid: { type: "string" },
      },
      title: "SNMPAgentLink",
      required: ["type", "oid"],
    },
    regex: {
      type: "object",
      additionalProperties: true,
      title: "Regex",
      properties: {
        type: { type: "string", enum: ["regex"], default: "regex" },
        pattern: { type: "string" },
        matchGroup: { type: "integer" },
        matchIndex: { type: "integer" },
      },
      required: ["type"],
    },
    substring: {
      type: "object",
      additionalProperties: true,
      title: "Substring",
      properties: {
        type: { type: "string", enum: ["substring"], default: "substring" },
        beginIndex: { type: "integer" },
        endIndex: { type: "integer" },
      },
      required: ["type", "beginIndex"],
    },
    jsonPath: {
      type: "object",
      additionalProperties: true,
      title: "JSON Path",
      properties: {
        type: { type: "string", enum: ["jsonPath"], default: "jsonPath" },
        path: { type: "string" },
        returnFirst: { type: "boolean" },
        returnLast: { type: "boolean" },
      },
      required: ["type", "path", "returnFirst", "returnLast"],
    },
    mathExpression: {
      type: "object",
      additionalProperties: true,
      title: "Mathematical Expression",
      properties: {
        type: {
          type: "string",
          enum: ["mathExpression"],
          default: "mathExpression",
        },
        expression: { type: "string" },
      },
      required: ["type"],
    },
    AnyType: {
      type: [
        "null",
        "number",
        "integer",
        "boolean",
        "string",
        "array",
        "object",
      ],
      additionalProperties: true,
      properties: {},
    },
    StringPredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for string values; will match based on configured options.",
      properties: {
        predicateType: { type: "string", enum: ["string"], default: "string" },
        match: { type: "string", enum: ["EXACT", "BEGIN", "END", "CONTAINS"] },
        caseSensitive: { type: "boolean" },
        value: { type: "string" },
        negate: { type: "boolean" },
      },
      title: "string",
      required: ["predicateType", "caseSensitive", "negate"],
    },
    BooleanPredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for boolean values; will evaluate the value as a boolean and match against this predicates value, any value that is not a boolean will not match",
      properties: {
        predicateType: {
          type: "string",
          enum: ["boolean"],
          default: "boolean",
        },
        value: { type: "boolean" },
      },
      title: "boolean",
      required: ["predicateType", "value"],
    },
    DateTimePredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for date time values; provided values should be valid ISO 8601 datetime strings (e.g. yyyy-MM-dd'T'HH:mm:ssZ or yyyy-MM-dd'T'HH:mm:ss±HH:mm), offset and time are optional, if no offset information is supplied then UTC is assumed.",
      properties: {
        predicateType: {
          type: "string",
          enum: ["datetime"],
          default: "datetime",
        },
        value: { type: "string" },
        rangeValue: { type: "string" },
        operator: {
          type: "string",
          enum: [
            "EQUALS",
            "GREATER_THAN",
            "GREATER_EQUALS",
            "LESS_THAN",
            "LESS_EQUALS",
            "BETWEEN",
          ],
        },
        negate: { type: "boolean" },
      },
      title: "datetime",
      required: ["predicateType", "negate"],
    },
    NumberPredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for number values; will match based on configured options.",
      properties: {
        predicateType: { type: "string", enum: ["number"], default: "number" },
        value: { type: "number" },
        rangeValue: { type: "number" },
        operator: {
          type: "string",
          enum: [
            "EQUALS",
            "GREATER_THAN",
            "GREATER_EQUALS",
            "LESS_THAN",
            "LESS_EQUALS",
            "BETWEEN",
          ],
        },
        negate: { type: "boolean" },
      },
      title: "number",
      required: ["predicateType", "negate"],
    },
    RadialGeofencePredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for GEO JSON point values; will return true if the point is within the specified radius of the specified latitude and longitude unless negated.",
      title: "Radial geofence",
      properties: {
        predicateType: { type: "string", enum: ["radial"], default: "radial" },
        radius: { type: "integer" },
        lat: { type: "number" },
        lng: { type: "number" },
        negated: { type: "boolean" },
      },
      required: ["predicateType", "radius", "lat", "lng", "negated"],
    },
    RectangularGeofencePredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for GEO JSON point values; will return true if the point is within the specified rectangle specified as latitude and longitude values of two corners unless negated.",
      title: "Rectangular geofence",
      properties: {
        predicateType: { type: "string", enum: ["rect"], default: "rect" },
        latMin: { type: "number" },
        lngMin: { type: "number" },
        latMax: { type: "number" },
        lngMax: { type: "number" },
        negated: { type: "boolean" },
      },
      required: [
        "predicateType",
        "latMin",
        "lngMin",
        "latMax",
        "lngMax",
        "negated",
      ],
    },
    ArrayPredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for array values; will match based on configured options.",
      properties: {
        predicateType: { type: "string", enum: ["array"], default: "array" },
        value: { $ref: "#/definitions/AnyType" },
        index: { type: "integer" },
        lengthEquals: { type: "integer" },
        lengthGreaterThan: { type: "integer" },
        lengthLessThan: { type: "integer" },
        negated: { type: "boolean" },
      },
      title: "array",
      required: ["predicateType", "negated"],
    },
    ValueAnyPredicate: {
      type: "object",
      additionalProperties: true,
      description: "Predicate that matches any value including null.",
      title: "Any value",
      properties: {
        predicateType: {
          type: "string",
          enum: ["value-any"],
          default: "value-any",
        },
      },
      required: ["predicateType"],
    },
    ValueEmptyPredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate that matches any empty/null value; unless negated.",
      title: "Empty value",
      properties: {
        predicateType: {
          type: "string",
          enum: ["value-empty"],
          default: "value-empty",
        },
        negate: { type: "boolean" },
      },
      required: ["predicateType", "negate"],
    },
    CalendarEventPredicate: {
      type: "object",
      additionalProperties: true,
      description:
        "Predicate for calendar event values; will match based on whether the calendar event is active for the specified time.",
      title: "Calendar",
      properties: {
        predicateType: {
          type: "string",
          enum: ["calendar-event"],
          default: "calendar-event",
        },
        timestamp: { type: "integer", format: "utc-millisec" },
      },
      required: ["predicateType"],
    },
    DefaultAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["DefaultAgentLink"],
          default: "DefaultAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
      },
      title: "DefaultAgentLink",
      required: ["type"],
    },
    HTTPAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["HTTPAgentLink"],
          default: "HTTPAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        headers: {
          type: "object",
          additionalProperties: { type: "array", items: { type: "string" } },
          description:
            "A JSON object of headers to be added to HTTP request; the key represents the name of the header and for each string value supplied a new header will be added with the key name and specified string value",
        },
        queryParameters: {
          type: "object",
          additionalProperties: { type: "array", items: { type: "string" } },
          description:
            "A JSON object of query parameters to be added to HTTP request URL; the key represents the name of the query parameter and for each string value supplied a new query parameter will be added with the key name and specified string value (e.g. 'https://..../?test=1&test=2')",
        },
        pollingMillis: {
          type: "integer",
          description:
            "Indicates that this HTTP request is used to update the linked attribute; this value indicates how frequently the HTTP request is made in order to update the linked attribute value",
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
        method: {
          type: "string",
          enum: ["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"],
          description:
            "The HTTP method to use when making requests for this linked attribute",
        },
        contentType: {
          type: "string",
          description:
            "The content type header value to use when making requests for this linked attribute (shortcut alternative to using headers parameter)",
        },
        pollingAttribute: {
          type: "string",
          description:
            "Allows the polled response to be written to another attribute with the specified name on the same asset as the linked attribute",
        },
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
      },
      title: "HTTPAgentLink",
      required: ["type", "messageConvertBinary", "messageConvertHex"],
    },
    MockAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["MockAgentLink"],
          default: "MockAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        requiredValue: { type: "string" },
      },
      title: "MockAgentLink",
      required: ["type"],
    },
    WebsocketAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["WebsocketAgentLink"],
          default: "WebsocketAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        websocketSubscriptions: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/websocket" },
              { $ref: "#/definitions/http" },
            ],
          },
        },
      },
      title: "WebsocketAgentLink",
      required: ["type"],
    },
    websocket: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: { type: "string", enum: ["websocket"], default: "websocket" },
        body: { $ref: "#/definitions/AnyType" },
      },
      title: "websocket",
      required: ["type"],
    },
    http: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: { type: "string", enum: ["http"], default: "http" },
        body: { $ref: "#/definitions/AnyType" },
        method: { type: "string", enum: ["GET", "PUT", "POST"] },
        contentType: { type: "string" },
        headers: {
          type: "object",
          additionalProperties: { type: "array", items: { type: "string" } },
        },
        uri: { type: "string" },
      },
      title: "http",
      required: ["type"],
    },
    ZWaveAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["ZWaveAgentLink"],
          default: "ZWaveAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        deviceNodeId: { type: "integer" },
        deviceEndpoint: { type: "integer" },
        deviceValue: { type: "string" },
      },
      title: "ZWaveAgentLink",
      required: ["type"],
    },
    StorageSimulatorAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["StorageSimulatorAgentLink"],
          default: "StorageSimulatorAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
      },
      title: "StorageSimulatorAgentLink",
      required: ["type"],
    },
    BluetoothMeshAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["BluetoothMeshAgentLink"],
          default: "BluetoothMeshAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        appKeyIndex: { type: "integer", minimum: 0, maximum: 2147483647 },
        address: { type: "string", pattern: "^([0-9A-Fa-f]{4})$" },
        modelName: { type: "string", pattern: "^.*\\S+.*$", minLength: 1 },
      },
      title: "BluetoothMeshAgentLink",
      required: ["type", "appKeyIndex", "modelName"],
    },
    MQTTAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["MQTTAgentLink"],
          default: "MQTTAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        subscriptionTopic: {
          type: "string",
          description:
            "An MQTT topic to subscribe to, any received payload will be pushed into the attribute; use value filter(s) to extract values from string payloads and/or value converters to do simple value mapping, complex processing may require a rule or a custom MQTT agent",
        },
        publishTopic: {
          type: "string",
          description:
            "An MQTT topic to publish attribute events to, any received payload will be pushed into the attribute; use write value converter and/or write value to do any processing, complex processing may require a rule or a custom MQTT agent",
        },
        qos: {
          type: "integer",
          minimum: 0,
          maximum: 2,
          description:
            "QoS level to use for publish/subscribe (default is 0 if unset)",
        },
      },
      title: "MQTTAgentLink",
      required: ["type"],
    },
    KNXAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["KNXAgentLink"],
          default: "KNXAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        dpt: { type: "string", pattern: "^\\d{1,3}\\.\\d{1,3}$" },
        actionGroupAddress: {
          type: "string",
          pattern: "^\\d{1,3}/\\d{1,3}/\\d{1,3}$",
        },
        statusGroupAddress: {
          type: "string",
          pattern: "^\\d{1,3}/\\d{1,3}/\\d{1,3}$",
        },
      },
      title: "KNXAgentLink",
      required: ["type", "dpt"],
    },
    SimulatorAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["SimulatorAgentLink"],
          default: "SimulatorAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        replayData: {
          type: "array",
          items: { $ref: "#/definitions/SimulatorReplayDatapoint" },
          description:
            "Used to store 24h dataset of values that should be replayed (i.e. written to the linked attribute) in a continuous loop.",
        },
      },
      title: "SimulatorAgentLink",
      required: ["type"],
    },
    SimulatorReplayDatapoint: {
      type: "object",
      additionalProperties: true,
      title: "Data point",
      properties: {
        timestamp: { type: "integer" },
        value: { $ref: "#/definitions/AnyType" },
      },
      required: ["timestamp"],
    },
    MailAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["MailAgentLink"],
          default: "MailAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
        subjectMatchPredicate: {
          $ref: "#/definitions/StringPredicate",
          description:
            "The predicate to apply to incoming mail message subjects to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent.",
        },
        fromMatchPredicate: {
          $ref: "#/definitions/StringPredicate",
          description:
            "The predicate to apply to incoming mail message from address(es) to determine if the message is intended for the linked attribute. This must be defined to enable attributes to be updated by the linked agent.",
        },
        useSubject: {
          type: "boolean",
          description: "Use the subject as value instead of the body",
        },
      },
      title: "MailAgentLink",
      required: ["type"],
    },
    VelbusAgentLink: {
      type: "object",
      additionalProperties: true,
      properties: {
        type: {
          type: "string",
          enum: ["VelbusAgentLink"],
          default: "VelbusAgentLink",
        },
        id: { type: "string", format: "or-agent-id" },
        deviceAddress: { type: "integer", minimum: 1, maximum: 255 },
        deviceValueLink: {
          type: "string",
          pattern: "^.*\\S+.*$",
          minLength: 1,
        },
        valueFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "Defines ValueFilters to apply to an incoming value before it is written to a protocol linked attribute; this is particularly useful for generic protocols. The message should pass through the filters in array order",
        },
        valueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValueConverter: {
          type: "object",
          patternProperties: {
            ".+": {
              type: [
                "null",
                "number",
                "integer",
                "boolean",
                "string",
                "array",
                "object",
              ],
            },
          },
        },
        writeValue: {
          type: "string",
          format: "or-multiline",
          description:
            "String to be used for attribute writes and can contain dynamic placeholders to allow dyanmic value and/or time injection with formatting (see documentation for details) into the string or alternatively write the string through to the protocol as is (static string)",
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
        messageMatchFilters: {
          type: "array",
          items: {
            oneOf: [
              { $ref: "#/definitions/regex" },
              { $ref: "#/definitions/substring" },
              { $ref: "#/definitions/jsonPath" },
              { $ref: "#/definitions/mathExpression" },
            ],
          },
          description:
            "ValueFilters to apply to incoming messages prior to comparison with the messageMatchPredicate",
        },
        updateOnWrite: {
          type: "boolean",
          description:
            "Don't expect a response from the protocol just update the attribute immediately on write",
        },
      },
      title: "VelbusAgentLink",
      required: ["type", "deviceAddress", "deviceValueLink"],
    },
  },
};

const traverse = require("json-schema-traverse");

// https://json-schema.org/understanding-json-schema/keywords
// see node_modules/@jsonforms/core/lib/models/jsonSchema7.d.ts
const jsonSchemaKeywords = [
  "$ref",
  "$id",
  "$schema",
  "title",
  "description",
  "default",
  "multipleOf",
  "maximum",
  "exclusiveMaximum",
  "minimum",
  "exclusiveMinimum",
  "maxLength",
  "minLength",
  "pattern",
  "additionalItems",
  "items",
  "maxItems",
  "minItems",
  "uniqueItems",
  "maxProperties",
  "minProperties",
  "required",
  "additionalProperties",
  "definitions",
  "properties",
  "patternProperties",
  "dependencies",
  "enum",
  "type",
  "allOf",
  "anyOf",
  "oneOf",
  "not",
  "format",
  "readOnly",
  "writeOnly",
  "examples",
  "contains",
  "propertyNames",
  "const",
  "if",
  "then",
  "else",
  "errorMessage",
];

const paths = new Set();

traverse(
  constraints,
  {},
  (
    schema,
    jsonPtr,
    rootSchema,
    parentJsonPtr,
    parentKeyword,
    parentSchema,
    keyIndex
  ) => {
    // console.log(`Visiting: ${jsonPtr}`);
    const path = jsonPtr
      .split("/")
      .filter(
        (segment) =>
          segment &&
          !/^\d+$/.test(segment) &&
          !jsonSchemaKeywords.includes(segment)
      )
      .join(".");
    if (path) {
      paths.add(path);
    }
  }
);

const fs = require("fs");
const path = require("path");

const localesDir = path.resolve("ui/app/shared/locales");
console.log(localesDir);
fs.readdir(localesDir, {}, (err, filenames) => {
  const locales = filenames.filter((v) => v.length === 2);
  for (const locale of locales) {
    const currFile = `${localesDir}/${locale}/or.json`;
    fs.readFile(currFile, {}, (err, json) => {
      const translations = JSON.parse(json.toString());
      translations.schema ??= {};
      translations.schema.item ??= {};
      const schemaItems = translations.schema.item;
      for (const path of [...paths]) {
        const segs = path.split(".");
        segs.reduce((acc, key, index) => {
          if (index === segs.length - 1) {
            acc[key] = {
              label: "",
              description: "",
            };
          } else {
            acc[key] ||= {};
          }
          return acc[key];
        }, schemaItems);
      }
      // TODO: Instead make this append on the line without changing formatting
      fs.writeFile(
        currFile,
        JSON.stringify(translations, null, 2),
        {},
        () => {}
      );
    });
  }
});
