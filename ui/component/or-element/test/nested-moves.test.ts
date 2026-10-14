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
import { NestedMoveFixture } from "./fixtures/elements.js";

type Nested = HTMLElement & {
  run(how: "move" | "reinsert"): Promise<{
    supported: boolean;
    order: number;
    connected: number;
    disconnected: number;
  }>;
};

ct("should leave a nested component alone when its wrapper is moved", async ({ mount }) => {
  const fixture = await mount(NestedMoveFixture);
  const result = await fixture.evaluate((element) => (element as Nested).run("move"));
  expect({ supported: result.supported, order: result.order }).toEqual({ supported: true, order: 1 });
  // A grid re-orders the wrapper divs, not the components, so descendants have to be spared too
  expect({ connected: result.connected, disconnected: result.disconnected }).toEqual({
    connected: 1,
    disconnected: 0,
  });
});

ct("should tear a nested component down when its wrapper is re-inserted", async ({ mount }) => {
  const fixture = await mount(NestedMoveFixture);
  const result = await fixture.evaluate((element) => (element as Nested).run("reinsert"));
  expect({ connected: result.connected, disconnected: result.disconnected }).toEqual({
    connected: 2,
    disconnected: 1,
  });
});
