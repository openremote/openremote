import baseConfig from "../custom-elements-manifest.config.mjs";
import {cemInheritancePlugin} from "@wc-toolkit/cem-inheritance";
import {fileURLToPath} from "node:url";
import path from "node:path";

const vaadinPkg = fileURLToPath(import.meta.resolve("@vaadin/button/package.json"));
const vaadinPath = path.join(vaadinPkg, "../../*/src/*.js").replaceAll("\\", "/");

export default {
    ...baseConfig,
    globs: [...baseConfig.globs, vaadinPath], // Also analyze the code inside the Vaadin folder
    plugins: [...baseConfig.plugins, cemInheritancePlugin()] // Inject the analysis of the inherited Vaadin code
}
