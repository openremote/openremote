import baseConfig from "../custom-elements-manifest.config.mjs";
import {cemInheritancePlugin} from "@wc-toolkit/cem-inheritance";
import {fileURLToPath} from "node:url";
import path from "node:path";

const vaadinPkg = fileURLToPath(import.meta.resolve("@vaadin/button/package.json"));
const vaadinPath = path.join(vaadinPkg, "../../*/src/*.js").replaceAll("\\", "/");

export default {
    ...baseConfig,
    globs: [...baseConfig.globs, vaadinPath], // Also analyze the code inside the Vaadin folder
    plugins: [
        ...baseConfig.plugins,
        {
            name: 'vaadin-jsdoc-parser',
            analyzePhase({ ts, node, moduleDoc }) {
                if (node.kind === ts.SyntaxKind.ClassDeclaration) {
                    const className = node.name.getText();
                    const declaration = moduleDoc.declarations.find(d => d.name === className);
                    const description = declaration?.description;

                    if (description) {
                        // 1. Parse CSS Parts
                        const partRegex = /`([^`]+)`\s*\|\s*([^\n|]+)/g;
                        if (description.includes('shadow DOM parts')) {
                            const partsSection = description.split('### Styling')[1]?.split('state attributes')[0];
                            let match;
                            declaration.cssParts = declaration.cssParts || [];
                            while ((match = partRegex.exec(partsSection)) !== null) {
                                declaration.attributes.push({ name: match[1], description: match[2].trim(), type: { text: "string" }});
                            }
                        }

                        // 2. Parse State Attributes (Mapped to attributes in CEM)
                        if (description.includes('state attributes')) {
                            const attrSection = description.split('state attributes')[1]?.split('custom CSS properties')[0];
                            let match;
                            declaration.attributes = declaration.attributes || [];
                            while ((match = partRegex.exec(attrSection)) !== null) {
                                // Only add if it doesn't already exist from the Lit properties
                                if (!declaration.attributes.some(a => a.name === match[1])) {
                                    declaration.attributes.push({ name: match[1], description: match[2].trim(), type: { text: "boolean" }});
                                }
                            }
                        }

                        // 3. Parse CSS Variables
                        const propRegex = /`(--[^`]+)`/g;
                        if (description.includes('custom CSS properties')) {
                            const cssPropSection = description.split('custom CSS properties')[1];
                            let match;
                            declaration.cssProperties = declaration.cssProperties || [];
                            while ((match = propRegex.exec(cssPropSection)) !== null) {
                                declaration.cssProperties.push({ name: match[1] });
                            }
                        }
                    }
                }
            }
        },
        cemInheritancePlugin()
    ] // Inject the analysis of the inherited Vaadin code
}
