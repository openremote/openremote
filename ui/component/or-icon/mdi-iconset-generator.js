// Script for collating mdi svg icons (@mdi/svg) into a single JSON file
var fs = require('fs');
var path = require("path");
var xpath = require('xpath');
var dom = require('xmldom').DOMParser;

if (!fs.existsSync("src")){
    fs.mkdirSync("src");
}

let mdiSvgDir = path.join(path.dirname(require.resolve("@mdi/svg/package.json")), "svg");
if (!fs.existsSync("dist")) {
    fs.mkdirSync("dist");
}
let mdiStream = fs.createWriteStream("./dist/mdi-icons.json" ,{flags: "w+"});
mdiStream.write("{\"size\":24,\"icons\":{");

let files = fs.readdirSync(mdiSvgDir).sort();

for (let i=0; i<files.length; i++) {

    let file = files[i];
    let fullPath = path.join(mdiSvgDir, file);
    let data = fs.readFileSync(fullPath, "utf8");
    let svg = new dom().parseFromString(data);
    let select = xpath.useNamespaces({"svg": "http://www.w3.org/2000/svg"});
    let pathAttrs = select("//svg:path/@d", svg);
    let pathData = pathAttrs[0].value;
    let name = file.substr(0, file.length-4);
    //name = name.replace(/-([\w])/g, function (g) { return g[1].toUpperCase(); });
    mdiStream.write("\"" + name + "\":\"" + pathData + "\"" + (i < files.length-1 ? "," : ""));
}

mdiStream.write("}}");
mdiStream.close();
