// Script for copying @mdi/font files into dist dir
var fs = require("fs");
var path = require("path");

if (!fs.existsSync("dist")) {
    fs.mkdirSync("dist");
}

let fontDir = path.join(path.dirname(require.resolve("@mdi/font/package.json")), "fonts");
let cssDir = path.join(path.dirname(require.resolve("@mdi/font/package.json")), "css");

if (!fs.existsSync("dist/Material Design Icons")) {
    fs.mkdirSync("dist/Material Design Icons", {recursive: true});
}

fs.cpSync(fontDir, "./dist/Material Design Icons/fonts", {recursive: true});
fs.cpSync(cssDir, "./dist/Material Design Icons/css", {recursive: true});
