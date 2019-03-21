var path = require("path");
const { spawnSync } = require('child_process');

let dirs = __dirname.split(path.sep);
dirs.pop();
let cwd = dirs.join(path.sep);
let isORRepo = dirs.pop() === "openremote";

if (!isORRepo) {
    cwd = path.join(cwd, "openremote");
}

const gradleModelWatch = spawnSync('gradlew', ["modelWatch"], {cwd: cwd, shell: true});
process.exit(gradleModelWatch.status);