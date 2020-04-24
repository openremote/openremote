const util = require("../webpack.util");

bundles = {
    "index.bundle": undefined
};

module.exports = util.generateExports(__dirname);