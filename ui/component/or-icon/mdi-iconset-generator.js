var fs = require('fs');
var path = require("path");
var xpath = require('xpath');
var dom = require('xmldom').DOMParser;

let mdiSvgDir = path.join(path.dirname(require.resolve("@mdi/svg/package.json")), "svg");
let mdiStream = fs.createWriteStream("./src/mdi-icons.ts" ,{flags: "w+"});
mdiStream.write("import {IconSetSvg} from \"./icon-set-svg\"; let mdi: IconSetSvg = new IconSetSvg(24, {");

let files = fs.readdirSync(mdiSvgDir);

for (let i=0; i<files.length; i++) {

    let file = files[i];
    let fullPath = path.join(mdiSvgDir, file);
    let data = fs.readFileSync(fullPath, "utf8");
    let svg = new dom().parseFromString(data);
    let select = xpath.useNamespaces({"svg": "http://www.w3.org/2000/svg"});
    let pathAttrs = select("//svg:path/@d", svg);
    let pathData = pathAttrs[0].value;
    let name = file.substr(0, file.length-4);
    if (!mdiStream.write("\"" + name + "\":\"" + pathData + "\"" + (i < files.length-1 ? "," : "")));
}

mdiStream.write("});export default mdi;");

mdiStream.close();