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
            "@openremote/core": "../../component/core/src",
            "@openremote/model": "../../component/model/src",
            "@openremote/rest": "../../component/rest/src",
            "@openremote/or-icon": "../../component/or-icon/src",
            "@openremote/or-mwc-components": "../../component/or-mwc-components/src",
            "@openremote/or-translate": "../../component/or-translate/src",
            "@openremote/or-tree-menu": "../../component/or-tree-menu/src",
            "@openremote/util": "../../util"
        },
        // Enable symlinks resolution for workspace packages
        symlinks: true
    }
});
