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
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-vaadin-components/or-vaadin-icon";
import "@openremote/or-icon";
import { OrMapBaseControl } from "./base";

export class OrMapNavigationControl extends OrMapBaseControl {
    private _map?: MapGL;
    private _compassIcon?: HTMLElement;

    private _onRotate = () => {
        if (this._compassIcon && this._map) {
            this._compassIcon.style.transform = `rotate(${-this._map.getBearing()}deg)`;
        }
    };

    onAdd(map: MapGL): HTMLElement {
        this._map = map;
        this._createContainer({ background: "white", display: "flex", flexDirection: "column", overflow: "hidden" });

        this._container!.appendChild(this._vaadinBtn("vaadin:plus", "Zoom in", () => map.zoomIn()));
        this._container!.appendChild(this._vaadinBtn("vaadin:minus", "Zoom out", () => map.zoomOut()));

        this._compassIcon = document.createElement("or-icon") as HTMLElement;
        this._compassIcon.setAttribute("icon", "or:compass");
        this._compassIcon.style.cssText = "--or-icon-width: 18px; --or-icon-height: 18px; transition: transform 0.1s ease;";

        const compassBtn = document.createElement("or-vaadin-button");
        compassBtn.setAttribute("theme", "icon");
        compassBtn.setAttribute("title", "Reset bearing to north");
        compassBtn.style.setProperty("--lumo-border-radius-m", "0");
        compassBtn.appendChild(this._compassIcon);
        compassBtn.addEventListener("click", () => map.resetNorthPitch({ duration: 200 }));
        this._container!.appendChild(compassBtn);

        map.on("rotate", this._onRotate);
        return this._container!;
    }

    private _vaadinBtn(icon: string, title: string, onClick: () => void): HTMLElement {
        const btn = document.createElement("or-vaadin-button");
        btn.setAttribute("theme", "icon");
        btn.setAttribute("title", title);
        btn.style.setProperty("--lumo-border-radius-m", "0");
        const ic = document.createElement("or-vaadin-icon") as HTMLElement;
        ic.setAttribute("icon", icon);
        ic.style.cssText = "width: 14px; height: 14px; color: black;";
        btn.appendChild(ic);
        btn.addEventListener("click", onClick);
        return btn;
    }

    onRemove(): void {
        this._map?.off("rotate", this._onRotate);
        super.onRemove();
        this._map = undefined;
        this._compassIcon = undefined;
    }
}
