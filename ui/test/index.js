import { defineConfig, test as base } from "@playwright/test";
import { expect, devices } from "@playwright/experimental-ct-core";

import { createPlugin } from "./plugin";
import { ct as ctBase, fixtures, camelCaseToSentenceCase } from "./fixtures";

function defineCtConfig(...configs) {
  const original = defineConfig(...configs);
  return {
    ...original,
    "@playwright/test": {
      ...original["@playwright/test"],
      // Playwright Webpack plugin
      plugins: [createPlugin],
      // Required for Playwright UI to understand the test source code
      babelPlugins: [[require.resolve("./plugin/transform")]],
    },
    "@playwright/experimental-ct-core": {
      // Used to attach components to the document
      registerSourceFile: require.resolve("./plugin/registerSource"),
    },
  };
}

// Must extend in the root of the package
const test = base.extend(fixtures);
const ct = ctBase.extend(fixtures);

export { test, ct, expect, devices, defineConfig, defineCtConfig, camelCaseToSentenceCase };
