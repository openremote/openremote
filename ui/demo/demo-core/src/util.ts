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
import { type Asset, type AssetQuery, AssetQueryMatch, WellknownAssets } from "@openremote/model";
import manager from "@openremote/core";

export async function getBuildingAsset(): Promise<Asset | undefined> {
  const query: AssetQuery = {
    names: [
      {
        predicateType: "string",
        match: AssetQueryMatch.EXACT,
        value: "De Rotterdam",
      },
    ],
    types: [WellknownAssets.BUILDINGASSET],
    select: {
      attributes: [],
    },
  };

  const response = await manager.rest.api.AssetResource.queryAssets(query);
  const assets = response.data;

  if (assets.length !== 1) {
    console.log("Failed to retrieve the asset");
    return;
  }
  return assets[0];
}

export async function getElectricityConsumerAsset(): Promise<Asset | undefined> {
  const query: AssetQuery = {
    names: [
      {
        predicateType: "string",
        match: AssetQueryMatch.EXACT,
        value: "Consumption Erasmianum",
      },
    ],
    types: [WellknownAssets.ELECTRICITYCONSUMERASSET],
  };

  const response = await manager.rest.api.AssetResource.queryAssets(query);
  const assets = response.data;

  if (assets.length !== 1) {
    console.log("Failed to retrieve the asset");
    return;
  }
  return assets[0];
}

export async function getElectricityChargerAsset(): Promise<Asset | undefined> {
  const query: AssetQuery = {
    names: [
      {
        predicateType: "string",
        match: AssetQueryMatch.EXACT,
        value: "Charger 1 Markthal",
      },
    ],
    types: [WellknownAssets.ELECTRICITYCHARGERASSET],
  };

  const response = await manager.rest.api.AssetResource.queryAssets(query);
  const assets = response.data;

  if (assets.length !== 1) {
    console.log("Failed to retrieve the asset");
    return;
  }
  return assets[0];
}
