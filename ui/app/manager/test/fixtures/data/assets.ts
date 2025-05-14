import { Asset } from "@openremote/model";
// TODO: use Util.mergeObjects from @openremote/core instead when DOM related utilities and or-icon are moved out
import { merge } from "lodash-es";

export const preparedAssets: Asset[] = [
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
      power: { name: "power", type: "number", meta: { readOnly: true } },
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
      panelPitch: { name: "panelPitch", type: "positiveInteger" },
      includeForecastSolarService: { name: "includeForecastSolarService", type: "boolean" },
      power: { name: "power", type: "number", meta: { readOnly: true } },
      setActualSolarValueWithForecast: { name: "setActualSolarValueWithForecast", type: "boolean" },
      powerForecast: { name: "powerForecast", type: "number", meta: { readOnly: true } },
      panelOrientation: { name: "panelOrientation", type: "panelOrientation" },
      energyExportTotal: { name: "energyExportTotal", type: "positiveNumber", meta: { readOnly: true } },
      powerExportMax: { name: "powerExportMax", type: "positiveNumber" },
      location: { name: "location", type: "GEO_JSONPoint" },
    },
  },
];

export const preparedAssetsWithLocation = merge(structuredClone(preparedAssets), [
  { attributes: { location: { value: { type: "Point", coordinates: [4.482259693115793, 51.91756799273] } } } },
  { attributes: { location: { value: { type: "Point", coordinates: [4.4845127486877345, 51.917435642781214] } } } },
]) as Asset[];

export const preparedAssetsWithReadonly = merge(structuredClone(preparedAssets), [
  {},
  { attributes: { panelPitch: { meta: { readOnly: true } } } },
]) as Asset[];

export const preparedAssetsForRules = merge(structuredClone(preparedAssets), [
  {
    attributes: {
      energyLevel: { meta: { readOnly: true, ruleState: true, storeDataPoints: true } },
      power: { meta: { readOnly: true, ruleState: true, storeDataPoints: true } },
    },
  },
  {
    attributes: {
      power: { meta: { readOnly: true, ruleState: true, storeDataPoints: true } },
      powerForecast: { meta: { readOnly: true, ruleState: true, storeDataPoints: true } },
    },
  },
]) as Asset[];

export default [
  {
    asset: "Electricity battery asset",
    name: "Battery",
    attr_1: "energyLevel",
    attr_2: "power",
    attr_3: "powerSetpoint",
    a1_type: "Positive number",
    a2_type: "Number",
    a3_type: "number",
    v1: "30",
    v2: "50",
    v3: "70",
    location_x: 705,
    location_y: 210,
    config_item_1: "Rule state",
    config_item_2: "Store data points",
    config_attr_1: "energyLevel",
    config_attr_2: "power",
  },
  {
    asset: "PV solar asset",
    name: "Solar Panel",
    attr_1: "panelPitch",
    attr_2: "power",
    attr_3: "powerForecast",
    a1_type: "Positive integer",
    a2_type: "Number",
    a3_type: "number",
    v1: "30",
    v2: "70",
    v3: "100",
    location_x: 600,
    location_y: 200,
    config_item_1: "Rule state",
    config_item_2: "Store data points",
    config_attr_1: "power",
    config_attr_2: "powerForecast",
  },
];
