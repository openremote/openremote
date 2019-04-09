import {
    AttributeDescriptor,
    ValueType
} from "@openremote/model";

export const attributeDescriptors: AttributeDescriptor[] = [
    {name: "flightProfiles", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "airportIata", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "airlineIata", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "originRegion", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "languageCodes", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}},
    {name: "passengerCapacity", valueDescriptor: {name: "NUMBER", valueType: ValueType.NUMBER}},
    {name: "countryCode", valueDescriptor: {name: "STRING", valueType: ValueType.STRING}}
];
