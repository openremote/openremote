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
import { IControl, LngLat, Map as MapGL } from "maplibre-gl";
import { InputType, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";

export const CoordinatesRegexPattern = "^[ ]*(?:Lat: )?(-?\\d+\\.?\\d*)[, ]+(?:Lng: )?(-?\\d+\\.?\\d*)[ ]*$";

export function getCoordinatesInputKeyHandler(valueChangedHandler: (value: LngLat | undefined) => void) {
    return (e: KeyboardEvent) => {
        if (e.code === "Enter" || e.code === "NumpadEnter") {
            const valStr = (e.target as OrMwcInput).value as string;
            let value: LngLat | undefined = !valStr ? undefined : {} as LngLat;

            if (valStr) {
                const lngLatArr = valStr.split(/[ ,]/).filter(v => !!v);
                if (lngLatArr.length === 2) {
                    value = new LngLat(
                        Number.parseFloat(lngLatArr[0]),
                        Number.parseFloat(lngLatArr[1])
                    );
                }
            }
            valueChangedHandler(value);
        }
    };
}

export class CoordinatesControl implements IControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    protected input!: OrMwcInput;
    protected _readonly = false;
    protected _value: any;
    protected _valueChangedHandler: (value: LngLat | undefined) => void;

    constructor(disabled = false, valueChangedHandler: (value: LngLat | undefined) => void) {
        this._readonly = disabled;
        this._valueChangedHandler = valueChangedHandler;
    }

    onAdd(map: MapGL): HTMLElement {
        this.map = map;
        const control = document.createElement("div");
        control.classList.add("maplibregl-ctrl");
        control.classList.add("maplibregl-ctrl-group");

        const input = new OrMwcInput();
        input.type = InputType.TEXT;
        input.outlined = true;
        input.compact = true;
        input.readonly = this._readonly;
        input.icon = "crosshairs-gps";
        input.value = this._value;
        input.pattern = CoordinatesRegexPattern;
        input.onkeyup = getCoordinatesInputKeyHandler(this._valueChangedHandler);

        control.appendChild(input);
        this.elem = control;
        this.input = input;
        return control;
    }

    onRemove(map: MapGL) {
        this.map = undefined;
        this.elem = undefined;
    }

    public set readonly(readonly: boolean) {
        this._readonly = readonly;
        if (this.input) {
            this.input.readonly = readonly;
        }
    }

    public set value(value: any) {
        this._value = value;
        if (this.input) {
            this.input.value = value;
        }
    }
}
