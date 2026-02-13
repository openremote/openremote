import baseConfig from "../custom-elements-manifest.config.mjs";
import {cemInheritancePlugin} from "@wc-toolkit/cem-inheritance";
import {fileURLToPath} from "node:url";

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
            /*{ key: 'shadow DOM parts', category: 'cssParts', inputType: undefined },*/ // TODO: Fix CSS parts to not override HTML attributes
            { key: 'state attributes', category: 'attributes', inputType: 'boolean' },
            { key: 'custom CSS properties', category: 'cssProperties', inputType: 'string' }
        ];

        sections.forEach(({ key, category, inputType }, i) => {
            const content = description.split(key)[1]?.split(sections[i + 1]?.key || '$')[0] || '';
            const matches = [...content.matchAll(/`([^`]+)`(?:\s*\|\s*([^\n|]+))?/g)];
            declaration[category] ??= [];

            const entries = matches.flatMap(([_, name, desc]) => {
                return [{
                    name: name,
                    description: desc?.trim(),
                    ...(inputType && { type: { text: inputType } })
                }]
            });
            declaration[category].push(...entries);
        });
        console.debug(`Injected additional JSDoc documentation for class ${declaration.name}`)
    }
}

/**
 * Custom plugin that we wrote ourselves, that corrects the 'extends' keyword for our Vaadin classes.
 * The analyzer does not understand `extends (TextField as new () => TextField & LitElement)`,
 * so we manually parse the Vaadin class, and link it to the correct Vaadin NPM package.
 */
const VaadinSuperclassParserPlugin = {
    name: 'vaadin-superclass-parser',
    analyzePhase({ ts, node, moduleDoc }) {
        if (node.kind !== ts.SyntaxKind.ClassDeclaration || !node.heritageClauses) return;

        const className = node.name.getText();
        const declaration = moduleDoc.declarations?.find(d => d.name === className);
        const extendsClause = node.heritageClauses.find(h => h.token === ts.SyntaxKind.ExtendsKeyword);
        const baseName = extendsClause?.types[0]?.getText().match(/\(([^ ]+) as/)?.[1];

        if (declaration && baseName) {
            const pkg = baseName.replaceAll(/([a-z])([A-Z])/g, '$1-$2').toLowerCase(); // For example, parses TextField to 'text-field'
            declaration.superclass = {
                name: baseName,
                package: `@vaadin/${pkg}`
            };
            console.debug(`Resolved conflict for super class documentation for: @vaadin/${pkg}`);
        }
    }
}

/* -------------------------------- */

// Retrieve path to the Vaadin NPM package
const vaadinFilesURL = new URL("../*/src/*.js", import.meta.resolve("@vaadin/button/package.json"));
const vaadinPath = fileURLToPath(vaadinFilesURL).replaceAll('\\', '/');

export default {
    ...baseConfig,
    globs: [...baseConfig.globs, vaadinPath], // Also analyze the code inside the Vaadin folder
    plugins: [
        ...baseConfig.plugins,
        VaadinSuperclassParserPlugin, // Resolve conflict in package names
        cemInheritancePlugin(), // Inject the analysis of the inherited Vaadin code
        VaadinJSDocParserPlugin // Inject JSDoc of the inherited Vaadin code
    ]
}
