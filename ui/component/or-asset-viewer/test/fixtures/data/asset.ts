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

export const notes = { name: "notes", type: "text" };
export const location = { name: "location", type: "GEO_JSONPoint" };
export const commonAttrs = { notes, location };
export const validAsset: Asset = {
  id: "validAsset",
  name: "Thing",
  realm: "master",
  type: "ThingAsset",
  attributes: { ...commonAttrs },
};
export const configuredAsset: Asset = {
    id: "configuredAsset",
    name: "Configured Thing",
    realm: "master",
    type: "ThingAsset",
    attributes: {
        notes: { ...notes, meta: { readOnly: true } },
        model: { name: "model", type: "text", meta: { label: "Model" } },
        location,
    },
};
export const invalidAsset: Asset = {
  id: "invalidAsset",
  name: "Thing",
  realm: "master",
  type: "ThingAsset",
  attributes: { ...commonAttrs, invalid: { name: "invalid", type: "integer" } },
};
