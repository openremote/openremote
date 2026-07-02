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

import { OrVaadinToggle } from "@openremote/or-vaadin-components/or-vaadin-toggle";
import { OrVaadinCheckboxGroup } from "@openremote/or-vaadin-components/or-vaadin-checkbox-group";

/*
 * These tests mirror the scenarios in `stories/or-vaadin-toggle.stories.ts`:
 * the Primary story, every variant of the "States & attributes" story, and the
 * "Toggle group" story (vertical + horizontal).
 *
 * Note: the component-test runner (@sand4rt/experimental-ct-web) forwards only
 * `event.detail` to `on` handlers, so the boolean state that comes through is
 * asserted via the `checked-changed` event (detail = `{ value: boolean }`); the
 * native `change` event carries no useful detail so it is only counted.
 */

// --- Primary ---------------------------------------------------------------

ct("Primary: renders label, checked state and helper text", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, {
    props: { label: "Toggle", checked: true, helperText: "Helper text shown below the toggle" },
  });

  await expect(component.getByRole("checkbox", { name: "Toggle" })).toBeChecked();
  await expect(component).toContainText("Toggle");
  await expect(component).toContainText("Helper text shown below the toggle");
  await expect(component).toHaveAttribute("has-helper");
});

// --- Events ----------------------------------------------------------------

ct("Events: change + checked-changed emit the new value on user toggle", async ({ mount }) => {
  const checkedChanges: boolean[] = [];
  let changeCount = 0;

  const component = await mount(OrVaadinToggle, {
    props: { label: "Toggle" },
    on: {
      change: () => {
        changeCount += 1;
      },
      "checked-changed": (detail: { value: boolean }) => {
        checkedChanges.push(detail.value);
      },
    },
  });

  const input = component.getByRole("checkbox", { name: "Toggle" });
  await expect(input).not.toBeChecked();

  // Turn on
  await input.click();
  await expect(input).toBeChecked();

  // Turn off
  await input.click();
  await expect(input).not.toBeChecked();

  // Vaadin emits an initial checked-changed(false) at mount, so assert on the user-driven
  // transitions (the last two values) and that `change` only fires on interaction.
  await expect.poll(() => changeCount).toBe(2);
  await expect.poll(() => checkedChanges.slice(-2)).toEqual([true, false]);
});

// --- States & attributes ---------------------------------------------------

ct("State: off (default) is unchecked", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { label: "Off (default)" } });
  await expect(component.getByRole("checkbox", { name: "Off (default)" })).not.toBeChecked();
});

ct("State: on is checked", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { label: "On", checked: true } });
  await expect(component.getByRole("checkbox", { name: "On" })).toBeChecked();
});

ct("State: disabled cannot be toggled and emits nothing", async ({ mount }) => {
  const checkedChanges: boolean[] = [];
  const component = await mount(OrVaadinToggle, {
    props: { label: "Disabled", disabled: true },
    on: { "checked-changed": (detail: { value: boolean }) => checkedChanges.push(detail.value) },
  });

  const input = component.getByRole("checkbox", { name: "Disabled" });
  await expect(input).toBeDisabled();
  await input.click({ force: true });
  await expect(input).not.toBeChecked();
  // Only the initial checked-changed(false) may be present; interaction must never switch it on.
  expect(checkedChanges).not.toContain(true);
});

ct("State: disabled + on stays checked", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { label: "Disabled on", checked: true, disabled: true } });
  const input = component.getByRole("checkbox", { name: "Disabled on" });
  await expect(input).toBeDisabled();
  await expect(input).toBeChecked();
});

ct("State: readonly does not change on click and emits nothing", async ({ mount }) => {
  const checkedChanges: boolean[] = [];
  const component = await mount(OrVaadinToggle, {
    props: { label: "Readonly", readonly: true },
    on: { "checked-changed": (detail: { value: boolean }) => checkedChanges.push(detail.value) },
  });

  const input = component.getByRole("checkbox", { name: "Readonly" });
  await input.click({ force: true });
  await expect(input).not.toBeChecked();
  // Only the initial checked-changed(false) may be present; interaction must never switch it on.
  expect(checkedChanges).not.toContain(true);
});

