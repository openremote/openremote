import fs from "node:fs";
import path from "node:path";

const localesDir = path.resolve(import.meta.dirname, "locales");

const filenames = await fs.promises.readdir(localesDir);
const locales = filenames.filter((v) => v.length === 2);

function deleteEmptyKeys(value) {
  if (value === "") {
    return true;
  }
  if (typeof value === "object") {
    for (const key of Object.keys(value)) {
      if (deleteEmptyKeys(value[key])) {
        delete value[key];
      }
    }
  }
  return false;
}

function deleteEmptyObjects(value) {
  if (typeof value === "object" && !Object.keys(value).filter(Boolean).length) {
    return true;
  }
  if (typeof value === "object") {
    for (const key of Object.keys(value)) {
      if (deleteEmptyObjects(value[key])) {
        delete value[key];
      }
    }
  }
  return false;
}

for (const locale of locales) {
  const currFile = `${localesDir}/${locale}/or.json`;
  const json = await fs.promises.readFile(currFile);
  if (!json.toString()) continue;
  const translations = JSON.parse(json.toString());
  deleteEmptyKeys(translations.schema.item);
  deleteEmptyObjects(translations.schema);
  await fs.promises.writeFile(currFile, JSON.stringify(translations, null, 2));
}
