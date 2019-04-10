import {
    AttributeDescriptor,
    ValueType
} from "@openremote/model";

export const attributeDescriptors: any[] =  [ {
        name: "flightProfiles",
        attributeValueDescriptor : {
            icon : "cubes",
            valueType : "OBJECT",
            name: "OBJECT"
        }
    }, {
        name: "languageCodes",
        attributeValueDescriptor : {
            icon : "ellipsis-h",
            valueType : "ARRAY",
            name: "ARRAY"
        }
    }, {
        name: "flightName",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "flightDirection",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "airlineIata",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "scheduleDate",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "scheduleTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "scheduledInBlockTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "estimatedLandingTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "actualLandingTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "estimatedPierTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "estimatedCheckPointTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "estimatedSchengenCheckPointTime",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "terminal",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "gate",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "gateNumber",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "hashtag",
            valueType : "NUMBER",
            name: "NUMBER"
        }
    }, {
        name: "pier",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "checkPoint",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "pierDisplayName",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "passengerCapacity",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "hashtag",
            valueType : "NUMBER",
            name: "NUMBER"
        }
    }, {
        name: "passengerArrivalCheckPoint",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "hashtag",
            valueType : "NUMBER",
            name: "NUMBER"
        }
    }, {
        name: "passengerSchengenCheckPoint",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "hashtag",
            valueType : "NUMBER",
            name: "NUMBER"
        }
    }, {
        name: "airportName",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "airportIata",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "city",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "countryCode",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    }, {
        name: "originRegion",
        metaItemDescriptors : [ ],
        valueDescriptor : {
            icon : "file-text-o",
            valueType : "STRING",
            name: "STRING"
        }
    } ];
