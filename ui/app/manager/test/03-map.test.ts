import { expect } from "@playwright/test";
import { test } from "./fixtures/manager.js";
import { preparedAssetsWithLocation as assets } from "./fixtures/data/assets.js";
import { users } from "./fixtures/data/users.js";

test.beforeEach(async ({ manager, browserName }) => {
  test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");
  // Given the Realm "smartcity" with the user "smartcity" and assets is setup
  await manager.setup("smartcity", { user: users.smartcity, assets });
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await manager.goToRealmStartPage("smartcity");
  await manager.login("smartcity");
});

test("Check markers on map", async ({ page }) => {
  const asset = assets[0].name;
  // Then Navigate to "map" tab
  // When Check "Battery" on map
  await page.waitForTimeout(1000);
  await expect(page.locator(".marker-icon")).toHaveCount(2);

  await page.click(".marker-container div or-icon svg path");
  const mapAssetCard = page.locator("#card-container", { hasText: asset });
  await mapAssetCard.waitFor();
  await page.waitForTimeout(400);
  await expect(mapAssetCard).toBeVisible();
  // Then Click and navigate
  await page.waitForTimeout(500);
  await page.click('button:has-text("View")');
  // Then We are at "Battery" page
  await page.waitForTimeout(1500);
  const assetPage = page.locator(`#asset-header >> text=${asset}`);
  await expect(assetPage).toBeVisible();
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