ct("State: readonly + on stays checked", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { label: "Readonly on", checked: true, readonly: true } });
  await expect(component.getByRole("checkbox", { name: "Readonly on" })).toBeChecked();
});

ct("State: indeterminate reflects the indeterminate attribute", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { label: "Indeterminate", indeterminate: true } });
  await expect(component).toHaveAttribute("indeterminate");
});

ct("State: required reflects the required attribute", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { label: "Required", required: true } });
  await expect(component).toHaveAttribute("required");
});

ct("State: renders without a label", async ({ mount }) => {
  const component = await mount(OrVaadinToggle, { props: { checked: true } });
  await expect(component.getByRole("checkbox")).toBeChecked();
  await expect(component).not.toHaveAttribute("has-label");
});

// --- Toggle group ----------------------------------------------------------

ct("Group (vertical): renders grouped toggles with their checked state", async ({ mount }) => {
  const component = await mount(OrVaadinCheckboxGroup, {
    props: { label: "Notifications", theme: "vertical" },
    slots: {
      // The default slot must be an array: the CT runner keeps only `fragment.firstChild` per string.
      default: [
        '<or-vaadin-toggle label="Email" value="email" checked></or-vaadin-toggle>',
        '<or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>',
        '<or-vaadin-toggle label="Push" value="push" checked></or-vaadin-toggle>',
      ],
    },
  });

  await expect(component).toContainText("Notifications");
  await expect(component.locator("or-vaadin-toggle")).toHaveCount(3);
  await expect(component.locator('or-vaadin-toggle[value="email"]')).toHaveJSProperty("checked", true);
  await expect(component.locator('or-vaadin-toggle[value="sms"]')).toHaveJSProperty("checked", false);
  await expect(component.locator('or-vaadin-toggle[value="push"]')).toHaveJSProperty("checked", true);
});

ct("Group (vertical): a child toggle switches independently when clicked", async ({ mount }) => {
  // Note: Vaadin's `checked-changed` is dispatched without `bubbles`/`composed`, so it does not
  // reach a group-level listener - the per-toggle event flow is covered by the "Events" test above.
  const component = await mount(OrVaadinCheckboxGroup, {
    props: { label: "Notifications", theme: "vertical" },
    slots: {
      default: [
        '<or-vaadin-toggle label="Email" value="email"></or-vaadin-toggle>',
        '<or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>',
      ],
    },
  });

  const email = component.locator('or-vaadin-toggle[value="email"]');
  const sms = component.locator('or-vaadin-toggle[value="sms"]');
  await email.getByRole("checkbox").click();
  await expect(email).toHaveJSProperty("checked", true);
  await expect(sms).toHaveJSProperty("checked", false);
});

ct("Group (horizontal): renders grouped toggles in a row layout", async ({ mount }) => {
  const component = await mount(OrVaadinCheckboxGroup, {
    props: { label: "Notifications" },
    slots: {
      default: [
        '<or-vaadin-toggle label="Email" value="email" checked></or-vaadin-toggle>',
        '<or-vaadin-toggle label="SMS" value="sms"></or-vaadin-toggle>',
        '<or-vaadin-toggle label="Push" value="push" checked></or-vaadin-toggle>',
      ],
    },
  });

  await expect(component.locator("or-vaadin-toggle")).toHaveCount(3);

  // Under the theme the group is horizontal by default: the toggles share a row (same top edge).
  const first = await component.locator('or-vaadin-toggle[value="email"]').boundingBox();
  const second = await component.locator('or-vaadin-toggle[value="sms"]').boundingBox();
  expect(first).not.toBeNull();
  expect(second).not.toBeNull();
  expect(Math.abs(first!.y - second!.y)).toBeLessThan(4);
  expect(second!.x).toBeGreaterThan(first!.x);
});
