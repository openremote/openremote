import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";

test.beforeEach(async ({ manager }) => {
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
});

test("Add new Realm", async ({ page, realmsPage }) => {
  // When Navigate to "Realms" page
  await realmsPage.goto();
  // Then Add a new Realm
  await realmsPage.addRealm("smartcity");
  // When Select smartcity realm
  await page.click("#realm-picker");
  await page.click('li[role="menuitem"]:has-text("smartcity")');
  // Then We see the smartcity realm
  await expect(page.locator("#desktop-right #realm-picker")).toContainText("smartcity");
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
