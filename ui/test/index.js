import { defineConfig, test } from "@playwright/test";
import { expect, devices } from "@playwright/experimental-ct-core";

import { createPlugin } from "./plugin";
import { ct as base, componentFixtures } from "./fixtures/component";

function createAppSetupAndTeardown(app) {
  return [
    {
      name: `setup ${app}`,
      testMatch: "**/*.setup.ts",
      teardown: `cleanup ${app}`,
      worker: 1,
    },
    {
      name: `cleanup ${app}`,
      testMatch: "**/*.cleanup.ts",
      worker: 1,
    },
  ];
}

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
const ct = base.extend(componentFixtures);

export * from "./fixtures/component";
export * from "./fixtures/page";
export { test, ct, expect, devices, defineConfig, defineCtConfig, createAppSetupAndTeardown };
