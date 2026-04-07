import { basename, resolve } from "node:path";
import { defineConfig as baseConfig, devices, Project } from ".";

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

/**
 * Creates a setup and teardown test project for a given app.
 * These configurations are intended to run before and after all other tests,
 * typically for initializing and cleaning up test environments. The app
 * project should have a dependency on the setup project while the cleanup
 * project is referenced to the setup project.
 *
 * @param app - The name of the app these projects are meant to be used by.
 * @returns An array of two configuration objects: one for setup and one for cleanup
 */
function createAppSetupAndTeardown(app: string) {
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

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export const defineAppConfig = (path: string) => {
  const name = basename(path);
  return baseConfig({
    testMatch: "*.test.ts",
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: Boolean(CI),
    /* Retry failed tests twice on CI only to allow flaky behavior such as test timeouts to be retried */
    retries: CI ? 2 : 0,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: [["html", { outputFolder: "app-test-report" }]],
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
      // Defaults to the default Manager Docker container port as that significantly speeds up the tests compared to serving the frontend with Webpack
      baseURL: managerUrl || DEV ? "http://localhost:9000" : "http://localhost:8080",
      /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
      trace: "retain-on-failure",
      video: "on",
    },
    /* Configure projects */
    projects: [
      ...createAppSetupAndTeardown(name),
      ...browsers.flatMap((browser) => ({
        name: `${name} ${browser.name}`,
        testDir: resolve(path, "test"),
        fullyParallel: false,
        dependencies: [`setup ${name}`],
        workers: 1,
        ...browser,
      })),
    ],
  });
};
