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
import { OrChart } from "@openremote/or-chart";

/** A chart with real update logic but no backend, so it can be given data and then moved. */
class LoadedChart extends OrChart {
  loadSettings() {}
  _loadData() {
    return Promise.resolve();
  }
}

customElements.define("loaded-or-chart", LoadedChart);

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

/** Counts the series the chart draws, and the points in the first one. */
function plotted(chart) {
  const series = chart._chart?.getOption().series ?? [];
  return { series: series.length, points: series[0]?.data?.length ?? 0 };
}

export class ChartReattachFixture extends HTMLElement {
  /**
   * Draws a line chart, then re-inserts it under a second parent.
   * @returns what the chart plotted before and after the re-insertion
   */
  async reattach() {
    const from = document.createElement("div");
    const to = document.createElement("div");
    for (const parent of [from, to]) {
      parent.style.width = "600px";
      parent.style.height = "400px";
    }
    this.append(from, to);

    const chart = document.createElement("loaded-or-chart");
    from.appendChild(chart);
    await settle(chart);

    const now = Date.now();
    chart._data = [
      {
        name: "Asset 1 value",
        type: "line",
        data: [
          [now - 2000, 1],
          [now - 1000, 2],
          [now, 3],
        ],
      },
    ];
    await settle(chart);
    const before = plotted(chart);

    to.appendChild(chart);
    await settle(chart);

    return { before, after: plotted(chart) };
  }
}

customElements.define("chart-reattach-fixture", ChartReattachFixture);
