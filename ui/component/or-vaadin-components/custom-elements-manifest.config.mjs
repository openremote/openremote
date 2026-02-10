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
            { key: 'shadow DOM parts', category: 'attributes', inputType: 'string' },
            { key: 'state attributes', category: 'attributes', inputType: 'boolean' },
            { key: 'custom CSS properties', category: 'cssProperties' }
        ];

        sections.forEach(({ key, category, inputType }, i) => {
            const content = description.split(key)[1]?.split(sections[i + 1]?.key || '$')[0] || '';
            const matches = [...content.matchAll(/`([^`]+)`(?:\s*\|\s*([^\n|]+))?/g)];
            declaration[category] ??= [];

            const existingNames = new Set(declaration.attributes?.map(a => a.name));
            const entries = matches.flatMap(([_, name, desc]) => {
                if (inputType && existingNames.has(name)) return []; // Prevent duplicates
                return [{
                    name: name,
                    ...(inputType && { description: desc?.trim(), type: { text: inputType } })
                }]
            });
            declaration[category].push(...entries);
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
