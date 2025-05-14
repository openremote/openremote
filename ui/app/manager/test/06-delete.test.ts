import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import { users } from "./fixtures/data/users.js";
import { preparedAssets as assets } from "./fixtures/data/assets.js";
import { custom } from "./fixtures/data/roles.js";

test.beforeEach(async ({ manager }) => {
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { user: users.smartcity, role: custom, assets });
  // When Login to OpenRemote "master" realm as "admin"
  await manager.goToRealmStartPage("master");
  await manager.login("admin");
});

test("Delete assets", async ({ page, assetsPage }) => {
  // When Delete assets
  await assetsPage.deleteSelectedAsset("Battery");
  await page.waitForTimeout(500);
  await assetsPage.deleteSelectedAsset("Solar Panel");

  // must wait to confirm that assets have been deleted
  await page.waitForTimeout(500);
  // Then We should see an empty asset column
  await expect(page.locator("text=Console")).toHaveCount(1);
  await expect(page.locator("text=Solar Panel")).toHaveCount(0);
  await expect(page.locator("text=Battery")).toHaveCount(0);
});

test("Delete user", async ({ page, manager }) => {
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
  // When Navigate to "Users" page
  await manager.navigateToMenuItem("Users");
  // Then Delete user
  await page.click('td:has-text("smartcity")');
  await page.click('button:has-text("delete")');
  await page.click('div[role="alertdialog"] button:has-text("Delete")');

  await page.waitForTimeout(500);
  // Then We should not see the "smartcity" user
  await expect(page.locator(`td:has-text("smartcity")`)).toHaveCount(0);
});

test("Delete role", async ({ page, manager }) => {
  // Then Switch to "smartcity" realm
  await manager.switchToRealmByRealmPicker("smartcity");
  // Then Navigate to "Roles" page
  await manager.navigateToMenuItem("Roles");
  // Then Create a new role
  // Then Delete role
  await page.click("text=Custom");
  await page.waitForTimeout(100);
  await page.click('tr[class="attribute-meta-row expanded"] >> button:has-text("delete")');
  await page.click('div[role="alertdialog"] button:has-text("Delete")');
  await page.waitForTimeout(100);
  // Then We should not see the Custom role
  await expect(page.locator("text=Custom")).toHaveCount(0);
});

test("Delete realm", async ({ page, manager, realmsPage }) => {
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
