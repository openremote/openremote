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
import { OrAttributeCard } from "@openremote/or-attribute-card";

/** A card with real update logic but no backend, so it can be given data and then moved. */
class LoadedCard extends OrAttributeCard {
  loadSettings() {}
  loadData() {}
}

customElements.define("loaded-attribute-card", LoadedCard);

/** Waits until the element has no update pending, then lets a frame pass. */
async function settle(element) {
  while (!(await element.updateComplete)) {
    /* keep waiting */
  }
  await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
  while (!(await element.updateComplete)) {
    /* keep waiting */
  }
}

/** Counts the points of the line the card draws. */
function plotted(card) {
  return card._chart?.data.datasets[0]?.data.length ?? 0;
}

export class CardReattachFixture extends HTMLElement {
  /**
   * Draws the card's line, then re-inserts the card under a second parent.
   * @returns the number of points drawn before and after the re-insertion
   */
  async reattach() {
    const from = document.createElement("div");
    const to = document.createElement("div");
    for (const parent of [from, to]) {
      parent.style.width = "400px";
      parent.style.height = "300px";
    }
    this.append(from, to);

    const card = document.createElement("loaded-attribute-card");
    card.realm = "master";
    from.appendChild(card);
    await settle(card);

    // The canvas is only rendered once there is an attribute, and setting one clears the data
    card.assets = [{ id: "asset-1", name: "Asset 1", type: "ThingAsset" }];
    card.assetAttributes = [[0, { name: "value" }]];
    await settle(card);

    const now = Date.now();
    card.data = [
      { x: now - 2000, y: 1 },
      { x: now - 1000, y: 2 },
      { x: now, y: 3 },
    ];
    await settle(card);
    const before = plotted(card);

    to.appendChild(card);
    await settle(card);

    return { before, after: plotted(card) };
  }
}

customElements.define("card-reattach-fixture", CardReattachFixture);
