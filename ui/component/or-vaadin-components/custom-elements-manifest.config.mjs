import baseConfig from "../custom-elements-manifest.config.mjs";
import {cemInheritancePlugin} from "@wc-toolkit/cem-inheritance";
import {fileURLToPath} from "node:url";
import path from "node:path";

/**
 * Custom plugin that we wrote ourselves, that parses the JSDoc format that Vaadin uses,
 * and generates the Custom Elements Manifest JSON fields for it.
 */
const VaadinJSDocParserPlugin = {
    name: 'vaadin-jsdoc-parser',
    analyzePhase({ ts, node, moduleDoc }) {
        if (node.kind !== ts.SyntaxKind.ClassDeclaration) return;
        const declaration = moduleDoc.declarations.find(d => d.name === node.name.getText());
        const description = declaration?.description;
        if (!description) return;

        const sections = [
            { key: 'shadow DOM parts', target: 'attributes', type: 'string' },
            { key: 'state attributes', target: 'attributes', type: 'boolean' },
            { key: 'custom CSS properties', target: 'cssProperties' }
        ];

        sections.forEach(({ key, target, type }, i) => {
            const content = description.split(key)[1]?.split(sections[i + 1]?.key || '$')[0];
            const regex = /`([^`]+)`(?:\s*\|\s*([^\n|]+))?/g;
            let match;

            declaration[target] = declaration[target] || [];
            while ((match = regex.exec(content)) !== null) {
                const entry = { name: match[1] };
                if (type) {
                    if (declaration.attributes?.some(a => a.name === entry.name)) continue;
                    Object.assign(entry, { description: match[2]?.trim(), type: { text: type } });
                }
                declaration[target].push(entry);
            }
        });
    }
}

/* -------------------------------- */

// Retrieve path to the Vaadin NPM package
const vaadinPkg = fileURLToPath(import.meta.resolve("@vaadin/button/package.json"));
const vaadinPath = path.join(vaadinPkg, "../../*/src/*.js").replaceAll("\\", "/");

export default {
    ...baseConfig,
    globs: [...baseConfig.globs, vaadinPath], // Also analyze the code inside the Vaadin folder
    plugins: [
        ...baseConfig.plugins,
        cemInheritancePlugin(), // Inject the analysis of the inherited Vaadin code
        VaadinJSDocParserPlugin
    ]
}
