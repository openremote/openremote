import { expect } from "@playwright/test";
import { test } from "./fixtures/test.js";

test.beforeEach(async ({ setup, login, goToRealmStartPage }) => {
  await goToRealmStartPage("master");
  // When Login to OpenRemote "master" realm as "admin"
  await login("admin");
});

test("Delete assets", async ({ page, deleteSelectedAsset }) => {
  // Given Setup "lv3"
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  // When Delete assets
  await deleteSelectedAsset("Battery");
  await page.waitForTimeout(500);
  await deleteSelectedAsset("Solar Panel");

  // must wait to confirm that assets have been deleted
  await page.waitForTimeout(500);
  // Then We should see an empty asset column
  await expect(page.locator("text=Console")).toHaveCount(1);
  await expect(page.locator("text=Solar Panel")).toHaveCount(0);
  await expect(page.locator("text=Battery")).toHaveCount(0);
});

test("Delete user", async ({ page, switchToRealmByRealmPicker, navigateToMenuItem }) => {
  // Given Setup "lv2"
  // When Login to OpenRemote "master" realm as "admin"
  // Then Switch to "smartcity" realm
  await switchToRealmByRealmPicker("smartcity")
  // When Navigate to "Users" page
  await navigateToMenuItem("Users");
  // Then Delete user
  await page.click('td:has-text("smartcity")');
  await page.click('button:has-text("delete")');
  await page.click('div[role="alertdialog"] button:has-text("Delete")');

  await page.waitForTimeout(500);
  // Then We should not see the "smartcity" user
  await expect(page.locator(`td:has-text("smartcity")`)).toHaveCount(0);
});

test("Delete role", async ({ page, switchToRealmByRealmPicker, navigateToMenuItem }) => {
  // Then Switch to "smartcity" realm
  await switchToRealmByRealmPicker("smartcity")
  // Then Navigate to "Roles" page
  await navigateToMenuItem("Roles");
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

test("Delete realm", async ({ page, deleteRealm, goToRealmStartPage, navigateToMenuItem }) => {
  // When Navigate to "Realms" page
  await navigateToMenuItem("Realms");
  // Then Delete realm
  await deleteRealm("smartcity");
  // Then We should not see the Realm picker
  await goToRealmStartPage("master");
  // must wait for the realm picker to be rendered
  await page.waitForTimeout(500);
  await expect(page.locator("#desktop-right #realm-picker")).not.toBeVisible();
});
