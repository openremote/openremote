import { getTsProgram, expandTypesPlugin } from "cem-plugin-expanded-types";

export default {
    globs: [
        '../or-app/src/**.ts',
        '../or-asset-tree/src/**.ts',
        '../or-asset-viewer/src/**.ts',
        '../or-chart/src/**.ts',
        '../or-icon/src/**.ts',
        '../or-gauge/src/**.ts',
        '../or-mobile-app/src/**.ts',
        '../or-mwc-components/src/**.ts',
        '../or-translate/src/**.ts',
    ],
    exclude: [
        '../core/**',
        '../docs/**',
        '../model/**',
        '../rest/**',
        '../util/**'
    ],
    /*overrideModuleCreation: ({ts, globs}) => {
        const program = getTsProgram(ts, globs, "tsconfig.json");
        return program
            .getSourceFiles()
            .filter((sf) => globs.find((glob) => sf.fileName.includes(glob)));
    },
    plugins: [expandTypesPlugin()],*/
    dev: true,
    dependencies: false,
    litelement: true
}
