import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";

test("Add new Realm", async ({ page, manager, realmsPage }) => {
  // Given Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
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

test("Delete realm", async ({ page, manager, realmsPage }) => {
  // Given the Realm "smartcity" is setup
  await manager.setup("smartcity");
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
  // When Navigate to "Realms" page
  await manager.navigateToMenuItem("Realms");
  // Then Delete realm
  await realmsPage.deleteRealm("smartcity");
  await page.waitForTimeout(500);
  // Then We should not see the Realm picker
  await manager.goToRealmStartPage("master");
  // must wait for the realm picker to be rendered
  await expect(page.locator("#desktop-right #realm-picker")).not.toBeVisible();
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
