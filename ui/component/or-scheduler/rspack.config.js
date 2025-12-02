const util = require("@openremote/util");

bundles = {
      "index": {
          vendor: {
              "moment": "moment"
          },
          excludeOr: true
      },
      "index.bundle": {
          excludeOr: true,
      },
    "index.bundle": undefined
};

module.exports = util.generateExports(__dirname);
