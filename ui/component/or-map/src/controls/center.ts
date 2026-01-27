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
import { LngLatLike, Map as MapGL } from "maplibre-gl";

export class CenterControl {
    protected map?: MapGL;
    protected elem?: HTMLElement;
    public pos?: LngLatLike;

    onAdd(map: MapGL): HTMLElement {
        this.map = map;
        const control = document.createElement("div");
        control.classList.add("maplibregl-ctrl");
        control.classList.add("maplibregl-ctrl-group");
        const button = document.createElement("button");
        button.className = "maplibregl-ctrl-compass";
        button.addEventListener("click", (ev) =>
            map.flyTo({
                center: this.pos,
                zoom: map.getZoom(),
            })
        );
        const buttonIcon = document.createElement("span");
        buttonIcon.className = "maplibregl-ctrl-icon";
        button.appendChild(buttonIcon);
        control.appendChild(button);
        this.elem = control;
        return control;
    }

    onRemove(map: MapGL) {
        this.map = undefined;
        this.elem = undefined;
    }
}
