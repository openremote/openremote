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
export const markers = [
  {
    attributeName: "onOff",
    showLabel: false,
    showUnits: false,
    colours: {
      type: "boolean",
      true: "fce61e",
      false: "000000",
    },
  },
  {
    attributeName: "temperature",
    showLabel: true,
    showUnits: true,
    colours: {
      type: "range",
      ranges: [
        {
          min: 0,
          colour: "39b54a",
        },
        {
          min: 30,
          colour: "f7931e",
        },
        {
          min: 40,
          colour: "c1272d",
        },
      ],
    },
  },
] as const;
