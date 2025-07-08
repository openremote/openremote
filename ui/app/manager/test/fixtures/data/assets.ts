import * as Util from "@openremote/core/lib/util";
import { Asset } from "@openremote/model";

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
      notes: { name: "notes", type: "text" },
      location: { name: "location", type: "GEO_JSONPoint" },
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
      notes: { name: "notes", type: "text" },
      location: { name: "location", type: "GEO_JSONPoint" },
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
      { attributes: { location: { value: { type: "Point", coordinates: [4.482259693115793, 51.91756799273] } } } },
      { attributes: { location: { value: { type: "Point", coordinates: [4.4845127486877345, 51.917435642781214] } } } },
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
  attributes: {
    notes: { name: "notes", type: "text", meta: {} },
    location: { name: "location", type: "GEO_JSONPoint", meta: {} },
  },
};

export const agent: Asset = {
  name: "Simulator",
  realm: "smartcity",
  type: "SimulatorAgent",
  attributes: {
    notes: { name: "notes", type: "text", meta: {} },
    agentDisabled: { name: "agentDisabled", type: "boolean", meta: {} },
    location: { name: "location", type: "GEO_JSONPoint", meta: {} },
    agentStatus: { name: "agentStatus", type: "connectionStatus", meta: { readOnly: true } },
  },
};
