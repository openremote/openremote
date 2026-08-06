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
// Plain JS on purpose: the component-test bundle compiles .ts files against ui/test's tsconfig
// (rootDir ui/test), so TS component sources outside that dir fail the build.
import { OrLogViewer } from "@openremote/or-log-viewer";

/** Fixed so the rendered ending timestamp is deterministic. */
export const TIMESTAMP = new Date(2026, 0, 15, 10, 30, 0);

const EVENTS = [
  {
    timestamp: TIMESTAMP.getTime(),
    level: "INFO",
    category: "DATA",
    subCategory: "Attribute",
    message: "Attribute updated",
  },
  {
    timestamp: TIMESTAMP.getTime() - 60000,
    level: "ERROR",
    category: "AGENT",
    subCategory: "Protocol",
    message: "Connection refused",
  },
];

/** Serves static events so the viewer renders without a manager REST connection. */
export class OrLogViewerTest extends OrLogViewer {
  constructor() {
    super();
    this.timestamp = TIMESTAMP;
  }

  _loadData() {
    this._data = EVENTS;
  }
}
customElements.define("or-log-viewer-test", OrLogViewerTest);
