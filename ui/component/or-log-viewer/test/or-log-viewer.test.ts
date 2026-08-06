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

import { OrLogViewerTest } from "./fixtures/or-log-viewer-test.js";

const NOW = new Date(2026, 0, 15, 8, 0, 0);
const step = (icon: string) => `#ending-controls or-vaadin-button:has(or-icon[icon="${icon}"])`;

ct.beforeEach(async ({ page, shared }) => {
  await page.clock.setFixedTime(NOW);
  await shared.locales();
  await shared.fonts();
});

ct("should show the timestamp the logs end at", async ({ mount }) => {
  const component = await mount(OrLogViewerTest);

  await expect(component.locator("#ending-date")).toHaveAttribute("value", "2026-01-15T10:30");
});

ct("should step the ending timestamp by the selected period", async ({ mount }) => {
  const component = await mount(OrLogViewerTest);
  const endingDate = component.locator("#ending-date");

  await component.locator(step("chevron-right")).click();
  // The forward step used to be swallowed by the preceding attribute, leaving the button without a
  // click handler and shifting every binding after it onto the wrong part of the template
  await expect(endingDate).toHaveAttribute("value", "2026-01-15T11:30");

  await component.locator(step("chevron-left")).click();
  await expect(endingDate).toHaveAttribute("value", "2026-01-15T10:30");
});

ct("should return the ending timestamp to the current time", async ({ mount }) => {
  const component = await mount(OrLogViewerTest);

  await component.locator(step("chevron-double-right")).click();

  await expect(component.locator("#ending-date")).toHaveAttribute("value", "2026-01-15T08:00");
});

ct("should list the returned log events", async ({ mount }) => {
  const component = await mount(OrLogViewerTest);
  const rows = component.getByRole("table", { name: "logs list" }).getByRole("row");

  await expect(rows).toHaveCount(3); // header + 2 events
  await expect(rows.nth(1)).toContainText("Attribute updated");
  await expect(rows.nth(2)).toContainText("Connection refused");
});
