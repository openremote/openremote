var production = process.env.NODE_ENV === "production";
var webpack = require("webpack");

module.exports = {
    entry: {
        'bundle': './dist/index.js'
    },
    output: {
        path:     __dirname + "/dist",
        library: 'ORIcon',
        libraryExport: "default",
        libraryTarget: 'umd',
        filename: "bundle.js"
    },
    externals: [
    ],
    plugins: [
        // Conditional compilation variables
        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(production)
        })
    ]
};