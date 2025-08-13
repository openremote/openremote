#!/usr/bin/env node
const path = require("path");
const fs = require("fs");
const { spawnSync } = require("child_process");
const { spawn } = require("child_process");

/* THIS IS JUST A WRAPPER THAT LAUNCHES GRADLE TASKS  */

/* Try and find the gradle wrapper in a parent dir */
function getGradleDirectory() {
    let dirs = process.cwd().split(path.sep);
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
    let cwd = getGradleDirectory();
    console.log("Running gradlew modelBuild task in " + cwd + " ...");
    const gradleModelWatch = spawnSync((process.platform === "win32" ? "gradlew" : "./gradlew"), ["modelWatch"], {
        cwd: cwd,
        shell: true
    });
    console.log("gradlew modelWatch finished! Status = " + gradleModelWatch.status);
    if (gradleModelWatch.stderr) {
        console.log(gradleModelWatch.stderr.toString());
    }
    process.exit(gradleModelWatch.status);
}
