const util = require("@openremote/util");
const {rspack} = require('@rspack/core');
const packageJson = require('./package.json');
const {RsdoctorRspackPlugin} = require('@rsdoctor/rspack-plugin');

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

    if (process.env.RSDOCTOR === "true") {
        config.plugins.push(new RsdoctorRspackPlugin())
    }

    // Add a custom base URL to resolve the config dir to the path of the dev server not root
    config.plugins.push(
        new rspack.DefinePlugin({
          APP_VERSION: JSON.stringify(packageJson.version)
        })
    );

    return config;
};
