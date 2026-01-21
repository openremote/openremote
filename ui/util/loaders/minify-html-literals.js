const { minifyHTMLLiterals } = require("minify-html-literals");

module.exports = function loader(source) {
    // Skip any TypeScript files that do not contain html`` template literals
    if (!source.match(/`([^`]+)`/)) return source;

    const result = minifyHTMLLiterals(source, this.getOptions());

    this.callback(null, result?.code ?? source, result?.map);
};
