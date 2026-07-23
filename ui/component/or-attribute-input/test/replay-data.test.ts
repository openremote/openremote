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

import { OrJSONForms } from "@openremote/or-json-forms";
import { jsonFormsAttributeRenderers } from "@openremote/or-attribute-input";

const schema = {
    type: "array",
    format: "simulator-replay-data",
    title: "Replay data",
} as any;

// `renderers` is intentionally not part of this helper: `jsonFormsAttributeRenderers`
// must be referenced lexically inside each mount() call so Playwright CT turns the
// import into a browser-side import ref. Referenced anywhere else, the import is
// evaluated in Node, which cannot require() the CSS files or-mwc-components pulls in.
const props = (data: any) => ({
    uischema: { type: "Control", scope: "#" } as any,
    schema,
    data,
    onChange: () => null,
    label: "Replay data",
});

/** Reads the data held by the JSON Forms core (the committed state after change events). */
function getData(component: any) {
    return component.evaluate((el: any) => el.core.data);
}

ct.beforeEach(async ({ shared }) => {
    await shared.fonts();
    await shared.locales();
});

ct("renders datapoints as one 'seconds, value' entry per line", async ({ mount }) => {
    const component = await mount(OrJSONForms, {
        props: {
            ...props([
                { timestamp: 0, value: 21.5 },
                { timestamp: 30, value: "on" },
                { timestamp: 60, value: true },
            ]),
            renderers: jsonFormsAttributeRenderers,
        },
        on: {},
    });

    // Strings render JSON-quoted so they round-trip as strings ("on" vs the bare on).
    await expect(component.getByRole("textbox")).toHaveValue('0, 21.5\n30, "on"\n60, true');
});

ct("commits parsed datapoints on change, preserving value types", async ({ mount }) => {
    const component = await mount(OrJSONForms, {
        props: { ...props([]), renderers: jsonFormsAttributeRenderers },
        on: {},
    });

    const input = component.getByRole("textbox");
    // Covers: number, quoted string of a number (stays a string), boolean,
    // a skipped blank line, and a bare unquoted string (lenient fallback).
    await input.fill('0, 5\n30, "5"\n60, true\n\n90, on');
    await input.blur();

    await expect.poll(() => getData(component)).toEqual([
        { timestamp: 0, value: 5 },
        { timestamp: 30, value: "5" },
        { timestamp: 60, value: true },
        { timestamp: 90, value: "on" },
    ]);
});

ct("flags the first invalid line, keeps the draft and recovers once fixed", async ({ mount }) => {
    const original = [{ timestamp: 0, value: 5 }];
    const component = await mount(OrJSONForms, {
        props: { ...props(original), renderers: jsonFormsAttributeRenderers },
        on: {},
    });

    const input = component.getByRole("textbox");
    await input.fill("0, 5\nabc, 7");
    await input.blur();

    // The error is shown with the offending line number and nothing is committed:
    // the draft (including the bad line) stays in the textarea for correction.
    const textArea = component.locator("or-vaadin-text-area");
    await expect(textArea).toHaveAttribute("invalid");
    await expect(component).toContainText("Invalid entry on line 2");
    await expect(input).toHaveValue("0, 5\nabc, 7");
    await expect.poll(() => getData(component)).toEqual(original);

    // Fixing the line clears the error and commits the parsed datapoints.
    await input.fill("0, 5\n10, 7");
    await input.blur();
    await expect(textArea).not.toHaveAttribute("invalid");
    await expect.poll(() => getData(component)).toEqual([
        { timestamp: 0, value: 5 },
        { timestamp: 10, value: 7 },
    ]);
});

ct("clearing the text removes the data", async ({ mount }) => {
    const component = await mount(OrJSONForms, {
        props: { ...props([{ timestamp: 0, value: 5 }]), renderers: jsonFormsAttributeRenderers },
        on: {},
    });

    const input = component.getByRole("textbox");
    await input.fill("");
    await input.blur();

    await expect.poll(() => getData(component)).toBeUndefined();
});
