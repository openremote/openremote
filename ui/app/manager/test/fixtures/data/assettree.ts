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
import type { Asset } from "@openremote/model";

export const parentAssets: Asset[] = [
  {
    name: "City Asset 1",
    type: "CityAsset",
    realm: "smartcity",
    attributes: {
      notes: { name: "notes", type: "text" },
      location: { name: "location", type: "GEO_JSONPoint" },
      country: { name: "country", type: "text" },
    },
  },
  {
    name: "City Asset 2",
    type: "CityAsset",
    realm: "smartcity",
    attributes: {
      notes: { name: "notes", type: "text" },
      location: { name: "location", type: "GEO_JSONPoint" },
      country: { name: "country", type: "text" },
    },
  },
];
export const buildingAsset = {
  name: "Building",
  type: "BuildingAsset",
  realm: "smartcity",
  attributes: {
    area: { name: "area", type: "positiveInteger" },
    city: { name: "city", type: "text" },
    country: { name: "country", type: "text" },
    location: { name: "location", type: "GEO_JSONPoint" },
    notes: { name: "notes", type: "text" },
    postalCode: { name: "postalCode", type: "text" },
    street: { name: "street", type: "text" },
  },
};
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
    energyCapacity: { name: "energyCapacity", type: "positiveNumber" },
  },
};
export const electricityAsset = {
  name: "Electricity meter",
  type: "ThingAsset",
  realm: "smartcity",
  attributes: {
    notes: { name: "notes", type: "text" },
    location: { name: "location", type: "GEO_JSONPoint" },
    power: { name: "power", type: "number", meta: { readOnly: false } },
  },
};
