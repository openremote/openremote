/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
        }
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
