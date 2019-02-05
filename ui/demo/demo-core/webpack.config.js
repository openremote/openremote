var HtmlWebpackPlugin = require("html-webpack-plugin");
var path = require("path");

module.exports = {
    mode: 'development',
    entry: {
        'bundle': './src/index.js'
    },
    output: {
        path: __dirname + "/dist",
        filename: "[name].[hash].js",
        publicPath: ""
    },
    devtool: 'inline-source-map',
    devServer: {
        port: 9000,
        contentBase: './dist',
        publicPath: "/" + __dirname.split(path.sep).slice(-1)[0]  + "/"
    },
    plugins: [
        new HtmlWebpackPlugin({
            chunksSortMode: 'none',
            inject: false,
            template: 'index.html'
        })
    ],
    module: {
        rules: [
            {
                test: /\.js$/,
                use: ["source-map-loader"],
                enforce: "pre",
                exclude: [
                    /node_modules/
                ]
            },
            {
                test: /\.js$/,
                include: function(modulePath) {
                    return /(@webcomponents[\/|\\]shadycss|lit-css|styled-lit-element|lit-html|@polymer|@lit|pwa-helpers)/.test(modulePath) || !/node_modules/.test(modulePath);
                },
                use: [
                    {
                        loader: 'babel-loader'
                    }
                ]
            },
            {
                test: /\.css$/,
                use: [
                    { loader: "css-loader" }
                ]
            },
            {
                test: /\.(png|jpg|gif|svg|eot|ttf|woff|woff2)$/,
                loader: 'url-loader',
                options: {
                    outputPath: "images/",
                    limit: 10000,
                },
            }
        ]
    }
};