#!/usr/bin/env node
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
const path = require("path");
const fs = require("fs");
const { spawnSync } = require("child_process");
const { spawn } = require("child_process");

/* THIS IS JUST A WRAPPER THAT LAUNCHES GRADLE TASKS  */

/* Try and find the gradle wrapper in a parent dir */
function getGradleDirectory() {
  const dirs = process.cwd().split(path.sep);
  let cwd = dirs.join(path.sep);

  while (dirs.length > 0 && !fs.existsSync(path.join(cwd, "gradlew"))) {
    dirs.pop();
    cwd = dirs.join(path.sep);
  }

  if (!fs.existsSync(path.join(cwd, "gradlew"))) {
    console.log("Failed to locate gradlew in a parent directory of: " + process.cwd());
    process.exit(1);
  } else {
    console.log("Located gradlew in parent directory: " + cwd);
  }

  return cwd;
}

if (process.argv.length >= 3 && process.argv[2] == "watch") {
  // Do watch
  const cwd = getGradleDirectory();
  console.log("Watching model for changes...");
  const child = spawn(process.platform === "win32" ? "gradlew" : "./gradlew", ["-t", "modelWatch"], {
    cwd,
    shell: true,
  });
  child.stdout.removeAllListeners("data");
  child.stderr.removeAllListeners("data");
  child.stdout.pipe(process.stdout);
  child.stderr.pipe(process.stderr);

  child.on("exit", function () {
    console.log("gradlew modelWatch finished! Status = " + child.status);
    process.exit(child.status);
  });
} else {
  // Do build
  const cwd = getGradleDirectory();
  console.log("Running gradlew modelBuild task in " + cwd + " ...");
  const gradleModelWatch = spawnSync(process.platform === "win32" ? "gradlew" : "./gradlew", ["modelWatch"], {
    cwd,
    shell: true,
  });
  console.log("gradlew modelWatch finished! Status = " + gradleModelWatch.status);
  if (gradleModelWatch.stderr) {
    console.log(gradleModelWatch.stderr.toString());
  }
  process.exit(gradleModelWatch.status);
}
