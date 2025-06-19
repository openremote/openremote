import { resolve } from "node:path";
import { defineCtConfig, Project } from "../";

const { CI } = process.env;

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default (path: string) => {
  const name = path.split("/").at(-1);
  return defineCtConfig({
    testMatch: "*.test.ts",
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: Boolean(CI),
    /* Retry on CI only */
    retries: CI ? 2 : 0,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: [["html", { outputFolder: "playwright-ct-report" }]],
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
