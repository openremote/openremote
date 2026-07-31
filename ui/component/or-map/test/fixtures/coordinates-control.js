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
import { CoordinatesControl } from "@openremote/or-map/controls/coordinates";

export class CoordinatesControlFixture extends HTMLElement {
  commit(value, event, readonly) {
    let result;
    const control = new CoordinatesControl(readonly, (lngLat) => {
      result = lngLat ? { lng: lngLat.lng, lat: lngLat.lat } : null;
    });
    const input = control.onAdd().querySelector("or-vaadin-text-field");
    input.value = value;
    input.dispatchEvent(
      event === "change"
        ? new Event("change", { bubbles: true })
        : new KeyboardEvent("keyup", { code: "Enter", bubbles: true })
    );
    control.onRemove();
    return result;
  }
}

customElements.define("coordinates-control-fixture", CoordinatesControlFixture);
