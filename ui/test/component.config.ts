import { basename, resolve } from "node:path";
import { defineCtConfig as baseConfig, Project } from ".";

const { CI } = process.env;

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export const defineCtConfig = (path: string) => {
  const name = basename(path);
  return baseConfig({
    testMatch: "*.test.ts",
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: Boolean(CI),
    /* Retry failed tests twice on CI only to allow flaky behavior such as test timeouts to be retried */
    retries: CI ? 2 : 0,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: [["html", { outputFolder: "component-test-report" }]],
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
      /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
      trace: "retain-on-failure",
      video: "on",
      ctTemplateDir: resolve(__dirname, "playwright"),
    },
    /* Configure projects */
    projects: [{ name, testDir: resolve(path, "test"), fullyParallel: true, use: { ct: name } }] as Project[],
  });
};
