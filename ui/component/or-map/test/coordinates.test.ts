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
import { ct, expect, type SharedComponentTestFixtures } from "@openremote/test";
import { CoordinatesControlFixture } from "./fixtures/coordinates-control.js";

type Mount = SharedComponentTestFixtures["mount"];
async function mountCoordinatesControl(mount: Mount, value: string, event: "change" | "Enter", readonly = false) {
    const fixture = await mount(CoordinatesControlFixture);
    return fixture.evaluate((element, args) => (element as HTMLElement & {
        commit(value: string, event: "change" | "Enter", readonly: boolean): {lng: number, lat: number} | null | undefined;
    }).commit(args.value, args.event, args.readonly), {value, event, readonly});
}

ct("should commit valid coordinates on change", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "4.89, 52.37", "change")).toEqual({lng: 4.89, lat: 52.37});
});

ct("should commit valid coordinates on Enter", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "4.89, 52.37", "Enter")).toEqual({lng: 4.89, lat: 52.37});
});

ct("should commit labeled coordinates", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "Lat: 52.37, Lng: 4.89", "change")).toEqual({lng: 4.89, lat: 52.37});
});

ct("should accept the lower longitude boundary", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "-180, 52.37", "change")).toEqual({lng: -180, lat: 52.37});
});

ct("should accept the upper longitude boundary", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "180, 52.37", "change")).toEqual({lng: 180, lat: 52.37});
});

ct("should clear coordinates on change", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "  ", "change")).toBeNull();
});

ct("should not commit invalid coordinates", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "not coordinates", "change")).toBeUndefined();
});

ct("should not commit longitude below -180", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "-180.1, 52.37", "change")).toBeUndefined();
});

ct("should not commit longitude above 180", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "180.1, 52.37", "change")).toBeUndefined();
});

ct("should not commit readonly coordinates", async ({mount}) => {
    expect(await mountCoordinatesControl(mount, "4.89, 52.37", "change", true)).toBeUndefined();
});
