import fs from "node:fs";
import path from "node:path";

import traverse from "json-schema-traverse";
import { Resolve } from "@jsonforms/core";

// https://json-schema.org/understanding-json-schema/keywords
// see node_modules/@jsonforms/core/lib/models/jsonSchema7.d.ts
const jsonSchemaKeywords = [
  "$ref",
  "$id",
  "$schema",
  "title",
  "description",
  "default",
  "multipleOf",
  "maximum",
  "exclusiveMaximum",
  "minimum",
  "exclusiveMinimum",
  "maxLength",
  "minLength",
  // "pattern",
  "additionalItems",
  "items",
  "maxItems",
  "minItems",
  "uniqueItems",
  "maxProperties",
  "minProperties",
  "required",
  "additionalProperties",
  // "definitions",
  "properties",
  "patternProperties",
  "dependencies",
  "enum",
  "type",
  "allOf",
  "anyOf",
  "oneOf",
  "not",
  "format",
  "readOnly",
  "writeOnly",
  "examples",
  "contains",
  "propertyNames",
  "const",
  "if",
  "then",
  "else",
  "errorMessage",
];

const response = await fetch(
  "http://localhost:8080/api/master/model/valueDescriptors"
);
const valueDescriptors = await response.json();
const localesDir = path.resolve("ui/app/shared/locales");

function processSchemaPath(jsonPtr, root, name, paths) {
  const segments = jsonPtr.split("/").filter(Boolean);
  const processedPath = [];
  let currentSchema = root;
  for (const [i, segment] of segments.entries()) {
    if (["oneOf", "anyOf", "allOf"].includes(segment)) {
      const index = segments[i + 1];
      if (/^\d+$/.test(index)) {
        const keyword = segment;
        const schemaArray = currentSchema?.[keyword];
        const schemaAtIndex = schemaArray?.[parseInt(index, 10)];
        let title = schemaAtIndex?.title || `${keyword}[${index}]`;
        processedPath.push(title);
        currentSchema = schemaAtIndex;
        continue;
      }
    }
    if (!/^\d+$/.test(segment) && !jsonSchemaKeywords.includes(segment)) {
      processedPath.push(segment);
    }
    // Try to descend into schema so we can access children correctly
    if (currentSchema?.properties && currentSchema.properties[segment]) {
      currentSchema = currentSchema.properties[segment];
    } else if (currentSchema?.[segment]) {
      currentSchema = currentSchema[segment];
    }
  }
  const path = processedPath.join(".");
  if (path) {
    // TODO: push definitions onto the schema item
    paths.add(`${name}.${path}`);
  }
  // console.log(jsonPtr, '=>', path);
}

function collectMissingKeys(path, schemaItems) {
  const segs = path.split(".");
  segs.reduce((acc, key, index) => {
    if (index === segs.length - 1 && (!acc[key] || Object.keys(acc[key]) < 1)) {
      acc[key] = { label: "", description: "" };
    } else {
      acc[key] ||= {};
    }
    return acc[key];
  }, schemaItems);
}

async function processLocaleFile(currFile, name, paths) {
  const json = await fs.promises.readFile(currFile);
  if (!json.toString()) return;

  const translations = JSON.parse(json.toString());
  translations.schema ??= {};
  translations.schema.item ??= {};

  const schemaItems = translations.schema.item;

  for (const path of paths) {
    collectMissingKeys(path, schemaItems);
  }

  await fs.promises.writeFile(currFile, JSON.stringify(translations, null, 2));
}

async function processDescriptor(name, props, localesDir) {
  console.log("valueDescriptor: ", name);
  const response = await fetch(
    "http://localhost:8080/api/master/model/getItemSchemas",
    {
      method: "POST",
      body: JSON.stringify({ name, ...props }),
      headers: { "Content-Type": "application/json" },
    }
  );

  const resolvedSchema = await response.json();
  const paths = new Set();

  traverse(resolvedSchema, {}, (schema, jsonPtr, root) => {
    if (schema.$ref) return;
    processSchemaPath(jsonPtr, root, name, paths);
  });

  const filenames = await fs.promises.readdir(localesDir);
  const locales = filenames.filter((v) => v.length === 2);

  for (const locale of locales) {
    const currFile = `${localesDir}/${locale}/or.json`;
    await processLocaleFile(currFile, name, paths);
  }
}

const allowedDescriptors = [
  "agentLink",
  "attributeLink",
  "valueConstraint",
  "forecastConfiguration",
  "valueFormat",
  // "units",
];

for (const { name, ...props } of Object.values(valueDescriptors)) {
  if (!allowedDescriptors.includes(name)) continue;
  await processDescriptor(name, props, localesDir);
}

console.info(localesDir);
