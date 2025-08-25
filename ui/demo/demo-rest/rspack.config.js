const util = require("@openremote/util");

module.exports = (env, argv) => {
    const IS_DEV_SERVER = process.argv.find(arg => arg.includes('rspack serve'));
    return util.getAppConfig(argv.mode, IS_DEV_SERVER, __dirname);
};
