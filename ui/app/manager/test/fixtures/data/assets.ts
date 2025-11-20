import * as Util from "@openremote/core/lib/util";
import { Asset, AssetTypeInfo } from "@openremote/model";

export const notes = { name: "notes", type: "text" };
export const location = { name: "location", type: "GEO_JSONPoint" };
export const commonAttrs = { notes, location };

export const assetMap = {
    Battery: "ElectricityBatteryAsset",
    "Solar Panel": "ElectricityProducerSolarAsset",
};
const assets = [
    {
        name: "Battery",
        type: "ThingAsset",
        realm: "smartcity",
        attributes: {
            ...commonAttrs,
            energyLevel: { name: "energyLevel", type: "positiveNumber", meta: { readOnly: true } },
            power: { name: "power", type: "number", meta: { readOnly: false } },
            powerSetpoint: { name: "powerSetpoint", type: "number" },
            energyCapacity: { name: "energyCapacity", type: "positiveNumber" },
        },
    },
    {
        name: "Solar Panel",
        type: "ThingAsset",
        realm: "smartcity",
        attributes: {
            ...commonAttrs,
            panelPitch: { name: "panelPitch", type: "positiveInteger", meta: { readOnly: true } },
            power: { name: "power", type: "number", meta: { readOnly: false } },
            powerForecast: { name: "powerForecast", type: "number", meta: { readOnly: true } },
        },
    },
] as const;

export const preparedAssetsWithLocation = Object.values(
    Util.mergeObjects(
        structuredClone(assets),
        [
            {
                attributes: {
                    location: { value: { type: "Point", coordinates: [4.482259693115793, 51.91756799273] } },
                },
            },
            {
                attributes: {
                    location: { value: { type: "Point", coordinates: [4.4845127486877345, 51.917435642781214] } },
                },
            },
        ],
        true
    )
);

export const preparedAssetsForRules = Object.values(
    Util.mergeObjects(
        structuredClone(assets),
        [
            {
                attributes: {
                    energyLevel: { meta: { ruleState: true, storeDataPoints: true } },
                    power: { meta: { ruleState: true, storeDataPoints: true } },
                },
            },
            {
                attributes: {
                    power: { meta: { ruleState: true, storeDataPoints: true } },
                    powerForecast: { meta: { ruleState: true, storeDataPoints: true } },
                },
            },
        ],
        true
    )
);

type AssetNames = (typeof assets)[number]["name"];

export const assetPatches: Record<
    AssetNames,
    {
        attribute1: string;
        attribute2: string;
        attribute3: string;
        value1: string;
        value2: string;
        value3: string;
        x: number;
        y: number;
    }
> = {
    Battery: {
        attribute1: "energyLevel",
        attribute2: "power",
        attribute3: "powerSetpoint",
        value1: "30",
        value2: "50",
        value3: "70",
        x: 705,
        y: 210,
    },
    "Solar Panel": {
        attribute1: "panelPitch",
        attribute2: "power",
        attribute3: "powerForecast",
        value1: "30",
        value2: "70",
        value3: "100",
        x: 600,
        y: 200,
    },
};

export type DefaultAssets = typeof assets;
export default assets;

export const thing: Asset = {
    name: "Thing",
    realm: "smartcity",
    type: "ThingAsset",
    attributes: { ...commonAttrs },
};

export const agent: Asset = {
    name: "Simulator",
    realm: "smartcity",
    type: "SimulatorAgent",
    attributes: {
        ...commonAttrs,
        agentDisabled: { name: "agentDisabled", type: "boolean", meta: {} },
        agentStatus: { name: "agentStatus", type: "connectionStatus", meta: { readOnly: true } },
    },
};

export type BBox = { south: number; north: number; west: number; east: number };

const rotterdam: BBox = {
    south: 51.89,
    north: 51.99,
    west: 4.24,
    east: 4.51,
};

/**
 * Generates assets with random location and asset types
 *
 * @param asset The asset to assign the location attribute
 * @param boundingBox The bounding box to add assets within
 * @default {@link rotterdam}
 */
export function assignLocation(asset: Asset, { south, north, east, west }: BBox = rotterdam): Asset {
    const y = randomBetween(south, north);
    const x = randomBetween(east, west);

    Object.assign(asset?.attributes ?? {}, {
        location: {
            name: "location",
            type: "GEO_JSONPoint",
            value: { type: "Point", coordinates: [x, y] },
            meta: {},
        },
    });

    return asset;
}

export function randomAsset(assetInfos: AssetTypeInfo[]): Asset {
    const validAssetInfos = assetInfos.filter(
        ({ assetDescriptor }) => assetDescriptor?.name && assetDescriptor.descriptorType === "asset"
    );

    const randomIndex = Math.round(randomBetween(0, validAssetInfos.length - 1));
    const info = validAssetInfos[randomIndex];

    const type = info.assetDescriptor!.name;
    const attributes = Object.fromEntries(
        Object.values(info?.attributeDescriptors ?? {}).map(({ name, type }) => [name, { name, type, meta: {} }])
    );

    return { type, name: type, attributes };
}

export function getAssetTypes(assets: Asset[]) {
    return assets.map(({ type }) => type!).filter((value, index, array) => array.indexOf(value) === index);
}

export function getAssetTypeColour(type: string, infos: AssetTypeInfo[]) {
    return infos.find(({ assetDescriptor }) => assetDescriptor?.name === type)?.assetDescriptor?.colour ?? "";
}

export function rgbToHex(rgb: string[]) {
    return rgb.map((i) => ("0" + parseInt(i).toString(16)).slice(-2)).join("");
}

function randomBetween(max: number, min: number) {
    return Math.random() * (max - min) + min;
}
