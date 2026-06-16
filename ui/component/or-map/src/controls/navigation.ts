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
import { css, html, LitElement } from "lit";
import { customElement, state } from "lit/decorators.js";
import type { Map as MapGL } from "maplibre-gl";
import { OrMapBaseControl } from "./base";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-vaadin-components/or-vaadin-icon";
import "@openremote/or-icon";

@customElement("or-map-navigation")
export class OrMapNavigation extends LitElement {

    static get styles() {
        return css`
            :host {
                display: flex;
                flex-direction: column;
                background: white;
                overflow: hidden;
                border-radius: var(--lumo-border-radius-m, 4px);
            }

            or-vaadin-button {
                --lumo-border-radius-m: 0;
            }

            or-vaadin-icon {
                width: 14px;
                height: 14px;
                color: black;
            }

            or-icon {
                --or-icon-width: 18px;
                --or-icon-height: 18px;
                transition: transform 0.1s ease;
            }
        `;
    }

    @state()
    private _bearing = 0;

    private _map?: MapGL;

    private _onRotate = () => {
        this._bearing = -(this._map?.getBearing() ?? 0);
    };

    public setMap(map: MapGL): void {
        this._map = map;
        map.on("rotate", this._onRotate);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this._map?.off("rotate", this._onRotate);
    }

    protected render() {
        return html`
            <or-vaadin-button theme="icon" title="Zoom in" @click="${() => this._map?.zoomIn()}">
                <or-vaadin-icon icon="vaadin:plus"></or-vaadin-icon>
            </or-vaadin-button>
            <or-vaadin-button theme="icon" title="Zoom out" @click="${() => this._map?.zoomOut()}">
                <or-vaadin-icon icon="vaadin:minus"></or-vaadin-icon>
            </or-vaadin-button>
            <or-vaadin-button theme="icon" title="Reset bearing to north" @click="${() => this._map?.resetNorthPitch({ duration: 200 })}">
                <or-icon icon="or:compass" style="transform: rotate(${this._bearing}deg)"></or-icon>
            </or-vaadin-button>
        `;
    }
}

export class OrMapNavigationControl extends OrMapBaseControl {
    private _component?: OrMapNavigation;

    onAdd(map: MapGL): HTMLElement {
        this._createContainer();
        this._component = document.createElement("or-map-navigation") as OrMapNavigation;
        this._component.setMap(map);
        this._container!.appendChild(this._component);
        return this._container!;
    }

    onRemove(): void {
        super.onRemove();
        this._component = undefined;
    }
}
