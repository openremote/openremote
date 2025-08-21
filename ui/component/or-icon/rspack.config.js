const util = require("@openremote/util");

bundles = {
    "index.bundle": {
        excludeOr: true
    }
};

module.exports = util.generateExports(__dirname);
