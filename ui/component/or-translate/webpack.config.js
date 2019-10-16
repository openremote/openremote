const util = require("../webpack.util");

bundles = {
    "index": {
        vendor: {
            "i18next": "i18next"
        },
        excludeOr: true
    },
    "index.bundle": {
        excludeOr: true,
    },
    "index.orbundle": undefined
};

module.exports = util.generateExports(__dirname);