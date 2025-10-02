export const parentAssets = [
    {
        name: "City Asset 1",
        type: "CityAsset",
        realm: "smartcity",
        attributes: {
            notes: { name: "notes", type: "text" },
            location: { name: "location", type: "GEO_JSONPoint" },
            country: { name: "country", type: "text" }
        }
    },
    {
        name: "City Asset 2",
        type: "CityAsset",
        realm: "smartcity",
        attributes: {
            notes: { name: "notes", type: "text" },
            location: { name: "location", type: "GEO_JSONPoint" },
            country: { name: "country", type: "text" }
        }
    }
];
export const batteryAsset = {
    name: "Battery",
    type: "ThingAsset",
    realm: "smartcity",
    attributes: {
        notes: { name: "notes", type: "text" },
        location: { name: "location", type: "GEO_JSONPoint" },
        energyLevel: { name: "energyLevel", type: "positiveNumber", meta: { readOnly: true } },
        power: { name: "power", type: "number", meta: { readOnly: false } },
        powerSetpoint: { name: "powerSetpoint", type: "number" },
        energyCapacity: { name: "energyCapacity", type: "positiveNumber" }
    }
};
export const electricityAsset = {
    name: "Electricity meter",
    type: "ThingAsset",
    realm: "smartcity",
    attributes: {
        notes: { name: "notes", type: "text" },
        location: { name: "location", type: "GEO_JSONPoint" },
        power: { name: "power", type: "number", meta: { readOnly: false } }
    }
};
