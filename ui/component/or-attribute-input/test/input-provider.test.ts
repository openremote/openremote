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

import { ProviderProbe } from "./fixtures/provider-probe.js";

// WellknownValueTypes string values, inlined to keep @openremote/model out of the Node test body.
const NUMBER = "number";
const TEXT = "text";
const JSON_OBJECT = "JSONObject";

/** Mounts the probe for a descriptor and returns the routing decision it reports. */
async function route(mount: any, descriptor: Record<string, unknown>) {
    const component = await mount(ProviderProbe, { props: { descriptor } });
    return component.getByRole("status");
}

ct("should route a multidimensional supported type to the JSON Forms editor", async ({ mount }) => {
    // A one-dimensional array of a supported type is not handled by the simple input.
    const status = await route(mount, { name: NUMBER, jsonType: "number", arrayDimensions: 1 });
    await expect(status).toHaveText("json-forms");
});

ct("should route a nested multidimensional supported type to the JSON Forms editor", async ({ mount }) => {
    const status = await route(mount, { name: NUMBER, jsonType: "number", arrayDimensions: 2 });
    await expect(status).toHaveText("json-forms");
});

ct("should keep a scalar supported complex type on the fallback input", async ({ mount }) => {
    // A plain (non-array) JSONObject is a supported type, so it stays on the simple JSON input.
    const status = await route(mount, { name: JSON_OBJECT, jsonType: "object" });
    await expect(status).toHaveText("fallback");
});

ct("should route an unsupported complex type to the JSON Forms editor", async ({ mount }) => {
    const status = await route(mount, { name: "customStruct", jsonType: "object" });
    await expect(status).toHaveText("json-forms");
});

ct("should keep a primitive type on the fallback input", async ({ mount }) => {
    const status = await route(mount, { name: TEXT, jsonType: "string" });
    await expect(status).toHaveText("fallback");
});
