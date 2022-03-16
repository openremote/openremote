import { expect } from "@openremote/test";
import { test, userStatePath } from "./fixtures/manager.js";
import { preparedAssetsWithLocation as assets } from "./fixtures/data/assets.js";

test.use({ storageState: userStatePath });

test("Check markers on map", async ({ page, manager, browserName }) => {
  test.skip(browserName === "firefox", "firefox headless mode does not support webgl required by maplibre");
  // Given assets with location are setup
  await manager.setup("smartcity", { assets });
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await manager.goToRealmStartPage("smartcity");
  const asset = assets[0].name;
  // Then Navigate to "map" tab
  // When Check "Battery" on map
  await expect(page.locator(".marker-icon")).toHaveCount(2);

  await page.click(".marker-container div or-icon svg path");
  const mapAssetCard = page.locator("#card-container", { hasText: asset });
  await mapAssetCard.waitFor();
  await expect(mapAssetCard).toBeVisible();
  // Then Click and navigate
  await page.click('button:has-text("View")');
  // Then We are at "Battery" page
  const assetPage = page.locator(`#asset-header >> text=${asset}`);
  await expect(assetPage).toBeVisible();
});

test.afterEach(async ({ manager }) => {
  await manager.cleanUp();
});
