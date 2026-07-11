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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import { ct, expect, type Page } from "@openremote/test";
import { CoordinatesControl } from "@openremote/or-map/controls/coordinates";
import type { Map as MapGL } from "maplibre-gl";

type ImportRef = {
    __pw_type: "importRef";
    id: string;
    property?: string;
};

type ImportRegistry = {
    resolveImportRef(importRef: ImportRef): Promise<unknown>;
};

async function commitCoordinates(page: Page, value: string, event: "change" | "Enter", readonly = false) {
    return page.evaluate(async ({controlImport, value, event, readonly}) => {
        const registry = (window as typeof window & {__pwRegistry: ImportRegistry}).__pwRegistry;
        const Control = await registry.resolveImportRef(controlImport) as typeof CoordinatesControl;

        let result: {lng: number, lat: number} | null | undefined;
        const control = new Control(readonly, (lngLat) => {
            result = lngLat ? {lng: lngLat.lng, lat: lngLat.lat} : null;
        });
        const container = control.onAdd(undefined as unknown as MapGL);
        document.body.appendChild(container);
        const input = container.querySelector("or-vaadin-text-field") as HTMLElement & {value: string};
        input.value = value;

        if (event === "change") {
            input.dispatchEvent(new Event("change", {bubbles: true}));
        } else {
            input.dispatchEvent(new KeyboardEvent("keyup", {code: "Enter", bubbles: true}));
        }

        control.onRemove();
        return result;
    }, {
        controlImport: CoordinatesControl as unknown as ImportRef,
        value,
        event,
        readonly
    });
}

ct("Should commit valid coordinates on change", async ({page}) => {
    expect(await commitCoordinates(page, "4.89, 52.37", "change")).toEqual({lng: 4.89, lat: 52.37});
});

ct("Should commit valid coordinates on Enter", async ({page}) => {
    expect(await commitCoordinates(page, "4.89, 52.37", "Enter")).toEqual({lng: 4.89, lat: 52.37});
});

ct("Should clear coordinates on change", async ({page}) => {
    expect(await commitCoordinates(page, "", "change")).toBeNull();
});

ct("Should not commit invalid coordinates", async ({page}) => {
    expect(await commitCoordinates(page, "not coordinates", "change")).toBeUndefined();
});

ct("Should not commit readonly or disabled coordinates", async ({page}) => {
    expect(await commitCoordinates(page, "4.89, 52.37", "change", true)).toBeUndefined();
});
