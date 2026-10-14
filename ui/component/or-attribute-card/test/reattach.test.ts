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
import { CardReattachFixture } from "./fixtures/card-reattach.js";

type Reattach = HTMLElement & { reattach(): Promise<{ before: number; after: number }> };

ct("should draw the data it holds again after being re-inserted", async ({ mount }) => {
  const fixture = await mount(CardReattachFixture);
  const result = await fixture.evaluate((element) => (element as Reattach).reattach());

  expect(result.before).toBe(3);
  // The chart was destroyed on detach and only built again once some property changed
  expect(result.after).toBe(3);
});
