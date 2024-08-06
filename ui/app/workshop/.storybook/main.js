import {join, dirname} from "path";
const webpack = require("webpack");

/**
 * This function is used to resolve the absolute path of a package.
 * It is needed in projects that use Yarn PnP or are set up within a monorepo.
 */
function getAbsolutePath(value) {
    const dir = dirname(require.resolve(join(value, "package.json")));
    return dir;
}

function getStandardModuleRules() {
    return [
        {
            test: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/, //output css as strings
            type: "asset/source"
        },
        {
            test: /\.css$/, //
            exclude: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/,
            use: [
                {loader: "css-loader"}
            ]
        },
        {
            test: /\.(png|jpg|ico|gif|svg|eot|ttf|woff|woff2|mp4)$/,
            type: "asset",
            generator: {
                filename: 'images/[hash][ext][query]'
            }
        },
        {
            test: /\.tsx?$/,
            exclude: /node_modules/,
            use: {
                loader: "ts-loader",
                options: {
                    projectReferences: true
                }
            }
        }
    ];
}

/** @type { import('@storybook/web-components-webpack5').StorybookConfig } */
const config = {
    addons: [
        getAbsolutePath("@storybook/addon-actions"),
        { name: "@storybook/addon-docs", options: { transcludeMarkdown: true }},
        getAbsolutePath("@storybook/addon-essentials"),
        getAbsolutePath("@storybook/addon-interactions"),
        getAbsolutePath("@storybook/addon-links"),
        getAbsolutePath("@storybook/addon-webpack5-compiler-babel"),
        getAbsolutePath("storybook-addon-pseudo-states")
    ],
    core: {},
    docs: {},
    framework: {
        name: getAbsolutePath("@storybook/web-components-webpack5"),
        /*options: {
            builder: {
                fsCache: true,
                lazyCompilation: true
            }
        },*/
    },
    staticDirs: ['../images'],
    stories: [
        /*getAbsolutePath("@openremote/or-chart") + "/!*.stories.@(js|jsx|mjs|ts|tsx)",
        getAbsolutePath("@openremote/or-icon") + "/!*.stories.@(js|jsx|mjs|ts|tsx)",*/
        "../stories/*.stories.@(js|jsx|mjs|ts|tsx)",
        "../docs/**/*.mdx"
    ],
    typescript: {
        check: false
    },
    webpackFinal: async (config, options) => {
        const newConfig = {
            ...config,
            module: {
                ...config.module,
                rules: [
                    ...config.module.rules.filter(r => !r.test || !r.test.toString()?.includes('css')),
                    ...getStandardModuleRules()
                ]
            },
            plugins: [
                ...config.plugins,
                new webpack.DefinePlugin({
                    CONFIG_URL_PREFIX: JSON.stringify(""),
                    PRODUCTION: JSON.stringify(false),
                    MANAGER_URL: JSON.stringify("http://localhost:8080"),
                    KEYCLOAK_URL: JSON.stringify(undefined)
                }),
            ],
            resolve: {
                ...config.resolve,
                extensions: [".ts", ".tsx", "..."],
                fallback: {
                    "vm": false,
                    "querystring": require.resolve("querystring-es3")
                }
            },
            watchOptions: {
                ignored: ['node_modules']
            }
        };
        console.log(newConfig.module.rules);
        console.log(options);
        return newConfig;
    }
};

export default config;
