const util = require("@openremote/util");

bundles = {
    "index": {
        vendor: {
            "axios": "axios",
            "qs": "Qs"
        },
        excludeOr: true
    },
    "index.bundle": {
        excludeOr: true,
    }
};

module.exports = util.generateExports(__dirname);
