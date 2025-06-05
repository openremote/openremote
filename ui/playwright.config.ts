import { defineConfig, devices, createAppSetupAndTeardown, Project } from "@openremote/test";
const { CI, DEV, managerUrl } = process.env;

const browsers: Project[] = [
  {
    name: "chromium",
    use: { ...devices["Desktop Chrome"] },
  },
  // {
  //   name: "firefox",
  //   use: { ...devices["Desktop Firefox"] },
  // },
  // {
  //   name: "webkit",
  //   use: { ...devices["Desktop Safari"] },
  // },
];

const orProjects: Project[] = [
  {
    name: "manager",
    dependencies: ["setup manager"],
    testDir: "app/manager/test",
    fullyParallel: false,
    workers: 1,
  },
];

const projects: Project[] = [
  ...createAppSetupAndTeardown("manager"),
  ...browsers.flatMap((browser) =>
    orProjects.map((project) => ({
      ...project,
      name: `${project.name} ${browser.name}`,
      use: { ...browser.use, ...project.use },
    }))
  ),
];

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testMatch: "*.test.ts",
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: Boolean(CI),
  /* Retry on CI only */
  retries: CI ? 2 : 0,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [["html", { outputFolder: "../playwright-e2e-report" }]],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    // Defaults to the default Manager Docker container port as that significantly speeds up the tests compared to serving the frontend with Webpack
    baseURL: managerUrl || DEV ? "http://localhost:9000" : "http://localhost:8080",
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "retain-on-failure",
    video: "on",
  },
  /* Configure projects */
  projects,
});
