// TODO: use Util.mergeObjects from @openremote/core instead when DOM related utilities and or-icon are moved out
import { merge } from "lodash-es";

const assets = [
  {
    name: "Battery",
    type: "ElectricityBatteryAsset",
    realm: "smartcity",
    attributes: {
      energyLevelPercentage: { name: "energyLevelPercentage", type: "positiveInteger", meta: { readOnly: true } },
      notes: { name: "notes", type: "text" },
      energyLevelPercentageMin: { name: "energyLevelPercentageMin", type: "positiveInteger" },
      supportsExport: { name: "supportsExport", type: "boolean" },
      efficiencyImport: { name: "efficiencyImport", type: "positiveInteger" },
      energyCapacity: { name: "energyCapacity", type: "positiveNumber" },
      powerSetpoint: { name: "powerSetpoint", type: "number" },
      energyLevel: { name: "energyLevel", type: "positiveNumber", meta: { readOnly: true } },
      efficiencyExport: { name: "efficiencyExport", type: "positiveInteger" },
      power: { name: "power", type: "number", meta: { readOnly: false } },
      supportsImport: { name: "supportsImport", type: "boolean" },
      powerImportMax: { name: "powerImportMax", type: "positiveNumber" },
      energyImportTotal: { name: "energyImportTotal", type: "positiveNumber", meta: { readOnly: true } },
      forceCharge: { name: "forceCharge", type: "executionStatus" },
      energyExportTotal: { name: "energyExportTotal", type: "positiveNumber", meta: { readOnly: true } },
      energyLevelPercentageMax: { name: "energyLevelPercentageMax", type: "positiveInteger" },
      powerExportMax: { name: "powerExportMax", type: "positiveNumber" },
      location: { name: "location", type: "GEO_JSONPoint" },
    },
  },
  {
    name: "Solar Panel",
    type: "ElectricityProducerSolarAsset",
    realm: "smartcity",
    attributes: {
      notes: { name: "notes", type: "text" },
      panelAzimuth: { name: "panelAzimuth", type: "integer" },
      panelPitch: { name: "panelPitch", type: "positiveInteger", meta: { readOnly: true } },
      includeForecastSolarService: { name: "includeForecastSolarService", type: "boolean" },
      power: { name: "power", type: "number", meta: { readOnly: false } },
      setActualSolarValueWithForecast: { name: "setActualSolarValueWithForecast", type: "boolean" },
      powerForecast: { name: "powerForecast", type: "number", meta: { readOnly: true } },
      panelOrientation: { name: "panelOrientation", type: "panelOrientation" },
      energyExportTotal: { name: "energyExportTotal", type: "positiveNumber", meta: { readOnly: true } },
      powerExportMax: { name: "powerExportMax", type: "positiveNumber" },
      location: { name: "location", type: "GEO_JSONPoint" },
    },
  },
] as const;

export const preparedAssetsWithLocation = merge(structuredClone(assets), [
  { attributes: { location: { value: { type: "Point", coordinates: [4.482259693115793, 51.91756799273] } } } },
  { attributes: { location: { value: { type: "Point", coordinates: [4.4845127486877345, 51.917435642781214] } } } },
]);

export const preparedAssetsForRules = merge(structuredClone(assets), [
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
]);

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
