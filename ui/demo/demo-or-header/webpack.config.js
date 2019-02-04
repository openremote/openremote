
var HtmlWebpackPlugin = require("html-webpack-plugin");
// The below was an attempt to get native ES6 module support alongside bundled JS modules but due to lack
// of understanding about webpack internals and poor docs given up for now and will just rely on bundles.
// class ES6Plugin {
//     apply(compiler) {
//         compiler.hooks.normalModuleFactory.tap(
//             "NormalModuleReplacementPlugin",
//             nmf => {
//                 nmf.hooks.afterResolve.tap("NormalModuleReplacementPlugin", result => {
//                     if (!result) return;
//                     if (/node_modules/.test(result.request)) {
//                         let relPath = path.relative(__dirname, result.request);
//                         let dest = relPath.split(path.sep);
//                         while(dest.length > 0 && dest[0] !== 'node_modules') {
//                             dest.shift();
//                         }
//                         dest.shift();
//                         dest = path.join(compiler.options.output.path, 'modules', dest.join(path.sep));
//                         fs.mkdirsSync(path.dirname(dest));
//                         fs.copySync(result.request, dest, {overwrite: true});
//                         result.resource = dest;
//                     } else {
//                         console.log(result.request);
//                     }
//                     return result;
//                 });
//             }
//         );
//     }
// }

module.exports = {
    mode: 'development',
    entry: {
        'bundle': './src/index.ts'
    },
    output: {
        path:     __dirname + "/dist",
        filename: "[name].[hash].js",
        publicPath: ""
    },
    resolve: {
        extensions: [ '.tsx', '.ts', '.js' ],
        mainFields: ['types', 'browser', 'module', 'main']
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
                test: /\.tsx?$/,
                use: 'awesome-typescript-loader',
                exclude: /node_modules/
            },
            {
                test: /\.js$/,
                exclude: /webcomponentsjs/,
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            presets: ['@babel/preset-env']
                        }
                    }
                ]
            }
        ]
    }
};