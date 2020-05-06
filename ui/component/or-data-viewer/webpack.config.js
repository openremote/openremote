const util = require("../webpack.util");

bundles = {
    "index": {
        excludeOr: true
    },
    "index.orbundle": undefined
};

module.exports = util.generateExports(__dirname);