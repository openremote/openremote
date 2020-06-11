var webpack = require("webpack");
var path = require("path");
var CopyWebpackPlugin = require("copy-webpack-plugin");
var HtmlWebpackPlugin = require("html-webpack-plugin");

var config = {
    entry: {
        'bundle': './src/index.js'
    },
    output: {
        path: __dirname + "/dist",
        publicPath: "",
        filename: "[name].[hash].js"
    },
    module: {
        rules: [
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

module.exports = (env, argv) => {

    const production = argv.mode === "production";
    const managerUrl = production ? undefined : "http://localhost:8080";
    const IS_DEV_SERVER = process.argv.find(arg => arg.includes('webpack-dev-server'));
    const OUTPUT_PATH = IS_DEV_SERVER ? 'src' : 'dist';

    // Only use babel for production otherwise source maps don't work
    if (production) {
        config.module.rules.push(
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
        );
    } else {
        config.module.rules.push(
            {
                test: /\.js$/,
                use: ["source-map-loader"],
                enforce: "pre",
                exclude: [
                    /node_modules/
                ]
            },
        );
    }

    config.plugins = [
        // Conditional compilation variables
        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(production),
            MANAGER_URL: JSON.stringify(managerUrl),
            "process.env":   {
                BABEL_ENV: JSON.stringify(argv.mode)
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
            // {
            //     from: path.join(path.dirname(require.resolve("@openremote/or-app/package.json")), "locales"),
            //     to: "locales"
            // },
            {
                from: "./images",
                to: "images"
            }
            // {
            //     from: "./locales",
            //     to: "locales"
            // }
        ])
    ];

    config.devtool = production ? false :"inline-source-map",
    config.devServer = {
        historyApiFallback: {
            index: "/" + __dirname.split(path.sep).slice(-1)[0] + "/",
        },
        port: 9000,
        publicPath: "/" + __dirname.split(path.sep).slice(-1)[0] + "/",
        contentBase: OUTPUT_PATH,
        disableHostCheck: true
    };
    config.watchOptions = {
        ignored: ['**/*.ts', 'node_modules']
    }

    return config;
};