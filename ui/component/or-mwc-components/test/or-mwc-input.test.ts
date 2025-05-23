import { ct, expect } from "@openremote/test";

import { OrMwcInput, InputType } from "../src/or-mwc-input";

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
  await component.click();
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
