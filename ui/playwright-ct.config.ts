import { defineCtConfig, devices, Project } from "@openremote/test";
const { CI } = process.env;

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
    name: "component",
    testDir: "component",
    fullyParallel: true,
    use: { ct: true, baseURL: "http://localhost:3100" },
  },
];

const projects: Project[] = [
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
export default defineCtConfig({
  testMatch: "*.test.ts",
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: Boolean(CI),
  /* Retry on CI only */
  retries: CI ? 2 : 0,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [["html", { outputFolder: "../playwright-ct-report" }]],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "retain-on-failure",
    video: "on",
  },
  /* Configure projects */
  projects,
});
