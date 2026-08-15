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

import { OrMwcInput, InputType } from "@openremote/or-mwc-components/or-mwc-input";

ct("Button should trigger or-mwc-input-changed event", async ({ mount }) => {
  let clicked = false;
  const component = await mount(OrMwcInput, {
    props: {
      type: InputType.BUTTON,
      raised: true,
      label: "button",
    },
    on: {
      "or-mwc-input-changed": () => (clicked = true),
    },
  });
  expect(clicked).toBeFalsy();
  await component.click();
  expect(clicked).toBeTruthy();
});

ct("Switch should switch", async ({ mount }) => {
  const component = await mount(OrMwcInput, {
    props: {
      type: InputType.SWITCH,
      label: "switch",
    },
  });
  const locator = component.getByRole("switch", { name: "switch" });
  await expect(locator).not.toBeChecked();
  await component.getByText("switch").click();
  await expect(locator).toBeChecked();
});

ct("Input should have text value", async ({ mount }) => {
  const component = await mount(OrMwcInput, {
    props: {
      type: InputType.TEXT,
      label: "text",
    },
  });
  const locator = component.getByRole("textbox", { name: "text" });
  await expect(locator).toHaveValue("");
  await locator.fill("input");
  await expect(locator).toHaveValue("input");
});
