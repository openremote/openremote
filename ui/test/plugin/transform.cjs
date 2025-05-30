"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true,
});
exports.default = void 0;
exports.importInfo = importInfo;
var _path = _interopRequireDefault(require("path"));
var _babelBundle = require("playwright/lib/transform/babelBundle");
var _transform = require("playwright/lib/transform/transform");
function _interopRequireDefault(obj) {
  return obj && obj.__esModule ? obj : { default: obj };
}
/**
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const t = _babelBundle.types;
let jsxComponentNames;
let classComponentNames;
let importInfos;
var _default = (exports.default = (0, _babelBundle.declare)((api) => {
  api.assertVersion(7);
  const result = {
    name: "playwright-debug-transform",
    visitor: {
      Program: {
        enter(path) {
          jsxComponentNames = collectJsxComponentUsages(path.node);
          classComponentNames = collectClassMountUsages(path.node);
          importInfos = new Map();
        },
        exit(path) {
          let firstDeclaration;
          let lastImportDeclaration;
          path.get("body").forEach((p) => {
            if (p.isImportDeclaration()) lastImportDeclaration = p;
            else if (!firstDeclaration) firstDeclaration = p;
          });
          const insertionPath = lastImportDeclaration || firstDeclaration;
          if (!insertionPath) return;
          for (const [localName, componentImport] of [...importInfos.entries()].reverse()) {
            insertionPath.insertAfter(
              t.variableDeclaration("const", [
                t.variableDeclarator(
                  t.identifier(localName),
                  t.objectExpression([
                    t.objectProperty(t.identifier("__pw_type"), t.stringLiteral("importRef")),
                    t.objectProperty(t.identifier("id"), t.stringLiteral(componentImport.id)),
                  ])
                ),
              ])
            );
          }
          (0, _transform.setTransformData)("playwright-ct-core", [...importInfos.values()]);
        },
      },
      ImportDeclaration(p) {
        const importNode = p.node;
        if (!t.isStringLiteral(importNode.source)) return;
        const ext = _path.default.extname(importNode.source.value);

        // Convert all non-JS imports into refs.
        if (artifactExtensions.has(ext)) {
          for (const specifier of importNode.specifiers) {
            if (t.isImportNamespaceSpecifier(specifier)) continue;
            const { localName, info } = importInfo(importNode, specifier, this.filename);
            importInfos.set(localName, info);
          }
          p.skip();
          p.remove();
          return;
        }

        // Convert JS imports that are used as components in JSX expressions into refs.
        let importCount = 0;
        for (const specifier of importNode.specifiers) {
          if (t.isImportNamespaceSpecifier(specifier)) continue;
          const { localName, info } = importInfo(importNode, specifier, this.filename);
          if (jsxComponentNames.has(localName) || classComponentNames.has(localName)) {
            importInfos.set(localName, info);
            ++importCount;
          }
        }

        // All the imports were from JSX => delete.
        if (importCount && importCount === importNode.specifiers.length) {
          p.skip();
          p.remove();
        }
      },
      MemberExpression(path) {
        if (!t.isIdentifier(path.node.object)) return;
        if (!importInfos.has(path.node.object.name)) return;
        if (!t.isIdentifier(path.node.property)) return;
        path.replaceWith(
          t.objectExpression([
            t.spreadElement(t.identifier(path.node.object.name)),
            t.objectProperty(t.identifier("property"), t.stringLiteral(path.node.property.name)),
          ])
        );
      },
    },
  };
  return result;
}));
function collectJsxComponentUsages(node) {
  const names = new Set();
  (0, _babelBundle.traverse)(node, {
    enter: (p) => {
      // Treat JSX-everything as component usages.
      if (t.isJSXElement(p.node)) {
        if (t.isJSXIdentifier(p.node.openingElement.name)) names.add(p.node.openingElement.name.name);
        if (
          t.isJSXMemberExpression(p.node.openingElement.name) &&
          t.isJSXIdentifier(p.node.openingElement.name.object) &&
          t.isJSXIdentifier(p.node.openingElement.name.property)
        )
          names.add(p.node.openingElement.name.object.name);
      }
    },
  });
  return names;
}
function collectClassMountUsages(node) {
  const names = new Set();
  (0, _babelBundle.traverse)(node, {
    enter: (p) => {
      // Treat calls to mount and all identifiers in arguments as component usages e.g. mount(Component)
      if (t.isCallExpression(p.node) && t.isIdentifier(p.node.callee) && p.node.callee.name === "mount") {
        p.traverse({
          Identifier: (p) => {
            names.add(p.node.name);
          },
        });
      }
    },
  });
  return names;
}
function importInfo(importNode, specifier, filename) {
  const importSource = importNode.source.value;
  const idPrefix = _path.default.join(filename, "..", importSource).replace(/[^\w_\d]/g, "_");
  const result = {
    id: idPrefix,
    filename,
    importSource,
    remoteName: undefined,
  };
  if (t.isImportDefaultSpecifier(specifier)) {
  } else if (t.isIdentifier(specifier.imported)) {
    result.remoteName = specifier.imported.name;
  } else {
    result.remoteName = specifier.imported.value;
  }
  if (result.remoteName) result.id += "_" + result.remoteName;
  return {
    localName: specifier.local.name,
    info: result,
  };
}
const artifactExtensions = new Set([
  // Frameworks
  ".vue",
  ".svelte",
  // Images
  ".jpg",
  ".jpeg",
  ".png",
  ".gif",
  ".svg",
  ".bmp",
  ".webp",
  ".ico",
  // CSS
  ".css",
  // Fonts
  ".woff",
  ".woff2",
  ".ttf",
  ".otf",
  ".eot",
]);
