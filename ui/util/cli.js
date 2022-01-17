#!/usr/bin/env node
const path = require("path");
const fs = require("fs");
const { spawnSync } = require("child_process");
const { spawn } = require("child_process");

/* THIS IS JUST A WRAPPER THAT LAUNCHES GRADLE TASKS  */

/* Try and get the openremote project dir or custom project dir */
function getWorkingDirectory() {
    let dirs = __dirname.split(path.sep);
    dirs.pop();

    // get CWD as the openremote repo root
    let cwd = dirs.join(path.sep);
    cwd = path.join(cwd, "..");

    if (fs.existsSync(path.join(cwd, "..", ".gitmodules"))) {
        // Assume we're in a custom project
        cwd = path.join(cwd, "..");
    }

    return cwd;
}

if (process.argv.length >= 3 && process.argv[2] =="watch") {

    // Do watch
    let cwd = getWorkingDirectory();
    console.log("Watching model for changes...");
    const child = spawn((process.platform === "win32" ? "gradlew" : "./gradlew"), ["-t", "modelWatch"], {
        cwd: cwd,
        shell: true
    });
    child.stdout.removeAllListeners("data");
    child.stderr.removeAllListeners("data");
    child.stdout.pipe(process.stdout);
    child.stderr.pipe(process.stderr);

    child.on("exit", function() {
        console.log("gradlew modelWatch finished! Status = " + child.status);
        process.exit(child.status);
    });
} else {

    // Do build
    let cwd = getWorkingDirectory();
    console.log("Running gradlew modelBuild task in " + cwd + " ...");
    const gradleModelWatch = spawnSync((process.platform === "win32" ? "gradlew" : "./gradlew"), ["modelWatch"], {
        cwd: cwd,
        shell: true
    });
    console.log("gradlew modelWatch finished! Status = " + gradleModelWatch.status);
    process.exit(gradleModelWatch.status);
}
