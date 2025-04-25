import { defineConfig, devices } from "@playwright/test";

const { CI } = process.env;

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testMatch: "tests/**/*.js",
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
    baseURL: process.env.managerUrl || "localhost:8080/",
    launchOptions: {
      // force GPU hardware acceleration (even in headless mode)
      // without hardware acceleration, tests will be much slower
      args: ["--use-gl=desktop"],
    },
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "retain-on-failure",
    video: "on",
  },
  webServer: {
    command: "npm run serve",
    reuseExistingServer: true,
  },
  /* Configure projects for major browsers */
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        permissions: ["clipboard-write", "clipboard-read"],
      },
    },

    {
      name: "firefox",
      use: { ...devices["Desktop Firefox"] },
    },

    ...(CI
      ? [
          {
            name: "webkit",
            use: { ...devices["Desktop Safari"] },
          },
        ]
      : []),
  ],
});
