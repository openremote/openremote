import { defineConfig, test as base } from "@playwright/test";
import { expect, devices } from "@playwright/experimental-ct-core";

import { defineConfig as defineCtWebConfig } from "@sand4rt/experimental-ct-web";

import { createPlugin } from "./plugin";
import { ct as ctBase, fixtures, camelCaseToSentenceCase } from "./fixtures";

function defineCtConfig(...configs) {
  const original = defineCtWebConfig(...configs);

  return {
    ...original,
    "@playwright/test": {
      // Includes babelPlugins to transform the test source code for Playwright UI
      ...original["@playwright/test"],
      // Playwright Webpack plugin
      plugins: [createPlugin],
    },
    "@playwright/experimental-ct-core": {
      // Used to attach components to the document
      ...original["@playwright/experimental-ct-core"],
    },
  };
}

// Must extend in the root of the package
const test = base.extend(fixtures);
const ct = ctBase.extend(fixtures);

export { test, ct, expect, devices, defineConfig, defineCtConfig, camelCaseToSentenceCase };
