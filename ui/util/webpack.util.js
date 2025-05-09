const fs = require("fs");
const path = require("path");
const webpack = require("webpack");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const ForkTsCheckerNotifierWebpackPlugin = require("fork-ts-checker-notifier-webpack-plugin");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin");

function getStandardModuleRules() {
    return {
        rules: [
            {
                test: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/, //output css as strings
                type: "asset/source"
            },
            {
                test: /\.css$/, //
                exclude: /(maplibre|mapbox|@material|gridstack|@mdi).*\.css$/,
                use: [
                    { loader: "css-loader" }
                ]
            },
            {
                test: /\.(png|jpg|ico|gif|svg|eot|ttf|woff|woff2|mp4)$/,
                type: "asset",
                generator: {
                    filename: 'images/[hash][ext][query]'
                }
            },
            {
                test: /\.tsx?$/,
                exclude: /node_modules/,
                use: {
                    loader: "ts-loader",
                    options: {
                        projectReferences: true
                    }
                }
            }
        ]
    };
}

function getAppConfig(mode, isDevServer, dirname, managerUrl, keycloakUrl, port) {
    const production = mode === "production";
    port = port || 9000;
    managerUrl = managerUrl || (production && !isDevServer ? undefined : "http://localhost:8080");
    const OUTPUT_PATH = isDevServer ? 'src' : 'dist';

    if (isDevServer) {
        console.log("");
        console.log("To customise the URL of the manager and/or keycloak use the managerUrl and/or keycloakUrl");
        console.log(" environment arguments e.g: ");
        console.log("");
        console.log("npm run serve -- --env managerUrl=https://localhost");
        console.log("npm run serve -- --env keycloakUrl=https://localhost/auth");
        console.log("");
        console.log("MANAGER URL: " + managerUrl || "");
        console.log("KEYCLOAK URL: " + keycloakUrl || (managerUrl + "/auth"));
        console.log("");
    }

    const config = {
        entry: {
            'bundle': './src/index.ts'
        },
        output: {
            path: dirname + "/dist",
            publicPath: isDevServer ? "/" + dirname.split(path.sep).slice(-1)[0] + "/" : "./",
            filename: production ? "[name].[contenthash].js" : "[name].js"
        },
        module: {...getStandardModuleRules()},
        // optimization: {
        //     minimize: true,
        //     minimizer: [
        //         // For webpack@5 you can use the `...` syntax to extend existing minimizers (i.e. `terser-webpack-plugin`), uncomment the next line
        //         // `...`,
        //         new CssMinimizerPlugin(),
        //     ],
        // },
        resolve: {
            extensions: [".ts", ".tsx", "..."],
            fallback: {
                "vm": false,
                "querystring": require.resolve("querystring-es3")
            }
        }
    };

    config.plugins = [
        // Conditional compilation variables
        new webpack.DefinePlugin({
            PRODUCTION: JSON.stringify(production),
            MANAGER_URL: JSON.stringify(managerUrl),
            KEYCLOAK_URL: JSON.stringify(keycloakUrl),
            "process.env":   {
                BABEL_ENV: JSON.stringify(mode)
            }
        }),
        // Generate our index page
        new HtmlWebpackPlugin({
            chunksSortMode: 'none',
            inject: false,
            template: 'index.html'
        })
    ];

    if (production) {
        config.plugins = [
            // new ForkTsCheckerWebpackPlugin({
            //     async: false,
            //     typescript: {
            //         memoryLimit: 4096
            //     }
            // }),
            ...config.plugins
        ];

        // Only use babel for production otherwise source maps don't work
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
        config.plugins = [
            // new ForkTsCheckerWebpackPlugin({
            //     typescript: {
            //         memoryLimit: 4096
            //     }
            // }),
            // new ForkTsCheckerNotifierWebpackPlugin({ title: 'TypeScript', excludeWarnings: false }),
            ...config.plugins
        ];
    }

    if (isDevServer) {
        // Load source maps generated by typescript
        // config.devtool = 'inline-source-map';
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

    // Build list of resources to copy
    const patterns = [
        {
            from: path.dirname(require.resolve("@webcomponents/webcomponentsjs")),
            to: "modules/@webcomponents/webcomponentsjs",
            globOptions: {
                ignore: ["!*.js"]
            }
        },
    ];
    // Check if images dir exists
    if (fs.existsSync(path.join(dirname, "images"))) {
        patterns.push(
            {
                from: "./images",
                    to: "images"
            }
        );
    }
    // Check if locales dir exists
    if (fs.existsSync(path.join(dirname, "locales"))) {
        patterns.push(
            {
                from: "./locales",
                to: "locales"
            }
        );
    }
    // Check if htaccess file exists
    if (fs.existsSync(path.join(dirname, ".htaccess"))) {
        patterns.push(
            {
                from: ".htaccess",
                to: ".htaccess",
                toType: 'file'
            }
        );
    }
    //Check if .appignore file exists
    if (fs.existsSync(path.join(dirname, ".appignore"))) {
      patterns.push(
        {
          from: ".appignore",
          to: ".appignore",
          toType: 'file'
        }
      );
    }

    // Copy unprocessed files
    config.plugins.push(
        new CopyWebpackPlugin({
            patterns: patterns
        })
    );

    config.devtool = !isDevServer ? false : "inline-source-map";
    config.devServer = {
        historyApiFallback: {
            index: "/" + dirname.split(path.sep).slice(-1)[0] + "/",
        },
        port: port,
        open: false,
        hot: false, // HMR doesn't work with webcomponents at present
        liveReload: true,
        static: OUTPUT_PATH
    };
    config.watchOptions = {
        ignored: ['node_modules']
    }

    return config;
}

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
        const entry = {};
        entry[name] = "./src/index.ts";

        const config = {
            entry: entry,
            mode: "production",
            output: {
                filename: "[name].js",
                path: path.resolve(dirname, "dist/umd"),
                library: libName,
                libraryTarget: "umd"
            },
            resolve: {
                extensions: [".ts", ".tsx", "..."],
                fallback: { "vm": false }
            },
            module: {...getStandardModuleRules()},
            externals: generateExternals(bundle)
        };

        return config;
    });
}

module.exports = {
    getLibName: getLibName,
    generateExports: generateExports,
    getAppConfig: getAppConfig
};
