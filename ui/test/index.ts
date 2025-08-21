import { defineConfig, test as base, type PlaywrightTestConfig, Fixtures } from "@playwright/test";
import { expect, devices } from "@playwright/experimental-ct-core";

import { defineConfig as defineCtWebConfig } from "@sand4rt/experimental-ct-web";

import { createPlugin } from "./plugin";
import { ct as ctBase, withPage, fixtures } from "./fixtures";
export type * from "./fixtures";

function defineCtConfig(config: PlaywrightTestConfig): PlaywrightTestConfig {
  const original = defineCtWebConfig(config) as PlaywrightTestConfig & {
    "@playwright/test": any;
    "@playwright/experimental-ct-core": any;
  };

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
  } as PlaywrightTestConfig;
}

// Must extend in the root of the package
const test = base.extend(fixtures as Fixtures);
const ct = ctBase.extend(fixtures as Fixtures);

export { test, ct, expect, devices, defineConfig, defineCtConfig, withPage };
