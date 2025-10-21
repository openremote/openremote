import {defineConfig} from "@rsbuild/core";

export default defineConfig({
    source: {
        decorators: {
            version: "legacy"
        }
    },
    dev: {
        hmr: false, // HMR does not work for our Web Components atm
        liveReload: true
    },
    resolve: {
        alias: {
            // Webpack fallbacks for Node.js modules
            vm: false,
            // Workspace aliases for proper resolution
            "@openremote/core": "../../component/core/src",
            "@openremote/model": "../../component/model/src",
            "@openremote/rest": "../../component/rest/src",
            "@openremote/or-asset-tree": "../../component/or-asset-tree/src",
            "@openremote/or-attribute-picker": "../../component/or-attribute-picker/src",
            "@openremote/or-components": "../../component/or-components/src",
            "@openremote/or-chart": "../../component/or-chart/src",
            "@openremote/or-icon": "../../component/or-icon/src",
            "@openremote/or-mwc-components": "../../component/or-mwc-components/src",
            "@openremote/or-translate": "../../component/or-translate/src",
            "@openremote/or-tree-menu": "../../component/or-tree-menu/src",
            "@openremote/util": "../../util"
        },
        // Enable symlinks resolution for workspace packages
        symlinks: true
    },
    tools: {
        bundlerChain: (chain, { CHAIN_ID }) => {
            chain.module.rule(CHAIN_ID.RULE.CSS).exclude.add(/(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/); // Exclude the external CSS imports from default bundling.
        },
        rspack: (config, { addRules }) => {
            addRules([{test: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/, type: "asset/source"}]); // Add rule to treat external CSS imports as raw strings.
            return config;
        }
    }
});
