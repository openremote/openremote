var HtmlWebpackPlugin = require("html-webpack-plugin");
var path = require("path");
var CopyWebpackPlugin = require("copy-webpack-plugin");

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
    watchOptions: {
        ignored: ['**/*.ts', 'node_modules']
    },
    plugins: [
        new HtmlWebpackPlugin({
            chunksSortMode: 'none',
            inject: false,
            template: 'index.html'
        }),
        new CopyWebpackPlugin([
            {
                from: path.dirname(require.resolve("@webcomponents/webcomponentsjs")),
                to: "modules/@webcomponents/webcomponentsjs",
                ignore: "!*.js"
            }
        ])
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
                    return /(@webcomponents[\/|\\]shadycss|lit-css|styled-lit-element|lit-html|lit-element|@polymer|@lit|pwa-helpers)/.test(modulePath) || !/node_modules/.test(modulePath);
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
                    limit: 10000
                }
            }
        ]
    }
};