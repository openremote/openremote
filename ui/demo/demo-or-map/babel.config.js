module.exports = api => {

    api.cache(false);
    return {
        presets: [
            [
                "@babel/preset-env",
                {
                    modules: false,
                    exclude: ['transform-async-to-generator', 'transform-regenerator'],
                    targets: [
                        "last 2 versions",
                        "IE 11"
                    ]
                }
            ]
        ],
        plugins: [
            "@babel/plugin-transform-regenerator",
            "@babel/external-helpers",
            "@babel/syntax-dynamic-import",
            "@babel/syntax-object-rest-spread",
            [
                "@babel/plugin-transform-runtime",
                {
                    "regenerator": true
                }
            ]
        ]
    };
};
