var production = process.env.NODE_ENV === "production";
var webpack = require("webpack");

module.exports = {
    entry: {
        'bundle': './dist/index.js'
    },
    output: {
        path:     __dirname + "/dist",
        libraryExport: "default",
        libraryTarget: 'umd',
        filename: "bundle.js"
    },
    externals: [
        {"@openremote/core": "ORCore"},
        {"@openremote/rest": "ORRest"}
    ],
    optimization: {
        minimize: false
    },
    plugins: [
        // Conditional compilation variables
        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(production)
        })
    ]
};