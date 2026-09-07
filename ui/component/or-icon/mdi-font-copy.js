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
// Script for copying @mdi/font files into build dir
const fs = require("fs");
const path = require("path");

if (!fs.existsSync("build")) {
  fs.mkdirSync("build");
}

const fontDir = path.join(path.dirname(require.resolve("@mdi/font/package.json")), "fonts");
const cssDir = path.join(path.dirname(require.resolve("@mdi/font/package.json")), "css");

if (!fs.existsSync("build/Material Design Icons")) {
  fs.mkdirSync("build/Material Design Icons", { recursive: true });
}

fs.cpSync(fontDir, "./build/Material Design Icons/fonts", { recursive: true });
fs.cpSync(cssDir, "./build/Material Design Icons/css", { recursive: true });
