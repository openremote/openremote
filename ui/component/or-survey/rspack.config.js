const util = require("@openremote/util");

bundles = {
    "index": {
        excludeOr: true
    },
    "index.bundle": {
    excludeOr: true,
    },
    "index.orbundle": undefined
};

module.exports = util.generateExports(__dirname);
