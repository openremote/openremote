var path = require("path");
const fs = require("fs");
const { spawn } = require("child_process");

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
    process.exit(gradleModelWatch.status);
});