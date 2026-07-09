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
import type { LngLatLike, Map as MapGL } from "maplibre-gl";
import { i18next } from "@openremote/or-translate";
import { OrMapBaseControl } from "./base";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-icon";

export class OrMapCenterControl extends OrMapBaseControl {
    private _map?: MapGL;
    public pos?: LngLatLike;

    onAdd(map: MapGL): HTMLElement {
        this._map = map;
        this._createContainer({ overflow: "hidden" });

        const button = document.createElement("or-vaadin-button");
        button.setAttribute("theme", "icon");
        button.setAttribute("title", i18next.t("mapPage.centerOnLocation"));

        const icon = document.createElement("or-icon") as HTMLElement;
        icon.setAttribute("icon", "or:compass");
        icon.style.cssText = "--or-icon-width: 18px; --or-icon-height: 18px;";
        button.appendChild(icon);

        button.addEventListener("click", () => {
            if (this.pos) this._map?.flyTo({ center: this.pos, zoom: this._map.getZoom() });
        });

        this._container!.appendChild(button);
        return this._container!;
    }

    onRemove(): void {
        super.onRemove();
        this._map = undefined;
    }
}
