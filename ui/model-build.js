var path = require("path");
const fs = require("fs");
const { spawnSync } = require("child_process");

function getWorkingDirectory() {
    let dirs = __dirname.split(path.sep);
    dirs.pop();

    let cwd = dirs.join(path.sep);

    // Dirs is now the repo root but is this a custom project repo or OR repo
    let isDirCalledOR = dirs.pop() === "openremote";
    if(isDirCalledOR) {
        try {
            if (fs.existsSync(path.join(cwd, "..", ".gitmodules"))) {
                cwd = path.join(cwd, "..");
            }
        } catch(err) {
            console.error(err);
            process.exit(1);
        }
    }

    return cwd;
}

let cwd = getWorkingDirectory();
console.log("Running gradlew modelBuild task...");
const gradleModelWatch = spawnSync((process.platform === "win32" ? "gradlew" : "./gradlew"), ["modelWatch"], {
    cwd: cwd,
    shell: true
});
console.log("gradlew modelWatch finished! Status = " + gradleModelWatch.status);
process.exit(gradleModelWatch.status);