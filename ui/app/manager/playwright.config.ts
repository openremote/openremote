import { defineConfig, devices } from "@playwright/test";
const { CI, DEV } = process.env;

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testMatch: ["test/**/*.test.ts"],
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: Boolean(CI),
  /* Retry on CI only */
  retries: CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: 1,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [["html"]],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: DEV ? "http://localhost:9000/" : "http://localhost:8080/",
    // baseURL: process.env.managerUrl || "localhost:9000/",
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "retain-on-failure",
    video: "on",
  },
  ...(DEV
    ? {
        webServer: {
          // Wait for the dev server to be ready
          url: "http://localhost:9000/manager/",
          command: "npm run serveNoModelBuild",
          reuseExistingServer: true,
        },
      }
    : {}),
  /* Configure projects for major browsers */
  projects: [
    {
      name: "setup",
      testMatch: /test\/global\.setup\.ts/,
      teardown: "cleanup",
    },
    {
      name: "cleanup",
      testMatch: /test\/global\.cleanup\.ts/,
    },
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
      dependencies: ["setup"],
    },
    {
      name: "firefox",
      use: { ...devices["Desktop Firefox"] },
      dependencies: ["setup"],
    },
    {
      name: "webkit",
      use: { ...devices["Desktop Safari"] },
      dependencies: ["setup"],
    },
  ],
});
