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
// Script for collating mdi svg icons (@mdi/svg) into a single JSON file
const fs = require("fs");
const path = require("path");
const xpath = require("xpath");
const dom = require("xmldom").DOMParser;

if (!fs.existsSync("src")) {
  fs.mkdirSync("src");
}

const mdiSvgDir = path.join(path.dirname(require.resolve("@mdi/svg/package.json")), "svg");
if (!fs.existsSync("build")) {
  fs.mkdirSync("build");
}
const mdiStream = fs.createWriteStream("./build/mdi-icons.json", { flags: "w+" });
mdiStream.write('{"size":24,"icons":{');

const files = fs.readdirSync(mdiSvgDir).sort();

for (let i = 0; i < files.length; i++) {
  const file = files[i];
  const fullPath = path.join(mdiSvgDir, file);
  const data = fs.readFileSync(fullPath, "utf8");
  const svg = new dom().parseFromString(data);
  const select = xpath.useNamespaces({ svg: "http://www.w3.org/2000/svg" });
  const pathAttrs = select("//svg:path/@d", svg);
  const pathData = pathAttrs[0].value;
  const name = file.substr(0, file.length - 4);
  // name = name.replace(/-([\w])/g, function (g) { return g[1].toUpperCase(); });
  mdiStream.write('"' + name + '":"' + pathData + '"' + (i < files.length - 1 ? "," : ""));
}

mdiStream.write("}}");
mdiStream.close();
