import { expect } from "@playwright/test";
import { test } from "./fixtures/test.js";
import assets from "./fixtures/data/assets.js";

test.beforeEach(async ({ setup, login, goToRealmStartPage }) => {
  await setup("smartcity", "lv4");
  // When Login to OpenRemote "smartcity" realm as "smartcity"
  await goToRealmStartPage("smartcity");
  await login("smartcity");
});

test("Check markers on map", async ({ page, browserName }) => {
  test.skip(browserName === 'firefox', "firefox headless mode does not support webgl required by maplibre")
  const asset = assets[0].name
  // Then Navigate to "map" tab
  // When Check "Battery" on map
  await page.waitForTimeout(1000);
  await expect(page.locator(".marker-icon")).toHaveCount(2);

  await page.click(".marker-container div or-icon svg path");
  const mapAssetCard = page.locator('#card-container', { hasText: asset });
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
