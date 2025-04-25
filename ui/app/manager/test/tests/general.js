import { expect } from "@playwright/test";
import { test } from "../fixtures/test";

// TODO: check the feature to see what the feature expects??
test.beforeEach(async ({ setup }) => {
  // Setup {string}
  await setup("smartcity", section);
  // Setup {string} for map
  await setup("smartcity", section, "location");
  // Setup {string} for rules
  await setup("smartcity", section, "config");
});

test("Add Realm", async ({ page, addRealm, setup }) => {
  When("Login to OpenRemote {string} realm as {string}", async function (realm, user) {
    let startTime = new Date() / 1000;
    await this.openApp(realm);
    await this.login(user);
    this.logTime(startTime);
  });

  /**
   * Setting menu
   */
  When("Navigate to {string} page", { timeout: 30000 }, async function (name) {
    let startTime = new Date() / 1000;
    await this.wait(200);
    await this.navigateToMenuItem(name);
    await this.wait(200);
    this.logTime(startTime);
  });

  /**
   * Tab menu
   */
  When("Navigate to {string} tab", async function (tab) {
    let startTime = new Date() / 1000;
    await this.navigateToTab(tab);
    await this.wait(200);
    this.logTime(startTime);
  });

  Then("Unselect", async function () {
    let startTime = new Date() / 1000;
    await this.unselect();
    this.logTime(startTime);
  });

  /**
   * snapshots
   */
  Then("Snapshot {string}", async function (string) {
    const { page } = this;
    await page?.screenshot({ path: join("test", "screenshots", `${string}.png`) });
  });
});
