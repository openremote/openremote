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
import type { Map as MapGL } from "maplibre-gl";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-icon";
import { OrMapBaseControl } from "./base";

export class OrMapGeolocateControl extends OrMapBaseControl {
    private _map?: MapGL;
    private _button?: HTMLElement;
    private _onLocate?: (pos: GeolocationPosition) => void;

    constructor(onLocate?: (pos: GeolocationPosition) => void) {
        super();
        this._onLocate = onLocate;
    }

    onAdd(map: MapGL): HTMLElement {
        this._map = map;
        this._createContainer({ background: "white", overflow: "hidden" });

        this._button = document.createElement("or-vaadin-button");
        this._button.setAttribute("theme", "icon");
        this._button.setAttribute("title", i18next.t("mapPage.findMyLocation"));
        const icon = document.createElement("or-icon") as HTMLElement;
        icon.setAttribute("icon", "mdi:crosshairs-gps");
        icon.style.cssText = "--or-icon-width: 18px; --or-icon-height: 18px; color: black;";
        this._button.appendChild(icon);
        this._button.addEventListener("click", () => this._locate());
        this._container!.appendChild(this._button);
        return this._container!;
    }

    private _locate() {
        if (!navigator.geolocation || !this._button) return;
        this._button.setAttribute("disabled", "");
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                if (this._onLocate) {
                    this._onLocate(pos);
                } else {
                    this._map?.flyTo({ center: [pos.coords.longitude, pos.coords.latitude], zoom: 14 });
                }
                this._button!.removeAttribute("disabled");
            },
            () => { this._button!.removeAttribute("disabled"); }
        );
    }

    onRemove(): void {
        super.onRemove();
        this._map = undefined;
        this._button = undefined;
    }
}
