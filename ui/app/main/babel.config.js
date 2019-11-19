module.exports = api => {

    api.cache(false);
    return {
        presets: [
            [
                "@babel/preset-env",
                {
                    modules: false,
                    exclude: ['transform-async-to-generator'],
                    targets: [
                        "last 2 versions",
                        "IE 11"
                    ]
                }
            ]
        ],
        plugins: [
            [
                "@babel/transform-runtime",
                {
                    "corejs": false,
                    "helpers": true,
                    "regenerator": true,
                    "useESModules": false
                }
            ],
            "@babel/syntax-dynamic-import",
            "@babel/syntax-object-rest-spread",
            [
                'module:fast-async',
                {
                    spec: true
                }
            ]
        ]
    };
};
