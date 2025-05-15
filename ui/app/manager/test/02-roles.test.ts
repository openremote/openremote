import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import { custom } from "./fixtures/data/roles.js";

test.beforeEach(async ({ manager }) => {
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { role: custom });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
});

test("Delete role", async ({ page, manager }) => {
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
  // Then Navigate to "Roles" page
  await manager.navigateToMenuItem("Roles");
  // Then Delete role
  await page.click("text=Custom");
  await page.click('tr[class="attribute-meta-row expanded"] >> button:has-text("delete")');
  await page.click('div[role="alertdialog"] button:has-text("Delete")');
  // Then We should not see the Custom role
  await expect(page.locator("text=Custom")).toHaveCount(0);
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
