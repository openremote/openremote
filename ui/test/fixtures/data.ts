/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
  applyPredictedDataPoints: {
    name: "applyPredictedDataPoints",
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
  bigNumber: {
    name: "bigNumber",
    type: "java.math.BigDecimal",
    jsonType: "number",
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
  timestampISO8601: {
    name: "timestampISO8601",
    type: "java.lang.String",
    constraints: [
      {
        type: "pattern",
        regexp:
          "^(?:[1-9]\\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:[\\.,]\\d{1,9})?(?:Z|[+-][01]\\d:[0-5]\\d)$",
        message: "{ValueConstraint.Pattern.message}",
      },
    ],
    format: {
      asDate: true,
      momentJsFormat: "DD-MMM-YYYY hh:mm A",
    },
    jsonType: "string",
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
  number: {
    name: "number",
    type: "java.lang.Double",
    jsonType: "number",
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
  bigInteger: {
    name: "bigInteger",
    type: "java.math.BigInteger",
    jsonType: "bigint",
  },
  colourRGB: {
    name: "colourRGB",
    type: "org.openremote.model.value.impl.ColourRGB",
    jsonType: "object",
  },
  JSONObject: {
    name: "JSONObject",
    type: "org.openremote.model.value.ValueType$ObjectMap",
    jsonType: "object",
  },
  JSON: {
    name: "JSON",
    type: "java.lang.Object",
    jsonType: "unknown",
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
  GEO_JSONPoint: {
    name: "GEO_JSONPoint",
    type: "org.openremote.model.geo.GeoJSONPoint",
    jsonType: "object",
  },
  boolean: {
    name: "boolean",
    type: "java.lang.Boolean",
    jsonType: "boolean",
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
};

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
  metaItemDescriptors: Object.keys(metaItemDescriptors),
  valueDescriptors: Object.keys(valueDescriptors),
};
