export const thingAssetInfo = {
    assetDescriptor: {
        descriptorType: "asset",
        name: "ThingAsset",
        icon: "cube-outline",
    },
    attributeDescriptors: [
        {
            name: "notes",
            type: "text",
            format: {
                multiline: true,
            },
        },
        {
            name: "location",
            type: "GEO_JSONPoint",
        },
        {
            name: "model",
            type: "text",
            optional: true,
        },
        {
            name: "email",
            type: "email",
            optional: true,
        },
        {
            name: "tags",
            type: "text[]",
            optional: true,
        },
        {
            name: "manufacturer",
            type: "text",
            optional: true,
        },
    ],
    metaItemDescriptors: [
        "agentLink",
        "agentLinkConfig",
        "attributeLinks",
        "accessPublicRead",
        "accessPublicWrite",
        "accessRestrictedRead",
        "accessRestrictedWrite",
        "readOnly",
        "storeDataPoints",
        "dataPointsMaxAgeDays",
        "hasPredictedDataPoints",
        "forecast",
        "ruleState",
        "ruleResetImmediate",
        "label",
        "format",
        "units",
        "constraints",
        "secret",
        "multiline",
        "showOnDashboard",
        "momentary",
        "userConnected",
    ],
    valueDescriptors: [
        "boolean",
        "booleanMap",
        "integer",
        "long",
        "bigInteger",
        "integerMap",
        "number",
        "numberMap",
        "bigNumber",
        "text",
        "textMap",
        "JSONObject",
        "JSON",
        "multivaluedTextMap",
        "positiveInteger",
        "negativeInteger",
        "positiveNumber",
        "negativeNumber",
        "integerByte",
        "byte",
        "timestamp",
        "timestampISO8601",
        "dateAndTime",
        "timeDurationISO8601",
        "periodDurationISO8601",
        "timeAndPeriodDurationISO8601",
        "assetID",
        "assetType",
        "direction",
        "attributeLink",
        "attributeReference",
        "attributeState",
        "GEO_JSONPoint",
        "colourRGB",
        "usernameAndPassword",
        "valueFormat",
        "valueConstraint",
        "agentLink",
        "forecastConfiguration",
        "valueDescriptor",
    ],
};

export const metaItemDescriptors = {
    accessRestrictedRead: {
        name: "accessRestrictedRead",
        type: "boolean",
    },
    momentary: {
        name: "momentary",
        type: "boolean",
    },
    accessRestrictedWrite: {
        name: "accessRestrictedWrite",
        type: "boolean",
    },
    agentLink: {
        name: "agentLink",
        type: "agentLink",
    },
    agentLinkConfig: {
        name: "agentLinkConfig",
        type: "JSONObject",
    },
    format: {
        name: "format",
        type: "valueFormat",
    },
    readOnly: {
        name: "readOnly",
        type: "boolean",
    },
    forecast: {
        name: "forecast",
        type: "forecastConfiguration",
    },
    ruleResetImmediate: {
        name: "ruleResetImmediate",
        type: "boolean",
    },
    label: {
        name: "label",
        type: "text",
    },
    units: {
        name: "units",
        type: "text[]",
    },
    secret: {
        name: "secret",
        type: "boolean",
    },
    constraints: {
        name: "constraints",
        type: "valueConstraint[]",
    },
    hasPredictedDataPoints: {
        name: "hasPredictedDataPoints",
        type: "boolean",
    },
    dataPointsMaxAgeDays: {
        name: "dataPointsMaxAgeDays",
        type: "positiveInteger",
    },
    ruleState: {
        name: "ruleState",
        type: "boolean",
    },
    storeDataPoints: {
        name: "storeDataPoints",
        type: "boolean",
    },
    accessPublicWrite: {
        name: "accessPublicWrite",
        type: "boolean",
    },
    userConnected: {
        name: "userConnected",
        type: "text",
    },
    showOnDashboard: {
        name: "showOnDashboard",
        type: "boolean",
    },
    accessPublicRead: {
        name: "accessPublicRead",
        type: "boolean",
    },
    multiline: {
        name: "multiline",
        type: "boolean",
    },
    attributeLinks: {
        name: "attributeLinks",
        type: "attributeLink[]",
    },
};

