/*import {getTsProgram, expandTypesPlugin} from "cem-plugin-expanded-types";*/

export default {
    globs: [
        '../**/src/**.ts',
    ],
    exclude: [
        '../core/**',
        '../docs/**',
        '../model/**',
        '../rest/**',
        '../util/**'
    ],
    /*overrideModuleCreation: ({ts, globs}) => {
        const program = getTsProgram(ts, globs, "util/tsconfig.json");
        return program
            .getSourceFiles()
            .filter((sf) => globs.find((glob) => sf.fileName.includes(glob)));
    },
    plugins: [expandTypesPlugin()],*/
    dev: true,
    dependencies: false,
    litelement: true
}
