const util = require("@openremote/util");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const webpack = require("webpack");

module.exports = (env, argv) => {

    const customConfigDir = env.config;
    const managerUrl = env.managerUrl;
    const keycloakUrl = env.keycloakUrl;
    const port = env.port;
    const IS_DEV_SERVER = !!process.argv.find(arg => arg.includes("serve"));

    if (IS_DEV_SERVER) {
        console.log("");
        console.log("To set a path to a custom manager_config.json use the config environment argument with a path");
        console.log("relative to this app e.g: ");
        console.log("");
        console.log("npm run serve -- --env config=../../../manager_config.json");
        console.log("");
    }

    const config = util.getAppConfig(argv.mode, IS_DEV_SERVER, __dirname, managerUrl, keycloakUrl, port);

    if (IS_DEV_SERVER) {
        console.log("CUSTOM_CONFIG_DIR: " + customConfigDir || "");
        console.log("");
    }

    if (IS_DEV_SERVER && customConfigDir) {
        // Try and include the static files in the specified config dir if we're in dev server mode
        config.plugins.push(new CopyWebpackPlugin({
            patterns: [
                {
                    from: customConfigDir
                }
            ]
        }));
    }

    if (IS_DEV_SERVER) {
        config.performance = {
            hints: false
        };
    }

    // Add a custom base URL to resolve the config dir to the path of the dev server not root
    config.plugins.push(
        new webpack.DefinePlugin({
            CONFIG_URL_PREFIX: JSON.stringify(IS_DEV_SERVER && customConfigDir ? "/manager" : "")
        })
    );

    return config;
};
