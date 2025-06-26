const util = require("@openremote/util");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const webpack = require("webpack");
const packageJson = require('./package.json');

module.exports = (env, argv) => {

    const managerUrl = env.managerUrl;
    const keycloakUrl = env.keycloakUrl;
    const port = env.port;
    const IS_DEV_SERVER = !!process.argv.find(arg => arg.includes("serve"));
    const config = util.getAppConfig(argv.mode, IS_DEV_SERVER, __dirname, managerUrl, keycloakUrl, port);

    if (IS_DEV_SERVER) {
        config.performance = {
            hints: false
        };
    }

    // Add a custom base URL to resolve the config dir to the path of the dev server not root
    config.plugins.push(
        new webpack.DefinePlugin({
            APP_VERSION: JSON.stringify(packageJson.version)
        })
    );

    return config;
};
