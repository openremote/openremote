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
import { ct, expect, type SharedComponentTestFixtures } from "@openremote/test";
import { MapReattachFixture } from "./fixtures/map-reattach.js";

type Mount = SharedComponentTestFixtures["mount"];
type Fixture = HTMLElement & {
  reattach(): Promise<{ afterFirstRender: number; afterReattach: number }>;
  remove(): Promise<{ unloaded: boolean; kept: boolean }>;
};

async function run<K extends "reattach" | "remove">(mount: Mount, step: K) {
  const fixture = await mount(MapReattachFixture);
  return fixture.evaluate((element, args) => (element as Fixture)[args.step as K](), { step });
}

ct("should set the map up again after the element is reattached", async ({ mount }) => {
  // Setup used to run only on the first render, so a map detached by a DOM move never came back
  const { afterFirstRender, afterReattach } = (await run(mount, "reattach")) as {
    afterFirstRender: number;
    afterReattach: number;
  };
  expect(afterReattach).toBeGreaterThan(afterFirstRender);
  expect(afterFirstRender).toBeGreaterThan(0);
});

ct("should drop the unloaded map when the element is removed", async ({ mount }) => {
  const { unloaded, kept } = (await run(mount, "remove")) as { unloaded: boolean; kept: boolean };
  expect(unloaded).toBe(true);
  // Holding on to an unloaded map is what stopped a rebuild from happening
  expect(kept).toBe(false);
});
