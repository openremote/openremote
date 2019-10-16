const path = require("path");

function getLibName(componentName) {
    if (componentName.startsWith("or-")) {
        componentName = componentName.substr(3);
    }
    componentName = componentName.replace(/-([a-z])/g, function (g) { return g[1].toUpperCase(); });
    return "OR" + componentName.charAt(0).toUpperCase() + componentName.substring(1);
}

function ORExternals(context, request, callback) {
    const match = request.match(/^@openremote\/([^\/]*)$/);
    if (match) {
        let component = getLibName(match[1]);
        console.log(request + " => " + component);
        return callback(null, "umd " + component);
    }
    callback();
}

function generateExternals(bundle) {
    if (!bundle) {
        return;
    }

    const externals = [];

    if (bundle.excludeOr) {
        externals.push(ORExternals);
    }
    if (bundle.vendor) {
        externals.push(bundle.vendor);
    }

    return externals;
}

function generateExports(dirname) {

    let libName = getLibName(dirname.split(path.sep).pop());

    return Object.entries(bundles).map(([name, bundle]) => {
        entry = {};
        entry[name] = "./dist/index.js";

        return {
            entry: entry,
            mode: "production",
            output: {
                filename: "[name].js",
                path: path.resolve(dirname, "dist/umd"),
                library: libName,
                libraryTarget: "umd"
            },
            externals: generateExternals(bundle)
        };
    });
}

module.exports = {
    getLibName: getLibName,
    generateExports: generateExports
};