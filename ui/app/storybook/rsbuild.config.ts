import {defineConfig} from "@rsbuild/core";

export default defineConfig({
    html: {
        template: "./src/index.html"
    },
    source: {
        decorators: {
            version: "legacy"
        }
    },
    resolve: {
        alias: {
            // Webpack fallbacks for Node.js modules
            vm: false,
            // Workspace aliases for proper resolution
            "@openremote/model": "../../component/model",
            "@openremote/rest": "../../component/rest",
            "@openremote/core": "../../component/core",
            "@openremote/or-app": "../../component/or-app",
            "@openremote/or-translate": "../../component/or-translate",
            "@openremote/or-components": "../../component/or-components",
            "@openremote/or-mwc-components": "../../component/or-mwc-components",
            "@openremote/or-asset-tree": "../../component/or-asset-tree",
            "@openremote/or-chart": "../../component/or-chart",
            "@openremote/or-icon": "../../component/or-icon",
            "@openremote/or-input": "../../component/or-input",
            "@openremote/or-map": "../../component/or-map",
            "@openremote/or-rules": "../../component/or-rules",
            "@openremote/util": "../../util"
        },
        // Enable symlinks resolution for workspace packages
        symlinks: true
    }
});
