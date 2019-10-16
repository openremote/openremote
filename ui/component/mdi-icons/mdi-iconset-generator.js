var fs = require('fs');
var path = require("path");
var xpath = require('xpath');
var dom = require('xmldom').DOMParser;

if (!fs.existsSync("src")){
    fs.mkdirSync("src");
}

let mdiSvgDir = path.join(path.dirname(require.resolve("@mdi/svg/package.json")), "svg");
let mdiStream = fs.createWriteStream("./src/index.ts" ,{flags: "w+"});
mdiStream.write("import {IconSetSvg} from \"@openremote/or-icon\";\n");
mdiStream.write("let mdi: IconSetSvg = {\n");
mdiStream.write("  size: 24,\n");
mdiStream.write("  icons: {\n");

let files = fs.readdirSync(mdiSvgDir);
let nameArr = [];

for (let i=0; i<files.length; i++) {

    let file = files[i];
    let fullPath = path.join(mdiSvgDir, file);
    let data = fs.readFileSync(fullPath, "utf8");
    let svg = new dom().parseFromString(data);
    let select = xpath.useNamespaces({"svg": "http://www.w3.org/2000/svg"});
    let pathAttrs = select("//svg:path/@d", svg);
    let pathData = pathAttrs[0].value;
    let name = file.substr(0, file.length-4);
    name = name.replace(/-([\w])/g, function (g) { return g[1].toUpperCase(); });
    nameArr.push("Mdi" + name.charAt(0).toUpperCase() + name.substring(1));
    mdiStream.write("    " + name + ":\"" + pathData + "\"" + (i < files.length-1 ? "," : "") + "\n");

    let iconFilename = "./src/" + name + ".ts";
    if (!fs.existsSync(iconFilename)) {
        let iconStream = fs.createWriteStream(iconFilename, {flags: "w+"});
        iconStream.write("export const Mdi" + name.charAt(0).toUpperCase() + name.substring(1) + " = \"" + pathData + "\";");
        iconStream.close();
    }
}

mdiStream.write("    }\n");
mdiStream.write("  };\n");
mdiStream.write("\n");
mdiStream.write("export default mdi;\n");
mdiStream.write("export const {" + nameArr.join(",") + "} = mdi.icons;\n");
mdiStream.close();