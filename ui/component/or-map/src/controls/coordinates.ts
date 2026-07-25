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
import { LngLat, Map as MapGL } from "maplibre-gl";
import { OrMapBaseControl } from "./base";
import "@openremote/or-vaadin-components/or-vaadin-text-field";
import "@openremote/or-icon";

const CoordinateNumberPattern = "-?\\d+\\.?\\d*";
export const CoordinatesRegexPattern = `^[ ]*(?:Lat:[ ]*(${CoordinateNumberPattern})[, ]+Lng:[ ]*(${CoordinateNumberPattern})|(${CoordinateNumberPattern})[, ]+(${CoordinateNumberPattern}))[ ]*$`;

export function commitCoordinatesInputValue(e: Event, valueChangedHandler: (value: LngLat | null) => void) {
    const input = e.target as HTMLInputElement;
    if (input.readOnly || input.disabled || input.hasAttribute("readonly") || input.hasAttribute("disabled")) {
        return;
    }

    if (!input.value.trim()) {
        valueChangedHandler(null);
        return;
    }

    const match = new RegExp(CoordinatesRegexPattern).exec(input.value);
    if (!match) {
        return;
    }

    const lng = Number(match[2] ?? match[3]);
    const lat = Number(match[1] ?? match[4]);
    if (!Number.isFinite(lng) || !Number.isFinite(lat) || lng < -180 || lng > 180 || lat < -90 || lat > 90) {
        return;
    }

    valueChangedHandler(new LngLat(lng, lat));
}

export function getCoordinatesInputKeyHandler(valueChangedHandler: (value: LngLat | null) => void) {
    return (e: KeyboardEvent) => {
        if (e.code === "Enter" || e.code === "NumpadEnter") {
            commitCoordinatesInputValue(e, valueChangedHandler);
        }
    };
}

export class CoordinatesControl extends OrMapBaseControl {
    protected input?: HTMLElement;
    protected _readonly = false;
    protected _value: any;
    protected _valueChangedHandler: (value: LngLat | null) => void;

    constructor(disabled = false, valueChangedHandler: (value: LngLat | null) => void) {
        super();
        this._readonly = disabled;
        this._valueChangedHandler = valueChangedHandler;
    }

    onAdd(_map: MapGL): HTMLElement {
        this._createContainer();

        const input = document.createElement("or-vaadin-text-field") as HTMLElement;
        if (this._readonly) input.setAttribute("readonly", "");
        if (this._value != null) (input as any).value = this._value;
        input.setAttribute("pattern", CoordinatesRegexPattern);
        input.addEventListener("keyup", getCoordinatesInputKeyHandler(this._valueChangedHandler) as EventListener);
        input.addEventListener("change", (e) => commitCoordinatesInputValue(e, this._valueChangedHandler));

        const icon = document.createElement("or-icon") as HTMLElement;
        icon.setAttribute("icon", "mdi:crosshairs");
        icon.setAttribute("slot", "prefix");
        icon.style.cssText = "--or-icon-width: 14px; --or-icon-height: 14px; margin-left: 4px; margin-right: 8px;";
        input.appendChild(icon);

        this._container!.appendChild(input);
        this.input = input;
        return this._container!;
    }

    onRemove(): void {
        super.onRemove();
        this.input = undefined;
    }

    public set readonly(readonly: boolean) {
        this._readonly = readonly;
        if (this.input) {
            if (readonly) {
                this.input.setAttribute("readonly", "");
            } else {
                this.input.removeAttribute("readonly");
            }
        }
    }

    public set value(value: any) {
        this._value = value;
        if (this.input) {
            (this.input as any).value = value;
        }
    }
}
