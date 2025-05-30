import type { Project } from "playwright/test";

export * from "playwright/test";
export { defineConfig, test } from "@playwright/test";
export {
  defineConfig as defineCtConfig,
  PlaywrightTestConfig,
  expect,
  devices,
} from "@playwright/experimental-ct-core";

export const createAppSetupAndTeardown: (app: string) => Project[];

export * from "./fixtures/component";
export * from "./fixtures/page";
