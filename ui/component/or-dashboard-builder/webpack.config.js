const util = require("@openremote/util");

var bundles = {
    "index": {
        excludeOr: true
    },
    "index.orbundle": undefined
};

module.exports = util.generateExports(__dirname);
