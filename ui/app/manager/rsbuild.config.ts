import { defineConfig } from "@rsbuild/core";

export default defineConfig({
    dev: {
        hmr: false, // HMR does not work for our Web Components atm
        liveReload: true
    },
    server: {
        port: 9000 // TODO: Leave default in production
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
            "@openremote/or-app": "../../component/or-app/src",
            "@openremote/or-asset-tree": "../../component/or-asset-tree/src",
            "@openremote/or-asset-viewer": "../../component/or-asset-viewer/src",
            "@openremote/or-attribute-barchart": "../../component/or-attribute-barchart/src",
            "@openremote/or-attribute-card": "../../component/or-attribute-card/src",
            "@openremote/or-attribute-history": "../../component/or-attribute-history/src",
            "@openremote/or-attribute-input": "../../component/or-attribute-input/src",
            "@openremote/or-attribute-picker": "../../component/or-attribute-picker/src",
            "@openremote/or-components": "../../component/or-components/src",
            "@openremote/or-chart": "../../component/or-chart/src",
            "@openremote/or-dashboard-builder": "../../component/or-dashboard-builder/src",
            "@openremote/or-gauge": "../../component/or-gauge/src",
            "@openremote/or-icon": "../../component/or-icon/src",
            "@openremote/or-json-forms": "../../component/or-json-forms/src",
            "@openremote/or-log-viewer": "../../component/or-log-viewer/src",
            "@openremote/or-map": "../../component/or-map/src",
            "@openremote/or-mwc-components": "../../component/or-mwc-components/src",
            "@openremote/or-rules": "../../component/or-rules/src",
            "@openremote/or-services": "../../component/or-services/src",
            "@openremote/or-translate": "../../component/or-translate/src",
            "@openremote/or-tree-menu": "../../component/or-tree-menu/src",
            "@openremote/util": "../../util"
        }
    },
    tools: {
        bundlerChain: (chain, { CHAIN_ID }) => {
            chain.module.rule(CHAIN_ID.RULE.CSS).exclude.add(/(@material|@mdi).*\.css$/); // Exclude the external CSS imports from default bundling.
        },
        rspack: (config, { addRules }) => {
            addRules([{test: /(@material|@mdi).*\.css$/, type: "asset/source"}]); // Add rule to treat external CSS imports as raw strings.
            return config;
        }
    }
});
