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
import { ct, expect } from "@openremote/test";
import { LifecycleElement } from "./fixtures/elements.js";

type Relocate = HTMLElement & {
  relocate(how: "move" | "reinsert"): Promise<{ supported: boolean; connected: number; disconnected: number }>;
};

ct("should be left alone by a state-preserving move", async ({ mount }) => {
  const element = await mount(LifecycleElement);
  const result = await element.evaluate((el) => (el as Relocate).relocate("move"));
  expect(result.supported).toBe(true);
  // Without connectedMoveCallback the platform tears the component down and sets it up again,
  // which is what made moved widgets refetch their contents
  expect({ connected: result.connected, disconnected: result.disconnected }).toEqual({
    connected: 1,
    disconnected: 0,
  });
});

ct("should be torn down and set up again by a plain re-insertion", async ({ mount }) => {
  const element = await mount(LifecycleElement);
  const result = await element.evaluate((el) => (el as Relocate).relocate("reinsert"));
  expect({ connected: result.connected, disconnected: result.disconnected }).toEqual({
    connected: 2,
    disconnected: 1,
  });
});
