var production = process.env.NODE_ENV === "production";
var webpack = require("webpack");
var path = require("path");
var managerUrl = production ? undefined : "http://localhost:8080";
var CopyWebpackPlugin = require("copy-webpack-plugin");
var HtmlWebpackPlugin = require("html-webpack-plugin");

module.exports = {
    mode: 'development',
    entry: {
        'bundle': './src/components/smart-building-app.js'
    },
    output: {
        path:     __dirname + "/dist",
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
        // Conditional compilation variables
        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(production),
            MANAGER_URL: JSON.stringify(managerUrl),
            "process.env":   {
                BABEL_ENV: JSON.stringify(process.env.NODE_ENV)
            }
        }),
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
            },
            {
                from: "./images",
                to: "images"
            },
            {
                from: "./locales",
                to: "locales"
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
                    return /(@webcomponents[\/|\\]shadycss|lit-css|styled-lit-element|lit-element|lit-html|@polymer|@lit|pwa-helpers)/.test(modulePath) || !/node_modules/.test(modulePath);
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
