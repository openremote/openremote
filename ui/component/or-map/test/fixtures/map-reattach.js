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
import { OrMap } from "@openremote/or-map";

/** Counts setup attempts without building a real map. */
class ProbeMap extends OrMap {
  constructor() {
    super();
    this.loadMapCalls = 0;
  }

  loadMap() {
    this.loadMapCalls++;
  }
}

customElements.define("probe-map", ProbeMap);

export class MapReattachFixture extends HTMLElement {
  /**
   * Renders a map, then moves it to a second parent.
   * @returns the setup attempt count after the first render and after the move
   */
  async reattach() {
    const from = document.createElement("div");
    const to = document.createElement("div");
    this.append(from, to);

    const map = document.createElement("probe-map");
    from.appendChild(map);
    while (!(await map.updateComplete)) {
      /* keep waiting */
    }

    const afterFirstRender = map.loadMapCalls;
    to.appendChild(map);
    await new Promise((resolve) => setTimeout(resolve, 0));

    return { afterFirstRender, afterReattach: map.loadMapCalls };
  }

  /**
   * Stands a built map in for the real one, then removes the element.
   * @returns whether the map was unloaded and whether the reference was dropped
   */
  async remove() {
    const map = document.createElement("or-map");
    this.appendChild(map);
    while (!(await map.updateComplete)) {
      /* keep waiting */
    }

    let unloaded = false;
    map._map = {
      unload: () => {
        unloaded = true;
      },
    };

    map.remove();
    await new Promise((resolve) => setTimeout(resolve, 0));

    return { unloaded, kept: map._map !== undefined };
  }
}

customElements.define("map-reattach-fixture", MapReattachFixture);
