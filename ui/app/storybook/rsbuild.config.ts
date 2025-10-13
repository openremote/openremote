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
            "@openremote/model": "../../component/model/src",
            "@openremote/rest": "../../component/rest/src",
            "@openremote/core": "../../component/core/src",
            "@openremote/or-app": "../../component/or-app/src",
            "@openremote/or-translate": "../../component/or-translate/src",
            "@openremote/or-components": "../../component/or-components/src",
            "@openremote/or-mwc-components": "../../component/or-mwc-components/src",
            "@openremote/or-asset-tree": "../../component/or-asset-tree/src",
            "@openremote/or-chart": "../../component/or-chart/src",
            "@openremote/or-icon": "../../component/or-icon/src",
            "@openremote/or-input": "../../component/or-input/src",
            "@openremote/or-map": "../../component/or-map/src",
            "@openremote/or-rules": "../../component/or-rules/src",
            "@openremote/util": "../../util"
        },
        // Enable symlinks resolution for workspace packages
        symlinks: true
    }
});