export const valueDescriptors = {
    numberMap: {
        name: "numberMap",
        type: "org.openremote.model.value.ValueType$DoubleMap",
        jsonType: "object",
    },
    bigNumber: {
        name: "bigNumber",
        type: "java.math.BigDecimal",
        jsonType: "number",
    },
    agentLink: {
        name: "agentLink",
        type: "org.openremote.model.asset.agent.AgentLink",
        metaUseOnly: true,
        jsonType: "object",
    },
    negativeNumber: {
        name: "negativeNumber",
        type: "java.lang.Double",
        constraints: [
            {
                type: "max",
                max: 0,
                message: "{ValueConstraint.Max.message}",
            },
        ],
        jsonType: "number",
    },
    integer: {
        name: "integer",
        type: "java.lang.Integer",
        jsonType: "number",
    },
    positiveNumber: {
        name: "positiveNumber",
        type: "java.lang.Double",
        constraints: [
            {
                type: "min",
                min: 0,
                message: "{ValueConstraint.Min.message}",
            },
        ],
        jsonType: "number",
    },
    usernameAndPassword: {
        name: "usernameAndPassword",
        type: "org.openremote.model.auth.UsernamePassword",
        jsonType: "object",
    },
    timeAndPeriodDurationISO8601: {
        name: "timeAndPeriodDurationISO8601",
        type: "org.openremote.model.value.impl.PeriodAndDuration",
        constraints: [
            {
                type: "pattern",
                regexp: "^P(?!$)((\\d+Y)|(\\d+\\.\\d+Y$))?((\\d+M)|(\\d+\\.\\d+M$))?((\\d+W)|(\\d+\\.\\d+W$))?((\\d+D)|(\\d+\\.\\d+D$))?(T(?=\\d)((\\d+H)|(\\d+\\.\\d+H$))?((\\d+M)|(\\d+\\.\\d+M$))?(\\d+(\\.\\d+)?S)?)??$",
                message: "{ValueConstraint.Pattern.message}",
            },
        ],
        jsonType: "object",
    },
    number: {
        name: "number",
        type: "java.lang.Double",
        jsonType: "number",
    },
    textMap: {
        name: "textMap",
        type: "org.openremote.model.value.ValueType$StringMap",
        jsonType: "object",
    },
    HTTPMethod: {
        name: "HTTPMethod",
        type: "org.openremote.model.http.HTTPMethod",
        constraints: [
            {
                type: "allowedValues",
                allowedValues: ["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"],
                message: "{ValueConstraint.AllowedValues.message}",
            },
        ],
        jsonType: "string",
    },
    negativeInteger: {
        name: "negativeInteger",
        type: "java.lang.Integer",
        constraints: [
            {
                type: "max",
                max: 0,
                message: "{ValueConstraint.Max.message}",
            },
        ],
        jsonType: "number",
    },
    text: {
        name: "text",
        type: "java.lang.String",
        jsonType: "string",
    },
    periodDurationISO8601: {
        name: "periodDurationISO8601",
        type: "java.time.Period",
        constraints: [
            {
                type: "pattern",
                regexp: "^P(?!$)((\\d+Y)|(\\d+\\.\\d+Y$))?((\\d+M)|(\\d+\\.\\d+M$))?((\\d+W)|(\\d+\\.\\d+W$))?((\\d+D)|(\\d+\\.\\d+D$))?(T(?=\\d)((\\d+H)|(\\d+\\.\\d+H$))?((\\d+M)|(\\d+\\.\\d+M$))?(\\d+(\\.\\d+)?S)?)??$",
                message: "{ValueConstraint.Pattern.message}",
            },
        ],
        jsonType: "object",
    },
    byte: {
        name: "byte",
        type: "java.lang.Byte",
        jsonType: "number",
    },
    bigInteger: {
        name: "bigInteger",
        type: "java.math.BigInteger",
        jsonType: "bigint",
    },
    attributeLink: {
        name: "attributeLink",
        type: "org.openremote.model.attribute.AttributeLink",
        metaUseOnly: true,
        jsonType: "object",
    },
    colourRGB: {
        name: "colourRGB",
        type: "org.openremote.model.value.impl.ColourRGB",
        jsonType: "object",
    },
    assetType: {
        name: "assetType",
        type: "java.lang.String",
        constraints: [
            {
                type: "allowedValues",
                allowedValues: ["ThingAsset"],
                message: "{ValueConstraint.AllowedValues.message}",
            },
        ],
        jsonType: "string",
    },
    JSONObject: {
        name: "JSONObject",
        type: "org.openremote.model.value.ValueType$ObjectMap",
        jsonType: "object",
    },
    attributeState: {
        name: "attributeState",
        type: "org.openremote.model.attribute.AttributeState",
        metaUseOnly: true,
        jsonType: "object",
    },
    JSON: {
        name: "JSON",
        type: "java.lang.Object",
        jsonType: "unknown",
    },
    attributeReference: {
        name: "attributeReference",
        type: "org.openremote.model.attribute.AttributeRef",
        metaUseOnly: true,
        jsonType: "object",
    },
    long: {
        name: "long",
        type: "java.lang.Long",
        jsonType: "number",
    },
    integerMap: {
        name: "integerMap",
        type: "org.openremote.model.value.ValueType$IntegerMap",
        jsonType: "object",
    },
    timestampISO8601: {
        name: "timestampISO8601",
        type: "java.lang.String",
        constraints: [
            {
                type: "pattern",
                regexp: "^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:[\\.,]\\d{1,9})?(?:Z|[+-][01]\\d:[0-5]\\d)$",
                message: "{ValueConstraint.Pattern.message}",
            },
        ],
        format: {
            asDate: true,
            momentJsFormat: "DD-MMM-YYYY hh:mm A",
        },
        jsonType: "string",
    },
    valueDescriptor: {
        name: "valueDescriptor",
        type: "org.openremote.model.value.ValueDescriptor",
        metaUseOnly: true,
        jsonType: "object",
    },
    multivaluedTextMap: {
        name: "multivaluedTextMap",
        type: "org.openremote.model.value.ValueType$MultivaluedStringMap",
        jsonType: "object",
    },
    timestamp: {
        name: "timestamp",
        type: "java.lang.Long",
        constraints: [
            {
                type: "min",
                min: 0,
                message: "{ValueConstraint.Min.message}",
            },
        ],
        format: {
            asDate: true,
            momentJsFormat: "DD-MMM-YYYY hh:mm A",
        },
        jsonType: "number",
    },
    direction: {
        name: "direction",
        type: "java.lang.Integer",
        constraints: [
            {
                type: "min",
                min: 0,
                message: "{ValueConstraint.Min.message}",
            },
            {
                type: "max",
                max: 360,
                message: "{ValueConstraint.Max.message}",
            },
        ],
        jsonType: "number",
    },
    forecastConfiguration: {
        name: "forecastConfiguration",
        type: "org.openremote.model.value.ForecastConfiguration",
        metaUseOnly: true,
        jsonType: "object",
    },
    timeDurationISO8601: {
        name: "timeDurationISO8601",
        type: "java.time.Duration",
        constraints: [
            {
                type: "pattern",
                regexp: "^P(?!$)((\\d+Y)|(\\d+\\.\\d+Y$))?((\\d+M)|(\\d+\\.\\d+M$))?((\\d+W)|(\\d+\\.\\d+W$))?((\\d+D)|(\\d+\\.\\d+D$))?(T(?=\\d)((\\d+H)|(\\d+\\.\\d+H$))?((\\d+M)|(\\d+\\.\\d+M$))?(\\d+(\\.\\d+)?S)?)??$",
                message: "{ValueConstraint.Pattern.message}",
            },
        ],
        jsonType: "object",
    },
    dateAndTime: {
        name: "dateAndTime",
        type: "java.util.Date",
        jsonType: "date",
    },
    booleanMap: {
        name: "booleanMap",
        type: "org.openremote.model.value.ValueType$BooleanMap",
        jsonType: "object",
    },
    valueFormat: {
        name: "valueFormat",
        type: "org.openremote.model.value.ValueFormat",
        metaUseOnly: true,
        jsonType: "object",
    },
    GEO_JSONPoint: {
        name: "GEO_JSONPoint",
        type: "org.openremote.model.geo.GeoJSONPoint",
        jsonType: "object",
    },
    integerByte: {
        name: "integerByte",
        type: "java.lang.Integer",
        constraints: [
            {
                type: "min",
                min: 0,
                message: "{ValueConstraint.Min.message}",
            },
            {
                type: "max",
                max: 255,
                message: "{ValueConstraint.Max.message}",
            },
        ],
        jsonType: "number",
    },
    boolean: {
        name: "boolean",
        type: "java.lang.Boolean",
        jsonType: "boolean",
    },
    assetID: {
        name: "assetID",
        type: "java.lang.String",
        constraints: [
            {
                type: "pattern",
                regexp: "^[0-9A-Za-z]{22}$",
                message: "{ValueConstraint.Pattern.message}",
            },
        ],
        jsonType: "string",
    },
    positiveInteger: {
        name: "positiveInteger",
        type: "java.lang.Integer",
        constraints: [
            {
                type: "min",
                min: 0,
                message: "{ValueConstraint.Min.message}",
            },
        ],
        jsonType: "number",
    },
    valueConstraint: {
        name: "valueConstraint",
        type: "org.openremote.model.value.ValueConstraint",
        metaUseOnly: true,
        jsonType: "object",
    },
};
