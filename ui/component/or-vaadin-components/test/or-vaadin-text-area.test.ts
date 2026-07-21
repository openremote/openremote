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
  ct("fixes the input area to --or-vaadin-text-area-height and scrolls instead of growing", async ({ mount }) => {
    const component = await mount(OrVaadinTextArea, { props: { label: "Text" } });
    await component.evaluate((el) => el.style.setProperty("--or-vaadin-text-area-height", "108px"));

    const input = component.getByRole("textbox", { name: "Text" });
    await input.fill(lines(30));

    // The textarea starts at the CSS variable and scrolls the overflowing
    // content; the container wraps it without a second scrollbar.
    const inputField = component.locator("[part~='input-field']");
    await expect(input).toHaveCSS("height", "108px");
    await expect(input).toHaveCSS("overflow-y", "auto");
    await expect(inputField).toHaveCSS("overflow-y", "hidden");

    // The host must not auto-grow when more content is added.
    const before = (await component.boundingBox())!.height;
    await input.fill(lines(60));
    await expect(input).toHaveCSS("height", "108px");
    const after = (await component.boundingBox())!.height;
    expect(after).toBe(before);

    // Fixed-height mode enables the native resize handle by default,
    // floored at the default height.
    await expect(input).toHaveCSS("resize", "vertical");
    await expect(input).toHaveCSS("min-height", "108px");

    // A native resize drag writes an inline height on the textarea;
    // later value changes must not snap it back to the CSS variable.
    await input.evaluate((el) => { el.style.height = "208px"; });
    await input.fill(lines(90));
    await expect(input).toHaveCSS("height", "208px");
  });

  ct("the resize handle drags the textarea taller but not below the floor", async ({ mount, page, shared }) => {
    const component = await mount(OrVaadinTextArea, { props: { label: "Text" } });
    await component.evaluate((el) => el.style.setProperty("--or-vaadin-text-area-height", "108px"));

    const input = component.getByRole("textbox", { name: "Text" });
    // Overflowing content matters: the scrollbar historically competed with the
    // handle (and on the input container the slotted textarea covered it entirely).
    await input.fill(lines(30));
    await expect(input).toHaveCSS("height", "108px");

    // Drag the native handle (bottom-right corner of the textarea) 100px down.
    const box = (await input.boundingBox())!;
    await page.mouse.move(box.x + box.width - 4, box.y + box.height - 4);
    await shared.drag(box.x + box.width - 4, box.y + box.height + 96);
    await expect.poll(async () => (await input.boundingBox())!.height).toBeGreaterThan(180);

    // Drag far upwards: min-height must stop the shrink at the default height.
    const grown = (await input.boundingBox())!;
    await page.mouse.move(grown.x + grown.width - 4, grown.y + grown.height - 4);
    await shared.drag(grown.x + grown.width - 4, grown.y - 300);
    await expect.poll(async () => (await input.boundingBox())!.height).toBe(108);
  });

  ct("resize=false removes the native resize handle", async ({ mount }) => {
    const component = await mount(OrVaadinTextArea, { props: { label: "Text", resize: false } });
    await component.evaluate((el) => el.style.setProperty("--or-vaadin-text-area-height", "108px"));

    const input = component.getByRole("textbox", { name: "Text" });
    await input.fill(lines(5));

    await expect(input).toHaveCSS("resize", "none");
    await expect(input).toHaveCSS("height", "108px");
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
