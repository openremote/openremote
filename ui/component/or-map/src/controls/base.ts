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
import type { IControl, Map as MapGL } from "maplibre-gl";

export abstract class OrMapBaseControl implements IControl {
    protected _container?: HTMLElement;

    protected _createContainer(extra?: Partial<CSSStyleDeclaration>): HTMLElement {
        const el = document.createElement("div");
        el.className = "maplibregl-ctrl";
        Object.assign(el.style, {
            fontSize: "16px",
            background: "white",
            boxShadow: "0px 2px 6px -1px var(--lumo-shade-10pct), 0px 8px 24px -4px var(--lumo-shade-30pct)",
            borderRadius: "var(--lumo-border-radius-m, 4px)",
            ...extra,
        });
        this._container = el;
        return el;
    }

    abstract onAdd(map: MapGL): HTMLElement;

    onRemove(): void {
        this._container?.remove();
        this._container = undefined;
    }
}
