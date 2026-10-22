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
import { ct, expect } from "./fixtures";

import { OrVaadinTab } from "@openremote/or-vaadin-components/or-vaadin-tab";
import { OrVaadinTabs } from "@openremote/or-vaadin-components/or-vaadin-tabs";

const tabs = [
  "<or-vaadin-tab>Status</or-vaadin-tab>",
  "<or-vaadin-tab>Mode</or-vaadin-tab>",
  "<or-vaadin-tab>Settings</or-vaadin-tab>",
];

ct("should select the clicked tab", async ({ mount }) => {
  const selected: number[] = [];
  const component = await mount(OrVaadinTabs, {
    props: { selected: 1 },
    slots: { default: tabs },
    on: {
      "selected-changed": (detail: { value: number }) => {
        selected.push(detail.value);
      },
    },
    hooksConfig: { components: [OrVaadinTab] },
  });

  await expect(component.getByRole("tab", { selected: true })).toHaveText("Mode");

  await component.getByRole("tab", { name: "Settings" }).click();

  await expect(component.getByRole("tab", { selected: true })).toHaveText("Settings");
  await expect.poll(() => selected.at(-1)).toBe(2);
});

ct("should move the indicator behind the selected tab", async ({ mount }) => {
  const component = await mount(OrVaadinTabs, {
    props: { selected: 0 },
    slots: { default: tabs },
    hooksConfig: { components: [OrVaadinTab] },
  });
  const indicator = component.locator("#indicator");
  const settings = component.getByRole("tab", { name: "Settings" });

  await settings.click();

  // The indicator slides, so poll until it has settled on the tab
  await expect
    .poll(async () => {
      const [indicatorBox, tabBox] = await Promise.all([indicator.boundingBox(), settings.boundingBox()]);
      return indicatorBox && tabBox && [indicatorBox.x - tabBox.x, indicatorBox.width - tabBox.width].map(Math.round);
    })
    .toEqual([0, 0]);
});

ct("should hide the indicator when no tab is selected", async ({ mount }) => {
  const component = await mount(OrVaadinTabs, {
    props: { selected: -1 },
    slots: { default: tabs },
    hooksConfig: { components: [OrVaadinTab] },
  });

  await expect(component.locator("#indicator")).toBeHidden();
});
