/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { ct, expect } from "@openremote/test";

import { OrVaadinTextArea } from "@openremote/or-vaadin-components/or-vaadin-text-area";

function lines(count: number): string {
  return Array.from({ length: count }, (_, i) => `line ${i + 1}`).join("\n");
}

ct.describe("Submit", () => {
  // The `submit` CustomEvent carries no detail, so it can only be counted.
  ct("enter dispatches a submit event", async ({ mount }) => {
    let submitCount = 0;
    const component = await mount(OrVaadinTextArea, {
      props: { label: "Text" },
      on: {
        submit: () => {
          submitCount += 1;
        },
      },
    });

    const input = component.getByRole("textbox", { name: "Text" });
    await input.fill("first");
    await input.press("Enter");

    await expect.poll(() => submitCount).toBe(1);
    // Enter must still insert the newline (default keydown behavior is preserved).
    await expect(input).toHaveValue("first\n");
  });

  ct("shift+enter inserts a newline without submitting", async ({ mount }) => {
    let submitCount = 0;
    const component = await mount(OrVaadinTextArea, {
      props: { label: "Text" },
      on: {
        submit: () => {
          submitCount += 1;
        },
      },
    });

    const input = component.getByRole("textbox", { name: "Text" });
    await input.fill("first");
    await input.press("Shift+Enter");
    await input.pressSequentially("second");

    await expect(input).toHaveValue("first\nsecond");
    expect(submitCount).toBe(0);
  });
});

ct.describe("Height", () => {
  ct("fixes the input area to --or-text-area-height and scrolls instead of growing", async ({ mount }) => {
    const component = await mount(OrVaadinTextArea, { props: { label: "Text" } });
    await component.evaluate((el) => el.style.setProperty("--or-text-area-height", "108px"));

    const input = component.getByRole("textbox", { name: "Text" });
    await input.fill(lines(30));

    // The input container is pinned to the CSS variable and hides its own
    // scrollbar; the textarea fills it and scrolls the overflowing content.
    const inputField = component.locator("[part~='input-field']");
    await expect(inputField).toHaveCSS("height", "108px");
    await expect(inputField).toHaveCSS("overflow-y", "hidden");
    await expect(input).toHaveCSS("overflow-y", "auto");

    // The host must not auto-grow when more content is added.
    const before = (await component.boundingBox())!.height;
    await input.fill(lines(60));
    await expect(inputField).toHaveCSS("height", "108px");
    const after = (await component.boundingBox())!.height;
    expect(after).toBe(before);
  });

  ct("keeps Vaadin's auto-grow behavior without the CSS variable", async ({ mount }) => {
    const component = await mount(OrVaadinTextArea, { props: { label: "Text" } });

    const input = component.getByRole("textbox", { name: "Text" });
    await input.fill(lines(1));
    const singleLine = (await component.boundingBox())!.height;

    await input.fill(lines(10));
    await expect.poll(async () => (await component.boundingBox())!.height).toBeGreaterThan(singleLine);
  });
});
